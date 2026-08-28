package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

/** Expert arena only. Finished matches are deliberately handled by ComboOverlayV9. */
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
    var showChat by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }

    suspend fun discover(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter {
                (it.hostId == uid || it.guestId == uid) &&
                    it.status in listOf("playing", "quiz", "sudden_death")
            }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val current = room
            val next = if (current == null) {
                runCatching { discover() }.getOrNull()
            } else {
                runCatching { backend.getRoom(current.id) }.getOrNull()
            }
            if (next?.status == "finished") {
                room = null
                words = emptyList()
                input = ""
                showChat = false
            } else if (next != null) {
                room = next
                words = runCatching { backend.getWords(next.id) }.getOrDefault(words)
                val me = backend.currentUserId()
                if (me != null) {
                    playerProfile = runCatching { backend.getProfile(me) }.getOrNull()
                    val opponentId = if (next.hostId == me) next.guestId else next.hostId
                    opponentProfile = if (next.isBot) null else opponentId?.let { runCatching { backend.getProfile(it) }.getOrNull() }
                }
                if (showChat && !next.isBot) chat = runCatching { backend.getChat(next.id) }.getOrDefault(chat)
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
    val myTurn = active.currentPlayerId == me && active.status in listOf("playing", "sudden_death")
    var seconds by remember(active.turnDeadline) { mutableIntStateOf(45) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val timerPulse by rememberInfiniteTransition(label = "expertTimerPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (seconds <= 10) 1.09f else 1f,
        animationSpec = infiniteRepeatable(tween(if (seconds <= 10) 420 else 1000), RepeatMode.Reverse),
        label = "expertTimerBeat",
    )
    LaunchedEffect(active.turnDeadline, active.currentPlayerId, active.status) {
        while (active.turnDeadline != null && active.status in listOf("playing", "sudden_death")) {
            seconds = runCatching { (Instant.parse(active.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            delay(250)
        }
    }
    LaunchedEffect(myTurn, active.id) {
        if (myTurn) {
            delay(140)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
    val suffixLen = active.roundNo.coerceIn(1, 3)
    val last = words.lastOrNull()?.normalizedWord?.uppercase().orEmpty()
    val required = if (last.isBlank()) "" else last.takeLast(suffixLen)

    fun submitWord() {
        val submitted = input.trim()
        if (submitted.isBlank() || !myTurn || busy) return
        input = ""
        scope.launch {
            busy = true
            runCatching { backend.submitWord(active.id, submitted) }
                .onSuccess { r ->
                    room = r
                    notice = when (r.lastEvent) {
                        "wrong_start_letter" -> sh("Yanlış başlangıç. −1", "Wrong prefix. −1")
                        "invalid_word" -> sh("Geçersiz kelime. −1", "Invalid word. −1")
                        "word_already_used" -> sh("Kelime tekrarlandı. −1", "Word repeated. −1")
                        "expert_x2" -> sh("×2 PUAN!", "×2 SCORE!")
                        "expert_x3" -> sh("×3 PUAN!", "×3 SCORE!")
                        "bilbakalim_started" -> sh("Bil Bakalım başlıyor!", "Bil Bakalim is starting!")
                        else -> sh("Kelime kabul edildi.", "Word accepted.")
                    }
                }
                .onFailure { notice = sh("Hamle gönderilemedi.", "Move could not be sent.") }
            busy = false
        }
    }

    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                ExpertPlayerCard(playerProfile?.displayName ?: sh("SEN", "YOU"), playerProfile?.avatarPath, playerProfile?.gender, myScore, myRounds, myTurn, Modifier.weight(1f))
                Surface(
                    modifier = Modifier.scale(timerPulse),
                    shape = CircleShape,
                    color = if (seconds <= 10) SonHarfPink.copy(alpha = .16f) else SonHarfGold.copy(alpha = .16f),
                    border = BorderStroke(2.dp, if (seconds <= 10) SonHarfPink else SonHarfGold),
                ) {
                    Column(Modifier.size(72.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("$seconds", color = if (seconds <= 10) SonHarfPink else SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("sn • ×$suffixLen", color = if (seconds <= 10) SonHarfPink else SonHarfGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                ExpertPlayerCard(if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: sh("RAKİP", "OPPONENT"), opponentProfile?.avatarPath, opponentProfile?.gender, oppScore, oppRounds, !myTurn, Modifier.weight(1f), isBot = active.isBot)
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .25f)),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Text("${sh("UZMAN", "EXPERT")} • ROUND ${active.roundNo}/3", color = SonHarfGold, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                        Text("${active.roundWordCount}/15", color = SonHarfCyan, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterEnd))
                    }
                    Text(
                        if (active.status == "quiz") sh("BİL BAKALIM", "BIL BAKALIM") else if (myTurn) sh("SIRA SENDE", "YOUR TURN") else if (active.isBot && active.botTurn) sh("BOT DÜŞÜNÜYOR", "BOT IS THINKING") else sh("RAKİBİN SIRASI", "OPPONENT'S TURN"),
                        color = if (myTurn) SonHarfCyan else SonHarfMuted,
                        fontWeight = FontWeight.Black,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (suffixLen) {
                                1 -> sh("SON 1 HARF", "LAST 1 LETTER")
                                2 -> sh("SON 2 HARF • ×2", "LAST 2 LETTERS • ×2")
                                else -> sh("SON 3 HARF • ×3", "LAST 3 LETTERS • ×3")
                            },
                            color = SonHarfMuted,
                            fontSize = 10.sp,
                        )
                        Text(if (required.isBlank()) "•" else required, fontSize = if (suffixLen == 1) 70.sp else 52.sp, fontWeight = FontWeight.Black, color = if (suffixLen == 1) SonHarfText else SonHarfGold)
                        Text(if (required.isBlank()) sh("İlk kelimeyi başlat.", "Start the first word.") else sh("$required ile başlayan kelime yaz", "Enter a word beginning with $required"), color = SonHarfMuted, fontSize = 11.sp)
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text(sh("KELİME ZİNCİRİ", "WORD CHAIN"), color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(words.takeLast(30)) { w ->
                                Surface(shape = RoundedCornerShape(12.dp), color = SonHarfGold.copy(alpha = .10f), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .35f))) {
                                    Text(w.word.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (notice.isNotBlank()) Text(notice, Modifier.fillMaxWidth(), color = if ("−1" in notice) SonHarfPink else SonHarfGreen, textAlign = TextAlign.Center, fontSize = 10.sp)

            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(40) },
                enabled = myTurn && !busy && active.status != "quiz",
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(if (myTurn) sh("Kelimenizi yazın…", "Type your word…") else sh("Rakibin sırası…", "Opponent's turn…")) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send,
                    showKeyboardOnFocus = true,
                    hintLocales = LocaleList(Locale(if (active.language == "tr") "tr-TR" else "en-US")),
                ),
                keyboardActions = KeyboardActions(onSend = { submitWord() }),
                trailingIcon = { IconButton(onClick = { submitWord() }, enabled = myTurn && !busy) { Text("➤", fontSize = 22.sp) } },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .5f)),
                ) { Text(sh("⚑ PES ET", "⚑ FORFEIT"), color = SonHarfPink) }
                OutlinedButton(
                    onClick = {
                        if (active.isBot) notice = sh("Bot maçında sohbet kapalı.", "Chat is disabled in bot matches.")
                        else scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()); showChat = true }
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SonHarfCyan.copy(alpha=.5f)),
                ) { Text(sh("● SOHBET", "● CHAT"), color = SonHarfCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showChat && !active.isBot) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text(sh("MAÇ SOHBETİ", "MATCH CHAT"), fontWeight = FontWeight.Black, fontSize = 21.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(chat.takeLast(30), key = { it.id }) { m ->
                            Text((if (m.senderId == me) sh("Sen: ", "You: ") else sh("Rakip: ", "Opponent: ")) + m.body, fontSize = 15.sp)
                        }
                    }
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it.take(300) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send,
                            showKeyboardOnFocus = true,
                            hintLocales = LocaleList(Locale(if (active.language == "tr") "tr-TR" else "en-US")),
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            val message = chatInput.trim()
                            if (message.isNotEmpty()) scope.launch {
                                runCatching { backend.sendChat(active.id, message) }.onSuccess {
                                    chatInput = ""
                                    chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                                }
                            }
                        }),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val message = chatInput.trim()
                    if (message.isNotEmpty()) scope.launch {
                        runCatching { backend.sendChat(active.id, message) }.onSuccess {
                            chatInput = ""
                            chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                        }
                    }
                }) { Text(sh("GÖNDER", "SEND"), fontSize = 16.sp) }
            },
            dismissButton = { TextButton(onClick = { showChat = false }) { Text(sh("KAPAT", "CLOSE"), fontSize = 16.sp) } },
        )
    }
}

