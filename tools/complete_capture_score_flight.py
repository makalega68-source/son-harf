#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

def write(path, text):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")

def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))

# Score target coordinates + arrival pulse/ring. Existing light card and WordSiegeScoreMetric stay intact.
path = "app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt"
replace_once(path,
'''import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
''',
'''import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.drawBehind
''')
replace_once(path,
'''import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
''',
'''import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
''')
replace_once(path,
'''import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
''',
'''import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
''')
replace_once(path,
'''import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
''',
'''import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
''')
replace_once(path,
'''    isBot: Boolean,
    modifier: Modifier = Modifier,
) {
    val edge = if (active) accent else WordSiegeGameUi.Border
''',
'''    isBot: Boolean,
    modifier: Modifier = Modifier,
    scoreArrivalTick: Int = 0,
    onScoreCenterChanged: (Offset) -> Unit = {},
) {
    val edge = if (active) accent else WordSiegeGameUi.Border
    val scoreScale = remember { Animatable(1f) }
    val scoreGlow = remember { Animatable(0f) }

    LaunchedEffect(scoreArrivalTick) {
        if (scoreArrivalTick <= 0) return@LaunchedEffect
        scoreScale.snapTo(1f)
        scoreGlow.snapTo(0f)
        coroutineScope {
            launch {
                scoreScale.animateTo(1.14f, tween(90))
                scoreScale.animateTo(1f, tween(170))
            }
            launch {
                scoreGlow.animateTo(1f, tween(70))
                scoreGlow.animateTo(0f, tween(260))
            }
        }
    }
''')
replace_once(path,
'''                val totalDescription = sh("Toplam $score", "Total $score")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = .10f),
                    border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
                ) {
                    Text(
                        "$score",
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp).semantics { contentDescription = totalDescription },
                        color = accent,
                        fontSize = 21.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
''',
'''                val totalDescription = sh("Toplam $score", "Total $score")
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scoreScale.value
                            scaleY = scoreScale.value
                        }
                        .drawBehind {
                            if (scoreGlow.value > 0f) {
                                drawCircle(
                                    color = accent.copy(alpha = .55f * scoreGlow.value),
                                    radius = size.maxDimension * (.58f + .10f * scoreGlow.value),
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                            }
                        }
                        .onGloballyPositioned { coordinates ->
                            onScoreCenterChanged(
                                coordinates.localToWindow(
                                    Offset(coordinates.size.width / 2f, coordinates.size.height / 2f),
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = .10f),
                        border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
                    ) {
                        Text(
                            "$score",
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp).semantics { contentDescription = totalDescription },
                            color = accent,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                }
''')

