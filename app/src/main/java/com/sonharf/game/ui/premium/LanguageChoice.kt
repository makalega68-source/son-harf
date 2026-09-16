package com.sonharf.game.ui.premium

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfPreferences
import com.sonharf.game.SonHarfTheme

/**
 * G5.4 — Her oyunun giriş ekranında "Bu maçı hangi dilde oynayacağım?"
 * seçici. İki büyük, bayrak ikonlu düğme; seçim SonHarfPreferences.
 * gameLanguage üzerinden cihazda hatırlanır. Varsayılan Türkçe.
 *
 * Callback tetiklenir; caller state'ini günceller ve seçili dili
 * eşleştirmeye + validate_game_word_v3'e language olarak gönderir.
 */
@Composable
fun LanguageChoiceRow(
    selected: String,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
    uiLanguage: String = "tr",
) {
    val context = LocalContext.current
    // Persist changes on tap. Caller may also drive from an outer state
    // (e.g. per-game override); the pref stays as a global default.
    fun pick(value: String) {
        onChoose(value)
        SonHarfPreferences.setGameLanguage(context, value)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LanguagePill(
            flag = "🇹🇷",
            title = "TÜRKÇE",
            subtitle = if (uiLanguage == "en") "Turkish dictionary" else "Türkçe sözlük",
            selected = selected == "tr",
            accent = SonHarfTheme.SonHarfOrange,
            modifier = Modifier.weight(1f),
            onClick = { pick("tr") },
        )
        LanguagePill(
            flag = "🇬🇧",
            title = "ENGLISH",
            subtitle = if (uiLanguage == "en") "English dictionary" else "İngilizce sözlük",
            selected = selected == "en",
            accent = SonHarfTheme.DiamondBlue,
            modifier = Modifier.weight(1f),
            onClick = { pick("en") },
        )
    }
}

@Composable
private fun LanguagePill(
    flag: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgAlpha = if (selected) 0.22f else 0.08f
    val borderAlpha = if (selected) 1f else 0.35f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = bgAlpha))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = accent.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(flag, style = TextStyle(fontSize = 28.sp))
        Text(
            title,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            ),
            color = SonHarfTheme.PremiumTextPrimary,
        )
        Text(
            subtitle,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = SonHarfTheme.PremiumTextSecondary,
        )
    }
}

/**
 * G5.4 — Ekranda oyun dilini gösteren küçük rozet: TR / EN.
 * Match arena üstünde, giriş ekranlarında, sonuç ekranında kullanılır.
 */
@Composable
fun GameLanguageBadge(
    language: String,
    modifier: Modifier = Modifier,
) {
    val label = if (language == "en") "EN" else "TR"
    val accent = if (language == "en") SonHarfTheme.DiamondBlue else SonHarfTheme.SonHarfOrange
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (language == "en") "🇬🇧" else "🇹🇷",
                style = TextStyle(fontSize = 10.sp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                ),
                color = accent,
            )
        }
    }
}

/**
 * Convenience: fetch the persisted game language once at composition
 * time and expose it as immediate state. Caller can `mutableStateOf`
 * this and mutate via LanguageChoiceRow's onChoose.
 */
@Composable
fun rememberGameLanguage(): String {
    val context = LocalContext.current
    var lang by remember { mutableStateOf(SonHarfPreferences.gameLanguage(context)) }
    LaunchedEffect(Unit) {
        // Re-read in case another screen changed it after the first
        // composition (cheap; no observer needed for a rare toggle).
        lang = SonHarfPreferences.gameLanguage(context)
    }
    return lang
}
