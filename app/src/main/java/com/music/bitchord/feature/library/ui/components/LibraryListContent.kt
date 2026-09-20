package com.music.bitchord.feature.library.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.bitchord.feature.localmusic.domain.model.LocalPlaylist
import com.music.bitchord.feature.localmusic.ui.components.LocalPlaylistItem

/**
 * List layout presentation of the Library featuring Favorites and Playlists.
 */
@Composable
fun LibraryListContent(
    filter: LibraryFilter,
    favoriteSongCount: Int,
    playlists: List<LocalPlaylist>,
    onFavoritesClick: () -> Unit,
    onFavoritesPlay: () -> Unit,
    onPlaylistClick: (LocalPlaylist) -> Unit,
    onPlaylistPlay: (LocalPlaylist) -> Unit,
    onRenamePlaylist: (LocalPlaylist) -> Unit,
    onDeletePlaylist: (LocalPlaylist) -> Unit,
    onCreatePlaylist: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val showFavorites = filter == LibraryFilter.ALL || filter == LibraryFilter.FAVORITES
    val showPlaylists = filter == LibraryFilter.ALL || filter == LibraryFilter.PLAYLISTS

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "library_list_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val totalCount = (if (showFavorites) 1 else 0) + (if (showPlaylists) playlists.size else 0)
                Text(
                    text = "$totalCount ${if (totalCount == 1) "Collection" else "Collections"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (showPlaylists) {
                    OutlinedButton(
                        onClick = onCreatePlaylist,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "New Playlist", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (showFavorites) {
            item(key = "library_list_favorites") {
                LibraryFavoritesItem(
                    songCount = favoriteSongCount,
                    onClick = onFavoritesClick,
                    onPlay = onFavoritesPlay,
                )
            }
        }

        if (showPlaylists) {
            items(playlists, key = { it.id }) { playlist ->
                LocalPlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onPlay = { onPlaylistPlay(playlist) },
                    onRename = { onRenamePlaylist(playlist) },
                    onDelete = { onDeletePlaylist(playlist) },
                )
            }
        }
    }
}
