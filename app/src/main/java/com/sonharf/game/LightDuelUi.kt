package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sonharf.game.data.*
import java.time.Instant
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val LBg = Color(0xFFF4F7FB)
private val LCard2 = Color(0xFFF0F4F8)
private val LText = Color(0xFF182235)
private val LMuted = Color(0xFF718096)
private val LBlue = Color(0xFF1769E0)
private val LBlueSoft = Color(0xFFE8F2FF)
private val LBorder = Color(0xFFDDE5EE)
private val LRed = Color(0xFFE24D6B)
private val LOrange = Color(0xFFF47B20)
private val LGold = Color(0xFFF3A81A)
private val LPurple = Color(0xFF7658D6)
private val LGreen = Color(0xFF22A85A)

@Composable
internal fun LightDuelLobby(
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    language: String,
    matching: Boolean,
    notice: String,
    showPrivate: Boolean,
    showFriends: Boolean,
    privateCode: String,
    friends: List<Pair<FriendshipDto, ProfileDto>>,
    invites: List<GameInviteDto>,
    onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit,
    onRandom: () -> Unit,
    onCancel: () -> Unit,
    onPrivate: () -> Unit,
    onFriends: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onInvite: (String) -> Unit,
    onInviteResponse: (String, Boolean) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, LBg))).statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarWithGender(playerAvatarPath, playerGender, playerName, 48.dp, LBlue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playerName, color = LText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(sh("Düelloya hazırsın", "Ready to duel"), color = LMuted, fontSize = 10.sp)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = LBlueSoft) {
                        Text(sh("DÜELLO", "DUEL"), Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = LBlue, fontWeight = FontWeight.Black)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, LBorder),
                ) {
                    Column(
                        Modifier.fillMaxWidth().height(280.dp).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (matching) CircularProgressIndicator(color = LBlue, strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(if (matching) sh("RAKİP ARANIYOR", "SEARCHING OPPONENT") else "SON HARF", color = LBlue, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Text(
                            if (matching) sh("Önce gerçek oyuncu • 15 sn sonra uygun BOT", "Real player first • suitable BOT after 15 sec")
                            else sh("Kelimeyi Sürdür, Rakibini Geç", "Continue the word, beat your rival"),
                            color = LMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LobbyPill(language == "tr", "🇹🇷 TÜRKÇE", Modifier.weight(1f)) { onLanguage("tr") }
                    LobbyPill(language == "en", "🇬🇧 ENGLISH", Modifier.weight(1f)) { onLanguage("en") }
                }
            }
            item {
                Button(
                    onClick = if (matching) onCancel else onRandom,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (matching) Color(0xFFFFEEF2) else LBlue,
                        contentColor = if (matching) LRed else Color.White,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        if (matching) sh("EŞLEŞMEYİ İPTAL ET", "CANCEL MATCHMAKING") else sh("DÜELLOYA GİR ⚡", "ENTER DUEL ⚡"),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LobbyAction("👥", sh("ARKADAŞ", "FRIENDS"), Modifier.weight(1f), onFriends)
                    LobbyAction("♛", sh("ÖZEL ODA", "PRIVATE ROOM"), Modifier.weight(1f), onPrivate)
                }
            }
            item { NoticeCard(notice) }
            if (showPrivate) item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sh("ÖZEL ODA", "PRIVATE ROOM"), color = LText, fontWeight = FontWeight.Black)
                        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text(sh("VIP ODA OLUŞTUR", "CREATE VIP ROOM")) }
                        OutlinedTextField(
                            value = privateCode,
                            onValueChange = onPrivateCode,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(sh("6 haneli oda kodu", "6-character room code")) },
                        )
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth()) {
                            Text(sh("ODA KODUYLA KATIL", "JOIN WITH ROOM CODE"))
                        }
                    }
                }
            }
            if (showFriends) item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sh("ARKADAŞLAR", "FRIENDS"), color = LText, fontWeight = FontWeight.Black)
                        invites.forEach { invite ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onInviteResponse(invite.id, true) }) { Text(sh("Kabul", "Accept")) }
                                TextButton(onClick = { onInviteResponse(invite.id, false) }) { Text(sh("Reddet", "Decline"), color = LRed) }
                            }
                        }
                        friends.forEach { (_, p) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.displayName, color = LText, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (p.presenceStatus == "online") sh("Çevrimiçi", "Online") else sh("Çevrimdışı", "Offline"),
                                        color = LMuted,
                                        fontSize = 9.sp,
                                    )
                                }
                                Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online") { Text(sh("Davet", "Invite")) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DuelStatusEvent(val id: String, val text: String, val color: Color)

