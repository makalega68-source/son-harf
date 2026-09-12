package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

/**
 * Store previews are built only from runtime-deliverable cosmetics.
 * No placeholder artwork, system emoji pack, or illustration-only product is rendered here.
 */
@Composable
internal fun StoreProductPreview(
    item: ShopItemDto,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    val shape = RoundedCornerShape(if (expanded) 20.dp else 16.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = SonHarfTheme.SurfaceSecondary,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        when (item.kind) {
            "profile_frame" -> RealFramePreview(item.id, expanded)
            "name_style" -> RealNameStylePreview(item.id, expanded)
            "keyboard_theme" -> RealKeyboardPreview(expanded)
            "game_theme" -> RealDarkArenaThemePreview(expanded)
        }
    }
}

@Composable
private fun RealFramePreview(frameId: String, expanded: Boolean) {
    val frameSize = if (expanded) 108.dp else 66.dp
    Box(
        Modifier.fillMaxSize().padding(if (expanded) 16.dp else 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(frameSize * .72f),
            shape = CircleShape,
            color = SonHarfTheme.Surface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(frameSize * .40f),
                    tint = SonHarfTheme.TextSecondary,
                )
            }
        }
        PurchasedProfileFrameOverlay(
            frameId = frameId,
            modifier = Modifier.size(frameSize),
        )
    }
}

@Composable
private fun RealNameStylePreview(itemId: String, expanded: Boolean) {
    val color = if (itemId == "name_cyan") SonHarfCyan else SonHarfText
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = sh("Oyuncu", "Player"),
                color = color,
                fontSize = if (expanded) 25.sp else 15.sp,
                fontWeight = FontWeight.Black,
            )
            if (expanded) {
                Text(
                    text = sh("Profilde görünen gerçek yazı stili", "Actual profile name style"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun RealKeyboardPreview(expanded: Boolean) {
    Box(
        Modifier.fillMaxSize().padding(if (expanded) 8.dp else 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (expanded) {
            EmbeddedWordKeyboard(
                value = "",
                language = if (SonHarfUiState.isEnglish) "en" else "tr",
                enabled = false,
                submitEnabled = false,
                maxLength = 12,
                onValueChange = {},
                onSubmit = {},
                compact = true,
                keySound = {},
                actionSound = {},
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("Q", "W", "E").forEach { letter ->
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFF121833),
                        border = BorderStroke(1.dp, Color(0xFF8A5CFF).copy(alpha = .32f)),
                    ) {
                        Text(
                            letter,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                            color = Color(0xFFF7F8FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealDarkArenaThemePreview(expanded: Boolean) {
    // These values intentionally mirror SonHarfTheme's active dark-arena runtime tokens.
    val background = Color(0xFF101914)
    val surface = Color(0xFF18241E)
    val surface2 = Color(0xFF213129)
    val primary = Color(0xFF557B67)
    val text = Color(0xFFF3F6F2)
    val muted = Color(0xFFB8C8BF)
    val tile = Color(0xFF4A4336)
    val tileBorder = Color(0xFF817150)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(background, surface2)))
            .padding(if (expanded) 13.dp else 7.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = surface2) {
                    Box(Modifier.size(if (expanded) 34.dp else 22.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, null, tint = muted, modifier = Modifier.size(if (expanded) 20.dp else 13.dp))
                    }
                }
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(sh("GECE ARENASI", "NIGHT ARENA"), color = text, fontSize = if (expanded) 13.sp else 8.sp, fontWeight = FontWeight.Black)
                    if (expanded) Text(sh("Gerçek uygulama renkleri", "Actual in-app colors"), color = muted, fontSize = 8.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("K", "U", "Ş").forEachIndexed { index, letter ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (index == 1) primary.copy(alpha = .45f) else tile,
                        border = BorderStroke(1.dp, if (index == 1) primary else tileBorder),
                    ) {
                        Box(
                            Modifier.size(if (expanded) 44.dp else 28.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(letter, color = text, fontSize = if (expanded) 18.sp else 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
