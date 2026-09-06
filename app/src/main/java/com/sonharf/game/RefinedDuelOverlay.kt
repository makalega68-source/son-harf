package com.sonharf.game

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.Locale

private val DuelBg get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF070A12) else Color(0xFFF4F7FB)
private val DuelSurface get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF111722) else Color.White
private val DuelSurface2 get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF202A38) else Color(0xFFE4E8EE)
private val DuelKey get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF182230) else Color(0xFFF9FAFC)
private val DuelText get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFF5F7FC) else Color(0xFF172033)
private val DuelMuted get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFAEB9C9) else Color(0xFF677386)
private val DuelBlue get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFF0B84D) else Color(0xFF238BFF)
private val DuelGreen = Color(0xFF1C9B5F)
private val DuelRed = Color(0xFFD53B45)
private val DuelBorder = Color(0xFFD4DCE7)
private val DuelVip = Color(0xFF8B63D9)

private data class DuelFeedback(val correct: Boolean, val label: String)
private const val DuelSubmitTimeoutMs = 7_000L
private val rejectedWordEvents = setOf(
    "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word",
    "ends_with_soft_g", "turn_expired", "not_your_turn",
)

private fun remainingSeconds(deadline: String?, live: Boolean): Int {
    if (!live || deadline.isNullOrBlank()) return 0
    return runCatching {
        val remainingMs = Instant.parse(deadline).toEpochMilli() - System.currentTimeMillis()
        if (remainingMs <= 0L) 0 else ((remainingMs + 999L) / 1000L).toInt()
    }.getOrDefault(0)
}

private fun deadlineExpired(deadline: String?): Boolean {
    if (deadline.isNullOrBlank()) return false
    return runCatching { Instant.parse(deadline).toEpochMilli() <= System.currentTimeMillis() }.getOrDefault(false)
}

private fun failedWordLabel(raw: String, shownWord: String): String = when {
    "word_already_used" in raw -> sh("$shownWord ✕ • DAHA ÖNCE KULLANILDI", "$shownWord ✕ • ALREADY USED")
    "wrong_start_letter" in raw -> sh("$shownWord ✕ • YANLIŞ HARF", "$shownWord ✕ • WRONG LETTER")
    "not_in_dictionary" in raw -> sh("$shownWord ✕ • SÖZLÜKTE YOK", "$shownWord ✕ • NOT IN DICTIONARY")
    "invalid_word" in raw -> sh("$shownWord ✕ • GEÇERSİZ", "$shownWord ✕ • INVALID")
    "ends_with_soft_g" in raw -> sh("$shownWord ✕ • Ğ İLE BİTEMEZ", "$shownWord ✕ • CANNOT END WITH Ğ")
    "voice_limit_reached" in raw -> sh("SESLİ GİRİŞ HAKKI DOLDU", "VOICE LIMIT REACHED")
    "not_your_turn" in raw -> sh("SIRA RAKİPTE", "OPPONENT'S TURN")
    else -> sh("$shownWord ✕ -5", "$shownWord ✕ -5")
}

