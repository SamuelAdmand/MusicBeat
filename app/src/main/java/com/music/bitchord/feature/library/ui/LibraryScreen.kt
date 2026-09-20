package com.music.bitchord.feature.library.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.feature.library.ui.components.LibraryFilter
import com.music.bitchord.feature.library.ui.components.LibraryGridContent
import com.music.bitchord.feature.library.ui.components.LibraryHeaderBar
import com.music.bitchord.feature.library.ui.components.LibraryListContent
import com.music.bitchord.feature.localmusic.data.LocalFavoritesStore
import com.music.bitchord.feature.localmusic.data.LocalPlaylistStore
import com.music.bitchord.feature.localmusic.domain.model.LocalPlaylist
import com.music.bitchord.feature.localmusic.ui.components.CreatePlaylistDialog
import com.music.bitchord.feature.localmusic.ui.components.DrillDownSongList

/**
 * Modern Library screen unifying user Favorites and Playlists with list/grid toggle.
 */
@Composable
fun LibraryScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onDeleteSong: ((Song) -> Unit)? = null,
    onSongTagsOrLyricsSaved: ((Song) -> Unit)? = null,
    songDropdownMenu: (@Composable (Song) -> Unit)? = null,
) {
    val playlists by LocalPlaylistStore.playlists.collectAsStateWithLifecycle()
    val favoriteIds by LocalFavoritesStore.favoriteIds.collectAsStateWithLifecycle()
    val libraryViewType by AppSettings.libraryPlaylistsViewType.collectAsStateWithLifecycle()
    val songsViewType by AppSettings.librarySongsViewType.collectAsStateWithLifecycle()

    var selectedFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<LocalPlaylist?>(null) }

    // Drill down navigation inside Library (e.g. for Favorites or specific Playlist)
    var drillDownLabel by remember { mutableStateOf<String?>(null) }
    var drillDownSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var drillDownArt by remember { mutableStateOf<String?>(null) }

    val inDrillDown = drillDownLabel != null
    val leaveDrillDown = {
        drillDownLabel = null
        drillDownSongs = emptyList()
        drillDownArt = null
    }

    BackHandler(enabled = inDrillDown) {
        leaveDrillDown()
    }

    // Filter favorite songs matching persisted favorite IDs
    val favoriteSongs = remember(songs, favoriteIds) {
        songs.filter { s ->
            val localKey = s.localUri ?: s.videoId
            localKey in favoriteIds || s.videoId in favoriteIds
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = if (inDrillDown) "drill:$drillDownLabel" else "library_main",
            transitionSpec = {
                if (targetState.startsWith("drill:")) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "library_content_transition",
            modifier = Modifier.fillMaxSize(),
        ) { key ->
            if (key.startsWith("drill:")) {
                DrillDownSongList(
                    label = drillDownLabel.orEmpty(),
                    artworkUrl = drillDownArt,
                    songs = drillDownSongs,
                    isArtist = false,
                    viewType = songsViewType,
                    onViewTypeToggle = {
                        val next = if (songsViewType == LibraryViewType.GRID) LibraryViewType.LIST else LibraryViewType.GRID
                        AppSettings.setLibrarySongsViewType(next)
                    },
                    selectedIds = emptySet(),
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onSongClick = { list, idx -> onSongClick(list, idx) },
                    onSongLongPress = onSongLongPress,
                    onSongMore = onSongLongPress,
                    onSongSwipe = onSongSwipe,
                    onShuffle = onShuffle,
                    onMore = null,
                    onBack = leaveDrillDown,
                    contentPadding = contentPadding,
                    songDropdownMenu = songDropdownMenu,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = contentPadding.calculateTopPadding()),
                ) {
                    LibraryHeaderBar(
                        selectedFilter = selectedFilter,
                        onFilterSelect = { selectedFilter = it },
                        viewType = libraryViewType,
                        onToggleViewType = {
                            val next = if (libraryViewType == LibraryViewType.GRID) LibraryViewType.LIST else LibraryViewType.GRID
                            AppSettings.setLibraryPlaylistsViewType(next)
                        },
                        onCreatePlaylist = { showCreateDialog = true },
                    )

                    val onFavoritesClick = {
                        drillDownLabel = "Favorites"
                        drillDownSongs = favoriteSongs
                        drillDownArt = "favorites"
                    }

                    val onFavoritesPlay = {
                        if (favoriteSongs.isNotEmpty()) {
                            onSongClick(favoriteSongs, 0)
                        }
                    }

                    val onPlaylistClick: (LocalPlaylist) -> Unit = { playlist ->
                        drillDownLabel = playlist.name
                        val pSongs = playlist.songIds.mapNotNull { id ->
                            songs.find { s -> s.localUri == id || s.videoId == id }
                        }
                        drillDownSongs = pSongs
                        drillDownArt = playlist.coverUrl ?: pSongs.firstNotNullOfOrNull { it.thumbnailUrl }
                    }

                    val onPlaylistPlay: (LocalPlaylist) -> Unit = { playlist ->
                        val pSongs = playlist.songIds.mapNotNull { id ->
                            songs.find { s -> s.localUri == id || s.videoId == id }
                        }
                        if (pSongs.isNotEmpty()) {
                            onSongClick(pSongs, 0)
                        }
                    }

                    if (libraryViewType == LibraryViewType.GRID) {
                        LibraryGridContent(
                            filter = selectedFilter,
                            favoriteSongCount = favoriteSongs.size,
                            playlists = playlists,
                            onFavoritesClick = onFavoritesClick,
                            onFavoritesPlay = onFavoritesPlay,
                            onPlaylistClick = onPlaylistClick,
                            onPlaylistPlay = onPlaylistPlay,
                            onRenamePlaylist = { playlistToRename = it },
                            onDeletePlaylist = { LocalPlaylistStore.deletePlaylist(it.id) },
                            onCreatePlaylist = { showCreateDialog = true },
                            contentPadding = PaddingValues(
                                top = 0.dp,
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        )
                    } else {
                        LibraryListContent(
                            filter = selectedFilter,
                            favoriteSongCount = favoriteSongs.size,
                            playlists = playlists,
                            onFavoritesClick = onFavoritesClick,
                            onFavoritesPlay = onFavoritesPlay,
                            onPlaylistClick = onPlaylistClick,
                            onPlaylistPlay = onPlaylistPlay,
                            onRenamePlaylist = { playlistToRename = it },
                            onDeletePlaylist = { LocalPlaylistStore.deletePlaylist(it.id) },
                            onCreatePlaylist = { showCreateDialog = true },
                            contentPadding = PaddingValues(
                                top = 0.dp,
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            title = "New Playlist",
            confirmText = "Create",
            onConfirm = { name ->
                LocalPlaylistStore.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    playlistToRename?.let { target ->
        CreatePlaylistDialog(
            initialName = target.name,
            title = "Rename Playlist",
            confirmText = "Save",
            onConfirm = { newName ->
                LocalPlaylistStore.renamePlaylist(target.id, newName)
                playlistToRename = null
            },
            onDismiss = { playlistToRename = null },
        )
    }
}
