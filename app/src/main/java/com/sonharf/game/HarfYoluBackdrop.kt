package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Quiet Harf Yolu backdrop: warm neutral field with restrained edge markers. */
@Composable
internal fun HarfYoluBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFEFA),
                    Color(0xFFF7F6F1),
                    Color(0xFFF4F2EC),
                ),
            ),
        )

        val sage = Color(0xFF365F53)
        val slate = Color(0xFF718693)
        val terracotta = Color(0xFFAD6A57)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(sage.copy(alpha = .08f), Color.Transparent),
                center = Offset(size.width * .92f, size.height * .14f),
                radius = size.minDimension * .55f,
            ),
            radius = size.minDimension * .55f,
            center = Offset(size.width * .92f, size.height * .14f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(slate.copy(alpha = .06f), Color.Transparent),
                center = Offset(size.width * .08f, size.height * .82f),
                radius = size.minDimension * .58f,
            ),
            radius = size.minDimension * .58f,
            center = Offset(size.width * .08f, size.height * .82f),
        )

        data class Marker(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        val markers = listOf(
            Marker(.035f, .12f, 10f, sage, .11f),
            Marker(.090f, .21f, 7f, slate, .08f),
            Marker(.965f, .10f, 9f, slate, .10f),
            Marker(.940f, .30f, 6f, terracotta, .07f),
            Marker(.030f, .62f, 7f, terracotta, .06f),
            Marker(.960f, .74f, 8f, sage, .08f),
        )
        markers.forEach { marker ->
            drawRoundRect(
                color = marker.color.copy(alpha = marker.alpha),
                topLeft = Offset(size.width * marker.x - marker.side / 2f, size.height * marker.y - marker.side / 2f),
                size = Size(marker.side, marker.side),
                cornerRadius = CornerRadius(marker.side * .25f, marker.side * .25f),
            )
        }
    }
}