# Live match capture tracking and arrival-gated display.
path = "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt"
replace_once(path,
'''    val myTargetScore = WordSiegeFinalRules.currentTerritoryScore(myWordPoints, myAreaCount)
    val rivalTargetScore = WordSiegeFinalRules.currentTerritoryScore(rivalWordPoints, rivalAreaCount)
    val displayedMyScore by animateIntAsState(myTargetScore, tween(260), label = "siege-my-score")
    val displayedRivalScore by animateIntAsState(rivalTargetScore, tween(260), label = "siege-rival-score")
''',
'''    val myTargetScore = WordSiegeFinalRules.currentTerritoryScore(myWordPoints, myAreaCount)
    val rivalTargetScore = WordSiegeFinalRules.currentTerritoryScore(rivalWordPoints, rivalAreaCount)
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
    var myScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }
    var rivalScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }
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
    val displayedMyScore =
        wordSiegeDisplayedScore(myTargetScore, pendingMyCapturePoints + candidateMyPoints)
    val displayedRivalScore =
        wordSiegeDisplayedScore(rivalTargetScore, pendingRivalCapturePoints + candidateRivalPoints)

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
            } else if (batch.owner == rivalOwner) {
                pendingRivalCapturePoints += batch.points
            }
        }
    }

    val activeCapture = captureQueue.firstOrNull()
    val captureEffect = activeCapture?.let { batch ->
        WordSiegeCaptureEffect(
            batch = batch,
            targetInWindow = if (batch.owner == myOwner) myScoreTargetInWindow else rivalScoreTargetInWindow,
            accent = if (batch.owner == myOwner) PanSiegeMineBorder else PanSiegeRivalBorder,
            onCubeArrived = {
                if (batch.owner == myOwner) {
                    pendingMyCapturePoints =
                        (pendingMyCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    myScoreArrivalTick += 1
                } else {
                    pendingRivalCapturePoints =
                        (pendingRivalCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    rivalScoreArrivalTick += 1
                }
            },
            onFinished = {
                captureQueue = captureQueue.filterNot { it.updateKey == batch.updateKey }
            },
        )
    }
''')
replace_once(path,
'''                leading = myTargetScore > rivalTargetScore,
                modifier = Modifier.weight(1f),
            )
''',
'''                leading = myTargetScore > rivalTargetScore,
                modifier = Modifier.weight(1f),
                scoreArrivalTick = myScoreArrivalTick,
                onScoreCenterChanged = { myScoreTargetInWindow = it },
            )
''')
replace_once(path,
'''                leading = rivalTargetScore > myTargetScore,
                modifier = Modifier.weight(1f),
            )
''',
'''                leading = rivalTargetScore > myTargetScore,
                modifier = Modifier.weight(1f),
                scoreArrivalTick = rivalScoreArrivalTick,
                onScoreCenterChanged = { rivalScoreTargetInWindow = it },
            )
''')
replace_once(path,
'''            enabled = canAct,
            lastMove = lastMove,
            viewportMode = boardViewportMode,
''',
'''            enabled = canAct,
            lastMove = lastMove,
            captureEffect = captureEffect,
            viewportMode = boardViewportMode,
''')
replace_once(path,
'''    enabled: Boolean,
    lastMove: WordSiegeMoveDto?,
    viewportMode: WordSiegeBoardViewportMode,
''',
'''    enabled: Boolean,
    lastMove: WordSiegeMoveDto?,
    captureEffect: WordSiegeCaptureEffect? = null,
    viewportMode: WordSiegeBoardViewportMode,
''')
replace_once(path,
'''    var dragging by remember(gameId) { mutableStateOf(false) }
    var initialized by remember(gameId) { mutableStateOf(false) }
    var observedMoveId by remember(gameId) { mutableStateOf(lastMove?.id) }
''',
'''    var dragging by remember(gameId) { mutableStateOf(false) }
    var initialized by remember(gameId) { mutableStateOf(false) }
    var viewportOriginInWindow by remember(gameId) { mutableStateOf(Offset.Unspecified) }
    var observedMoveId by remember(gameId) { mutableStateOf(lastMove?.id) }
''')
replace_once(path,
'''                .clipToBounds()
                .onGloballyPositioned { viewport = it.size }
                .pointerInput(gameId, viewportMode, viewport, boardPx, closeScale) {
''',
'''                .clipToBounds()
                .onGloballyPositioned {
                    viewport = it.size
                    viewportOriginInWindow = it.localToWindow(Offset.Zero)
                }
                .pointerInput(gameId, viewportMode, viewport, boardPx, closeScale) {
''')
replace_once(path,
'''            PurchasedBoardActionVfxOverlay(
                events = actionVfxEvents,
                transform = transform,
                cellSizePx = tilePx,
                modifier = Modifier.matchParentSize(),
            )

            SmallFloatingActionButton(
''',
'''            PurchasedBoardActionVfxOverlay(
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

            SmallFloatingActionButton(
''')
replace_once(path,
'''    leading: Boolean,
    modifier: Modifier = Modifier,
) {
    WordSiegeScoreCard(
''',
'''    leading: Boolean,
    modifier: Modifier = Modifier,
    scoreArrivalTick: Int = 0,
    onScoreCenterChanged: (Offset) -> Unit = {},
) {
    WordSiegeScoreCard(
''')
replace_once(path,
'''        avatarVisible = profile?.avatarVisibility != "hidden", isBot = false,
        modifier = modifier,
    )
''',
'''        avatarVisible = profile?.avatarVisibility != "hidden", isBot = false,
        modifier = modifier,
        scoreArrivalTick = scoreArrivalTick,
        onScoreCenterChanged = onScoreCenterChanged,
    )
''')

