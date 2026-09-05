package com.sonharf.game

import android.provider.Settings
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlin.math.abs
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
    val lobbyBg = Color(0xFF060817)
    val lobbyCard = Color(0xFF10162D)
    val lobbyText = Color(0xFFF7F8FF)
    val lobbyMuted = Color(0xFFAAB5CE)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(lobbyBg, Color(0xFF11102B), lobbyBg))).statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarRectWithGender(playerAvatarPath, playerGender, playerName, 48.dp, 60.dp, Color(0xFF5B9DFF))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playerName, color = lobbyText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(sh("Düelloya hazırsın", "Ready to duel"), color = lobbyMuted, fontSize = 13.sp)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = LPurple.copy(alpha = .25f), border = BorderStroke(1.dp, LGold.copy(alpha = .45f))) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SportsEsports, null, Modifier.size(19.dp), tint = LGold)
                            Spacer(Modifier.width(6.dp))
                            Text(sh("DÜELLO", "DUEL"), color = lobbyText, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = lobbyCard),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, LPurple.copy(alpha = .55f)),
                ) {
                    Column(
                        Modifier.fillMaxWidth().height(300.dp).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            DuelSearchProfile(playerName, playerAvatarPath, playerGender, Color(0xFF5B9DFF), false)
                            Surface(shape = CircleShape, color = LGold) {
                                Text("VS", Modifier.padding(13.dp), color = Color(0xFF271700), fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            DuelSearchProfile(sh("RAKİP", "RIVAL"), null, null, LRed, matching)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(if (matching) sh("RAKİP ARANIYOR", "SEARCHING OPPONENT") else "SON HARF", color = if (matching) LGold else lobbyText, fontSize = 29.sp, fontWeight = FontWeight.Black)
                        Text(
                            if (matching) sh("Önce gerçek oyuncu • 15 sn sonra uygun BOT", "Real player first • suitable BOT after 15 sec")
                            else sh("Kelimeyi Sürdür, Rakibini Geç", "Continue the word, beat your rival"),
                            color = lobbyMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LobbyPill(language == "tr", "TR • TÜRKÇE", Modifier.weight(1f)) { onLanguage("tr") }
                    LobbyPill(language == "en", "EN • ENGLISH", Modifier.weight(1f)) { onLanguage("en") }
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
                    LobbyAction(Icons.Rounded.Groups, sh("ARKADAŞ", "FRIENDS"), Modifier.weight(1f), onFriends)
                    LobbyAction(Icons.Rounded.MeetingRoom, sh("ÖZEL ODA", "PRIVATE ROOM"), Modifier.weight(1f), onPrivate)
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
                                        fontSize = 12.sp,
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
    busy: Boolean,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    triviaSelection: Long?,
    voiceSupported: Boolean,
    voiceUses: Int,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
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
    val myWordStreak = if (host) room.hostStreak else room.guestStreak
    val oppWordStreak = if (host) room.guestStreak else room.hostStreak
    val liveWordPhase = room.status in listOf("playing", "final", "sudden_death")
    val quizActive = room.status == "quiz" && triviaRound != null && triviaQuestion != null
    val myTurn = room.currentPlayerId == me && liveWordPhase
    val lastItem = words.lastOrNull()
    val lastWord = lastItem?.normalizedWord?.trim().orEmpty()
    val required = lastWord.takeLast(1).takeIf { it.isNotBlank() }?.let { gameUppercase(it, room.language) } ?: "•"
    val inputMatches = ClassicCompetitionRules.inputStartsWithRequired(wordInput, required, room.language)
    val critical = ClassicCompetitionRules.isCritical(myScore, oppScore, room.finalMovesRemaining)
    val reducedMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
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
            playerAvatarPath = playerAvatarPath,
            playerGender = playerGender,
            opponentName = opponentName,
            opponentAvatarPath = opponentAvatarPath,
            opponentGender = opponentGender,
            opponentRating = opponentRating,
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
    var seconds by remember(deadline, room.status) { mutableIntStateOf(0) }
    var timerSynchronizing by remember(room.id) { mutableStateOf(false) }
    var timeoutSignalKey by remember(room.id) { mutableStateOf<String?>(null) }
    var showMoreMenu by remember(room.id) { mutableStateOf(false) }
    var lastSignalledSecond by remember(deadline) { mutableIntStateOf(Int.MIN_VALUE) }
    val timerPulse = remember(deadline) { Animatable(1f) }
    val letterPulse = remember(room.id) { Animatable(1f) }
    var actionOverlay by remember(room.id) { mutableStateOf<String?>(null) }
    var actionThreat by remember(room.id) { mutableStateOf(false) }
    var previousMyScore by remember(room.id) { mutableIntStateOf(myScore) }
    var previousOppScore by remember(room.id) { mutableIntStateOf(oppScore) }
    var previousLeader by remember(room.id) { mutableIntStateOf(ClassicCompetitionRules.leader(myScore, oppScore)) }
    var previousWordId by remember(room.id) { mutableStateOf(lastItem?.id) }

    LaunchedEffect(lastItem?.id) {
        if (lastItem != null && lastItem.id != previousWordId && !reducedMotion) {
            letterPulse.snapTo(1f)
            letterPulse.animateTo(1.13f, tween(130, easing = FastOutSlowInEasing))
            letterPulse.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(room.roundNo) { actionOverlay = null }

    LaunchedEffect(myScore, oppScore, lastItem?.id) {
        val leader = ClassicCompetitionRules.leader(myScore, oppScore)
        val leadMessage = ClassicCompetitionRules.leadChangeText(previousLeader, leader, room.language)
        val newWord = lastItem != null && lastItem.id != previousWordId
        val mine = newWord && lastItem?.playerId == me
        val opponent = newWord && !mine
        val scoreDelta = when {
            mine -> (myScore - previousMyScore).coerceAtLeast(0)
            opponent -> (oppScore - previousOppScore).coerceAtLeast(0)
            else -> 0
        }
        val wordText = lastItem?.word?.trim().takeUnless { it.isNullOrBlank() } ?: lastItem?.normalizedWord.orEmpty()
        val wordStreak = if (mine) myWordStreak else oppWordStreak
        val combo = if (newWord) ClassicCompetitionRules.comboLabel(wordStreak, room.language) else null
        val strong = newWord && ClassicCompetitionRules.isStrongScoreDelta(scoreDelta)
        val longWord = newWord && ClassicCompetitionRules.isLongWord(wordText)

        val message = when {
            leadMessage != null -> leadMessage
            strong && mine -> if (room.language == "en") "POWER MOVE +$scoreDelta" else "GÜÇLÜ HAMLE +$scoreDelta"
            strong && opponent -> if (room.language == "en") "OPPONENT POWER MOVE +$scoreDelta" else "RAKİP GÜÇLÜ HAMLE +$scoreDelta"
            combo != null -> combo
            longWord && mine -> if (room.language == "en") "LONG WORD" else "UZUN KELİME"
            longWord && opponent -> if (room.language == "en") "OPPONENT LONG WORD" else "RAKİP UZUN KELİME"
            else -> null
        }
        if (message != null) {
            actionOverlay = message
            actionThreat = opponent || leader < 0
            if (SonHarfPreferences.soundEnabled(context)) {
                when {
                    strong && mine -> SonHarfSoundFx.wordAccepted()
                    strong && opponent -> SonHarfSoundFx.warning()
                    leadMessage != null -> SonHarfSoundFx.leadChange()
                    else -> SonHarfSoundFx.scoreTick()
                }
            }
            if (mine && (strong || longWord) && SonHarfPreferences.vibrationEnabled(context)) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            delay(ClassicCompetitionRules.ACTION_OVERLAY_MS)
            actionOverlay = null
        }
        previousLeader = leader
        previousMyScore = myScore
        previousOppScore = oppScore
        previousWordId = lastItem?.id
    }

    LaunchedEffect(deadline, room.currentPlayerId, room.status, triviaRound?.id) {
        val endMs = classicDeadlineEpochMs(deadline) ?: run {
            seconds = 0
            timerSynchronizing = false
            return@LaunchedEffect
        }
        val anchor = ClassicMonotonicDeadlineAnchor(
            serverDeadlineEpochMs = endMs,
            wallEpochMsAtAnchor = System.currentTimeMillis(),
            elapsedRealtimeMsAtAnchor = SystemClock.elapsedRealtime(),
        )
        val eventKey = if (quizActive) "quiz:${triviaRound?.id}:$deadline" else classicDeadlineEventKey(room)
        if (timeoutSignalKey != eventKey) timerSynchronizing = false
        while (true) {
            val remaining = anchor.remainingMs(SystemClock.elapsedRealtime())
            val shown = anchor.displaySeconds(SystemClock.elapsedRealtime())
            seconds = shown
            if (remaining <= 0L) {
                timerSynchronizing = true
                if (timeoutSignalKey != eventKey) {
                    timeoutSignalKey = eventKey
                    if (quizActive && triviaRound?.resolvedAt == null) onTriviaTimeout() else if (!quizActive) onTimeout()
                }
                break
            }
            if (!quizActive && shown <= ClassicCompetitionRules.URGENT_SECONDS && shown != lastSignalledSecond) {
                lastSignalledSecond = shown
                if (SonHarfPreferences.soundEnabled(context)) {
                    if (shown <= ClassicCompetitionRules.HAPTIC_SECONDS) SonHarfSoundFx.heartbeat() else SonHarfSoundFx.countdown()
                }
                if (ClassicCompetitionRules.shouldHaptic(shown) && SonHarfPreferences.vibrationEnabled(context)) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            delay(minOf(225L, remaining.coerceAtLeast(1L)))
        }
    }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.validWordCount, room.roundNo, room.status) {
        val currentKey = classicDeadlineEventKey(room)
        if (timeoutSignalKey != null && timeoutSignalKey != currentKey) {
            timerSynchronizing = false
        }
    }

    LaunchedEffect(seconds, reducedMotion) {
        if (!quizActive && ClassicCompetitionRules.isUrgent(seconds) && !reducedMotion) {
            val half = (ClassicCompetitionRules.timerCadenceMs(seconds) / 2).toInt()
            timerPulse.snapTo(1f)
            timerPulse.animateTo(if (seconds <= 3) 1.12f else 1.07f, tween(half))
            timerPulse.animateTo(1f, tween(half))
        } else {
            timerPulse.snapTo(1f)
        }
    }

    val timerColor = when {
        quizActive -> LPurple
        seconds <= 3 -> LRed
        seconds <= 6 -> LOrange
        seconds <= 10 -> LGold
        else -> LBlue
    }
    val shownMyScore by animateIntAsState(myScore, tween(if (reducedMotion) 0 else 280), label = "server-my-score")
    val shownOppScore by animateIntAsState(oppScore, tween(if (reducedMotion) 0 else 280), label = "server-opp-score")

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, LBg))).statusBarsPadding()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompetitivePlayerCard(
                    playerName, playerAvatarPath, playerGender, playerRating, shownMyScore,
                    myTurn, LBlue, false, Modifier.weight(1f),
                )
                Surface(
                    modifier = Modifier.size(78.dp).graphicsLayer {
                        scaleX = timerPulse.value
                        scaleY = timerPulse.value
                    },
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(if (ClassicCompetitionRules.isUrgent(seconds)) 4.dp else 2.dp, timerColor),
                    shadowElevation = if (ClassicCompetitionRules.isUrgent(seconds)) 5.dp else 2.dp,
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (timerSynchronizing && !quizActive) "—" else seconds.toString(),
                            color = LText,
                            fontSize = 31.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(if (quizActive) "BONUS" else sh("SN", "SEC"), color = timerColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(Modifier.weight(1f)) {
          CompetitivePlayerCard(
              opponentName.removeSuffix(" BOT"), opponentAvatarPath, opponentGender, opponentRating, shownOppScore,
              !myTurn && liveWordPhase, LRed, room.isBot, Modifier.fillMaxWidth(),
          )
          Box(Modifier.align(Alignment.TopEnd)) {
              IconButton(
                  onClick = { showMoreMenu = true },
                  modifier = Modifier.size(48.dp),
              ) {
                  Icon(Icons.Rounded.MoreVert, sh("Diğer seçenekler", "More options"), tint = LText)
              }
              DropdownMenu(
                  expanded = showMoreMenu,
                  onDismissRequest = { showMoreMenu = false },
              ) {
                  DropdownMenuItem(
                      text = { Text(sh("PES ET", "FORFEIT"), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                      onClick = {
                          showMoreMenu = false
                          onForfeit()
                      },
                      leadingIcon = { Icon(Icons.Rounded.Flag, null, tint = LRed) },
                  )
              }
          }
      }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (critical) Color(0xFFFFF8E8) else LBlueSoft,
                border = BorderStroke(1.dp, if (critical) LGold.copy(alpha = .5f) else LBlue.copy(alpha = .2f)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (timerSynchronizing && !quizActive) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = LBlue, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            when {
                                quizActive -> sh("BONUS DÜELLOSU", "BONUS DUEL")
                                timerSynchronizing && !quizActive -> sh("Senkronize ediliyor…", "Synchronizing…")
                                myTurn -> sh("● SIRA SENDE", "● YOUR TURN")
                                room.isBot && room.botTurn -> sh("● BOT DÜŞÜNÜYOR", "● BOT THINKING")
                                else -> sh("● RAKİBİN SIRASI", "● OPPONENT TURN")
                            },
                            color = if (myTurn || timerSynchronizing) LBlue else if (quizActive) LPurple else LRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        ClassicCompetitionRules.scoreDifferenceText(myScore, oppScore, room.language),
                        color = if (myScore >= oppScore) LBlue else LRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (critical) Text(sh("KRİTİK", "CRITICAL"), color = LOrange, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (myTurn) LBlue.copy(alpha = .45f) else LBorder),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val lastDisplay = gameUppercase(lastWord, room.language)
                    Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFF7F9FC)) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (lastDisplay.isBlank()) {
                                Text(sh("İlk kelimeyi sen başlat", "You start the first word"), color = LText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            } else {
                                Text(sh("SON KABUL EDİLEN KELİME", "LAST ACCEPTED WORD"), color = LMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(lastDisplay, color = LText, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = actionOverlay != null,
                        enter = fadeIn(tween(170)) + scaleIn(initialScale = .88f, animationSpec = tween(240, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(180)) + scaleOut(targetScale = 1.04f, animationSpec = tween(180)),
                    ) {
                        val comboMoment = actionOverlay.orEmpty().contains("SERİ") || actionOverlay.orEmpty().contains("STREAK")
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (actionThreat) Icons.Rounded.TrendingDown else Icons.Rounded.AutoAwesome,
                                null,
                                Modifier.size(22.dp),
                                tint = if (actionThreat) LRed else if (comboMoment) LGreen else LGold,
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                actionOverlay.orEmpty(),
                                color = if (actionThreat) LRed else if (comboMoment) LGreen else LBlue,
                                fontSize = if (comboMoment) 18.sp else 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(sh("SIRADAKİ ZORUNLU HARF", "NEXT REQUIRED LETTER"), color = LBlue, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(
                        required,
                        modifier = Modifier.graphicsLayer {
                            scaleX = letterPulse.value
                            scaleY = letterPulse.value
                        },
                        color = LText,
                        fontSize = 60.sp,
                        lineHeight = 62.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (required == "•") sh("SERBEST BAŞLANGIÇ", "FREE START") else sh("“$required” İLE BAŞLA", "START WITH “$required”"),
                        color = LBlue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(9.dp))
                    if (words.isNotEmpty()) {
                        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(words.takeLast(5), key = { it.id }) { word ->
                                Surface(shape = RoundedCornerShape(10.dp), color = LCard2) {
                                    Text(
                                        gameUppercase(word.word.trim().ifBlank { word.normalizedWord.trim() }, room.language),
                                        Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        color = LMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallAction(Icons.Rounded.Flag, sh("PES ET", "FORFEIT"), LRed, Modifier.weight(1f), onForfeit)
                SmallAction(Icons.Rounded.Chat, sh("SOHBET", "CHAT"), LBlue, Modifier.weight(1f), onChat)
            }

            CompetitiveInputBar(
                value = wordInput,
                inputMatches = inputMatches,
                feedbackWord = feedbackWord,
                feedbackCorrect = feedbackCorrect,
                myTurn = myTurn,
                busy = busy,
                quiz = quizActive,
                voiceSupported = voiceSupported,
                voiceUses = voiceUses,
                onVoice = onVoice,
                modifier = Modifier.padding(horizontal = 10.dp),
            )

            GameKeyboard(
                value = wordInput,
                language = room.language,
                enabled = !busy && !quizActive,
                submitEnabled = myTurn && wordInput.isNotBlank() && inputMatches != false && !busy && !quizActive,
                onValueChange = onWordInput,
                onSubmit = onSubmit,
                modifier = Modifier.navigationBarsPadding(),
            )
        }

        if (quizActive) {
            val activeRound = requireNotNull(triviaRound)
            BonusDialog(
                round = activeRound,
                question = requireNotNull(triviaQuestion),
                myAnswer = (if (host) activeRound.hostAnswer else activeRound.guestAnswer) ?: triviaSelection,
                opponentAnswer = if (room.isBot) activeRound.botAnswer else if (host) activeRound.guestAnswer else activeRound.hostAnswer,
                myWon = activeRound.winnerSide == if (host) "host" else "guest",
                tied = activeRound.winnerSide == "tie",
                onTrivia = onTrivia,
            )
        }
    }
}

@Composable
private fun CompetitivePlayerCard(
    name: String,
    avatarPath: String?,
    gender: String?,
    rating: Int,
    score: Int,
    active: Boolean,
    accent: Color,
    bot: Boolean,
    modifier: Modifier,
) {
    val league = ratingLeagueProgress(rating).leagueName
    Card(
        modifier = modifier.height(94.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = .07f) else Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (active) 3.dp else 1.dp, if (active) accent else LBorder),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (bot) SyntheticBotPortrait(name, gender ?: botGenderForName(name), 42.dp, 52.dp, accent)
                else ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, 42.dp, 52.dp, accent)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(name, color = LText, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(score.toString(), color = accent, fontSize = duelScoreFontSize(score).sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
            Text("$league • $rating", color = LMuted, fontSize = 12.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun CompetitiveInputBar(
    value: String,
    inputMatches: Boolean?,
    feedbackWord: String?,
    feedbackCorrect: Boolean?,
    myTurn: Boolean,
    busy: Boolean,
    quiz: Boolean,
    voiceSupported: Boolean,
    voiceUses: Int,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedbackVisible = feedbackWord != null && feedbackCorrect != null && value.isBlank()
    val displayedValue = if (feedbackVisible) feedbackWord.orEmpty() else value
    val statusColor = when {
        feedbackVisible && feedbackCorrect == true -> LGreen
        feedbackVisible && feedbackCorrect == false -> LRed
        inputMatches == false -> LRed
        myTurn && !quiz -> LGold
        else -> LMuted
    }
    val borderColor = when {
        feedbackVisible -> statusColor
        inputMatches == false -> LRed
        myTurn && !quiz -> LGold
        else -> LBorder
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (myTurn && !quiz) Color(0xFFFFF8DD) else Color.White,
        border = BorderStroke(if (myTurn && !quiz) 3.dp else 2.dp, borderColor),
        shadowElevation = if (myTurn && !quiz) 8.dp else 0.dp,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onVoice,
                    enabled = myTurn && !busy && !quiz && voiceSupported && voiceUses < 5,
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp),
                ) {
                    Icon(Icons.Rounded.Mic, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${5 - voiceUses}", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    displayedValue,
                    color = if (feedbackVisible) statusColor else LText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (feedbackVisible) {
                    Icon(
                        if (feedbackCorrect == true) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        if (feedbackCorrect == true) sh("Doğru", "Correct") else sh("Yanlış", "Wrong"),
                        Modifier.size(26.dp),
                        tint = statusColor,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (feedbackCorrect == true) sh("DOĞRU", "CORRECT") else sh("YANLIŞ", "WRONG"),
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                } else if (myTurn && !quiz) {
                    Icon(Icons.Rounded.Bolt, sh("Sıra sende", "Your turn"), Modifier.size(24.dp), tint = LGold)
                }
        }
    }
}

@Composable
private fun GameKeyboard(
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
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = when (index) { 1 -> 10.dp; 2 -> 28.dp; else -> 0.dp }),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    row.forEach { key ->
                        KeyButton(key, enabled && value.length < 40, Modifier.weight(1f)) {
                            SonHarfSoundFx.typingClick()
                            onValueChange((value + key).take(40))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyButton(sh("SİL", "DELETE"), enabled && value.isNotEmpty(), Modifier.weight(1f)) { onValueChange(value.dropLast(1)) }
                KeyButton(sh("TEMİZLE", "CLEAR"), enabled && value.isNotEmpty(), Modifier.weight(1.35f)) { onValueChange("") }
                Button(
                    onClick = onSubmit,
                    enabled = submitEnabled,
                    modifier = Modifier.weight(1.7f).height(44.dp),
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Text(sh("GÖNDER", "SEND"), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.height(48.dp).clickable(enabled = enabled, interactionSource = source, indication = null, onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (enabled) LText else LMuted.copy(alpha = .4f),
                fontSize = if (label.length > 5) 14.sp else 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CompetitiveResult(
    room: GameRoomDto,
    me: String?,
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    opponentName: String,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentRating: Int,
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DuelResultProfile(playerName, playerAvatarPath, playerGender, startingRating, LBlue, Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$myRounds : $oppRounds", color = LText, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text("VS", color = LGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    DuelResultProfile(opponentName, opponentAvatarPath, opponentGender, opponentRating, LRed, Modifier.weight(1f))
                }
                if (delta != null) {
                    Text("Rating ${if (delta >= 0) "+" else ""}$delta", color = if (delta >= 0) LGreen else LRed, fontSize = 21.sp, fontWeight = FontWeight.Black)
                } else {
                    Text(sh("Rating sonucu sunucudan doğrulanıyor…", "Confirming rating from server…"), color = LMuted, fontSize = 12.sp)
                }
                if (progress != null) {
                    Text("${progress.leagueName} • $confirmedRating", color = LGold, fontWeight = FontWeight.Black)
                    if (progress.nextAt != null) {
                        Text(
                            sh("${progress.nextLeagueName} ligine ${progress.pointsToNext} puan", "${progress.pointsToNext} points to ${progress.nextLeagueName}"),
                            color = LMuted,
                            fontSize = 12.sp,
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
                                    fontSize = 12.sp,
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
                    Icon(Icons.Rounded.Bolt, null)
                    Spacer(Modifier.width(6.dp))
                    Text(sh("RÖVANŞ", "REMATCH"), fontSize = 17.sp, fontWeight = FontWeight.Black)
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
            Text(label, color = LMuted, fontSize = 12.sp)
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
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0718).copy(alpha = .72f)).padding(18.dp), contentAlignment = Alignment.Center) {
            var value by remember(round.id) { mutableStateOf("") }
            val parsed = value.toLongOrNull()
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 390.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21163F)),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, LPurple.copy(alpha = .7f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(shape = RoundedCornerShape(50), color = LGold.copy(alpha = .14f)) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(20.dp), tint = LGold)
                            Spacer(Modifier.width(7.dp))
                            Text("BİL BAKALIM  +${round.bonusPoints}", color = LGold, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(question.question, color = Color.White, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (round.resolvedAt == null && myAnswer == null) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter(Char::isDigit).take(16) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                            placeholder = { Text(sh("Tahminini yaz", "Enter your estimate"), color = Color.White.copy(alpha = .5f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LGold,
                                unfocusedBorderColor = LPurple,
                                cursorColor = LGold,
                            ),
                        )
                        Button(
                            onClick = { parsed?.let(onTrivia) },
                            enabled = parsed != null,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LPurple, contentColor = Color.White),
                        ) {
                            Text(sh("TAHMİNİ KİLİTLE", "LOCK ESTIMATE"), fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    } else if (round.resolvedAt == null) {
                        CircularProgressIndicator(color = LGold)
                        Text(sh("Tahminin alındı • Rakip bekleniyor", "Estimate received • Waiting for opponent"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    } else {
                        val correct = round.correctAnswer
                        val estimates = listOfNotNull(
                            myAnswer?.let { Triple(sh("SEN", "YOU"), it, correct?.let { answer -> abs(it - answer) }) },
                            opponentAnswer?.let { Triple(sh("RAKİP", "OPPONENT"), it, correct?.let { answer -> abs(it - answer) }) },
                        ).sortedByDescending { it.second }
                        val bestDistance = estimates.mapNotNull { it.third }.minOrNull()
                        estimates.filter { correct == null || it.second >= correct }.forEach { (label, estimate, distance) ->
                            QuizEstimateResult(label, estimate, distance != null && distance == bestDistance, tied)
                        }
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = LGold.copy(alpha = .13f), border = BorderStroke(1.dp, LGold.copy(alpha = .55f))) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sh("DOĞRU CEVAP", "CORRECT ANSWER"), color = LGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Text(correct?.toString() ?: "—", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        estimates.filter { correct != null && it.second < correct }.forEach { (label, estimate, distance) ->
                            QuizEstimateResult(label, estimate, distance != null && distance == bestDistance, tied)
                        }
                        Text(
                            if (tied) sh("AYNI YAKINLIK • BERABERE", "EQUALLY CLOSE • TIE") else if (myWon) sh("EN YAKIN TAHMİN SENİN", "YOU HAD THE CLOSEST ESTIMATE") else sh("RAKİP DAHA YAKINDI", "OPPONENT WAS CLOSER"),
                            color = if (tied) LGold else if (myWon) LGreen else LRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizEstimateResult(label: String, estimate: Long, closest: Boolean, tied: Boolean) {
    val accent = if (closest || tied) LGreen else LRed
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .12f), border = BorderStroke(1.dp, accent.copy(alpha = .55f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (closest || tied) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, Modifier.size(22.dp), tint = accent)
            Spacer(Modifier.width(9.dp))
            Text(label, color = Color.White.copy(alpha = .72f), fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text(estimate.toString(), color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
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
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(38.dp),
            shape = RoundedCornerShape(11.dp),
            color = Color.White,
            border = BorderStroke(1.dp, accent.copy(alpha = .4f)),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, Modifier.size(16.dp), tint = accent)
                Spacer(Modifier.width(4.dp))
                Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
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
            Text(text, color = if (selected) LBlue else LText, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LobbyAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.height(88.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LBorder),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(24.dp), tint = LBlue)
            Text(title, color = LText, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DuelSearchProfile(name: String, avatarPath: String?, gender: String?, accent: Color, loading: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(shape = RoundedCornerShape(20.dp), color = accent.copy(alpha = .15f), border = BorderStroke(2.dp, accent.copy(alpha = .7f))) {
            Box(Modifier.size(74.dp, 92.dp), contentAlignment = Alignment.Center) {
                if (avatarPath != null) ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, 68.dp, 86.dp, accent)
                else if (loading) CircularProgressIndicator(Modifier.size(32.dp), color = accent, strokeWidth = 3.dp)
                else Icon(Icons.Rounded.PersonSearch, null, Modifier.size(36.dp), tint = accent)
            }
        }
        Text(name, color = Color(0xFFF7F8FF), fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun DuelResultProfile(name: String, avatarPath: String?, gender: String?, rating: Int, accent: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, 68.dp, 84.dp, accent)
        Text(name, color = LText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${ratingLeagueProgress(rating).leagueName} • $rating", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun NoticeCard(notice: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp), color = Color.White, border = BorderStroke(1.dp, LBorder)) {
        Text(notice, Modifier.fillMaxWidth().padding(10.dp), color = LMuted, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}
