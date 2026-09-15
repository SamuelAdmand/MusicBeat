package com.music.bitchord.feature.lyricseditor.domain.model

data class LyricsSearchResultItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Int = 0,
    val provider: String,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
) {
    val hasSynced: Boolean get() = !syncedLyrics.isNullOrBlank()
    val hasPlain: Boolean get() = !plainLyrics.isNullOrBlank()

    val formattedDuration: String get() {
        if (durationSeconds <= 0) return ""
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    val formatBadge: String get() = when {
        hasSynced && hasPlain -> "Synced & Plain"
        hasSynced -> "Synced (LRC)"
        hasPlain -> "Plain Text"
        else -> "Lyrics"
    }
}
