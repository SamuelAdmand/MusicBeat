package com.music.bitchord.feature.localsongactions.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.feature.localmusic.data.LocalFavoritesStore
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.icons.BitChordIcons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

/**
 * Modal bottom sheet presenting primary actions for a local song:
 * 1. Play again
 * 2. Add to playlist
 * 3. Edit lyrics
 * 4. Tag editor
 * 5. Share
 * 6. Details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSongActionsSheet(
    song: Song,
    onPlayAgain: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditLyrics: () -> Unit,
    onTagEditor: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            LocalSongActionsHeader(song = song)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            val favoriteIds by LocalFavoritesStore.favoriteIds.collectAsStateWithLifecycle()
            val isFav = (song.localUri?.toString() in favoriteIds) || (song.videoId in favoriteIds)

            LocalSongActionItem(
                icon = Icons.Rounded.Replay,
                label = "Play again",
                onClick = onPlayAgain,
            )

            LocalSongActionItem(
                icon = if (isFav) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                label = if (isFav) "Remove from favorites" else "Add to favorites",
                onClick = {
                    LocalFavoritesStore.toggleFavorite(song.localUri?.toString() ?: song.videoId)
                    onDismissRequest()
                },
            )

            LocalSongActionItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = "Add to playlist",
                onClick = onAddToPlaylist,
            )

            LocalSongActionItem(
                icon = Icons.AutoMirrored.Rounded.Notes,
                label = "Edit lyrics",
                onClick = onEditLyrics,
            )

            LocalSongActionItem(
                icon = Icons.Rounded.Edit,
                label = "Tag editor",
                onClick = onTagEditor,
            )

            LocalSongActionItem(
                icon = Icons.Rounded.Share,
                label = "Share",
                onClick = onShare,
            )

            LocalSongActionItem(
                icon = Icons.Rounded.Info,
                label = "Details",
                onClick = onDetails,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Track header showing artwork, title, and artist/album subtitle.
 */
@Composable
private fun LocalSongActionsHeader(
    song: Song,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .thumbnailBorder(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = buildString {
                append(song.artist)
                song.albumName?.takeIf { it.isNotBlank() }?.let {
                    append(" • ")
                    append(it)
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Individual action item in the bottom sheet.
 */
@Composable
private fun LocalSongActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
