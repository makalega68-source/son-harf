package com.sonharf.game.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.data.QuickChatKey

/**
 * G4.6 — MAÇ İÇİ HIZLI SOHBET sheet.
 *
 * Bottom-half sheet (fake modal — caller owns the enter/exit); shows
 * 6 hazır mesaj + 3 emoji. Serbest yazı burada YOK (spec: hızlı
 * mesajlar açılır, serbest yazı YOK). Kuşatma serbest yazı için
 * ayrı bir composable yapılacak.
 *
 * Kullanım:
 *   var open by remember { mutableStateOf(false) }
 *   IconButton(onClick = { open = true }) { balon ikonu }
 *   QuickChatSheet(open, language, onSend = { key ->
 *       scope.launch { backend.sendQuickChat(roomId, key) }
 *       open = false
 *   }, onDismiss = { open = false }, onMuteOpponent = { ... })
 */
@Composable
fun QuickChatSheet(
    visible: Boolean,
    language: String,
    onSend: (QuickChatKey) -> Unit,
    onDismiss: () -> Unit,
    onMuteOpponent: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
    ) {
        Box(Modifier.fillMaxWidth()) {
            // Dim scrim above content; tap to dismiss.
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SonHarfTheme.PremiumPanel)
                    .border(
                        1.dp,
                        SonHarfTheme.PremiumPanelBorder,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (language == "en") "QUICK CHAT" else "HIZLI SOHBET",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = SonHarfTheme.PremiumTextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (onMuteOpponent != null) {
                        Text(
                            "🔇 " + (if (language == "en") "MUTE" else "SUSTUR"),
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = SonHarfTheme.PremiumTextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .clickable {
                                    onMuteOpponent()
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                // Quick messages, wrap-style 2-column-ish rows.
                QuickChatKey.quickMessages.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { key ->
                            QuickChatPill(
                                text = key.label(language),
                                accent = SonHarfTheme.SonHarfOrange,
                                modifier = Modifier.weight(1f),
                                onClick = { onSend(key) },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                // Emoji row.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickChatKey.emojis.forEach { emo ->
                        QuickChatPill(
                            text = emo.label(language),
                            accent = SonHarfTheme.KusatmaPurple,
                            modifier = Modifier.weight(1f),
                            large = true,
                            onClick = { onSend(emo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickChatPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (large) 14.dp else 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(
                fontSize = if (large) 22.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = SonHarfTheme.PremiumTextPrimary,
        )
    }
}