# Practice board keeps both viewport callback and capture effect.
path = "app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt"
replace_once(path,
'''    moveEventKey: Int? = null,
    resolvedIndices: Set<Int> = emptySet(),
    language: String = SonHarfUiState.language,
''',
'''    moveEventKey: Int? = null,
    resolvedIndices: Set<Int> = emptySet(),
    captureEffect: WordSiegeCaptureEffect? = null,
    language: String = SonHarfUiState.language,
''')
replace_once(path,
'''    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE) }
    var initialized by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }
''',
'''    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE) }
    var initialized by remember { mutableStateOf(false) }
    var viewportOriginInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }
''')
replace_once(path,
'''                .clipToBounds()
                .onGloballyPositioned { viewport = it.size }
                .pointerInput(mode, viewport, boardPx, closeScale) {
''',
'''                .clipToBounds()
                .onGloballyPositioned {
                    viewport = it.size
                    viewportOriginInWindow = it.localToWindow(Offset.Zero)
                }
                .pointerInput(mode, viewport, boardPx, closeScale) {
''')
replace_once(path,
'''            PurchasedBoardActionVfxOverlay(
                events = actionVfxEvents,
                transform = transform,
                cellSizePx = tilePx,
                modifier = Modifier.matchParentSize(),
            )
''',
'''            PurchasedBoardActionVfxOverlay(
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
''')

# Practice screen queues only successful move deltas.
path = "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
replace_once(path,
'''import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
''',
'''import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
''')
replace_once(path,
'''    var boardViewportMode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }
    var actionVfxEvent by remember { mutableIntStateOf(0) }
    var showSiegePulse by remember { mutableStateOf(false) }
''',
'''    var boardViewportMode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }
    var actionVfxEvent by remember { mutableIntStateOf(0) }
    var captureQueue by remember { mutableStateOf<List<WordSiegeCaptureBatch>>(emptyList()) }
    var pendingPlayerCapturePoints by remember { mutableIntStateOf(0) }
    var pendingBotCapturePoints by remember { mutableIntStateOf(0) }
    var playerScoreArrivalTick by remember { mutableIntStateOf(0) }
    var botScoreArrivalTick by remember { mutableIntStateOf(0) }
    var playerScoreTargetInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var botScoreTargetInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var showSiegePulse by remember { mutableStateOf(false) }
''')
replace_once(path,
'''    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)
    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)
    val displayedPlayerScore by animateIntAsState(playerTargetScore, tween(260), label = "practice-player-score")
    val displayedBotScore by animateIntAsState(botTargetScore, tween(260), label = "practice-bot-score")
''',
'''    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)
    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)
    val displayedPlayerScore = wordSiegeDisplayedScore(playerTargetScore, pendingPlayerCapturePoints)
    val displayedBotScore = wordSiegeDisplayedScore(botTargetScore, pendingBotCapturePoints)
''')
replace_once(path,
'''    fun clearSelection() {
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
    }

    fun completeTutorial() {
''',
'''    fun clearSelection() {
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
        if (owner == 1) pendingPlayerCapturePoints += batch.points
        else pendingBotCapturePoints += batch.points
    }

    fun completeTutorial() {
''')
replace_once(path,
'''        boardViewportMode = WordSiegeBoardViewportMode.FIT
        actionVfxEvent = 0
        notice = if (matchmakingFallback) {
''',
'''        boardViewportMode = WordSiegeBoardViewportMode.FIT
        actionVfxEvent = 0
        captureQueue = emptyList()
        pendingPlayerCapturePoints = 0
        pendingBotCapturePoints = 0
        playerScoreArrivalTick = 0
        botScoreArrivalTick = 0
        notice = if (matchmakingFallback) {
''')
replace_once(path,
'''.onSuccess { (next, move) ->
                state = next
                lastMove = move
''',
'''.onSuccess { (next, move) ->
                enqueueCapture(state, next, 1)
                state = next
                lastMove = move
''')
replace_once(path,
'''                val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements)
                state = next
                lastMove = move
''',
'''                val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements)
                enqueueCapture(state, next, 2)
                state = next
                lastMove = move
''')
replace_once(path,
'''    BackHandler(onBack = onExit)

    LaunchedEffect(
''',
'''    val activeCapture = captureQueue.firstOrNull()
    val captureEffect = activeCapture?.let { batch ->
        WordSiegeCaptureEffect(
            batch = batch,
            targetInWindow = if (batch.owner == 1) playerScoreTargetInWindow else botScoreTargetInWindow,
            accent = if (batch.owner == 1) PracticePlayerAccent else PracticeRivalAccent,
            onCubeArrived = {
                if (batch.owner == 1) {
                    pendingPlayerCapturePoints =
                        (pendingPlayerCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    playerScoreArrivalTick += 1
                } else {
                    pendingBotCapturePoints =
                        (pendingBotCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)
                    botScoreArrivalTick += 1
                }
            },
            onFinished = {
                captureQueue = captureQueue.filterNot { it.updateKey == batch.updateKey }
            },
        )
    }

    BackHandler(onBack = onExit)

    LaunchedEffect(
''')
replace_once(path,
'''                        isBot = false,
                        modifier = Modifier.weight(1f),
                    )
''',
'''                        isBot = false,
                        modifier = Modifier.weight(1f),
                        scoreArrivalTick = playerScoreArrivalTick,
                        onScoreCenterChanged = { playerScoreTargetInWindow = it },
                    )
''')
replace_once(path,
'''                        isBot = true,
                        modifier = Modifier.weight(1f),
                    )
''',
'''                        isBot = true,
                        modifier = Modifier.weight(1f),
                        scoreArrivalTick = botScoreArrivalTick,
                        onScoreCenterChanged = { botScoreTargetInWindow = it },
                    )
''')
replace_once(path,
'''                        moveEventKey = actionVfxEvent.takeIf { it > 0 },
                        resolvedIndices = lastMove?.placements?.keys ?: emptySet(),
                        modifier = if (boardViewportMode == WordSiegeBoardViewportMode.CLOSE) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(1f),
''',
'''                        moveEventKey = actionVfxEvent.takeIf { it > 0 },
                        resolvedIndices = lastMove?.placements?.keys ?: emptySet(),
                        captureEffect = captureEffect,
                        modifier = if (boardViewportMode == WordSiegeBoardViewportMode.CLOSE) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(1f),
''')
replace_once(path,
'''    isBot: Boolean,
    modifier: Modifier = Modifier,
) {
    WordSiegeScoreCard(
''',
'''    isBot: Boolean,
    modifier: Modifier = Modifier,
    scoreArrivalTick: Int = 0,
    onScoreCenterChanged: (Offset) -> Unit = {},
) {
    WordSiegeScoreCard(
''')
replace_once(path,
'''        gender = gender, avatarVisible = avatarVisible, isBot = isBot,
        modifier = modifier,
    )
''',
'''        gender = gender, avatarVisible = avatarVisible, isBot = isBot,
        modifier = modifier,
        scoreArrivalTick = scoreArrivalTick,
        onScoreCenterChanged = onScoreCenterChanged,
    )
''')

