package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class PracticeBotProfile(val name: String, val gender: String)

private val WordSiegePracticeBots = listOf(
    PracticeBotProfile("Mesut", "erkek"), PracticeBotProfile("İmran", "kadın"),
    PracticeBotProfile("Ayaz", "erkek"), PracticeBotProfile("Eren", "erkek"),
    PracticeBotProfile("Esin", "kadın"), PracticeBotProfile("Can", "erkek"),
    PracticeBotProfile("Deniz", "erkek"), PracticeBotProfile("Mert", "erkek"),
    PracticeBotProfile("Selin", "kadın"), PracticeBotProfile("Burak", "erkek"),
    PracticeBotProfile("Elif", "kadın"), PracticeBotProfile("Kerem", "erkek"),
    PracticeBotProfile("Derya", "kadın"), PracticeBotProfile("Arda", "erkek"),
    PracticeBotProfile("Zeynep", "kadın"), PracticeBotProfile("Emre", "erkek"),
    PracticeBotProfile("Ceren", "kadın"),
)

private val PracticePlayerAccent = Color(0xFF3C8E62)
private val PracticePlayerFill = Color(0xFF8FD6AA)
private val PracticeRivalAccent = Color(0xFFA84642)
private val PracticeRivalFill = Color(0xFFEDA39E)
private val PracticeNeutralFill = Color(0xFFE7E8E1)
private val PracticeSiegeWarm = Color(0xFFE3A64F)

@Composable
internal fun WordSiegePracticeScreen(
    onExit: () -> Unit,
    matchmakingFallback: Boolean = false,
) {
    WordSiegeGameTheme {
        WordSiegePracticeContent(onExit, matchmakingFallback)
    }
}

