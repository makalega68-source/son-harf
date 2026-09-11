package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium botanical background shared by Son Harf's light surfaces.
 *
 * It deliberately uses vector drawing instead of a full-screen bitmap: the
 * soft leaves, mint wash and faint gold dust scale cleanly on every phone and
 * add effectively no decode/memory risk to startup.
 */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFFFEF9),
                    0.38f to Color(0xFFFBFCF6),
                    0.68f to Color(0xFFF1F7F0),
                    1.00f to Color(0xFFDDEFE3),
                ),
            ),
        )

        // Diffuse mint atmosphere at the bottom, matching the reference without
        // introducing a hard decorative edge behind cards.
        val rearWave = Path().apply {
            moveTo(0f, size.height * .63f)
            cubicTo(
                size.width * .18f, size.height * .55f,
                size.width * .52f, size.height * .81f,
                size.width, size.height * .66f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            rearWave,
            brush = Brush.verticalGradient(
                listOf(Color(0x185FAD8C), Color(0x4A8EC6A4)),
                startY = size.height * .58f,
                endY = size.height,
            ),
        )

        val frontWave = Path().apply {
            moveTo(0f, size.height * .78f)
            cubicTo(
                size.width * .28f, size.height * .68f,
                size.width * .58f, size.height * .91f,
                size.width, size.height * .76f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            frontWave,
            brush = Brush.verticalGradient(
                listOf(Color(0x167AC5A7), Color(0x3D70B996)),
                startY = size.height * .72f,
                endY = size.height,
            ),
        )

        data class Sprig(
            val x: Float,
            val y: Float,
            val scale: Float,
            val angle: Float,
            val alpha: Float,
            val leaves: Int,
        )

        listOf(
            Sprig(-.025f, .10f, .95f, 24f, .14f, 4),
            Sprig(.72f, -.015f, 1.00f, 22f, .12f, 4),
            Sprig(.93f, .17f, .62f, -28f, .10f, 3),
            Sprig(-.035f, .35f, .55f, -36f, .09f, 3),
            Sprig(.89f, .43f, .52f, 28f, .08f, 3),
            Sprig(-.025f, .69f, .86f, 30f, .15f, 4),
            Sprig(.80f, .70f, .82f, -27f, .13f, 4),
            Sprig(.31f, .86f, .46f, 20f, .07f, 3),
        ).forEach { sprig -> drawSprig(sprig.x, sprig.y, sprig.scale, sprig.angle, sprig.alpha, sprig.leaves) }

        // Very restrained warm dust gives premium depth around the lower half
        // without becoming a visible particle effect.
        val dust = listOf(
            .12f to .58f, .21f to .74f, .47f to .66f, .65f to .82f,
            .82f to .57f, .91f to .76f, .36f to .93f,
        )
        dust.forEachIndexed { index, pair ->
            val radius = if (index % 2 == 0) 1.7f else 1.2f
            drawCircle(
                color = Color(0xFFD7B35C).copy(alpha = .11f),
                radius = radius,
                center = Offset(size.width * pair.first, size.height * pair.second),
            )
        }
    }
}

private fun DrawScope.drawSprig(
    x: Float,
    y: Float,
    scale: Float,
    angle: Float,
    alpha: Float,
    leafCount: Int,
) {
    val base = Offset(size.width * x, size.height * y)
    val stemLength = 122f * scale
    val radians = Math.toRadians(angle.toDouble())
    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    val end = base + direction * stemLength

    drawLine(
        color = Color(0xFF5C9478).copy(alpha = alpha * .72f),
        start = base,
        end = end,
        strokeWidth = 1.6f * scale,
    )

    val leafW = 58f * scale
    val leafH = 23f * scale
    repeat(leafCount) { index ->
        val t = (index + 1f) / (leafCount + .25f)
        val center = base + direction * (stemLength * t)
        val side = if (index % 2 == 0) 1f else -1f
        val leafAngle = angle + side * (42f - index * 3f)
        rotate(leafAngle, center) {
            drawOval(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF4A866B).copy(alpha = alpha),
                        Color(0xFFB5D7BF).copy(alpha = alpha * .88f),
                    ),
                    start = center - Offset(leafW / 2f, 0f),
                    end = center + Offset(leafW / 2f, 0f),
                ),
                topLeft = center - Offset(leafW / 2f, leafH / 2f),
                size = Size(leafW, leafH),
            )
            drawLine(
                color = Color.White.copy(alpha = alpha * .58f),
                start = center - Offset(leafW * .30f, 0f),
                end = center + Offset(leafW * .30f, 0f),
                strokeWidth = .9f * scale,
            )
        }
    }
}

private operator fun Offset.times(value: Float): Offset = Offset(x * value, y * value)
