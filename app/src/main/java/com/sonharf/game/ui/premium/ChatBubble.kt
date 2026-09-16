package com.sonharf.game.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.data.QuickChatKey
import kotlinx.coroutines.delay

/**
 * G4.6 — Rakibin kartının üstünde 2 saniyeliğine görünen sohbet
 * balonu. Aynı mesaj key + timestamp kombinasyonu ile yeniden
 * tetiklenirse yeniden görünür.
 *
 * Sustur (mute) etkinse çağıran taraf bu composable'ı çağırmamalı.
 */
@Composable
fun ChatBubble(
    messageKey: String?,
    body: String?,
    triggerToken: Any?,
    language: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(triggerToken) {
        if (triggerToken != null && (messageKey != null || !body.isNullOrBlank())) {
            visible = true
            delay(2000)
            visible = false
        }
    }
    val label = QuickChatKey.byServerKey(messageKey)?.label(language)
        ?: body.orEmpty().ifEmpty { return }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = SonHarfTheme.PremiumTextPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