@Composable
internal fun RefinedDuelOverlay() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var myProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<DuelFeedback?>(null) }
    var actionText by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var timeoutClaimKey by remember { mutableStateOf<String?>(null) }
    var voiceUses by remember { mutableIntStateOf(0) }
    var voiceRequestId by remember { mutableStateOf<String?>(null) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var triviaSelection by remember { mutableStateOf<Long?>(null) }
    var triviaTimeoutKey by remember { mutableStateOf<String?>(null) }

    val voiceInput = rememberVoiceWordInput(room?.language ?: SonHarfUiState.language) { recognized, requestId ->
        input = recognized.take(40)
        voiceRequestId = requestId
        feedback = null
    }

    suspend fun loadProfiles(r: GameRoomDto) {
        val me = backend.currentUserId() ?: return
        myProfile = runCatching { backend.getProfile(me) }.getOrNull()
        opponentProfile = if (r.isBot) null else {
            val id = if (r.hostId == me) r.guestId else r.hostId
            id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        }
    }

    suspend fun discover(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter {
                (it.hostId == me || it.guestId == me) &&
                    it.status in setOf("playing", "quiz", "final", "sudden_death", "paused")
            }
            .maxWithOrNull(compareBy<GameRoomDto> { it.roundNo }.thenBy { it.validWordCount })
    }

    LaunchedEffect(Unit) {
        // A running match can be restored before MonsterExperienceApp is composed.
        // Load equipped Style here as well so an owned/equipped arena is never lost.
        withTimeoutOrNull(3_500L) {
            runCatching { SonHarfCosmetics.applyAndPersist(context, backend.getEquippedCosmetics()) }
        }
        while (true) {
            val current = room
            val incoming = withTimeoutOrNull(5_000L) {
                if (current == null) runCatching { discover() }.getOrNull()
                else runCatching { backend.getRoom(current.id) }.getOrNull()
            }
            if (incoming != null) {
                val accept = current == null || shouldAcceptClassicSnapshot(current, incoming)
                if (accept) {
                    room = incoming
                    if (current == null || classicDeadlineEventKey(current) != classicDeadlineEventKey(incoming)) {
                        timeoutClaimKey = null
                    }
                }
                // Words change only when the authoritative count changes. Avoiding a second
                // network request on every room tick keeps scrolling and typing smooth.
                if (current == null || current.validWordCount != incoming.validWordCount) {
                    words = runCatching { backend.getWords(incoming.id) }.getOrDefault(words)
                }
                if (myProfile == null || (!incoming.isBot && opponentProfile == null)) {
                    withTimeoutOrNull(3_500L) { loadProfiles(incoming) }
                }
                if (showChat && !incoming.isBot) chat = runCatching { backend.getChat(incoming.id) }.getOrDefault(chat)
                if (incoming.status == "quiz") {
                    val nextRound = runCatching { backend.getActiveTriviaRound(incoming.id) }.getOrNull()
                    if (nextRound != null) {
                        if (triviaRound?.id != nextRound.id || triviaQuestion?.id != nextRound.questionId) {
                            triviaQuestion = runCatching { backend.getTriviaQuestion(nextRound.questionId) }.getOrNull()
                            triviaSelection = null
                            triviaTimeoutKey = null
                        }
                        triviaRound = nextRound
                        if (triviaSelection == null) {
                            triviaSelection = runCatching { backend.getMyTriviaAnswer(nextRound.id)?.answerIndex }.getOrNull()
                        }
                    }
                } else {
                    triviaRound = null
                    triviaQuestion = null
                    triviaSelection = null
                    triviaTimeoutKey = null
                }
            }
            delay(900)
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(1450)
            feedback = null
        }
    }
    LaunchedEffect(actionText) {
        if (actionText != null) {
            delay(1800)
            actionText = null
        }
    }

    val active = room ?: return
    val me = backend.currentUserId()
    val isHost = me == active.hostId
    val myScore = if (isHost) active.hostScore else active.guestScore
    val opponentScore = if (isHost) active.guestScore else active.hostScore
    val myStreak = if (isHost) active.hostStreak else active.guestStreak
    val opponentStreak = if (isHost) active.guestStreak else active.hostStreak
    val liveWordPhase = active.status in setOf("playing", "final", "sudden_death")
    val myTurn = liveWordPhase && active.currentPlayerId == me && !active.botTurn
    val opponentTurn = liveWordPhase && !myTurn
    val locale = if (active.language == "tr") Locale("tr", "TR") else Locale.ENGLISH
    val lastWord = words.lastOrNull()?.word?.uppercase(locale).orEmpty()
    val required = words.lastOrNull()?.normalizedWord?.takeLast(1)
        ?.let { gameUppercase(it, active.language) }
        .orEmpty()
    var seconds by remember(active.id, active.turnDeadline, active.currentPlayerId, active.status) {
        mutableIntStateOf(remainingSeconds(active.turnDeadline, liveWordPhase))
    }

    LaunchedEffect(active.id) {
        voiceUses = runCatching { backend.getVoiceUses(active.id) }.getOrDefault(0)
    }

    LaunchedEffect(active.turnDeadline, active.currentPlayerId, active.status) {
        while (true) {
            seconds = remainingSeconds(active.turnDeadline, liveWordPhase)
            if (seconds <= 0) break
            delay(200)
        }
    }

    LaunchedEffect(active.id, active.turnDeadline, active.currentPlayerId, active.status, seconds) {
        if (!liveWordPhase || active.turnDeadline == null || seconds > 0 || !deadlineExpired(active.turnDeadline)) return@LaunchedEffect
        val key = classicDeadlineEventKey(active)
        if (timeoutClaimKey == key) return@LaunchedEffect
        timeoutClaimKey = key
        runCatching { backend.claimTurnTimeout(active.id) }
            .onSuccess { updated ->
                if (shouldAcceptClassicSnapshot(active, updated)) room = updated
                feedback = DuelFeedback(false, sh("SÜRE DOLDU", "TIME UP"))
            }
            .onFailure {
                delay(350)
                val reconciled = runCatching { backend.getRoom(active.id) }.getOrNull()
                if (reconciled != null && shouldAcceptClassicSnapshot(active, reconciled)) room = reconciled
            }
    }

    LaunchedEffect(active.id, active.status, triviaRound?.id, triviaRound?.resolvedAt, triviaRound?.answerDeadline, triviaRound?.resultUntil) {
        val round = triviaRound ?: return@LaunchedEffect
        if (active.status != "quiz") return@LaunchedEffect
        val deadline = if (round.resolvedAt == null) round.answerDeadline else round.resultUntil
        if (deadline.isNullOrBlank()) return@LaunchedEffect
        while (!deadlineExpired(deadline)) delay(200)
        val key = "${round.id}:${round.resolvedAt ?: "answer"}"
        if (triviaTimeoutKey == key) return@LaunchedEffect
        triviaTimeoutKey = key
        val updated = runCatching {
            if (round.resolvedAt == null) backend.claimTriviaTimeout(round.id)
            else backend.finishTriviaResult(round.id)
        }.getOrNull()
        if (updated != null && shouldAcceptClassicSnapshot(active, updated)) room = updated
    }

    LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount, active.roundNo) {
        if (!active.isBot || !active.botTurn || !liveWordPhase) return@LaunchedEffect
        delay(650)
        repeat(3) { attempt ->
            val latest = runCatching { backend.getRoom(active.id) }.getOrNull() ?: return@repeat
            if (!latest.botTurn || latest.status !in setOf("playing", "final", "sudden_death")) {
                if (shouldAcceptClassicSnapshot(active, latest)) room = latest
                return@LaunchedEffect
            }
            val moved = runCatching { backend.botTakeTurn(active.id) }.getOrNull()
            if (moved != null) {
                room = moved
                words = runCatching { backend.getWords(active.id) }.getOrDefault(words)
                return@LaunchedEffect
            }
            if (attempt < 2) delay(500)
        }
    }

    fun manualEdit(next: String) {
        if (!myTurn || busy) return
        input = next.take(40)
        voiceRequestId = null
    }

    fun appendKey(key: String) {
        if (!myTurn || busy || input.length >= 40) return
        manualEdit(input + key)
    }

    fun submit() {
        val submitted = input.trim()
        if (!myTurn || busy || submitted.isBlank()) return
        scope.launch {
            busy = true
            try {
                // The server already verifies turn ownership. A separate pre-flight getRoom()
                // added one full round trip before every answer and was the visible submit delay.
                val before = active
                val beforeMine = if (me == before.hostId) before.hostScore else before.guestScore
                val beforeOpp = if (me == before.hostId) before.guestScore else before.hostScore
                val shownWord = gameUppercase(submitted, active.language)
                val voiceToken = voiceRequestId
                val result = runCatching {
                    withTimeout(DuelSubmitTimeoutMs) {
                        if (voiceToken != null) backend.submitVoiceWord(active.id, submitted, voiceToken)
                        else backend.submitWord(active.id, submitted)
                    }
                }
                result.onSuccess { updated ->
                    room = updated
                    input = ""
                    voiceRequestId = null
                    if (voiceToken != null) {
                        voiceUses = withTimeoutOrNull(2_500L) { backend.getVoiceUses(active.id) } ?: (voiceUses + 1)
                    }
                    val afterMine = if (me == updated.hostId) updated.hostScore else updated.guestScore
                    val serverRejected = updated.lastEventPlayerId == me && updated.lastEvent in rejectedWordEvents
                    val accepted = !serverRejected && (
                        updated.validWordCount > before.validWordCount ||
                            updated.lastEventPlayerId == me ||
                            (updated.status == "finished" && updated.lastEvent == "sudden_death_word")
                        )
                    if (accepted) {
                        val delta = afterMine - beforeMine
                        feedback = DuelFeedback(true, "$shownWord ✓ +${if (delta > 0) delta else 3}")
                        val streak = if (me == updated.hostId) updated.hostStreak else updated.guestStreak
                        val afterOpp = if (me == updated.hostId) updated.guestScore else updated.hostScore
                        actionText = when {
                            beforeMine <= beforeOpp && afterMine > afterOpp -> sh("👑 LİDERLİK SENDE", "👑 YOU TOOK THE LEAD")
                            streak >= 5 -> sh("⚡ KELİME FIRTINASI x$streak", "⚡ WORD STORM x$streak")
                            streak >= 3 -> sh("🔥 SERİ x$streak", "🔥 STREAK x$streak")
                            submitted.length >= 9 -> sh("💥 UZUN KELİME", "💥 LONG WORD")
                            else -> null
                        }
                    } else {
                        feedback = DuelFeedback(false, failedWordLabel(updated.lastEvent.orEmpty(), shownWord))
                    }
                    words = withTimeoutOrNull(2_500L) { backend.getWords(active.id) } ?: words
                }.onFailure { error ->
                    input = ""
                    voiceRequestId = null
                    val (reconciled, reconciledWords) = coroutineScope {
                        val roomTask = async { withTimeoutOrNull(2_500L) { backend.getRoom(active.id) } }
                        val wordsTask = async { withTimeoutOrNull(2_500L) { backend.getWords(active.id) } ?: words }
                        roomTask.await() to wordsTask.await()
                    }
                    val normalizedSubmitted = submitted.lowercase(locale)
                    val acceptedOnServer = reconciledWords.any {
                        it.playerId == me && (
                            it.word.lowercase(locale) == normalizedSubmitted ||
                                it.normalizedWord.lowercase(locale) == normalizedSubmitted
                            )
                    }
                    if (reconciled != null) room = reconciled
                    words = reconciledWords
                    feedback = if (acceptedOnServer) DuelFeedback(true, "$shownWord ✓")
                    else DuelFeedback(false, if (error is kotlinx.coroutines.TimeoutCancellationException) sh("BAĞLANTI YAVAŞ • TEKRAR DENE", "SLOW CONNECTION • TRY AGAIN") else failedWordLabel(error.message.orEmpty(), shownWord))
                }
            } finally {
                busy = false
            }
        }
    }

    fun submitTrivia(estimate: Int) {
        val round = triviaRound ?: return
        if (busy || active.status != "quiz" || round.resolvedAt != null || triviaSelection != null) return
        scope.launch {
            busy = true
            runCatching { backend.answerTrivia(round.id, estimate) }
                .onSuccess { updated ->
                    room = updated
                    triviaSelection = estimate.toLong()
                    triviaRound = runCatching { backend.getActiveTriviaRound(active.id) }.getOrDefault(triviaRound)
                }
                .onFailure { actionText = sh("Cevap gönderilemedi, tekrar dene", "Answer could not be sent, try again") }
            busy = false
        }
    }

    Surface(Modifier.fillMaxSize(), color = DuelBg) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            DuelTopControls(onForfeit = { showForfeit = true }, onChat = {
                if (myProfile?.isVip != true) actionText = sh("Sohbet VIP üyelerine özeldir", "Chat is for VIP members")
                else if (active.isBot) actionText = sh("Bot maçında sohbet kapalı", "Chat is disabled in bot matches")
                else scope.launch {
                    chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList())
                    showChat = true
                }
            })

            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactPlayerCard(
                    name = myProfile?.displayName ?: sh("SEN", "YOU"),
                    score = myScore,
                    streak = myStreak,
                    active = myTurn,
                    avatarPath = myProfile?.avatarPath,
                    gender = myProfile?.gender,
                    frameId = SonHarfCosmetics.profileFrameId,
                    modifier = Modifier.weight(1f),
                )
                DuelCountdown(active.status, seconds, Modifier.width(68.dp))
                CompactPlayerCard(
                    name = if (active.isBot) active.botName ?: "KelimeBot" else opponentProfile?.displayName ?: sh("RAKİP", "OPPONENT"),
                    score = opponentScore,
                    streak = opponentStreak,
                    active = opponentTurn,
                    avatarPath = opponentProfile?.avatarPath,
                    gender = opponentProfile?.gender,
                    frameId = null,
                    modifier = Modifier.weight(1f),
                )
            }

            TurnStatusBar(status = active.status, myTurn = myTurn, opponentTurn = opponentTurn, isBot = active.isBot)

            Box(Modifier.fillMaxWidth().weight(1f).heightIn(min = 155.dp, max = 285.dp), contentAlignment = Alignment.Center) {
                CentralWordCard(lastWord, required, active.status, feedback, actionText)
            }

            PlayedWordsStrip(words = words, isVip = myProfile?.isVip == true, locale = locale)

            TypedWordField(
                input = input,
                enabled = myTurn && !busy,
                feedback = feedback,
                voiceSupported = voiceInput.supported,
                voiceUses = voiceUses,
                onVoice = {
                    if (!myTurn || busy) return@TypedWordField
                    if (voiceUses >= 5) feedback = DuelFeedback(false, sh("SESLİ GİRİŞ HAKKI DOLDU", "VOICE LIMIT REACHED"))
                    else voiceInput.launch()
                },
            )
            AndroidTurkishKeyboard(
                enabled = myTurn && !busy,
                onKey = ::appendKey,
                onDelete = { if (input.isNotEmpty() && myTurn && !busy) manualEdit(input.dropLast(1)) },
                onSend = ::submit,
            )
        }
    }

    if (showForfeit) {
        AlertDialog(
            onDismissRequest = { showForfeit = false },
            title = { Text(sh("Maçtan çekil?", "Forfeit match?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Maç rakibin lehine sona erecek.", "The match will end in your opponent's favor.")) },
            confirmButton = {
                TextButton(onClick = {
                    showForfeit = false
                    scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } }
                }) { Text(sh("PES ET", "FORFEIT"), color = DuelRed, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showForfeit = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showChat && !active.isBot) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text(sh("MAÇ SOHBETİ", "MATCH CHAT"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(chat.takeLast(30), key = { it.id }) { message ->
                            Text(
                                (if (message.senderId == me) sh("Sen: ", "You: ") else sh("Rakip: ", "Opponent: ")) + message.body,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it.take(300) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(sh("Mesaj yaz", "Type a message")) },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = chatInput.trim()
                    if (text.isNotEmpty()) scope.launch {
                        runCatching { backend.sendChat(active.id, text) }.onSuccess {
                            chatInput = ""
                            chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                        }
                    }
                }) { Text(sh("GÖNDER", "SEND"), fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showChat = false }) { Text(sh("KAPAT", "CLOSE")) } },
        )
    }

    if (active.status == "quiz") {
        val round = triviaRound
        val question = triviaQuestion
        if (round != null && question != null) {
            RefinedTriviaDialog(
                round = round,
                question = question,
                myAnswer = triviaSelection,
                me = me,
                busy = busy,
                onSubmit = ::submitTrivia,
            )
        }
    }
}

