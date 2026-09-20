package com.music.bitchord.data.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * SpotiFLAC Mobile lyrics fetching engine for MusicBeat.
 *
 * Implements:
 * 1. Parallel candidate dispatch (parallelism = 3).
 * 2. Priority grace period (3000ms): higher-priority providers are prioritized without
 *    causing long blocking delays if one provider is slow.
 * 3. Provider health cooldowns (10 minutes on network/server failures).
 * 4. Singleflight in-flight request deduplication.
 * 5. Two-tier caching (24h success cache, 5m negative cache).
 * 6. Instrumental title heuristics and fallback searches.
 */
object LyricsFetchEngine {

    private const val PARALLELISM = 3
    private const val PRIORITY_GRACE_MS = 3000L

    private val INSTRUMENTAL_PATTERN = Regex(
        "(?i)(?:^|[\\s\\[(\\\\-])(?:instrumental|inst\\.?)(?:[\\s\\])]|$)"
    )

    private val flightScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<LyricsRepository.Result?>>()

    /**
     * Executes lyrics retrieval for a song using SpotiFLAC Mobile's fetching logic.
     */
    suspend fun fetchLyrics(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        sources: Set<LyricsSource>,
        order: List<LyricsSource>,
        prioritizeSyllableSync: Boolean,
        fetcher: suspend (source: LyricsSource, videoId: String, title: String, artist: String, durationMs: Long, album: String?) -> List<LyricLine>?,
    ): LyricsRepository.Result? {
        val flightKey = "${artist.lowercase().trim()}|${title.lowercase().trim()}|${durationMs / 1000}"

        // 1. Singleflight deduplication
        val deferred = inFlight.computeIfAbsent(flightKey) {
            flightScope.async {
                fetchLyricsInternal(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    album = album,
                    sources = sources,
                    order = order,
                    prioritizeSyllableSync = prioritizeSyllableSync,
                    fetcher = fetcher,
                )
            }
        }

        return try {
            deferred.await()
        } finally {
            inFlight.remove(flightKey, deferred)
        }
    }

    private suspend fun fetchLyricsInternal(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
        sources: Set<LyricsSource>,
        order: List<LyricsSource>,
        prioritizeSyllableSync: Boolean,
        fetcher: suspend (source: LyricsSource, videoId: String, title: String, artist: String, durationMs: Long, album: String?) -> List<LyricLine>?,
    ): LyricsRepository.Result? {
        // 2. Check Instrumental heuristic
        if (INSTRUMENTAL_PATTERN.containsMatchIn(title)) {
            LyricsLog.i("Engine", "Track marked instrumental by title heuristic: \"$title\"")
            return null
        }

        // 3. Check Cache
        val (cachedResult, isNegativeHit) = LyricsFetchCache.get(title, artist, durationMs)
        if (isNegativeHit) {
            LyricsLog.i("Engine", "Negative cache hit (no lyrics previously found): \"$title\"")
            return null
        }
        if (cachedResult != null) {
            LyricsLog.s("Engine", "Cache hit for \"$title\" from ${cachedResult.source.label}")
            return cachedResult
        }

        // Filter and order active sources
        val activeSources = order.filter { it in sources }
        if (activeSources.isEmpty()) {
            LyricsLog.w("Engine", "No lyrics sources enabled")
            return null
        }

        // 4. Parallel Dispatch with Priority Grace Period
        val result = executeParallelSearch(
            videoId = videoId,
            title = title,
            artist = artist,
            durationMs = durationMs,
            album = album,
            activeSources = activeSources,
            prioritizeSyllableSync = prioritizeSyllableSync,
            fetcher = fetcher,
        )

        // 5. Update Cache
        if (result != null) {
            LyricsFetchCache.putSuccess(title, artist, durationMs, result)
        } else {
            LyricsFetchCache.putNegative(title, artist, durationMs)
        }

        return result
    }

    private data class CandidateResult(
        val index: Int,
        val source: LyricsSource,
        val lines: List<LyricLine>?,
        val error: Throwable?,
    )

