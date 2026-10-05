package com.sonharf.game

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay

internal fun tickerLine(row: TickerFeedRow): String? = when (row.kind) {
    // Announcements reach everyone in both languages: Turkish first, then English.
    "announcement" -> {
        val tr = row.messageTr?.trim().orEmpty()
        val en = row.messageEn?.trim().orEmpty()
        listOf(tr, en).filter { it.isNotBlank() }.distinct().joinToString(TickerGap).ifBlank { null }
    }
    "purchase" -> {
        val name = row.playerName?.takeIf { it.isNotBlank() } ?: return null
        val item = sh(row.itemNameTr ?: return null, row.itemNameEn ?: row.itemNameTr ?: return null)
        sh("$name, $item aldı", "$name bought $item")
    }
    else -> null
}

/**
 * Thin band at the very top of every screen, games included: each announcement and each store
 * purchase passes once per device, then never again. The band itself never comes and goes, so
 * the page below does not jump: with nothing new it rests on a quiet brand line.
 */
@Composable
internal fun TopNewsTicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lobby_ticker", Context.MODE_PRIVATE) }
    var seen by remember { mutableStateOf(prefs.getString("seen", "").orEmpty().split('\n').filter { it.isNotBlank() }) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    val foreground = rememberAppForeground()
    LaunchedEffect(foreground) {
        if (!foreground || !SupabaseProvider.configured) return@LaunchedEffect
        while (true) {
            gameRequestResult { TickerBackend.feed() }.onSuccess { rows -> lines = rows.mapNotNull(::tickerLine).distinct() }
            delay(60_000)
        }
    }
    val current = lines.firstOrNull { it !in seen }
    if (current == null) {
        TickerStrip(Icons.Rounded.Campaign, sh("Kelime Tahtı · Kelimeni kur, tahtı fethet.", "Word Throne · Build your words, claim the throne."),
            modifier, scroll = false, quiet = true)
        return
    }
    LaunchedEffect(current) {
        delay((5_000L + current.length * 140L).coerceAtMost(22_000L))
        seen = (seen + current).takeLast(300)
        prefs.edit().putString("seen", seen.joinToString("\n")).apply()
    }
    key(current) { TickerStrip(Icons.Rounded.Campaign, current, modifier, iterations = 1) }
}

/** The screenshot harness turns ticker motion off so captures are deterministic. */
internal object TickerMotion { var enabled = true }

/**
 * Bottom band of the menu pages: the Kelime Atölyesi countdown, and every 10 minutes the podium
 * of the last tournament scrolls past. The server rolls the podium over when a new
 * tournament finishes, so each 2-hour tournament's winners take the next turns.
 */
@Composable
internal fun WorkshopPodiumTicker(modifier: Modifier = Modifier) {
    var event by remember { mutableStateOf<AtelierTournament?>(null) }
    var showPodium by remember { mutableStateOf(false) }
    val foreground = rememberAppForeground()
    LaunchedEffect(foreground) {
        if (!foreground || !SupabaseProvider.configured) return@LaunchedEffect
        while (true) {
            gameRequestResult { ThroneBackend.tournament() }.onSuccess { event = it }
            delay(30_000)
        }
    }
    // Podium for 30 s, then the countdown until the next 10-minute mark.
    LaunchedEffect(Unit) {
        delay(8_000)
        while (true) {
            showPodium = true
            delay(30_000)
            showPodium = false
            delay(570_000)
        }
    }
    val now = serverNow(event?.serverTime.orEmpty(), Unit)
    val winners = event?.winners.orEmpty().sortedBy { it.rank }.take(3)
    if (showPodium && winners.isNotEmpty()) {
        val medals = listOf("🥇", "🥈", "🥉")
        val podium = winners.mapIndexed { i, w -> "${medals.getOrElse(i) { "" }} ${w.name} ${w.score}" }
            .joinToString(TickerGap)
        TickerStrip(Icons.Rounded.EmojiEvents, sh("Atölye kazananları:  ", "Workshop winners:  ") + podium,
            modifier, gold = true, iterations = 1)
    } else {
        val text = when {
            event?.active == true -> sh("Atölye turnuvası açık · ×${event?.multiplier}", "Workshop tournament live · ×${event?.multiplier}")
            now > 0 -> sh("Atölye turnuvası: ", "Workshop tournament: ") + tournamentClockText(tournamentNextRegular(now), now)
            else -> sh("Atölye turnuvası", "Workshop tournament")
        }
        TickerStrip(Icons.Rounded.EmojiEvents, text, modifier, scroll = false)
    }
}

private const val TickerGap = "      •      "

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TickerStrip(icon: ImageVector, text: String, modifier: Modifier, gold: Boolean = false, scroll: Boolean = true,
    iterations: Int = 2, quiet: Boolean = false) {
    Row(modifier.fillMaxWidth().height(26.dp)
        .background(Brush.horizontalGradient(listOf(LobbyBrand.NavBar, LobbyBrand.Band, LobbyBrand.NavBar))),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.fillMaxHeight().background(LobbyBrand.Gold.copy(alpha = .18f)).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = LobbyBrand.Gold, modifier = Modifier.size(16.dp))
        }
        Text(text,
            Modifier.weight(1f).padding(horizontal = 8.dp).then(
                if (scroll && TickerMotion.enabled) Modifier.basicMarquee(iterations = iterations, animationMode = MarqueeAnimationMode.Immediately,
                    initialDelayMillis = 900, repeatDelayMillis = 1_200, velocity = 46.dp) else Modifier),
            color = when { gold -> LobbyBrand.Gold; quiet -> Color.White.copy(alpha = .55f); else -> Color.White },
            fontSize = 12.sp, fontWeight = if (quiet) FontWeight.Medium else FontWeight.Bold, maxLines = 1)
    }
}
