package com.music.bitchord.feature.localsongactions.domain.model

import kotlinx.serialization.Serializable
import java.text.DateFormat
import java.util.Date

/**
 * Playback statistics for a local song.
 */
@Serializable
data class LocalPlayStats(
    val playedCount: Int = 0,
    val skippedCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
) {
    val playedText: String
        get() = when (playedCount) {
            0 -> "Never"
            1 -> "1 time"
            else -> "$playedCount times"
        }

    val skippedText: String
        get() = when (skippedCount) {
            0 -> "Never"
            1 -> "1 time"
            else -> "$skippedCount times"
        }

    val lastPlayedText: String
        get() = if (lastPlayedTimestamp <= 0L) {
            "Never"
        } else {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastPlayedTimestamp))
        }

    companion object {
        val Empty = LocalPlayStats()
    }
}
