package com.sonharf.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
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

private data class WordFeedbackV10(
    val correct: Boolean,
    val title: String,
    val detail: String,
    val duplicateWord: String? = null,
)

@Composable
fun SketchGameOverlayV10() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<WordFeedbackV10?>(null) }
    var busy by remember { mutableStateOf(false) }
    var isVip by remember { mutableStateOf(false) }
    var myProfile by remember { mutableStateOf<SocialProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<SocialProfileDto?>(null) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var triviaFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var dismissedRoomId by remember { mutableStateOf<String?>(null) }

    suspend fun loadProfiles(r: GameRoomDto) {
        val me = backend.currentUserId() ?: return
        myProfile = runCatching { backend.getSocialProfile(me) }.getOrNull()
        isVip = myProfile?.isVip == true
        opponentProfile = if (r.isBot) null else {
            val opponentId = if (r.hostId == me) r.guestId else r.hostId
            opponentId?.let { runCatching { backend.getSocialProfile(it) }.getOrNull() }
        }
    }

    suspend fun discover(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        val found = SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { it.id != dismissedRoomId && (it.hostId == uid || it.guestId == uid) && it.status in listOf("playing", "quiz", "final", "sudden_death", "paused") }
            .maxByOrNull { it.validWordCount }
        if (found != null) loadProfiles(found)
        return found
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

    fun messageForEvent(event: String?, submitted: String): WordFeedbackV10 = when (event) {
        "word_already_used" -> WordFeedbackV10(false, sh("YANLIŞ ✕", "WRONG ✕"), sh("Bu kelime daha önce kullanıldı", "This word was already used"), submitted.uppercase())
        "wrong_start_letter" -> WordFeedbackV10(false, sh("YANLIŞ ✕", "WRONG ✕"), sh("Kelime son harfle başlamalı", "Word must start with the last letter"))
        "not_in_dictionary" -> WordFeedbackV10(false, sh("YANLIŞ ✕", "WRONG ✕"), sh("Kelime sözlükte bulunamadı", "Word was not found in the dictionary"))
        "invalid_word" -> WordFeedbackV10(false, sh("YANLIŞ ✕", "WRONG ✕"), sh("Geçersiz kelime", "Invalid word"))
        "turn_expired" -> WordFeedbackV10(false, sh("SÜRE DOLDU ✕", "TIME UP ✕"), sh("Hamle süresi doldu", "Turn time expired"))
        else -> WordFeedbackV10(true, sh("DOĞRU ✓", "CORRECT ✓"), submitted.uppercase())
    }

    fun failureFeedback(raw: String, submitted: String): WordFeedbackV10 = when {
        "word_already_used" in raw -> messageForEvent("word_already_used", submitted)
        "wrong_start_letter" in raw -> messageForEvent("wrong_start_letter", submitted)
        "not_in_dictionary" in raw -> messageForEvent("not_in_dictionary", submitted)
        "invalid_word" in raw -> messageForEvent("invalid_word", submitted)
        "not_your_turn" in raw -> WordFeedbackV10(false, sh("BEKLE ✕", "WAIT ✕"), sh("Sıra rakibinde", "It is the opponent's turn"))
        else -> WordFeedbackV10(false, sh("HATA ✕", "ERROR ✕"), sh("Hamle gönderilemedi, tekrar dene", "Move could not be sent, try again"))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val current = room
            val updated = if (current == null) runCatching { discover() }.getOrNull() else runCatching { backend.getRoom(current.id) }.getOrNull()
            if (updated != null) {
                room = updated
                words = runCatching { backend.getWords(updated.id) }.getOrDefault(words)
                refreshTrivia(updated)
                if (myProfile == null || (!updated.isBot && opponentProfile == null)) loadProfiles(updated)
                if (showChat && !updated.isBot) chat = runCatching { backend.getChat(updated.id) }.getOrDefault(chat)
            }
            delay(500)
        }
    }
    LaunchedEffect(notice) { if (notice.isNotBlank()) { delay(1800); notice = "" } }
    LaunchedEffect(feedback) { if (feedback != null) { delay(if (feedback?.duplicateWord != null) 2100 else 1500); feedback = null } }
    LaunchedEffect(triviaFeedback) { if (triviaFeedback != null) { delay(1500); triviaFeedback = null } }

    val active = room ?: return
    val me = backend.currentUserId()
    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { input = "" }
    LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount, active.roundNo, active.turnDeadline) {
        if (!active.isBot || !active.botTurn || active.status !in listOf("playing", "final", "sudden_death")) return@LaunchedEffect
        delay(900L)
        repeat(3) { attempt ->
            val latest = runCatching { backend.getRoom(active.id) }.getOrNull() ?: return@repeat
            if (!latest.botTurn || latest.status !in listOf("playing", "final", "sudden_death")) { room = latest; return@LaunchedEffect }
            val moved = runCatching { backend.botTakeTurn(active.id) }.getOrNull()
            if (moved != null) { room = moved; words = runCatching { backend.getWords(active.id) }.getOrDefault(words); return@LaunchedEffect }
            if (attempt < 2) delay(650L)
        }
        val latest = runCatching { backend.getRoom(active.id) }.getOrNull()
        val expired = latest?.turnDeadline?.let { deadline -> runCatching { java.time.Instant.parse(deadline).isBefore(java.time.Instant.now()) }.getOrDefault(false) } == true
        if (latest != null && latest.botTurn && expired) {
            runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it }
        }
    }

    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        ArenaV10(
            room = active,
            me = me,
            words = words,
            input = input,
            onInput = { input = it.take(40) },
            busy = busy,
            notice = notice,
            feedback = feedback,
            isVip = isVip,
            myProfile = myProfile,
            opponentProfile = opponentProfile,
            triviaRound = triviaRound,
            triviaQuestion = triviaQuestion,
            triviaFeedback = triviaFeedback,
            onSubmit = {
                val submitted = input.trim()
                if (submitted.isBlank() || busy) return@ArenaV10
                scope.launch {
                    busy = true
                    val latest = runCatching { backend.getRoom(active.id) }.getOrNull()
                    if (latest == null || latest.currentPlayerId != me || latest.status !in listOf("playing", "final", "sudden_death")) {
                        feedback = WordFeedbackV10(false, sh("BEKLE ✕", "WAIT ✕"), sh("Sıra sende değil", "It is not your turn"))
                        input = ""
                        busy = false
                        return@launch
                    }
                    val duplicate = if (isVip) words.lastOrNull { it.normalizedWord.equals(submitted, ignoreCase = true) || it.word.equals(submitted, ignoreCase = true) } else null
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { result ->
                            room = result
                            input = ""
                            val rejected = result.lastEventPlayerId == me && result.lastEvent in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired")
                            feedback = if (rejected) {
                                val f = messageForEvent(result.lastEvent, submitted)
                                if (result.lastEvent == "word_already_used" && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f
                            } else messageForEvent(null, submitted)
                        }
                        .onFailure { e ->
                            input = ""
                            val reconciledRoom = runCatching { backend.getRoom(active.id) }.getOrNull()
                            val reconciledWords = runCatching { backend.getWords(active.id) }.getOrDefault(words)
                            val acceptedOnServer = reconciledWords.any {
                                it.playerId == me && (it.word.equals(submitted, ignoreCase = true) || it.normalizedWord.equals(submitted, ignoreCase = true))
                            } && ((reconciledRoom?.validWordCount ?: active.validWordCount) >= active.validWordCount)
                            val stateAdvanced = reconciledRoom != null && (
                                reconciledRoom.validWordCount > active.validWordCount || reconciledRoom.currentPlayerId != active.currentPlayerId
                            )
                            if (acceptedOnServer || stateAdvanced) {
                                if (reconciledRoom != null) room = reconciledRoom
                                words = reconciledWords
                                feedback = if (acceptedOnServer) messageForEvent(null, submitted) else null
                            } else {
                                val f = failureFeedback(e.message.orEmpty(), submitted)
                                feedback = if (f.duplicateWord != null && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f
                            }
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
                            refreshTrivia(updated)
                        }
                        .onFailure { triviaFeedback = false to sh("Cevap gönderilemedi", "Answer could not be sent") }
                }
            },
            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onGoodWord = {
                SonHarfSoundFx.softNotify()
                notice = sh("👏 İyi kelime!", "👏 Nice word!")
                if (!active.isBot) scope.launch { runCatching { backend.sendChat(active.id, "👏 İyi kelime!") } }
            },
            onChat = {
                if (!isVip) notice = sh("Sohbet VIP üyelerine özeldir.", "Chat is for VIP members.")
                else if (active.isBot) notice = sh("Bot maçında sohbet kapalı.", "Chat is disabled in bot matches.")
                else scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()); showChat = true }
            },
            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); input = ""; feedback = null; loadProfiles(it) } } },
            onExit = {
                dismissedRoomId = active.id
                room = null
                words = emptyList()
                input = ""
                feedback = null
                SonHarfGameNavigation.requestLobby()
            },
        )
    }

    if (showChat && !active.isBot) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text(sh("MAÇ SOHBETİ", "MATCH CHAT"), fontWeight = FontWeight.Black, fontSize = 21.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            confirmButton = { TextButton(onClick = { if (chatInput.isNotBlank()) scope.launch { runCatching { backend.sendChat(active.id, chatInput) }.onSuccess { chatInput = ""; chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat) } } }) { Text(sh("GÖNDER", "SEND"), fontSize = 16.sp) } },
            dismissButton = { TextButton(onClick = { showChat = false }) { Text(sh("KAPAT", "CLOSE"), fontSize = 16.sp) } },
        )
    }
}