# Regression contract recognizes the arrival-gated score presentation.
path = "app/src/test/java/com/sonharf/game/WordSiegeFinalRulesTest.kt"
replace_once(path,
'''        assertTrue(practice.contains("animateIntAsState"))
        assertTrue(pan.contains("animateIntAsState"))
        assertFalse(practice.contains("delay(28)"))
''',
'''        assertTrue(practice.contains("wordSiegeDisplayedScore"))
        assertTrue(pan.contains("WordSiegeCaptureTracker"))
        assertTrue(practice.contains("WordSiegeCaptureEffect"))
        assertTrue(pan.contains("WordSiegeCaptureEffect"))
        assertFalse(practice.contains("delay(28)"))
''')

write("app/src/main/java/com/sonharf/game/WordSiegeCaptureEffect.kt", r'''package com.sonharf.game

internal const val WORD_SIEGE_CAPTURE_POINTS_PER_CUBE = 2

internal data class WordSiegeCaptureBatch(
    val updateKey: String,
    val owner: Int,
    val indices: List<Int>,
) {
    init {
        require(owner == 1 || owner == 2) { "capture owner must be 1 or 2" }
    }

    val points: Int
        get() = indices.size * WORD_SIEGE_CAPTURE_POINTS_PER_CUBE
}

internal fun wordSiegeCaptureBatch(
    updateKey: String,
    previousOwners: List<Int>,
    currentOwners: List<Int>,
    capturingOwner: Int,
): WordSiegeCaptureBatch? {
    if (updateKey.isBlank() || capturingOwner !in 1..2) return null
    if (previousOwners.size != currentOwners.size) return null

    val captured = currentOwners.indices
        .asSequence()
        .filter(WordSiegeBoardSpec::isValidIndex)
        .filter { index ->
            currentOwners[index] == capturingOwner && previousOwners[index] != capturingOwner
        }
        .toList()

    return captured.takeIf { it.isNotEmpty() }?.let {
        WordSiegeCaptureBatch(updateKey = updateKey, owner = capturingOwner, indices = it)
    }
}

internal fun wordSiegeDisplayedScore(actualScore: Int, pendingCapturePoints: Int): Int =
    (actualScore - pendingCapturePoints.coerceAtLeast(0)).coerceAtLeast(0)

/**
 * Keeps the last consumed server board as a visual baseline.
 * The initial update is consumed by construction, so open/reconnect never replay old cubes.
 */
internal class WordSiegeCaptureTracker(
    initialUpdateKey: String?,
    initialOwners: List<Int>,
) {
    private var baselineOwners: List<Int> = initialOwners.toList()
    private val consumedUpdateKeys = mutableSetOf<String>().apply {
        initialUpdateKey?.takeIf(String::isNotBlank)?.let(::add)
    }

    fun preview(
        updateKey: String?,
        currentOwners: List<Int>,
        capturingOwner: Int?,
        expectedCaptured: Int,
    ): WordSiegeCaptureBatch? {
        val key = updateKey?.takeIf(String::isNotBlank) ?: return null
        val owner = capturingOwner?.takeIf { it in 1..2 } ?: return null
        if (expectedCaptured <= 0 || key in consumedUpdateKeys) return null

        val batch = wordSiegeCaptureBatch(
            updateKey = key,
            previousOwners = baselineOwners,
            currentOwners = currentOwners,
            capturingOwner = owner,
        ) ?: return null

        // Move metadata and board row can arrive on different realtime ticks.
        if (batch.indices.size < expectedCaptured) return null
        return batch
    }

    fun consume(batch: WordSiegeCaptureBatch, currentOwners: List<Int>) {
        if (currentOwners.size != baselineOwners.size) return
        consumedUpdateKeys += batch.updateKey
        baselineOwners = currentOwners.toList()
    }
}
''')