    private suspend fun executeParallelSearch(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
        activeSources: List<LyricsSource>,
        prioritizeSyllableSync: Boolean,
        fetcher: suspend (source: LyricsSource, videoId: String, title: String, artist: String, durationMs: Long, album: String?) -> List<LyricLine>?,
    ): LyricsRepository.Result? = coroutineScope {
        val semaphore = Semaphore(PARALLELISM)
        val resultsChannel = Channel<CandidateResult>(activeSources.size)

        // Filter out sources in health cooldown
        val viableSourcesWithIndex = activeSources.mapIndexedNotNull { index, source ->
            if (LyricsProviderHealth.shouldSkip(source.id)) null else (index to source)
        }

        if (viableSourcesWithIndex.isEmpty()) {
            LyricsLog.w("Engine", "All active providers are currently in cooldown")
            return@coroutineScope null
        }

        // Launch candidate searches
        viableSourcesWithIndex.forEach { (index, source) ->
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val lines = fetchWithFallback(source, videoId, title, artist, durationMs, album, fetcher)
                        if (!lines.isNullOrEmpty()) {
                            LyricsProviderHealth.markAvailable(source.id)
                        }
                        resultsChannel.send(CandidateResult(index, source, lines, null))
                    } catch (e: Throwable) {
                        if (e !is CancellationException) {
                            LyricsProviderHealth.markUnavailable(source.id, e)
                        }
                        resultsChannel.send(CandidateResult(index, source, null, e))
                    }
                }
            }
        }

        val completed = mutableSetOf<Int>()
        var bestCandidate: CandidateResult? = null
        var pendingCount = viableSourcesWithIndex.size
        var graceStartedAt = 0L

        fun hasPendingEarlier(targetIndex: Int): Boolean {
            return viableSourcesWithIndex.any { (idx, _) -> idx < targetIndex && idx !in completed }
        }

        while (pendingCount > 0) {
            // Check if current best candidate has no earlier candidates pending
            bestCandidate?.let { best ->
                if (!hasPendingEarlier(best.index)) {
                    resultsChannel.close()
                    return@coroutineScope LyricsRepository.Result(best.source, best.lines!!)
                }
            }

            val remainingGrace = if (bestCandidate != null && graceStartedAt > 0) {
                val elapsed = System.currentTimeMillis() - graceStartedAt
                (PRIORITY_GRACE_MS - elapsed).coerceAtLeast(0L)
            } else {
                Long.MAX_VALUE
            }

            if (bestCandidate != null && remainingGrace == 0L) {
                LyricsLog.i("Engine", "Priority grace period expired, returning ${bestCandidate.source.label}")
                resultsChannel.close()
                return@coroutineScope LyricsRepository.Result(bestCandidate.source, bestCandidate.lines!!)
            }

            // Await next result or timeout
            val candidate = if (bestCandidate != null) {
                kotlinx.coroutines.withTimeoutOrNull(remainingGrace) {
                    resultsChannel.receiveCatching().getOrNull()
                }
            } else {
                resultsChannel.receiveCatching().getOrNull()
            }

            if (candidate == null) {
                // Grace expired
                if (bestCandidate != null) {
                    LyricsLog.i("Engine", "Priority grace expired, returning ${bestCandidate.source.label}")
                    resultsChannel.close()
                    return@coroutineScope LyricsRepository.Result(bestCandidate.source, bestCandidate.lines!!)
                }
                break
            }

            completed.add(candidate.index)
            pendingCount--

            val lines = candidate.lines
            if (!lines.isNullOrEmpty()) {
                val isWordSynced = lines.any { it.isWordSynced }
                val isLineSynced = lines.any { it.timeMs > 0 }

                // Determine if this candidate is better than current best
                val isBetter = bestCandidate == null ||
                    (candidate.index < bestCandidate.index) ||
                    (prioritizeSyllableSync && isWordSynced && !bestCandidate.lines!!.any { it.isWordSynced })

                if (isBetter) {
                    bestCandidate = candidate
                    if (graceStartedAt == 0L) {
                        graceStartedAt = System.currentTimeMillis()
                    }
                }
            }
        }

        resultsChannel.close()
        bestCandidate?.let { LyricsRepository.Result(it.source, it.lines!!) }
    }

    private suspend fun fetchWithFallback(
        source: LyricsSource,
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
        fetcher: suspend (source: LyricsSource, videoId: String, title: String, artist: String, durationMs: Long, album: String?) -> List<LyricLine>?,
    ): List<LyricLine>? {
        // 1. Direct query
        var lines = runCatching { fetcher(source, videoId, title, artist, durationMs, album) }.getOrNull()
        if (!lines.isNullOrEmpty()) return lines

        // 2. Simplified title fallback
        val cleanTitle = simplifyTitle(title)
        val cleanArtist = LrcParser.cleanArtistForSearch(artist)

        if (cleanTitle != title || cleanArtist != artist) {
            lines = runCatching { fetcher(source, videoId, cleanTitle, cleanArtist, durationMs, album) }.getOrNull()
            if (!lines.isNullOrEmpty()) return lines
        }

        // 3. Primary artist fallback
        val primaryArtist = cleanArtist.split(",", "&", "/", "feat.", "ft.").firstOrNull()?.trim()
        if (!primaryArtist.isNullOrBlank() && primaryArtist != cleanArtist) {
            lines = runCatching { fetcher(source, videoId, cleanTitle, primaryArtist, durationMs, album) }.getOrNull()
            if (!lines.isNullOrEmpty()) return lines
        }

        return null
    }

    private fun simplifyTitle(title: String): String {
        return title.replace(Regex("(?i)\\((?:feat\\.?|ft\\.?|official|video|audio|remix|from)[^)]*\\)"), " ")
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("(?i)\\b(?:official (?:video|audio|music video)|lyric video|4k video)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
