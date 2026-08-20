package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Active-match-only arena based on the user's hand-drawn layout.
 *
 * The existing V6 screen remains responsible for lobby, matchmaking, private-room
 * creation and its server-side turn driver. This overlay mounts only when there is
 * a live match and becomes the single interactive arena visible to the player.
 */
@Composable
fun SketchGameOverlayV7() {
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

    suspend fun discoverRoom(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
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
            delay(650)
        }
    }

    val active = room ?: return
    val me = backend.currentUserId()

    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) {
        input = ""
    }

    fun resultMessage(result: GameRoomDto, submitted: String): String = when {
        result.lastEventPlayerId == me && result.lastEvent == "word_already_used" -> sh("Bu kelime daha önce kullanıldı. −1", "This word was already used. −1")
        result.lastEventPlayerId == me && result.lastEvent == "wrong_start_letter" -> sh("Kelime son harfle başlamalı. −1", "Word must start with the last letter. −1")
        result.lastEventPlayerId == me && result.lastEvent == "not_in_dictionary" -> sh("Kelime doğrulanamadı. −1", "Word could not be validated. −1")
        result.lastEventPlayerId == me && result.lastEvent == "invalid_word" -> sh("Geçersiz kelime. −1", "Invalid word. −1")
        result.lastEventPlayerId == me && result.lastEvent == "turn_expired" -> sh("Süre doldu. −1", "Time expired. −1")
        result.lastEvent == "provisional_word" -> sh("${submitted.uppercase()} kabul edildi • sözlük incelemesine eklendi", "${submitted.uppercase()} accepted • queued for dictionary review")
        else -> sh("${submitted.uppercase()} kabul edildi", "${submitted.uppercase()} accepted")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SonHarfBg,
    ) {
        SketchArenaV7(
            room = active,
            me = me,
            words = words,
            input = input,
            onInput = { input = it.take(40) },
            busy = busy,
            notice = notice,
            triviaRound = triviaRound,
            triviaQuestion = triviaQuestion,
            onSubmit = {
                val submitted = input.trim()
                if (submitted.isBlank() || busy) return@SketchArenaV7
                scope.launch {
                    busy = true
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { result ->
                            room = result
                            input = ""
                            notice = resultMessage(result, submitted)
                            if (result.lastEventPlayerId == me && result.lastEvent in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired")) SonHarfSoundFx.warning()
                            else SonHarfSoundFx.wordAccepted()
                        }
                        .onFailure { error ->
                            val raw = error.message.orEmpty()
                            notice = when {
                                "not_your_turn" in raw -> sh("Sıra rakibinde.", "Opponent's turn.")
                                "room_not_playing" in raw -> sh("Bu turda kelime gönderilemez.", "A word cannot be sent in this phase.")
                                "permission denied" in raw -> sh("Sunucu yetkisi yenileniyor. Tekrar dene.", "Server permission is refreshing. Try again.")
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
            onForfeit = {
                scope.launch {
                    runCatching { backend.forfeit(active.id) }.onSuccess { room = it }
                }
            },
            onChat = {
                if (active.isBot) {
                    notice = sh("Bot maçında sohbet kapalı.", "Chat is disabled in bot matches.")
                } else {
                    scope.launch {
                        chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList())
                        showChat = true
                    }
                }
            },
            onRematch = {
                scope.launch {
                    runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }
                        .onSuccess { next -> room = next; words = emptyList(); input = ""; notice = sh("Rövanş başlıyor.", "Rematch starting.") }
                        .onFailure { notice = sh("Rövanş başlatılamadı.", "Rematch could not start.") }
                }
            },
            onExit = {
                dismissedRoomId = active.id
                room = null
                words = emptyList()
                input = ""
            },
        )
    }

    if (showChat && !active.isBot) {
        SketchChatDialogV7(
            messages = chat,
            me = me,
            input = chatInput,
            onInput = { chatInput = it.take(300) },
            onClose = { showChat = false },
            onSend = {
                if (chatInput.isBlank()) return@SketchChatDialogV7
                scope.launch {
                    runCatching { backend.sendChat(active.id, chatInput) }
                        .onSuccess { chatInput = ""; chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat) }
                }
            },
        )
    }
}

@Composable
private fun SketchArenaV7(
    room: GameRoomDto,
    me: String?,
    words: List<GameWordDto>,
    input: String,
    onInput: (String) -> Unit,
    busy: Boolean,
    notice: String,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
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
    val lastWord = words.lastOrNull()?.normalizedWord
    val required = lastWord?.lastOrNull()?.uppercaseChar()?.toString() ?: "•"
    val playerName = sh("SEN", "YOU")
    val opponentName = if (room.isBot) "${room.botName ?: if (room.language == "en") "WordBot" else "KelimeBot"} BOT" else sh("RAKİP", "OPPONENT")
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching {
                (java.time.Instant.parse(room.turnDeadline).epochSecond - java.time.Instant.now().epochSecond).toInt().coerceAtLeast(0)
            }.getOrDefault(45)
            if (seconds <= 0) break
            delay(1000)
        }
    }

    if (room.status == "finished") {
        val won = room.winnerId == me || (room.isBot && room.winnerId == null && myScore > opponentScore)
        Box(Modifier.fillMaxSize().background(SonHarfBg).padding(22.dp), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, if (won) SonHarfGold.copy(alpha = .55f) else SonHarfPink.copy(alpha = .40f)),
            ) {
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
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SonHarfSurface2, SonHarfBg, SonHarfSurface))).padding(horizontal = 12.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            SketchPlayerCardV7(playerName, myScore, myRounds, myTurn, SonHarfCyan, Modifier.weight(1f))
            Box(Modifier.size(66.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(SonHarfCyan, SonHarfGold, SonHarfPink, SonHarfCyan))).padding(3.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$seconds", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(sh("sn", "sec"), fontSize = 8.sp, color = SonHarfMuted)
                    }
                }
            }
            SketchPlayerCardV7(opponentName, opponentScore, opponentRounds, !myTurn, SonHarfPink, Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", fontWeight = FontWeight.Black)
                    Text("${room.roundWordCount}/10", color = SonHarfCyan, fontWeight = FontWeight.Black)
                }

                if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) {
                    Text(sh("GENEL KÜLTÜR • +${triviaRound.bonusPoints}", "TRIVIA • +${triviaRound.bonusPoints}"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(triviaQuestion.question, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    val options = listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD)
                    options.chunked(2).forEachIndexed { rowIndex, pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            pair.forEachIndexed { colIndex, option ->
                                val index = rowIndex * 2 + colIndex
                                OutlinedButton(onClick = { onTrivia(index) }, modifier = Modifier.weight(1f).heightIn(min = 42.dp), contentPadding = PaddingValues(6.dp)) {
                                    Text(option, fontSize = 10.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    Text(if (myTurn) sh("SIRA SENDE", "YOUR TURN") else if (room.isBot && room.botTurn) sh("BOT DÜŞÜNÜYOR", "BOT THINKING") else sh("RAKİBİN SIRASI", "OPPONENT'S TURN"), color = if (myTurn) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(sh("KELİME", "WORD"), color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(if (lastWord == null) sh("İlk kelimeyi yaz", "Enter the first word") else sh("$required ile başlayan kelime yaz", "Enter a word starting with $required"), color = SonHarfMuted, fontSize = 11.sp)
                        }
                        Surface(shape = RoundedCornerShape(16.dp), color = SonHarfPurple.copy(alpha = .13f), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f))) {
                            Text(required, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), fontSize = 42.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        if (words.isNotEmpty()) {
            Column {
                Text(sh("KELİME ZİNCİRİ", "WORD CHAIN"), color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(words.takeLast(8)) { word ->
                        Surface(shape = RoundedCornerShape(12.dp), color = SonHarfSurface2, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f))) {
                            Text(word.word.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (notice.isNotBlank()) {
            val warning = notice.contains("−1") || notice.contains("gönderilemedi") || notice.contains("could not") || notice.contains("yetkisi")
            Surface(color = if (warning) SonHarfPink.copy(alpha = .14f) else SonHarfCyan.copy(alpha = .08f), shape = RoundedCornerShape(12.dp)) {
                Text(notice, Modifier.fillMaxWidth().padding(8.dp), color = if (warning) SonHarfPink else SonHarfMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = SonHarfSurface,
            border = BorderStroke(1.5.dp, if (myTurn) SonHarfCyan.copy(alpha = .75f) else SonHarfMuted.copy(alpha = .25f)),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (input.isBlank()) if (myTurn) sh("Kelimenizi yazın…", "Type your word…") else sh("Rakibin sırası…", "Opponent's turn…") else input.uppercase(), modifier = Modifier.weight(1f), color = if (input.isBlank()) SonHarfMuted else SonHarfText, fontSize = 18.sp)
                Button(onClick = onSubmit, enabled = myTurn && input.length >= 2 && !busy, modifier = Modifier.size(42.dp), shape = CircleShape, contentPadding = PaddingValues(0.dp)) { Text("➤") }
            }
        }

        Text(sh("KLAVYE", "KEYBOARD"), color = SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 9.sp)
        SketchKeyboardV7(language = room.language, enabled = myTurn && !busy && room.status != "quiz", input = input, onInput = onInput, onSubmit = onSubmit)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f).height(44.dp), border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .55f))) {
                Text(sh("⚑ PES ET", "⚑ FORFEIT"), color = SonHarfPink, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(44.dp), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .55f))) {
                Text(sh("● SOHBET", "● CHAT"), color = SonHarfCyan, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SketchKeyboardV7(language: String, enabled: Boolean, input: String, onInput: (String) -> Unit, onSubmit: () -> Unit) {
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

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { keys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                keys.forEach { key ->
                    Button(
                        onClick = { onInput(input + key) },
                        enabled = enabled && input.length < 40,
                        modifier = Modifier.weight(1f).height(39.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfSurface2, contentColor = SonHarfText, disabledContainerColor = SonHarfSurface2.copy(alpha = .45f)),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(key.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick = { if (input.isNotEmpty()) onInput(input.dropLast(1)) }, enabled = enabled && input.isNotEmpty(), modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfSurface2)) { Text("⌫") }
            Button(onClick = onSubmit, enabled = enabled && input.length >= 2, modifier = Modifier.weight(2f).height(40.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)) { Text(sh("GÖNDER", "SEND"), fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun SketchPlayerCardV7(name: String, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = .10f) else SonHarfSurface), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, if (active) accent.copy(alpha = .55f) else SonHarfMuted.copy(alpha = .13f))) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(7.dp))
            Column {
                Text(name, maxLines = 1, color = if (active) accent else SonHarfMuted, fontSize = 9.sp)
                Text(score.toString(), fontWeight = FontWeight.Black, fontSize = 21.sp)
                Text("$rounds round", color = SonHarfMuted, fontSize = 7.sp)
            }
        }
    }
}

@Composable
private fun SketchChatDialogV7(messages: List<ChatMessageDto>, me: String?, input: String, onInput: (String) -> Unit, onClose: () -> Unit, onSend: () -> Unit) {
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
