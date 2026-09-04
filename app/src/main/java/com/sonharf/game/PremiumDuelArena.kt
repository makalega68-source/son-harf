package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.TriviaQuestionDto
import com.sonharf.game.data.TriviaRoundDto
import java.time.Instant
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Active standard Son Harf match surface.
 *
 * This component intentionally owns presentation only. Score, turn, deadline,
 * word and bonus decisions remain server-authoritative in OnlineGameScreenV6.
 */
private object PremiumDuelUi {
    val Background = Color(0xFFF4F7FC)
    val Surface = Color.White
    val SurfaceSoft = Color(0xFFF0F5FB)
    val Text = Color(0xFF142033)
    val Muted = Color(0xFF68788D)
    val Border = Color(0xFFD8E2EE)
    val Blue = Color(0xFF126DE5)
    val BlueSoft = Color(0xFFE8F2FF)
    val Red = Color(0xFFE44868)
    val RedSoft = Color(0xFFFFEDF2)
    val Green = Color(0xFF20A85A)
    val Gold = Color(0xFFF2A51A)
}

@Composable
internal fun PremiumDuelArena(
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
    transientEvent: DuelTransientEvent?,
    wordInput: String,
    onWordInput: (String) -> Unit,
    notice: String,
    busy: Boolean,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    triviaSelection: Long?,
    voiceSupported: Boolean,
    voiceUses: Int,
    chatEnabled: Boolean,
    unreadChatCount: Int,
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
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val opponentScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val opponentRounds = if (host) room.guestRounds else room.hostRounds
    val myStreak = if (host) room.hostStreak else room.guestStreak
    val opponentStreak = if (host) room.guestStreak else room.hostStreak
    val liveWordPhase = room.status in setOf("playing", "final", "sudden_death")
    val quizActive = room.status == "quiz" && triviaRound != null && triviaQuestion != null
    val myTurn = room.currentPlayerId == me && liveWordPhase
    val lastAccepted = words.lastOrNull()?.normalizedWord?.trim().orEmpty()
    val requiredLetter = lastAccepted.takeLast(1).takeIf { it.isNotBlank() }
        ?.let { gameUppercase(it, room.language) }
        ?: "•"
    val shownWord = feedbackWord ?: gameUppercase(lastAccepted, room.language)
    val shownWordColor = when {
        feedbackCorrect == false -> PremiumDuelUi.Red
        shownWord.isNotBlank() -> PremiumDuelUi.Green
        else -> PremiumDuelUi.Blue
    }

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
            oppRounds = opponentRounds,
            words = words,
            isVip = isVip,
            language = room.language,
            onRematch = onRematch,
            onExit = onExit,
        )
        return
    }

    val triviaResolved = quizActive && triviaRound?.resolvedAt != null
    val deadline = when {
        triviaResolved -> triviaRound?.resultUntil
        quizActive -> triviaRound?.answerDeadline
        else -> room.turnDeadline
    }
    var seconds by remember(deadline, room.status, triviaResolved) {
        mutableIntStateOf(initialDuelSeconds(deadline, triviaResolved))
    }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(deadline, room.currentPlayerId, room.status, triviaResolved) {
        val endMs = runCatching { deadline?.let { Instant.parse(it).toEpochMilli() } }.getOrNull()
        if (endMs == null) {
            seconds = -1
            return@LaunchedEffect
        }
        var lastPulse = Int.MIN_VALUE
        while (true) {
            val remaining = endMs - Instant.now().toEpochMilli()
            if (remaining <= 0L) {
                seconds = 0
                if (quizActive && !triviaResolved) {
                    onTriviaTimeout()
                } else if (!quizActive) {
                    SonHarfSoundFx.explosion()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTimeout()
                }
                break
            }
            val shown = ceil(remaining / 1000.0).toInt().coerceAtLeast(1)
            seconds = shown
            if (!quizActive && shown in 1..10 && shown != lastPulse) {
                lastPulse = shown
                SonHarfSoundFx.heartbeat()
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            delay(minOf(100L, remaining))
        }
    }

    val leader = duelLeader(myScore, opponentScore)
    var previousLeader by remember(room.id) { mutableIntStateOf(leader) }
    var previousScores by remember(room.id) { mutableStateOf(myScore to opponentScore) }
    var leadEvent by remember(room.id) { mutableStateOf<DuelTransientEvent?>(null) }
    var leadEventSerial by remember(room.id) { mutableLongStateOf(10_000L) }

    LaunchedEffect(myScore, opponentScore) {
        val scoreChanged = previousScores != (myScore to opponentScore)
        if (
            scoreChanged &&
            feedbackCorrect != false &&
            transientEvent?.tone != DuelEventTone.Error &&
            leader != previousLeader
        ) {
            val message = when (leader) {
                1 -> sh("ÖNE GEÇTİN", "YOU TOOK THE LEAD")
                -1 -> sh("RAKİP ÖNE GEÇTİ", "OPPONENT TOOK THE LEAD")
                else -> sh("SKOR EŞİTLENDİ", "SCORE TIED")
            }
            val tone = when (leader) {
                1 -> DuelEventTone.Player
                -1 -> DuelEventTone.Opponent
                else -> DuelEventTone.Tie
            }
            leadEventSerial += 1L
            leadEvent = DuelTransientEvent(leadEventSerial, message, tone, 1_100L)
        }
        previousLeader = leader
        previousScores = myScore to opponentScore
    }
    LaunchedEffect(leadEvent?.id) {
        val shown = leadEvent ?: return@LaunchedEffect
        delay(shown.durationMs)
        if (leadEvent?.id == shown.id) leadEvent = null
    }
    LaunchedEffect(transientEvent?.id) {
        if (transientEvent != null) leadEvent = null
    }

    var overflowExpanded by remember(room.id) { mutableStateOf(false) }
    var confirmForfeit by remember(room.id) { mutableStateOf(false) }
    var showRules by remember(room.id) { mutableStateOf(false) }
    val connectionNotice = notice.takeIf {
        it.contains("bağlantı sorunu", ignoreCase = true) ||
            it.contains("bağlantı yenileniyor", ignoreCase = true) ||
            it.contains("connection problem", ignoreCase = true) ||
            it.contains("bakım", ignoreCase = true) ||
            it.contains("maintenance", ignoreCase = true)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, PremiumDuelUi.Background, Color(0xFFEDF3FA))
                )
            )
            .statusBarsPadding(),
    ) {
        val compactHeight = maxHeight < 700.dp
        val narrowWidth = maxWidth < 370.dp
        val horizontalPadding = if (narrowWidth) 8.dp else 12.dp
        val sectionGap = if (compactHeight) 4.dp else 6.dp

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(sectionGap),
        ) {
            PremiumDuelTopBar(
                expanded = overflowExpanded,
                onExpandedChange = { overflowExpanded = it },
                voiceEnabled = myTurn && !busy && !quizActive && voiceSupported && voiceUses < 5,
                voiceUsesLeft = (5 - voiceUses).coerceAtLeast(0),
                onVoice = onVoice,
                onRules = { showRules = true },
                onForfeit = { confirmForfeit = true },
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )

            PremiumDuelHud(
                playerName = playerName,
                playerAvatarPath = playerAvatarPath,
                playerGender = playerGender,
                playerRating = playerRating,
                playerScore = myScore,
                playerRounds = myRounds,
                playerStreak = myStreak,
                opponentName = opponentName.removeSuffix(" BOT"),
                opponentAvatarPath = opponentAvatarPath,
                opponentGender = opponentGender,
                opponentRating = opponentRating,
                opponentScore = opponentScore,
                opponentRounds = opponentRounds,
                opponentStreak = opponentStreak,
                opponentIsBot = room.isBot,
                myTurn = myTurn,
                opponentActive = !myTurn && liveWordPhase,
                seconds = seconds,
                quizActive = quizActive,
                roundNo = room.roundNo,
                roundWordCount = room.roundWordCount,
                compact = compactHeight,
                narrow = narrowWidth,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )

            PremiumCompetitionRail(
                myTurn = myTurn,
                botThinking = room.isBot && room.botTurn,
                quizActive = quizActive,
                myScore = myScore,
                opponentScore = opponentScore,
                chatEnabled = chatEnabled,
                unreadChatCount = unreadChatCount,
                bonusEnabled = myTurn && room.status == "playing" && !busy,
                onChat = onChat,
                onBonus = onBonus,
                narrow = narrowWidth,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )

            PremiumDuelStage(
                requiredLetter = requiredLetter,
                shownWord = shownWord,
                shownWordColor = shownWordColor,
                firstWord = lastAccepted.isBlank(),
                myTurn = myTurn,
                myScore = myScore,
                opponentScore = opponentScore,
                transientEvent = transientEvent ?: leadEvent,
                connectionNotice = connectionNotice,
                compact = compactHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = if (compactHeight) 104.dp else 132.dp)
                    .padding(horizontal = horizontalPadding),
            )

            PremiumMatchWordHistory(
                words = words,
                language = room.language,
                me = me,
                compact = compactHeight,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )

            PremiumWordInput(
                value = wordInput,
                myTurn = myTurn,
                busy = busy,
                quizActive = quizActive,
                voiceSupported = voiceSupported,
                voiceUses = voiceUses,
                onVoice = onVoice,
                compact = compactHeight,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )

            PremiumAndroidGameKeyboard(
                value = wordInput,
                language = room.language,
                enabled = !busy && !quizActive,
                submitEnabled = myTurn && wordInput.isNotBlank() && !busy && !quizActive,
                onValueChange = onWordInput,
                onSubmit = onSubmit,
                compact = compactHeight,
                modifier = Modifier.navigationBarsPadding(),
            )
        }

        if (quizActive) {
            val activeTrivia = requireNotNull(triviaRound)
            BonusDialog(
                round = activeTrivia,
                question = requireNotNull(triviaQuestion),
                myAnswer = (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer)
                    ?: triviaSelection,
                opponentAnswer = if (room.isBot) {
                    activeTrivia.botAnswer
                } else if (host) {
                    activeTrivia.guestAnswer
                } else {
                    activeTrivia.hostAnswer
                },
                myWon = activeTrivia.winnerSide == if (host) "host" else "guest",
                tied = activeTrivia.winnerSide == "tie",
                onTrivia = onTrivia,
            )
        }
    }

    if (confirmForfeit) {
        AlertDialog(
            onDismissRequest = { confirmForfeit = false },
            icon = { Icon(Icons.Rounded.Flag, contentDescription = null, tint = PremiumDuelUi.Red) },
            title = {
                Text(
                    sh("Maçtan çıkılsın mı?", "Leave the match?"),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    sh(
                        "Pes edersen maç rakibin lehine tamamlanır. Bu işlem geri alınamaz.",
                        "If you forfeit, the match ends in your opponent's favor. This cannot be undone.",
                    ),
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmForfeit = false
                        onForfeit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumDuelUi.Red),
                ) {
                    Text(sh("PES ET", "FORFEIT"), fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmForfeit = false }) {
                    Text(sh("VAZGEÇ", "CANCEL"), color = PremiumDuelUi.Text)
                }
            },
        )
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = PremiumDuelUi.Blue) },
            title = { Text(sh("Düello Kuralları", "Duel Rules"), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    sh(
                        "Sırandaki kelime, önceki kelimenin son harfiyle başlamalıdır. Kullanılmış veya sözlükte bulunmayan kelimeler kabul edilmez. Skor, süre ve sıra sunucu tarafından belirlenir.",
                        "Your word must begin with the previous word's final letter. Repeated or unknown words are rejected. Score, time and turn are controlled by the server.",
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showRules = false }) {
                    Text(sh("ANLADIM", "GOT IT"), color = PremiumDuelUi.Blue, fontWeight = FontWeight.Black)
                }
            },
        )
    }
}

