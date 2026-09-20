package com.music.bitchord.feature.localmusic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.model.isSameTrackAs
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.ui.components.FastScroller
import com.music.bitchord.ui.components.SectionIndexer
import com.music.bitchord.feature.artistimage.model.ArtistImage
import com.music.bitchord.ui.components.ExplicitSongTitle
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.ROW_DIVIDER_INSET
import com.music.bitchord.ui.components.SongRow
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.icons.BitChordIcons

@Composable
fun DrillDownHeader(
    label: String,
    artworkUrl: String?,
    songs: List<Song>,
    isArtist: Boolean = false,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 8.dp, bottom = 12.dp),
    ) {
        val shape = if (isArtist) CircleShape else RoundedCornerShape(16.dp)
        val isFavorites = label.equals("Favorites", ignoreCase = true) || artworkUrl == "favorites"

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(188.dp)
                .clip(shape)
                .background(
                    if (isFavorites) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE91E63),
                                Color(0xFF8E24AA),
                                Color(0xFF3F51B5),
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isFavorites) {
                Icon(
                    imageVector = BitChordIcons.HeartFilled,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(76.dp),
                )
            } else {
                Icon(
                    imageVector = if (isArtist) Icons.Rounded.Person else Icons.Rounded.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(64.dp),
                )
                val imageModel: Any? = if (isArtist) {
                    remember(label, artworkUrl) {
                        ArtistImage(
                            name = label,
                            fallbackUrl = artworkUrl ?: songs.firstNotNullOfOrNull { it.thumbnailUrl },
                            isLarge = true,
                        )
                    }
                } else {
                    (artworkUrl ?: songs.firstNotNullOfOrNull { it.thumbnailUrl })?.artworkAt(CARD_ART_PX)
                }

                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .then(if (isArtist) Modifier.thumbnailBorder(shape) else Modifier),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildString {
                if (isArtist) append("Artist · ")
                append(pluralStringResource(R.plurals.track_count_plural, songs.size, songs.size))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun DrillDownActionRow(
    songs: List<Song>,
    viewType: LibraryViewType? = null,
    onViewTypeToggle: (() -> Unit)? = null,
    onSongClick: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val buttonBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val buttonBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val buttonContentColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(buttonBackground)
                .border(0.5.dp, buttonBorder, CircleShape)
                .clickable { if (songs.isNotEmpty()) onShuffle(songs) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                BitChordIcons.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = buttonContentColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Row(
            modifier = Modifier
                .height(50.dp)
                .clip(CircleShape)
                .background(buttonBackground)
                .border(0.5.dp, buttonBorder, CircleShape)
                .clickable { if (songs.isNotEmpty()) onSongClick(songs, 0) }
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                BitChordIcons.Play,
                contentDescription = null,
                tint = buttonContentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.play),
                style = MaterialTheme.typography.titleMedium,
                color = buttonContentColor,
            )
        }
        if (onViewTypeToggle != null && viewType != null) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(buttonBackground)
                    .border(0.5.dp, buttonBorder, CircleShape)
                    .clickable(onClick = onViewTypeToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (viewType == LibraryViewType.GRID) BitChordIcons.ListView else BitChordIcons.GridView,
                    contentDescription = stringResource(
                        if (viewType == LibraryViewType.GRID) R.string.switch_to_list_view else R.string.switch_to_grid_view,
                    ),
                    tint = buttonContentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (onMore != null) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(buttonBackground)
                    .border(0.5.dp, buttonBorder, CircleShape)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = stringResource(R.string.more),
                    tint = buttonContentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongGridCard(
    song: Song,
    selected: Boolean = false,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(4.dp),
    ) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .thumbnailBorder(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
            if (song.thumbnailUrl != null) {
                AsyncImage(
                    model = song.thumbnailUrl.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (isCurrent) {
                Icon(
                    Icons.Rounded.GraphicEq,
                    contentDescription = stringResource(R.string.now_playing),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ExplicitSongTitle(
            song = song,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = song.artist.ifBlank { stringResource(R.string.unknown_artist) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DrillDownSongList(
    label: String,
    artworkUrl: String?,
    songs: List<Song>,
    isArtist: Boolean = false,
    viewType: LibraryViewType = LibraryViewType.LIST,
    onViewTypeToggle: (() -> Unit)? = null,
    selectedIds: Set<String> = emptySet(),
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongMore: ((Song) -> Unit)? = null,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onMore: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    songDropdownMenu: (@Composable (Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (viewType == LibraryViewType.GRID) {
        val gridState = rememberLazyGridState()
        Box(modifier = modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = PAGE_GUTTER,
                    end = PAGE_GUTTER,
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DrillDownHeader(
                        label = label,
                        artworkUrl = artworkUrl,
                        songs = songs,
                        isArtist = isArtist,
                        onBack = onBack,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DrillDownActionRow(
                        songs = songs,
                        viewType = viewType,
                        onViewTypeToggle = onViewTypeToggle,
                        onSongClick = onSongClick,
                        onShuffle = onShuffle,
                        onMore = onMore,
                    )
                }
                itemsIndexed(songs) { index, song ->
                    SongGridCard(
                        song = song,
                        selected = song.videoId in selectedIds,
                        isCurrent = song.isSameTrackAs(currentSong),
                        onClick = { onSongClick(songs, index) },
                        onLongPress = { onSongLongPress(song) },
                    )
                }
            }
            FastScroller(
                gridState = gridState,
                itemCount = songs.size,
                headerCount = 2,
                sectionNameForIndex = { index -> SectionIndexer.getSectionName(songs[index].title) },
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 8.dp,
                ),
            )
        }
    } else {
        val listState = rememberLazyListState()
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                item {
                    DrillDownHeader(
                        label = label,
                        artworkUrl = artworkUrl,
                        songs = songs,
                        isArtist = isArtist,
                        onBack = onBack,
                    )
                }
                item {
                    DrillDownActionRow(
                        songs = songs,
                        viewType = viewType,
                        onViewTypeToggle = onViewTypeToggle,
                        onSongClick = onSongClick,
                        onShuffle = onShuffle,
                        onMore = onMore,
                    )
                }
                itemsIndexed(songs) { index, song ->
                    SongRow(
                        song = song,
                        selected = song.videoId in selectedIds,
                        isCurrent = song.isSameTrackAs(currentSong),
                        isPlaying = song.isSameTrackAs(currentSong) && isPlaying,
                        trackNumber = index + 1,
                        onClick = { onSongClick(songs, index) },
                        onLongPress = { onSongLongPress(song) },
                        onMore = onSongMore?.let { more -> { more(song) } },
                        onSwipeToQueue = { onSongSwipe(song) },
                        dropdownMenu = songDropdownMenu?.let { menu -> { menu(song) } },
                    )
                    if (index < songs.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
            FastScroller(
                listState = listState,
                itemCount = songs.size,
                headerCount = 2,
                sectionNameForIndex = { index -> SectionIndexer.getSectionName(songs[index].title) },
                contentPadding = contentPadding,
            )
        }
    }
}