write("app/src/main/java/com/sonharf/game/WordSiegeCaptureMotion.kt", r'''package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal const val WORD_SIEGE_CAPTURE_FLIGHT_MS = 540
internal const val WORD_SIEGE_CAPTURE_STAGGER_MS = 85L

internal data class WordSiegeCaptureEffect(
    val batch: WordSiegeCaptureBatch,
    val targetInWindow: Offset,
    val accent: Color,
    val onCubeArrived: () -> Unit,
    val onFinished: () -> Unit,
)

internal fun wordSiegeCaptureCellCenterInWindow(
    index: Int,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
    viewportOriginInWindow: Offset,
): Offset {
    if (!wordSiegeCapturePositionReady(viewportOriginInWindow)) return Offset.Unspecified
    return viewportOriginInWindow + wordSiegeCellCenterInViewport(index, transform, cellSizePx)
}

internal fun wordSiegeCaptureArcPoint(start: Offset, end: Offset, progress: Float): Offset {
    val t = progress.coerceIn(0f, 1f)
    val oneMinus = 1f - t
    val lift = maxOf(
        42f,
        abs(end.y - start.y) * .10f + abs(end.x - start.x) * .055f,
    )
    val control = Offset(
        x = (start.x + end.x) / 2f,
        y = minOf(start.y, end.y) - lift,
    )
    return Offset(
        x = oneMinus * oneMinus * start.x + 2f * oneMinus * t * control.x + t * t * end.x,
        y = oneMinus * oneMinus * start.y + 2f * oneMinus * t * control.y + t * t * end.y,
    )
}

internal fun wordSiegeCaptureStaggerDelayMs(ordinal: Int): Long =
    ordinal.coerceAtLeast(0) * WORD_SIEGE_CAPTURE_STAGGER_MS

internal fun wordSiegeCapturePositionReady(position: Offset): Boolean =
    position.x.isFinite() && position.y.isFinite()

@Composable
internal fun WordSiegeCaptureFlightOverlay(
    effect: WordSiegeCaptureEffect,
    sourcePositionsInWindow: Map<Int, Offset>,
    anchorOriginInWindow: Offset,
) {
    val allSourcesReady = effect.batch.indices.all { index ->
        sourcePositionsInWindow[index]?.let(::wordSiegeCapturePositionReady) == true
    }
    val ready = allSourcesReady &&
        wordSiegeCapturePositionReady(effect.targetInWindow) &&
        wordSiegeCapturePositionReady(anchorOriginInWindow)

    val frozenSources = remember(effect.batch.updateKey, ready) {
        if (ready) sourcePositionsInWindow.toMap() else emptyMap()
    }
    val frozenTarget = remember(effect.batch.updateKey, ready) {
        if (ready) effect.targetInWindow else Offset.Unspecified
    }
    val progresses = remember(effect.batch.updateKey) {
        effect.batch.indices.map { Animatable(0f) }
    }
    var started by remember(effect.batch.updateKey) { mutableStateOf(false) }
    val onCubeArrived by rememberUpdatedState(effect.onCubeArrived)
    val onFinished by rememberUpdatedState(effect.onFinished)

    LaunchedEffect(effect.batch.updateKey, ready) {
        if (!ready || started) return@LaunchedEffect
        started = true
        coroutineScope {
            progresses.forEachIndexed { ordinal, animation ->
                launch {
                    delay(wordSiegeCaptureStaggerDelayMs(ordinal))
                    animation.snapTo(0f)
                    animation.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = WORD_SIEGE_CAPTURE_FLIGHT_MS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                    onCubeArrived()
                }
            }
        }
        onFinished()
    }

    if (!ready || frozenSources.isEmpty()) return

    val popupHalfPx = with(LocalDensity.current) { 36.dp.toPx() }

    effect.batch.indices.forEachIndexed { ordinal, index ->
        val progress = progresses[ordinal].value
        if (progress <= 0.001f || progress >= 0.9999f) return@forEachIndexed

        val start = frozenSources.getValue(index)
        val point = wordSiegeCaptureArcPoint(start, frozenTarget, progress)
        val earlier = wordSiegeCaptureArcPoint(start, frozenTarget, (progress - .055f).coerceAtLeast(0f))
        val velocity = point - earlier
        val localPoint = point - anchorOriginInWindow

        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(
                x = (localPoint.x - popupHalfPx).roundToInt(),
                y = (localPoint.y - popupHalfPx).roundToInt(),
            ),
            properties = PopupProperties(focusable = false),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        alpha = when {
                            progress < .10f -> (progress / .10f).coerceIn(0f, 1f)
                            progress > .86f -> ((1f - progress) / .14f).coerceIn(0f, 1f)
                            else -> 1f
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    if (progress < .20f) {
                        val flash = (1f - progress / .20f).coerceIn(0f, 1f)
                        drawCircle(
                            color = effect.accent.copy(alpha = .28f * flash),
                            radius = 18.dp.toPx() + 8.dp.toPx() * (1f - flash),
                            center = center,
                        )
                    }

                    val length = sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
                    val unit = if (length > .001f) {
                        Offset(velocity.x / length, velocity.y / length)
                    } else {
                        Offset.Zero
                    }
                    repeat(5) { particle ->
                        val distance = (particle + 1) * 5.5f
                        drawCircle(
                            color = effect.accent.copy(alpha = .34f * (1f - particle / 5f)),
                            radius = (3.1f - particle * .38f).dp.toPx(),
                            center = center - unit * distance,
                        )
                    }
                    drawCircle(
                        color = effect.accent.copy(alpha = .16f),
                        radius = 15.dp.toPx(),
                        center = center,
                    )
                }
                Text(
                    text = "+$WORD_SIEGE_CAPTURE_POINTS_PER_CUBE",
                    color = effect.accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
''')

