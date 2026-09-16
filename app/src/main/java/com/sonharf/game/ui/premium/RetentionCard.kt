package com.sonharf.game.ui.premium

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.data.DailyStreakDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SeasonalThemeDto
import com.sonharf.game.data.WordCollectionDto
import com.sonharf.game.data.getActiveSeasonalTheme
import com.sonharf.game.data.getDailyStreak
import com.sonharf.game.data.getWordCollection

/**
 * G4.4 — Geri dönme (retention) kartı.
 *
 * Üç dilim: 🔥 günlük seri, ay içinde eklenen farklı kelime sayısı,
 * ve aktif mevsimsel tema rozeti. Hiçbir "seri kurtarma" satışı YOK
 * (spec: kaçırılan gün için para/elmas istemez). Haftada 1 kaçırma
 * grace'i sunucuda otomatik uygulanır; kart sadece bilgi verir.
 *
 * Self-loading: sadece bir kere yerleştir, verileri kendi çeker.
 */
@Composable
fun RetentionCard(
    modifier: Modifier = Modifier,
    language: String = "tr",
    refreshTick: Int = 0,
) {
    val backend = remember { OnlineGameBackend() }
    var streak by remember { mutableStateOf<DailyStreakDto?>(null) }
    var collection by remember { mutableStateOf<WordCollectionDto?>(null) }
    var theme by remember { mutableStateOf<SeasonalThemeDto?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        loaded = false
        streak = runCatching { backend.getDailyStreak() }.getOrNull()
        collection = runCatching { backend.getWordCollection() }.getOrNull()
        theme = runCatching { backend.getActiveSeasonalTheme() }.getOrNull()
        loaded = true
    }

    GamePanel(
        modifier = modifier.fillMaxWidth(),
        title = if (language == "en") "KEEP COMING BACK" else "GERİ DÖNME",
        accent = SonHarfTheme.SonHarfOrange,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                !loaded -> {
                    Text(
                        if (language == "en") "Loading…" else "Yükleniyor…",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = SonHarfTheme.PremiumTextSecondary,
                    )
                }
                streak == null -> {
                    Text(
                        if (language == "en") "No stats yet." else "Henüz istatistik verisi yok.",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = SonHarfTheme.PremiumTextSecondary,
                    )
                }
                else -> {
                    val s = streak!!
                    StreakRow(s, language)
                    val col = collection
                    if (col != null) {
                        Divider()
                        CollectionRow(col, language)
                    }
                    val t = theme
                    if (t != null) {
                        Divider()
                        SeasonalRow(t, language)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakRow(s: DailyStreakDto, language: String) {
    val streakColor = if (s.currentStreak > 0) SonHarfTheme.SonHarfOrange
    else SonHarfTheme.PremiumTextSecondary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🔥", style = TextStyle(fontSize = 26.sp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (language == "en") "Daily streak" else "Günlük seri",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = SonHarfTheme.PremiumTextSecondary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    s.currentStreak.toString(),
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black),
                    color = streakColor,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (language == "en") "days" else "gün",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = SonHarfTheme.PremiumTextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                buildString {
                    if (s.longestStreak > s.currentStreak) {
                        append(
                            if (language == "en") "Best: ${s.longestStreak}"
                            else "En iyi: ${s.longestStreak}"
                        )
                        append(" · ")
                    }
                    append(
                        when {
                            s.playedToday -> if (language == "en") "Today played ✓" else "Bugün oynadın ✓"
                            s.graceAvailable -> if (language == "en") "1 skip left this week"
                                                else "Bu hafta 1 kaçırma hakkın var"
                            else -> if (language == "en") "Play today to keep the streak"
                                    else "Seriyi korumak için bugün oyna"
                        }
                    )
                },
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = SonHarfTheme.PremiumTextSecondary,
            )
        }
    }
}

@Composable
private fun CollectionRow(col: WordCollectionDto, language: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("📚", style = TextStyle(fontSize = 22.sp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (language == "en") "Word collection" else "Kelime koleksiyonu",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = SonHarfTheme.PremiumTextSecondary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    col.totalWords.toString(),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black),
                    color = SonHarfTheme.PremiumTextPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (language == "en") "unique words" else "farklı kelime",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = SonHarfTheme.PremiumTextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                if (language == "en") "This month: ${col.thisMonth} new"
                else "Bu ay: ${col.thisMonth} yeni",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = SonHarfTheme.PremiumTextSecondary,
            )
        }
    }
}

@Composable
private fun SeasonalRow(t: SeasonalThemeDto, language: String) {
    val label = if (language == "en") t.nameEn else t.nameTr
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🎉", style = TextStyle(fontSize = 22.sp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (language == "en") "Seasonal theme" else "Mevsimsel tema",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = SonHarfTheme.PremiumTextSecondary,
            )
            Text(
                label,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = SonHarfTheme.GoldBright,
            )
            Text(
                if (language == "en")
                    "${t.wordCount} words · ${t.multiplier}x score"
                else
                    "${t.wordCount} kelime · ${t.multiplier}x puan",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = SonHarfTheme.PremiumTextSecondary,
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 0.5.dp),
    )
}