package com.music.bitchord.feature.localsearch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
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
    onAlbumClick: (String, List<Song>, String?) -> Unit,
    onArtistClick: (String, List<Song>) -> Unit,
    onFolderClick: (String, List<Song>) -> Unit,
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

    val searchResults = remember(query, selectedFilter, songs) {
        LocalSearchUseCase.search(query, selectedFilter, songs)
    }

    val matchedSongs = remember(searchResults) {
        searchResults.filterIsInstance<LocalSearchResult.Track>().map { it.song }
    }

    Column(
        modifier = modifier
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
                            onAlbumClick(album.title, album.songs, album.thumbnailUrl)
                        },
                        onArtistClick = { artist ->
                            onRecordSearch(query.trim())
                            onArtistClick(artist.name, artist.songs)
                        },
                        onFolderClick = { folder ->
                            onRecordSearch(query.trim())
                            onFolderClick(folder.name, folder.songs)
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
