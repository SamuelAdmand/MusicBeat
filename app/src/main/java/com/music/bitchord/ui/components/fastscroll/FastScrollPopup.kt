package com.music.bitchord.ui.components.fastscroll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Teardrop alphabet popup for the fast scroller, matching Material Design 2 / Booming Music.
 * Displays the current section letter/symbol with an animated bubble expanding from the thumb.
 */
@Composable
fun FastScrollPopup(
    section: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val popupShape = FastScrollPopupShape()
    val popupWidth = 78.dp
    val popupHeight = 64.dp

    AnimatedVisibility(
        visible = visible && section.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(120, easing = FastOutSlowInEasing)) +
            scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                initialScale = 0.3f,
                transformOrigin = TransformOrigin(1f, 0.5f),
            ),
        exit = fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing)) +
            scaleOut(
                animationSpec = tween(150, easing = FastOutSlowInEasing),
                targetScale = 0.3f,
                transformOrigin = TransformOrigin(1f, 0.5f),
            ),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(width = popupWidth, height = popupHeight)
                .shadow(elevation = 4.dp, shape = popupShape)
                .background(MaterialTheme.colorScheme.primaryContainer, popupShape),
        ) {
            // Text is centered inside the left circular lobe (diameter = popupHeight = 64dp)
            Box(
                modifier = Modifier
                    .size(popupHeight)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = section,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
