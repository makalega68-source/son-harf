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
internal enum class AtelierMode { LOBBY, PRACTICE, DAILY }

private object CompUi {
    val Cream = Color(0xFFFCF8F0)
    val Ink = Color(0xFF2B2720)
    val InkMuted = Color(0xFF6E665A)
    val Green = Color(0xFF2E7A53)
    val GreenSoft = Color(0xFFDDEDE1)
    val Gold = Color(0xFFB08D45)
    val GoldSoft = Color(0xFFF1E4C3)
    val Edge = Color(0xFFCDB387)
    val Night = Color(0xFF26324A)
    val NightTop = Color(0xFF3B4C70)
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

/** Lobby: today's official race, free practice, last week's reward and the two leaderboards. */
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Round length for both the race and practice; each length has its own daily race and board.
        listOf(
            60 to sh("1 Dakika · 6 görev", "1 Minute · 6 tasks"),
            120 to sh("2 Dakika · 15 görev", "2 Minutes · 15 tasks"),
            180 to sh("3 Dakika · 24 görev", "3 Minutes · 24 tasks"),
            300 to sh("5 Dakika · 36 görev", "5 Minutes · 36 tasks"),
        ).chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (value, label) ->
                    val selected = seconds == value
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (selected) CompUi.GoldSoft else CompUi.Cream)
                            .border(1.dp, if (selected) CompUi.Gold else CompUi.Edge, RoundedCornerShape(14.dp))
                            .clickable(enabled = !starting) { onSeconds(value) }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = CompUi.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        Text(
            sh("3 ve 5 dakikalık yarışlarda hazırlık, strateji ve final var. İlerledikçe daha uzun kelimeler ve bitiş harfi görevleri gelir. Rakibini puanla geç.",
                "The 3- and 5-minute races have warm-up, strategy and final phases. Later tasks ask for longer words and specific ending letters. Outscore your rival."),
            color = CompUi.Ink.copy(alpha = .75f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val today = board?.today
        // Official daily race card.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(CompUi.NightTop, CompUi.Night)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(sh("GÜNLÜK YARIŞ", "DAILY RACE"), color = Color(0xFFFFD98A), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text(
                sh("Bugün herkes aynı harflerle yarışıyor. Tek resmî hakkın var!", "Everyone races with the same letters today. You get one official try!"),
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            )
            when {
                !online -> Text(sh("Yarış için internet bağlantısı gerekli.", "The race needs an internet connection."), color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                today?.finished == true -> {
                    val me = if (!weekly) board?.me else null
                    val total = board?.total ?: 0
                    Text(
                        sh("Bugünkü puanın: ${today.score}", "Today's score: ${today.score}") +
                            (me?.let { sh(" · ${it.rank}. / $total", " · #${it.rank} of $total") } ?: ""),
                        color = Color(0xFFBFF0C8), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(sh("Yeni yarış yarın başlıyor. Sıralamayı buradan izle.", "A new race starts tomorrow. Follow the board here."), color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                }
                today?.started == true -> Text(
                    sh("Bugünkü hakkını başlattın ama bitirmedin; puan kaydedilmedi. Yarın tekrar!", "You started today's try but didn't finish; no score saved. Try again tomorrow!"),
                    color = Color.White.copy(alpha = .85f), fontSize = 13.sp,
                )
                else -> {
                    Text(sh("Başladıktan sonra çıkarsan hakkın yanar.", "Leaving after you start uses up your try."), color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                    Button(
                        onClick = onDaily,
                        enabled = !starting,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC94A), contentColor = CompUi.Ink),
                    ) {
                        if (starting) CircularProgressIndicator(Modifier.size(20.dp), color = CompUi.Ink, strokeWidth = 2.dp)
                        else Text(sh("Yarışa Başla", "Start the Race"), fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onPractice,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CompUi.Edge),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CompUi.Ink, containerColor = CompUi.Cream),
        ) {
            Text(sh("Serbest Antrenman", "Free Practice"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        notice?.let { Text(it, color = CompUi.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }

        // Last week's reward.
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
                    Text(sh("Ödülünü al: +${reward.reward} Son Coin", "Claim your reward: +${reward.reward} Son Coin"), color = CompUi.InkMuted, fontSize = 13.sp)
                }
                Text(sh("AL", "CLAIM"), color = CompUi.Green, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }

        // Leaderboards.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CompUi.Cream)
                .border(1.dp, CompUi.Edge.copy(alpha = .6f), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardTab(sh("Bugün", "Today"), !weekly, Modifier.weight(1f)) { onWeekly(false) }
                BoardTab(sh("Bu Hafta", "This Week"), weekly, Modifier.weight(1f)) { onWeekly(true) }
            }
            if (weekly) {
                Text(
                    sh("Haftalık ödül: 1. 100 · 2. 60 · 3. 40 · 4–10. 15 Son Coin", "Weekly reward: 1st 100 · 2nd 60 · 3rd 40 · 4–10th 15 Son Coin"),
                    color = CompUi.InkMuted, fontSize = 12.sp,
                )
            }
            when {
                !online -> Text(sh("Sıralama çevrim dışıyken görünmez.", "The board is not available offline."), color = CompUi.InkMuted, fontSize = 13.sp)
                loadingBoard && board == null -> Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = CompUi.Green, strokeWidth = 2.dp)
                }
                board == null || board.rows.isEmpty() -> Text(
                    sh("Henüz kimse yarışmadı. İlk sen ol!", "No one has raced yet. Be the first!"),
                    color = CompUi.InkMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                else -> {
                    board.rows.take(20).forEach { BoardRow(it) }
                    val me = board.me
                    if (me != null && board.rows.take(20).none { it.me }) {
                        Text(sh("Sen: ${me.rank}. · ${me.score} puan", "You: #${me.rank} · ${me.score} pts"), color = CompUi.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(sh("${board.total} oyuncu", "${board.total} players"), color = CompUi.InkMuted, fontSize = 12.sp)
                }
            }
        }
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
        ProfilePhotoAvatar(avatarPath = row.avatarPath, name = row.name, size = 34.dp, accent = CompUi.Gold)
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
