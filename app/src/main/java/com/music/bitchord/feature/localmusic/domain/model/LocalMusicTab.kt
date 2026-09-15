package com.music.bitchord.feature.localmusic.domain.model

/**
 * Top-level tabs available in the Local Music library view.
 */
enum class LocalMusicTab(val index: Int) {
    TRACKS(0),
    ALBUMS(1),
    ARTISTS(2),
    FOLDERS(3),
    PLAYLISTS(4);

    companion object {
        fun fromIndex(index: Int): LocalMusicTab =
            entries.firstOrNull { it.index == index } ?: TRACKS
    }
}
