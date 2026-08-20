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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ChatMessageDto
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.TriviaQuestionDto
import com.sonharf.game.data.TriviaRoundDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Final active-match arena based on the user's hand-drawn layout. */
@Composable
fun SketchGameOverlayV8() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var showChat by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var dismissedRoomId by remember { mutableStateOf<String?>(null) }
    var isVip by remember { mutableStateOf(false) }

    suspend fun discoverRoom(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        isVip = runCatching { backend.getProfile(uid).isVip }.getOrDefault(false)
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter {
                it.id != dismissedRoomId &&
                    (it.hostId == uid || it.guestId == uid) &&
                    it.status in listOf("playing", "quiz", "final", "sudden_death", "paused")
            }
            .maxByOrNull { it.validWordCount }
    }

    suspend fun refreshTrivia(r: GameRoomDto) {
        if (r.status == "quiz") {
            triviaRound = runCatching { backend.getActiveTriviaRound(r.id) }.getOrNull()
            triviaQuestion = triviaRound?.let { runCatching { backend.getTriviaQuestion(it.questionId) }.getOrNull() }
        } else {
            triviaRound = null
            triviaQuestion = null
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val current = room
            if (current == null) {
                val found = runCatching { discoverRoom() }.getOrNull()
                if (found != null) {
                    room = found
                    words = runCatching { backend.getWords(found.id) }.getOrDefault(emptyList())
                    refreshTrivia(found)
                    notice = ""
                }
            } else {
                val updated = runCatching { backend.getRoom(current.id) }.getOrNull()
                if (updated != null) {
                    room = updated
                    words = runCatching { backend.getWords(updated.id) }.getOrDefault(words)
                    refreshTrivia(updated)
                    if (showChat && !updated.isBot) chat = runCatching { backend.getChat(updated.id) }.getOrDefault(chat)
                }
            }
            delay(550)
        }
    }

    val active = room ?: return
    val me = backend.currentUserId()

    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { input = "" }

    fun resultMessage(result: GameRoomDto, submitted: String): String = when {
        result.lastEventPlayerId == me && result.lastEvent == "word_already_used" -> sh("Bu kelime daha önce kullanıldı. −1", "This word was already used. −1")
        result.lastEventPlayerId == me && result.lastEvent == "wrong_start_letter" -> sh("Kelime son harfle başlamalı. −1", "Word must start with the last letter. −1")
        result.lastEventPlayerId == me && result.lastEvent == "invalid_word" -> sh("Geçersiz kelime. −1", "Invalid word. −1")
        result.lastEventPlayerId == me && result.lastEvent == "turn_expired" -> sh("Süre doldu. −1", "Time expired. −1")
        result.lastEvent == "provisional_word" -> sh("${submitted.uppercase()} kabul edildi • sözlük kontrolüne eklendi", "${submitted.uppercase()} accepted • queued for review")
        else -> sh("${submitted.uppercase()} kabul edildi", "${submitted.uppercase()} accepted")
    }

    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        SketchArenaV8(
            room = active,
            me = me,
            words = words,
            input = input,
            onInput = { input = it.take(40) },
            busy = busy,
            notice = notice,
            triviaRound = triviaRound,
            triviaQuestion = triviaQuestion,
            isVip = isVip,
            onSubmit = {
                val submitted = input.trim()
                if (submitted.isBlank() || busy) return@SketchArenaV8
                scope.launch {
                    busy = true
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { result ->
                            room = result
                            input = ""
                            notice = resultMessage(result, submitted)
                            if (result.lastEventPlayerId == me && result.lastEvent in setOf("word_already_used", "wrong_start_letter", "invalid_word", "turn_expired")) SonHarfSoundFx.warning() else SonHarfSoundFx.wordAccepted()
                        }
                        .onFailure { error ->
                            val raw = error.message.orEmpty()
                            notice = when {
                                "not_your_turn" in raw -> sh("Sıra rakibinde.", "Opponent's turn.")
                                "room_not_playing" in raw -> sh("Bu aşamada kelime gönderilemez.", "A word cannot be sent in this phase.")
                                else -> sh("Hamle gönderilemedi. Tekrar dene.", "Move could not be sent. Try again.")
                            }
                            SonHarfSoundFx.warning()
                        }
                    busy = false
                }
            },
            onTrivia = { index ->
                scope.launch {
                    val round = triviaRound ?: return@launch
                    runCatching { backend.answerTrivia(round.id, index) }
                        .onSuccess { updated -> room = updated; refreshTrivia(updated); notice = sh("Bonus yanıtın işlendi.", "Bonus answer submitted.") }
                        .onFailure { notice = sh("Bonus yanıtı gönderilemedi.", "Bonus answer could not be sent.") }
                }
            },
            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onChat = {
                if (active.isBot) notice = sh("Bot maçında sohbet kapalı.", "Chat is disabled in bot matches.")
                else scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()); showChat = true }
            },
            onRematch = {
                scope.launch {
                    runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }
                        .onSuccess { next -> room = next; words = emptyList(); input = ""; notice = sh("Rövanş başlıyor.", "Rematch starting.") }
                        .onFailure { notice = sh("Rövanş başlatılamadı.", "Rematch could not start.") }
                }
            },
            onExit = { dismissedRoomId = active.id; room = null; words = emptyList(); input = "" },
        )
    }

    if (showChat && !active.isBot) {
        SketchChatDialogV8(chat, me, chatInput, { chatInput = it.take(300) }, { showChat = false }) {
            if (chatInput.isBlank()) return@SketchChatDialogV8
            scope.launch {
                runCatching { backend.sendChat(active.id, chatInput) }
                    .onSuccess { chatInput = ""; chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat) }
            }
        }
    }
}

