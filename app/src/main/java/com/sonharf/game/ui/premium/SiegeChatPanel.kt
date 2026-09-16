package com.sonharf.game.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.data.ChatMessageDto
import com.sonharf.game.data.QuickChatKey

/**
 * G4.6 — Kelime Kuşatması maç içi sohbet paneli (serbest yazı da olur).
 *
 * Alttan yarım ekran açılır. Oyun tahtası üstte görünür kalır çünkü
 * caller'ın layout'unda üstte tahta, altta bu panel yer alır (paneli
 * scrim'siz zIndex + Modifier.fillMaxHeight(0.5f) ile gösterir).
 *
 * TXT gereksinimleri:
 *  - Üstte hazır mesajlar + emoji satırı
 *  - Altta yazı kutusu + gönder butonu
 *  - Sıra/süre DURMAZ; sıra size gelince kenar uyarısı yanıp söner
 *  - Sunucu tarafındaki G4.6 filtreler (link/telefon, minor gate,
 *    rate-limit) send yolunda otomatik uygulanır
 *
 * Kullanım örneği (caller):
 *   var open by remember { mutableStateOf(false) }
 *   SiegeChatPanel(
 *       visible = open,
 *       messages = chatMessages,
 *       meId = myId,
 *       language = language,
 *       yourTurnWhileOpen = myTurn && open,
 *       onQuickSend = { key -> scope.launch { backend.sendQuickChat(roomId, key) } },
 *       onFreeSend = { body -> scope.launch { backend.sendChat(roomId, body) } },
 *       onDismiss = { open = false },
 *   )
 */
@Composable
fun SiegeChatPanel(
    visible: Boolean,
    messages: List<ChatMessageDto>,
    meId: String?,
    language: String,
    yourTurnWhileOpen: Boolean,
    onQuickSend: (QuickChatKey) -> Unit,
    onFreeSend: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "siege-turn-flash")
    val flashAlpha by transition.animateFloat(
        initialValue = if (yourTurnWhileOpen) 0.35f else 0f,
        targetValue = if (yourTurnWhileOpen) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flash",
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxWidth()) {
            // The scrim only covers the space ABOVE the panel; the
            // playing board stays interactive because the caller draws
            // it above this composable. Tapping the scrim closes the
            // panel.
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .background(Color.Transparent)
                    .clickable(onClick = onDismiss),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SonHarfTheme.PremiumPanel)
                    .border(
                        1.dp,
                        SonHarfTheme.PremiumPanelBorder,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    )
                    .padding(14.dp),
            ) {
                // Header + turn indicator strip.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (language == "en") "MATCH CHAT" else "MAÇ SOHBETİ",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = SonHarfTheme.PremiumTextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "✕",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Black),
                        color = SonHarfTheme.PremiumTextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                if (yourTurnWhileOpen) {
                    Spacer(Modifier.height(6.dp))
                    // Kenar uyarısı: alttan yukarı doğru yanıp sönen
                    // ince şerit (TXT: "sıra gelince panel kenarında
                    // uyarı yanıp söner").
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(SonHarfTheme.KusatmaPurple.copy(alpha = flashAlpha)),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (language == "en") "It's your turn — the board is above."
                        else "Sıra sende — tahta yukarıda.",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = SonHarfTheme.KusatmaPurple.copy(alpha = 0.4f + flashAlpha * 0.6f),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Message list (scrollable). Newest at bottom.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    messages.forEach { m ->
                        MessageBubble(
                            message = m,
                            mine = m.senderId == meId,
                            language = language,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Quick messages row (chunked).
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickChatKey.quickMessages.take(3).forEach { key ->
                        QuickPill(
                            label = key.label(language),
                            onClick = { onQuickSend(key) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickChatKey.emojis.forEach { key ->
                        QuickPill(
                            label = key.label(language),
                            onClick = { onQuickSend(key) },
                            modifier = Modifier.weight(1f),
                            emojiStyle = true,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Free-text input + send.
                var draft by remember { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { new -> if (new.length <= 200) draft = new },
                            singleLine = false,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = SonHarfTheme.PremiumTextPrimary,
                            ),
                            cursorBrush = SolidColor(SonHarfTheme.KusatmaPurple),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (draft.isEmpty()) {
                            Text(
                                if (language == "en") "Type a message…" else "Mesaj yaz…",
                                style = TextStyle(fontSize = 13.sp),
                                color = SonHarfTheme.PremiumTextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (draft.isBlank()) SonHarfTheme.PremiumPanel
                                else SonHarfTheme.KusatmaPurple,
                            )
                            .clickable(enabled = draft.isNotBlank()) {
                                val body = draft.trim()
                                if (body.isNotEmpty()) {
                                    onFreeSend(body)
                                    draft = ""
                                }
                            }
                            .padding(10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = "Gönder",
                            tint = if (draft.isBlank()) SonHarfTheme.PremiumTextSecondary else Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageDto,
    mine: Boolean,
    language: String,
) {
    val label = QuickChatKey.byServerKey(message.messageKey)?.label(language)
        ?: message.body
    val align = if (mine) Alignment.CenterEnd else Alignment.CenterStart
    val bg = if (mine) SonHarfTheme.KusatmaPurple.copy(alpha = 0.28f)
    else Color.White.copy(alpha = 0.08f)
    Box(Modifier.fillMaxWidth(), contentAlignment = align) {
        Text(
            label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = SonHarfTheme.PremiumTextPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun QuickPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emojiStyle: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SonHarfTheme.KusatmaPurple.copy(alpha = 0.18f))
            .border(
                1.dp,
                SonHarfTheme.KusatmaPurple.copy(alpha = 0.45f),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = if (emojiStyle) 10.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = if (emojiStyle) 20.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = SonHarfTheme.PremiumTextPrimary,
        )
    }
}
