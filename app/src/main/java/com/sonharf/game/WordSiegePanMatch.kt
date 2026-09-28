package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.WordSiegeCellDto
import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeMoveDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private val PanSiegeTile get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.ivory else Color(0xFFF7E3A6)
private val PanSiegeTileBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bevel else Color(0xFFC9A560)
private val PanSiegeBoardSurface = Color(0xFFEFE7D2)
private val PanSiegeFrameNavy get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frame else Color(0xFF102C4C)
private val PanSiegeFrameEdge get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frameEdge else Color(0xFF9EC7D8)
private val PanSiegeFrameInner get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frameShade else Color(0xFFD7E7ED)
private val PanSiegeNeutral get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.empty else Color(0xFFF3EEDF)
private val PanSiegeMine get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.mine else Color(0xFF5FAF73)
private val PanSiegeRival get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.rival else Color(0xFFD9776F)
private val PanSiegeNeutralBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.emptyEdge else Color(0xFFDCD3BD)
private val PanSiegeBonusBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonusBorder else Color(0xFFC9BFA5)
private val PanSiegeMineBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.mine else Color(0xFF7FC391)
private val PanSiegeRivalBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.rival else Color(0xFFEB9E97)
private val PanSiegeBonus2H get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus2H else Color(0xFFCFE6F5)
private val PanSiegeBonus3H get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus3H else Color(0xFFF6D3E2)
private val PanSiegeBonus2K get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus2K else Color(0xFFD6ECCB)
private val PanSiegeBonus3K get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus3K else Color(0xFFF8DCC3)
private val PanSiegeBonus4K get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus4K else Color(0xFFE2D6F2)
private val PanSiegeBonusStar get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonusStar else Color(0xFFFBEBB5)
private val PanSiegeLastMove = Color(0xFFE0A82E)
private val PanSiegeBonusLabel get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonusLabel else Color(0xFF3F4A5A)
private val PanSiegeCellSize = 52.dp
internal const val WORD_SIEGE_BOT_FALLBACK_DELAY_MS = 15_000L

