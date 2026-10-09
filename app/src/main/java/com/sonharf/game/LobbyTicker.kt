package com.sonharf.game

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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

private val ThroneHourZone = ZoneId.of("Europe/Istanbul")

internal fun throneHourActive(nowMillis: Long): Boolean {
    if (nowMillis <= 0L) return false
    return Instant.ofEpochMilli(nowMillis).atZone(ThroneHourZone).hour in 19..21
}

internal fun throneHourBoundary(nowMillis: Long): Long {
    if (nowMillis <= 0L) return 0L
    val now = Instant.ofEpochMilli(nowMillis).atZone(ThroneHourZone)
    val target: ZonedDateTime = if (throneHourActive(nowMillis)) {
        now.toLocalDate().atTime(22, 0).atZone(ThroneHourZone)
    } else {
        val today = now.toLocalDate().atTime(19, 0).atZone(ThroneHourZone)
        if (now.isBefore(today)) today else today.plusDays(1)
    }
    return target.toInstant().toEpochMilli()
}

@Composable
internal fun HomeEventPanel(onOpenEvents: () -> Unit, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    var event by remember { mutableStateOf<AtelierTournament?>(null) }
    val foreground = rememberAppForeground()
    LaunchedEffect(foreground) {
        if (!foreground || !SupabaseProvider.configured) return@LaunchedEffect
        while (true) {
            gameRequestResult { ThroneBackend.tournament() }.onSuccess { event = it }
            delay(30_000)
        }
    }
    val now = serverNow(event?.serverTime.orEmpty(), "home-event-panel")
    val live = throneHourActive(now)
    val throneClock = if (now > 0L) tournamentClockText(throneHourBoundary(now), now) else "—:—:—"
    val workshopClock = if (now > 0L) tournamentClockText(
        tournamentTimeMillis(event?.nextStart.orEmpty()).takeIf { it > now } ?: tournamentNextRegular(now), now
    ) else "—:—:—"
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
        onClick = onOpenEvents,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = LobbyBrand.Band,
        border = androidx.compose.foundation.BorderStroke(1.dp, LobbyBrand.Gold.copy(alpha = .58f)),
        shadowElevation = 5.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 13.dp, top = 10.dp, end = 9.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).background(Brush.radialGradient(listOf(LobbyBrand.Gold.copy(alpha = .30f), Color.Transparent)), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Bolt, null, tint = LobbyBrand.Gold, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (live) sh("TAHT SAATİ BAŞLADI", "THRONE HOUR IS LIVE") else sh("SIRADAKİ ETKİNLİK", "NEXT EVENT"), color = LobbyBrand.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(if (live) sh("Tüm oyunlarda ×2 XP", "×2 XP in every game") else sh("Taht Saati · 19.00–22.00 · ×2 XP", "Throne Hour · 19:00–22:00 · ×2 XP"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(if (live) sh("Bitmesine $throneClock", "Ends in $throneClock") else sh("$throneClock sonra  •  Atölye ×1,5: $workshopClock", "$throneClock away  •  Workshop ×1.5: $workshopClock"), color = Color.White.copy(alpha = .72f), fontSize = 10.sp, maxLines = 1)
            }
            Surface(onClick = onPlay, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = LobbyBrand.Gold) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("OYNA", "PLAY"), color = LobbyBrand.NavBar, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Icon(Icons.Rounded.ChevronRight, null, tint = LobbyBrand.NavBar, modifier = Modifier.size(15.dp))
                }
            }
        }
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
