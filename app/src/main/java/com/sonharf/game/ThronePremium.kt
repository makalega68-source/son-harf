package com.sonharf.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Throne page hero: the Canva throne artwork across the top, a gold pulse of light over it,
 * "TAHT" in gold letter tiles and the countdown to the weekly reset below.
 */
@Composable
internal fun ThroneHero(onBack: () -> Unit, resetAt: Long, now: Long) {
    val t = rememberInfiniteTransition(label = "throne-hero")
    val glow by t.animateFloat(.55f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    Box(Modifier.fillMaxWidth().height(318.dp).premiumPanel(RoundedCornerShape(28.dp), glow = true)) {
        Image(rememberArtPainter(R.drawable.throne_hero_art, ArtCache.HERO_MAX_WIDTH_PX), sh("Altın taht", "Golden throne"),
            contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth().height(232.dp))
        // Breathing light on the throne, then a fade into the panel so the text below stays readable.
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width / 2, size.height * .30f)
            drawCircle(Brush.radialGradient(listOf(PremiumKit.GoldLight.copy(alpha = .22f * glow), Color.Transparent), c, size.minDimension * .55f),
                size.minDimension * .55f, c)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, PremiumKit.PanelBottom.copy(alpha = .92f), PremiumKit.PanelBottom),
                startY = size.height * .45f, endY = size.height * .78f))
        }
        // The Altın Kral sits on the throne, alive and scattering gold sparkles.
        GoldKingOnThrone(Modifier.align(Alignment.TopCenter).padding(top = 44.dp).size(196.dp, 160.dp))
        IconButton(onClick = onBack, modifier = Modifier.padding(10.dp).size(44.dp).align(Alignment.TopStart)
            .background(LobbyBrand.NavBar.copy(alpha = .7f), CircleShape)) {
            Icon(Icons.Rounded.ChevronLeft, sh("Geri", "Back"), tint = LobbyBrand.Gold)
        }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumLetterTiles(sh("TAHT", "THRONE"), tile = 30.dp)
            Text(sh("Üç oyun · Tek haftalık yarış", "Three games · One weekly race"), color = Color.White.copy(alpha = .88f),
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(50), color = LobbyBrand.NavBar.copy(alpha = .8f),
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


/** The king of mascots, enthroned: regal idle moves, a gold halo and twinkling sparkles around it. */
@Composable
internal fun GoldKingOnThrone(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "gold-king")
    val clock by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "clock")
    val halo by t.animateFloat(.6f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "halo")
    var actionKey by remember { mutableLongStateOf(0L) }
    var action by remember { mutableStateOf<WordSiegeMascotAction?>(null) }
    LaunchedEffect(Unit) {
        // Slow, sovereign moves only: a measured look over the realm, an approving nod, a gleam.
        val moves = listOf(WordSiegeMascotAction.LOOK_AROUND, WordSiegeMascotAction.NOD,
            WordSiegeMascotAction.SPARKLE, WordSiegeMascotAction.LOOK_AROUND)
        var i = 0
        delay(1_200)
        while (true) {
            action = moves[i % moves.size]; actionKey += 1; i++
            delay(6_500)
        }
    }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width / 2, size.height * .45f)
            val r = size.height * .55f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE7A3).copy(alpha = .38f * halo), Color(0xFFFFC83C).copy(alpha = .12f), Color.Transparent), c, r), r, c)
        }
        WordSiegeMascot(
            moveId = null, lastMoveMine = false, pendingCells = emptyList(), playerTurn = false,
            requestedEmotion = WordSiegeMascotEmotion.PROUD,
            modifier = Modifier.padding(top = 6.dp).size(124.dp),
            actionKey = actionKey, action = action,
            hat = WordSiegeMascotHat.NONE, skin = WordSiegeMascotSkin.GOLD_KING,
            onTap = { action = WordSiegeMascotAction.NOD; actionKey += 1 },
        )
        // Twinkling four-point stars and gold motes drifting up around the king.
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2
            val cy = size.height * .45f
            for (k in 0 until 14) {
                val seed = k * 0.6180339f
                val phase = ((clock + seed) % 1f)
                val angle = (seed * 6.2832f * 3f) + clock * 6.2832f * (if (k % 2 == 0) .25f else -.18f)
                val radius = size.height * (.36f + (k % 4) * .07f)
                val p = Offset(cx + kotlin.math.cos(angle) * radius * 1.15f, cy + kotlin.math.sin(angle) * radius * .82f)
                val twinkle = kotlin.math.sin(phase * 6.2832f).let { it * it }
                val arm = (3.5f + (k % 3) * 2.2f) * density * (.4f + .6f * twinkle)
                val col = if (k % 3 == 0) Color.White else Color(0xFFFFE07A)
                sparkleStar(p, arm, col.copy(alpha = .35f + .65f * twinkle))
            }
            for (k in 0 until 10) {
                val seed = (k * 0.381966f) % 1f
                val life = (clock * 1.6f + seed) % 1f
                val x = cx + (seed - .5f) * size.width * .8f + kotlin.math.sin((life + seed) * 9f) * 6f * density
                val y = size.height * (.95f - life * .9f)
                drawCircle(Color(0xFFFFD86A).copy(alpha = (1f - life) * .85f), (1.2f + seed * 1.6f) * density, Offset(x, y))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.sparkleStar(c: Offset, arm: Float, color: Color) {
    val w = arm * .22f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(c.x, c.y - arm)
        quadraticTo(c.x + w, c.y - w, c.x + arm, c.y)
        quadraticTo(c.x + w, c.y + w, c.x, c.y + arm)
        quadraticTo(c.x - w, c.y + w, c.x - arm, c.y)
        quadraticTo(c.x - w, c.y - w, c.x, c.y - arm)
        close()
    }
    drawCircle(color.copy(alpha = color.alpha * .35f), arm * .9f, c)
    drawPath(path, color)
}
