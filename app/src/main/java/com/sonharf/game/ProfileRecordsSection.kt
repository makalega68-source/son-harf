package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class SiegeRecordSummary(val count: Int, val wins: Int, val averageScore: Int, val averageControl: Int, val finalCubes: Int)
internal fun siegeRecordSummary(games: List<WordSiegeGameDto>, me: String?): SiegeRecordSummary {
    val finished = games.distinctBy { it.id }.filter { me != null && me in listOf(it.playerOneId, it.playerTwoId) && it.status == "finished" }
    val total = finished.sumOf { if (it.playerOneId == me) it.playerOneWordScore + it.playerOneAreaScore else it.playerTwoWordScore + it.playerTwoAreaScore }
    val cubes = finished.sumOf { if (it.playerOneId == me) it.playerOneArea else it.playerTwoArea }
    return SiegeRecordSummary(finished.size, finished.count { it.winnerId == me }, if (finished.isEmpty()) 0 else total / finished.size,
        if (finished.isEmpty()) 0 else (cubes * 100L / (finished.size * WordSiegeBoardSpec.CellCount)).toInt(), cubes)
}

@Composable
internal fun ProfileRecordsSection(backend: OnlineGameBackend, onRivals: () -> Unit) {
    var records by remember { mutableStateOf<PersonalRecordsDto?>(null) }
    var siege by remember { mutableStateOf<SiegeRecordSummary?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry) {
        coroutineScope {
            val personal = async { gameRequestResult { backend.getPersonalRecords() } }
            val classic = async { gameRequestResult { backend.getWordSiegeGameSummaries("classic", finishedLimit = 1000) } }
            val fast = async { gameRequestResult { backend.getWordSiegeGameSummaries("series", finishedLimit = 1000) } }
            val a = personal.await(); val b = classic.await(); val c = fast.await()
            records = a.getOrNull()
            siege = if (b.isSuccess && c.isSuccess) siegeRecordSummary(b.getOrThrow() + c.getOrThrow(), backend.currentUserId()) else null
            failed = a.isFailure || b.isFailure || c.isFailure
        }
    }
    ActivityTile(sh("İSTATİSTİKLER", "STATISTICS")) {
        records?.let { r ->
            RecordRow(sh("Son Harf · gerçek rakip maçları", "Last Letter · real-player matches"), "${r.realPvpMatches}")
            RecordRow(sh("Galibiyet oranı", "Win rate"), if (r.realPvpMatches == 0) "—" else "${r.realPvpWins * 100L / r.realPvpMatches}%")
            RecordRow(sh("En uzun kelime", "Longest word"), r.longestWord.ifBlank { "—" })
            RecordRow(sh("En iyi galibiyet serisi", "Best win streak"), "${r.bestStreak}")
        }
        siege?.let { s ->
            HorizontalDivider(color = LobbyPalette.Line)
            RecordRow(sh("Kuşatma · kayıtlı biten maçlar", "Siege · recorded finished matches"), "${s.count}")
            RecordRow(sh("Kuşatma galibiyet oranı", "Siege win rate"), if (s.count == 0) "—" else "${s.wins * 100L / s.count}%")
            RecordRow(sh("Ortalama skor", "Average score"), if (s.count == 0) "—" else "${s.averageScore}")
            RecordRow(sh("Ortalama bitiş hâkimiyeti", "Average final control"), if (s.count == 0) "—" else "${s.averageControl}%")
            RecordRow(sh("Bitişte elde tutulan toplam küp", "Total cubes held at finish"), "${s.finalCubes}")
        }
        if (failed) TextButton(onClick = { retry++ }) { Text(sh("İstatistikler eksik · Yenile", "Incomplete statistics · Retry"), color = Hf.Red) }
        if (records == null && siege == null && !failed) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Green)
        TextButton(onClick = onRivals) { Text(sh("RAKİP GEÇMİŞİ", "RIVAL HISTORY"), color = Hf.Green) }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), color = LobbyPalette.Muted, fontSize = 12.sp)
        Text(value, color = LobbyPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
