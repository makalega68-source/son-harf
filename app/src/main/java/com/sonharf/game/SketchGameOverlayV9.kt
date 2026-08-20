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
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SketchGameOverlayV9() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var isVip by remember { mutableStateOf(false) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var triviaFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var dismissedRoomId by remember { mutableStateOf<String?>(null) }

    suspend fun discover(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        isVip = runCatching { backend.getProfile(uid).isVip }.getOrDefault(false)
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { it.id != dismissedRoomId && (it.hostId == uid || it.guestId == uid) && it.status in listOf("playing", "quiz", "final", "sudden_death", "paused") }
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
                val found = runCatching { discover() }.getOrNull()
                if (found != null) {
                    room = found
                    words = runCatching { backend.getWords(found.id) }.getOrDefault(emptyList())
                    refreshTrivia(found)
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
            delay(500)
        }
    }

    LaunchedEffect(notice) {
        if (notice.isNotBlank()) {
            delay(1800)
            notice = ""
        }
    }
    LaunchedEffect(triviaFeedback) {
        if (triviaFeedback != null) {
            delay(1500)
            triviaFeedback = null
        }
    }

    val active = room ?: return
    val me = backend.currentUserId()
    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { input = "" }

    fun eventNotice(r: GameRoomDto, submitted: String) = when {
        r.lastEventPlayerId == me && r.lastEvent == "word_already_used" -> sh("Bu kelime daha önce kullanıldı. −1", "This word was already used. −1")
        r.lastEventPlayerId == me && r.lastEvent == "wrong_start_letter" -> sh("Kelime son harfle başlamalı. −1", "Word must start with the last letter. −1")
        r.lastEventPlayerId == me && r.lastEvent == "invalid_word" -> sh("Geçersiz kelime. −1", "Invalid word. −1")
        r.lastEventPlayerId == me && r.lastEvent == "turn_expired" -> sh("Süre doldu. −1", "Time expired. −1")
        r.lastEvent == "provisional_word" -> sh("${submitted.uppercase()} kabul edildi", "${submitted.uppercase()} accepted")
        else -> sh("${submitted.uppercase()} kabul edildi", "${submitted.uppercase()} accepted")
    }

    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        ArenaV9(
            room = active, me = me, words = words, input = input, onInput = { input = it.take(40) }, busy = busy,
            notice = notice, isVip = isVip, triviaRound = triviaRound, triviaQuestion = triviaQuestion, triviaFeedback = triviaFeedback,
            onSubmit = {
                val submitted = input.trim()
                if (submitted.isBlank() || busy) return@ArenaV9
                scope.launch {
                    busy = true
                    val latest = runCatching { backend.getRoom(active.id) }.getOrNull()
                    if (latest == null) {
                        notice = sh("Bağlantı yenileniyor…", "Refreshing connection…")
                        busy = false
                        return@launch
                    }
                    room = latest
                    if (latest.currentPlayerId != me || latest.status !in listOf("playing", "final", "sudden_death")) {
                        input = ""
                        notice = ""
                        busy = false
                        return@launch
                    }
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { result ->
                            room = result
                            input = ""
                            notice = eventNotice(result, submitted)
                            if (result.lastEventPlayerId == me && result.lastEvent in setOf("word_already_used", "wrong_start_letter", "invalid_word", "turn_expired")) SonHarfSoundFx.warning() else SonHarfSoundFx.wordAccepted()
                        }
                        .onFailure {
                            val refreshed = runCatching { backend.getRoom(active.id) }.getOrNull()
                            if (refreshed != null) {
                                room = refreshed
                                notice = if (refreshed.currentPlayerId == me && refreshed.status in listOf("playing", "final", "sudden_death")) sh("Bağlantı yenilendi. Tekrar gönder.", "Connection refreshed. Send again.") else ""
                            } else notice = sh("Bağlantı zayıf. Tekrar dene.", "Weak connection. Try again.")
                            if (notice.isNotBlank()) SonHarfSoundFx.warning()
                        }
                    busy = false
                }
            },
            onTrivia = { index ->
                scope.launch {
                    val round = triviaRound ?: return@launch
                    val before = if (me == active.hostId) active.hostScore else active.guestScore
                    runCatching { backend.answerTrivia(round.id, index) }
                        .onSuccess { updated ->
                            room = updated
                            val after = if (me == updated.hostId) updated.hostScore else updated.guestScore
                            val correct = after > before
                            triviaFeedback = if (correct) true to sh("DOĞRU! +${round.bonusPoints} PUAN", "CORRECT! +${round.bonusPoints} POINTS") else false to sh("YANLIŞ CEVAP", "WRONG ANSWER")
                            if (correct) SonHarfSoundFx.bonus() else SonHarfSoundFx.warning()
                            refreshTrivia(updated)
                        }
                        .onFailure {
                            triviaFeedback = false to sh("Cevap gönderilemedi", "Answer could not be sent")
                            SonHarfSoundFx.warning()
                        }
                }
            },
            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onChat = {
                if (active.isBot) notice = sh("Bot maçında sohbet kapalı.", "Chat is disabled in bot matches.")
                else scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()); showChat = true }
            },
            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); input = "" } } },
            onExit = { dismissedRoomId = active.id; room = null; words = emptyList(); input = "" },
        )
    }

    if (showChat && !active.isBot) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text(sh("SOHBET", "CHAT"), fontWeight = FontWeight.Black) },
            text = {
                Column {
                    chat.takeLast(8).forEach { m -> Text((if (m.senderId == me) sh("Sen: ", "You: ") else sh("Rakip: ", "Opponent: ")) + m.body, fontSize = 11.sp, modifier = Modifier.padding(vertical = 3.dp)) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(chatInput, { chatInput = it.take(300) }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) })
                }
            },
            confirmButton = { TextButton(onClick = { if (chatInput.isNotBlank()) scope.launch { runCatching { backend.sendChat(active.id, chatInput) }.onSuccess { chatInput = ""; chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat) } } }) { Text(sh("GÖNDER", "SEND")) } },
            dismissButton = { TextButton(onClick = { showChat = false }) { Text(sh("KAPAT", "CLOSE")) } },
        )
    }
}

