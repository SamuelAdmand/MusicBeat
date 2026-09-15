package com.music.bitchord.feature.localmusic.domain.model

/**
 * Represents a filesystem directory containing audio files.
 *
 * @property path Absolute filesystem directory path (e.g. "/storage/emulated/0/Music/Rock")
 * @property name Folder display name (e.g. "Rock")
 * @property parentPath Path of the parent directory, or null if root
 * @property songCount Number of audio tracks in this directory
 * @property isBlacklisted Whether this folder is excluded from library scanning
 */
data class LocalFolder(
    val path: String,
    val name: String,
    val parentPath: String? = null,
    val songCount: Int = 0,
    val isBlacklisted: Boolean = false,
)
