package com.music.bitchord.ui.components.fastscroll

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Material Design 2 teardrop speech-bubble shape with a rounded pointer tip pointing to the right.
 * Matches the exact geometry of Booming Music's `Md2PopupBackground` (AndroidFastScroll library).
 */
class FastScrollPopupShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val h = size.height
        val r = h / 2f
        val sqrt2 = sqrt(2f)
        // Ensure width is at least r + sqrt(2) * r to allow the curves to resolve properly
        val w = max(size.width, r + sqrt2 * r)

        val path = Path().apply {
            reset()

            // 1. Left semicircle centered at (r, r) from 90° (bottom) sweeping 180° clockwise to 270° (top)
            arcTo(
                rect = Rect(left = 0f, top = 0f, right = 2f * r, bottom = 2f * r),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )

            // 2. Top curve towards tip: center (w - sqrt2 * r, r), radius r, from -90° (270°) sweep 45° to 315°
            val topCornerCx = w - sqrt2 * r
            arcTo(
                rect = Rect(
                    left = topCornerCx - r,
                    top = 0f,
                    right = topCornerCx + r,
                    bottom = 2f * r,
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 45f,
                forceMoveTo = false,
            )

            // 3. Rounded pointer tip: radius r / 5, center (w - sqrt2 * (r / 5), r), from -45° sweep 90° to +45°
            val tipRadius = r / 5f
            val tipCx = w - sqrt2 * tipRadius
            arcTo(
                rect = Rect(
                    left = tipCx - tipRadius,
                    top = r - tipRadius,
                    right = tipCx + tipRadius,
                    bottom = r + tipRadius,
                ),
                startAngleDegrees = -45f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )

            // 4. Bottom curve back from tip: center (w - sqrt2 * r, r), radius r, from 45° sweep 45° to 90°
            arcTo(
                rect = Rect(
                    left = topCornerCx - r,
                    top = 0f,
                    right = topCornerCx + r,
                    bottom = 2f * r,
                ),
                startAngleDegrees = 45f,
                sweepAngleDegrees = 45f,
                forceMoveTo = false,
            )

            close()
        }

        return Outline.Generic(path)
    }
}
