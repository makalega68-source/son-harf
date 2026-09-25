package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Shared lightweight app backdrop.
 *
 * The historical function name is intentionally retained to avoid touching screen wiring. The
 * artwork itself now follows the purchased compact sports-dashboard language: a clean center,
 * cool blue depth, turquoise motion and restrained orange action markers. Everything is vector
 * drawn, so the retheme adds no bitmap decode or startup-memory cost.
 */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFFFCF6),
                    0.45f to Color(0xFFF9F4EA),
                    0.78f to Color(0xFFF5EDE0),
                    1.00f to Color(0xFFF0E6D5),
                ),
            ),
        )

        // Broad dashboard bands stay at the edges so cards and text keep a quiet center field.
        val rearBand = Path().apply {
            moveTo(0f, size.height * .70f)
            cubicTo(
                size.width * .22f, size.height * .61f,
                size.width * .58f, size.height * .86f,
                size.width, size.height * .69f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            rearBand,
            brush = Brush.verticalGradient(
                listOf(Color(0x12246C4C), Color(0x1F95651C)),
                startY = size.height * .62f,
                endY = size.height,
            ),
        )

        val frontBand = Path().apply {
            moveTo(0f, size.height * .84f)
            cubicTo(
                size.width * .30f, size.height * .72f,
                size.width * .66f, size.height * .94f,
                size.width, size.height * .80f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            frontBand,
            brush = Brush.verticalGradient(
                listOf(Color(0x14246C4C), Color(0x1D95651C)),
                startY = size.height * .75f,
                endY = size.height,
            ),
        )

        // Faint live-score style guide lines create motion without competing with gameplay UI.
        listOf(.18f, .52f, .86f).forEachIndexed { index, y ->
            val start = Offset(-size.width * .04f, size.height * y)
            val end = Offset(size.width * .33f, size.height * (y - .055f))
            drawLine(
                color = if (index == 1) Color(0xFF246C4C).copy(alpha = .075f)
                else Color(0xFF95651C).copy(alpha = .055f),
                start = start,
                end = end,
                strokeWidth = 2.2f,
            )
        }

        data class Marker(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        val markers = listOf(
            Marker(.035f, .11f, 10f, Color(0xFF95651C), .18f),
            Marker(.075f, .19f, 7f, Color(0xFF246C4C), .18f),
            Marker(.955f, .08f, 9f, Color(0xFFB27338), .17f),
            Marker(.905f, .23f, 6f, Color(0xFF95651C), .13f),
            Marker(.025f, .48f, 7f, Color(0xFFB27338), .12f),
            Marker(.965f, .54f, 8f, Color(0xFF246C4C), .14f),
            Marker(.070f, .76f, 6f, Color(0xFF95651C), .12f),
            Marker(.915f, .73f, 7f, Color(0xFFB27338), .11f),
        )
        markers.forEach { marker ->
            val side = marker.side
            drawRoundRect(
                color = marker.color.copy(alpha = marker.alpha),
                topLeft = Offset(size.width * marker.x - side / 2f, size.height * marker.y - side / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * .28f, side * .28f),
            )
        }

        // Small turquoise/orange status dots echo the purchased kit's data-rich sports cards.
        listOf(
            Triple(.13f, .31f, Color(0xFF246C4C)),
            Triple(.86f, .36f, Color(0xFFB27338)),
            Triple(.18f, .91f, Color(0xFFB27338)),
            Triple(.78f, .88f, Color(0xFF246C4C)),
        ).forEach { (x, y, color) ->
            drawCircle(
                color = color.copy(alpha = .11f),
                radius = 2.1f,
                center = Offset(size.width * x, size.height * y),
            )
        }
    }
}
