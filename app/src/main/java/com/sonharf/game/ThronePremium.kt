package com.sonharf.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Throne page hero: slowly turning gold rays behind a glowing crown, "TAHT" in gold letter
 * tiles and the countdown to the weekly reset.
 */
@Composable
internal fun ThroneHero(onBack: () -> Unit, resetAt: Long, now: Long) {
    val t = rememberInfiniteTransition(label = "throne-hero")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "rays")
    val glow by t.animateFloat(.6f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    Box(Modifier.fillMaxWidth().height(286.dp).premiumPanel(RoundedCornerShape(28.dp), glow = true)) {
        // Rays and light.
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width / 2, size.height * .40f)
            drawCircle(Brush.radialGradient(listOf(LobbyBrand.Gold.copy(alpha = .42f * glow), Color.Transparent), c, size.minDimension * .62f),
                size.minDimension * .62f, c)
        }
        Canvas(Modifier.matchParentSize().rotate(spin)) {
            val c = Offset(size.width / 2, size.height * .40f)
            val r = size.maxDimension
            repeat(16) { i ->
                val a = Math.toRadians(i * 22.5).toFloat()
                val a2 = a + Math.toRadians(7.0).toFloat()
                val ray = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + cos(a) * r, c.y + sin(a) * r)
                    lineTo(c.x + cos(a2) * r, c.y + sin(a2) * r)
                    close()
                }
                drawPath(ray, Brush.radialGradient(listOf(PremiumKit.GoldLight.copy(alpha = .20f), Color.Transparent), c, r * .55f))
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.padding(10.dp).size(44.dp).align(Alignment.TopStart)
            .background(LobbyBrand.NavBar.copy(alpha = .7f), CircleShape)) {
            Icon(Icons.Rounded.ChevronLeft, sh("Geri", "Back"), tint = LobbyBrand.Gold)
        }
        Column(Modifier.fillMaxSize().padding(top = 26.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(width = 120.dp, height = 92.dp)) { drawCrown(glow) }
            PremiumLetterTiles(sh("TAHT", "THRONE"), tile = 30.dp)
            Text(sh("Üç oyun · Tek haftalık yarış", "Three games · One weekly race"), color = Color.White.copy(alpha = .86f),
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(50), color = LobbyBrand.NavBar.copy(alpha = .75f),
                border = BorderStroke(1.dp, LobbyBrand.Gold.copy(alpha = .7f))) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Schedule, null, tint = LobbyBrand.Gold, modifier = Modifier.size(16.dp))
                    Text(sh("Yeni hafta: ", "New week in: ") + if (now == 0L || resetAt == 0L) "—" else tournamentClockText(resetAt, now),
                        style = TextStyle(brush = PremiumKit.goldText), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** A five-point gold crown with jewels, filling the canvas. */
private fun DrawScope.drawCrown(glow: Float) {
    val w = size.width
    val h = size.height
    val base = h * .78f
    val crown = Path().apply {
        moveTo(w * .08f, base)
        lineTo(w * .02f, h * .22f)
        lineTo(w * .27f, h * .50f)
        lineTo(w * .50f, h * .06f)
        lineTo(w * .73f, h * .50f)
        lineTo(w * .98f, h * .22f)
        lineTo(w * .92f, base)
        close()
    }
    drawPath(crown, Brush.verticalGradient(listOf(PremiumKit.GoldLight, PremiumKit.Rim, PremiumKit.GoldDeep)))
    drawPath(crown, Color(0xFF8A6418), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
    // Band.
    drawRoundRect(Brush.verticalGradient(listOf(PremiumKit.Rim, PremiumKit.GoldDeep)), Offset(w * .06f, base - h * .02f),
        Size(w * .88f, h * .18f), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
    // Jewels: ruby in the middle, teal at the sides, pearls on the tips.
    drawCircle(Color(0xFFD63A3A), h * .075f, Offset(w * .5f, base + h * .07f))
    drawCircle(Color(0xFF2FB8C9), h * .055f, Offset(w * .27f, base + h * .07f))
    drawCircle(Color(0xFF2FB8C9), h * .055f, Offset(w * .73f, base + h * .07f))
    listOf(Offset(w * .02f, h * .22f), Offset(w * .5f, h * .06f), Offset(w * .98f, h * .22f)).forEach {
        drawCircle(Color.White.copy(alpha = .55f + .45f * glow), h * .05f, it)
    }
    drawCircle(Color.White.copy(alpha = .35f * glow), h * .025f, Offset(w * .47f, base + h * .045f))
}
