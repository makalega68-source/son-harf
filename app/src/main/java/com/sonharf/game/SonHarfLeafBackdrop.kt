package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate

/** Calm botanical background shared by the light Son Harf surfaces. */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFCFDF8), Color(0xFFF5F8F0), Color(0xFFE5F1E8)),
            ),
        )

        val wave = Path().apply {
            moveTo(0f, size.height * .62f)
            cubicTo(size.width * .22f, size.height * .54f, size.width * .54f, size.height * .80f, size.width, size.height * .65f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(wave, Color(0x335FAF8D))

        val wave2 = Path().apply {
            moveTo(0f, size.height * .76f)
            cubicTo(size.width * .31f, size.height * .66f, size.width * .55f, size.height * .92f, size.width, size.height * .77f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(wave2, Color(0x286FAF9C))

        data class Leaf(val x: Float, val y: Float, val scale: Float, val angle: Float, val alpha: Int)
        val leaves = listOf(
            Leaf(-.03f, .13f, 1.25f, 38f, 54), Leaf(.29f, .08f, .52f, 34f, 35),
            Leaf(.72f, -.02f, 1.35f, 42f, 42), Leaf(.90f, .10f, .60f, -34f, 50),
            Leaf(.04f, .34f, .58f, -38f, 40), Leaf(.86f, .31f, .50f, 28f, 34),
            Leaf(.10f, .69f, 1.08f, 38f, 54), Leaf(.63f, .76f, .52f, -26f, 42),
            Leaf(.83f, .69f, 1.02f, -32f, 58), Leaf(.38f, .86f, .62f, 26f, 38),
        )
        leaves.forEach { leaf ->
            val center = Offset(size.width * leaf.x, size.height * leaf.y)
            val leafSize = Size(92f * leaf.scale, 38f * leaf.scale)
            rotate(leaf.angle, center) {
                drawOval(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF397B60).copy(alpha = leaf.alpha / 255f), Color(0xFF9AC9A7).copy(alpha = leaf.alpha / 255f)),
                        start = center - Offset(leafSize.width / 2f, 0f),
                        end = center + Offset(leafSize.width / 2f, 0f),
                    ),
                    topLeft = center - Offset(leafSize.width / 2f, leafSize.height / 2f),
                    size = leafSize,
                )
                drawLine(
                    color = Color.White.copy(alpha = .22f),
                    start = center - Offset(leafSize.width * .36f, 0f),
                    end = center + Offset(leafSize.width * .36f, 0f),
                    strokeWidth = 1.2f,
                )
            }
        }
    }
}
