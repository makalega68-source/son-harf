package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bounded presentation envelope: no timers, points or game state are changed by effects. */
internal fun arenaImpactAlpha(t: Float): Float {
    val time = t.coerceIn(0f, 1f)
    return (time / .12f).coerceAtMost(1f) * ((1f - time) / .35f).coerceAtMost(1f)
}

/** One burst per newly observed server/local move. Mounting an existing match never replays it. */
@Composable
internal fun ArenaMoveImpact(
    eventKey: String?,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    bannerTop: Dp = 140.dp,
) {
    var previous by remember { mutableStateOf(eventKey) }
    var caption by remember { mutableStateOf(label) }
    var tint by remember { mutableStateOf(accent) }
    val time = remember { Animatable(1f) }
    LaunchedEffect(eventKey) {
        if (eventKey == previous) return@LaunchedEffect
        previous = eventKey
        if (eventKey == null) { time.snapTo(1f); return@LaunchedEffect }
        caption = label; tint = accent
        time.snapTo(0f)
        time.animateTo(1f, tween(1_150, easing = LinearEasing))
    }
    val t = time.value
    val alpha = arenaImpactAlpha(t)
    if (alpha <= 0f) return
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            // Effects stay at the edges so the letters and score remain legible.
            drawRoundRect(tint.copy(alpha = .32f * alpha), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                style = Stroke((1.5f + 2f * (1f - t)).dp.toPx()))
            for (i in 0 until 16) {
                val corner = i / 4
                val origin = Offset(if (corner % 2 == 0) 10.dp.toPx() else size.width - 10.dp.toPx(),
                    if (corner < 2) size.height * .26f else size.height * .72f)
                val angle = (i % 4 * .38f + .35f) * if (corner % 2 == 0) 1f else -1f
                val travel = 12.dp.toPx() + 38.dp.toPx() * t
                val delta = Offset(sin(angle) * travel, -cos(angle) * travel + t * t * 30.dp.toPx())
                drawCircle(tint.copy(alpha = alpha * .7f), (2f * (1f - t) + .5f).dp.toPx(), origin + delta)
            }
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = bannerTop).widthIn(max = 240.dp)
            .graphicsLayer { this.alpha = alpha; translationY = (1f - t) * 10.dp.toPx(); scaleX = .94f + .06f * alpha; scaleY = scaleX }
            .background(Brush.horizontalGradient(listOf(tint.copy(alpha = .96f), Color(0xFF283329))), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFD9B77A), RoundedCornerShape(14.dp)).padding(horizontal = 15.dp, vertical = 9.dp)) {
            Text(caption, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The final five seconds breathe at the screen edge, with no input interception. */
@Composable
internal fun ArenaCriticalFrame(seconds: Int, active: Boolean, modifier: Modifier = Modifier) {
    if (!active || seconds !in 1..5) return
    val transition = rememberInfiniteTransition(label = "arena-critical")
    val pulse by transition.animateFloat(.12f, .48f,
        infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "arena-critical-edge")
    Canvas(modifier) {
        drawRoundRect(Color(0xFFC85A54).copy(alpha = pulse),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()), style = Stroke(3.dp.toPx()))
    }
}
