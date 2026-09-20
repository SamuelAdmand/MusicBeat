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
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.Job
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
            if (totalItems <= 1) return@FastScrollerCore 0f

            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()

            // Exact boundary check for top
            if (firstItem.index == 0 && firstItem.offset >= layoutInfo.viewportStartOffset) {
                return@FastScrollerCore 0f
            }

            // Exact boundary check for bottom
            val viewportEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
            if (lastItem.index == totalItems - 1 && (lastItem.offset + lastItem.size) <= viewportEnd + 2) {
                return@FastScrollerCore 1f
            }

            // Continuous top item progress without integer jumps
            val firstItemSize = firstItem.size.toFloat().coerceAtLeast(1f)
            val topScrollOffset = (layoutInfo.viewportStartOffset - firstItem.offset).toFloat()
            val topItemFraction = (topScrollOffset / firstItemSize).coerceIn(0f, 1f)
            val continuousFirstIndex = firstItem.index.toFloat() + topItemFraction

            // Continuous bottom item progress as items enter from below
            val lastItemSize = lastItem.size.toFloat().coerceAtLeast(1f)
            val bottomVisiblePixels = (viewportEnd - lastItem.offset).toFloat()
            val bottomItemFraction = (bottomVisiblePixels / lastItemSize).coerceIn(0f, 1f)
            val continuousLastIndex = lastItem.index.toFloat() + bottomItemFraction

            val continuousVisibleItems = (continuousLastIndex - continuousFirstIndex).coerceAtLeast(1f)
            val maxScrollableIndex = (totalItems.toFloat() - continuousVisibleItems).coerceAtLeast(0.001f)

            (continuousFirstIndex / maxScrollableIndex).coerceIn(0f, 1f)
        },
        activeTopItemIndex = {
            val firstIndex = listState.firstVisibleItemIndex
            (firstIndex - headerCount).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        targetItemIndexForFraction = { fraction ->
            ((fraction * (itemCount - 1)).roundToInt()).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        isScrollInProgress = { listState.isScrollInProgress },
        onScrollToFraction = { fraction ->
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount.takeIf { it > 0 } ?: (itemCount + headerCount)
            if (totalItems <= 1) return@FastScrollerCore
            val visibleCount = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val maxTargetIndex = (totalItems - visibleCount).coerceAtLeast(0)
            val targetIndex = (fraction * maxTargetIndex).roundToInt().coerceIn(0, totalItems - 1)
            listState.scrollToItem(targetIndex, 0)
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
            if (totalItems <= 1) return@FastScrollerCore 0f

            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()

            // Exact boundary check for top
            if (firstItem.index == 0 && firstItem.offset.y >= layoutInfo.viewportStartOffset) {
                return@FastScrollerCore 0f
            }

            // Exact boundary check for bottom
            val viewportEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
            if (lastItem.index == totalItems - 1 && (lastItem.offset.y + lastItem.size.height) <= viewportEnd + 2) {
                return@FastScrollerCore 1f
            }

            // Top row items & continuous progress across rows to avoid column-skip jumps
            val topRowItems = visibleItems.filter { it.offset.y == firstItem.offset.y }
            val topRowHeight = topRowItems.maxOfOrNull { it.size.height }?.toFloat()?.coerceAtLeast(1f)
                ?: firstItem.size.height.toFloat().coerceAtLeast(1f)
            val itemsInTopRow = topRowItems.size.coerceAtLeast(1)
            val topScrollOffset = (layoutInfo.viewportStartOffset - firstItem.offset.y).toFloat()
            val topRowFraction = (topScrollOffset / topRowHeight).coerceIn(0f, 1f)
            val continuousFirstIndex = firstItem.index.toFloat() + (topRowFraction * itemsInTopRow.toFloat())

            // Bottom row items & continuous progress as bottom row enters
            val bottomRowItems = visibleItems.filter { it.offset.y == lastItem.offset.y }
            val bottomRowHeight = bottomRowItems.maxOfOrNull { it.size.height }?.toFloat()?.coerceAtLeast(1f)
                ?: lastItem.size.height.toFloat().coerceAtLeast(1f)
            val itemsInBottomRow = bottomRowItems.size.coerceAtLeast(1)
            val bottomVisiblePixels = (viewportEnd - lastItem.offset.y).toFloat()
            val bottomRowFraction = (bottomVisiblePixels / bottomRowHeight).coerceIn(0f, 1f)
            val continuousLastIndex = lastItem.index.toFloat() + (bottomRowFraction * itemsInBottomRow.toFloat())

            val continuousVisibleItems = (continuousLastIndex - continuousFirstIndex).coerceAtLeast(1f)
            val maxScrollableIndex = (totalItems.toFloat() - continuousVisibleItems).coerceAtLeast(0.001f)

            (continuousFirstIndex / maxScrollableIndex).coerceIn(0f, 1f)
        },
        activeTopItemIndex = {
            val firstIndex = gridState.firstVisibleItemIndex
            (firstIndex - headerCount).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        targetItemIndexForFraction = { fraction ->
            ((fraction * (itemCount - 1)).roundToInt()).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        },
        isScrollInProgress = { gridState.isScrollInProgress },
        onScrollToFraction = { fraction ->
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount.takeIf { it > 0 } ?: (itemCount + headerCount)
            if (totalItems <= 1) return@FastScrollerCore
            val visibleCount = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val maxTargetIndex = (totalItems - visibleCount).coerceAtLeast(0)
            val targetIndex = (fraction * maxTargetIndex).roundToInt().coerceIn(0, totalItems - 1)
            gridState.scrollToItem(targetIndex, 0)
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

        // Track active scroll job to avoid concurrent coroutines fighting for LazyListState scrollMutex
        var scrollJob by remember { mutableStateOf<Job?>(null) }
        var lastScrolledIndex by remember { mutableStateOf<Int?>(null) }

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

            // Only trigger list scroll if target item index actually changed to eliminate mutex churn
            if (topSongIndex != lastScrolledIndex) {
                lastScrolledIndex = topSongIndex
                scrollJob?.cancel()
                scrollJob = scope.launch {
                    onScrollToFraction(fraction)
                }
            }
        }

        // ── 1. Teardrop speech-bubble popup ──────────────────────────────────────
        // Evaluated during the layout/offset phase to eliminate recompositions
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
                    val effectiveFraction = if (isDragging) dragFraction else scrollProgress()
                    val thumbY = effectiveFraction * availableDistancePx
                    val popupY = (thumbY + (thumbHeightPx / 2f) - (popupHeightPx / 2f))
                        .coerceIn(0f, (trackHeightPx - popupHeightPx).coerceAtLeast(0f))
                    IntOffset(
                        x = 0,
                        y = popupY.roundToInt(),
                    )
                },
        )

        // ── 2. Draggable vertical track & thumb ──────────────────────────────────
        val currentAvailableDistance by rememberUpdatedState(availableDistancePx)
        val currentScrollProgress by rememberUpdatedState(scrollProgress)
        val currentIsDragging by rememberUpdatedState(isDragging)
        val currentDragFraction by rememberUpdatedState(dragFraction)
        val currentThumbVisible by rememberUpdatedState(thumbVisible || thumbAlpha > 0.05f || isDragging)
        val currentUpdateScroll by rememberUpdatedState(::updateScroll)
        val touchMarginPx = with(density) { 10.dp.toPx() }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(20.dp)
                .fillMaxHeight()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
                .pointerInput(availableDistancePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // If scrollbar is hidden or not active, do not intercept touches.
                        if (!currentThumbVisible) {
                            return@awaitEachGesture
                        }

                        // Check if touch position is actually on the thumb
                        val fraction = if (currentIsDragging) currentDragFraction else currentScrollProgress()
                        val currentThumbOffset = fraction * currentAvailableDistance
                        val thumbTop = currentThumbOffset - touchMarginPx
                        val thumbBottom = currentThumbOffset + thumbHeightPx + touchMarginPx
                        val isHit = down.position.y in thumbTop..thumbBottom

                        if (!isHit) {
                            return@awaitEachGesture
                        }

                        // Touch is directly on the thumb: consume and engage dragging
                        down.consume()
                        isDragging = true
                        thumbVisible = true
                        lastScrolledIndex = null
                        currentUpdateScroll(down.position.y)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            currentUpdateScroll(change.position.y)
                        }
                        isDragging = false
                        lastScrolledIndex = null
                    }
                },
        ) {
            FastScrollThumb(
                isDragging = isDragging,
                alpha = thumbAlpha,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp)
                    .offset {
                        val effectiveFraction = if (isDragging) dragFraction else scrollProgress()
                        val thumbOffset = (effectiveFraction * availableDistancePx).roundToInt()
                        IntOffset(x = 0, y = thumbOffset)
                    },
            )
        }
    }
}
