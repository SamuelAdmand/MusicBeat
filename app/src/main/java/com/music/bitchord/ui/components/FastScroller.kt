package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Custom teardrop speech-bubble shape pointing to the right edge (towards the thumb).
 * Matches Material Design 2 / BoomingMusic fast-scroll popup bubble.
 */
class FastScrollPopupShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val r = h / 2f
        val path = Path().apply {
            reset()
            // Start at top center of circular part: (r, 0)
            moveTo(r, 0f)
            // Arc on the left (circle from 270° counter-clockwise to 90°)
            arcTo(
                rect = Rect(left = 0f, top = 0f, right = h, bottom = h),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false,
            )
            // Now at (r, h)
            // Smooth bottom curve towards the pointer tip at (w, r)
            quadraticTo(x1 = r + (w - r) * 0.45f, y1 = h, x2 = w, y2 = r)
            // Smooth top curve back from tip (w, r) to (r, 0)
            quadraticTo(x1 = r + (w - r) * 0.45f, y1 = 0f, x2 = r, y2 = 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Fast scroller overlay for [LazyListState].
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
            } else if (lastItem.index == totalItems - 1 && (lastItem.offset + lastItem.size) <= (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding + 2)) {
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
            } else if (lastItem.index == totalItems - 1 && (lastItem.offset.y + lastItem.size.height) <= (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding + 2)) {
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
 */
@Composable
fun FastScrollerCore(
    itemCount: Int,
    scrollProgress: () -> Float,
    isScrollInProgress: () -> Boolean,
    onScrollToFraction: suspend (Float) -> Unit,
    sectionNameForIndex: (Int) -> String,
    modifier: Modifier = Modifier,
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
    var lastSection by remember { mutableStateOf("") }
    var thumbVisible by remember { mutableStateOf(false) }

    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
    val popupWidthDp = 144.dp
    val popupHeightDp = 76.dp
    val popupHeightPx = with(density) { popupHeightDp.toPx() }

    // Auto-hide thumb after inactivity
    val scrolling = isScrollInProgress()
    LaunchedEffect(scrolling, isDragging) {
        if (isDragging || scrolling) {
            thumbVisible = true
        } else {
            delay(1500)
            thumbVisible = false
        }
    }

    val thumbAlpha by animateFloatAsState(
        targetValue = if (thumbVisible || isDragging) 1f else 0f,
        animationSpec = tween(250),
        label = "thumbAlpha",
    )

    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 5.dp,
        animationSpec = tween(150),
        label = "thumbWidth",
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val topPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() }
        val bottomPaddingPx = with(density) { contentPadding.calculateBottomPadding().toPx() }
        val trackHeightPx = (constraints.maxHeight - topPaddingPx - bottomPaddingPx).coerceAtLeast(1f)
        val availableDistancePx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

        // Read scroll progress reactively from list/grid state
        val computedFraction = scrollProgress()
        val effectiveFraction = if (isDragging) dragFraction else computedFraction
        val thumbOffsetInTrack = effectiveFraction * availableDistancePx

        // Handle thumb positioning & list scrolling during drag gestures
        fun updateScroll(yInsideTrack: Float) {
            val relativeY = (yInsideTrack - thumbHeightPx / 2f).coerceIn(0f, availableDistancePx)
            val fraction = (relativeY / availableDistancePx).coerceIn(0f, 1f)
            dragFraction = fraction

            val rawSectionIndex = (fraction * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
            val newSection = sectionNameForIndex(rawSectionIndex)
            val formattedSection = if (newSection.isNotBlank()) newSection else "#"
            currentSection = formattedSection
            if (formattedSection != lastSection) {
                lastSection = formattedSection
                haptics.play(Haptic.Select)
            }

            scope.launch {
                onScrollToFraction(fraction)
            }
        }

        // Draggable vertical track on the right edge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(52.dp)
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
            // Teardrop popup bubble positioned to the left of the scroll thumb
            val popupY = (thumbOffsetInTrack + (thumbHeightPx / 2f) - (popupHeightPx / 2f))
                .coerceIn(0f, (trackHeightPx - popupHeightPx).coerceAtLeast(0f))

            AnimatedVisibility(
                visible = isDragging && currentSection.isNotEmpty(),
                enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.5f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = -with(density) { 12.dp.roundToPx() },
                            y = popupY.roundToInt(),
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(width = popupWidthDp, height = popupHeightDp)
                        .shadow(elevation = 8.dp, shape = FastScrollPopupShape())
                        .background(MaterialTheme.colorScheme.primaryContainer, FastScrollPopupShape()),
                ) {
                    // Position text in the center of the circular lobe (leftmost popupHeightDp x popupHeightDp)
                    Box(
                        modifier = Modifier
                            .size(popupHeightDp)
                            .align(Alignment.CenterStart),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = currentSection,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Visible Thumb Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp)
                    .offset { IntOffset(x = 0, y = thumbOffsetInTrack.roundToInt()) }
                    .size(width = thumbWidth, height = thumbHeightDp)
                    .graphicsLayer { alpha = if (isDragging) 1f else thumbAlpha }
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    ),
            )
        }
    }
}
