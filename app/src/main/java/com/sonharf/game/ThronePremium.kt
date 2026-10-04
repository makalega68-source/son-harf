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
import androidx.compose.runtime.getValue
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
        Image(painterResource(R.drawable.throne_hero_art), sh("Altın taht", "Golden throne"),
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