@Composable
internal fun WordSiegePanMatch(
    game: WordSiegeGameDto,
    me: String?,
    profiles: Map<String, ProfileDto>,
    moves: List<WordSiegeMoveDto>,
    placements: Map<Int, Int>,
    selectedRackIndex: Int?,
    busy: Boolean,
    notice: String?,
    onBack: () -> Unit,
    onBoardCell: (Int) -> Unit,
    onRackTile: (Int) -> Unit,
    onSubmit: () -> Unit,
    onPass: () -> Unit,
    onExchange: () -> Unit,
    onChat: () -> Unit,
    onForfeit: () -> Unit,
    onCancelWaiting: () -> Unit,
    /** Finger-dragged tiles replace the pending placements (Kelimelik-style). */
    onPlacementsChange: (Map<Int, Int>) -> Unit = {},
) {
    val mine = me?.let(profiles::get)
    val opponentId = if (me == game.playerOneId) game.playerTwoId else game.playerOneId
    val opponent = opponentId?.let(profiles::get)
    val myOwner = if (me == game.playerOneId) 1 else 2
    val rivalOwner = if (myOwner == 1) 2 else 1
    val myTurn = game.status == "playing" && game.currentPlayerId == me
    val rack = if (me == game.playerOneId) game.playerOneRack else game.playerTwoRack.orEmpty()
    val canAct = myTurn && !busy
    val lastMove = moves.lastOrNull()
    val myAreaCount = panSiegeAreaCount(game, myOwner)
    val rivalAreaCount = panSiegeAreaCount(game, rivalOwner)
    val myWordPoints = panSiegeWordScore(game, myOwner)
    val rivalWordPoints = panSiegeWordScore(game, rivalOwner)
    val myTerritoryPoints = if (myOwner == 1) game.playerOneAreaScore else game.playerTwoAreaScore
    val rivalTerritoryPoints = if (rivalOwner == 1) game.playerOneAreaScore else game.playerTwoAreaScore
    val myTargetScore = WordSiegeFinalRules.scoreWithTerritoryLedger(myWordPoints, myTerritoryPoints)
    val rivalTargetScore = WordSiegeFinalRules.scoreWithTerritoryLedger(rivalWordPoints, rivalTerritoryPoints)
    val boardOwners = game.board.map { it.owner }
    val captureTracker = remember(game.id) {
        WordSiegeCaptureTracker(
            initialUpdateKey = lastMove?.id?.toString(),
            initialOwners = boardOwners,
        )
    }
    var captureQueue by remember(game.id) { mutableStateOf<List<WordSiegeCaptureBatch>>(emptyList()) }
    var pendingMyCapturePoints by remember(game.id) { mutableIntStateOf(0) }
    var pendingRivalCapturePoints by remember(game.id) { mutableIntStateOf(0) }
    var pendingMyLossPoints by remember(game.id) { mutableIntStateOf(0) }
    var pendingRivalLossPoints by remember(game.id) { mutableIntStateOf(0) }
    var myScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }
    var rivalScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }
    var myScoreLossTick by remember(game.id) { mutableIntStateOf(0) }
    var rivalScoreLossTick by remember(game.id) { mutableIntStateOf(0) }
    var myScoreTargetInWindow by remember(game.id) { mutableStateOf(Offset.Unspecified) }
    var rivalScoreTargetInWindow by remember(game.id) { mutableStateOf(Offset.Unspecified) }

    val moveOwner = when (lastMove?.playerId) {
        game.playerOneId -> 1
        game.playerTwoId -> 2
        else -> null
    }
    val captureCandidate = captureTracker.preview(
        updateKey = lastMove?.id?.toString(),
        currentOwners = boardOwners,
        capturingOwner = moveOwner,
        expectedCaptured = lastMove?.capturedCells ?: 0,
    )
    val candidateAlreadyQueued = captureCandidate?.let { candidate ->
        captureQueue.any { it.updateKey == candidate.updateKey }
    } ?: false
    val candidateMyPoints =
        if (!candidateAlreadyQueued && captureCandidate?.owner == myOwner) captureCandidate.points else 0
    val candidateRivalPoints =
        if (!candidateAlreadyQueued && captureCandidate?.owner == rivalOwner) captureCandidate.points else 0
    val candidateMyLoss =
        if (!candidateAlreadyQueued && captureCandidate?.owner == rivalOwner) captureCandidate.opponentLossPoints else 0
    val candidateRivalLoss =
        if (!candidateAlreadyQueued && captureCandidate?.owner == myOwner) captureCandidate.opponentLossPoints else 0
    val displayedMyScore = wordSiegeDisplayedScore(
        myTargetScore, pendingMyCapturePoints + candidateMyPoints, pendingMyLossPoints + candidateMyLoss,
    )
    val displayedRivalScore = wordSiegeDisplayedScore(
        rivalTargetScore, pendingRivalCapturePoints + candidateRivalPoints, pendingRivalLossPoints + candidateRivalLoss,
    )

    LaunchedEffect(lastMove?.id, boardOwners) {
        val move = lastMove ?: return@LaunchedEffect
        val owner = when (move.playerId) {
            game.playerOneId -> 1
            game.playerTwoId -> 2
            else -> return@LaunchedEffect
        }
        val batch = captureTracker.preview(
            updateKey = move.id.toString(),
            currentOwners = boardOwners,
            capturingOwner = owner,
            expectedCaptured = move.capturedCells,
        ) ?: return@LaunchedEffect
        captureTracker.consume(batch, boardOwners)
        if (captureQueue.none { it.updateKey == batch.updateKey }) {
            captureQueue = captureQueue + batch
            if (batch.owner == myOwner) {
                pendingMyCapturePoints += batch.points
                pendingRivalLossPoints += batch.opponentLossPoints
            } else if (batch.owner == rivalOwner) {
                pendingRivalCapturePoints += batch.points
                pendingMyLossPoints += batch.opponentLossPoints
            }
        }
    }

    val activeCapture = captureQueue.firstOrNull()
    val captureEffect = activeCapture?.let { batch ->
        WordSiegeCaptureEffect(
            batch = batch,
            targetInWindow = if (batch.owner == myOwner) myScoreTargetInWindow else rivalScoreTargetInWindow,
            accent = if (batch.owner == myOwner) PanSiegeMineBorder else PanSiegeRivalBorder,
            onCubeArrived = { index ->
                if (batch.owner == myOwner) {
                    pendingMyCapturePoints =
                        (pendingMyCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    myScoreArrivalTick += 1
                    if (index in batch.opponentIndices) {
                        pendingRivalLossPoints =
                            (pendingRivalLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)
                        rivalScoreLossTick += 1
                    }
                } else {
                    pendingRivalCapturePoints =
                        (pendingRivalCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    rivalScoreArrivalTick += 1
                    if (index in batch.opponentIndices) {
                        pendingMyLossPoints =
                            (pendingMyLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)
                        myScoreLossTick += 1
                    }
                }
            },
            onFinished = {
                captureQueue = captureQueue.filterNot { it.updateKey == batch.updateKey }
            },
        )
    }
    val myMapControl = ((myAreaCount * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val rivalMapControl = ((rivalAreaCount * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val displayedCurrentPlayerId = game.currentPlayerId
    var fallbackPracticeActive by remember(game.id) { mutableStateOf(false) }
    var shuffleSeed by remember(game.id) { mutableIntStateOf(0) }
    var boardViewportMode by remember(game.id) { mutableStateOf(WordSiegeBoardViewportMode.FIT) }
    val visualMyTurn = game.status == "playing" && displayedCurrentPlayerId == me
    val rackOrder = remember(rack, shuffleSeed) {
        if (shuffleSeed == 0) rack.indices.toList() else wordSiegeShuffledRackIndices(rack.length, shuffleSeed)
    }
    val readyFeedback = wordSiegeValidationFeedback(
        placementsCount = placements.size,
        turkish = !SonHarfUiState.isEnglish,
    )
    val tileDrag = remember(game.id) { WordSiegeTileDrag() }
    // Green check on a valid word; PRO also sees its points above the word.
    var pendingScore by remember(game.id) { mutableStateOf<Int?>(null) }
    var pendingFor by remember(game.id) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    LaunchedEffect(placements, game.board, rack) {
        pendingScore = null
        if (placements.isEmpty()) return@LaunchedEffect
        val tiles = placements
        delay(90L)
        pendingScore = withContext(Dispatchers.Default) {
            runCatching { WordSiegePracticeEngine.previewValidScore(game.board, rack, tiles, game.language) }.getOrNull()
        }
        pendingFor = tiles
    }
    val pendingValid = pendingScore != null && pendingFor == placements
    fun dropTile(rackIndex: Int, fromCell: Int?, target: Int?) {
        if (!canAct) return
        onPlacementsChange(wordSiegeDropTile(placements, game.board, rackIndex, fromCell, target))
    }

    LaunchedEffect(game.id, game.status) {
        if (game.status == "waiting") {
            delay(WORD_SIEGE_BOT_FALLBACK_DELAY_MS)
            fallbackPracticeActive = true
        } else {
            fallbackPracticeActive = false
        }
    }

    if (fallbackPracticeActive && game.status == "waiting") {
        WordSiegePracticeScreen(
            onExit = { fallbackPracticeActive = false },
            matchmakingFallback = true,
        )
        return
    }

    WordSiegeGameTheme {
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(WordSiegeGameUi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, sh("Oyunlar", "Games"), tint = WordSiegeGameUi.Text)
            }
            Column(Modifier.weight(1f)) {
                Text(sh("KELİME TAHTI", "WORD THRONE"), color = WordSiegeGameUi.Text, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    if (game.status == "playing") {
                        if (visualMyTurn) sh("SIRA SENDE", "YOUR TURN") else sh("RAKİPTE", "RIVAL'S TURN")
                    } else panSiegeStatusLabel(game, me),
                    color = if (visualMyTurn) PanSiegeMineBorder else PanSiegeRivalBorder,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            IconButton(onClick = onForfeit, enabled = game.status == "playing" && !busy, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = WordSiegeGameUi.Red)
            }
        }

        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PanSiegePlayerCard(
                profile = mine,
                fallbackName = sh("Sen", "You"),
                score = displayedMyScore,
                wordPoints = myWordPoints,
                territoryPoints = myTerritoryPoints,
                areaCount = myAreaCount,
                accent = PanSiegeMineBorder,
                active = displayedCurrentPlayerId == me,
                leading = myTargetScore > rivalTargetScore,
                modifier = Modifier.weight(1f),
                scoreArrivalTick = myScoreArrivalTick,
                scoreLossTick = myScoreLossTick,
                onScoreCenterChanged = { myScoreTargetInWindow = it },
            )
            PanSiegePlayerCard(
                profile = opponent,
                fallbackName = if (game.status == "waiting") sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"),
                score = displayedRivalScore,
                wordPoints = rivalWordPoints,
                territoryPoints = rivalTerritoryPoints,
                areaCount = rivalAreaCount,
                accent = PanSiegeRivalBorder,
                active = displayedCurrentPlayerId == opponentId,
                leading = rivalTargetScore > myTargetScore,
                modifier = Modifier.weight(1f),
                scoreArrivalTick = rivalScoreArrivalTick,
                scoreLossTick = rivalScoreLossTick,
                onScoreCenterChanged = { rivalScoreTargetInWindow = it },
                mascot = rememberRivalMascot(opponent?.id),
            )
        }


        if (game.status == "waiting") {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = WordSiegeGameUi.Surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, WordSiegeGameUi.Border),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = SiegePurple)
                    Spacer(Modifier.height(12.dp))
                    Text(sh("RAKİP ARANIYOR", "FINDING A RIVAL"), color = WordSiegeGameUi.Text, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        sh(
                            "15 saniye içinde rakip bulunmazsa geçici AI maçı hemen başlayacak. Gerçek rakip araması arka planda sürecek.",
                            "If no rival is found within 15 seconds, a temporary AI match starts immediately while real matchmaking continues in the background.",
                        ),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onCancelWaiting, enabled = !busy, border = BorderStroke(1.dp, WordSiegeGameUi.Red)) {
                        Text(sh("ARAMAYI İPTAL ET", "CANCEL SEARCH"), color = WordSiegeGameUi.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
            notice?.let { PanSiegeNotice(it) }
            return@Column
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        PanSiegeBoard(
            gameId = game.id,
            board = game.board,
            rack = rack,
            placements = placements,
            myOwner = myOwner,
            enabled = canAct,
            playerTurn = myTurn,
            lastMoveMine = lastMove?.playerId == me,
            lastMove = lastMove,
            captureEffect = captureEffect,
            viewportMode = boardViewportMode,
            onViewportModeChange = { boardViewportMode = it },
            modifier = Modifier.fillMaxSize(),
            mascotSignal = lastMove?.let { move ->
                val mineMove = move.playerId == me
                when {
                    mineMove && move.primaryWord.length >= 8 ->
                        WordSiegeMascotSignal("rare:${move.id}", WordSiegeMascotEvent.RARE_WORD, word = move.primaryWord)
                    mineMove && (move.totalScore >= 25 || move.capturedCells >= 3 || move.opponentCaptured > 0) ->
                        WordSiegeMascotSignal("big:${move.id}", WordSiegeMascotEvent.BIG_PRAISE, word = move.primaryWord)
                    mineMove -> WordSiegeMascotSignal("ok:${move.id}", WordSiegeMascotEvent.PRAISE, word = move.primaryWord)
                    move.totalScore >= 25 || move.opponentCaptured > 0 ->
                        WordSiegeMascotSignal("rival:${move.id}", WordSiegeMascotEvent.RIVAL_STRONG)
                    rivalTargetScore - myTargetScore >= 40 ->
                        WordSiegeMascotSignal("behind:${move.id}", WordSiegeMascotEvent.BEHIND)
                    else -> null
                }
            },
            mascotOutcome = if (game.status == "finished") {
                when (game.winnerId) {
                    null -> WordSiegeMascotOutcome.DRAW
                    me -> WordSiegeMascotOutcome.WIN
                    else -> WordSiegeMascotOutcome.LOSS
                }
            } else {
                null
            },
            playerName = mine?.displayName,
            playerGender = mine?.gender,
            tileDrag = tileDrag,
            onTileDrop = ::dropTile,
            pendingValid = pendingValid,
            pendingScore = pendingScore.takeIf { mine?.isVip == true },
            onCell = onBoardCell,
            onChat = onChat,
        )
        }

        if (game.status == "playing") {
            if (boardViewportMode == WordSiegeBoardViewportMode.FIT) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (placements.isNotEmpty()) {
                        Text(readyFeedback.message, color = PanSiegeMineBorder, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    } else Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }

                WordSiegePremiumPanel(
                    game = game,
                    placements = placements,
                    canAct = canAct,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rackOrder.forEach { rackIndex ->
                    val letter = rack.getOrNull(rackIndex) ?: return@forEach
                    PanSiegeRackTile(
                        letter = letter,
                        selected = selectedRackIndex == rackIndex,
                        used = rackIndex in placements.values || tileDrag.rackIndex == rackIndex,
                        enabled = canAct,
                        modifier = Modifier.weight(1f).wordSiegeTileDragSource(
                            drag = tileDrag,
                            enabled = canAct && rackIndex !in placements.values,
                            rackIndex = rackIndex,
                            fromCell = null,
                            letter = letter,
                            onDrop = ::dropTile,
                        ),
                        onClick = { onRackTile(rackIndex) },
                    )
                }
                repeat((7 - rack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).height(48.dp)) }
            }

            Row(Modifier.fillMaxWidth()) {
                WordSiegeCompactAction(sh("GERİ AL", "UNDO"), Icons.Rounded.Undo,
                    canAct && placements.isNotEmpty(), Modifier.weight(1f)) {
                    placements.keys.lastOrNull()?.let(onBoardCell)
                }
                WordSiegeCompactAction(sh("KARIŞTIR", "SHUFFLE"), Icons.Rounded.Shuffle,
                    canAct && rack.length > 1, Modifier.weight(1f)) {
                    shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1
                }
                WordSiegeCompactAction(sh("PAS", "PASS"), Icons.Rounded.SkipNext,
                    canAct, Modifier.weight(1f), onPass)
                WordSiegeCompactAction(sh("DEĞİŞTİR", "EXCHANGE"), Icons.Rounded.SwapHoriz,
                    canAct && game.bag.isNotEmpty(), Modifier.weight(1f), onExchange)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    WordSiegeSideAction(
                        sh("SOHBET", "CHAT"),
                        Icons.Rounded.Chat,
                        modifier = Modifier.width(74.dp),
                        onClick = onChat,
                    )
                    ChatUnreadDot(GameChatBadge.unread)
                }
                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanSiegeMineBorder,
                        contentColor = Color.White,
                        disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                        disabledContentColor = WordSiegeGameUi.DisabledContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) {
                    // PRO's move score now floats above the word on the board.
                    if (busy) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
                WordSiegeOnlineBagButton(
                    game = game,
                    modifier = Modifier.width(82.dp),
                )
            }
        } else {
            PanSiegeFinishedCard(game, me)
        }

        notice?.let { PanSiegeNotice(it) }
        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) lastMove?.let { PanSiegeLastMoveInfo(it) }
        if (game.status == "playing") {
            WordSiegeTurnStrip(
                text = if (visualMyTurn) sh("SIRA SENDE • Kelimeni oluştur", "YOUR TURN • Build your word") else sh("RAKİP OYNUYOR", "RIVAL IS PLAYING"),
                playerTurn = visualMyTurn,
                playerAccent = PanSiegeMineBorder,
                rivalAccent = PanSiegeRivalBorder,
            )
        }
    }
    WordSiegeTileDragOverlay(tileDrag)
    }
    }
}

