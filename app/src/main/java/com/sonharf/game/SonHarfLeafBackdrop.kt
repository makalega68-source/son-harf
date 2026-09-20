package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Shared lightweight app backdrop.
 *
 * The center is deliberately quiet for reading and play. Very low-contrast sage landscape forms
 * remain at the edges so the product has a recognizable game identity without looking like a
 * colorful children's dashboard. Everything is vector drawn; no bitmap or overdraw-heavy layer.
 */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFFFEFA),
                    0.55f to Color(0xFFF8F6F0),
                    1.00f to Color(0xFFF1F2EC),
                ),
            ),
        )

        val distant = Path().apply {
            moveTo(0f, size.height * .78f)
            cubicTo(
                size.width * .16f, size.height * .68f,
                size.width * .27f, size.height * .75f,
                size.width * .40f, size.height * .69f,
            )
            cubicTo(
                size.width * .58f, size.height * .61f,
                size.width * .73f, size.height * .80f,
                size.width, size.height * .70f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            distant,
            brush = Brush.verticalGradient(
                listOf(Color(0x1277977F), Color(0x2677977F)),
                startY = size.height * .64f,
                endY = size.height,
            ),
        )

        val foreground = Path().apply {
            moveTo(0f, size.height * .89f)
            cubicTo(
                size.width * .22f, size.height * .78f,
                size.width * .40f, size.height * .94f,
                size.width * .58f, size.height * .85f,
            )
            cubicTo(
                size.width * .74f, size.height * .78f,
                size.width * .88f, size.height * .91f,
                size.width, size.height * .83f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            foreground,
            brush = Brush.verticalGradient(
                listOf(Color(0x173F6B52), Color(0x2B3F6B52)),
                startY = size.height * .80f,
                endY = size.height,
            ),
        )

        // Sparse edge details hint at map/territory geometry while leaving the reading field clear.
        listOf(
            Offset(size.width * .05f, size.height * .20f),
            Offset(size.width * .94f, size.height * .16f),
            Offset(size.width * .04f, size.height * .55f),
            Offset(size.width * .96f, size.height * .59f),
        ).forEachIndexed { index, point ->
            val radius = if (index % 2 == 0) 3.2f else 2.5f
            drawCircle(
                color = if (index % 2 == 0) Color(0xFF285943).copy(alpha = .08f)
                else Color(0xFF6F8794).copy(alpha = .07f),
                radius = radius,
                center = point,
            )
        }

        // A single soft gold marker is enough to carry the prestige accent.
        drawCircle(
            color = Color(0xFFB58A39).copy(alpha = .08f),
            radius = 3f,
            center = Offset(size.width * .90f, size.height * .34f),
        )
    }
}
