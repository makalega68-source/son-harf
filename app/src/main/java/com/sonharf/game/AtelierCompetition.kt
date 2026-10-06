package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.AtelierBoardDto
import com.sonharf.game.data.AtelierBoardRowDto
import com.sonharf.game.data.AtelierWeeklyRewardDto

/** Which kind of round the workshop is showing. */
internal enum class AtelierMode { LOBBY, PRACTICE, DAILY, TOURNAMENT }

private object CompUi {
    val Cream = Color(0xFFFCF8F0)
    val Ink = Color(0xFF2B2720)
    val InkMuted = Color(0xFF6E665A)
    val Green = Color(0xFF2E7A53)
    val GreenSoft = Color(0xFFDDEDE1)
    val Gold = Color(0xFFB08D45)
    val GoldSoft = Color(0xFFF1E4C3)
    val Edge = Color(0xFFCDB387)
    val Night = Color(0xFF214938)
    val NightTop = Color(0xFF3F8F61)
}

/**
 * Where the player is on today's board while the official run is going: their live rank and
 * the next rival to overtake, so every word feels like a step up the table.
 */
@Composable
internal fun AtelierRivalStrip(board: AtelierBoardDto?, score: Int) {
    val others = board?.rows.orEmpty().filter { !it.me }
    val above = others.filter { it.score > score }
    val rank = above.size + 1
    val shownRank = if ((board?.total ?: 0) > others.size && rank > others.size) "50+" else rank.toString()
    val next = above.minByOrNull { it.score }
    val text = when {
        board == null -> sh("Günlük Yarış · sıralama yükleniyor…", "Daily Race · loading the board…")
        others.isEmpty() -> sh("Bugünün ilk yarışçısı sensin! Çıtayı sen koy.", "You're today's first racer! Set the bar.")
        next == null -> sh("🥇 Şu an zirvedesin! ${others.size} rakibin arkanda.", "🥇 You're on top! ${others.size} rivals behind you.")
        else -> sh("Hedef sıralama $shownRank · Sıradaki: ${next.name} (${next.score}) · ${next.score - score + 1} puan",
            "Target rank $shownRank · Next: ${next.name} (${next.score}) · ${next.score - score + 1} pts")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(CompUi.NightTop, CompUi.Night)))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🏁", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Lobby, top to bottom: pick a length, play (free practice), then the tournament. The daily race
 * is gone from the workshop; a last-week reward still waiting is shown so it can be claimed.
 */
