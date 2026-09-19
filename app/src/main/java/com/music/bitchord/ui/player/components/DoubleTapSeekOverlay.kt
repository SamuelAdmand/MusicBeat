package com.music.bitchord.ui.player.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Direction of double-tap seek.
 */
enum class SeekDirection {
    FORWARD,
    BACKWARD,
}

/**
 * State of the double-tap seek visual feedback.
 */
data class SeekFeedback(
    val direction: SeekDirection,
    val seconds: Int,
    val timestamp: Long = SystemClock.uptimeMillis(),
)

/**
 * Wraps content with a double-tap gesture detector on the left and right halves
 * to seek backward and forward respectively, displaying an animated visual feedback overlay.
 *
 * @param enabled Whether double-tap seeking is active.
 * @param onSeekRelative Callback with the seek offset in seconds (e.g. -5 or +5).
 * @param modifier The modifier to apply to the container.
 * @param shape Shape to clip the feedback overlay to (e.g. matching album art corners).
 * @param content The composable content to wrap (e.g. album art).
 */
@Composable
fun DoubleTapSeekArea(
    enabled: Boolean,
    onSeekRelative: (deltaSeconds: Long) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    content: @Composable () -> Unit,
) {
    var feedback by remember { mutableStateOf<SeekFeedback?>(null) }
    val currentOnSeekRelative by rememberUpdatedState(onSeekRelative)
    val isEnabled by rememberUpdatedState(enabled)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        var lastTapTime = 0L
                        var lastIsForward = false
                        var tapCounter = 0

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!isEnabled) return@awaitEachGesture
                            val downTime = SystemClock.uptimeMillis()
                            val isForward = down.position.x >= size.width / 2f

                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                val upTime = SystemClock.uptimeMillis()
                                val distance = (up.position - down.position).getDistance()
                                if (distance <= viewConfiguration.touchSlop && upTime - downTime < 350L) {
                                    val timeSinceLast = upTime - lastTapTime
                                    if (timeSinceLast < 450L && isForward == lastIsForward) {
                                        // 2nd, 3rd, etc. tap in rapid sequence on the same side
                                        tapCounter++
                                        val seekSeconds = (tapCounter - 1) * 5
                                        val deltaSec = if (isForward) 5L else -5L
                                        currentOnSeekRelative(deltaSec)
                                        feedback = SeekFeedback(
                                            direction = if (isForward) SeekDirection.FORWARD else SeekDirection.BACKWARD,
                                            seconds = seekSeconds,
                                            timestamp = upTime,
                                        )
                                    } else {
                                        // 1st tap of a potential sequence
                                        tapCounter = 1
                                        lastIsForward = isForward
                                    }
                                    lastTapTime = upTime
                                }
                            } else {
                                // Drag or cancellation
                                tapCounter = 0
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        content()

        feedback?.let { currentFeedback ->
            DoubleTapSeekOverlay(
                feedback = currentFeedback,
                onDismiss = { feedback = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Animated visual feedback overlay showing "-5s" or "+5s" and seek direction glyphs.
 * Uses an elegant expanding arc ripple and smooth spring/fade transitions.
 */
@Composable
fun DoubleTapSeekOverlay(
    feedback: SeekFeedback,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isForward = feedback.direction == SeekDirection.FORWARD
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.82f) }
    val rippleScale = remember { Animatable(0.4f) }

    LaunchedEffect(feedback.timestamp) {
        // Smooth entrance / bounce on each tap
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
            )
        }
        launch {
            rippleScale.snapTo(0.4f)
            rippleScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            )
        }
        launch {
            scale.snapTo(0.85f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }

        // Wait then smoothly fade out
        delay(650L)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        )
        onDismiss()
    }

    Box(
        modifier = modifier.graphicsLayer { this.alpha = alpha.value },
    ) {
        // Curved radial ripple expanding from the side
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart),
        ) {
            val centerOffset = Offset(
                x = if (isForward) size.width else 0f,
                y = size.height / 2f,
            )
            val maxRadius = size.height.coerceAtLeast(size.width) * 1.1f * rippleScale.value
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = centerOffset,
                    radius = maxRadius,
                ),
                center = centerOffset,
                radius = maxRadius,
            )
        }

        // Frosted indicator pill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${if (isForward) "+" else "-"}${feedback.seconds}s",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(0f, 2f),
                            blurRadius = 4f,
                        ),
                    ),
                    color = Color.White,
                )
            }
        }
    }
}
