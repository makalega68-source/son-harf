package com.sonharf.game.ui.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import kotlin.math.sin
import kotlin.random.Random

/**
 * Slowly-drifting soft letters + dot particles background (G5.0).
 * - 10-15 faint letters float; each has its own phase.
 * - When reduced-motion is on, renders a static frame instead.
 */
@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    accent: Color = SonHarfTheme.SonHarfOrange,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val letters = remember {
        // Deterministic seed so preview looks the same across recompositions.
        val random = Random(seed = 42)
        val glyphs = "ABCDEFĞHIİKLMNOÖPRSŞTUÜVYZ"
        List(12) {
            LetterDrift(
                glyph = glyphs[random.nextInt(glyphs.length)],
                x = random.nextFloat(),
                y = random.nextFloat(),
                phase = random.nextFloat() * 6.28f,
                sizeSp = 20f + random.nextFloat() * 60f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "bg-drift")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bg-t",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SonHarfTheme.PremiumBgStart, SonHarfTheme.PremiumBgEnd),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDriftingLetters(letters, if (reducedMotion) 0f else t, accent)
        }
    }
}

private data class LetterDrift(
    val glyph: Char,
    val x: Float,
    val y: Float,
    val phase: Float,
    val sizeSp: Float,
)

private fun DrawScope.drawDriftingLetters(
    letters: List<LetterDrift>,
    t: Float,
    accent: Color,
) {
    val w = size.width
    val h = size.height
    val paintColor = accent.copy(alpha = 0.09f).toArgb()
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = paintColor
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        letters.forEach { l ->
            val dx = sin((t * 6.28f) + l.phase) * 12f
            val dy = sin((t * 6.28f * 0.7f) + l.phase * 1.3f) * 10f
            paint.textSize = l.sizeSp * density
            canvas.nativeCanvas.drawText(
                l.glyph.toString(),
                (l.x * w) + dx,
                (l.y * h) + dy,
                paint,
            )
        }
    }
}