@Composable
private fun PanSiegeBoard(
    gameId: String,
    board: List<WordSiegeCellDto>,
    rack: String,
    placements: Map<Int, Int>,
    myOwner: Int,
    enabled: Boolean,
    playerTurn: Boolean,
    lastMoveMine: Boolean,
    lastMove: WordSiegeMoveDto?,
    captureEffect: WordSiegeCaptureEffect? = null,
    viewportMode: WordSiegeBoardViewportMode,
    onViewportModeChange: (WordSiegeBoardViewportMode) -> Unit,
    modifier: Modifier = Modifier,
    mascotSignal: WordSiegeMascotSignal? = null,
    mascotOutcome: WordSiegeMascotOutcome? = null,
    playerName: String? = null,
    playerGender: String? = null,
    tileDrag: WordSiegeTileDrag? = null,
    onTileDrop: (rackIndex: Int, fromCell: Int?, target: Int?) -> Unit = { _, _, _ -> },
    pendingValid: Boolean = false,
    pendingScore: Int? = null,
    onCell: (Int) -> Unit,
    onChat: () -> Unit,
) {
    val density = LocalDensity.current
    val tilePx = with(density) { PanSiegeCellSize.toPx() }
    val boardPx = tilePx * WordSiegeBoardSpec.Size
    var viewport by remember(gameId) { mutableStateOf(IntSize.Zero) }
    var closePan by remember(gameId) { mutableStateOf(Offset.Zero) }
    var dragging by remember(gameId) { mutableStateOf(false) }
    var initialized by remember(gameId) { mutableStateOf(false) }
    var viewportOriginInWindow by remember(gameId) { mutableStateOf(Offset.Unspecified) }
    val mascotTouches = remember(gameId) { WordSiegeMascotTouchState() }
    var observedMoveId by remember(gameId) { mutableStateOf(lastMove?.id) }
    var actionVfxMoveId by remember(gameId) { mutableStateOf<Long?>(null) }
    var highlightedIndices by remember(gameId) { mutableStateOf<Set<Int>>(emptySet()) }
    val highlightAlpha = remember(gameId) { Animatable(0f) }
    val closeScale = remember(viewport, boardPx) {
        wordSiegeOnlineCloseScale(
            viewportWidthPx = viewport.width.toFloat(),
            viewportHeightPx = viewport.height.toFloat(),
            boardWidthPx = boardPx,
        )
    }

    val transform by remember(viewportMode, viewport, boardPx, closePan, closeScale) {
        derivedStateOf {
            wordSiegeBoardTransform(
                mode = viewportMode,
                viewportWidthPx = viewport.width.toFloat(),
                viewportHeightPx = viewport.height.toFloat(),
                boardWidthPx = boardPx,
                closeScale = closeScale,
                closePan = closePan,
            )
        }
    }
    val boardBorderWidth = wordSiegeBoardBorderWidthDp(transform.scale).dp
    WordSiegeRegisterBoardHitTest(tileDrag, viewportOriginInWindow, viewport, transform, tilePx)
    val dragHover by remember(tileDrag) { derivedStateOf { tileDrag?.hoverCell } }
    val draggedFrom = tileDrag?.fromCell

    fun clampClosePan(candidate: Offset): Offset = clampWordSiegeBoardPan(
        candidate,
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        boardPx,
        closeScale,
    )

    fun centerCloseOn(index: Int): Offset =
        wordSiegeCenteredClosePan(
            index = index,
            viewportWidthPx = viewport.width.toFloat(),
            viewportHeightPx = viewport.height.toFloat(),
            boardWidthPx = boardPx,
            cellSizePx = tilePx,
            scale = closeScale,
        )

    fun toggleViewport(focusIndex: Int) {
        val nextMode = viewportMode.toggle()
        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {
            closePan = centerCloseOn(focusIndex)
        }
        onViewportModeChange(nextMode)
    }

    LaunchedEffect(viewport, gameId, boardPx, closeScale) {
        if (!initialized && viewport.width > 0 && viewport.height > 0) {
            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)
            initialized = true
            observedMoveId = lastMove?.id
        } else if (initialized) {
            closePan = clampClosePan(closePan)
        }
    }

    LaunchedEffect(lastMove?.id, viewportMode) {
        val moveId = lastMove?.id
        if (initialized && moveId != null && moveId != observedMoveId) {
            observedMoveId = moveId
            actionVfxMoveId = moveId
            highlightedIndices = lastMove.placedTiles.map { it.index }.filter(WordSiegeBoardSpec::isValidIndex).toSet()
            highlightAlpha.snapTo(0f)
            launch {
                highlightAlpha.animateTo(1f, tween(WORD_SIEGE_LAST_MOVE_ENTER_MS))
                delay(WORD_SIEGE_LAST_MOVE_HOLD_MS.toLong())
                highlightAlpha.animateTo(0.42f, tween(WORD_SIEGE_LAST_MOVE_EXIT_MS))
            }
            if (!dragging && viewportMode == WordSiegeBoardViewportMode.CLOSE) {
                val indices = lastMove.placedTiles.map { it.index }.filter(WordSiegeBoardSpec::isValidIndex)
                if (indices.isNotEmpty()) {
                    val avgRow = indices.map(WordSiegeBoardSpec::row).average()
                    val avgColumn = indices.map(WordSiegeBoardSpec::column).average()
                    val targetIndex = WordSiegeBoardSpec.index(
                        avgRow.toInt().coerceIn(0, WordSiegeBoardSpec.Size - 1),
                        avgColumn.toInt().coerceIn(0, WordSiegeBoardSpec.Size - 1),
                    )
                    closePan = centerCloseOn(targetIndex)
                }
            }
        }
    }

    val resolvedIndices = remember(actionVfxMoveId, lastMove?.id) {
        val move = lastMove
        if (move != null && actionVfxMoveId == move.id) {
            move.placedTiles.map { it.index }.filter(WordSiegeBoardSpec::isValidIndex).toSet()
        } else {
            emptySet()
        }
    }
    val actionVfxEvents = remember(gameId, placements, actionVfxMoveId, resolvedIndices) {
        buildList {
            placements.toSortedMap().forEach { (index, rackIndex) ->
                if (WordSiegeBoardSpec.isValidIndex(index)) {
                    add(PurchasedBoardVfxEvent("placement:$gameId:$index:$rackIndex", index, PurchasedBoardVfxKind.PLACEMENT))
                }
            }
            resolvedIndices.sorted().forEach { index ->
                add(PurchasedBoardVfxEvent("resolved:$actionVfxMoveId:$index", index, PurchasedBoardVfxKind.RESOLVED))
            }
        }
    }

    Surface(
        modifier = modifier,
        color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frame else Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frameEdge else Color(0xFFD2DBE5)),
        shadowElevation = if (WordSiegeWalnutIvory.enabled) 7.dp else 14.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.boardGrain else Brush.linearGradient(listOf(Color(0xFFEFE7D2), Color(0xFFECE3CB), Color(0xFFEFE7D2), Color(0xFFE9DFC5))))
                .border(1.dp, if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frameShade else Color(0xFFC9A560), RoundedCornerShape(14.dp))
                .clipToBounds()
                .onGloballyPositioned {
                    viewport = it.size
                    viewportOriginInWindow = it.localToWindow(Offset.Zero)
                }
                .wordSiegeMascotTouchWatcher(mascotTouches)
                .wordSiegeBoardDoubleTap(
                    key = Triple(gameId, viewportMode, transform),
                    cellAt = { wordSiegeCellAt(it, transform, tilePx) },
                    onDoubleTap = { toggleViewport(it) },
                )
                .pointerInput(gameId, viewportMode, viewport, boardPx, closeScale) {
                    if (viewportMode == WordSiegeBoardViewportMode.CLOSE) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragCancel = { dragging = false },
                            onDragEnd = { dragging = false },
                        ) { change, dragAmount ->
                            change.consume()
                            closePan = clampClosePan(closePan + dragAmount)
                        }
                    }
                },
        ) {
            Column(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(PanSiegeCellSize * WordSiegeBoardSpec.Size)
                    .graphicsLayer {
                        translationX = transform.pan.x
                        translationY = transform.pan.y
                        scaleX = transform.scale
                        scaleY = transform.scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                repeat(WordSiegeBoardSpec.Size) { row ->
                    Row {
                        repeat(WordSiegeBoardSpec.Size) { column ->
                            val index = WordSiegeBoardSpec.index(row, column)
                            val placedRackIndex = placements[index]
                            val pendingRackIndex = placedRackIndex?.takeIf { index != draggedFrom }
                            val pending = pendingRackIndex != null
                            val boardCell = board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) }
                            PanSiegeBoardCell(
                                cell = board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) },
                                pendingLetter = pendingRackIndex?.let(rack::getOrNull),
                                pending = pending,
                                myOwner = myOwner,
                                enabled = enabled,
                                overview = viewportMode == WordSiegeBoardViewportMode.FIT,
                                size = PanSiegeCellSize,
                                borderWidth = boardBorderWidth,
                                lastMoveHighlight = if (index in highlightedIndices) highlightAlpha.value else 0f,
                                onClick = { onCell(index) },
                                onDoubleClick = { toggleViewport(index) },
                                dropTarget = dragHover == index && boardCell.letter == null,
                                // Keyed on the placed tile so the gesture survives the tile hiding while carried.
                                dragSource = if (placedRackIndex != null) {
                                    Modifier.wordSiegeTileDragSource(
                                        drag = tileDrag,
                                        enabled = enabled,
                                        rackIndex = placedRackIndex,
                                        fromCell = index,
                                        letter = rack.getOrNull(placedRackIndex) ?: ' ',
                                        onDrop = onTileDrop,
                                    )
                                } else Modifier,
                            )
                        }
                    }
                }
            }

            WordSiegePendingMoveBadges(
                cells = placements.keys.filter { it != draggedFrom }.takeIf { it.size == placements.size }.orEmpty(),
                transform = transform,
                cellSizePx = tilePx,
                valid = pendingValid,
                score = pendingScore,
            )

            PurchasedBoardActionVfxOverlay(
                events = actionVfxEvents,
                transform = transform,
                cellSizePx = tilePx,
                modifier = Modifier.matchParentSize(),
            )

            captureEffect?.let { effect ->
                val sourcePositions = effect.batch.indices.associateWith { index ->
                    wordSiegeCaptureCellCenterInWindow(
                        index = index,
                        transform = transform,
                        cellSizePx = tilePx,
                        viewportOriginInWindow = viewportOriginInWindow,
                    )
                }
                WordSiegeCaptureFlightOverlay(
                    effect = effect,
                    sourcePositionsInWindow = sourcePositions,
                    anchorOriginInWindow = viewportOriginInWindow,
                )
            }

            // Tapping the mascot sends it flying to another perch; the right corners stay free
            // for the centre and chat buttons.
            WordSiegeMascotCompanion(
                anchors = WordSiegeBoardMascotPerches,
                mascotSize = 76.dp,
                moveId = lastMove?.id,
                lastMoveMine = lastMoveMine,
                playerTurn = playerTurn,
                modifier = Modifier.matchParentSize().padding(3.dp),
                moveScore = lastMove?.totalScore ?: 0,
                capturedCells = lastMove?.capturedCells ?: 0,
                opponentCaptured = lastMove?.opponentCaptured ?: 0,
                moveCell = lastMove?.placedTiles?.firstOrNull()?.index,
                pendingCells = placements.keys,
                signal = mascotSignal,
                outcome = mascotOutcome,
                playerName = playerName,
                playerGender = playerGender,
                touches = mascotTouches,
                // After a strong capture it may fly over to admire the new territory.
                visit = lastMove?.takeIf { lastMoveMine && (it.capturedCells >= 2 || it.opponentCaptured > 0) }?.let { move ->
                    wordSiegeMascotCellVisit(
                        key = "cap:${move.id}",
                        indices = move.placedTiles.map { it.index },
                        transform = transform,
                        cellSizePx = tilePx,
                        viewportWidthPx = viewport.width.toFloat(),
                        viewportHeightPx = viewport.height.toFloat(),
                        kind = WordSiegeMascotVisitKind.CAPTURE,
                    )
                },
            )

            SmallFloatingActionButton(
                onClick = { toggleViewport(WordSiegeBoardSpec.CenterIndex) },
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(36.dp),
                shape = CircleShape,
                containerColor = WordSiegeGameUi.Surface.copy(alpha = .95f),
                contentColor = Color(0xFF2C3E55),
            ) {
                Icon(Icons.Rounded.CenterFocusStrong, sh("Merkeze dön", "Center board"), Modifier.size(19.dp))
            }

            SmallFloatingActionButton(
                onClick = onChat,
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).size(42.dp),
                shape = CircleShape,
                containerColor = WordSiegeGameUi.Surface.copy(alpha = .95f),
                contentColor = Color(0xFF2C3E55),
            ) {
                Box {
                    Icon(Icons.Rounded.Chat, sh("Oyun içi sohbet", "In-game chat"), Modifier.size(20.dp))
                }
            }
            if (GameChatBadge.unread > 0) Box(Modifier.align(Alignment.BottomEnd).padding(7.dp).size(42.dp)) { ChatUnreadDot(GameChatBadge.unread) }
        }
    }
}