@Composable
private fun SketchArenaV8(
    room: GameRoomDto,
    me: String?,
    words: List<GameWordDto>,
    input: String,
    onInput: (String) -> Unit,
    busy: Boolean,
    notice: String,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    isVip: Boolean,
    onSubmit: () -> Unit,
    onTrivia: (Int) -> Unit,
    onForfeit: () -> Unit,
    onChat: () -> Unit,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val opponentScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val opponentRounds = if (host) room.guestRounds else room.hostRounds
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val lastWord = words.lastOrNull()?.normalizedWord?.uppercase()
    val required = lastWord?.lastOrNull()?.toString() ?: ""
    val playerName = sh("SEN", "YOU")
    val opponentName = if (room.isBot) "${room.botName ?: if (room.language == "en") "WordBot" else "KelimeBot"} BOT" else sh("RAKİP", "OPPONENT")
    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        var lastTick = -1
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (java.time.Instant.parse(room.turnDeadline).epochSecond - java.time.Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            if (seconds in 1..10 && seconds != lastTick) {
                lastTick = seconds
                SonHarfSoundFx.countdown()
            }
            if (seconds <= 0) break
            delay(250)
        }
    }

    if (room.status == "finished") {
        val won = room.winnerId == me || (room.isBot && room.winnerId == null && myScore > opponentScore)
        Box(Modifier.fillMaxSize().background(SonHarfBg).padding(22.dp), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (won) sh("KAZANDIN", "YOU WON") else sh("MAÇ BİTTİ", "MATCH OVER"), fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("$myRounds - $opponentRounds", fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text(sh("Puan $myScore - $opponentScore", "Score $myScore - $opponentScore"), color = SonHarfMuted)
                    Button(onClick = onRematch, modifier = Modifier.fillMaxWidth()) { Text(sh("TEKRAR OYNA", "PLAY AGAIN"), fontWeight = FontWeight.Black) }
                    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text(sh("LOBİYE DÖN", "BACK TO LOBBY")) }
                }
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SonHarfSurface2, SonHarfBg, SonHarfSurface))).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SketchPlayerCardV8(playerName, myScore, myRounds, myTurn, SonHarfCyan, Modifier.weight(1f))
            Box(Modifier.size(62.dp).clip(CircleShape).background(if (seconds <= 10) SonHarfPink else SonHarfGold).padding(3.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$seconds", fontSize = 23.sp, fontWeight = FontWeight.Black, color = if (seconds <= 10) SonHarfPink else SonHarfText)
                        Text(sh("sn", "sec"), fontSize = 8.sp, color = SonHarfMuted)
                    }
                }
            }
            SketchPlayerCardV8(opponentName, opponentScore, opponentRounds, !myTurn, SonHarfPink, Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .16f)),
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("${room.roundWordCount}/10", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(sh("GENEL KÜLTÜR • +${triviaRound.bonusPoints}", "TRIVIA • +${triviaRound.bonusPoints}"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text(triviaQuestion.question, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                        listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD).forEachIndexed { index, option ->
                            OutlinedButton(onClick = { onTrivia(index) }, modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp), contentPadding = PaddingValues(4.dp)) {
                                Text(option, fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    Text(if (myTurn) sh("SIRA SENDE", "YOUR TURN") else if (room.isBot && room.botTurn) sh("BOT DÜŞÜNÜYOR", "BOT THINKING") else sh("RAKİBİN SIRASI", "OPPONENT'S TURN"), color = if (myTurn) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    if (lastWord == null) {
                        Text(sh("İLK KELİME", "FIRST WORD"), fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    } else {
                        Text(
                            buildAnnotatedString {
                                if (lastWord.length > 1) append(lastWord.dropLast(1))
                                withStyle(SpanStyle(color = SonHarfPink, fontWeight = FontWeight.Black)) { append(lastWord.takeLast(1)) }
                            },
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(if (required.isBlank()) sh("İlk kelimeyi sen başlat", "Start with any valid word") else sh("$required ile başlayan bir kelime yaz", "Enter a word starting with $required"), color = SonHarfMuted, fontSize = 11.sp)
                }
            }
        }

        if (words.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth().height(38.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                items(words.takeLast(8)) { word ->
                    val border = if (isVip) SonHarfGold.copy(alpha = .55f) else SonHarfMuted.copy(alpha = .14f)
                    Surface(shape = RoundedCornerShape(10.dp), color = if (isVip) SonHarfPurple.copy(alpha = .08f) else SonHarfSurface2, border = BorderStroke(1.dp, border)) {
                        Text(word.word.uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else Spacer(Modifier.height(38.dp))

        if (isVip) {
            Text(sh("♛ VIP kelime zinciri görünümü aktif", "♛ VIP word-chain style active"), color = SonHarfGold, fontSize = 8.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        if (notice.isNotBlank()) {
            val warning = notice.contains("−1") || notice.contains("gönderilemedi") || notice.contains("could not")
            Text(notice, modifier = Modifier.fillMaxWidth(), color = if (warning) SonHarfPink else SonHarfMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(15.dp),
            color = SonHarfSurface,
            border = BorderStroke(1.5.dp, if (myTurn) SonHarfCyan.copy(alpha = .75f) else SonHarfMuted.copy(alpha = .25f)),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (input.isBlank()) if (myTurn) sh("Kelimenizi yazın…", "Type your word…") else sh("Rakibin sırası…", "Opponent's turn…") else input.uppercase(), modifier = Modifier.weight(1f), color = if (input.isBlank()) SonHarfMuted else SonHarfText, fontSize = 17.sp, maxLines = 1)
                Button(onClick = onSubmit, enabled = myTurn && input.length >= 2 && !busy, modifier = Modifier.size(39.dp), shape = CircleShape, contentPadding = PaddingValues(0.dp)) { Text("➤") }
            }
        }

        SketchKeyboardV8(room.language, myTurn && !busy && room.status != "quiz", input, onInput, onSubmit)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.3.dp, SonHarfPink.copy(alpha = .70f))) {
                Text(sh("⚑ PES ET", "⚑ FORFEIT"), color = SonHarfPink, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
            OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.3.dp, SonHarfCyan.copy(alpha = .70f))) {
                Text(sh("● SOHBET", "● CHAT"), color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SketchKeyboardV8(language: String, enabled: Boolean, input: String, onInput: (String) -> Unit, onSubmit: () -> Unit) {
    val rows = if (language == "en") {
        listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("z","x","c","v","b","n","m"),
        )
    } else {
        listOf(
            listOf("q","w","e","r","t","y","u","ı","o","p","ğ","ü"),
            listOf("a","s","d","f","g","h","j","k","l","ş","i"),
            listOf("z","x","c","v","b","n","m","ö","ç"),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { keys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                keys.forEach { key ->
                    Button(
                        onClick = { onInput(input + key) },
                        enabled = enabled && input.length < 40,
                        modifier = Modifier.weight(1f).height(43.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfSurface2, contentColor = SonHarfText, disabledContainerColor = SonHarfSurface2.copy(alpha = .45f)),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(key.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = { if (input.isNotEmpty()) onInput(input.dropLast(1)) },
                enabled = enabled && input.isNotEmpty(),
                modifier = Modifier.weight(1f).height(43.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (SonHarfUiState.darkMode) Color(0xFF28344A) else Color(0xFFD5DCE8), contentColor = SonHarfText),
            ) { Text("⌫", fontSize = 19.sp, fontWeight = FontWeight.Black) }
            Button(onClick = onSubmit, enabled = enabled && input.length >= 2, modifier = Modifier.weight(2.2f).height(43.dp), shape = RoundedCornerShape(9.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)) {
                Text(sh("GÖNDER", "SEND"), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SketchPlayerCardV8(name: String, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {
    Card(modifier = modifier.height(62.dp), colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = .10f) else SonHarfSurface), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, if (active) accent.copy(alpha = .55f) else SonHarfMuted.copy(alpha = .13f))) {
        Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(name, maxLines = 1, color = if (active) accent else SonHarfMuted, fontSize = 8.sp)
                Text(score.toString(), fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text("$rounds round", color = SonHarfMuted, fontSize = 7.sp)
            }
        }
    }
}

@Composable
private fun SketchChatDialogV8(messages: List<ChatMessageDto>, me: String?, input: String, onInput: (String) -> Unit, onClose: () -> Unit, onSend: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(sh("SOHBET", "CHAT"), fontWeight = FontWeight.Black) },
        text = {
            Column {
                messages.takeLast(8).forEach { message ->
                    Text((if (message.senderId == me) sh("Sen: ", "You: ") else sh("Rakip: ", "Opponent: ")) + message.body, fontSize = 11.sp, modifier = Modifier.padding(vertical = 3.dp))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = input, onValueChange = onInput, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) })
            }
        },
        confirmButton = { TextButton(onClick = onSend, enabled = input.isNotBlank()) { Text(sh("GÖNDER", "SEND")) } },
        dismissButton = { TextButton(onClick = onClose) { Text(sh("KAPAT", "CLOSE")) } },
    )
}
