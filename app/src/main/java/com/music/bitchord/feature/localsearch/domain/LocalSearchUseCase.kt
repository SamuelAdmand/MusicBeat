package com.music.bitchord.feature.localsearch.domain

import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.artistimage.util.ArtistSplitter
import com.music.bitchord.feature.localsearch.domain.model.LocalSearchFilter
import com.music.bitchord.feature.localsearch.domain.model.LocalSearchResult
import java.util.Locale

object LocalSearchUseCase {
    fun search(
        query: String,
        filter: LocalSearchFilter,
        allSongs: List<Song>,
    ): List<LocalSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val q = trimmed.lowercase(Locale.ROOT)

        val results = mutableListOf<LocalSearchResult>()

        // Find matching songs
        val matchedSongs = if (filter == LocalSearchFilter.ALL || filter == LocalSearchFilter.SONGS) {
            allSongs.filter { song ->
                song.title.lowercase(Locale.ROOT).contains(q) ||
                    song.artist.lowercase(Locale.ROOT).contains(q) ||
                    song.albumName?.lowercase(Locale.ROOT)?.contains(q) == true
            }
        } else {
            emptyList()
        }

        // Find matching albums
        val matchedAlbums = if (filter == LocalSearchFilter.ALL || filter == LocalSearchFilter.ALBUMS) {
            allSongs
                .filter { !it.albumName.isNullOrBlank() }
                .groupBy { it.albumName.orEmpty() }
                .filter { (albumName, songs) ->
                    albumName.lowercase(Locale.ROOT).contains(q) ||
                        songs.any { it.artist.lowercase(Locale.ROOT).contains(q) }
                }
                .map { (albumName, songs) ->
                    LocalSearchResult.Album(
                        title = albumName,
                        artist = songs.firstOrNull()?.artist.orEmpty(),
                        thumbnailUrl = songs.firstNotNullOfOrNull { it.thumbnailUrl },
                        songs = songs,
                    )
                }
        } else {
            emptyList()
        }

        // Find matching artists
        val matchedArtists = if (filter == LocalSearchFilter.ALL || filter == LocalSearchFilter.ARTISTS) {
            ArtistSplitter.groupSongsByArtist(allSongs)
                .filter { it.name.lowercase(Locale.ROOT).contains(q) }
                .sortedWith(
                    compareByDescending<com.music.bitchord.feature.artistimage.model.ArtistEntry> {
                        it.name.lowercase(Locale.ROOT).startsWith(q)
                    }.thenByDescending { it.songs.size }
                )
                .map { entry ->
                    LocalSearchResult.Artist(
                        name = entry.name,
                        songs = entry.songs,
                        thumbnailUrl = entry.thumbnailUrl,
                    )
                }
        } else {
            emptyList()
        }

        // Find matching folders
        val matchedFolders = if (filter == LocalSearchFilter.ALL || filter == LocalSearchFilter.FOLDERS) {
            allSongs
                .filter { !it.localPath.isNullOrBlank() }
                .groupBy { song ->
                    val path = song.localPath.orEmpty()
                    path.substringBeforeLast('/', "")
                }
                .filter { (dir, _) ->
                    val folderName = dir.substringAfterLast('/')
                    folderName.lowercase(Locale.ROOT).contains(q) || dir.lowercase(Locale.ROOT).contains(q)
                }
                .map { (dir, songs) ->
                    LocalSearchResult.Folder(
                        name = dir.substringAfterLast('/'),
                        path = dir,
                        songs = songs,
                    )
                }
                .sortedWith(
                    compareByDescending<LocalSearchResult.Folder> { it.name.lowercase(Locale.ROOT).startsWith(q) }
                        .thenByDescending { it.songs.size }
                )
        } else {
            emptyList()
        }

        when (filter) {
            LocalSearchFilter.ALL -> {
                // Artists and Albums must precede individual tracks so users see the top artist/album results first
                results.addAll(matchedArtists)
                results.addAll(matchedAlbums)
                results.addAll(matchedSongs.map { LocalSearchResult.Track(it) })
                results.addAll(matchedFolders)
            }
            LocalSearchFilter.SONGS -> results.addAll(matchedSongs.map { LocalSearchResult.Track(it) })
            LocalSearchFilter.ALBUMS -> results.addAll(matchedAlbums)
            LocalSearchFilter.ARTISTS -> results.addAll(matchedArtists)
            LocalSearchFilter.FOLDERS -> results.addAll(matchedFolders)
        }

        return results
    }
}