@Composable
private fun RefinedTriviaDialog(
    round: TriviaRoundDto,
    question: TriviaQuestionDto,
    myAnswer: Long?,
    me: String?,
    busy: Boolean,
    onSubmit: (Int) -> Unit,
) {
    var value by remember(round.id) { mutableStateOf("") }
    val estimate = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = {},
        containerColor = DuelSurface,
        title = { Text("★ ${sh("BİL BAKALIM", "TRIVIA")} +${round.bonusPoints}", color = DuelBlue, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(question.question, color = DuelText, fontWeight = FontWeight.Bold)
                when {
                    round.resolvedAt != null -> {
                        Text(sh("DOĞRU CEVAP: ${round.correctAnswer ?: "—"}", "CORRECT ANSWER: ${round.correctAnswer ?: "—"}"), color = DuelText, fontWeight = FontWeight.Black)
                        Text(
                            if (round.winnerId == null) sh("BERABERE", "TIE")
                            else if (round.winnerId == me) sh("KAZANDIN", "YOU WON")
                            else sh("RAKİP DAHA YAKIN", "OPPONENT WAS CLOSER"),
                            color = if (round.winnerId == me) DuelGreen else DuelRed,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    myAnswer != null -> Text(sh("Cevabın alındı: $myAnswer", "Answer received: $myAnswer"), color = DuelBlue, fontWeight = FontWeight.Bold)
                    else -> OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.filter(Char::isDigit).take(9) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(sh("Tahminin", "Your estimate")) },
                    )
                }
            }
        },
        confirmButton = {
            if (round.resolvedAt == null && myAnswer == null) {
                Button(onClick = { estimate?.let(onSubmit) }, enabled = estimate != null && !busy) {
                    Text(sh("CEVABI GÖNDER", "SEND ANSWER"), fontWeight = FontWeight.Black)
                }
            }
        },
    )
}