@Composable
internal fun LightDuelArena(
    room: GameRoomDto,
    me: String?,
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    playerRating: Int,
    opponentName: String,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentRating: Int,
    words: List<GameWordDto>,
    isVip: Boolean,
    feedbackWord: String?,
    feedbackCorrect: Boolean?,
    wordInput: String,
    onWordInput: (String) -> Unit,
    notice: String,
    busy: Boolean,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    triviaSelection: Long?,
    voiceSupported: Boolean,
    voiceUses: Int,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onBonus: () -> Unit,
    onVoice: () -> Unit,
    onTrivia: (Long) -> Unit,
    onTriviaTimeout: () -> Unit,
    onChat: () -> Unit,
    onForfeit: () -> Unit,
    onExit: () -> Unit,
    onRematch: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val liveWordPhase = room.status in listOf("playing", "final", "sudden_death")
    val quizActive = room.status == "quiz" && triviaRound != null && triviaQuestion != null
    val myTurn = room.currentPlayerId == me && liveWordPhase
    val lastItem = words.lastOrNull()
    val lastWord = lastItem?.normalizedWord?.trim().orEmpty()
    val required = lastWord.takeLast(1).takeIf { it.isNotBlank() }?.let { gameUppercase(it, room.language) } ?: "•"
    val critical = ClassicCompetitionRules.isCritical(myScore, oppScore, room.finalMovesRemaining)

    if (room.status == "waiting") {
        WaitingRoom(room.code, playerName, onExit)
        return
    }
    if (room.status == "finished") {
        CompetitiveResult(
            room = room,
            me = me,
            playerName = playerName,
            opponentName = opponentName,
            startingRating = playerRating,
            myRounds = myRounds,
            oppRounds = oppRounds,
            words = words,
            isVip = isVip,
            language = room.language,
            onRematch = onRematch,
            onExit = onExit,
        )
        return
    }

    val deadline = when {
        quizActive && triviaRound?.resolvedAt != null -> triviaRound?.resultUntil
        quizActive -> triviaRound?.answerDeadline
        else -> room.turnDeadline
    }
    var seconds by remember(deadline, room.status) { mutableStateOf<Int?>(null) }
    var lastSignalledSecond by remember(deadline) { mutableIntStateOf(Int.MIN_VALUE) }
    var timeoutHandled by remember(deadline, room.currentPlayerId, room.status) { mutableStateOf(false) }

    var queuedEvents by remember(room.id) { mutableStateOf(emptyList<DuelStatusEvent>()) }
    var activeEvent by remember(room.id) { mutableStateOf<DuelStatusEvent?>(null) }
    var previousLeader by remember(room.id) { mutableIntStateOf(ClassicCompetitionRules.leader(myScore, oppScore)) }
    var previousWordId by remember(room.id) { mutableStateOf(lastItem?.id) }
    var previousFeedbackSignature by remember(room.id) { mutableStateOf("$feedbackWord|$feedbackCorrect|$notice") }
    var feedbackVisible by remember(room.id) { mutableStateOf(!feedbackWord.isNullOrBlank()) }
    var showMenu by remember(room.id) { mutableStateOf(false) }
    var showForfeitConfirm by remember(room.id) { mutableStateOf(false) }
    var submitLatched by remember(room.id) { mutableStateOf(false) }

    fun enqueue(event: DuelStatusEvent) {
        if (activeEvent?.id != event.id && queuedEvents.none { it.id == event.id }) queuedEvents = queuedEvents + event
    }

    LaunchedEffect(room.id, queuedEvents.size, activeEvent?.id) {
        if (activeEvent == null && queuedEvents.isNotEmpty()) {
            activeEvent = queuedEvents.first()
            queuedEvents = queuedEvents.drop(1)
        }
    }
    LaunchedEffect(room.id, activeEvent?.id) {
        val event = activeEvent ?: return@LaunchedEffect
        delay(ClassicCompetitionRules.ACTION_OVERLAY_MS)
        if (activeEvent?.id == event.id) activeEvent = null
    }

    LaunchedEffect(lastItem?.id, myScore, oppScore) {
        val newLeader = ClassicCompetitionRules.leader(myScore, oppScore)
        val newWord = lastItem != null && lastItem.id != previousWordId
        if (newWord && lastItem?.playerId == me) {
            enqueue(DuelStatusEvent("correct:${lastItem.id}", sh("DOĞRU", "CORRECT"), LGreen))
        }
        if (newLeader != previousLeader) {
            val event = when {
                newLeader == 0 && previousLeader != 0 -> DuelStatusEvent("tie:$myScore:$oppScore", sh("SKOR EŞİTLENDİ", "SCORE TIED"), LPurple)
                newLeader > 0 -> DuelStatusEvent("lead-me:$myScore:$oppScore", sh("ÖNE GEÇTİN", "YOU TOOK THE LEAD"), LBlue)
                newLeader < 0 -> DuelStatusEvent("lead-opp:$myScore:$oppScore", sh("RAKİP ÖNE GEÇTİ", "OPPONENT TOOK THE LEAD"), LRed)
                else -> null
            }
            if (event != null) {
                enqueue(event)
                if (SonHarfPreferences.soundEnabled(context)) SonHarfSoundFx.leadChange()
            }
        }
        previousLeader = newLeader
        previousWordId = lastItem?.id
    }

    LaunchedEffect(feedbackWord, feedbackCorrect, notice) {
        val signature = "$feedbackWord|$feedbackCorrect|$notice"
        if (signature != previousFeedbackSignature) {
            feedbackVisible = !feedbackWord.isNullOrBlank()
            if (feedbackCorrect == false && !feedbackWord.isNullOrBlank()) {
                val reason = duelFriendlyReason(notice, room.language)
                val text = if (reason.isNullOrBlank()) sh("YANLIŞ", "WRONG") else "${sh("YANLIŞ", "WRONG")} • $reason"
                enqueue(DuelStatusEvent("wrong:$signature", text, LRed))
            }
            previousFeedbackSignature = signature
        }
    }
    LaunchedEffect(room.currentPlayerId, room.roundNo) {
        if (feedbackCorrect == false) feedbackVisible = false
    }
    LaunchedEffect(wordInput) {
        if (wordInput.isNotBlank()) feedbackVisible = false
    }
    LaunchedEffect(busy) {
        if (!busy) submitLatched = false
    }

    LaunchedEffect(deadline, room.currentPlayerId, room.status) {
        timeoutHandled = false
        val endMs = runCatching { deadline?.let { Instant.parse(it).toEpochMilli() } }.getOrNull()
        if (endMs == null) {
            seconds = null
            return@LaunchedEffect
        }
        while (true) {
            val remaining = endMs - Instant.now().toEpochMilli()
            if (remaining <= 0L) {
                seconds = 0
                if (!timeoutHandled) {
                    timeoutHandled = true
                    if (quizActive && triviaRound?.resolvedAt == null) {
                        onTriviaTimeout()
                    } else if (!quizActive) {
                        enqueue(DuelStatusEvent("timeout:$deadline", sh("SÜREN DOLDU", "TIME'S UP"), LRed))
                        onTimeout()
                    }
                }
                break
            }
            val shown = ceil(remaining / 1000.0).toInt().coerceAtLeast(1)
            seconds = shown
            if (!quizActive && shown <= ClassicCompetitionRules.URGENT_SECONDS && shown != lastSignalledSecond) {
                lastSignalledSecond = shown
                if (SonHarfPreferences.soundEnabled(context)) {
                    if (shown <= ClassicCompetitionRules.HAPTIC_SECONDS) SonHarfSoundFx.heartbeat() else SonHarfSoundFx.countdown()
                }
                if (ClassicCompetitionRules.shouldHaptic(shown) && SonHarfPreferences.vibrationEnabled(context)) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            delay(minOf(100L, remaining))
        }
    }

    val baseStatus = when {
        quizActive -> sh("BONUS", "BONUS")
        critical -> sh("KRİTİK", "CRITICAL")
        myTurn -> sh("SIRA SENDE", "YOUR TURN")
        room.isBot -> sh("BOT DÜŞÜNÜYOR", "BOT THINKING")
        else -> sh("RAKİBİN SIRASI", "OPPONENT'S TURN")
    }
    val baseStatusColor = when {
        critical -> LOrange
        myTurn -> LBlue
        room.isBot -> LMuted
        else -> LRed
    }
    val timerColor = if ((seconds ?: Int.MAX_VALUE) <= 10) LRed else if (quizActive) LPurple else LBlue
    val shownWord = when {
        wordInput.isNotBlank() -> gameUppercase(wordInput, room.language)
        feedbackVisible && !feedbackWord.isNullOrBlank() -> gameUppercase(feedbackWord, room.language)
        else -> ""
    }
    val shownWordColor = when {
        wordInput.isNotBlank() -> LText
        feedbackVisible && feedbackCorrect == true -> LGreen
        feedbackVisible && feedbackCorrect == false -> LRed
        else -> LText
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, LBg)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val compactHeight = maxHeight < 700.dp
        val hudHeight = if (compactHeight) 82.dp else 92.dp
        val historyHeight = if (compactHeight) 40.dp else 46.dp
        Column(Modifier.fillMaxSize()) {
            CompactDuelHud(
                modifier = Modifier.fillMaxWidth().height(hudHeight),
                playerName = playerName,
                playerAvatarPath = playerAvatarPath,
                playerGender = playerGender,
                playerRating = playerRating,
                playerScore = myScore,
                playerActive = myTurn,
                opponentName = opponentName.removeSuffix(" BOT"),
                opponentAvatarPath = opponentAvatarPath,
                opponentGender = opponentGender,
                opponentRating = opponentRating,
                opponentScore = oppScore,
                opponentActive = !myTurn && liveWordPhase,
                opponentIsBot = room.isBot,
                seconds = seconds,
                timerColor = timerColor,
                roundNo = room.roundNo,
                roundWordCount = room.roundWordCount,
                menuExpanded = showMenu,
                onMenuExpanded = { showMenu = it },
                onChat = onChat,
                chatEnabled = !room.isBot,
                onBonus = onBonus,
                bonusEnabled = liveWordPhase && !busy,
                onVoice = onVoice,
                voiceEnabled = voiceSupported && voiceUses < 5 && myTurn && !busy,
                voiceUses = voiceUses,
                onForfeitRequest = { showForfeitConfirm = true },
                forfeitEnabled = !busy,
            )
            DuelStatusLine(
                modifier = Modifier.fillMaxWidth().height(34.dp),
                event = activeEvent,
                fallbackText = baseStatus,
                fallbackColor = baseStatusColor,
            )
            DuelWordStage(
                modifier = Modifier.fillMaxWidth().weight(1f),
                requiredLetter = required,
                word = shownWord,
                wordColor = shownWordColor,
                compactHeight = compactHeight,
            )
            MatchWordHistory(
                words = words,
                language = room.language,
                modifier = Modifier.fillMaxWidth().height(historyHeight),
            )
            DuelGameKeyboard(
                value = wordInput,
                language = room.language,
                enabled = liveWordPhase && !busy,
                submitEnabled = myTurn && wordInput.isNotBlank() && !busy && !submitLatched,
                onValueChange = onWordInput,
                onSubmit = {
                    if (!submitLatched && myTurn && wordInput.isNotBlank() && !busy) {
                        submitLatched = true
                        onSubmit()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (quizActive && triviaRound != null && triviaQuestion != null) {
        val hostAnswer = triviaRound.hostAnswer
        val guestAnswer = triviaRound.guestAnswer
        val myAnswer = if (host) hostAnswer else guestAnswer
        val opponentAnswer = if (host) guestAnswer else hostAnswer
        val myWon = triviaRound.winnerId == me
        val tied = triviaRound.resolvedAt != null && triviaRound.winnerId == null
        BonusDialog(triviaRound, triviaQuestion, myAnswer, opponentAnswer, myWon, tied, onTrivia)
    }

    if (showForfeitConfirm) {
        AlertDialog(
            onDismissRequest = { showForfeitConfirm = false },
            title = { Text(sh("Maçtan çekil?", "Forfeit match?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Bu işlem maçı sonlandırır. Devam etmek istiyor musun?", "This will end the match. Do you want to continue?")) },
            confirmButton = {
                TextButton(onClick = {
                    showForfeitConfirm = false
                    onForfeit()
                }) { Text(sh("PES ET", "FORFEIT"), color = LRed, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showForfeitConfirm = false }) { Text(sh("VAZGEÇ", "CANCEL")) }
            },
        )
    }
}

@Composable
private fun CompactDuelHud(
    modifier: Modifier,
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    playerRating: Int,
    playerScore: Int,
    playerActive: Boolean,
    opponentName: String,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentRating: Int,
    opponentScore: Int,
    opponentActive: Boolean,
    opponentIsBot: Boolean,
    seconds: Int?,
    timerColor: Color,
    roundNo: Int,
    roundWordCount: Int,
    menuExpanded: Boolean,
    onMenuExpanded: (Boolean) -> Unit,
    onChat: () -> Unit,
    chatEnabled: Boolean,
    onBonus: () -> Unit,
    bonusEnabled: Boolean,
    onVoice: () -> Unit,
    voiceEnabled: Boolean,
    voiceUses: Int,
    onForfeitRequest: () -> Unit,
    forfeitEnabled: Boolean,
) {
    Row(
        modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactPlayer(
            modifier = Modifier.weight(1f),
            name = playerName,
            avatarPath = playerAvatarPath,
            gender = playerGender,
            rating = playerRating,
            score = playerScore,
            active = playerActive,
            accent = LBlue,
            bot = false,
            avatarOnEnd = false,
        )
        Column(
            Modifier.width(78.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(58.dp).background(Color.White, CircleShape).then(
                    Modifier.padding(2.dp)
                ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxSize().background(Color.White, CircleShape).then(
                        Modifier.padding(1.dp)
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = seconds?.toString() ?: "—",
                        color = timerColor,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TUR $roundNo/3 • $roundWordCount/10", color = LMuted, fontSize = 7.sp, maxLines = 1)
                DuelOverflowMenu(
                    expanded = menuExpanded,
                    onExpandedChange = onMenuExpanded,
                    onChat = onChat,
                    chatEnabled = chatEnabled,
                    onBonus = onBonus,
                    bonusEnabled = bonusEnabled,
                    onVoice = onVoice,
                    voiceEnabled = voiceEnabled,
                    voiceUses = voiceUses,
                    onForfeitRequest = onForfeitRequest,
                    forfeitEnabled = forfeitEnabled,
                )
            }
        }
        CompactPlayer(
            modifier = Modifier.weight(1f),
            name = opponentName,
            avatarPath = opponentAvatarPath,
            gender = opponentGender,
            rating = opponentRating,
            score = opponentScore,
            active = opponentActive,
            accent = LRed,
            bot = opponentIsBot,
            avatarOnEnd = true,
        )
    }
}

@Composable
private fun CompactPlayer(
    modifier: Modifier,
    name: String,
    avatarPath: String?,
    gender: String?,
    rating: Int,
    score: Int,
    active: Boolean,
    accent: Color,
    bot: Boolean,
    avatarOnEnd: Boolean,
) {
    val avatarWidth = 42.dp
    val avatarHeight = 54.dp
    val info: @Composable () -> Unit = {
        Column(
            Modifier.weight(1f),
            horizontalAlignment = if (avatarOnEnd) Alignment.End else Alignment.Start,
        ) {
            Text(name, color = LText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(score.toString(), color = LText, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 1)
            val league = ratingLeagueProgress(rating)
            Text("${league.leagueName} • $rating", color = LMuted, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Row(
        modifier.then(if (active) Modifier.background(accent.copy(alpha = .06f), RoundedCornerShape(12.dp)) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!avatarOnEnd) {
            if (bot) SyntheticBotPortrait(name, width = avatarWidth, height = avatarHeight, accent = accent)
            else ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, avatarWidth, avatarHeight, accent)
            Spacer(Modifier.width(5.dp))
            info()
        } else {
            info()
            Spacer(Modifier.width(5.dp))
            if (bot) SyntheticBotPortrait(name, width = avatarWidth, height = avatarHeight, accent = accent)
            else ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, avatarWidth, avatarHeight, accent)
        }
    }
}

@Composable
private fun DuelStatusLine(
    modifier: Modifier,
    event: DuelStatusEvent?,
    fallbackText: String,
    fallbackColor: Color,
) {
    Box(
        modifier.semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = event != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = event?.text.orEmpty(),
                color = event?.color ?: fallbackColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        if (event == null) {
            Text(
                text = fallbackText,
                color = fallbackColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DuelWordStage(
    modifier: Modifier,
    requiredLetter: String,
    word: String,
    wordColor: Color,
    compactHeight: Boolean,
) {
    Column(
        modifier.padding(horizontal = 10.dp, vertical = if (compactHeight) 2.dp else 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = requiredLetter,
            color = LText,
            fontSize = if (compactHeight) 66.sp else 82.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Spacer(Modifier.height(if (compactHeight) 1.dp else 5.dp))
        val wordSize = when {
            word.length >= 26 -> 19.sp
            word.length >= 20 -> 22.sp
            word.length >= 15 -> 25.sp
            else -> if (compactHeight) 28.sp else 32.sp
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = word.ifBlank { " " },
                color = wordColor,
                fontSize = wordSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MatchWordHistory(
    words: List<GameWordDto>,
    language: String,
    modifier: Modifier,
) {
    val state = rememberLazyListState()
    LaunchedEffect(words.lastOrNull()?.id) {
        if (words.isNotEmpty()) state.animateScrollToItem(words.lastIndex)
    }
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        if (words.isEmpty()) {
            Text(sh("Henüz kelime yok", "No words yet"), Modifier.padding(horizontal = 12.dp), color = LMuted, fontSize = 9.sp)
        } else {
            LazyRow(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(items = words, key = { it.id }) { item ->
                    Surface(shape = RoundedCornerShape(9.dp), color = LCard2) {
                        Text(
                            text = gameUppercase(item.word.trim().ifBlank { item.normalizedWord.trim() }, language),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            color = LMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuelOverflowMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onChat: () -> Unit,
    chatEnabled: Boolean,
    onBonus: () -> Unit,
    bonusEnabled: Boolean,
    onVoice: () -> Unit,
    voiceEnabled: Boolean,
    voiceUses: Int,
    onForfeitRequest: () -> Unit,
    forfeitEnabled: Boolean,
) {
    Box {
        IconButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = sh("Diğer maç işlemleri", "More match actions"), tint = LMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            if (chatEnabled) {
                DropdownMenuItem(
                    text = { Text(sh("SOHBET", "CHAT")) },
                    onClick = { onExpandedChange(false); onChat() },
                )
            }
            DropdownMenuItem(
                text = { Text(sh("BONUS", "BONUS")) },
                enabled = bonusEnabled,
                onClick = { onExpandedChange(false); onBonus() },
            )
            DropdownMenuItem(
                text = { Text("${sh("SESLİ GİRİŞ", "VOICE INPUT")} • ${5 - voiceUses.coerceIn(0, 5)}/5") },
                enabled = voiceEnabled,
                onClick = { onExpandedChange(false); onVoice() },
            )
            DropdownMenuItem(
                text = { Text(sh("PES ET", "FORFEIT"), color = LRed) },
                enabled = forfeitEnabled,
                onClick = { onExpandedChange(false); onForfeitRequest() },
            )
        }
    }
}

@Composable
private fun DuelGameKeyboard(
    value: String,
    language: String,
    enabled: Boolean,
    submitEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = if (language.lowercase() == "en") {
        listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M"),
        )
    } else {
        listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "Ğ", "Ü"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ş", "İ"),
            listOf("Z", "X", "C", "V", "B", "N", "M", "Ö", "Ç"),
        )
    }
    Surface(
        modifier = modifier,
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = when (index) { 1 -> 5.dp; 2 -> 16.dp; else -> 0.dp }),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    row.forEach { key ->
                        DuelKeyButton(key, enabled && value.length < 40, Modifier.weight(1f)) {
                            SonHarfSoundFx.typingClick()
                            onValueChange((value + key).take(40))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DuelKeyButton(sh("SİL", "DELETE"), enabled && value.isNotEmpty(), Modifier.weight(1f)) {
                    SonHarfSoundFx.typingClick()
                    onValueChange(value.dropLast(1))
                }
                DuelKeyButton(sh("TEMİZLE", "CLEAR"), enabled && value.isNotEmpty(), Modifier.weight(1.35f)) {
                    SonHarfSoundFx.typingClick()
                    onValueChange("")
                }
                Button(
                    onClick = onSubmit,
                    enabled = submitEnabled,
                    modifier = Modifier.weight(1.7f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LBlue,
                        disabledContainerColor = Color(0xFFCBD3DD),
                        contentColor = Color.White,
                        disabledContentColor = Color.White,
                    ),
                ) {
                    Text(sh("GÖNDER", "SEND"), fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DuelKeyButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.height(46.dp).clickable(enabled = enabled, interactionSource = source, indication = null, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (enabled) LText else LMuted.copy(alpha = .42f),
                fontSize = if (label.length > 5) 8.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private fun duelFriendlyReason(raw: String, language: String): String? {
    val text = raw.trim()
    if (text.isBlank()) return null
    val lower = text.lowercase()
    if (lower.contains("kabul edildi") || lower.contains("accepted")) return null
    return when {
        lower.contains("başlangıç") || lower.contains("start") -> if (language == "en") "Wrong starting letter" else "Başlangıç harfi yanlış"
        lower.contains("kullan") || lower.contains("already") -> if (language == "en") "Word already used" else "Kelime daha önce kullanıldı"
        lower.contains("sözlük") || lower.contains("dictionary") -> if (language == "en") "Not found in dictionary" else "Sözlükte bulunamadı"
        lower.contains("ğ") || lower.contains("soft g") -> if (language == "en") "Words cannot end with Ğ" else "Kelime Ğ ile bitemez"
        lower.contains("süre") || lower.contains("expired") -> if (language == "en") "Turn expired" else "Süre doldu"
        else -> text.take(72)
    }
}

@Composable
private fun CompetitiveResult(
    room: GameRoomDto,
    me: String?,
    playerName: String,
    opponentName: String,
    startingRating: Int,
    myRounds: Int,
    oppRounds: Int,
    words: List<GameWordDto>,
    isVip: Boolean,
    language: String,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    var confirmedRating by remember(room.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(room.id, room.status, me) {
        if (room.status == "finished" && me != null) {
            confirmedRating = runCatching { OnlineGameBackend().getProfile(me).rating }.getOrNull()
        }
    }
    val won = room.winnerId == me
    val draw = room.winnerId == null
    val delta = confirmedRating?.minus(startingRating)
    val progress = confirmedRating?.let(::ratingLeagueProgress)
    val longest = words
        .map { it.word.trim().ifBlank { it.normalizedWord.trim() } }
        .filter { it.isNotBlank() }
        .maxByOrNull { it.length }
        ?.let { gameUppercase(it, language) }
        ?: "—"

    Box(Modifier.fillMaxSize().background(LBg).statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(.9f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, if (won) LBlue.copy(alpha = .4f) else if (draw) LGold.copy(alpha = .4f) else LRed.copy(alpha = .35f)),
        ) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (draw) sh("BERABERE", "DRAW") else if (won) sh("ZAFER", "VICTORY") else sh("MAÇ BİTTİ", "MATCH OVER"),
                    color = if (won) LBlue else if (draw) LGold else LRed,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Black,
                )
                Text("$playerName  $myRounds : $oppRounds  $opponentName", color = LText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (delta != null) {
                    Text("Rating ${if (delta >= 0) "+" else ""}$delta", color = if (delta >= 0) LGreen else LRed, fontSize = 21.sp, fontWeight = FontWeight.Black)
                } else {
                    Text(sh("Rating sonucu sunucudan doğrulanıyor…", "Confirming rating from server…"), color = LMuted, fontSize = 9.sp)
                }
                if (progress != null) {
                    Text("${progress.leagueName} • $confirmedRating", color = LGold, fontWeight = FontWeight.Black)
                    if (progress.nextAt != null) {
                        Text(
                            sh("${progress.nextLeagueName} ligine ${progress.pointsToNext} puan", "${progress.pointsToNext} points to ${progress.nextLeagueName}"),
                            color = LMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResultMetric(sh("KELİME", "WORDS"), words.size.toString(), Modifier.weight(1f))
                    ResultMetric(sh("EN UZUN", "LONGEST"), longest, Modifier.weight(1f))
                }
                if (isVip && words.isNotEmpty()) {
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(words) { w ->
                            Surface(shape = RoundedCornerShape(9.dp), color = LBlueSoft) {
                                Text(
                                    gameUppercase(w.word.trim().ifBlank { w.normalizedWord.trim() }, language),
                                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = onRematch,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LBlue),
                ) {
                    Text(sh("RÖVANŞ ⚡", "REMATCH ⚡"), fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(sh("BİR MAÇ DAHA", "ONE MORE MATCH"), color = LText, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier.height(60.dp), shape = RoundedCornerShape(14.dp), color = LCard2) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = LMuted, fontSize = 8.sp)
            Text(value, color = LText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BonusDialog(
    round: TriviaRoundDto,
    question: TriviaQuestionDto,
    myAnswer: Long?,
    opponentAnswer: Long?,
    myWon: Boolean,
    tied: Boolean,
    onTrivia: (Long) -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).padding(18.dp), contentAlignment = Alignment.Center) {
            var value by remember(round.id) { mutableStateOf("") }
            val parsed = value.toLongOrNull()
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("★ BİL BAKALIM +${round.bonusPoints}", color = LGold, fontWeight = FontWeight.Black)
                    Text(question.question, color = LText, fontWeight = FontWeight.Bold)
                    if (round.resolvedAt == null && myAnswer == null) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter(Char::isDigit).take(16) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                        Button(onClick = { parsed?.let(onTrivia) }, enabled = parsed != null, modifier = Modifier.fillMaxWidth()) {
                            Text(sh("CEVABI KİLİTLE", "LOCK ANSWER"))
                        }
                    } else if (round.resolvedAt == null) {
                        Text(sh("Cevabın alındı.", "Answer received."), color = LBlue)
                    } else {
                        Text(sh("ASIL CEVAP ${round.correctAnswer ?: "—"}", "ACTUAL ANSWER ${round.correctAnswer ?: "—"}"), color = LText, fontWeight = FontWeight.Black)
                        Text(
                            if (tied) sh("BERABERE", "TIE")
                            else if (myWon) sh("DOĞRU • $myAnswer", "CORRECT • $myAnswer")
                            else sh("RAKİP DAHA YAKIN • $opponentAnswer", "OPPONENT CLOSER • $opponentAnswer"),
                            color = if (myWon) LGreen else LRed,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingRoom(code: String, playerName: String, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(LBg).statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth(.86f)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(playerName, fontWeight = FontWeight.Black)
                Text(sh("RAKİP BEKLENİYOR", "WAITING FOR OPPONENT"), color = LBlue, fontWeight = FontWeight.Black)
                Text(code, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                CircularProgressIndicator(color = LBlue)
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text(sh("ODADAN ÇIK", "LEAVE ROOM"), color = LRed) }
            }
        }
    }
}

@Composable
private fun LobbyPill(selected: Boolean, text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier.height(50.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) LBlueSoft else Color.White,
        border = BorderStroke(1.dp, if (selected) LBlue.copy(alpha = .4f) else LBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (selected) LBlue else LText, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LobbyAction(icon: String, title: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.height(88.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LBorder),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 20.sp)
            Text(title, color = LText, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NoticeCard(notice: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp), color = Color.White, border = BorderStroke(1.dp, LBorder)) {
        Text(notice, Modifier.fillMaxWidth().padding(10.dp), color = LMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}
