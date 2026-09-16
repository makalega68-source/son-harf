package com.sonharf.game.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme

/**
 * G5.5 — "Nasıl oynanır?" açılır kartı.
 *
 * 3 kısa madde + her maddenin başında küçük bir emoji "çizim".
 * Kart varsayılan kapalı; başlığa tıklandığında açılır/kapanır.
 */
@Composable
fun HowToPlayCard(
    steps: List<HowToStep>,
    modifier: Modifier = Modifier,
    title: String = "NASIL OYNANIR?",
    accent: Color = SonHarfTheme.GoldBright,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SonHarfTheme.PremiumPanel.copy(alpha = 0.85f))
            .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("❓", style = TextStyle(fontSize = 18.sp))
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                ),
                color = SonHarfTheme.PremiumTextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (expanded) "▲" else "▼",
                style = TextStyle(fontSize = 12.sp),
                color = accent,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(accent.copy(alpha = 0.22f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "${index + 1}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                ),
                                color = accent,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            step.icon,
                            style = TextStyle(fontSize = 22.sp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            step.text,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp,
                            ),
                            color = SonHarfTheme.PremiumTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** One instruction step: an emoji "icon" + the sentence. */
data class HowToStep(
    val icon: String,
    val text: String,
)