@Composable
private fun WordSiegePracticeContent(
    onExit: () -> Unit,
    matchmakingFallback: Boolean,
) {
    // Profile enrichment is optional. Core practice/fallback actions never require backend availability.
    val backend = remember { runCatching { OnlineGameBackend() }.getOrNull() }
    val me = remember(backend) { backend?.currentUserId() }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var botProfile by remember { mutableStateOf(WordSiegePracticeBots.random()) }
    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }
    val context = LocalContext.current.applicationContext
    var dictionaryReady by remember { mutableStateOf(SharedDictionaryService.hasSnapshot(state.language)) }
    var dictionaryLoading by remember { mutableStateOf(false) }
    var dictionaryRetryKey by remember { mutableIntStateOf(0) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var notice by remember(matchmakingFallback) {
        mutableStateOf(
            if (matchmakingFallback) {
                sh(
                    "Geçici bot maçı başladı. Gerçek rakip araması arka planda sürüyor.",
                    "Temporary bot match started. Real matchmaking continues in the background.",
                )
            } else null,
        )
    }
    var lastMove by remember { mutableStateOf<WordSiegePracticeMove?>(null) }
    var botThinking by remember { mutableStateOf(false) }
    var botDecisionSalt by remember { mutableStateOf(kotlin.random.Random.nextLong()) }
    var showPass by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showRestart by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var boardViewportMode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }
    var actionVfxEvent by remember { mutableIntStateOf(0) }
    var captureQueue by remember { mutableStateOf<List<WordSiegeCaptureBatch>>(emptyList()) }
    var pendingPlayerCapturePoints by remember { mutableIntStateOf(0) }
    var pendingBotCapturePoints by remember { mutableIntStateOf(0) }
    var pendingPlayerLossPoints by remember { mutableIntStateOf(0) }
    var pendingBotLossPoints by remember { mutableIntStateOf(0) }
    var playerScoreArrivalTick by remember { mutableIntStateOf(0) }
    var botScoreArrivalTick by remember { mutableIntStateOf(0) }
    var playerScoreLossTick by remember { mutableIntStateOf(0) }
    var botScoreLossTick by remember { mutableIntStateOf(0) }
    var playerScoreTargetInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var botScoreTargetInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var showSiegePulse by remember { mutableStateOf(false) }
    var zoneInfoCode by remember { mutableStateOf<String?>(null) }
    var tutorialStep by remember { mutableIntStateOf(-1) }
    var showChat by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf<List<Pair<Boolean, String>>>(emptyList()) }

    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)
    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)
    val displayedPlayerScore = wordSiegeDisplayedScore(
        playerTargetScore, pendingPlayerCapturePoints, pendingPlayerLossPoints,
    )
    val displayedBotScore = wordSiegeDisplayedScore(
        botTargetScore, pendingBotCapturePoints, pendingBotLossPoints,
    )
    val displayedOwner = state.currentOwner
    // Tile selection and board placement must stay responsive even while the dictionary snapshot is warming up.
    // Dictionary readiness is enforced only when the player submits the move.
    val canPlayerAct = state.status == "playing" && state.currentOwner == 1 && !botThinking
    val rackOrder = remember(state.playerRack, shuffleSeed) {
        if (shuffleSeed == 0) state.playerRack.indices.toList()
        else wordSiegeShuffledRackIndices(state.playerRack.length, shuffleSeed)
    }
    val readyFeedback = wordSiegeValidationFeedback(
        placementsCount = placements.size,
        turkish = !SonHarfUiState.isEnglish,
    )
    val previewCapturedCells = placements.keys.count { index -> state.board.getOrNull(index)?.owner != 1 }
    val playerTerritoryPoints = state.playerAreaScore
    val botTerritoryPoints = state.botAreaScore
    val playerMapControl = ((state.playerArea * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val botMapControl = ((state.botArea * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val latestAreaPoints = (lastMove?.capturedCells ?: 0) * WordSiegeFinalRules.CUBE_TRANSFER_POINTS

    LaunchedEffect(me, backend) {
        val b = backend ?: return@LaunchedEffect
        if (me != null) playerProfile = runCatching { b.getProfile(me) }.getOrNull()
    }

    LaunchedEffect(state.language, dictionaryRetryKey) {
        dictionaryLoading = true
        val restored = withContext(Dispatchers.IO) {
            SharedDictionaryService.restorePersisted(context, state.language)
        }
        dictionaryReady = restored
        runCatching { SharedDictionaryService.preloadCanonical(context, state.language) }
            .onSuccess {
                dictionaryReady = true
                if (!restored) {
                    notice = if (matchmakingFallback) {
                        sh(
                            "Sözlük hazır. Bot maçı sürerken gerçek rakip araması devam ediyor.",
                            "Dictionary ready. Real matchmaking continues during the bot match.",
                        )
                    } else {
                        sh(
                            "Sözlük hazır. İlk hamlede merkezden geç.",
                            "Dictionary ready. Your first move must cross the Crown Zone.",
                        )
                    }
                }
            }
            .onFailure {
                dictionaryReady = restored
                if (!restored) {
                    notice = sh(
                        "Sözlük yüklenemedi. Yenile düğmesine basıp tekrar dene.",
                        "Dictionary could not be loaded. Tap refresh and try again.",
                    )
                }
            }
        dictionaryLoading = false
    }

    LaunchedEffect(actionVfxEvent, latestAreaPoints) {
        if (actionVfxEvent > 0 && latestAreaPoints > 0) {
            showSiegePulse = true
            delay(1_300)
            showSiegePulse = false
        }
    }

    fun clearSelection() {
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
    }

    fun enqueueCapture(previous: WordSiegePracticeState, next: WordSiegePracticeState, owner: Int) {
        val batch = wordSiegeCaptureBatch(
            updateKey = "practice:${next.moveCount}:$owner",
            previousOwners = previous.board.map { it.owner },
            currentOwners = next.board.map { it.owner },
            capturingOwner = owner,
        ) ?: return
        if (captureQueue.any { it.updateKey == batch.updateKey }) return
        captureQueue = captureQueue + batch
        if (owner == 1) {
            pendingPlayerCapturePoints += batch.points
            pendingBotLossPoints += batch.opponentLossPoints
        } else {
            pendingBotCapturePoints += batch.points
            pendingPlayerLossPoints += batch.opponentLossPoints
        }
    }

    fun completeTutorial() {
        WordSiegePracticeTutorialPrefs.markCompleted(context)
        tutorialStep = -1
    }

    fun resetMatch(changeOpponent: Boolean) {
        state = WordSiegePracticeEngine.newGame(state.language)
        if (changeOpponent) botProfile = WordSiegePracticeBots.random()
        botDecisionSalt = kotlin.random.Random.nextLong()
        lastMove = null
        shuffleSeed = 0
        boardViewportMode = WordSiegeBoardViewportMode.FIT
        actionVfxEvent = 0
        captureQueue = emptyList()
        pendingPlayerCapturePoints = 0
        pendingBotCapturePoints = 0
        pendingPlayerLossPoints = 0
        pendingBotLossPoints = 0
        playerScoreArrivalTick = 0
        botScoreArrivalTick = 0
        playerScoreLossTick = 0
        botScoreLossTick = 0
        notice = if (matchmakingFallback) {
            sh(
                "Yeni bot maçı başladı. Gerçek rakip araması sürüyor.",
                "New bot match started. Real matchmaking is still running.",
            )
        } else {
            sh(
                "İlk hamle sende. Kelimeni merkezden geçir.",
                "Your first move is yours. Cross the Crown Zone.",
            )
        }
        showChat = false
        chatDraft = ""
        chatMessages = emptyList()
        clearSelection()
    }

    fun startAgain() = resetMatch(changeOpponent = true)
    fun startRematch() = resetMatch(changeOpponent = false)

    fun applyPlayerMove() {
        if (!dictionaryReady) {
            if (!dictionaryLoading) dictionaryRetryKey += 1
            notice = sh(
                "Sözlük hazırlanıyor. Harflerini yerleştirebilirsin; doğrulama hazır olduğunda hamleni onayla.",
                "Dictionary is preparing. You can place tiles now and confirm once validation is ready.",
            )
            return
        }
        runCatching { WordSiegePracticeEngine.applyMove(state, 1, placements) }
            .onSuccess { (next, move) ->
                enqueueCapture(state, next, 1)
                state = next
                lastMove = move
                actionVfxEvent += 1
                val areaPoints = move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS
                notice = sh(
                    "${move.primaryWord} • Kelime +${move.wordScore} • Bölge +$areaPoints",
                    "${move.primaryWord} • Word +${move.wordScore} • Territory +$areaPoints",
                )
                if (tutorialStep == 3) tutorialStep = 4
                clearSelection()
                SonHarfSoundFx.wordAccepted()
            }
            .onFailure {
                notice = wordSiegeFriendlyError(it.message.orEmpty())
                SonHarfSoundFx.warning()
            }
    }

    val activeCapture = captureQueue.firstOrNull()
    val captureEffect = activeCapture?.let { batch ->
        WordSiegeCaptureEffect(
            batch = batch,
            targetInWindow = if (batch.owner == 1) playerScoreTargetInWindow else botScoreTargetInWindow,
            accent = if (batch.owner == 1) PracticePlayerAccent else PracticeRivalAccent,
            onCubeArrived = { index ->
                if (batch.owner == 1) {
                    pendingPlayerCapturePoints =
                        (pendingPlayerCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    playerScoreArrivalTick += 1
                    if (index in batch.opponentIndices) {
                        pendingBotLossPoints =
                            (pendingBotLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)
                        botScoreLossTick += 1
                    }
                } else {
                    pendingBotCapturePoints =
                        (pendingBotCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    botScoreArrivalTick += 1
                    if (index in batch.opponentIndices) {
                        pendingPlayerLossPoints =
                            (pendingPlayerLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)
                        playerScoreLossTick += 1
                    }
                }
            },
            onFinished = {
                captureQueue = captureQueue.filterNot { it.updateKey == batch.updateKey }
            },
        )
    }

    BackHandler(onBack = onExit)

    LaunchedEffect(
        state.currentOwner,
        state.moveCount,
        state.status,
        dictionaryReady,
        playerProfile?.rating,
        playerProfile?.wins,
        playerProfile?.losses,
    ) {
        if (!dictionaryReady || state.status != "playing" || state.currentOwner != 2) return@LaunchedEffect
        botThinking = true
        try {
            delay(950)
            val planned = resilientPracticeBotMove(
                state = state,
                playerRating = playerProfile?.rating ?: 1000,
                playerWins = playerProfile?.wins ?: 0,
                playerLosses = playerProfile?.losses ?: 0,
                decisionSalt = botDecisionSalt,
            )
            if (planned == null) {
                val exchange = practiceBotExchangeIndices(state)
                if (exchange.isNotEmpty()) {
                    state = WordSiegePracticeEngine.exchange(state, 2, exchange)
                    notice = sh(
                        "${botProfile.name} uygun hamle bulamadı; 3 harf değiştirdi. Sıra sende.",
                        "${botProfile.name} found no legal move and exchanged 3 tiles. Your turn.",
                    )
                } else {
                    state = WordSiegePracticeEngine.pass(state, 2)
                    notice = sh(
                        "${botProfile.name} oynayacak hamle bulamadı ve pas verdi. Sıra sende.",
                        "${botProfile.name} found no legal move and passed. Your turn.",
                    )
                }
            } else {
                val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements)
                enqueueCapture(state, next, 2)
                state = next
                lastMove = move
                actionVfxEvent += 1
                val areaPoints = move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS
                notice = sh(
                    "${botProfile.name}: ${move.primaryWord} • Kelime +${move.wordScore} • Bölge +$areaPoints",
                    "${botProfile.name}: ${move.primaryWord} • Word +${move.wordScore} • Territory +$areaPoints",
                )
                SonHarfSoundFx.scoreTick()
            }
        } finally {
            botThinking = false
        }
    }

    Surface(Modifier.fillMaxSize(), color = WordSiegeGameUi.Background) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            val compact = maxHeight < 700.dp || maxWidth < 600.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
            ) {
                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(if (compact) 40.dp else 46.dp)) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = WordSiegeGameUi.Text)
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            sh("KELİME TAHTI", "WORD THRONE"),
                            color = WordSiegeGameUi.Text,
                            fontSize = if (compact) 16.sp else 18.sp,
                            lineHeight = if (compact) 18.sp else 21.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                        Text(
                            when {
                                matchmakingFallback && dictionaryReady -> sh("BOT MAÇI • GERÇEK RAKİP ARANIYOR", "BOT MATCH • FINDING REAL RIVAL")
                                matchmakingFallback -> sh("BOT MAÇI", "BOT MATCH")
                                dictionaryLoading -> sh("ALIŞTIRMA • HAZIRLANIYOR", "PRACTICE • PREPARING")
                                else -> sh("ALIŞTIRMA", "PRACTICE")
                            },
                            color = PracticePlayerAccent,
                            fontSize = if (compact) 8.sp else 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                    if (!matchmakingFallback) {
                        IconButton(
                            onClick = { tutorialStep = 0 },
                            modifier = Modifier.size(if (compact) 38.dp else 44.dp),
                        ) {
                            Icon(Icons.Rounded.HelpOutline, sh("Nasıl oynanır?", "How to play?"), tint = PracticePlayerAccent)
                        }
                    }
                    IconButton(onClick = { showForfeit = true }, enabled = state.status == "playing", modifier = Modifier.size(if (compact) 40.dp else 46.dp)) {
                        Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = WordSiegeGameUi.Red)
                    }
                    IconButton(
                        onClick = {
                            if (!dictionaryReady) dictionaryRetryKey += 1
                            else if (state.moveCount > 0 || placements.isNotEmpty()) showRestart = true else startAgain()
                        },
                        modifier = Modifier.size(if (compact) 40.dp else 46.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, sh("Yeni oyun", "New game"), tint = WordSiegeGameUi.Blue)
                    }
                }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    WordSiegePracticeScoreCard(
                        name = playerProfile?.displayName ?: sh("SEN", "YOU"),
                        score = displayedPlayerScore,
                        wordPoints = state.playerWordScore,
                        territoryPoints = playerTerritoryPoints,
                        area = state.playerArea,
                        accent = PracticePlayerAccent,
                        active = displayedOwner == 1,
                        leading = playerTargetScore > botTargetScore,
                        compact = compact,
                        avatarPath = playerProfile?.avatarPath,
                        gender = playerProfile?.gender,
                        avatarVisible = playerProfile?.avatarVisibility != "hidden",
                        isBot = false,
                        modifier = Modifier.weight(1f),
                        scoreArrivalTick = playerScoreArrivalTick,
                        scoreLossTick = playerScoreLossTick,
                        onScoreCenterChanged = { playerScoreTargetInWindow = it },
                    )
                    WordSiegePracticeScoreCard(
                        name = botProfile.name,
                        score = displayedBotScore,
                        wordPoints = state.botWordScore,
                        territoryPoints = botTerritoryPoints,
                        area = state.botArea,
                        accent = PracticeRivalAccent,
                        active = displayedOwner == 2,
                        leading = botTargetScore > playerTargetScore,
                        compact = compact,
                        avatarPath = null,
                        gender = botProfile.gender,
                        avatarVisible = true,
                        isBot = true,
                        modifier = Modifier.weight(1f),
                        scoreArrivalTick = botScoreArrivalTick,
                        scoreLossTick = botScoreLossTick,
                        onScoreCenterChanged = { botScoreTargetInWindow = it },
                    )
                }




                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    WordSiegePracticeBoard(
                        board = state.board,
                        rack = state.playerRack,
                        placements = placements,
                        myOwner = 1,
                        enabled = canPlayerAct,
                        moveEventKey = actionVfxEvent.takeIf { it > 0 },
                        resolvedIndices = lastMove?.placements?.keys ?: emptySet(),
                        captureEffect = captureEffect,
                        modifier = if (boardViewportMode == WordSiegeBoardViewportMode.CLOSE) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(1f),
                        onViewportModeChange = { boardViewportMode = it },
                        onCell = { boardIndex ->
                            if (!canPlayerAct) return@WordSiegePracticeBoard
                            if (placements.containsKey(boardIndex)) {
                                selectedRackIndex = placements.getValue(boardIndex)
                                placements = placements - boardIndex
                            } else if (state.board[boardIndex].letter == null) {
                                val rackIndex = selectedRackIndex ?: return@WordSiegePracticeBoard
                                if (rackIndex !in placements.values) {
                                    placements = placements + (boardIndex to rackIndex)
                                    selectedRackIndex = null
                                    if (tutorialStep == 2) tutorialStep = 3
                                }
                            }
                        },
                    )

                    if (tutorialStep >= 0 && !matchmakingFallback) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        ) {
                            WordSiegePracticeTutorialCard(
                                step = tutorialStep,
                                compact = compact,
                                onStart = { tutorialStep = 1 },
                                onFinish = ::completeTutorial,
                                onSkip = ::completeTutorial,
                            )
                        }
                    }
                }

                if (state.status == "playing") {
                    if (boardViewportMode == WordSiegeBoardViewportMode.FIT) Row(
                        Modifier.fillMaxWidth().height(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (placements.isNotEmpty()) {
                            Text(
                                readyFeedback.message,
                                color = PracticePlayerAccent,
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                sh(
                                    "Bölge +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                                    "Territory +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                                ),
                                color = WordSiegeGameUi.Muted,
                                fontSize = 8.sp,
                                lineHeight = 12.sp,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        rackOrder.forEach { rackIndex ->
                            val letter = state.playerRack.getOrNull(rackIndex) ?: return@forEach
                            WordSiegePracticeRackTile(
                                letter = letter,
                                selected = selectedRackIndex == rackIndex,
                                used = rackIndex in placements.values,
                                enabled = canPlayerAct,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val pending = placements.entries.firstOrNull { it.value == rackIndex }?.key
                                    if (pending != null) placements = placements - pending
                                    val selecting = selectedRackIndex != rackIndex
                                    selectedRackIndex = if (selecting) rackIndex else null
                                    if (selecting && tutorialStep == 1) tutorialStep = 2
                                },
                            )
                        }
                        repeat((7 - state.playerRack.length).coerceAtLeast(0)) {
                            Spacer(Modifier.weight(1f).height(if (compact) 40.dp else 44.dp))
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        WordSiegeCompactAction(sh("GERİ AL", "UNDO"), Icons.Rounded.Undo,
                            canPlayerAct && placements.isNotEmpty(), Modifier.weight(1f)) {
                            placements.keys.lastOrNull()?.let { boardIndex ->
                                selectedRackIndex = placements[boardIndex]
                                placements = wordSiegeUndoPendingPlacement(placements, boardIndex)
                            }
                        }
                        WordSiegeCompactAction(sh("KARIŞTIR", "SHUFFLE"), Icons.Rounded.Shuffle,
                            canPlayerAct && state.playerRack.length > 1, Modifier.weight(1f)) {
                            shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1
                        }
                        WordSiegeCompactAction(sh("PAS", "PASS"), Icons.Rounded.SkipNext,
                            canPlayerAct, Modifier.weight(1f)) { showPass = true }
                        WordSiegeCompactAction(sh("DEĞİŞTİR", "EXCHANGE"), Icons.Rounded.SwapHoriz,
                            canPlayerAct && state.bag.isNotEmpty(), Modifier.weight(1f)) {
                            exchangeSelection = emptySet(); showExchange = true
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WordSiegeSideAction(
                            sh("SOHBET", "CHAT"),
                            Icons.Rounded.Chat,
                            modifier = Modifier.width(74.dp),
                        ) { showChat = true }
                        Button(
                            onClick = ::applyPlayerMove,
                            shape = RoundedCornerShape(10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PracticePlayerAccent,
                                contentColor = Color.White,
                                disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                                disabledContentColor = WordSiegeGameUi.DisabledContent,
                            ),
                            contentPadding = PaddingValues(horizontal = 3.dp),
                        ) {
                            Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                        WordSiegePracticeBagButton(
                            bag = state.bag,
                            modifier = Modifier.width(82.dp),
                        )
                    }
                } else {
                    val won = state.winnerOwner == 1
                    val draw = state.winnerOwner == null
                    val color = when { won -> PracticePlayerAccent; draw -> WordSiegeGameUi.Gold; else -> PracticeRivalAccent }
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = color.copy(alpha = .08f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = .35f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (won) sh("TAHT SENİN!", "THE THRONE IS YOURS!")
                                    else if (draw) sh("BERABERE", "DRAW")
                                    else sh("${botProfile.name.uppercase()} KAZANDI", "${botProfile.name.uppercase()} WON"),
                                    color = color,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    sh("Kelime ve bölge puanların ayrı hesaplandı.", "Word and territory points were scored separately."),
                                    color = WordSiegeGameUi.Muted,
                                    fontSize = 8.sp,
                                )
                            }
                            TextButton(onClick = ::startAgain) {
                                Text(sh("YENİ OYUN", "NEW GAME"), color = WordSiegeGameUi.Blue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                        }
                    }
                }

                val statusMessage = notice ?: lastMove?.let { move ->
                    sh(
                        "Son: ${move.formedWords.joinToString(" + ")} • Kelime +${move.wordScore} • Bölge +${move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                        "Last: ${move.formedWords.joinToString(" + ")} • Word +${move.wordScore} • Territory +${move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                    )
                } ?: sh(
                    "Harf seç → boş hücreye yerleştir → kelimeyi tamamla → HAMLEYİ ONAYLA",
                    "Pick a tile → place it → complete a word → CONFIRM MOVE",
                )
                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegePracticeStatusBar(statusMessage, compact)
                WordSiegeTurnStrip(
                    text = when {
                        state.status == "finished" && matchmakingFallback -> sh("BOT MAÇI BİTTİ • RAKİP ARAMASI SÜRÜYOR", "BOT MATCH FINISHED • MATCHMAKING CONTINUES")
                        state.status == "finished" -> sh("ALIŞTIRMA BİTTİ", "PRACTICE FINISHED")
                        botThinking -> sh("${botProfile.name.uppercase()} HAMLESİNİ HAZIRLIYOR", "${botProfile.name.uppercase()} IS PREPARING A MOVE")
                        displayedOwner == 1 -> sh("SIRA SENDE • Kelimeni oluştur", "YOUR TURN • Build your word")
                        else -> sh("${botProfile.name.uppercase()} OYNUYOR", "${botProfile.name.uppercase()} IS PLAYING")
                    },
                    playerTurn = displayedOwner == 1 && !botThinking,
                    playerAccent = PracticePlayerAccent,
                    rivalAccent = PracticeRivalAccent,
                )
            }
        }
    }

    if (showRestart) {
        AlertDialog(
            onDismissRequest = { showRestart = false },
            title = { Text(sh("Yeni oyun başlat?", "Start a new game?"), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    if (matchmakingFallback) {
                        sh("Bot maçı sıfırlanacak. Gerçek rakip araması devam edecek.", "The bot match will reset. Real matchmaking will continue.")
                    } else {
                        sh("Mevcut alıştırmadaki ilerleme sıfırlanacak.", "Current practice progress will be reset.")
                    },
                    color = WordSiegeGameUi.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { showRestart = false; startAgain() }) {
                    Text(sh("YENİ OYUN", "NEW GAME"), color = WordSiegeGameUi.Blue, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showRestart = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showPass) {
        AlertDialog(
            onDismissRequest = { showPass = false },
            title = { Text(sh("Turu geç?", "Pass this turn?"), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    sh("Torbada 20’den az harf varken art arda 4 pas ve/veya değişim maçı bitirir.", "When fewer than 20 tiles remain, 4 consecutive passes and/or exchanges end the match."),
                    color = WordSiegeGameUi.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPass = false
                    state = WordSiegePracticeEngine.pass(state, 1)
                    clearSelection()
                }) {
                    Text(sh("PAS VER", "PASS"), color = WordSiegeGameUi.Gold, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showPass = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showForfeit) {
        AlertDialog(
            onDismissRequest = { showForfeit = false },
            title = { Text(sh("Pes etmek istiyor musun?", "Do you want to forfeit?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Bu maçı rakibin kazanır.", "Your rival wins this match."), color = WordSiegeGameUi.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showForfeit = false
                    state = WordSiegePracticeEngine.forfeit(state, 1)
                    clearSelection()
                }) {
                    Text(sh("PES ET", "FORFEIT"), color = WordSiegeGameUi.Red, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showForfeit = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showExchange) {
        AlertDialog(
            onDismissRequest = { showExchange = false },
            title = { Text(sh("HARF DEĞİŞTİR", "EXCHANGE TILES"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        sh("Seçtiğin harfler torbaya döner ve turun biter.", "Selected tiles return to the bag and your turn ends."),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 12.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.playerRack.forEachIndexed { index, letter ->
                            WordSiegePracticeRackTile(
                                letter,
                                index in exchangeSelection,
                                false,
                                true,
                                Modifier.weight(1f),
                            ) {
                                exchangeSelection = if (index in exchangeSelection) exchangeSelection - index else exchangeSelection + index
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = exchangeSelection.isNotEmpty() && exchangeSelection.size <= state.bag.length,
                    onClick = {
                        state = WordSiegePracticeEngine.exchange(state, 1, exchangeSelection)
                        showExchange = false
                        clearSelection()
                    },
                ) {
                    Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showExchange = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showChat) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text(sh("SOHBET • ${botProfile.name}", "CHAT • ${botProfile.name}"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chatListState = rememberLazyListState()
                    LaunchedEffect(chatMessages.size) {
                        if (chatMessages.isNotEmpty()) chatListState.animateScrollToItem(chatMessages.lastIndex)
                    }
                    if (chatMessages.isEmpty()) {
                        Text(
                            sh("Botla kısa mesajlaşabilirsin.", "You can exchange short messages with the bot."),
                            color = WordSiegeGameUi.Muted,
                            fontSize = 12.sp,
                        )
                    } else {
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp, max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(chatMessages) { _, item ->
                                val (mine, message) = item
                                Box(Modifier.fillMaxWidth()) {
                                    Surface(
                                        modifier = Modifier.align(if (mine) Alignment.CenterEnd else Alignment.CenterStart),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (mine) PracticePlayerAccent.copy(alpha = .13f) else PracticeRivalAccent.copy(alpha = .10f),
                                    ) {
                                        Text(
                                            (if (mine) sh("Sen: ", "You: ") else "${botProfile.name}: ") + message,
                                            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            color = WordSiegeGameUi.Text,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(sh("Mesaj", "Message")) },
                        trailingIcon = {
                            IconButton(
                                enabled = chatDraft.isNotBlank(),
                                onClick = {
                                    val message = chatDraft.trim()
                                    if (message.isNotEmpty()) {
                                        chatMessages = chatMessages + (true to message) +
                                            (false to sh("İyi oyunlar!", "Good game!"))
                                        chatDraft = ""
                                    }
                                },
                            ) { Icon(Icons.Rounded.Send, sh("Gönder", "Send")) }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChat = false }) {
                    Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black)
                }
            },
        )
    }

    if (state.status == "finished") {
        WordSiegePracticeResultDialog(
            winnerOwner = state.winnerOwner,
            opponentName = botProfile.name,
            playerScore = WordSiegePracticeEngine.totalScore(state, 1),
            botScore = WordSiegePracticeEngine.totalScore(state, 2),
            onRematch = ::startRematch,
            onExit = onExit,
        )
    }

    zoneInfoCode?.let { code ->
        WordSiegePracticeZoneInfoDialog(
            code = code,
            onDismiss = { zoneInfoCode = null },
        )
    }
}

@Composable
private fun WordSiegePracticeResultDialog(
    winnerOwner: Int?,
    opponentName: String,
    playerScore: Int,
    botScore: Int,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else .84f,
        animationSpec = tween(420),
        label = "practiceResultScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(300),
        label = "practiceResultAlpha",
    )
    val won = winnerOwner == 1
    val draw = winnerOwner == null
    val accent = when {
        won -> PracticePlayerAccent
        draw -> WordSiegeGameUi.Gold
        else -> PracticeRivalAccent
    }
    val title = when {
        won -> sh("KAZANDIN", "YOU WON")
        draw -> sh("BERABERE", "DRAW")
        else -> sh("KAYBETTİN", "YOU LOST")
    }

    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        icon = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = accent.copy(alpha = .14f),
                border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
            ) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp).size(32.dp),
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "$playerScore  —  $botScore",
                    color = WordSiegeGameUi.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    if (won) sh("$opponentName karşısında tahtı aldın.", "You took the throne against $opponentName.")
                    else if (draw) sh("Puanlar eşitlendi.", "The scores are tied.")
                    else sh("$opponentName bu maçı aldı.", "$opponentName won this match."),
                    color = WordSiegeGameUi.Muted,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(sh("RÖVANŞ?", "REMATCH?"), color = WordSiegeGameUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(sh("Aynı rakiple hemen tekrar oyna.", "Play the same opponent again now."), color = WordSiegeGameUi.Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onRematch,
                colors = ButtonDefaults.buttonColors(containerColor = PracticePlayerAccent, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
            ) { Text(sh("EVET", "YES"), fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            OutlinedButton(onClick = onExit, shape = RoundedCornerShape(10.dp)) {
                Text(sh("HAYIR", "NO"), fontWeight = FontWeight.Black)
            }
        },
    )
}

@Composable
private fun WordSiegePracticeScoreCard(
    name: String,
    score: Int,
    wordPoints: Int,
    territoryPoints: Int,
    area: Int,
    accent: Color,
    active: Boolean,
    leading: Boolean,
    compact: Boolean,
    avatarPath: String?,
    gender: String?,
    avatarVisible: Boolean,
    isBot: Boolean,
    modifier: Modifier = Modifier,
    scoreArrivalTick: Int = 0,
    scoreLossTick: Int = 0,
    onScoreCenterChanged: (Offset) -> Unit = {},
) {
    WordSiegeScoreCard(
        name = name, score = score, wordPoints = wordPoints,
        territoryPoints = territoryPoints, area = area, accent = accent,
        active = active, leading = leading, avatarPath = avatarPath,
        gender = gender, avatarVisible = avatarVisible, isBot = isBot,
        modifier = modifier,
        scoreArrivalTick = scoreArrivalTick,
        scoreLossTick = scoreLossTick,
        onScoreCenterChanged = onScoreCenterChanged,
    )
}
