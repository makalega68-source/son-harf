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

/** What a ticker line is counted by: an announcement by its Turkish text, so the English half or
 * a language switch never makes it "new"; a purchase by who bought what. */
internal fun tickerKey(row: TickerFeedRow): String? = when (row.kind) {
    "announcement" -> row.messageTr?.trim()?.takeIf { it.isNotBlank() }?.let { "a|$it" }
    "purchase" -> "p|${row.playerName.orEmpty()}|${row.itemNameTr.orEmpty()}|${row.happenedAt.orEmpty()}"
    else -> null
}

/** How many times a line may pass on one device: an announcement 3 times, a purchase once. */
internal fun tickerShowLimit(kind: String): Int = if (kind == "announcement") ANNOUNCEMENT_SHOW_LIMIT else 1

internal const val ANNOUNCEMENT_SHOW_LIMIT = 3

private data class TickerItem(val key: String, val text: String, val limit: Int)

/** Per-device show counts, kept in preferences so an app restart never resets them. */
private object TickerShows {
    private const val PREFS = "lobby_ticker"
    private const val COUNTS = "shown_counts"
    private var counts: LinkedHashMap<String, Int>? = null

    private fun load(context: Context): LinkedHashMap<String, Int> = counts ?: LinkedHashMap<String, Int>().also { map ->
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(COUNTS, "").orEmpty().split('\n').forEach { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) line.substring(0, tab).toIntOrNull()?.let { map[line.substring(tab + 1)] = it }
        }
        counts = map
    }

    fun count(context: Context, key: String): Int = load(context)[key] ?: 0

    fun bump(context: Context, key: String) {
        val map = load(context)
        map[key] = (map.remove(key) ?: 0) + 1
        while (map.size > 300) map.remove(map.keys.first())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(COUNTS, map.entries.joinToString("\n") { "${it.value}\t${it.key.replace('\n', ' ')}" }).apply()
    }
}

/**
 * Thin band at the very top of every screen, games included: each announcement passes at most
 * three times per device and each store purchase once, then never again. A pass is counted the
 * moment it starts, so closing the app mid-scroll still counts it. The band itself never comes
 * and goes, so the page below does not jump: with nothing new it rests on a quiet brand line.
 */
@Composable
internal fun TopNewsTicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var items by remember { mutableStateOf<List<TickerItem>>(emptyList()) }
    var showing by remember { mutableStateOf<TickerItem?>(null) }
    val foreground = rememberAppForeground()
    LaunchedEffect(foreground) {
        if (!foreground || !SupabaseProvider.configured) return@LaunchedEffect
        while (true) {
            gameRequestResult { TickerBackend.feed() }.onSuccess { rows ->
                items = rows.mapNotNull { row ->
                    val key = tickerKey(row) ?: return@mapNotNull null
                    val text = tickerLine(row) ?: return@mapNotNull null
                    TickerItem(key, text, tickerShowLimit(row.kind))
                }.distinctBy { it.key }
            }
            delay(60_000)
        }
    }
    LaunchedEffect(foreground) {
        if (!foreground) return@LaunchedEffect
        while (true) {
            val next = items.firstOrNull { TickerShows.count(context, it.key) < it.limit }
            if (next == null) {
                showing = null
                delay(2_000)
                continue
            }
            TickerShows.bump(context, next.key)
            showing = next
            delay((5_000L + next.text.length * 140L).coerceAtMost(22_000L))
            showing = null
            delay(1_500)
        }
    }
    val current = showing
    if (current == null) {
        TickerStrip(Icons.Rounded.Campaign, sh("Kelime Tahtı · Kelimeni kur, tahtı fethet.", "Word Throne · Build your words, claim the throne."),
            modifier, scroll = false, quiet = true)
        return
    }
    key(current.key, current.text) { TickerStrip(Icons.Rounded.Campaign, current.text, modifier, iterations = 1) }
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
