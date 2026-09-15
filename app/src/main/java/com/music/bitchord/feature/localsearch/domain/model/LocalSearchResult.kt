package com.music.bitchord.feature.localsearch.domain.model

import com.music.bitchord.data.model.Song

sealed interface LocalSearchResult {
    data class Track(val song: Song) : LocalSearchResult
    data class Album(
        val title: String,
        val artist: String,
        val thumbnailUrl: String?,
        val songs: List<Song>,
    ) : LocalSearchResult
    data class Artist(
        val name: String,
        val songs: List<Song>,
        val thumbnailUrl: String?,
    ) : LocalSearchResult
    data class Folder(
        val name: String,
        val path: String,
        val songs: List<Song>,
    ) : LocalSearchResult
}
