package com.sonharf.game.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.data.DailyQuestDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.getOrIssueDailyQuests

/**
 * G4.3 — Günlük Görevler kartı.
 *
 * Fetches the 3 quests for today (one per game) from the server and
 * renders each with a progress bar in that game's accent color. The
 * card is stateless from the caller's point of view: it self-loads
 * on first composition and refreshes when [refreshTick] changes.
 *
 * Show on the main screen. Each individual game screen can also show
 * a compact single-quest row for its own game via [DailyQuestGameRow].
 */
@Composable
fun DailyQuestsCard(
    modifier: Modifier = Modifier,
    language: String = "tr",
    refreshTick: Int = 0,
) {
    val backend = remember { OnlineGameBackend() }
    var quests by remember { mutableStateOf<List<DailyQuestDto>?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        loaded = false
        quests = runCatching {
            backend.getOrIssueDailyQuests()
        }.getOrDefault(emptyList())
        loaded = true
    }

    GamePanel(
        modifier = modifier.fillMaxWidth(),
        title = if (language == "en") "DAILY QUESTS" else "GÜNLÜK GÖREVLER",
        accent = SonHarfTheme.GoldBright,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                !loaded -> {
                    Text(
                        if (language == "en") "Loading…" else "Yükleniyor…",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = SonHarfTheme.PremiumTextSecondary,
                    )
                }
                quests.isNullOrEmpty() -> {
                    Text(
                        if (language == "en") "No quests today." else "Bugün görev yok.",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = SonHarfTheme.PremiumTextSecondary,
                    )
                }
                else -> {
                    quests?.forEach { q ->
                        DailyQuestRow(q, language)
                    }
                }
            }
        }
    }
}

/** Single row: dot + title + progress bar + reward + "3/3" or ✓. */
@Composable
fun DailyQuestRow(quest: DailyQuestDto, language: String = "tr") {
    val accent = gameAccent(quest.game)
    val title = if (language == "en") quest.titleEn else quest.titleTr
    val progressFraction = if (quest.target <= 0) 0f else
        (quest.progress.toFloat() / quest.target.toFloat()).coerceIn(0f, 1f)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(6.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = SonHarfTheme.PremiumTextPrimary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (quest.completed) {
                    Text(
                        "✓",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Black),
                        color = SonHarfTheme.GoldBright,
                    )
                } else {
                    Text(
                        "${quest.progress}/${quest.target}",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black),
                        color = SonHarfTheme.PremiumTextPrimary,
                    )
                }
            }
            // Progress bar with reward pill on the right.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accent.copy(alpha = 0.7f), accent),
                                ),
                            ),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "💎 ${quest.rewardDiamonds}",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = SonHarfTheme.DiamondBlue,
                )
            }
        }
    }
}

/**
 * Compact one-row indicator to show inside a single game's card ("x/1"
 * label plus a mini progress bar). Reads the quest for [game] from
 * [quests]; renders nothing if none is issued for that game today.
 */
@Composable
fun DailyQuestGameRow(
    game: String,
    quests: List<DailyQuestDto>?,
    modifier: Modifier = Modifier,
    language: String = "tr",
) {
    val q = quests?.firstOrNull { it.game == game } ?: return
    val accent = gameAccent(game)
    val fraction = if (q.target <= 0) 0f else
        (q.progress.toFloat() / q.target.toFloat()).coerceIn(0f, 1f)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (q.completed) "✓" else "${q.progress}/${q.target}",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black),
            color = if (q.completed) SonHarfTheme.GoldBright else accent,
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(accent),
            )
        }
    }
}

private fun gameAccent(game: String): Color = when (game) {
    "son_harf" -> SonHarfTheme.SonHarfOrange
    "kusatma" -> SonHarfTheme.KusatmaPurple
    "kelime_yolu" -> SonHarfTheme.KelimeYoluTeal
    else -> SonHarfTheme.GoldBright
}