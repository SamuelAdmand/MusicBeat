package com.music.bitchord.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.components.fastscroll.FAST_SCROLLER_THUMB_HEIGHT
import com.music.bitchord.ui.components.fastscroll.FastScrollPopup
import com.music.bitchord.ui.components.fastscroll.FastScrollThumb
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Typealias for backward compatibility with previous shape imports
typealias FastScrollPopupShape = com.music.bitchord.ui.components.fastscroll.FastScrollPopupShape

/**
 * Fast scroller overlay for [LazyListState].
 * Synchronizes the alphabet popup letter with the item currently at the top of the viewport.
 */
@Composable
fun FastScroller(
    listState: LazyListState,
    itemCount: Int,
    sectionNameForIndex: (Int) -> String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    headerCount: Int = 1,
    minItemsForFastScroll: Int = 6,
) {
    FastScrollerCore(
        itemCount = itemCount,
        scrollProgress = {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || itemCount <= 0) return@FastScrollerCore 0f
            val totalItems = layoutInfo.totalItemsCount
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            if (firstItem.index == 0 && firstItem.offset >= layoutInfo.viewportStartOffset) {
                0f
            } else if (lastItem.index == totalItems - 1 &&
                (lastItem.offset + lastItem.size) <= (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding + 2)
            ) {
                1f
            } else {
                val visibleCount = visibleItems.size
                val maxFirstIndex = (totalItems - visibleCount).coerceAtLeast(1)
                val itemSize = firstItem.size.toFloat().coerceAtLeast(1f)
                val subItemOffset = (-firstItem.offset.toFloat() / itemSize).coerceIn(0f, 1f)
                val currentIndex = firstItem.index + subItemOffset
                (currentIndex / maxFirstIndex).coerceIn(0f, 1f)
            }
        },
        activeTopItemIndex = {
            val firstIndex = listState.firstVisibleItemIndex
            (firstIndex - headerCount).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        targetItemIndexForFraction = { fraction ->
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount.takeIf { it > 0 } ?: (itemCount + headerCount)
            val visibleCount = layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 8
            val maxFirstIndex = (totalItems - visibleCount).coerceAtLeast(1)
            val targetIndex = (fraction * maxFirstIndex).roundToInt().coerceIn(0, maxFirstIndex)
            (targetIndex - headerCount).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        isScrollInProgress = { listState.isScrollInProgress },
        onScrollToFraction = { fraction ->
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount.takeIf { it > 0 } ?: (itemCount + headerCount)
            val visibleCount = layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 8
            val maxFirstIndex = (totalItems - visibleCount).coerceAtLeast(1)
            if (fraction <= 0.005f) {
                listState.scrollToItem(0, 0)
            } else if (fraction >= 0.995f) {
                listState.scrollToItem(totalItems - 1, 0)
            } else {
                val exactTarget = fraction * maxFirstIndex
                val targetIndex = exactTarget.toInt().coerceIn(0, maxFirstIndex)
                val remainder = exactTarget - targetIndex
                val avgSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 150
                val scrollOffset = (remainder * avgSize).roundToInt()
                listState.scrollToItem(targetIndex, scrollOffset)
            }
        },
        sectionNameForIndex = sectionNameForIndex,
        modifier = modifier,
        contentPadding = contentPadding,
        headerCount = headerCount,
        minItemsForFastScroll = minItemsForFastScroll,
    )
}

/**
 * Fast scroller overlay for [LazyGridState].
 * Synchronizes the alphabet popup letter with the grid item currently at the top of the viewport.
 */
@Composable
fun FastScroller(
    gridState: LazyGridState,
    itemCount: Int,
    sectionNameForIndex: (Int) -> String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    headerCount: Int = 1,
    minItemsForFastScroll: Int = 6,
) {
    FastScrollerCore(
        itemCount = itemCount,
        scrollProgress = {
            val layoutInfo = gridState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || itemCount <= 0) return@FastScrollerCore 0f
            val totalItems = layoutInfo.totalItemsCount
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            if (firstItem.index == 0 && firstItem.offset.y >= layoutInfo.viewportStartOffset) {
                0f
            } else if (lastItem.index == totalItems - 1 &&
                (lastItem.offset.y + lastItem.size.height) <= (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding + 2)
            ) {
                1f
            } else {
                val visibleCount = visibleItems.size
                val maxFirstIndex = (totalItems - visibleCount).coerceAtLeast(1)
                val itemHeight = firstItem.size.height.toFloat().coerceAtLeast(1f)
                val subItemOffset = (-firstItem.offset.y.toFloat() / itemHeight).coerceIn(0f, 1f)
                val currentIndex = firstItem.index + subItemOffset
                (currentIndex / maxFirstIndex).coerceIn(0f, 1f)
            }
        },
        activeTopItemIndex = {
            val firstIndex = gridState.firstVisibleItemIndex
            (firstIndex - headerCount).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        targetItemIndexForFraction = { fraction ->
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount.takeIf { it > 0 } ?: (itemCount + headerCount)
            val visibleCount = layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 8
            val maxFirstIndex = (totalItems - visibleCount).coerceAtLeast(1)
            val targetIndex = (fraction * maxFirstIndex).roundToInt().coerceIn(0, maxFirstIndex)
            (targetIndex - headerCount).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        isScrollInProgress = { gridState.isScrollInProgress },
        onScrollToFraction = { fraction ->
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount.takeIf { it > 0 } ?: (itemCount + headerCount)
            val visibleCount = layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 8
            val maxFirstIndex = (totalItems - visibleCount).coerceAtLeast(1)
            if (fraction <= 0.005f) {
                gridState.scrollToItem(0, 0)
            } else if (fraction >= 0.995f) {
                gridState.scrollToItem(totalItems - 1, 0)
            } else {
                val exactTarget = fraction * maxFirstIndex
                val targetIndex = exactTarget.toInt().coerceIn(0, maxFirstIndex)
                val remainder = exactTarget - targetIndex
                val avgHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 200
                val scrollOffset = (remainder * avgHeight).roundToInt()
                gridState.scrollToItem(targetIndex, scrollOffset)
            }
        },
        sectionNameForIndex = sectionNameForIndex,
        modifier = modifier,
        contentPadding = contentPadding,
        headerCount = headerCount,
        minItemsForFastScroll = minItemsForFastScroll,
    )
}

/**
 * Core fast scroller implementation with draggable thumb and animated teardrop popup bubble.
 * Follows Material Design 2 / Booming Music reference behavior.
 */
@Composable
fun FastScrollerCore(
    itemCount: Int,
    scrollProgress: () -> Float,
    isScrollInProgress: () -> Boolean,
    onScrollToFraction: suspend (Float) -> Unit,
    sectionNameForIndex: (Int) -> String,
    modifier: Modifier = Modifier,
    activeTopItemIndex: () -> Int = { 0 },
    targetItemIndexForFraction: (Float) -> Int = { (it * (itemCount - 1)).roundToInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0)) },
    contentPadding: PaddingValues = PaddingValues(),
    headerCount: Int = 1,
    minItemsForFastScroll: Int = 6,
) {
    if (itemCount < minItemsForFastScroll) return

    val density = LocalDensity.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var currentSection by remember { mutableStateOf("") }
    var lastHapticSection by remember { mutableStateOf("") }
    var thumbVisible by remember { mutableStateOf(false) }

    val thumbHeightPx = with(density) { FAST_SCROLLER_THUMB_HEIGHT.toPx() }
    val popupHeightDp = 64.dp
    val popupHeightPx = with(density) { popupHeightDp.toPx() }

    // Auto-hide thumb after inactivity (1.5 seconds)
    val scrolling = isScrollInProgress()
    LaunchedEffect(scrolling, isDragging) {
        if (isDragging || scrolling) {
            thumbVisible = true
        } else {
            delay(1500)
            thumbVisible = false
        }
    }

    // Keep section updated when idle/naturally scrolling
    val liveTopIndex by remember { derivedStateOf { activeTopItemIndex() } }
    LaunchedEffect(liveTopIndex, isDragging) {
        if (!isDragging && itemCount > 0) {
            val section = sectionNameForIndex(liveTopIndex)
            currentSection = if (section.isNotBlank()) section else "#"
        }
    }

    val thumbAlpha by animateFloatAsState(
        targetValue = if (thumbVisible || isDragging) 1f else 0f,
        animationSpec = tween(250),
        label = "fastScrollThumbAlpha",
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val topPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() }
        val bottomPaddingPx = with(density) { contentPadding.calculateBottomPadding().toPx() }
        val trackHeightPx = (constraints.maxHeight - topPaddingPx - bottomPaddingPx).coerceAtLeast(1f)
        val availableDistancePx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

        // Read scroll progress reactively
        val computedFraction = scrollProgress()
        val effectiveFraction = if (isDragging) dragFraction else computedFraction
        val thumbOffsetInTrack = effectiveFraction * availableDistancePx

        // Handle thumb positioning & list scrolling during drag gestures
        fun updateScroll(yInsideTrack: Float) {
            val relativeY = (yInsideTrack - thumbHeightPx / 2f).coerceIn(0f, availableDistancePx)
            val fraction = (relativeY / availableDistancePx).coerceIn(0f, 1f)
            dragFraction = fraction

            // Accurately resolve the item that appears at the top of the viewport
            val topSongIndex = targetItemIndexForFraction(fraction)
            val newSection = sectionNameForIndex(topSongIndex)
            val formattedSection = if (newSection.isNotBlank()) newSection else "#"
            currentSection = formattedSection

            if (formattedSection != lastHapticSection) {
                lastHapticSection = formattedSection
                haptics.play(Haptic.Select)
            }

            scope.launch {
                onScrollToFraction(fraction)
            }
        }

        // ── 1. Teardrop speech-bubble popup ──────────────────────────────────────
        // Vertical center of the popup is aligned with the vertical center of the thumb
        val popupY = (thumbOffsetInTrack + (thumbHeightPx / 2f) - (popupHeightPx / 2f))
            .coerceIn(0f, (trackHeightPx - popupHeightPx).coerceAtLeast(0f))

        FastScrollPopup(
            section = currentSection,
            visible = isDragging && currentSection.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                    end = 16.dp, // 16dp puts the pointer tip directly adjacent to the thumb with a clean 4dp gap
                )
                .offset {
                    IntOffset(
                        x = 0,
                        y = popupY.roundToInt(),
                    )
                },
        )

        // ── 2. Draggable vertical track & thumb ──────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(48.dp)
                .fillMaxHeight()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
                .pointerInput(itemCount, availableDistancePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        thumbVisible = true
                        updateScroll(down.position.y)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            updateScroll(change.position.y)
                        }
                        isDragging = false
                    }
                },
        ) {
            FastScrollThumb(
                isDragging = isDragging,
                alpha = thumbAlpha,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp)
                    .offset { IntOffset(x = 0, y = thumbOffsetInTrack.roundToInt()) },
            )
        }
    }
}
