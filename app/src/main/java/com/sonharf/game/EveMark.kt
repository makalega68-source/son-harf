package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Lightweight white Eve fallback used whenever the GLB asset is not packaged.
 * It deliberately stays code-only so debug APKs always have a visible companion.
 */
@Composable
internal fun EveMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val fur = Color(0xFFF8FFF8)
        val furShadow = Color(0xFFDCEFD9)
        val mint = Color(0xFF9BCFA4)
        val mintDeep = Color(0xFF5E9E72)
        val eye = Color(0xFF5C8A36)
        val ink = Color(0xFF173B35)
        val pink = Color(0xFFE9A9A6)

        fun oval(color: Color, l: Float, t: Float, r: Float, b: Float) {
            drawOval(color = color, topLeft = Offset(l, t), size = Size(r - l, b - t))
        }

        // Tail behind the body.
        drawArc(
            color = furShadow,
            startAngle = 210f,
            sweepAngle = 245f,
            useCenter = false,
            topLeft = Offset(w * .08f, h * .47f),
            size = Size(w * .48f, h * .38f),
            style = Stroke(width = w * .12f),
        )

        // Body and chest.
        oval(furShadow, w * .29f, h * .43f, w * .71f, h * .91f)
        oval(fur, w * .33f, h * .45f, w * .67f, h * .88f)
        oval(Color.White, w * .40f, h * .55f, w * .60f, h * .82f)

        // Ears.
        val leftEar = Path().apply {
            moveTo(w * .24f, h * .31f)
            lineTo(w * .30f, h * .03f)
            lineTo(w * .47f, h * .25f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(w * .53f, h * .25f)
            lineTo(w * .70f, h * .03f)
            lineTo(w * .76f, h * .31f)
            close()
        }
        drawPath(leftEar, fur)
        drawPath(rightEar, fur)
        val leftInner = Path().apply {
            moveTo(w * .29f, h * .24f)
            lineTo(w * .32f, h * .10f)
            lineTo(w * .41f, h * .24f)
            close()
        }
        val rightInner = Path().apply {
            moveTo(w * .59f, h * .24f)
            lineTo(w * .68f, h * .10f)
            lineTo(w * .71f, h * .24f)
            close()
        }
        drawPath(leftInner, mint.copy(alpha = .55f))
        drawPath(rightInner, mint.copy(alpha = .55f))

        // Head + soft leafy tufts.
        oval(furShadow, w * .19f, h * .17f, w * .81f, h * .61f)
        oval(fur, w * .22f, h * .18f, w * .78f, h * .59f)
        repeat(5) { i ->
            val x = w * (.38f + i * .06f)
            drawCircle(if (i % 2 == 0) fur else furShadow, w * .065f, Offset(x, h * .18f - (i % 2) * h * .025f))
        }

        // Eyes.
        oval(Color.White, w * .29f, h * .30f, w * .45f, h * .45f)
        oval(Color.White, w * .55f, h * .30f, w * .71f, h * .45f)
        drawCircle(eye, w * .052f, Offset(w * .38f, h * .375f))
        drawCircle(eye, w * .052f, Offset(w * .62f, h * .375f))
        drawCircle(ink, w * .027f, Offset(w * .38f, h * .38f))
        drawCircle(ink, w * .027f, Offset(w * .62f, h * .38f))
        drawCircle(Color.White, w * .012f, Offset(w * .365f, h * .36f))
        drawCircle(Color.White, w * .012f, Offset(w * .605f, h * .36f))

        // Nose, smile and cheeks.
        drawCircle(mintDeep, w * .023f, Offset(w * .50f, h * .455f))
        drawArc(color = ink, startAngle = 15f, sweepAngle = 120f, useCenter = false, topLeft = Offset(w * .43f, h * .445f), size = Size(w * .07f, h * .07f), style = Stroke(w * .012f))
        drawArc(color = ink, startAngle = 45f, sweepAngle = 120f, useCenter = false, topLeft = Offset(w * .50f, h * .445f), size = Size(w * .07f, h * .07f), style = Stroke(w * .012f))
        drawCircle(pink.copy(alpha = .35f), w * .033f, Offset(w * .30f, h * .47f))
        drawCircle(pink.copy(alpha = .35f), w * .033f, Offset(w * .70f, h * .47f))

        // Feet and waving paw.
        oval(furShadow, w * .26f, h * .81f, w * .47f, h * .94f)
        oval(furShadow, w * .53f, h * .81f, w * .74f, h * .94f)
        oval(fur, w * .69f, h * .49f, w * .86f, h * .69f)
        drawCircle(pink.copy(alpha = .72f), w * .031f, Offset(w * .79f, h * .57f))
        repeat(3) { i ->
            drawCircle(pink.copy(alpha = .62f), w * .014f, Offset(w * (.745f + i * .04f), h * .535f))
        }

        // Leaf-shaped forehead accent; it keeps Eve recognizable without changing white fur.
        oval(mint.copy(alpha = .85f), w * .455f, h * .17f, w * .505f, h * .25f)
        oval(mint.copy(alpha = .65f), w * .505f, h * .18f, w * .555f, h * .25f)
    }
}
