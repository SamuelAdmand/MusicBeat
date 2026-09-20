package com.music.bitchord.feature.lyrics.domain

import java.io.File

/**
 * Representation of an installed lyrics extension.
 */
data class LyricsExtension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val wordSynced: Boolean,
    val defaultPriority: Int,
    val scriptFile: File,
    val isBundled: Boolean = false,
)
