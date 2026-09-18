package com.music.bitchord.feature.localsongactions.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.feature.localsongactions.data.LocalSongMetadataRetriever
import com.music.bitchord.feature.localsongactions.domain.model.LocalSongFullMetadata

/**
 * Modal bottom sheet displaying rich metadata, play statistics, and file details
 * for a local song, matching Screenshot 1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSongDetailsSheet(
    song: Song,
    onDismissRequest: () -> Unit,
    onLyricsEditorClick: () -> Unit,
    onTagEditorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var metadata by remember { mutableStateOf<LocalSongFullMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(song) {
        isLoading = true
        metadata = LocalSongMetadataRetriever.retrieve(context, song)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DetailsHeader(
                    song = song,
                    metadata = metadata,
                    isLoading = isLoading,
                )
            }

            item {
                DetailsActionButtons(
                    onLyricsEditorClick = onLyricsEditorClick,
                    onTagEditorClick = onTagEditorClick,
                )
            }

            item {
                MetadataCard(metadata = metadata)
            }

            if (metadata != null) {
                item {
                    FileInfoCard(metadata = metadata!!)
                }
            }
        }
    }
}

@Composable
private fun DetailsHeader(
    song: Song,
    metadata: LocalSongFullMetadata?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(256),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata?.title ?: song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = metadata?.artist ?: song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailsActionButtons(
    onLyricsEditorClick: () -> Unit,
    onTagEditorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            onClick = onLyricsEditorClick,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Notes,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Lyrics editor",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }

        Button(
            onClick = onTagEditorClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Tag editor",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MetadataCard(
    metadata: LocalSongFullMetadata?,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                icon = Icons.Rounded.Info,
                title = "Metadata",
            )

            metadata?.album?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Album", value = it)
            }
            metadata?.albumArtist?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Artist", value = it)
            } ?: metadata?.artist?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Artist", value = it)
            }
            metadata?.trackNumber?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Track", value = it)
            }
            metadata?.discNumber?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Disc", value = it)
            }
            metadata?.publisher?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Publisher", value = it)
            }
            metadata?.genre?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Genre", value = it)
            }
            metadata?.year?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Year", value = it)
            }
            metadata?.composer?.takeIf { it.isNotBlank() }?.let {
                KeyValueRow(key = "Composer", value = it)
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileInfoCard(
    metadata: LocalSongFullMetadata,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                icon = Icons.Rounded.AudioFile,
                title = "File",
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                metadata.format?.takeIf { it.isNotBlank() }?.let {
                    BadgeChip(text = it, containerColor = MaterialTheme.colorScheme.primaryContainer)
                }
                if (metadata.isLossless) {
                    BadgeChip(text = "Lossless", containerColor = MaterialTheme.colorScheme.secondaryContainer)
                }
                metadata.bitrate?.takeIf { it.isNotBlank() }?.let {
                    BadgeChip(text = it, containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                }
                metadata.sampleRate?.takeIf { it.isNotBlank() }?.let {
                    BadgeChip(text = it, containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }

            metadata.durationText?.let {
                KeyValueRow(key = "Length", value = it)
            }
            metadata.fileSize?.let {
                KeyValueRow(key = "Size", value = it)
            }
            metadata.channels?.let {
                KeyValueRow(key = "Channels", value = it)
            }
            metadata.filePath?.let {
                KeyValueRow(key = "File path", value = it)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun KeyValueRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.65f),
        )
    }
}

@Composable
private fun BadgeChip(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
