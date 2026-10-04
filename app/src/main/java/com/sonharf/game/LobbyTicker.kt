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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay

/** Lines shown when nobody has bought anything lately and no announcement is set. */
private fun tickerFallback(): List<String> = listOf(
    sh("Kelime Tahtı'na hoş geldin! Yeni tahtalar ve çerçeveler mağazada seni bekliyor.",
        "Welcome to Word Throne! New boards and frames are waiting in the store."),
    sh("Kelime Atölyesi turnuvaları her 2 saatte bir; 19:00 ve 22:00'de tecrübe ×3.",
        "Word Workshop tournaments every 2 hours; ×3 experience at 19:00 and 22:00."),
    sh("Haftanın Taht yarışında yerini al: üç oyunda kazandığın XP tek sıralamada toplanır.",
        "Take your place in this week's Throne race: XP from all three games counts."),
)

internal fun tickerLine(row: TickerFeedRow): String? = when (row.kind) {
    "announcement" -> sh(row.messageTr ?: return null, row.messageEn ?: row.messageTr ?: return null)
    "purchase" -> {
        val name = row.playerName?.takeIf { it.isNotBlank() } ?: return null
        val item = sh(row.itemNameTr ?: return null, row.itemNameEn ?: row.itemNameTr ?: return null)
        sh("$name mağazadan $item aldı!", "$name just bought $item!")
    }
    else -> null
}

/**
 * Thin scrolling band at the very top of every screen, games included: admin announcements
 * first, then who bought what in the store. Polls the server once a minute while visible.
 */
@Composable
internal fun TopNewsTicker(modifier: Modifier = Modifier) {
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    val foreground = rememberAppForeground()
    LaunchedEffect(foreground) {
        if (!foreground || !SupabaseProvider.configured) return@LaunchedEffect
        while (true) {
            gameRequestResult { TickerBackend.feed() }.onSuccess { rows -> lines = rows.mapNotNull(::tickerLine) }
            delay(60_000)
        }
    }
    // One line at a time: it scrolls through once, rests, then the next one comes. Cheaper to draw
    // than an endless band and easier to read.
    val shown = lines.ifEmpty { tickerFallback() }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(shown) {
        index = 0
        while (true) {
            val line = shown.getOrNull(index % shown.size).orEmpty()
            delay((5_000L + line.length * 140L).coerceAtMost(22_000L))
            index = (index + 1) % shown.size
        }
    }
    key(index, shown) {
        TickerStrip(Icons.Rounded.Campaign, shown.getOrNull(index % shown.size).orEmpty(), modifier, iterations = 1)
    }
}

/** The screenshot harness turns ticker motion off so captures are deterministic. */
internal object TickerMotion { var enabled = true }

/**
 * Bottom band of the menu pages: the Kelime Atölyesi countdown, and every 5 minutes the podium
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
    // Podium for 40 s, then the countdown until the next 5-minute mark.
    LaunchedEffect(Unit) {
        delay(8_000)
        while (true) {
            showPodium = true
            delay(40_000)
            showPodium = false
            delay(260_000)
        }
    }
    val now = serverNow(event?.serverTime.orEmpty(), Unit)
    val winners = event?.winners.orEmpty().sortedBy { it.rank }.take(3)
    if (showPodium && winners.isNotEmpty()) {
        val medals = listOf("🥇", "🥈", "🥉")
        val podium = winners.mapIndexed { i, w -> "${medals.getOrElse(i) { "" }} ${w.rank}. ${w.name} · ${w.score}" }
            .joinToString(TickerGap)
        TickerStrip(Icons.Rounded.EmojiEvents, sh("Kelime Atölyesi son turnuva kazananları:  ", "Word Workshop last tournament winners:  ") + podium,
            modifier, gold = true)
    } else {
        val text = when {
            event?.active == true -> sh("Kelime Atölyesi turnuvası şu an açık · Tecrübe ×${event?.multiplier} · Hemen katıl!",
                "Word Workshop tournament is live · Experience ×${event?.multiplier} · Join now!")
            now > 0 -> sh("Kelime Atölyesi turnuvasına kalan süre: ", "Next Word Workshop tournament in: ") +
                tournamentClockText(tournamentNextRegular(now), now)
            else -> sh("Kelime Atölyesi turnuvaları her 2 saatte bir başlar.", "Word Workshop tournaments start every 2 hours.")
        }
        TickerStrip(Icons.Rounded.EmojiEvents, text, modifier, scroll = false)
    }
}

private const val TickerGap = "      •      "

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TickerStrip(icon: ImageVector, text: String, modifier: Modifier, gold: Boolean = false, scroll: Boolean = true,
    iterations: Int = 2) {
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
            color = if (gold) LobbyBrand.Gold else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
