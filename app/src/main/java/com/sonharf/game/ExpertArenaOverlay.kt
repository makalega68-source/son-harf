package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExpertArenaOverlay() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf<String?>(null) }

    suspend fun discover(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { it.id != dismissed && (it.hostId == uid || it.guestId == uid) && it.status in listOf("playing","sudden_death","finished") }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val current = room
            val next = if (current == null) runCatching { discover() }.getOrNull() else runCatching { backend.getRoom(current.id) }.getOrNull()
            if (next != null) {
                room = next
                words = runCatching { backend.getWords(next.id) }.getOrDefault(words)
            }
            delay(500)
        }
    }
    LaunchedEffect(notice) { if (notice.isNotBlank()) { delay(1800); notice = "" } }

    val active = room ?: return
    val me = backend.currentUserId()
    val host = me == active.hostId
    val myScore = if (host) active.hostScore else active.guestScore
    val oppScore = if (host) active.guestScore else active.hostScore
    val myRounds = if (host) active.hostRounds else active.guestRounds
    val oppRounds = if (host) active.guestRounds else active.hostRounds
    val myTurn = active.currentPlayerId == me && active.status in listOf("playing","sudden_death")
    val suffixLen = active.roundNo.coerceIn(1,3)
    val last = words.lastOrNull()?.normalizedWord?.uppercase().orEmpty()
    val required = if (last.isBlank()) "" else last.takeLast(suffixLen)

    if (active.status == "finished") {
        Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
            Box(Modifier.statusBarsPadding().navigationBarsPadding().padding(22.dp), contentAlignment = Alignment.Center) {
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(sh("UZMAN MODU TAMAMLANDI", "EXPERT MODE COMPLETE"), color = SonHarfGold, fontWeight = FontWeight.Black)
                        Text("$myRounds - $oppRounds", fontSize = 42.sp, fontWeight = FontWeight.Black)
                        Text("$myScore - $oppScore", color = SonHarfMuted)
                        Button(onClick = { scope.launch { runCatching { backend.restartBotMatch(active.id) }.onSuccess { room = it; words = emptyList() } } }, enabled = active.isBot, modifier = Modifier.fillMaxWidth()) { Text(sh("TEKRAR OYNA", "PLAY AGAIN")) }
                        OutlinedButton(onClick = { dismissed = active.id; room = null; words = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text(sh("LOBİYE DÖN", "BACK TO LOBBY")) }
                    }
                }
            }
        }
        return
    }

    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                ExpertPlayerCard(sh("SEN", "YOU"), myScore, myRounds, myTurn, Modifier.weight(1f))
                Surface(shape = CircleShape, color = SonHarfGold.copy(alpha=.16f), border = BorderStroke(2.dp, SonHarfGold)) {
                    Column(Modifier.size(64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("×$suffixLen", color = SonHarfGold, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text(sh("PUAN", "SCORE"), color = SonHarfMuted, fontSize = 7.sp)
                    }
                }
                ExpertPlayerCard(if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else sh("RAKİP", "OPPONENT"), oppScore, oppRounds, !myTurn, Modifier.weight(1f))
            }

            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha=.25f))) {
                Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${sh("UZMAN", "EXPERT")} • ROUND ${active.roundNo}/3", color = SonHarfGold, fontWeight = FontWeight.Black)
                        Text("${active.roundWordCount}/15", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    }
                    Text(if (myTurn) sh("SIRA SENDE", "YOUR TURN") else if (active.isBot && active.botTurn) sh("BOT DÜŞÜNÜYOR", "BOT IS THINKING") else sh("RAKİBİN SIRASI", "OPPONENT'S TURN"), color = if (myTurn) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Black)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(when (suffixLen) { 1 -> sh("SON 1 HARF", "LAST 1 LETTER"); 2 -> sh("SON 2 HARF • ×2", "LAST 2 LETTERS • ×2"); else -> sh("SON 3 HARF • ×3", "LAST 3 LETTERS • ×3") }, color = SonHarfMuted, fontSize = 10.sp)
                        Text(if (required.isBlank()) "•" else required, fontSize = if (suffixLen == 1) 70.sp else 52.sp, fontWeight = FontWeight.Black, color = if (suffixLen == 1) SonHarfText else SonHarfGold)
                        Text(if (required.isBlank()) sh("İlk kelimeyi başlat.", "Start the first word.") else sh("$required ile başlayan kelime yaz", "Enter a word beginning with $required"), color = SonHarfMuted, fontSize = 11.sp)
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text(sh("KELİME ZİNCİRİ", "WORD CHAIN"), color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(words.takeLast(15)) { w -> Surface(shape = RoundedCornerShape(12.dp), color = SonHarfSurface2) { Text(w.word.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp) } }
                        }
                    }
                }
            }

            if (notice.isNotBlank()) Text(notice, Modifier.fillMaxWidth(), color = if ("−1" in notice) SonHarfPink else SonHarfGreen, textAlign = TextAlign.Center, fontSize = 10.sp)

            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(40) },
                enabled = myTurn && !busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (myTurn) sh("Kelimenizi yazın…", "Type your word…") else sh("Rakibin sırası…", "Opponent's turn…")) },
                trailingIcon = {
                    IconButton(onClick = {
                        val submitted = input.trim(); if (submitted.isBlank() || !myTurn || busy) return@IconButton
                        scope.launch {
                            busy = true
                            runCatching { backend.submitWord(active.id, submitted) }
                                .onSuccess { r ->
                                    room = r; input = ""
                                    notice = when (r.lastEvent) {
                                        "wrong_start_letter" -> sh("Yanlış başlangıç. −1", "Wrong prefix. −1")
                                        "invalid_word" -> sh("Geçersiz kelime. −1", "Invalid word. −1")
                                        "word_already_used" -> sh("Kelime tekrarlandı. −1", "Word repeated. −1")
                                        "expert_x2" -> sh("×2 PUAN!", "×2 SCORE!")
                                        "expert_x3" -> sh("×3 PUAN!", "×3 SCORE!")
                                        else -> sh("Kelime kabul edildi.", "Word accepted.")
                                    }
                                }
                                .onFailure { notice = sh("Hamle gönderilemedi.", "Move could not be sent.") }
                            busy = false
                        }
                    }) { Text("➤", fontSize = 22.sp) }
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, SonHarfPink.copy(alpha=.5f))) { Text(sh("⚑ PES ET", "⚑ FORFEIT"), color = SonHarfPink) }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) { Text(sh("UZMAN MODU", "EXPERT MODE"), color = SonHarfGold) }
            }
        }
    }
}

@Composable private fun ExpertPlayerCard(name: String, score: Int, rounds: Int, active: Boolean, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (active) SonHarfCyan.copy(alpha=.11f) else SonHarfSurface), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (active) SonHarfCyan.copy(alpha=.5f) else SonHarfMuted.copy(alpha=.12f))) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, maxLines = 1, fontSize = 9.sp, color = SonHarfMuted); Text("$score", fontSize = 24.sp, fontWeight = FontWeight.Black); Text("$rounds ${sh("round", "round")}", fontSize = 7.sp, color = SonHarfMuted)
        }
    }
}
