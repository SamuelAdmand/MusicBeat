package com.music.bitchord.data.lyrics

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Where the player gets its lyrics.
 *
 * Eight sources, tried in [order] — the user's own priority list in Settings,
 * defaulting to [LyricsSource.entries]:
 *
 *  - [BetterLyrics] and [PaxSenix] — Apple Music TTML, per-syllable, from two
 *    independent hosts so one having a bad day doesn't cost the timing.
 *  - [LyricsPlus] — the YouLy+ backend; finest timing of the lot, flakiest hosting.
 *  - [SimpMusicLyrics] — keyed on the video id, so it can't fetch the wrong edit.
 *  - [LrcLib], [Musixmatch], [KuGou] — line-synced only, but between them
 *    almost always up, and [KuGou] carries a lot that the others don't.
 *
 * Every enabled source is asked *at the same time*, but their answers are
 * taken in [order]: the loop awaits them one at a time in that sequence, so a
 * lower-priority source finishing first never preempts one still pending
 * ahead of it. Asked one after another instead, a miss on each source would
 * cost its own round trip before the next was even tried, and a track with no
 * lyrics anywhere would spend the best part of a minute finding that out with
 * eight of them. Run together, a miss costs whatever the slowest one needed
 * to still be waited on took.
 *
 * A word-timed answer wins outright. Failing that, a line-timed one is taken
 * from the highest-priority source that had it — better a whole line lighting
 * up in sync than the right animation on lyrics that don't exist.
 */
object LyricsRepository {

    /** Lyrics, and which source they turned out to come from. */
    data class Result(val source: LyricsSource, val lines: List<LyricLine>)

    /**
     * [sources] is the user's pick from Settings; anything not in it is not
     * contacted at all. An empty set means no lyrics, which is the same answer
     * as switching the feature off. [order] is tried first-to-last; a source
     * missing from it (an upgrade that added one after the order was last
     * saved) falls in after everything named, in [LyricsSource]'s own order.
     *
     * [prioritizeSyllableSync] decides what happens once *something* has come
     * back: off, the highest-priority source's own answer is taken as-is,
     * word-synced or not — priority is priority, and second-guessing it with
     * more network calls after it has already answered is not what "first"
     * was supposed to mean. On, a merely line-synced answer is kept only as a
     * fallback, and the search keeps going through the rest of [order] for a
     * word-synced one, taking the top-priority source that has one.
     */
    suspend fun lyrics(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        sources: Set<LyricsSource> = com.music.bitchord.data.settings.AppSettings.lyricsSources.value,
        order: List<LyricsSource> = com.music.bitchord.data.settings.AppSettings.lyricsSourceOrder.value,
        prioritizeSyllableSync: Boolean = false,
    ): Result? = coroutineScope {
        LyricsLog.clear()
        LyricsLog.i("Repository", "Looking up lyrics for \"$title\" by \"$artist\" (${durationMs / 1000}s)")

        val sequence = order.filter { it in sources }

        LyricsLog.i("Repository", "Active sources order: ${sequence.joinToString { it.label }}")

        // Genius is a plain text web scraper. To preserve bandwidth and avoid rate-limiting,
        // it starts lazily and is only contacted if all higher-priority synced sources miss.
        val racing: List<Pair<LyricsSource, Deferred<List<LyricLine>?>>> = sequence.map { source ->
            val startMode = if (source.id == "genius") kotlinx.coroutines.CoroutineStart.LAZY else kotlinx.coroutines.CoroutineStart.DEFAULT
            source to async(Dispatchers.IO, start = startMode) {
                fetch(source, videoId, title, artist, durationMs, album)
            }
        }

        try {
            var lineSynced: Result? = null
            for ((source, job) in racing) {
                // If we already found a line-synced or better result, skip Genius completely
                if (lineSynced != null && source.id == "genius") {
                    LyricsLog.i("Repository", "Skipping Genius fallback because higher-priority source answered")
                    continue
                }

                if (source.id == "genius" && lineSynced == null) {
                    LyricsLog.w("Repository", "All synced providers missed. Running Genius fallback...")
                }

                val lines = runCatching { job.await() }.getOrNull() ?: continue
                if (lines.any { it.isWordSynced }) {
                    LyricsLog.s("Repository", "Word-synced match from ${source.label}")
                    return@coroutineScope result(source, lines)
                }
                if (!prioritizeSyllableSync && lines.any { it.timeMs > 0 }) {
                    LyricsLog.s("Repository", "Line-synced match from ${source.label}")
                    return@coroutineScope result(source, lines)
                }
                if (lineSynced == null) lineSynced = result(source, lines)
            }
            lineSynced
        } finally {
            // Whoever lost the race is no longer worth waiting on, and
            // coroutineScope will not return while they are still running.
            racing.forEach { it.second.cancel() }
        }
    }

    suspend fun fetch(
        source: LyricsSource,
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
    ): List<LyricLine>? {
        LyricsLog.i(source.label, "Querying ${source.label} (${source.id})...")

        val ext = com.music.bitchord.feature.lyrics.manager.LyricsExtensionManager.getExtension(source.id)
        var found: List<LyricLine>? = null

        if (ext != null && ext.scriptFile.exists()) {
            val raw = com.music.bitchord.feature.lyrics.engine.LyricsExtensionExecutor.getLyrics(
                extensionId = ext.id,
                scriptFile = ext.scriptFile,
                title = title,
                artist = artist,
                durationMs = durationMs,
                album = album,
                videoId = videoId,
            )
            if (!raw.isNullOrBlank()) {
                found = ProviderLyrics.parse(raw)
            }
        }

        if (found.isNullOrEmpty()) {
            LyricsLog.w(source.label, "No lyrics returned")
        } else {
            val syncType = when {
                found.any { it.isWordSynced } -> "word-synced"
                found.any { it.timeMs > 0 } -> "line-synced"
                else -> "plain text"
            }
            LyricsLog.s(source.label, "Returned ${found.size} lines ($syncType)")
        }
        return found
    }

    /**
     * Whichever source won, its lines get the same last pass: the answering
     * vocal split off the lead so it can be drawn under it. Done here rather
     * than in each parser because most of them write it as a bracket and only
     * [TtmlLyrics] knows it structurally — [withBackgroundVocals] leaves that
     * one's own split alone.
     */
    private fun result(source: LyricsSource, lines: List<LyricLine>) =
        Result(source, lines.withBackgroundVocals())
}
