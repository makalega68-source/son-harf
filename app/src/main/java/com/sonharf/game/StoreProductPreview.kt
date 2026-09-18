package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ShopItemDto

/** Store previews mirror cosmetics that are actually deliverable by the runtime. */
@Composable
internal fun StoreProductPreview(
    item: ShopItemDto,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (expanded) 20.dp else 16.dp),
        color = SonHarfTheme.SurfaceSecondary,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        when (item.kind) {
            "profile_frame" -> RealFramePreview(item.id, expanded)
            "name_style" -> RealNameStylePreview(item.id, expanded)
            "keyboard_theme" -> RealKeyboardPreview(item.id, expanded)
            "game_theme" -> BlackThemePreview(expanded)
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("◆", color = SonHarfTheme.Primary, fontSize = if (expanded) 30.sp else 20.sp)
            }
        }
    }
}

@Composable
private fun RealFramePreview(frameId: String, expanded: Boolean) {
    val frameSize = if (expanded) 108.dp else 66.dp
    Box(Modifier.fillMaxSize().padding(if (expanded) 16.dp else 4.dp), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.size(frameSize * .72f), shape = CircleShape, color = SonHarfTheme.Surface) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, modifier = Modifier.size(frameSize * .40f), tint = SonHarfTheme.TextSecondary)
            }
        }
        PurchasedProfileFrameOverlay(frameId = frameId, modifier = Modifier.size(frameSize))
    }
}

@Composable
private fun RealNameStylePreview(itemId: String, expanded: Boolean) {
    val color = when (itemId) {
        "name_cyan" -> Color(0xFF2B9CB5)
        "name_sapphire" -> Color(0xFF2E6FB7)
        "name_amethyst" -> Color(0xFF7D5CA8)
        "name_aurelia" -> Color(0xFF9C742D)
        else -> SonHarfTheme.TextPrimary
    }
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(sh("Oyuncu", "Player"), color = color, fontSize = if (expanded) 25.sp else 15.sp, fontWeight = FontWeight.Black)
            if (expanded) Text(sh("Gerçek profil isim stili", "Actual profile name style"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RealKeyboardPreview(itemId: String, expanded: Boolean) {
    val palette = SonHarfCosmetics.keyboardPaletteFor(itemId)
    val rows = if (expanded) listOf("QWERTY", "ASDFG", "ZXCV") else listOf("QWE", "ASD", "ZXC")
    val crystal = itemId == "keyboard_crystal"
    val shell = if (crystal) {
        Brush.linearGradient(listOf(Color(0xFFF8FCFF), Color(0xFFC9DCE6), Color(0xFFF7FBFD)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF0E1012), Color(0xFF302A20), Color(0xFF101214)))
    }
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(shell).padding(if (expanded) 11.dp else 5.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(if (expanded) 12.dp else 8.dp),
            color = palette.background.copy(alpha = if (crystal) .68f else .84f),
            border = BorderStroke(if (expanded) 1.5.dp else 1.dp, palette.secondaryBorder),
        ) {
            Column(Modifier.fillMaxSize().padding(if (expanded) 8.dp else 3.dp), verticalArrangement = Arrangement.spacedBy(if (expanded) 6.dp else 2.dp)) {
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (expanded) 5.dp else 2.dp)) {
                        row.forEach { letter ->
                            Surface(
                                modifier = Modifier.weight(1f).height(if (expanded) 28.dp else 12.dp),
                                shape = RoundedCornerShape(if (expanded) 7.dp else 4.dp),
                                color = palette.key,
                                border = BorderStroke(1.dp, palette.border),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(letter.toString(), color = palette.text, fontSize = if (expanded) 10.sp else 5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().height(if (expanded) 22.dp else 9.dp),
                    shape = RoundedCornerShape(if (expanded) 7.dp else 4.dp),
                    color = palette.action,
                    border = BorderStroke(1.dp, palette.secondaryBorder),
                ) {}
            }
        }
    }
}

@Composable
private fun BlackThemePreview(expanded: Boolean) {
    val background = Color(0xFF090B10)
    val surface = Color(0xFF121722)
    val tile = Color(0xFF1A2230)
    val border = Color(0xFF344154)
    val blue = Color(0xFF3B82F6)
    val turquoise = Color(0xFF1FD1C2)
    val purple = Color(0xFF9B6CFF)
    val text = Color(0xFFF8FAFC)

    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(background, surface, Color(0xFF171126)))).padding(if (expanded) 13.dp else 7.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(9.dp), color = blue.copy(alpha = .18f), border = BorderStroke(1.dp, blue.copy(alpha = .60f))) {
                    Box(Modifier.size(if (expanded) 34.dp else 22.dp), contentAlignment = Alignment.Center) {
                        Text("◆", color = turquoise, fontSize = if (expanded) 17.sp else 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(7.dp))
                Column {
                    Text("BLACK THEME", color = text, fontSize = if (expanded) 13.sp else 8.sp, fontWeight = FontWeight.Black)
                    if (expanded) Text(sh("PREMIUM KOYU GÖRÜNÜM", "PREMIUM DARK LOOK"), color = purple, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("K", "U", "Ş", "A").forEachIndexed { index, letter ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (index in 1..2) blue.copy(alpha = .28f) else tile,
                        border = BorderStroke(1.dp, if (index == 2) turquoise else border),
                    ) {
                        Box(Modifier.size(if (expanded) 44.dp else 28.dp), contentAlignment = Alignment.Center) {
                            Text(letter, color = text, fontSize = if (expanded) 18.sp else 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