@Composable
private fun ArenaV10(
    room: GameRoomDto,
    me: String?,
    words: List<GameWordDto>,
    input: String,
    onInput: (String) -> Unit,
    busy: Boolean,
    notice: String,
    feedback: WordFeedbackV10?,
    isVip: Boolean,
    myProfile: SocialProfileDto?,
    opponentProfile: SocialProfileDto?,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    triviaFeedback: Pair<Boolean, String>?,
    onSubmit: () -> Unit,
    onTrivia: (Int) -> Unit,
    onForfeit: () -> Unit,
    onGoodWord: () -> Unit,
    onChat: () -> Unit,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val currentRoundWords = words.takeLast(room.roundWordCount.coerceAtLeast(0))
    val myStreak = currentRoundWords.count { it.playerId == me }.coerceAtMost(9)
    val opponentId = if (me == room.hostId) room.guestId else room.hostId
    val oppStreak = currentRoundWords.count { it.playerId == opponentId }.coerceAtMost(9)
    val scoreDiff = myScore - oppScore
    val pressureLabel = when { scoreDiff <= -6 -> sh("Yüksek", "High"); scoreDiff < 3 -> sh("Orta", "Medium"); else -> sh("Düşük", "Low") }
    val ratingGain = (10 + (oppScore - myScore).coerceIn(-2, 6)).coerceIn(8, 16)
    val streakMultiplier = if (myStreak >= 5) "x1.5" else if (myStreak >= 3) "x1.2" else "x1.0"
    val displayLocale = if (room.language == "tr") java.util.Locale("tr", "TR") else java.util.Locale.ENGLISH
    val lastWord = words.lastOrNull()?.normalizedWord?.uppercase(displayLocale)
    val required = lastWord?.lastOrNull()?.toString().orEmpty()
    val lastWordFont = when {
        lastWord == null -> 36.sp
        lastWord.length >= 22 -> 21.sp
        lastWord.length >= 18 -> 24.sp
        lastWord.length >= 14 -> 29.sp
        lastWord.length >= 10 -> 35.sp
        else -> 44.sp
    }
    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }
    val timerScale by animateFloatAsState(
        targetValue = if (seconds in 1..10 && seconds % 2 == 0) 1.10f else 1f,
        animationSpec = tween(170),
        label = "finalTenHeartbeat",
    )

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        var lastTick = -1
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (java.time.Instant.parse(room.turnDeadline).epochSecond - java.time.Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            if (seconds in 1..10 && seconds != lastTick) { lastTick = seconds; SonHarfSoundFx.heartbeat() }
            if (seconds <= 0) break
            delay(250)
        }
    }

    if (room.status == "finished") {
        val won = room.winnerId == me || (room.isBot && room.winnerId == null && myScore > oppScore)
        val winnerName = if (won) myProfile?.displayName ?: sh("Sen", "You") else if (room.isBot) room.botName ?: "KelimeBot" else opponentProfile?.displayName ?: sh("Rakip", "Opponent")
        val winnerGender = if (won) myProfile?.gender else opponentProfile?.gender
        Box(Modifier.fillMaxSize().background(SonHarfBg).statusBarsPadding().navigationBarsPadding().padding(22.dp), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha=.48f))) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(sh("MAÇ SONUCU", "MATCH RESULT"), fontSize = 23.sp, fontWeight = FontWeight.Black)
                    if (room.isBot && !won) {
                        Box(Modifier.size(82.dp).clip(CircleShape).background(SonHarfSurface2), contentAlignment = Alignment.Center) { Text("🤖", fontSize = 48.sp) }
                    } else {
                        SocialAvatar(winnerGender, winnerName, 88.dp, accent = SonHarfGold)
                    }
                    Text(sh("KAZANAN", "WINNER"), color = SonHarfCyan, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(winnerName, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("$myRounds - $oppRounds", fontSize = 46.sp, fontWeight = FontWeight.Black)
                    Button(onClick = onRematch, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(sh("TEKRAR OYNA", "PLAY AGAIN"), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(sh("LOBİYE DÖN", "BACK TO LOBBY"), fontSize = 17.sp) }
                }
            }
        }
        return
    }

    val arenaBrush = if (SonHarfCosmetics.auroraTheme) Brush.verticalGradient(listOf(Color(0xFFD9F1FF), Color(0xFFE8E2FF), Color(0xFFDDF9FF))) else Brush.verticalGradient(listOf(SonHarfSurface2, SonHarfBg, Color(0xFFE7F6FF)))
    Column(
        Modifier.fillMaxSize().background(arenaBrush).statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerV10(myProfile?.displayName ?: sh("SEN", "YOU"), myProfile?.gender, myProfile?.avatarPath, myScore, myRounds, myTurn, SonHarfCyan, Modifier.weight(1f))
            Box(Modifier.size(70.dp).scale(timerScale).clip(CircleShape).background(if (seconds <= 10) SonHarfPink else SonHarfGold).padding(3.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$seconds", color = if (seconds <= 10) SonHarfPink else SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black); Text(sh("sn", "sec"), fontSize = 12.sp, color = if (seconds <= 10) SonHarfPink else SonHarfMuted) }
                }
            }
            PlayerV10(if (room.isBot) "${room.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: sh("RAKİP", "OPPONENT"), if (room.isBot) "other" else opponentProfile?.gender, if (room.isBot) null else opponentProfile?.avatarPath, oppScore, oppRounds, !myTurn, SonHarfPink, Modifier.weight(1f), room.isBot)
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha=.96f)),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, if (SonHarfCosmetics.auroraTheme) SonHarfPurple.copy(alpha=.3f) else SonHarfMuted.copy(alpha=.16f)),
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            when { scoreDiff > 0 -> sh("ÖNDESİN +$scoreDiff", "YOU LEAD +$scoreDiff"); scoreDiff < 0 -> sh("RAKİP ÖNDE ${-scoreDiff}", "OPPONENT +${-scoreDiff}"); else -> sh("BAŞA BAŞ", "TIED") },
                            modifier = Modifier.weight(1f), color = if (scoreDiff >= 0) SonHarfCyan else SonHarfPink, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1,
                        )
                        Text("${room.roundWordCount}/10", modifier = Modifier.weight(1f), color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.End)
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥 ${sh("SERİ", "STREAK")}: $myStreak", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${sh("RAKİP SERİSİ", "OPP STREAK")}: $oppStreak", color = SonHarfPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(progress = { (myStreak / 5f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = SonHarfGold, trackColor = SonHarfSurface2)
                        Text("📊 Rating +$ratingGain ${sh("kazanırsan", "if you win")}", color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(sh("GENEL KÜLTÜR • +${triviaRound.bonusPoints}", "TRIVIA • +${triviaRound.bonusPoints}"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(triviaQuestion.question, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
                            listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD).forEachIndexed { i, option ->
                                OutlinedButton(onClick = { onTrivia(i) }, modifier = Modifier.fillMaxWidth().heightIn(min = 43.dp), contentPadding = PaddingValues(5.dp)) { Text(option, fontSize = 14.sp, textAlign = TextAlign.Center) }
                            }
                        }
                    } else {
                        Text(if (myTurn) sh("SIRA SENDE", "YOUR TURN") else if (room.isBot && room.botTurn) sh("BOT DÜŞÜNÜYOR", "BOT THINKING") else sh("RAKİBİN SIRASI", "OPPONENT'S TURN"), color = if (myTurn) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        if (lastWord.isNullOrBlank()) Text(sh("İLK KELİME", "FIRST WORD"), fontSize = 36.sp, fontWeight = FontWeight.Black)
                        else Text(buildAnnotatedString { append(lastWord.dropLast(1)); withStyle(SpanStyle(color = SonHarfPink)) { append(lastWord.takeLast(1)) } }, modifier = Modifier.fillMaxWidth(), fontSize = lastWordFont, lineHeight = (lastWordFont.value * 1.08f).sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
                        Text(if (required.isBlank()) sh("İlk kelimeyi yaz", "Enter the first word") else sh("$required ile başlayan bir kelime yaz", "Enter a word starting with $required"), color = SonHarfMuted, fontSize = 15.sp)
                    }
                }

                feedback?.let { f ->
                    val tone = if (f.correct) SonHarfGreen else SonHarfPink
                    Surface(
                        Modifier.align(Alignment.TopCenter).padding(top = 42.dp, start = 10.dp, end = 10.dp).fillMaxWidth(.94f),
                        color = tone.copy(alpha = .96f),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 4.dp,
                    ) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(f.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (f.duplicateWord != null) "${f.duplicateWord} • ${sh("daha önce çıktı", "already used")}" else f.detail,
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                            )
                        }
                    }
                }
                triviaFeedback?.let { (ok, msg) ->
                    Surface(Modifier.align(Alignment.Center).padding(14.dp), color = (if (ok) SonHarfGreen else SonHarfPink).copy(alpha=.94f), shape = RoundedCornerShape(18.dp)) {
                        Text(msg, Modifier.padding(horizontal=20.dp, vertical=14.dp), color=Color.White, fontWeight=FontWeight.Black, fontSize=20.sp, textAlign=TextAlign.Center)
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.CenterStart) {
            if (!isVip) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = SonHarfSurface2, border = BorderStroke(1.dp, SonHarfGold.copy(alpha=.35f))) {
                    Text(sh("🔒 KELİME ZİNCİRİ • VIP", "🔒 WORD CHAIN • VIP"), Modifier.fillMaxWidth().padding(vertical=10.dp), color=SonHarfGold, fontWeight=FontWeight.Black, fontSize=13.sp, textAlign=TextAlign.Center)
                }
            } else if (words.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    items(words.takeLast(30)) { w ->
                        val duplicate = feedback?.duplicateWord?.equals(w.word, ignoreCase = true) == true
                        Surface(shape = RoundedCornerShape(12.dp), color = if (duplicate) SonHarfPink.copy(alpha=.22f) else SonHarfGold.copy(alpha=.12f), border = BorderStroke(if (duplicate) 2.dp else 1.dp, if (duplicate) SonHarfPink else SonHarfGold.copy(alpha=.5f))) {
                            Text(w.word.uppercase(displayLocale), Modifier.padding(horizontal=12.dp, vertical=8.dp), color = if (duplicate) SonHarfPink else SonHarfText, fontSize = if (duplicate) 17.sp else 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else Text(sh("Kelime zinciri burada görünecek", "Word chain will appear here"), color = SonHarfMuted, fontSize = 13.sp)
        }

        Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
            if (notice.isNotBlank()) Text(notice, color = if ("−1" in notice) SonHarfPink else SonHarfMuted, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
            else if (isVip) Text(sh("VIP: maçtaki çıkan kelimelerin tamamı takip edilir", "VIP: all used words in the match are tracked"), color = SonHarfGold, fontSize = 12.sp, maxLines = 1)
        }

        Surface(Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), color = SonHarfSurface, border = BorderStroke(1.6.dp, if (myTurn) SonHarfCyan.copy(alpha=.75f) else SonHarfMuted.copy(alpha=.25f))) {
            Row(Modifier.fillMaxSize().padding(horizontal=15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (input.isBlank()) if (myTurn) sh("Kelimenizi yazın…", "Type your word…") else sh("Rakibin sırası…", "Opponent's turn…") else input.uppercase(displayLocale), Modifier.weight(1f), color = if (input.isBlank()) SonHarfMuted else SonHarfText, fontSize=19.sp)
                TextButton(onClick=onSubmit, enabled=myTurn && input.length>=2 && !busy) { Text("➤", fontSize=26.sp) }
            }
        }
        KeyboardV10(room.language, myTurn && !busy && room.status != "quiz", input, onInput, onSubmit)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick=onForfeit, modifier=Modifier.weight(1f).height(42.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfPink.copy(alpha=.55f))) { Text(sh("⚑ PES", "⚑ FORFEIT"), color=SonHarfPink, fontWeight=FontWeight.Bold, fontSize=12.sp, maxLines=1) }
            OutlinedButton(onClick=onGoodWord, modifier=Modifier.weight(1.18f).height(42.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfCyan.copy(alpha=.55f))) { Text(sh("👏 İYİ KELİME", "👏 NICE WORD"), color=SonHarfText, fontWeight=FontWeight.Bold, fontSize=11.sp, maxLines=1) }
            OutlinedButton(onClick=onChat, modifier=Modifier.weight(1.18f).height(42.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfCyan.copy(alpha=.55f))) { Text(if (isVip) sh("● SOHBET", "● CHAT") else sh("🔒 CHAT • VIP", "🔒 CHAT • VIP"), color=if (isVip) SonHarfCyan else SonHarfGold, fontWeight=FontWeight.Bold, fontSize=11.sp, maxLines=1) }
        }
        Surface(Modifier.fillMaxWidth().height(48.dp), shape=RoundedCornerShape(14.dp), color=SonHarfSurface.copy(alpha=.9f), border=BorderStroke(1.dp, SonHarfMuted.copy(alpha=.14f))) {
            Row(Modifier.fillMaxSize().padding(horizontal=8.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                StatusChipV10("❤", sh("KRİTİK MOD", "CRITICAL"), if (seconds <= 10) sh("AKTİF", "ACTIVE") else "${seconds} sn", if (seconds <= 10) SonHarfPink else SonHarfMuted)
                StatusChipV10("⚡", sh("SERİ BONUSU", "STREAK BONUS"), streakMultiplier, SonHarfGold)
                StatusChipV10("🎯", sh("RAKİP BASKISI", "PRESSURE"), pressureLabel, if (scoreDiff < 0) SonHarfPink else SonHarfCyan)
                StatusChipV10("🏆", sh("HEDEF", "TARGET"), sh("10 puan", "10 points"), SonHarfGreen)
            }
        }
    }
}

@Composable
private fun StatusChipV10(icon: String, title: String, value: String, tone: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 64.dp, max = 92.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 13.sp)
            Spacer(Modifier.width(2.dp))
            Text(title, color = SonHarfText, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
        Text(value, color = tone, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun KeyboardV10(language: String, enabled: Boolean, input: String, onInput: (String) -> Unit, onSubmit: () -> Unit) {
    val rows = if (language == "en") listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("z","x","c","v","b","n","m"),
    ) else listOf(
        listOf("q","w","e","r","t","y","u","ı","o","p","ğ","ü"),
        listOf("a","s","d","f","g","h","j","k","l","ş","i"),
        listOf("z","x","c","v","b","n","m","ö","ç"),
    )
    val neon = SonHarfCosmetics.keyboardIsNeon
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { keys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                keys.forEach { k ->
                    val label = if (language == "tr") when (k) { "i" -> "İ"; "ı" -> "I"; else -> k.uppercase() } else k.uppercase()
                    Button(onClick = { onInput(input + k) }, enabled = enabled && input.length < 40, modifier = Modifier.weight(1f).height(43.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = if (neon) Color(0xFF10283D) else SonHarfSurface2, contentColor = if (neon) SonHarfCyan else SonHarfText), border = if (neon) BorderStroke(1.dp, SonHarfCyan.copy(alpha=.65f)) else null, contentPadding = PaddingValues(0.dp)) {
                        Text(label, fontSize=13.sp, fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { if (input.isNotEmpty()) onInput(input.dropLast(1)) },
            enabled = enabled && input.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text(sh("SİL", "DELETE"), fontSize = 14.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun PlayerV10(name: String, gender: String?, avatarPath: String?, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier, isBot: Boolean = false) {
    Card(modifier=modifier.fillMaxHeight(), colors=CardDefaults.cardColors(containerColor=if(active) accent.copy(alpha=.10f) else SonHarfSurface), shape=RoundedCornerShape(18.dp), border=BorderStroke(1.dp, if(active) accent.copy(alpha=.55f) else SonHarfMuted.copy(alpha=.13f))) {
        Row(Modifier.fillMaxSize().padding(horizontal=7.dp, vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
            if (isBot) Box(Modifier.size(width=60.dp, height=66.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha=.15f)), contentAlignment=Alignment.Center) { Text("🤖", fontSize=31.sp) }
            else Box(Modifier.size(width=62.dp, height=68.dp), contentAlignment=Alignment.Center) {
                ProfilePhotoAvatar(avatarPath, name, 60.dp, visible = true, accent = accent, shape = RoundedCornerShape(14.dp))
                val female = gender?.lowercase() in setOf("kadın", "kadin", "female", "woman")
                val male = gender?.lowercase() in setOf("erkek", "male", "man")
                if (female || male) {
                    Surface(modifier = Modifier.align(Alignment.BottomEnd).size(17.dp), shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, if (female) Color(0xFFEF6FA7) else Color(0xFF3C9EEB))) {
                        Box(contentAlignment = Alignment.Center) { Text(if (female) "♀" else "♂", color = if (female) Color(0xFFEF6FA7) else Color(0xFF3C9EEB), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), verticalArrangement=Arrangement.Center) { Text(name, maxLines=1, color=if(active) accent else SonHarfMuted, fontSize=11.sp, fontWeight=FontWeight.Bold); Text(score.toString(), fontWeight=FontWeight.Black, fontSize=24.sp); Text("$rounds round", color=SonHarfMuted, fontSize=10.sp) }
        }
    }
}
