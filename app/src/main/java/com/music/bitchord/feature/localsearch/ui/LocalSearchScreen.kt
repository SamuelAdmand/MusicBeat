package com.music.bitchord.feature.localsearch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.localmusic.ui.components.DrillDownSongList
import com.music.bitchord.feature.localsearch.domain.LocalSearchUseCase
import com.music.bitchord.feature.localsearch.domain.model.LocalSearchFilter
import com.music.bitchord.feature.localsearch.domain.model.LocalSearchResult
import com.music.bitchord.feature.localsearch.ui.components.LocalSearchField
import com.music.bitchord.feature.localsearch.ui.components.LocalSearchFilterChips
import com.music.bitchord.feature.localsearch.ui.components.LocalSearchResultRow
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.ROW_DIVIDER_INSET

@Composable
fun LocalSearchScreen(
    songs: List<Song>,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onAlbumClick: ((String, List<Song>, String?) -> Unit)? = null,
    onArtistClick: ((String, List<Song>) -> Unit)? = null,
    onFolderClick: ((String, List<Song>) -> Unit)? = null,
    onShuffle: ((List<Song>) -> Unit)? = null,
    focusTrigger: Int = 0,
    recentSearches: List<String> = emptyList(),
    onRecordSearch: (String) -> Unit = {},
    onRemoveSearch: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(LocalSearchFilter.ALL) }
    val listState = rememberLazyListState()

    var drillDownLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var drillDownSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var drillDownArt by remember { mutableStateOf<String?>(null) }
    var isDrillDownArtist by rememberSaveable { mutableStateOf(false) }

    val inDrillDown = drillDownLabel != null
    val leaveDrillDown = {
        drillDownLabel = null
        drillDownSongs = emptyList()
        drillDownArt = null
        isDrillDownArtist = false
    }

    BackHandler(enabled = inDrillDown) {
        leaveDrillDown()
    }

    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) {
            leaveDrillDown()
        }
    }

    val searchResults = remember(query, selectedFilter, songs) {
        LocalSearchUseCase.search(query, selectedFilter, songs)
    }

    val matchedSongs = remember(searchResults) {
        searchResults.filterIsInstance<LocalSearchResult.Track>().map { it.song }
    }

    AnimatedContent(
        targetState = if (inDrillDown) "drill:$drillDownLabel" else "search",
        transitionSpec = {
            if (targetState.startsWith("drill:")) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "local_search_content",
        modifier = modifier.fillMaxSize(),
    ) { target ->
        if (target.startsWith("drill:")) {
            DrillDownSongList(
                label = drillDownLabel ?: "",
                artworkUrl = drillDownArt,
                songs = drillDownSongs,
                isArtist = isDrillDownArtist,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onSongClick = onSongClick,
                onSongLongPress = onSongLongPress,
                onSongMore = onSongLongPress,
                onSongSwipe = onSongSwipe,
                onShuffle = onShuffle ?: { songsToShuffle ->
                    if (songsToShuffle.isNotEmpty()) {
                        onSongClick(songsToShuffle.shuffled(), 0)
                    }
                },
                onBack = leaveDrillDown,
                contentPadding = contentPadding,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding()),
            ) {
                // Search Input Header
                LocalSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {
                        if (query.isNotBlank()) {
                            onRecordSearch(query.trim())
                        }
                    },
                    focusTrigger = focusTrigger,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                )

                // Filter Chips
                LocalSearchFilterChips(
                    selectedFilter = selectedFilter,
                    onFilterSelect = { selectedFilter = it },
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                // Content
                if (query.isBlank()) {
                    // Recent searches or initial search prompt
                    if (recentSearches.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                bottom = contentPadding.calculateBottomPadding() + 16.dp,
                            ),
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 12.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Recent searches",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    TextButton(onClick = onClearSearchHistory) {
                                        Text(
                                            text = "Clear all",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            items(recentSearches, key = { it }) { term ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            query = term
                                            onRecordSearch(term)
                                        }
                                        .padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = term,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { onRemoveSearch(term) },
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = contentPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Search offline music",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Type a title, artist, album, or folder name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = contentPadding.calculateBottomPadding()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No results found for \"$query\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                val matchedArtists = remember(searchResults) {
                    searchResults.filterIsInstance<LocalSearchResult.Artist>()
                }
                val matchedAlbums = remember(searchResults) {
                    searchResults.filterIsInstance<LocalSearchResult.Album>()
                }
                val matchedTracks = remember(searchResults) {
                    searchResults.filterIsInstance<LocalSearchResult.Track>()
                }
                val matchedFolders = remember(searchResults) {
                    searchResults.filterIsInstance<LocalSearchResult.Folder>()
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    ),
                ) {
                    item {
                        Text(
                            text = "${searchResults.size} results",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                        )
                    }

                    if (selectedFilter == LocalSearchFilter.ALL) {
                        if (matchedArtists.isNotEmpty()) {
                            item(key = "section:artists") {
                                SearchSectionHeader(title = "Artists", count = matchedArtists.size)
                            }
                            items(matchedArtists, key = { "artist:${it.name}" }) { artist ->
                                LocalSearchResultRow(
                                    result = artist,
                                    onSongClick = {},
                                    onSongLongPress = {},
                                    onSongSwipe = {},
                                    onAlbumClick = {},
                                    onArtistClick = {
                                        onRecordSearch(query.trim())
                                        if (onArtistClick != null) {
                                            onArtistClick(artist.name, artist.songs)
                                        } else {
                                            drillDownLabel = artist.name
                                            drillDownSongs = artist.songs
                                            drillDownArt = artist.thumbnailUrl
                                            isDrillDownArtist = true
                                        }
                                    },
                                    onFolderClick = {},
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }

                        if (matchedAlbums.isNotEmpty()) {
                            item(key = "section:albums") {
                                SearchSectionHeader(title = "Albums", count = matchedAlbums.size)
                            }
                            items(matchedAlbums, key = { "album:${it.title}:${it.artist}" }) { album ->
                                LocalSearchResultRow(
                                    result = album,
                                    onSongClick = {},
                                    onSongLongPress = {},
                                    onSongSwipe = {},
                                    onAlbumClick = {
                                        onRecordSearch(query.trim())
                                        if (onAlbumClick != null) {
                                            onAlbumClick(album.title, album.songs, album.thumbnailUrl)
                                        } else {
                                            drillDownLabel = album.title
                                            drillDownSongs = album.songs
                                            drillDownArt = album.thumbnailUrl
                                            isDrillDownArtist = false
                                        }
                                    },
                                    onArtistClick = {},
                                    onFolderClick = {},
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }

                        if (matchedTracks.isNotEmpty()) {
                            item(key = "section:songs") {
                                SearchSectionHeader(title = "Songs", count = matchedTracks.size)
                            }
                            items(matchedTracks, key = { "song:${it.song.videoId}" }) { track ->
                                LocalSearchResultRow(
                                    result = track,
                                    onSongClick = { song ->
                                        onRecordSearch(query.trim())
                                        val index = matchedSongs.indexOf(song).coerceAtLeast(0)
                                        onSongClick(matchedSongs, index)
                                    },
                                    onSongLongPress = onSongLongPress,
                                    onSongSwipe = onSongSwipe,
                                    onAlbumClick = {},
                                    onArtistClick = {},
                                    onFolderClick = {},
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }

                        if (matchedFolders.isNotEmpty()) {
                            item(key = "section:folders") {
                                SearchSectionHeader(title = "Folders", count = matchedFolders.size)
                            }
                            items(matchedFolders, key = { "folder:${it.path}" }) { folder ->
                                LocalSearchResultRow(
                                    result = folder,
                                    onSongClick = {},
                                    onSongLongPress = {},
                                    onSongSwipe = {},
                                    onAlbumClick = {},
                                    onArtistClick = {},
                                    onFolderClick = {
                                        onRecordSearch(query.trim())
                                        if (onFolderClick != null) {
                                            onFolderClick(folder.name, folder.songs)
                                        } else {
                                            drillDownLabel = folder.name
                                            drillDownSongs = folder.songs
                                            drillDownArt = null
                                            isDrillDownArtist = false
                                        }
                                    },
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    } else {
                        items(searchResults, key = { result ->
                            when (result) {
                                is LocalSearchResult.Track -> "song:${result.song.videoId}"
                                is LocalSearchResult.Album -> "album:${result.title}:${result.artist}"
                                is LocalSearchResult.Artist -> "artist:${result.name}"
                                is LocalSearchResult.Folder -> "folder:${result.path}"
                            }
                        }) { result ->
                            LocalSearchResultRow(
                                result = result,
                                onSongClick = { song ->
                                    onRecordSearch(query.trim())
                                    val index = matchedSongs.indexOf(song).coerceAtLeast(0)
                                    onSongClick(matchedSongs, index)
                                },
                                onSongLongPress = onSongLongPress,
                                onSongSwipe = onSongSwipe,
                                onAlbumClick = { album ->
                                    onRecordSearch(query.trim())
                                    if (onAlbumClick != null) {
                                        onAlbumClick(album.title, album.songs, album.thumbnailUrl)
                                    } else {
                                        drillDownLabel = album.title
                                        drillDownSongs = album.songs
                                        drillDownArt = album.thumbnailUrl
                                        isDrillDownArtist = false
                                    }
                                },
                                onArtistClick = { artist ->
                                    onRecordSearch(query.trim())
                                    if (onArtistClick != null) {
                                        onArtistClick(artist.name, artist.songs)
                                    } else {
                                        drillDownLabel = artist.name
                                        drillDownSongs = artist.songs
                                        drillDownArt = artist.thumbnailUrl
                                        isDrillDownArtist = true
                                    }
                                },
                                onFolderClick = { folder ->
                                    onRecordSearch(query.trim())
                                    if (onFolderClick != null) {
                                        onFolderClick(folder.name, folder.songs)
                                    } else {
                                        drillDownLabel = folder.name
                                        drillDownSongs = folder.songs
                                        drillDownArt = null
                                        isDrillDownArtist = false
                                    }
                                },
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
