package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Shared restrained application backdrop for the light system and optional Black Theme. */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    val dark = SonHarfTheme.IsDark
    Canvas(modifier) {
        val background = if (dark) {
            listOf(Color(0xFF101412), Color(0xFF151B18), Color(0xFF101412))
        } else {
            listOf(Color(0xFFFFFEFA), Color(0xFFF7F6F1), Color(0xFFF4F2EC))
        }
        drawRect(brush = Brush.verticalGradient(background))

        val sage = if (dark) Color(0xFF78A493) else Color(0xFF365F53)
        val slate = if (dark) Color(0xFF8DA1AF) else Color(0xFF718693)
        val terracotta = if (dark) Color(0xFFC98770) else Color(0xFFAD6A57)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(sage.copy(alpha = if (dark) .11f else .07f), Color.Transparent),
                center = Offset(size.width * .90f, size.height * .12f),
                radius = size.minDimension * .62f,
            ),
            radius = size.minDimension * .62f,
            center = Offset(size.width * .90f, size.height * .12f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(slate.copy(alpha = if (dark) .09f else .05f), Color.Transparent),
                center = Offset(size.width * .08f, size.height * .84f),
                radius = size.minDimension * .60f,
            ),
            radius = size.minDimension * .60f,
            center = Offset(size.width * .08f, size.height * .84f),
        )

        data class Marker(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        val markers = listOf(
            Marker(.030f, .11f, 10f, sage, if (dark) .16f else .10f),
            Marker(.078f, .19f, 7f, slate, if (dark) .14f else .08f),
            Marker(.960f, .10f, 9f, slate, if (dark) .15f else .09f),
            Marker(.928f, .24f, 6f, terracotta, if (dark) .12f else .06f),
            Marker(.028f, .50f, 6f, terracotta, if (dark) .10f else .05f),
            Marker(.966f, .58f, 7f, sage, if (dark) .12f else .07f),
            Marker(.060f, .80f, 6f, slate, if (dark) .10f else .06f),
            Marker(.936f, .78f, 7f, sage, if (dark) .10f else .06f),
        )
        markers.forEach { marker ->
            drawRoundRect(
                color = marker.color.copy(alpha = marker.alpha),
                topLeft = Offset(size.width * marker.x - marker.side / 2f, size.height * marker.y - marker.side / 2f),
                size = Size(marker.side, marker.side),
                cornerRadius = CornerRadius(marker.side * .26f, marker.side * .26f),
            )
        }
    }
}
