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

/** Shared application backdrop for both the premium light system and the sellable Black Theme. */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    val dark = SonHarfTheme.IsDark
    Canvas(modifier) {
        val baseStops = if (dark) {
            arrayOf(
                0.00f to Color(0xFF090B10),
                0.42f to Color(0xFF0D1119),
                0.76f to Color(0xFF101724),
                1.00f to Color(0xFF120E1B),
            )
        } else {
            arrayOf(
                0.00f to Color(0xFFFBFDFF),
                0.42f to Color(0xFFF7FAFF),
                0.76f to Color(0xFFF3F8FF),
                1.00f to Color(0xFFF8F5FF),
            )
        }
        drawRect(brush = Brush.verticalGradient(colorStops = baseStops))

        val lowerGlow = Path().apply {
            moveTo(0f, size.height * .76f)
            cubicTo(size.width * .22f, size.height * .66f, size.width * .58f, size.height * .91f, size.width, size.height * .76f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            lowerGlow,
            brush = Brush.verticalGradient(
                listOf(
                    if (dark) Color(0x191FD1C2) else Color(0x0D12B8A6),
                    if (dark) Color(0x243B82F6) else Color(0x142563EB),
                    if (dark) Color(0x209B6CFF) else Color(0x107C3AED),
                ),
                startY = size.height * .69f,
                endY = size.height,
            ),
        )

        val topAura = Path().apply {
            moveTo(size.width * .52f, 0f)
            cubicTo(size.width * .72f, size.height * .07f, size.width * .87f, size.height * .02f, size.width, size.height * .11f)
            lineTo(size.width, 0f)
            close()
        }
        drawPath(
            topAura,
            brush = Brush.horizontalGradient(
                if (dark) listOf(Color.Transparent, Color(0x243B82F6), Color(0x209B6CFF))
                else listOf(Color.Transparent, Color(0x122563EB), Color(0x117C3AED)),
            ),
        )

        data class Marker(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        val markers = listOf(
            Marker(.030f, .10f, 12f, if (dark) Color(0xFF3B82F6) else Color(0xFF2563EB), if (dark) .25f else .18f),
            Marker(.074f, .18f, 8f, if (dark) Color(0xFF1FD1C2) else Color(0xFF12B8A6), if (dark) .23f else .17f),
            Marker(.958f, .09f, 11f, if (dark) Color(0xFF9B6CFF) else Color(0xFF7C3AED), if (dark) .23f else .17f),
            Marker(.907f, .22f, 7f, if (dark) Color(0xFFFF8A34) else Color(0xFFF97316), if (dark) .21f else .16f),
            Marker(.025f, .47f, 8f, if (dark) Color(0xFFFF8A34) else Color(0xFFF97316), if (dark) .18f else .12f),
            Marker(.970f, .53f, 9f, if (dark) Color(0xFF1FD1C2) else Color(0xFF12B8A6), if (dark) .19f else .14f),
            Marker(.060f, .78f, 7f, if (dark) Color(0xFF9B6CFF) else Color(0xFF7C3AED), if (dark) .18f else .12f),
            Marker(.930f, .74f, 8f, if (dark) Color(0xFF3B82F6) else Color(0xFF2563EB), if (dark) .18f else .12f),
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
    }
}
