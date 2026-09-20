package com.music.bitchord.feature.lyrics.domain

import kotlinx.serialization.Serializable

@Serializable
data class RemoteLyricsRegistry(
    val version: Int = 1,
    val updated_at: String = "",
    val extensions: List<RemoteLyricsExtensionItem> = emptyList(),
)

@Serializable
data class RemoteLyricsExtensionItem(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "MusicBeat Community",
    val wordSynced: Boolean = false,
    val defaultPriority: Int = 99,
    val manifest_url: String? = null,
    val script_url: String? = null,
)

@Serializable
data class LyricsExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "MusicBeat Community",
    val wordSynced: Boolean = false,
    val defaultPriority: Int = 99,
)