private fun initialDuelSeconds(
    deadline: String?,
    triviaResolved: Boolean,
): Int {
    if (deadline == null) return -1
    if (triviaResolved) return 3
    val remaining = runCatching {
        ceil((Instant.parse(deadline).toEpochMilli() - Instant.now().toEpochMilli()) / 1000.0)
            .toInt()
    }.getOrNull()
    return remaining?.coerceIn(0, 10) ?: 10
}

@Composable
private fun PremiumDuelTopBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    voiceEnabled: Boolean,
    voiceUsesLeft: Int,
    onVoice: () -> Unit,
    onRules: () -> Unit,
    onForfeit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sh("SON HARF • DÜELLO", "SON HARF • DUEL"),
            color = PremiumDuelUi.Text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .6.sp,
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = PremiumDuelUi.RedSoft,
            border = BorderStroke(1.dp, PremiumDuelUi.Red.copy(alpha = .22f)),
        ) {
            Row(
                Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(PremiumDuelUi.Red))
                Text(
                    sh("CANLI", "LIVE"),
                    color = PremiumDuelUi.Red,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(Modifier.width(2.dp))
        Box {
            IconButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = sh("Maç menüsü", "Match menu"),
                    tint = PremiumDuelUi.Text,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            sh("Sesli giriş • $voiceUsesLeft hak", "Voice input • $voiceUsesLeft left"),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                    enabled = voiceEnabled,
                    onClick = {
                        onExpandedChange(false)
                        onVoice()
                    },
                )
                DropdownMenuItem(
                    text = { Text(sh("Oyun kuralları", "Game rules"), fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                    onClick = {
                        onExpandedChange(false)
                        onRules()
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            sh("Pes et", "Forfeit"),
                            color = PremiumDuelUi.Red,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = PremiumDuelUi.Red)
                    },
                    onClick = {
                        onExpandedChange(false)
                        onForfeit()
                    },
                )
            }
        }
    }
}

