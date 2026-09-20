package com.music.bitchord.data.lyrics

import com.music.bitchord.feature.lyrics.domain.LyricsExtension

/**
 * Dynamic representation of an available lyrics source (extension).
 * Loaded dynamically from registry.json and installed extensions.
 *
 * Nothing is hardcoded; all providers are loaded from the extensions registry.
 */
data class LyricsSource(
    val id: String,
    val label: String,
    val detail: String = "",
    val wordSynced: Boolean = false,
) {
    /** Alias for backward compatibility */
    val extensionId: String get() = id

    /** Name alias for backward compatibility */
    val name: String get() = id

    companion object {
        fun fromExtension(ext: LyricsExtension): LyricsSource =
            LyricsSource(
                id = ext.id,
                label = ext.name,
                detail = ext.description,
                wordSynced = ext.wordSynced,
            )
    }
}
