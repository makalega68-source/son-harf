package com.sonharf.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium menu kit: the lobby's petrol word-board backdrop, gold-rimmed panels and gold
 * headings, shared by every menu page so none of them looks plain next to the lobby.
 */
internal object PremiumKit {
    val PanelTop = Color(0xFF175D6E)
    val PanelBottom = Color(0xFF0C3540)
    val Rim = Color(0xFFF2C14E)
    val GoldLight = Color(0xFFFFE7A3)
    val GoldDeep = Color(0xFFC8962F)
    val goldText: Brush get() = Brush.verticalGradient(listOf(GoldLight, Rim, GoldDeep))
}

/** Petrol gradient with the faint board grid and a soft gold light at the top. */
@Composable
internal fun PremiumMenuBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.background(Brush.verticalGradient(listOf(LobbyBrand.Sky, LobbyBrand.Band, LobbyBrand.NavBar)))) {
        val cell = 44.dp.toPx()
        val line = LobbyBrand.Grid.copy(alpha = .28f)
        var x = 0f
        while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += cell }
        var y = 0f
        while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += cell }
        drawRect(Brush.radialGradient(listOf(LobbyBrand.Gold.copy(alpha = .13f), Color.Transparent),
            center = Offset(size.width * .5f, 0f), radius = size.width * .9f))
    }
}

/** Page ground: transparent over the premium backdrop, the solid palette ground otherwise. */
internal fun Modifier.menuGround(): Modifier =
    if (SonHarfCosmetics.petrolMenus) this else this.background(LobbyPalette.Ground)

/** Deep petrol panel with a gold hairline rim, the card style of every premium menu page. */
internal fun Modifier.premiumPanel(shape: Shape = RoundedCornerShape(20.dp), glow: Boolean = false): Modifier =
    this.shadow(if (glow) 10.dp else 4.dp, shape, ambientColor = LobbyBrand.Gold.copy(alpha = .25f), spotColor = Color.Black)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(PremiumKit.PanelTop, PremiumKit.PanelBottom)))
        .border(BorderStroke(if (glow) 1.5.dp else 1.dp, Brush.verticalGradient(listOf(PremiumKit.GoldLight.copy(alpha = .9f), PremiumKit.Rim.copy(alpha = .35f)))), shape)

/** A gold rule with a small diamond in the middle, under premium headings. */
@Composable
internal fun PremiumRule(modifier: Modifier = Modifier, width: Dp = 140.dp) {
    Canvas(modifier.width(width).height(10.dp)) {
        val cy = size.height / 2
        val mid = size.width / 2
        val d = 4.dp.toPx()
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, PremiumKit.Rim)), Offset(0f, cy), Offset(mid - d * 2, cy), 1.5.dp.toPx())
        drawLine(Brush.horizontalGradient(listOf(PremiumKit.Rim, Color.Transparent), startX = mid + d * 2, endX = size.width),
            Offset(mid + d * 2, cy), Offset(size.width, cy), 1.5.dp.toPx())
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(mid, cy - d); lineTo(mid + d, cy); lineTo(mid, cy + d); lineTo(mid - d, cy); close()
        }
        drawPath(path, PremiumKit.Rim)
    }
}

/** Gold letter tiles spelling a short word, slowly bobbing; used in hero headers. */
@Composable
internal fun PremiumLetterTiles(word: String, tile: Dp = 30.dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "tiles")
    val phase by t.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "phase")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        word.forEachIndexed { i, ch ->
            val lift = kotlin.math.sin(phase + i * .7f) * 3f
            Box(Modifier.graphicsLayer { translationY = lift * density }.size(tile, tile * 1.15f)
                .shadow(5.dp, RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp))
                .background(PremiumKit.goldText).border(1.dp, Color(0xFFFFF3CF), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(ch.toString(), color = Color(0xFF3A2A00),
                    fontSize = (tile.value * .58f).sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