@Composable
private fun PremiumDuelHud(
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    playerRating: Int,
    playerScore: Int,
    playerRounds: Int,
    playerStreak: Int,
    opponentName: String,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentRating: Int,
    opponentScore: Int,
    opponentRounds: Int,
    opponentStreak: Int,
    opponentIsBot: Boolean,
    myTurn: Boolean,
    opponentActive: Boolean,
    seconds: Int,
    quizActive: Boolean,
    roundNo: Int,
    roundWordCount: Int,
    compact: Boolean,
    narrow: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (narrow) 5.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremiumPlayerCard(
            name = playerName,
            avatarPath = playerAvatarPath,
            gender = playerGender,
            rating = playerRating,
            score = playerScore,
            rounds = playerRounds,
            streak = playerStreak,
            active = myTurn,
            accent = PremiumDuelUi.Blue,
            bot = false,
            mirrored = false,
            compact = compact,
            narrow = narrow,
            modifier = Modifier.weight(1f),
        )

        PremiumRoundTimer(
            seconds = seconds,
            quizActive = quizActive,
            roundNo = roundNo,
            roundWordCount = roundWordCount,
            compact = compact,
            narrow = narrow,
        )

        PremiumPlayerCard(
            name = opponentName,
            avatarPath = opponentAvatarPath,
            gender = opponentGender,
            rating = opponentRating,
            score = opponentScore,
            rounds = opponentRounds,
            streak = opponentStreak,
            active = opponentActive,
            accent = PremiumDuelUi.Red,
            bot = opponentIsBot,
            mirrored = true,
            compact = compact,
            narrow = narrow,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PremiumPlayerCard(
    name: String,
    avatarPath: String?,
    gender: String?,
    rating: Int,
    score: Int,
    rounds: Int,
    streak: Int,
    active: Boolean,
    accent: Color,
    bot: Boolean,
    mirrored: Boolean,
    compact: Boolean,
    narrow: Boolean,
    modifier: Modifier,
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 260),
        label = if (mirrored) "opponent-score" else "player-score",
    )
    val height = if (compact) 88.dp else 100.dp
    val photoWidth = when {
        narrow -> 42.dp
        compact -> 44.dp
        else -> 48.dp
    }
    val photoHeight = when {
        compact -> 56.dp
        else -> 64.dp
    }

    Surface(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        color = PremiumDuelUi.Surface,
        border = BorderStroke(
            if (active) 2.dp else 1.dp,
            if (active) accent.copy(alpha = .88f) else PremiumDuelUi.Border,
        ),
        shadowElevation = if (active) 5.dp else 1.dp,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        if (mirrored) {
                            listOf(Color.White, accent.copy(alpha = if (active) .14f else .06f))
                        } else {
                            listOf(accent.copy(alpha = if (active) .14f else .06f), Color.White)
                        }
                    )
                )
                .padding(horizontal = if (narrow) 5.dp else 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!mirrored) {
                PremiumDuelAvatar(bot, name, avatarPath, gender, photoWidth, photoHeight, accent)
                Spacer(Modifier.width(if (narrow) 4.dp else 6.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (mirrored) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (active && !mirrored) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                    }
                    Text(
                        name,
                        color = PremiumDuelUi.Text,
                        fontSize = if (narrow) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (mirrored) TextAlign.End else TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )
                    if (active && mirrored) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                    }
                }
                Text(
                    animatedScore.toString(),
                    color = accent,
                    fontSize = duelScoreFontSize(animatedScore).sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    buildString {
                        append(ratingLeagueProgress(rating).leagueName.uppercase())
                        append(" • ")
                        append(rating)
                    },
                    color = PremiumDuelUi.Muted,
                    fontSize = if (narrow) 7.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    buildString {
                        append("$rounds")
                        append(sh(" TUR", " ROUND"))
                        if (streak >= 2) append(" • 🔥$streak")
                    },
                    color = if (streak >= 2) PremiumDuelUi.Gold else PremiumDuelUi.Muted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            if (mirrored) {
                Spacer(Modifier.width(if (narrow) 4.dp else 6.dp))
                PremiumDuelAvatar(bot, name, avatarPath, gender, photoWidth, photoHeight, accent)
            }
        }
    }
}