@Composable
internal fun AtelierLobby(
    online: Boolean,
    board: AtelierBoardDto?,
    weekly: Boolean,
    loadingBoard: Boolean,
    reward: AtelierWeeklyRewardDto?,
    notice: String?,
    starting: Boolean,
    seconds: Int,
    onSeconds: (Int) -> Unit,
    onWeekly: (Boolean) -> Unit,
    onDaily: () -> Unit,
    onPractice: () -> Unit,
    onClaim: () -> Unit,
    tournament: @Composable () -> Unit = {},
    leaderboard: @Composable () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Last week's reward first: it is the one thing waiting for the player.
        if (reward != null && reward.reward > 0 && !reward.claimed) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CompUi.GoldSoft)
                    .border(1.dp, CompUi.Gold, RoundedCornerShape(14.dp))
                    .clickable(onClick = onClaim)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🏆", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Geçen hafta ${reward.rank}. oldun!", "You finished #${reward.rank} last week!"), color = CompUi.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(sh("Ödülünü al: +${reward.reward} Altın", "Claim your reward: +${reward.reward} Gold"), color = CompUi.InkMuted, fontSize = 13.sp)
                }
                Text(sh("AL", "CLAIM"), color = CompUi.Green, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }

        // 1. Length: four small chips; the line under them says what the choice means.
        val lengths = listOf(
            60 to sh("1 Dakika · 6 görev", "1 Minute · 6 tasks"),
            120 to sh("2 Dakika · 15 görev", "2 Minutes · 15 tasks"),
            180 to sh("3 Dakika · 24 görev", "3 Minutes · 24 tasks"),
            300 to sh("5 Dakika · 36 görev", "5 Minutes · 36 tasks"),
        )
        Text(sh("SÜRE", "LENGTH"), color = LobbyPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lengths.forEach { (value, _) ->
                val selected = seconds == value
                Box(
                    Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (selected) CompUi.Green else LobbyPalette.Paper)
                        .border(1.dp, if (selected) CompUi.Green else CompUi.Edge, RoundedCornerShape(12.dp))
                        .clickable(enabled = !starting) { onSeconds(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(sh("${value / 60} dk", "${value / 60} min"), color = if (selected) Color.White else LobbyPalette.Ink,
                        fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Text(lengths.firstOrNull { it.first == seconds }?.second.orEmpty(), color = LobbyPalette.Muted, fontSize = 12.sp)

        // 2. Play: the big obvious button.
        Button(
            onClick = onPractice,
            enabled = !starting,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CompUi.Green, contentColor = Color.White),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(sh("OYNA", "PLAY"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(sh("Serbest Antrenman", "Free Practice"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = .85f))
            }
        }

        // 3. The tournament.
        tournament()
        // 4. The tournament leaderboard (top 100, new every 2 hours).
        leaderboard()

        notice?.let { Text(it, color = CompUi.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }

    }
}

@Composable
private fun BoardTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CompUi.Green else CompUi.GreenSoft)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.White else CompUi.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoardRow(row: AtelierBoardRowDto) {
    val medal = when (row.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (row.me) CompUi.GreenSoft else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
            if (medal != null) Text(medal, fontSize = 20.sp)
            else Text("${row.rank}", color = CompUi.InkMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        ProfilePhotoAvatar(avatarPath = row.avatarPath, name = row.name, size = 34.dp, accent = CompUi.Gold, frameId = rememberPlayerFrame(row.userId.takeIf { it.isNotBlank() }))
        Spacer(Modifier.width(8.dp))
        Text(
            row.name + if (row.me) sh(" (sen)", " (you)") else "",
            modifier = Modifier.weight(1f),
            color = CompUi.Ink,
            fontSize = 15.sp,
            fontWeight = if (row.me) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier.clip(CircleShape).background(if (row.rank <= 3) CompUi.GoldSoft else Color.Transparent).padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("${row.score}", color = CompUi.Green, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

/**
 * The workshop tournament's leaderboard: the top 100 of the live tournament, or of the latest one
 * until the next starts, so it changes every 2 hours. Scrolls inside its own card; the player's
 * own place is pinned under it when they are outside the top 100.
 */
@Composable
internal fun AtelierTournamentLeaderboard(online: Boolean) {
    var board by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.sonharf.game.data.TournamentBoard?>(null) }
    var loading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    val foreground = rememberAppForeground()
    androidx.compose.runtime.LaunchedEffect(online, foreground) {
        if (!online || !foreground || !com.sonharf.game.data.SupabaseProvider.configured) { loading = false; return@LaunchedEffect }
        while (true) {
            gameRequestResult { com.sonharf.game.data.ThroneBackend.tournamentBoard() }.onSuccess { board = it }
            loading = false
            kotlinx.coroutines.delay(60_000)
        }
    }
    val current = board
    val started = parseServerInstantMs(current?.eventStart)
    val time = started?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.of("Europe/Istanbul")).toLocalTime()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sh("TURNUVA SIRALAMASI", "TOURNAMENT LEADERBOARD"), color = LobbyPalette.Ink, fontSize = 13.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(
                    when {
                        current?.live == true && time != null -> sh("Canlı · $time turnuvası · ilk 100", "Live · $time tournament · top 100")
                        time != null -> sh("$time turnuvası · her 2 saatte yenilenir", "$time tournament · new every 2 hours")
                        else -> sh("Her 2 saatte yenilenir · ilk 100", "New every 2 hours · top 100")
                    },
                    color = LobbyPalette.Muted, fontSize = 12.sp,
                )
            }
            if (current?.live == true) Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFD7263D)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(sh("CANLI", "LIVE"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CompUi.Cream)
                .border(1.dp, CompUi.Edge.copy(alpha = .6f), RoundedCornerShape(18.dp)),
        ) {
            val rows = current?.rows.orEmpty()
            when {
                !online -> Text(sh("Sıralama çevrim dışıyken görünmez.", "The leaderboard is not available offline."),
                    Modifier.fillMaxWidth().padding(18.dp), color = CompUi.InkMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                loading && current == null -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = CompUi.Green, strokeWidth = 2.dp)
                }
                rows.isEmpty() -> Text(sh("Henüz kimse yarışmadı. İlk turnuvada kürsüye sen çık!", "No one has raced yet. Take the podium in the first tournament!"),
                    Modifier.fillMaxWidth().padding(18.dp), color = CompUi.InkMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                else -> {
                    // Up to 100 rows scroll inside the card (about eight are visible at once).
                    androidx.compose.foundation.lazy.LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
                    ) {
                        items(rows.size, key = { rows[it].userId.ifBlank { "r$it" } }) { index -> TournamentBoardLine(rows[index]) }
                    }
                    val me = current?.me
                    if (me != null && rows.none { it.me }) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(CompUi.Edge.copy(alpha = .6f)))
                        Text(sh("Sen: ${me.rank}. · ${me.score} puan · ${me.stages}/3 aşama", "You: #${me.rank} · ${me.score} pts · ${me.stages}/3 stages"),
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), color = CompUi.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(sh("${current?.total ?: rows.size} oyuncu yarıştı", "${current?.total ?: rows.size} players raced"),
                        Modifier.fillMaxWidth().padding(bottom = 10.dp), color = CompUi.InkMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun TournamentBoardLine(row: com.sonharf.game.data.TournamentBoardRow) {
    Row(
        Modifier.fillMaxWidth().background(if (row.me) CompUi.GreenSoft else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val medal = when (row.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
        Box(Modifier.width(40.dp), contentAlignment = Alignment.CenterStart) {
            if (medal != null) Text(medal, fontSize = 20.sp)
            else Text("${row.rank}.", color = CompUi.InkMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(row.name, Modifier.weight(1f), color = CompUi.Ink, fontSize = 15.sp,
            fontWeight = if (row.me || row.rank <= 3) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sh("${row.stages}/3", "${row.stages}/3"), Modifier.width(44.dp), color = CompUi.InkMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("${row.score}", Modifier.width(64.dp), color = CompUi.Ink, fontSize = 15.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
    }
}