write("app/src/test/java/com/sonharf/game/WordSiegeCaptureEffectTest.kt", r'''package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WordSiegeCaptureEffectTest {
    @Test fun neutralAndOpponentOwnedCubesAreBothIncluded() {
        val previous = owners(0 to 0, 1 to 2, 2 to 1, 3 to 0)
        val current = owners(0 to 1, 1 to 1, 2 to 1, 3 to 0)

        val batch = wordSiegeCaptureBatch(
            updateKey = "move-2",
            previousOwners = previous,
            currentOwners = current,
            capturingOwner = 1,
        )

        assertNotNull(batch)
        assertEquals(listOf(0, 1), batch!!.indices)
        assertEquals(4, batch.points)
    }

    @Test fun initialAndRepeatedServerUpdateNeverReplay() {
        val initial = owners(0 to 1, 1 to 2)
        val tracker = WordSiegeCaptureTracker("10", initial)

        assertNull(tracker.preview("10", initial, 1, expectedCaptured = 1))

        val next = initial.toMutableList().apply { this[1] = 1 }
        val batch = tracker.preview("11", next, 1, expectedCaptured = 1)
        assertNotNull(batch)
        tracker.consume(batch!!, next)

        assertNull(tracker.preview("11", next, 1, expectedCaptured = 1))

        val reconnect = WordSiegeCaptureTracker("11", next)
        assertNull(reconnect.preview("11", next, 1, expectedCaptured = 1))
    }

    @Test fun passRejectedAndResetStatesProduceNoSyntheticCapture() {
        val initial = owners(0 to 1, 1 to 2)
        val tracker = WordSiegeCaptureTracker("20", initial)

        assertNull(tracker.preview("21", initial, 2, expectedCaptured = 0))
        assertNull(tracker.preview("20", initial, 1, expectedCaptured = 1))

        val reset = List(WordSiegeBoardSpec.CellCount) { 0 }
        val newGameTracker = WordSiegeCaptureTracker(null, reset)
        assertNull(newGameTracker.preview(null, reset, null, expectedCaptured = 0))
    }

    @Test fun trackerWaitsForCompleteBoardRowBeforeConsumingMove() {
        val initial = owners(0 to 0, 1 to 2)
        val tracker = WordSiegeCaptureTracker("30", initial)
        val partial = initial.toMutableList().apply { this[0] = 1 }
        assertNull(tracker.preview("31", partial, 1, expectedCaptured = 2))

        val complete = partial.toMutableList().apply { this[1] = 1 }
        val batch = tracker.preview("31", complete, 1, expectedCaptured = 2)
        assertEquals(listOf(0, 1), batch!!.indices)
    }

    @Test fun visibleScoreWithholdsOnlyPendingCapturePoints() {
        assertEquals(38, wordSiegeDisplayedScore(actualScore = 42, pendingCapturePoints = 4))
        assertEquals(42, wordSiegeDisplayedScore(actualScore = 42, pendingCapturePoints = 0))
        assertEquals(0, wordSiegeDisplayedScore(actualScore = 0, pendingCapturePoints = 4))
    }

    private fun owners(vararg changes: Pair<Int, Int>): List<Int> =
        MutableList(WordSiegeBoardSpec.CellCount) { 0 }.apply {
            changes.forEach { (index, owner) -> this[index] = owner }
        }
}
''')