@Composable
private fun PanSiegeLastMoveInfo(move: WordSiegeMoveDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = WordSiegeGameUi.Surface,
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
    ) {
        Text(
            sh(
                "Kelime +${move.wordScore}  •  Bölge +${move.areaScore}  •  Toplam +${move.totalScore}",
                "Word +${move.wordScore}  •  Territory +${move.areaScore}  •  Total +${move.totalScore}",
            ),
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = WordSiegeGameUi.Text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PanSiegeBoardCell(
    cell: WordSiegeCellDto,
    pendingLetter: Char?,
    pending: Boolean,
    myOwner: Int,
    enabled: Boolean,
    overview: Boolean,
    size: Dp,
    borderWidth: Dp,
    lastMoveHighlight: Float,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    dropTarget: Boolean = false,
    dragSource: Modifier = Modifier,
) {
    val owner = if (pending) myOwner else cell.owner
    val territoryColor = when {
        owner == 0 -> PanSiegeNeutral
        owner == myOwner -> PanSiegeMine
        else -> PanSiegeRival
    }
    val letter = pendingLetter?.toString() ?: cell.letter
    val canPlace = enabled && (cell.letter == null || pending)
    val activeBonus = if (letter == null && !cell.bonusUsed) cell.bonus else null
    val bonusSurface = when (activeBonus) {
        "2H" -> PanSiegeBonus2H
        "3H" -> PanSiegeBonus3H
        "2K" -> PanSiegeBonus2K
        "3K" -> PanSiegeBonus3K
        WordSiegeBoardSpec.CenterBonus -> PanSiegeBonus4K
        WordSiegeBoardSpec.StarBonus -> PanSiegeBonusStar
        else -> null
    }
    val baseColor = when {
        pending -> if (WordSiegeWalnutIvory.enabled) territoryColor else PanSiegeTile
        letter != null -> territoryColor
        bonusSurface != null -> bonusSurface
        else -> PanSiegeNeutral
    }
    val displayBase = if (pending && !WordSiegeWalnutIvory.enabled) Color(0xFFF2D680) else baseColor
    val border = when {
        pending -> PanSiegeTileBorder
        letter != null && owner == myOwner -> PanSiegeMineBorder
        letter != null && owner != 0 -> PanSiegeRivalBorder
        activeBonus == WordSiegeBoardSpec.CenterBonus || activeBonus == WordSiegeBoardSpec.StarBonus -> if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.selection else Color(0xFFB07F1E)
        activeBonus != null -> PanSiegeBonusBorder
        else -> PanSiegeNeutralBorder
    }
    val regionGap = 1.25.dp
    val boardInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by boardInteraction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .94f else 1f, tween(if (pressed) 65 else 150), label = "siege cell press")

    Box(
        Modifier
            .size(size)
            .then(dragSource)
            .combinedClickable(
                interactionSource = boardInteraction,
                indication = null,
                onClick = {
                    dispatchWordSiegeBoardTap(
                        WordSiegeBoardTapAction.PLACE,
                        canPlace,
                        onClick,
                        onDoubleClick,
                    )
                },
                onDoubleClick = {
                    dispatchWordSiegeBoardTap(
                        WordSiegeBoardTapAction.TOGGLE_VIEWPORT,
                        canPlace,
                        onClick,
                        onDoubleClick,
                    )
                },
            )
            .padding(regionGap)
            .graphicsLayer { scaleX = if (WordSiegeWalnutIvory.enabled) pressScale else 1f; scaleY = if (WordSiegeWalnutIvory.enabled) pressScale else 1f }
            .clip(RoundedCornerShape(7.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(displayBase, Color.White, if (WordSiegeWalnutIvory.enabled) .08f else .18f),
                        displayBase,
                        androidx.compose.ui.graphics.lerp(displayBase, Color.Black, if (WordSiegeWalnutIvory.enabled) .10f else .07f),
                    )
                )
            )
            .border(
                width = if (dropTarget) 3.dp else if (lastMoveHighlight > 0f) 1.75.dp else 0.dp,
                color = if (dropTarget) Color(0xFF2FB36A) else PanSiegeLastMove.copy(alpha = .45f + .45f * lastMoveHighlight),
                shape = RoundedCornerShape(7.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = if (WordSiegeWalnutIvory.enabled) Modifier.fillMaxSize().padding(if (letter != null && owner != 0) 4.dp else .75.dp)
                .shadow(if (letter != null) 1.5.dp else 0.dp, RoundedCornerShape(7.dp)) else Modifier.fillMaxSize(),
            color = Color.Transparent,
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(
                if (pending) maxOf(if (WordSiegeWalnutIvory.enabled) 1.6.dp else 1.4.dp, borderWidth) else if (WordSiegeWalnutIvory.enabled) .65.dp else .45.dp,
                if (WordSiegeWalnutIvory.enabled) (if (pending) WordSiegeWalnutIvory.selection else if (letter != null) WordSiegeWalnutIvory.bevel else if (activeBonus != null) border else WordSiegeWalnutIvory.emptyEdge) else (if (pending) border.copy(alpha = .92f) else Color(0xFFCDBF9F)),
            ),
        ) {
            Box(
                modifier = if (WordSiegeWalnutIvory.enabled && letter != null) Modifier.background(WordSiegeWalnutIvory.tile) else Modifier,
                contentAlignment = Alignment.Center,
            ) {
                if (lastMoveHighlight > 0f) Box(Modifier.matchParentSize().background(PanSiegeLastMove.copy(alpha = .045f * lastMoveHighlight)))
                if (WordSiegeWalnutIvory.enabled && owner != 0 && !pending) {
                    Box(
                        Modifier.align(if (owner == myOwner) Alignment.TopStart else Alignment.TopEnd)
                            .padding(3.dp).size(7.dp)
                            .clip(if (owner == myOwner) CircleShape else RoundedCornerShape(1.dp))
                            .background(if (owner == myOwner) PanSiegeMine else PanSiegeRival),
                    )
                }
                if (letter != null) {
                    Text(
                        letter,
                        color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.ink else if (!pending && owner != 0) Color.White else Color(0xFF4A3217),
                        fontSize = if (overview) 24.sp else 22.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = if (overview) .10.sp else .25.sp,
                    )
                    Text(
                        panSiegeLetterValue(letter),
                        color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.secondaryInk else (if (!pending && owner != 0) Color.White else Color(0xFF4A3217)).copy(alpha = .78f),
                        fontSize = WordSiegeBoardAccessibility.BoardLetterPoint,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    )
                } else if (activeBonus != null) {
                    Text(
                        androidx.compose.ui.text.buildAnnotatedString {
                            val label = WordSiegeBoardSpec.displayBonusLabel(activeBonus, !SonHarfUiState.isEnglish)
                            val parts = label.split("\n")
                            if (parts.size > 1 && overview) {
                                withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 20.sp)) {
                                    append(parts.first().take(1)); append(parts.last())
                                }
                            } else if (parts.size > 1) {
                                withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 10.sp)) { append(parts.first()) }
                                append("\n")
                                append(parts.last())
                            } else append(label)
                        },
                        color = PanSiegeBonusLabel,
                        fontSize = WordSiegeBoardAccessibility.BoardBonus,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 17.sp,
                        fontWeight = if (overview) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanSiegeRackTile(
    letter: Char,
    selected: Boolean,
    used: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .95f else 1f, tween(if (pressed) 65 else 150), label = "siege rack press")
    Surface(
        modifier = modifier.height(48.dp).graphicsLayer { scaleX = if (WordSiegeWalnutIvory.enabled) pressScale else 1f; scaleY = if (WordSiegeWalnutIvory.enabled) pressScale else 1f }
            .combinedClickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        color = when {
            used -> WordSiegeGameUi.SurfaceSoft
            WordSiegeWalnutIvory.enabled -> WordSiegeWalnutIvory.ivory
            selected -> Color(0xFFD6C38D)
            else -> Color(0xFFE3D6B0)
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (WordSiegeWalnutIvory.enabled) (if (selected) WordSiegeWalnutIvory.selection else WordSiegeWalnutIvory.bevel) else (if (selected) PanSiegeMineBorder else Color(0xFFC9A560))),
        shadowElevation = if (WordSiegeWalnutIvory.enabled) (if (pressed) 0.dp else if (selected) 4.dp else 2.dp) else (if (selected) 7.dp else 4.dp),
    ) {
        Box(if (WordSiegeWalnutIvory.enabled) Modifier.background(WordSiegeWalnutIvory.tile) else Modifier, contentAlignment = Alignment.Center) {
            Text(
                letter.toString(),
                color = if (WordSiegeWalnutIvory.enabled) (if (used) WordSiegeWalnutIvory.ink.copy(alpha = .35f) else WordSiegeWalnutIvory.ink) else (if (used) WordSiegeGameUi.Muted.copy(alpha = .45f) else Color(0xFF4A3217)),
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = .35.sp,
            )
            Text(
                panSiegeLetterValue(letter.toString()),
                color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.secondaryInk else WordSiegeGameUi.Muted,
                fontSize = WordSiegeBoardAccessibility.RackPoint,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        }
    }
}

@Composable
private fun PanSiegeNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WordSiegeGameUi.SurfaceSoft,
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
    ) {
        Text(message, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = WordSiegeGameUi.Text, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PanSiegePlayerCard(
    profile: ProfileDto?,
    fallbackName: String,
    score: Int,
    wordPoints: Int,
    territoryPoints: Int,
    areaCount: Int,
    accent: Color,
    active: Boolean,
    leading: Boolean,
    modifier: Modifier = Modifier,
    scoreArrivalTick: Int = 0,
    scoreLossTick: Int = 0,
    onScoreCenterChanged: (Offset) -> Unit = {},
    mascot: WordSiegeMascotSkin? = null,
) {
    // The rival's mascot perches on their card; no mascot of your own is needed to see it.
    Box(modifier) {
    WordSiegeScoreCard(
        name = profile?.displayName ?: fallbackName,
        score = score, wordPoints = wordPoints, territoryPoints = territoryPoints,
        area = areaCount, accent = accent, active = active, leading = leading,
        avatarPath = profile?.avatarPath, gender = profile?.gender,
        avatarVisible = profile?.avatarVisibility != "hidden", isBot = false,
        modifier = Modifier.fillMaxWidth(),
        scoreArrivalTick = scoreArrivalTick,
        scoreLossTick = scoreLossTick,
        onScoreCenterChanged = onScoreCenterChanged,
        frameId = rememberPlayerFrame(profile?.id),
    )
    if (mascot != null) {
        WordSiegeMascot(
            moveId = null,
            lastMoveMine = false,
            pendingCells = emptyList(),
            playerTurn = active,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-18).dp).size(36.dp),
            // The rival's mascot, bare: your own hat is not theirs.
            hat = WordSiegeMascotHat.NONE,
            skin = mascot,
        )
    }
    }
}

@Composable
private fun PanSiegeFinishedCard(game: WordSiegeGameDto, me: String?) {
    val won = game.winnerId == me
    val draw = game.winnerId == null
    val mine = if (me != null && me == game.playerTwoId) 2 else 1
    val rival = if (mine == 1) 2 else 1
    val myWords = panSiegeWordScore(game, mine)
    val rivalWords = panSiegeWordScore(game, rival)
    val myCubes = panSiegeAreaCount(game, mine)
    val myTotal = myWords + if (mine == 1) game.playerOneAreaScore else game.playerTwoAreaScore
    val rivalTotal = rivalWords + if (rival == 1) game.playerOneAreaScore else game.playerTwoAreaScore
    // Every player first sees the victory or defeat clip full screen; closing it shows the details.
    var showResult by androidx.compose.runtime.saveable.rememberSaveable(game.id) { mutableStateOf(!draw) }
    if (showResult) {
        MatchResultScreen(
            won = won,
            title = if (won) sh("TAHT SENİN!", "THE THRONE IS YOURS!") else sh("KAYBETTİN", "YOU LOST"),
            subtitle = null,
            mine = ResultScore(sh("SEN", "YOU"), "$myTotal"),
            rival = ResultScore(sh("RAKİP", "RIVAL"), "$rivalTotal"),
            primaryLabel = sh("DEVAM", "CONTINUE"),
            onPrimary = { showResult = false },
            secondaryLabel = sh("DETAYLAR", "DETAILS"),
            onSecondary = { showResult = false },
        )
    }
    HfCard(modifier = Modifier.fillMaxWidth(), color = Hf.Ground) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            HfTitleRule(
                when { draw -> sh("BERABERE", "DRAW"); won -> sh("TAHT SENİN!", "THE THRONE IS YOURS!"); else -> sh("OYUN BİTTİ", "GAME OVER") },
                fontSize = 24.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PanSiegeResultSide(sh("SEN", "YOU"), myTotal, Hf.Green, Modifier.weight(1f))
                PanSiegeResultSide(sh("RAKİP", "RIVAL"), rivalTotal, Hf.Red, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            HfCard(modifier = Modifier.fillMaxWidth(), color = Hf.Ground) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    PanSiegeResultRow(sh("Kazanılan küpler", "Cubes won"), "+$myCubes")
                    HorizontalDivider(color = Hf.Gold.copy(alpha = .45f))
                    PanSiegeResultRow(sh("Kelime puanı", "Word points"), "+$myWords")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                sh("Sonuç = kelime puanı + şu an sahip olunan küpler (küp başına 2)", "Result = word score + currently owned cubes (2 per cube)"),
                color = WordSiegeGameUi.Muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PanSiegeResultSide(label: String, score: Int, accent: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = Hf.PillShape, color = accent, border = BorderStroke(1.5.dp, Hf.Gold)) {
            Text(label, Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Hf.Text, fontSize = 17.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
        Surface(shape = RoundedCornerShape(14.dp), color = Hf.Ivory) {
            Text("$score", Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Hf.Text, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PanSiegeResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Hf.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = Hf.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

private fun panSiegeWordScore(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneWordScore else game.playerTwoWordScore

private fun panSiegeAreaCount(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneArea else game.playerTwoArea

@Composable
private fun panSiegeStatusLabel(game: WordSiegeGameDto, me: String?): String = when {
    game.status == "cancelled" -> sh("İptal edildi", "Cancelled")
    game.status != "finished" -> sh("Devam ediyor", "In progress")
    game.winnerId == null -> sh("Berabere", "Draw")
    game.winnerId == me -> sh("Kazandın", "You won")
    else -> sh("Rakip kazandı", "Rival won")
}

private fun panSiegeLetterValue(letter: String): String = when (letter) {
    "A", "E", "İ", "K", "L", "N", "R", "T" -> "1"
    "I", "M", "O", "S", "U" -> "2"
    "B", "D", "Ü", "Y" -> "3"
    "C", "Ç", "Ş", "Z" -> "4"
    "G", "H", "P" -> "5"
    "F", "Ö", "V" -> "7"
    "Ğ" -> "8"
    "J" -> "10"
    else -> "1"
}

/**
 * Perches for the board mascot (fractions of the board viewport). All sit on the outer edge,
 * where cells are rarely played; the right-hand corners stay free for the board buttons.
 */
internal val WordSiegeBoardMascotPerches = listOf(
    Offset(0f, 0f),
    Offset(0f, 1f),
    Offset(.5f, 0f),
    Offset(.5f, 1f),
    Offset(0f, .5f),
)

/** Converts board cells into a mascot visit point (viewport fraction); null when off-screen. */
internal fun wordSiegeMascotCellVisit(
    key: String,
    indices: Collection<Int>,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    kind: WordSiegeMascotVisitKind,
): WordSiegeMascotVisit? {
    if (indices.isEmpty() || viewportWidthPx <= 0f || viewportHeightPx <= 0f) return null
    val row = indices.map { WordSiegeBoardSpec.row(it) + .5f }.average().toFloat()
    val column = indices.map { WordSiegeBoardSpec.column(it) + .5f }.average().toFloat()
    val x = (transform.pan.x + column * cellSizePx * transform.scale) / viewportWidthPx
    val y = (transform.pan.y + row * cellSizePx * transform.scale) / viewportHeightPx
    if (x !in .06f..0.94f || y !in .06f..0.94f) return null
    return WordSiegeMascotVisit(key, Offset(x, y), kind)
}
