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
 * Shared premium application backdrop.
 *
 * The historical function name is retained so every existing screen can adopt the new design
 * without changing navigation or gameplay wiring. The center stays quiet and bright; blue,
 * turquoise, purple and orange live at the edges as restrained brand energy.
 */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFBFDFF),
                    0.42f to Color(0xFFF7FAFF),
                    0.76f to Color(0xFFF3F8FF),
                    1.00f to Color(0xFFF8F5FF),
                ),
            ),
        )

        val lowerGlow = Path().apply {
            moveTo(0f, size.height * .76f)
            cubicTo(
                size.width * .22f, size.height * .66f,
                size.width * .58f, size.height * .91f,
                size.width, size.height * .76f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            lowerGlow,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0x0D12B8A6),
                    Color(0x142563EB),
                    Color(0x107C3AED),
                ),
                startY = size.height * .69f,
                endY = size.height,
            ),
        )

        val topAura = Path().apply {
            moveTo(size.width * .52f, 0f)
            cubicTo(
                size.width * .72f, size.height * .07f,
                size.width * .87f, size.height * .02f,
                size.width, size.height * .11f,
            )
            lineTo(size.width, 0f)
            close()
        }
        drawPath(
            topAura,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0x122563EB), Color(0x117C3AED)),
            ),
        )

        data class Marker(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        val markers = listOf(
            Marker(.030f, .10f, 12f, Color(0xFF2563EB), .18f),
            Marker(.074f, .18f, 8f, Color(0xFF12B8A6), .17f),
            Marker(.958f, .09f, 11f, Color(0xFF7C3AED), .17f),
            Marker(.907f, .22f, 7f, Color(0xFFF97316), .16f),
            Marker(.025f, .47f, 8f, Color(0xFFF97316), .12f),
            Marker(.970f, .53f, 9f, Color(0xFF12B8A6), .14f),
            Marker(.060f, .78f, 7f, Color(0xFF7C3AED), .12f),
            Marker(.930f, .74f, 8f, Color(0xFF2563EB), .12f),
        )
        markers.forEach { marker ->
            val side = marker.side
            drawRoundRect(
                color = marker.color.copy(alpha = marker.alpha),
                topLeft = Offset(size.width * marker.x - side / 2f, size.height * marker.y - side / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * .30f, side * .30f),
            )
        }

        listOf(
            Triple(.13f, .31f, Color(0xFF12B8A6)),
            Triple(.86f, .36f, Color(0xFFF97316)),
            Triple(.18f, .91f, Color(0xFF7C3AED)),
            Triple(.80f, .89f, Color(0xFF2563EB)),
        ).forEach { (x, y, color) ->
            drawCircle(
                color = color.copy(alpha = .11f),
                radius = 2.4f,
                center = Offset(size.width * x, size.height * y),
            )
        }
    }
}