write("app/src/test/java/com/sonharf/game/WordSiegeCaptureMotionTest.kt", r'''package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSiegeCaptureMotionTest {
    @Test fun transformedCubeCenterUsesRealZoomPanAndViewportOrigin() {
        val transform = WordSiegeBoardTransform(
            scale = 2f,
            pan = Offset(10f, 20f),
            renderedWidthPx = 1_500f,
            renderedHeightPx = 1_500f,
        )
        val index = WordSiegeBoardSpec.index(1, 2)

        val center = wordSiegeCaptureCellCenterInWindow(
            index = index,
            transform = transform,
            cellSizePx = 50f,
            viewportOriginInWindow = Offset(100f, 200f),
        )

        assertEquals(360f, center.x, .001f)
        assertEquals(370f, center.y, .001f)
    }

    @Test fun arcStartsAndEndsExactlyAtRequestedCoordinates() {
        val start = Offset(25f, 400f)
        val end = Offset(180f, 40f)

        assertOffsetEquals(start, wordSiegeCaptureArcPoint(start, end, 0f))
        assertOffsetEquals(end, wordSiegeCaptureArcPoint(start, end, 1f))
    }

    @Test fun multipleCubesUseShortSequentialStagger() {
        assertEquals(0L, wordSiegeCaptureStaggerDelayMs(0))
        assertEquals(85L, wordSiegeCaptureStaggerDelayMs(1))
        assertEquals(255L, wordSiegeCaptureStaggerDelayMs(3))
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, .001f)
        assertEquals(expected.y, actual.y, .001f)
    }
}
''')

print("capture score flight patch applied")
