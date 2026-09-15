package com.music.bitchord.feature.tageditor.domain.model

import android.graphics.Bitmap

/**
 * Encapsulates all editable audio tag metadata and artwork for a track.
 */
data class SongTagData(
    val title: String = "",
    val album: String = "",
    val artist: String = "",
    val albumArtist: String = "",
    val composer: String = "",
    val conductor: String = "",
    val publisher: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val trackTotal: String = "",
    val discNumber: String = "",
    val discTotal: String = "",
    val lyrics: String = "",
    val lyricist: String = "",
    val arranger: String = "",
    val comment: String = "",
    val artworkBitmap: Bitmap? = null,
    val artworkChanged: Boolean = false,
    val artworkDeleted: Boolean = false,
)
