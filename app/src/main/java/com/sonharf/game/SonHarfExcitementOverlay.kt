package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

/**
 * Additive HUD only. It never writes match state and therefore cannot alter scoring,
 * matchmaking, turn ownership or server reconciliation.
 */
@Composable
internal fun SonHarfExcitementOverlay() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            room = if (me == null) null else runCatching {
                SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
                    .filter { (it.hostId == me || it.guestId == me) && it.status in setOf("playing", "quiz", "final", "sudden_death", "paused") }
                    .maxByOrNull { it.validWordCount }
            }.getOrNull()
            delay(600)
        }
    }

    val active = room ?: return
    val me = backend.currentUserId()
    val host = me == active.hostId
    val myStreak = if (host) active.hostStreak else active.guestStreak
    val oppStreak = if (host) active.guestStreak else active.hostStreak
    val myScore = if (host) active.hostScore else active.guestScore
    val oppScore = if (host) active.guestScore else active.hostScore
    val remainingToMilestone = (5 - (active.validWordCount % 5)).let { if (it == 0) 5 else it }
    val statusText = when (active.status) {
        "quiz" -> sh("🧠 BİL BAKALIM TURU", "🧠 TRIVIA ROUND")
        "final" -> sh("⚡ FİNAL HAMLELERİ", "⚡ FINAL MOVES")
        "sudden_death" -> sh("🔥 ANİ ÖLÜM", "🔥 SUDDEN DEATH")
        "paused" -> sh("⏸ BAĞLANTI BEKLENİYOR", "⏸ WAITING FOR CONNECTION")
        else -> sh("🧠 Bil Bakalım'a $remainingToMilestone kelime", "$remainingToMilestone words to trivia")
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier.statusBarsPadding().padding(top = 88.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(13.dp),
            color = Color(0xF4FFFFFF),
            border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .34f)),
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("🔥 $myStreak", color = SonHarfPink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text("$myScore-$oppScore", color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(statusText, modifier = Modifier.weight(1f, fill = false), color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                if (oppStreak >= 3) Text("Rakip 🔥$oppStreak", color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
