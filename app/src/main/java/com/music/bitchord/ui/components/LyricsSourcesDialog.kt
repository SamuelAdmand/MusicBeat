package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.feature.lyrics.manager.LyricsExtensionManager
import com.music.bitchord.ui.components.lyrics.LyricsExtensionSyncHeader
import com.music.bitchord.ui.components.lyrics.LyricsSourceItemCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

private const val SWAP_THRESHOLD = 0.6f

/**
 * Modern dialog for ordering and enabling lyrics extensions.
 * Provides live GitHub sync, drag-and-drop reordering, and syllable sync toggling.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun LyricsSourcesDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val selected by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val savedOrder by AppSettings.lyricsSourceOrder.collectAsStateWithLifecycle()
    val prioritizeSyllableSync by AppSettings.prioritizeSyllableSync.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSyncing by LyricsExtensionManager.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by LyricsExtensionManager.syncMessage.collectAsStateWithLifecycle()
    var showRepoSettingsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SCRIM_COLOR)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(28.dp))
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            // 1. Header with title, description, and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 20.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lyrics_sources),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.lyrics_sources_order),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 2. Extension Sync & Update Banner
            LyricsExtensionSyncHeader(
                extensionCount = savedOrder.size,
                isSyncing = isSyncing,
                syncMessage = syncMessage,
                onSyncClick = {
                    scope.launch {
                        LyricsExtensionManager.syncFromRepository(context, force = true)
                    }
                },
                onSettingsClick = { showRepoSettingsDialog = true },
            )

            // 3. Scrollable Sources & Syllable Sync Toggle
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    ReorderableSourceList(
                        order = savedOrder,
                        selected = selected,
                        onReorder = AppSettings::setLyricsSourceOrder,
                        onToggle = { source ->
                            val checked = source in selected
                            if (checked && selected.size <= 1) return@ReorderableSourceList
                            AppSettings.setLyricsSources(
                                if (checked) selected - source else selected + source,
                            )
                        },
                    )

                    Spacer(Modifier.height(6.dp))

                    ModernSyllableSyncCard(
                        checked = prioritizeSyllableSync,
                        onToggle = { AppSettings.setPrioritizeSyllableSync(!prioritizeSyllableSync) },
                    )
                }
            }

            // 4. Fixed Bottom Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = AppSettings::resetLyricsSourceSettings,
                    ) {
                        Text(
                            text = stringResource(R.string.reset_to_default),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    FilledTonalButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (showRepoSettingsDialog) {
        LyricsRepoSettingsDialog(
            hazeState = hazeState,
            onDismiss = { showRepoSettingsDialog = false },
        )
    }
}

/**
 * Modern card for prioritizing word-synced lyrics over line-synced ones.
 */
@Composable
private fun ModernSyllableSyncCard(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.prioritize_syllable_lyrics),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.prioritize_syllable_lyrics_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

/**
 * Modern checkable and drag-reorderable list of lyrics sources.
 */
@Composable
private fun ReorderableSourceList(
    order: List<LyricsSource>,
    selected: Set<LyricsSource>,
    onReorder: (List<LyricsSource>) -> Unit,
    onToggle: (LyricsSource) -> Unit,
) {
    var liveOrder by remember(order) { mutableStateOf(order) }
    var draggedSource by remember { mutableStateOf<LyricsSource?>(null) }
    var totalDrag by remember { mutableStateOf(0f) }
    var startIndex by remember { mutableStateOf(0) }
    var pitchPx by remember { mutableStateOf(0f) }
    var lockedPitchPx by remember { mutableStateOf(0f) }

    Column {
        liveOrder.forEach { source ->
            key(source) {
                val checked = source in selected
                val toggleable = !checked || selected.size > 1
                val dragging = source == draggedSource
                val ext = LyricsExtensionManager.getExtension(source.id)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 1f else 0f)
                        .onSizeChanged { pitchPx = it.height.toFloat() }
                        .graphicsLayer {
                            translationY = if (dragging) {
                                totalDrag - (liveOrder.indexOf(source) - startIndex) * lockedPitchPx
                            } else {
                                0f
                            }
                        },
                ) {
                    LyricsSourceItemCard(
                        source = source,
                        selected = checked,
                        toggleable = toggleable,
                        isDragging = dragging,
                        version = ext?.version,
                        onToggle = { onToggle(source) },
                        dragModifier = Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedSource = source
                                    totalDrag = 0f
                                    startIndex = liveOrder.indexOf(source)
                                    lockedPitchPx = pitchPx
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    val pitch = lockedPitchPx
                                    if (pitch <= 0f) return@detectDragGestures
                                    var index = liveOrder.indexOf(source)
                                    if (index < 0) return@detectDragGestures

                                    totalDrag = (totalDrag + delta.y).coerceIn(
                                        -startIndex * pitch,
                                        (liveOrder.lastIndex - startIndex) * pitch,
                                    )

                                    while (true) {
                                        val travelled = totalDrag / pitch
                                        val moved = (index - startIndex).toFloat()
                                        if (travelled > moved + SWAP_THRESHOLD && index < liveOrder.lastIndex) {
                                            liveOrder = liveOrder.toMutableList().apply {
                                                add(index + 1, removeAt(index))
                                            }
                                            index++
                                        } else if (travelled < moved - SWAP_THRESHOLD && index > 0) {
                                            liveOrder = liveOrder.toMutableList().apply {
                                                add(index - 1, removeAt(index))
                                            }
                                            index--
                                        } else {
                                            break
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedSource = null
                                    totalDrag = 0f
                                    onReorder(liveOrder)
                                },
                                onDragCancel = {
                                    draggedSource = null
                                    totalDrag = 0f
                                    liveOrder = order
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