@Composable
private fun ExpertPlayerCard(name: String, avatarPath: String?, gender: String?, score: Int, rounds: Int, active: Boolean, modifier: Modifier, isBot: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (active) SonHarfCyan.copy(alpha = .11f) else SonHarfSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (active) SonHarfCyan.copy(alpha = .5f) else SonHarfMuted.copy(alpha = .12f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (isBot) {
                Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = SonHarfPink.copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Text("🤖", fontSize = 23.sp) } }
            } else {
                Box {
                    ProfilePhotoAvatar(avatarPath, name, 42.dp, visible = true, accent = if (active) SonHarfCyan else SonHarfPurple)
                    val g = gender?.trim()?.lowercase()
                    val female = g in setOf("kadın", "kadin", "female", "woman")
                    val male = g in setOf("erkek", "male", "man")
                    if (female || male) {
                        Surface(Modifier.align(Alignment.BottomEnd).size(15.dp), shape = CircleShape, color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2), border = BorderStroke(1.dp, Color.White)) {
                            Box(contentAlignment = Alignment.Center) { Text(if (female) "♀" else "♂", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name, maxLines = 1, fontSize = 9.sp, color = SonHarfMuted, textAlign = TextAlign.Center)
                Text("$score", fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("$rounds ${sh("round", "round")}", fontSize = 7.sp, color = SonHarfMuted)
            }
        }
    }
}
