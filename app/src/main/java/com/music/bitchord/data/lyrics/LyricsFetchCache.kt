package com.music.bitchord.data.lyrics

import java.util.concurrent.ConcurrentHashMap

/**
 * Two-tier cache for lyrics lookups, modeled directly after SpotiFLAC Mobile.
 *
 * - Successful lyrics lookups are cached in memory for 24 hours.
 * - Negative lookups (no lyrics found across any provider) are cached for 5 minutes
 *   so that tracks without lyrics do not repeatedly hammer third-party APIs.
 */
object LyricsFetchCache {

    private const val SUCCESS_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val NEGATIVE_TTL_MS = 5 * 60 * 1000L      // 5 minutes
    private const val MAX_CACHE_ENTRIES = 500

    private data class CacheEntry(
        val result: LyricsRepository.Result?,
        val expiresAt: Long,
        val isNegative: Boolean,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private fun cacheKey(title: String, artist: String, durationMs: Long): String {
        val roundedSec = (durationMs / 10000L) * 10L // round to nearest 10s
        return "${artist.lowercase().trim()}|${title.lowercase().trim()}|$roundedSec"
    }

    /**
     * Looks up cached lyrics for a track.
     * Returns a Pair: (foundResult, isNegativeCacheHit)
     */
    fun get(title: String, artist: String, durationMs: Long): Pair<LyricsRepository.Result?, Boolean> {
        val key = cacheKey(title, artist, durationMs)
        val entry = cache[key] ?: return Pair(null, false)
        val now = System.currentTimeMillis()

        if (now > entry.expiresAt) {
            cache.remove(key)
            return Pair(null, false)
        }

        if (entry.isNegative) {
            return Pair(null, true)
        }

        return Pair(entry.result, false)
    }

    /**
     * Stores a successful lyrics result in cache for 24 hours.
     */
    fun putSuccess(title: String, artist: String, durationMs: Long, result: LyricsRepository.Result) {
        evictIfFull()
        val key = cacheKey(title, artist, durationMs)
        cache[key] = CacheEntry(
            result = result,
            expiresAt = System.currentTimeMillis() + SUCCESS_TTL_MS,
            isNegative = false,
        )
    }

    /**
     * Stores a negative result in cache for 5 minutes.
     */
    fun putNegative(title: String, artist: String, durationMs: Long) {
        evictIfFull()
        val key = cacheKey(title, artist, durationMs)
        cache[key] = CacheEntry(
            result = null,
            expiresAt = System.currentTimeMillis() + NEGATIVE_TTL_MS,
            isNegative = true,
        )
    }

    /**
     * Clears all cached entries.
     */
    fun clearAll() {
        cache.clear()
    }

    private fun evictIfFull() {
        if (cache.size >= MAX_CACHE_ENTRIES) {
            val now = System.currentTimeMillis()
            val expired = cache.filter { it.value.expiresAt < now }.keys
            expired.forEach { cache.remove(it) }

            if (cache.size >= MAX_CACHE_ENTRIES) {
                // Evict oldest 50 entries
                val oldestKeys = cache.entries.sortedBy { it.value.expiresAt }.take(50).map { it.key }
                oldestKeys.forEach { cache.remove(it) }
            }
        }
    }
}