@Composable
private fun ArenaV9(
    room: GameRoomDto, me: String?, words: List<GameWordDto>, input: String, onInput: (String) -> Unit, busy: Boolean,
    notice: String, isVip: Boolean, triviaRound: TriviaRoundDto?, triviaQuestion: TriviaQuestionDto?, triviaFeedback: Pair<Boolean, String>?,
    onSubmit: () -> Unit, onTrivia: (Int) -> Unit, onForfeit: () -> Unit, onChat: () -> Unit, onRematch: () -> Unit, onExit: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val lastWord = words.lastOrNull()?.normalizedWord?.uppercase()
    val required = lastWord?.lastOrNull()?.toString().orEmpty()
    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        var lastTick = -1
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (java.time.Instant.parse(room.turnDeadline).epochSecond - java.time.Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            if (seconds in 1..10 && seconds != lastTick) { lastTick = seconds; SonHarfSoundFx.countdown() }
            if (seconds <= 0) break
            delay(250)
        }
    }

    if (room.status == "finished") {
        val won = room.winnerId == me || (room.isBot && room.winnerId == null && myScore > oppScore)
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(22.dp), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (won) sh("KAZANDIN", "YOU WON") else sh("MAÇ BİTTİ", "MATCH OVER"), fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("$myRounds - $oppRounds", fontSize = 40.sp, fontWeight = FontWeight.Black)
                    Button(onClick = onRematch, modifier = Modifier.fillMaxWidth()) { Text(sh("TEKRAR OYNA", "PLAY AGAIN")) }
                    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text(sh("LOBİYE DÖN", "BACK TO LOBBY")) }
                }
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SonHarfSurface2, SonHarfBg, SonHarfSurface))).statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerV9(sh("SEN", "YOU"), myScore, myRounds, myTurn, SonHarfCyan, Modifier.weight(1f))
            Box(Modifier.size(60.dp).clip(CircleShape).background(if (seconds <= 10) SonHarfPink else SonHarfGold).padding(3.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$seconds", fontSize = 22.sp, fontWeight = FontWeight.Black); Text(sh("sn", "sec"), fontSize = 8.sp, color = SonHarfMuted) } }
            }
            PlayerV9(if (room.isBot) "${room.botName ?: "KelimeBot"} BOT" else sh("RAKİP", "OPPONENT"), oppScore, oppRounds, !myTurn, SonHarfPink, Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
            shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .16f)),
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("${room.roundWordCount}/10", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(sh("GENEL KÜLTÜR • +${triviaRound.bonusPoints}", "TRIVIA • +${triviaRound.bonusPoints}"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Text(triviaQuestion.question, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                            listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD).forEachIndexed { i, option ->
                                OutlinedButton(onClick = { onTrivia(i) }, modifier = Modifier.fillMaxWidth().heightIn(min = 33.dp), contentPadding = PaddingValues(3.dp)) { Text(option, fontSize = 10.sp, textAlign = TextAlign.Center) }
                            }
                        }
                    } else {
                        Text(if (myTurn) sh("SIRA SENDE", "YOUR TURN") else if (room.isBot && room.botTurn) sh("BOT DÜŞÜNÜYOR", "BOT THINKING") else sh("RAKİBİN SIRASI", "OPPONENT'S TURN"), color = if (myTurn) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        if (lastWord.isNullOrBlank()) Text(sh("İLK KELİME", "FIRST WORD"), fontSize = 32.sp, fontWeight = FontWeight.Black)
                        else Text(buildAnnotatedString { append(lastWord.dropLast(1)); withStyle(SpanStyle(color = SonHarfPink)) { append(lastWord.takeLast(1)) } }, fontSize = 38.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text(if (required.isBlank()) sh("İlk kelimeyi yaz", "Enter the first word") else sh("$required ile başlayan bir kelime yaz", "Enter a word starting with $required"), color = SonHarfMuted, fontSize = 11.sp)
                    }
                }
                triviaFeedback?.let { (ok, msg) ->
                    Surface(Modifier.align(Alignment.Center).padding(14.dp), color = (if (ok) SonHarfGreen else SonHarfPink).copy(alpha = .92f), shape = RoundedCornerShape(16.dp)) {
                        Text(msg, Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (words.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (isVip) Text("♛ ${sh("VIP KELİME ZİNCİRİ", "VIP WORD CHAIN")}", color = SonHarfGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.height(43.dp)) {
                    items(words.takeLast(7)) { w ->
                        Surface(shape = RoundedCornerShape(11.dp), color = if (isVip) SonHarfGold.copy(alpha = .10f) else SonHarfSurface2, border = BorderStroke(1.dp, if (isVip) SonHarfGold.copy(alpha = .45f) else SonHarfMuted.copy(alpha = .14f))) {
                            Text(w.word.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (notice.isNotBlank()) {
            val bad = notice.contains("−1") || notice.contains("zayıf") || notice.contains("Weak")
            Text(notice, modifier = Modifier.fillMaxWidth(), color = if (bad) SonHarfPink else SonHarfMuted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
        }

        Surface(Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), color = SonHarfSurface, border = BorderStroke(1.4.dp, if (myTurn) SonHarfCyan.copy(alpha = .7f) else SonHarfMuted.copy(alpha = .25f))) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (input.isBlank()) if (myTurn) sh("Kelimenizi yazın…", "Type your word…") else sh("Rakibin sırası…", "Opponent's turn…") else input.uppercase(), Modifier.weight(1f), color = if (input.isBlank()) SonHarfMuted else SonHarfText, fontSize = 17.sp)
                TextButton(onClick = onSubmit, enabled = myTurn && input.length >= 2 && !busy) { Text("➤", fontSize = 23.sp) }
            }
        }

        KeyboardV9(room.language, myTurn && !busy && room.status != "quiz", input, onInput, onSubmit)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f).height(40.dp), border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .55f))) { Text(sh("⚑ PES ET", "⚑ FORFEIT"), color = SonHarfPink, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(40.dp), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .55f))) { Text(sh("● SOHBET", "● CHAT"), color = SonHarfCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun KeyboardV9(language: String, enabled: Boolean, input: String, onInput: (String) -> Unit, onSubmit: () -> Unit) {
    val rows = if (language == "en") listOf(listOf("q","w","e","r","t","y","u","i","o","p"), listOf("a","s","d","f","g","h","j","k","l"), listOf("z","x","c","v","b","n","m"))
    else listOf(listOf("q","w","e","r","t","y","u","ı","o","p","ğ","ü"), listOf("a","s","d","f","g","h","j","k","l","ş","i"), listOf("z","x","c","v","b","n","m","ö","ç"))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { keys -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { keys.forEach { k -> Button(onClick = { onInput(input + k) }, enabled = enabled && input.length < 40, modifier = Modifier.weight(1f).height(41.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfSurface2, contentColor = SonHarfText, disabledContainerColor = SonHarfSurface2.copy(alpha = .45f)), contentPadding = PaddingValues(0.dp)) { Text(k.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick = { if (input.isNotEmpty()) onInput(input.dropLast(1)) }, enabled = enabled && input.isNotEmpty(), modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(9.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3E5EA), contentColor = Color(0xFF29303D))) { Text("⌫", fontSize = 20.sp, fontWeight = FontWeight.Black) }
            Button(onClick = onSubmit, enabled = enabled && input.length >= 2, modifier = Modifier.weight(2.1f).height(42.dp), shape = RoundedCornerShape(9.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)) { Text(sh("GÖNDER", "SEND"), fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun PlayerV9(name: String, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {
    Card(modifier = modifier.height(74.dp), colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = .10f) else SonHarfSurface), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, if (active) accent.copy(alpha = .55f) else SonHarfMuted.copy(alpha = .13f))) {
        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(31.dp).clip(CircleShape).background(accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(6.dp))
            Column { Text(name, maxLines = 1, color = if (active) accent else SonHarfMuted, fontSize = 8.sp); Text(score.toString(), fontWeight = FontWeight.Black, fontSize = 20.sp); Text("$rounds round", color = SonHarfMuted, fontSize = 7.sp) }
        }
    }
}
