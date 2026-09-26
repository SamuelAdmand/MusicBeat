package com.music.bitchord.feature.localsongactions.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.SleepTimer
import kotlinx.coroutines.delay
import com.music.bitchord.feature.artistimage.util.ArtistSplitter
import java.util.Locale

/**
 * Modal bottom sheet presenting primary actions for a local song:
 * 1. Go to Album
 * 2. Go to Artist
 * 3. Add to playlist
 * 4. Equalizer
 * 5. Sleep timer
 * 6. Tag editor
 * 7. Edit lyrics
 * 8. Details
 * 9. Share file
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSongActionsSheet(
    song: Song,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEqualizer: () -> Unit,
    onTagEditor: () -> Unit,
    onEditLyrics: () -> Unit,
    onDetails: () -> Unit,
    onShareFile: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pickingSleepTimer by remember { mutableStateOf(false) }

    val afterTrack by SleepTimer.afterTrack.collectAsStateWithLifecycle()
    val deadline by SleepTimer.deadline.collectAsStateWithLifecycle()
    val remaining by produceState<Long?>(initialValue = SleepTimer.remainingMs(), deadline) {
        while (true) {
            value = SleepTimer.remainingMs()
            delay(1000)
        }
    }
    val sleepTimerStatus = remaining?.let { ms ->
        val totalSeconds = (ms + 999) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        "%d:%02d".format(Locale.ROOT, minutes, seconds)
    } ?: stringResource(R.string.after_this_song).takeIf { afterTrack }

    val artists = remember(song.artist) {
        val split = ArtistSplitter.split(song.artist)
        if (split.isEmpty() && song.artist.isNotBlank()) listOf(song.artist) else split
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = pickingSleepTimer,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "SheetContentTransition",
        ) { inSleepTimerPicker ->
            if (inSleepTimerPicker) {
                LocalSleepTimerPicker(
                    onBack = { pickingSleepTimer = false },
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                ) {
                    LocalSongActionsHeader(song = song)

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    // 1. Go to Album
                    LocalSongActionItem(
                        icon = Icons.Rounded.Album,
                        label = "Go to Album",
                        onClick = onGoToAlbum,
                    )

                    // 2. Go to Artist
                    LocalSongActionItem(
                        icon = Icons.Rounded.Person,
                        label = "Go to Artist",
                        trailingIcon = if (artists.size > 1) Icons.AutoMirrored.Rounded.KeyboardArrowRight else null,
                        onClick = onGoToArtist,
                    )

                    // 3. Add to playlist
                    LocalSongActionItem(
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        label = "Add to playlist",
                        onClick = onAddToPlaylist,
                    )

                    // 4. Equalizer
                    LocalSongActionItem(
                        icon = Icons.Rounded.GraphicEq,
                        label = "Equalizer",
                        onClick = onEqualizer,
                    )

                    // 5. Sleep timer
                    LocalSongActionItem(
                        icon = Icons.Rounded.Bedtime,
                        label = "Sleep timer",
                        value = sleepTimerStatus,
                        onClick = { pickingSleepTimer = true },
                    )

                    // 6. Tag editor
                    LocalSongActionItem(
                        icon = Icons.Rounded.Edit,
                        label = "Tag editor",
                        onClick = onTagEditor,
                    )

                    // 7. Edit lyrics
                    LocalSongActionItem(
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        label = "Edit lyrics",
                        onClick = onEditLyrics,
                    )

                    // 8. Details
                    LocalSongActionItem(
                        icon = Icons.Rounded.Info,
                        label = "Details",
                        onClick = onDetails,
                    )

                    // 9. Share file
                    LocalSongActionItem(
                        icon = Icons.Rounded.Share,
                        label = "Share file",
                        onClick = onShareFile,
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
