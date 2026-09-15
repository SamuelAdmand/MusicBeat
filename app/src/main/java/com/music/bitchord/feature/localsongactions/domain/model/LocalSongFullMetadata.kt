package com.music.bitchord.feature.localsongactions.domain.model

/**
 * Rich metadata representation for a local audio file, mirroring the tags
 * and audio header properties displayed in the Details sheet.
 */
data class LocalSongFullMetadata(
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val publisher: String? = null,
    val composer: String? = null,
    val lyricist: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val durationText: String? = null,
    val fileSize: String? = null,
    val filePath: String? = null,
    val format: String? = null,
    val bitrate: String? = null,
    val sampleRate: String? = null,
    val channels: String? = null,
    val isLossless: Boolean = false,
    val lyrics: String? = null,
) {
    val isMissingMetadata: Boolean
        get() = album.isNullOrBlank() && trackNumber.isNullOrBlank() && publisher.isNullOrBlank() && composer.isNullOrBlank()

    companion object {
        val Empty = LocalSongFullMetadata(
            title = "",
            artist = "",
        )
    }
}
