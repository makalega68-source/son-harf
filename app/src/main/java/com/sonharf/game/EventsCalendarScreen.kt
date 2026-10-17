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
                if (hour in 19..21) 2.0 else 1.5
        }
    }.filter { it.first >= now }.take(8)
}

@Composable
internal fun EventsCalendarScreen(onBack: () -> Unit, onAtelier: () -> Unit, onThrone: () -> Unit) {
    var selected by remember { mutableStateOf<DailyWordEvent?>(null) }
    val context=androidx.compose.ui.platform.LocalContext.current
    val user=remember { OnlineGameBackend().currentUserId() ?: "guest" }
    if(selected!=null) {
        DailyWordEventScreen(requireNotNull(selected),user) { selected=null }
        return
    }
    val prefs=remember { context.getSharedPreferences("daily_word_events_v1",0) }
    var day by remember { mutableStateOf(dailyEventDay()) }
    LaunchedEffect(Unit) { while(true) { day=dailyEventDay(); delay(30_000) } }
    var tournament by remember { mutableStateOf<AtelierTournament?>(null) }
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
                val a = event.await()
                failed = a.isFailure
                a.onSuccess { tournament = it; loadedAt = android.os.SystemClock.elapsedRealtime() }
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
        item { ActivityTile(sh("Taht Saati", "Throne Hour")) {
            Text(sh("Her akşam 19.00–22.00", "Every evening, 19:00–22:00"), color = LobbyPalette.Ink, fontWeight = FontWeight.Bold)
            Text(sh("Kuşatma, Son Harf ve Atölye'de ×2 XP. Atölye bonusuyla birleşmez.", "×2 XP in Siege, Last Letter and Workshop. It does not stack with the Workshop bonus."), color = LobbyPalette.Muted)
            Button(onClick=onThrone) { Text(sh("Tahtı aç", "Open Throne")) }
        } }
        if (failed) item { TextButton(onClick = { retry++ }) { Text(sh("Takvim güncellenemedi · Yenile", "Calendar unavailable · Retry"), color = Hf.Red) } }
        if (tournament == null && !failed) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Green) }
        tournament?.let { event ->
            item(key="atelier_tournament") {
                val next=if(now>0)atelierCalendar(now).firstOrNull() else null
                ActivityTile(sh("Atölye Turnuvası", "Workshop Tournament")) {
                    Text(if(event.active)sh("Şimdi açık · ×${event.multiplier} XP", "Live now · ×${event.multiplier} XP")
                        else sh("Bir sonraki turnuva", "Next tournament"),color=LobbyPalette.Ink)
                    Text(if(event.active)sh("${event.stage}. tur · Bitiş ", "Stage ${event.stage} · Ends ")+socialDate(event.stageEnds)
                        else next?.let { socialDate(Instant.ofEpochMilli(it.first).toString())+" · "+tournamentClockText(it.first,now) } ?: "—",color=LobbyPalette.Muted)
                    Button(onClick=onAtelier) { Text(if(event.active)sh("Katıl","Join")else sh("Atölyeyi aç","Open workshop")) }
                }
            }
        }
        item { Text(sh("Günün bulmacaları", "Daily puzzles"),color=LobbyPalette.Ink,fontWeight=FontWeight.Bold) }
        DailyWordEvent.entries.forEach { event -> item(key=event.name) {
            val answers=prefs.getString(eventProgressKey(user,day,SonHarfUiState.isEnglish,event),"").orEmpty()
            ActivityTile(dailyEventTitle(event)) {
                Text(dailyEventHint(event),color=LobbyPalette.Muted)
                Text(if(answers.length>=5)sh("Tamamlandı · ${eventScore(answers)}/100 · Yarın yenilenir", "Complete · ${eventScore(answers)}/100 · Returns tomorrow")
                    else sh("5 bulmaca · ${answers.length}/5 tamamlandı", "5 puzzles · ${answers.length}/5 complete"),color=LobbyPalette.Accent)
                Button(onClick={selected=event}) { Text(if(answers.length>=5)sh("Sonucun", "Your result")else if(answers.isEmpty())sh("Oyna", "Play")else sh("Devam et", "Continue")) }
            }
        } }
    }
}