@Composable
private fun DuelTopControls(onForfeit: () -> Unit, onChat: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onForfeit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text(sh("Pes Et", "Forfeit"), color = DuelRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onChat, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text(sh("Sohbet", "Chat"), color = DuelBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompactPlayerCard(
    name: String,
    score: Int,
    streak: Int,
    active: Boolean,
    avatarPath: String?,
    gender: String?,
    frameId: String?,
    modifier: Modifier = Modifier,
) {
    val border = if (active) DuelBlue else DuelBorder
    val background = if (active) DuelBlue.copy(alpha = .09f) else DuelSurface
    Surface(modifier = modifier.fillMaxHeight(), color = background, shape = RoundedCornerShape(15.dp), border = BorderStroke(if (active) 2.dp else 1.dp, border)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            FramedProfilePhotoAvatar(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = 34.dp,
                frameId = frameId,
                accent = if (active) DuelBlue else DuelSurface2,
                showGenderBadge = false,
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = DuelText, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$score", color = if (active) DuelBlue else DuelText, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
            if (streak >= 3) Text("🔥$streak", fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DuelCountdown(status: String, seconds: Int, modifier: Modifier = Modifier) {
    val urgent = seconds in 1..3
    Surface(modifier = modifier.height(56.dp), color = if (urgent) DuelRed.copy(alpha = .1f) else DuelSurface, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, if (urgent) DuelRed else DuelBorder)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                when (status) {
                    "quiz" -> "BİL"
                    "paused" -> "…"
                    "finished" -> "✓"
                    else -> seconds.toString().padStart(2, '0')
                },
                color = if (urgent) DuelRed else DuelText,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
            Text(
                when (status) {
                    "paused" -> sh("BAĞLANTI", "LINK")
                    "finished" -> sh("BİTTİ", "DONE")
                    else -> sh("SN", "SEC")
                },
                color = DuelMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TurnStatusBar(status: String, myTurn: Boolean, opponentTurn: Boolean, isBot: Boolean) {
    val (label, color) = when {
        status == "paused" -> sh("BAĞLANTI BEKLENİYOR", "WAITING FOR CONNECTION") to DuelMuted
        status == "quiz" -> sh("BİL BAKALIM", "TRIVIA") to DuelVip
        status == "sudden_death" && myTurn -> sh("ALTIN HARF • SIRA SENDE", "SUDDEN DEATH • YOUR TURN") to DuelRed
        status == "sudden_death" -> sh("ALTIN HARF • RAKİPTE", "SUDDEN DEATH • OPPONENT") to DuelRed
        status == "final" && myTurn -> sh("FİNAL • SIRA SENDE", "FINAL • YOUR TURN") to DuelBlue
        status == "final" -> sh("FİNAL • RAKİPTE", "FINAL • OPPONENT") to DuelBlue
        status == "finished" -> sh("MAÇ TAMAMLANDI", "MATCH COMPLETE") to DuelGreen
        myTurn -> sh("SIRA SENDE", "YOUR TURN") to DuelBlue
        opponentTurn -> (if (isBot) sh("BOT DÜŞÜNÜYOR", "BOT IS THINKING") else sh("RAKİBİN SIRASI", "OPPONENT'S TURN")) to DuelMuted
        else -> sh("HAZIR", "READY") to DuelMuted
    }
    Surface(Modifier.fillMaxWidth().height(28.dp), color = color.copy(alpha = .08f), shape = RoundedCornerShape(10.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CentralWordCard(lastWord: String, required: String, status: String, feedback: DuelFeedback?, actionText: String?) {
    val feedbackColor = if (feedback?.correct == true) DuelGreen else DuelRed
    Surface(Modifier.fillMaxWidth().fillMaxHeight(), color = DuelSurface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, DuelBorder)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(sh("SON KELİME", "LAST WORD"), color = DuelMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(
                if (lastWord.isBlank()) sh("İLK KELİME", "FIRST WORD") else lastWord,
                color = DuelText,
                fontSize = if (lastWord.length > 15) 22.sp else 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    status == "quiz" -> "?"
                    status == "paused" -> "…"
                    status == "finished" -> "✓"
                    required.isBlank() -> "—"
                    else -> required
                },
                color = when {
                    feedback != null -> feedbackColor
                    status == "finished" -> DuelGreen
                    else -> DuelBlue
                },
                fontSize = 70.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                when {
                    status == "quiz" -> sh("BİL BAKALIM", "TRIVIA")
                    status == "paused" -> sh("RAKİP BAĞLANTISI BEKLENİYOR", "WAITING FOR OPPONENT")
                    status == "finished" -> sh("MAÇ TAMAMLANDI", "MATCH COMPLETE")
                    required.isBlank() -> sh("İLK HARF SERBEST", "FIRST LETTER FREE")
                    else -> sh("BU HARFLE BAŞLA", "START WITH THIS LETTER")
                },
                color = DuelMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            // Always reserve this line so correct/wrong feedback never shifts the card,
            // keyboard, or surrounding screen vertically.
            Spacer(Modifier.height(3.dp))
            Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                when {
                    feedback != null -> Text(feedback.label, color = feedbackColor, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    actionText != null -> Text(actionText, color = DuelBlue, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun PlayedWordsStrip(words: List<GameWordDto>, isVip: Boolean, locale: Locale) {
    val recent = words.takeLast(24)
    Surface(
        Modifier.fillMaxWidth().height(36.dp),
        color = if (isVip) DuelVip.copy(alpha = .08f) else DuelSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isVip) DuelVip.copy(alpha = .45f) else DuelBorder),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isVip) "VIP" else sh("KELİMELER", "WORDS"),
                color = if (isVip) DuelVip else DuelMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 9.dp, end = 6.dp),
            )
            if (recent.isEmpty()) {
                Text(sh("Henüz kelime yok", "No words yet"), color = DuelMuted, fontSize = 10.sp)
            } else {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(recent, key = { it.id }) { word ->
                        Surface(color = DuelSurface2.copy(alpha = .72f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                word.word.trim().ifBlank { word.normalizedWord.trim() }.uppercase(locale),
                                color = DuelText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypedWordField(
    input: String,
    enabled: Boolean,
    feedback: DuelFeedback?,
    voiceSupported: Boolean,
    voiceUses: Int,
    onVoice: () -> Unit,
) {
    val color = when {
        feedback?.correct == true -> DuelGreen
        feedback?.correct == false -> DuelRed
        enabled -> DuelBlue
        else -> DuelBorder
    }
    Surface(Modifier.fillMaxWidth().height(52.dp), color = DuelSurface, shape = RoundedCornerShape(15.dp), border = BorderStroke(if (feedback != null) 2.dp else 1.dp, color)) {
        Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (input.isBlank()) sh("KELİMENİ YAZ", "TYPE YOUR WORD") else input,
                color = if (input.isBlank()) DuelMuted.copy(alpha = .55f) else DuelText,
                fontSize = 20.sp,
                fontWeight = if (input.isBlank()) FontWeight.Bold else FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (voiceSupported) {
                TextButton(
                    onClick = onVoice,
                    enabled = enabled,
                    modifier = Modifier.width(54.dp).fillMaxHeight(),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎙", fontSize = 18.sp)
                        Text("${(5 - voiceUses).coerceAtLeast(0)}/5", color = DuelMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidTurkishKeyboard(enabled: Boolean, onKey: (String) -> Unit, onDelete: () -> Unit, onSend: () -> Unit) {
    val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "Ğ", "Ü"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ş", "İ"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "Ö", "Ç"),
    )
    Surface(Modifier.fillMaxWidth(), color = if (SonHarfCosmetics.keyboardIsNeon) Color(0xFF102C3B) else DuelSurface2, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            rows.forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (index == 1) Spacer(Modifier.weight(.45f))
                    row.forEach { key -> DuelKeyButton(key, enabled, Modifier.weight(1f)) { onKey(key) } }
                    if (index == 1) Spacer(Modifier.weight(.45f))
                    if (index == 2) DuelKeyButton("⌫", enabled, Modifier.weight(1.25f), destructive = true, onClick = onDelete)
                }
            }
            Button(
                onClick = onSend,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(45.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DuelBlue, disabledContainerColor = Color(0xFFB9C2CE)),
            ) {
                Text(sh("GÖNDER", "SEND"), fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun DuelKeyButton(label: String, enabled: Boolean, modifier: Modifier, destructive: Boolean = false, onClick: () -> Unit) {
    val neon = SonHarfCosmetics.keyboardIsNeon
    val bg = when {
        neon && destructive -> Color(0xFF244C5B)
        neon -> Color(0xFF163F50)
        destructive -> Color(0xFFD4D8E0)
        else -> DuelKey
    }
    Surface(
        modifier = modifier.height(39.dp),
        onClick = onClick,
        enabled = enabled,
        color = bg,
        shape = RoundedCornerShape(9.dp),
        shadowElevation = 1.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = if (!enabled) DuelMuted.copy(alpha = .4f) else if (neon) Color(0xFF62E4EF) else DuelText, fontSize = if (destructive) 20.sp else 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
