package com.music.bitchord.feature.artistimage.model

import com.music.bitchord.data.model.Song
import java.util.Locale

/**
 * Model representing an individual artist entry aggregated across the library.
 *
 * @property name Canonical artist name.
 * @property songs Distinct list of tracks featuring this artist.
 * @property thumbnailUrl Optional artwork URL fallback from one of the artist's tracks.
 * @property key Unique LazyList / LazyGrid key for this artist.
 */
data class ArtistEntry(
    val name: String,
    val songs: List<Song>,
    val thumbnailUrl: String? = songs.firstNotNullOfOrNull { it.thumbnailUrl },
    val key: String = "artist:${name.lowercase(Locale.ROOT)}",
)
