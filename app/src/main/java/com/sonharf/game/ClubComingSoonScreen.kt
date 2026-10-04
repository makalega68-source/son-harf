package com.sonharf.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Kulüp tab: the club feature is not open yet, so the page is one calm "YAKINDA" stage. */
@Composable
internal fun ClubComingSoonScreen() {
    val glow = rememberInfiniteTransition(label = "club-soon")
    val pulse by glow.animateFloat(.55f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val word = sh("YAKINDA", "SOON")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LobbyBrand.Sky, LobbyBrand.Band, LobbyBrand.NavBar))),
        contentAlignment = Alignment.Center) {
        // The same word-board grid as the lobby, fading out towards the bottom.
        Canvas(Modifier.matchParentSize()) {
            val cell = 44.dp.toPx()
            val line = LobbyBrand.Grid.copy(alpha = .30f)
            var x = 0f
            while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += cell }
            var y = 0f
            while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += cell }
            drawRect(Brush.radialGradient(listOf(LobbyBrand.Gold.copy(alpha = .16f * pulse), Color.Transparent),
                center = Offset(size.width / 2, size.height * .42f), radius = size.minDimension * .7f))
        }
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(104.dp).shadow(18.dp, CircleShape).clip(CircleShape)
                .background(Brush.radialGradient(listOf(LobbyBrand.Chip, LobbyBrand.Band)))
                .border(2.dp, LobbyBrand.Gold, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Groups, null, tint = LobbyBrand.Gold, modifier = Modifier.size(56.dp))
            }
            Text(sh("KULÜP", "CLUB"), color = Color.White.copy(alpha = .85f), fontSize = 15.sp,
                fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            // The word laid out as gold letter tiles, gently breathing.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer { scaleX = .97f + .03f * pulse; scaleY = .97f + .03f * pulse }) {
                word.forEach { ch ->
                    Box(Modifier.size(width = 38.dp, height = 46.dp).shadow(6.dp, RoundedCornerShape(9.dp))
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE7A3), LobbyBrand.Gold, Color(0xFFC8962F))))
                        .border(1.dp, Color(0xFFFFF3CF), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                        Text(ch.toString(), color = Color(0xFF3A2A00), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(sh("Arkadaşlarınla kulüp kur, birlikte yarış ve kulüp sıralamasında zirveye çık. Çok yakında burada.",
                "Start a club with your friends, compete together and climb the club ranking. Coming here soon."),
                color = Color.White.copy(alpha = .82f), fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp))
            Surface(shape = RoundedCornerShape(50), color = LobbyBrand.Gold.copy(alpha = .14f),
                border = BorderStroke(1.dp, LobbyBrand.Gold.copy(alpha = .6f))) {
                Text(sh("Hazırlanıyor", "In the works"), Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = LobbyBrand.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
