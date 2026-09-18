package com.music.bitchord.ui.components.fastscroll

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

val FAST_SCROLLER_THUMB_HEIGHT = 52.dp
val FAST_SCROLLER_THUMB_REST_WIDTH = 6.dp
val FAST_SCROLLER_THUMB_DRAG_WIDTH = 8.dp

/**
 * Fast scroller thumb matching Booming Music's `scroller_thumb.xml` (52dp height, 6dp width, 3dp radius).
 * Expands slightly to 8dp while actively dragging.
 */
@Composable
fun FastScrollThumb(
    isDragging: Boolean,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) FAST_SCROLLER_THUMB_DRAG_WIDTH else FAST_SCROLLER_THUMB_REST_WIDTH,
        animationSpec = tween(150),
        label = "fastScrollThumbWidth",
    )

    Box(
        modifier = modifier
            .size(width = thumbWidth, height = FAST_SCROLLER_THUMB_HEIGHT)
            .graphicsLayer { this.alpha = if (isDragging) 1f else alpha }
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            ),
    )
}