@Composable
private fun PremiumDuelAvatar(
    bot: Boolean,
    name: String,
    avatarPath: String?,
    gender: String?,
    width: Dp,
    height: Dp,
    accent: Color,
) {
    if (bot) {
        SyntheticBotPortrait(
            name = name,
            gender = gender ?: botGenderForName(name),
            width = width,
            height = height,
            accent = accent,
        )
    } else {
        ProfilePhotoAvatarRectWithGender(
            avatarPath = avatarPath,
            gender = gender,
            name = name,
            width = width,
            height = height,
            accent = accent,
        )
    }
}

@Composable
private fun PremiumRoundTimer(
    seconds: Int,
    quizActive: Boolean,
    roundNo: Int,
    roundWordCount: Int,
    compact: Boolean,
    narrow: Boolean,
) {
    val size = when {
        narrow -> 58.dp
        compact -> 64.dp
        else -> 70.dp
    }
    val timerColor = when {
        quizActive -> PremiumDuelUi.Gold
        seconds in 1..10 -> PremiumDuelUi.Red
        else -> PremiumDuelUi.Blue
    }
    val progress = if (seconds < 0) 1f else (seconds / 10f).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.width(IntrinsicSize.Min),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = timerColor,
                trackColor = PremiumDuelUi.SurfaceSoft,
                strokeWidth = if (narrow) 3.dp else 4.dp,
            )
            Surface(
                modifier = Modifier.size(size - 9.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        if (seconds < 0) "—" else seconds.toString(),
                        color = PremiumDuelUi.Text,
                        fontSize = if (narrow) 23.sp else 27.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (quizActive) "BONUS" else sh("SN", "SEC"),
                        color = timerColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (roundNo > 0) {
                sh("TUR $roundNo/3 • ${roundWordCount.coerceIn(0, 10)}/10", "ROUND $roundNo/3 • ${roundWordCount.coerceIn(0, 10)}/10")
            } else {
                "—"
            },
            color = PremiumDuelUi.Muted,
            fontSize = if (narrow) 6.sp else 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PremiumCompetitionRail(
    myTurn: Boolean,
    botThinking: Boolean,
    quizActive: Boolean,
    myScore: Int,
    opponentScore: Int,
    chatEnabled: Boolean,
    unreadChatCount: Int,
    bonusEnabled: Boolean,
    onChat: () -> Unit,
    onBonus: () -> Unit,
    narrow: Boolean,
    modifier: Modifier = Modifier,
) {
    val difference = abs(myScore - opponentScore)
    val lead = duelLeader(myScore, opponentScore)
    val turnText = when {
        quizActive -> sh("BONUS DÜELLOSU", "BONUS DUEL")
        myTurn -> sh("SIRA SENDE", "YOUR TURN")
        botThinking -> sh("BOT DÜŞÜNÜYOR", "BOT THINKING")
        else -> sh("RAKİBİN SIRASI", "OPPONENT'S TURN")
    }
    val turnColor = when {
        quizActive -> PremiumDuelUi.Gold
        myTurn -> PremiumDuelUi.Blue
        else -> PremiumDuelUi.Red
    }
    val scoreText = when (lead) {
        1 -> sh("+$difference ÖNDESİN", "+$difference AHEAD")
        -1 -> sh("$difference PUAN GERİDESİN", "$difference BEHIND")
        else -> sh("BERABERE", "TIED")
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(if (narrow) 48.dp else 50.dp),
        shape = RoundedCornerShape(15.dp),
        color = Color.White.copy(alpha = .96f),
        border = BorderStroke(1.dp, PremiumDuelUi.Border),
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = if (narrow) 7.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (narrow) 5.dp else 7.dp),
        ) {
            Row(
                modifier = Modifier.weight(1.05f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(turnColor))
                Text(
                    turnText,
                    color = turnColor,
                    fontSize = if (narrow) 7.sp else 8.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                scoreText,
                modifier = Modifier.weight(.9f),
                color = when (lead) {
                    1 -> PremiumDuelUi.Blue
                    -1 -> PremiumDuelUi.Red
                    else -> PremiumDuelUi.Gold
                },
                fontSize = if (narrow) 7.sp else 8.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedButton(
                onClick = onBonus,
                enabled = bonusEnabled,
                modifier = Modifier
                    .width(if (narrow) 59.dp else 66.dp)
                    .height(if (narrow) 36.dp else 38.dp),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                border = BorderStroke(1.dp, PremiumDuelUi.Gold.copy(alpha = .55f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PremiumDuelUi.Gold,
                    disabledContentColor = PremiumDuelUi.Muted.copy(alpha = .35f),
                ),
            ) {
                Text("★", fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(2.dp))
                Text(sh("BONUS", "BONUS"), fontSize = 7.sp, fontWeight = FontWeight.Black)
            }
            Box {
                OutlinedButton(
                    onClick = onChat,
                    enabled = chatEnabled,
                    modifier = Modifier
                        .width(if (narrow) 72.dp else 82.dp)
                        .height(if (narrow) 36.dp else 38.dp),
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 5.dp),
                    border = BorderStroke(1.dp, PremiumDuelUi.Blue.copy(alpha = .45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PremiumDuelUi.Blue,
                        disabledContentColor = PremiumDuelUi.Muted.copy(alpha = .45f),
                    ),
                ) {
                    Icon(
                        Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(sh("SOHBET", "CHAT"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                if (unreadChatCount > 0 && chatEnabled) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                        shape = CircleShape,
                        color = PremiumDuelUi.Red,
                    ) {
                        Text(
                            unreadChatCount.coerceAtMost(9).let { if (unreadChatCount > 9) "9+" else it.toString() },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumDuelStage(
    requiredLetter: String,
    shownWord: String,
    shownWordColor: Color,
    firstWord: Boolean,
    myTurn: Boolean,
    myScore: Int,
    opponentScore: Int,
    transientEvent: DuelTransientEvent?,
    connectionNotice: String?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 22.dp else 28.dp),
        color = PremiumDuelUi.Surface,
        border = BorderStroke(
            if (myTurn) 1.5.dp else 1.dp,
            if (myTurn) PremiumDuelUi.Blue.copy(alpha = .48f) else PremiumDuelUi.Border,
        ),
        shadowElevation = 3.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (myTurn) PremiumDuelUi.BlueSoft else PremiumDuelUi.RedSoft,
                            Color.White,
                            Color(0xFFF8FAFD),
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = if (compact) 7.dp else 10.dp),
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DuelTransientMessage(
                    event = transientEvent,
                    fallbackNotice = connectionNotice,
                    compact = compact,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        if (firstWord) sh("İLK KELİME", "FIRST WORD") else sh("ZORUNLU HARF", "REQUIRED LETTER"),
                        color = PremiumDuelUi.Muted,
                        fontSize = if (compact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .6.sp,
                    )
                    Text(
                        requiredLetter,
                        color = PremiumDuelUi.Text,
                        fontSize = if (compact) 51.sp else 64.sp,
                        lineHeight = if (compact) 53.sp else 66.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        shownWord.ifBlank {
                            if (firstWord) sh("ZİNCİRİ BAŞLAT", "START THE CHAIN") else "—"
                        },
                        color = shownWordColor,
                        fontSize = when {
                            compact -> 19.sp
                            shownWord.length > 15 -> 19.sp
                            else -> 23.sp
                        },
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DuelBalanceBar(
                    myScore = myScore,
                    opponentScore = opponentScore,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DuelTransientMessage(
    event: DuelTransientEvent?,
    fallbackNotice: String?,
    compact: Boolean,
) {
    var renderedEvent by remember { mutableStateOf(event) }
    LaunchedEffect(event?.id) {
        if (event != null) renderedEvent = event
    }
    Box(
        Modifier.fillMaxWidth().height(if (compact) 21.dp else 25.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = event != null,
            enter = fadeIn(tween(140)) + scaleIn(tween(180), initialScale = .97f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = .98f),
        ) {
            Text(
                renderedEvent?.text.orEmpty(),
                color = when (renderedEvent?.tone) {
                    DuelEventTone.Player -> PremiumDuelUi.Blue
                    DuelEventTone.Opponent -> PremiumDuelUi.Red
                    DuelEventTone.Tie, DuelEventTone.Milestone -> PremiumDuelUi.Gold
                    DuelEventTone.Error -> PremiumDuelUi.Red
                    null -> PremiumDuelUi.Muted
                },
                fontSize = if (compact) 11.sp else 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (event == null && !fallbackNotice.isNullOrBlank()) {
            Text(
                fallbackNotice,
                color = PremiumDuelUi.Muted,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DuelBalanceBar(
    myScore: Int,
    opponentScore: Int,
    modifier: Modifier = Modifier,
) {
    val safeMine = myScore.coerceAtLeast(0)
    val safeOpponent = opponentScore.coerceAtLeast(0)
    val total = safeMine + safeOpponent
    val playerShare = if (total == 0) .5f else (safeMine.toFloat() / total).coerceIn(.08f, .92f)
    Row(
        modifier.height(5.dp).clip(CircleShape).background(PremiumDuelUi.SurfaceSoft),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .weight(playerShare)
                .background(PremiumDuelUi.Blue)
        )
        Box(
            Modifier
                .fillMaxHeight()
                .weight(1f - playerShare)
                .background(PremiumDuelUi.Red.copy(alpha = .76f))
        )
    }
}

@Composable
private fun PremiumMatchWordHistory(
    words: List<GameWordDto>,
    language: String,
    me: String?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(words.lastOrNull()?.id) {
        if (words.isNotEmpty() && !listState.isScrollInProgress) {
            listState.animateScrollToItem(words.lastIndex)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(if (compact) 47.dp else 53.dp),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, PremiumDuelUi.Border),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                sh("KELİME ZİNCİRİ", "WORD CHAIN"),
                color = PremiumDuelUi.Blue,
                fontSize = 7.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(3.dp))
            if (words.isEmpty()) {
                Text(sh("İlk kelime bekleniyor", "Waiting for the first word"), color = PremiumDuelUi.Muted, fontSize = 9.sp)
            } else {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(items = words, key = { it.id }) { word ->
                        val mine = word.playerId != null && word.playerId == me
                        val accent = if (mine) PremiumDuelUi.Blue else PremiumDuelUi.Red
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = accent.copy(alpha = .08f),
                            border = BorderStroke(1.dp, accent.copy(alpha = .20f)),
                        ) {
                            Text(
                                gameUppercase(
                                    word.word.trim().ifBlank { word.normalizedWord.trim() },
                                    language,
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = PremiumDuelUi.Text,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
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
private fun PremiumWordInput(
    value: String,
    myTurn: Boolean,
    busy: Boolean,
    quizActive: Boolean,
    voiceSupported: Boolean,
    voiceUses: Int,
    onVoice: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val voiceEnabled = myTurn && !busy && !quizActive && voiceSupported && voiceUses < 5
    Surface(
        modifier = modifier.fillMaxWidth().height(if (compact) 43.dp else 47.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            if (myTurn && !quizActive) 1.5.dp else 1.dp,
            if (myTurn && !quizActive) PremiumDuelUi.Blue else PremiumDuelUi.Border,
        ),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onVoice,
                enabled = voiceEnabled,
                modifier = Modifier.height(if (compact) 34.dp else 37.dp),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 7.dp),
                border = BorderStroke(1.dp, PremiumDuelUi.Blue.copy(alpha = .32f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PremiumDuelUi.Blue,
                    disabledContentColor = PremiumDuelUi.Muted.copy(alpha = .38f),
                ),
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = sh("Sesli giriş", "Voice input"), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text((5 - voiceUses).coerceAtLeast(0).toString(), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(7.dp))
            Text(
                when {
                    value.isNotBlank() -> value
                    quizActive -> sh("Bonus turu devam ediyor…", "Bonus round in progress…")
                    myTurn -> sh("Kelimenizi yazın…", "Type your word…")
                    else -> sh("Sıranı beklerken kelimeyi hazırlayabilirsin…", "Prepare your word while you wait…")
                },
                modifier = Modifier.weight(1f),
                color = if (value.isBlank()) PremiumDuelUi.Muted else PremiumDuelUi.Text,
                fontSize = if (value.isBlank()) 11.sp else 17.sp,
                fontWeight = if (value.isBlank()) FontWeight.Medium else FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PremiumAndroidGameKeyboard(
    value: String,
    language: String,
    enabled: Boolean,
    submitEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val rows = if (language.equals("en", ignoreCase = true)) {
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
    val keyHeight = if (compact) 37.dp else 41.dp
    val actionHeight = if (compact) 42.dp else 46.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFEDF2F8),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, PremiumDuelUi.Border),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = when (rowIndex) {
                                1 -> if (language.equals("en", true)) 17.dp else 10.dp
                                2 -> if (language.equals("en", true)) 34.dp else 28.dp
                                else -> 0.dp
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    row.forEach { letter ->
                        PremiumKeyboardKey(
                            label = letter,
                            enabled = enabled && value.length < 40,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                        ) {
                            SonHarfSoundFx.typingClick()
                            onValueChange((value + letter).take(40))
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PremiumKeyboardAction(
                    label = sh("SİL", "DELETE"),
                    enabled = enabled && value.isNotEmpty(),
                    height = actionHeight,
                    modifier = Modifier.weight(1f),
                ) {
                    SonHarfSoundFx.tap()
                    onValueChange(value.dropLast(1))
                }
                PremiumKeyboardAction(
                    label = sh("TEMİZLE", "CLEAR"),
                    enabled = enabled && value.isNotEmpty(),
                    height = actionHeight,
                    modifier = Modifier.weight(1.35f),
                ) {
                    SonHarfSoundFx.tap()
                    onValueChange("")
                }
                Button(
                    onClick = {
                        SonHarfSoundFx.tap()
                        onSubmit()
                    },
                    enabled = submitEnabled,
                    modifier = Modifier.weight(1.7f).height(actionHeight),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumDuelUi.Blue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFD6DEE8),
                        disabledContentColor = PremiumDuelUi.Muted.copy(alpha = .60f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                ) {
                    Text(sh("GÖNDER", "SEND"), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PremiumKeyboardKey(
    label: String,
    enabled: Boolean,
    height: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(height)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) Color.White else Color(0xFFF7F9FC),
        border = BorderStroke(1.dp, if (enabled) Color(0xFFC8D4E2) else Color(0xFFDEE5ED)),
        shadowElevation = if (enabled) 1.5.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (enabled) PremiumDuelUi.Text else PremiumDuelUi.Muted.copy(alpha = .42f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PremiumKeyboardAction(
    label: String,
    enabled: Boolean,
    height: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PremiumDuelUi.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = PremiumDuelUi.Text,
            disabledContainerColor = Color(0xFFF6F8FB),
            disabledContentColor = PremiumDuelUi.Muted.copy(alpha = .42f),
        ),
        contentPadding = PaddingValues(horizontal = 3.dp),
    ) {
        Text(
            label,
            fontSize = if (label.length > 6) 9.sp else 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}
