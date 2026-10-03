package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

internal fun atelierCalendar(now: Long): List<Pair<Long, Double>> {
    val zone = ZoneId.of("Europe/Istanbul")
    val local = Instant.ofEpochMilli(now).atZone(zone)
    return (0..1).flatMap { day ->
        ((0..22 step 2).toList() + 19).distinct().sorted().map { hour ->
            local.toLocalDate().plusDays(day.toLong()).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli() to
                if (hour == 19 || hour == 22) 3.0 else 1.5
        }
    }.filter { it.first >= now }.take(8)
}

@Composable
internal fun EventsCalendarScreen(onBack: () -> Unit, onAtelier: () -> Unit, onThrone: () -> Unit) {
    var tournament by remember { mutableStateOf<AtelierTournament?>(null) }
    var week by remember { mutableStateOf<ThroneWeek?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var loadedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val foreground = rememberAppForeground()
    LaunchedEffect(foreground, retry) {
        if (!foreground) return@LaunchedEffect
        while (true) {
            coroutineScope {
                val event = async { gameRequestResult { ThroneBackend.tournament() } }
                val throne = async { gameRequestResult { ThroneBackend.week() } }
                val a = event.await(); val b = throne.await()
                failed = a.isFailure || b.isFailure
                a.onSuccess { tournament = it; loadedAt = android.os.SystemClock.elapsedRealtime() }
                b.onSuccess { week = it }
            }
            delay(30_000)
        }
    }
    LaunchedEffect(foreground) {
        if (!foreground) return@LaunchedEffect
        while (true) { elapsed = android.os.SystemClock.elapsedRealtime(); delay(1_000) }
    }
    val server = tournament?.serverTime?.let(::tournamentTimeMillis) ?: 0L
    val now = if (server > 0L) server + (elapsed - loadedAt).coerceAtLeast(0L) else 0L
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MainScreenHeader(sh("Etkinlikler", "Events"), sh("Türkiye saati", "Istanbul time"), onBack = onBack) }
        if (failed) item { TextButton(onClick = { retry++ }) { Text(sh("Takvim güncellenemedi · Yenile", "Calendar unavailable · Retry"), color = Hf.Red) } }
        if (tournament == null && !failed) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Green) }
        tournament?.let { event ->
            if (event.active) item {
                ActivityTile(sh("ŞİMDİ · Atölye ×${event.multiplier} XP", "LIVE · Atelier ×${event.multiplier} XP")) {
                    Text(sh("${event.stage}. tur · Bitiş ", "Stage ${event.stage} · Ends ") + socialDate(event.stageEnds), color = LobbyPalette.Muted)
                    Button(onClick = onAtelier) { Text(sh("KATIL", "JOIN")) }
                }
            }
            if (now > 0) {
                item { Text(sh("YAKLAŞAN TURNUVALAR", "UPCOMING TOURNAMENTS"), color = LobbyPalette.Ink, fontWeight = FontWeight.Black) }
                atelierCalendar(now).forEach { (start, multiplier) -> item(key = start) {
                    ActivityTile(socialDate(Instant.ofEpochMilli(start).toString()) + " · ×$multiplier XP") {
                        Text(tournamentClockText(start, now), color = Hf.Green)
                        TextButton(onClick = onAtelier) { Text(sh("ATÖLYEYİ AÇ", "OPEN ATELIER")) }
                    }
                } }
            }
        }
        week?.let { w -> item {
            ActivityTile(sh("BU HAFTA · Taht yarışı", "THIS WEEK · Throne race")) {
                Text("${w.me.xp} XP · " + sh("Sıra ", "Rank ") + (w.me.rank.takeIf { it > 0 }?.toString() ?: "—"), color = LobbyPalette.Ink)
                Text(sh("Sıfırlama: ", "Reset: ") + socialDate(w.resetAt), color = LobbyPalette.Muted)
                TextButton(onClick = onThrone) { Text(sh("TAHTI AÇ", "OPEN THRONE")) }
            }
        } }
    }
}
