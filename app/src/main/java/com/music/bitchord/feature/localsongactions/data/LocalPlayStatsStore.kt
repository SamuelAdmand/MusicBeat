package com.music.bitchord.feature.localsongactions.data

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.feature.localsongactions.domain.model.LocalPlayStats

/**
 * Manages local song playback and skip statistics.
 */
object LocalPlayStatsStore {

    private const val PREFS_NAME = "bitchord_local_play_stats"
    private const val KEY_PLAY_COUNT = "play_"
    private const val KEY_SKIP_COUNT = "skip_"
    private const val KEY_LAST_PLAYED = "last_"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStats(context: Context, songId: String): LocalPlayStats {
        val prefs = getPrefs(context)
        val playCount = prefs.getInt(KEY_PLAY_COUNT + songId, 0)
        val skipCount = prefs.getInt(KEY_SKIP_COUNT + songId, 0)
        val lastPlayed = prefs.getLong(KEY_LAST_PLAYED + songId, 0L)
        return LocalPlayStats(
            playedCount = playCount,
            skippedCount = skipCount,
            lastPlayedTimestamp = lastPlayed,
        )
    }

    fun recordPlay(context: Context, songId: String): LocalPlayStats {
        val prefs = getPrefs(context)
        val currentPlay = prefs.getInt(KEY_PLAY_COUNT + songId, 0) + 1
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_PLAY_COUNT + songId, currentPlay)
            .putLong(KEY_LAST_PLAYED + songId, now)
            .apply()
        val skipCount = prefs.getInt(KEY_SKIP_COUNT + songId, 0)
        return LocalPlayStats(currentPlay, skipCount, now)
    }

    fun recordSkip(context: Context, songId: String): LocalPlayStats {
        val prefs = getPrefs(context)
        val currentSkip = prefs.getInt(KEY_SKIP_COUNT + songId, 0) + 1
        val lastPlayed = prefs.getLong(KEY_LAST_PLAYED + songId, 0L)
        prefs.edit()
            .putInt(KEY_SKIP_COUNT + songId, currentSkip)
            .apply()
        val playCount = prefs.getInt(KEY_PLAY_COUNT + songId, 0)
        return LocalPlayStats(playCount, currentSkip, lastPlayed)
    }

    fun resetStats(context: Context, songId: String): LocalPlayStats {
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_PLAY_COUNT + songId)
            .remove(KEY_SKIP_COUNT + songId)
            .remove(KEY_LAST_PLAYED + songId)
            .apply()
        return LocalPlayStats.Empty
    }

    fun exportStats(context: Context): Map<String, LocalPlayStats> {
        val prefs = getPrefs(context)
        val all = prefs.all
        val songIds = mutableSetOf<String>()
        all.keys.forEach { key ->
            when {
                key.startsWith(KEY_PLAY_COUNT) -> songIds.add(key.removePrefix(KEY_PLAY_COUNT))
                key.startsWith(KEY_SKIP_COUNT) -> songIds.add(key.removePrefix(KEY_SKIP_COUNT))
                key.startsWith(KEY_LAST_PLAYED) -> songIds.add(key.removePrefix(KEY_LAST_PLAYED))
            }
        }
        return songIds.associateWith { songId ->
            getStats(context, songId)
        }
    }

    fun importStats(context: Context, incoming: Map<String, LocalPlayStats>) {
        if (incoming.isEmpty()) return
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        incoming.forEach { (songId, stats) ->
            if (stats.playedCount > 0) editor.putInt(KEY_PLAY_COUNT + songId, stats.playedCount)
            if (stats.skippedCount > 0) editor.putInt(KEY_SKIP_COUNT + songId, stats.skippedCount)
            if (stats.lastPlayedTimestamp > 0L) editor.putLong(KEY_LAST_PLAYED + songId, stats.lastPlayedTimestamp)
        }
        editor.apply()
    }
}
