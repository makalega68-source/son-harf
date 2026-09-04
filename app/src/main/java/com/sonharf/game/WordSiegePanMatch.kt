package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay

private val PanSiegeTile = Color(0xFFFFE3A5)
private val PanSiegeTileBorder = Color(0xFFD99818)
private val PanSiegeBoardSurface = Color(0xFFE7EDF5)
private val PanSiegeNeutral = Color(0xFFF7F8FA)
private val PanSiegeMine = Color(0xFF35C878)
private val PanSiegeRival = Color(0xFFFF5F57)
private val PanSiegeNeutralBorder = Color(0xFF7890A8)
private val PanSiegeBonusBorder = Color(0xFF5279A6)
private val PanSiegeMineBorder = Color(0xFF147A48)
private val PanSiegeRivalBorder = Color(0xFFB72E35)
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
    val myEarnedCubePoints = remember(moves, me) { WordSiegeFinalRules.earnedCubePoints(moves, me) }
    val rivalEarnedCubePoints = remember(moves, opponentId) { WordSiegeFinalRules.earnedCubePoints(moves, opponentId) }
    val myTargetScore = remember(game.playerOneWordScore, game.playerTwoWordScore, myOwner, myEarnedCubePoints, rivalEarnedCubePoints) {
        WordSiegeFinalRules.netScore(panSiegeWordScore(game, myOwner), myEarnedCubePoints, rivalEarnedCubePoints)
    }
    val rivalTargetScore = remember(game.playerOneWordScore, game.playerTwoWordScore, rivalOwner, rivalEarnedCubePoints, myEarnedCubePoints) {
        WordSiegeFinalRules.netScore(panSiegeWordScore(game, rivalOwner), rivalEarnedCubePoints, myEarnedCubePoints)
    }
    var fallbackPracticeActive by remember(game.id) { mutableStateOf(false) }
    var shuffleSeed by remember(game.id) { mutableIntStateOf(0) }
    val visualMyTurn = myTurn
    val rackOrder = remember(rack, shuffleSeed) {
        if (shuffleSeed == 0) rack.indices.toList() else wordSiegeShuffledRackIndices(rack.length, shuffleSeed)
    }
    val readyFeedback = wordSiegeValidationFeedback(
        placementsCount = placements.size,
        turkish = !SonHarfUiState.isEnglish,
    )
    val previewCapturedCells = placements.keys.count { index -> game.board.getOrNull(index)?.owner != myOwner }

    LaunchedEffect(game.id, game.status) {
        if (game.status == "waiting") {
            delay(WORD_SIEGE_BOT_FALLBACK_DELAY_MS)
            fallbackPracticeActive = true
        } else {
            fallbackPracticeActive = false
        }
    }

    if (fallbackPracticeActive && game.status == "waiting") {
        WordSiegePracticeScreen(onExit = { fallbackPracticeActive = false })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, sh("Oyunlar", "Games"), tint = MainUi.Text)
            }
            Column(Modifier.weight(1f)) {
                Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    if (game.status == "playing") {
                        if (visualMyTurn) sh("SIRA SENDE", "YOUR TURN") else sh("RAKİPTE", "RIVAL'S TURN")
                    } else panSiegeStatusLabel(game, me),
                    color = if (visualMyTurn) MainUi.Green else MainUi.Red,
                    fontSize = GameTypography.Secondary,
                    fontWeight = FontWeight.Black,
                )
            }
            Surface(shape = RoundedCornerShape(99.dp), color = SiegePurpleSoft) {
                Text(sh("SÜRE YOK", "NO TIMER"), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = SiegePurple, fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PanSiegePlayerCard(
                profile = mine,
                fallbackName = sh("Sen", "You"),
                score = myTargetScore,
                earnedCubePoints = myEarnedCubePoints,
                areaCount = panSiegeAreaCount(game, myOwner),
                accent = MainUi.Green,
                active = game.currentPlayerId == me,
                modifier = Modifier.weight(1f),
            )
            PanSiegePlayerCard(
                profile = opponent,
                fallbackName = if (game.status == "waiting") sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"),
                score = rivalTargetScore,
                earnedCubePoints = rivalEarnedCubePoints,
                areaCount = panSiegeAreaCount(game, rivalOwner),
                accent = MainUi.Red,
                active = game.currentPlayerId == opponentId,
                modifier = Modifier.weight(1f),
            )
        }

        if (game.status == "waiting") {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = MainUi.Surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = SiegePurple)
                    Spacer(Modifier.height(12.dp))
                    Text(sh("RAKİP ARANIYOR", "FINDING A RIVAL"), color = MainUi.Text, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        sh(
                            "15 saniye içinde rakip bulunmazsa botla hemen başlayacaksın. Gerçek rakip araması arka planda sürecek.",
                            "If no rival is found within 15 seconds, practice starts immediately with a bot while real matchmaking continues in the background.",
                        ),
                        color = MainUi.Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onCancelWaiting, enabled = !busy, border = BorderStroke(1.dp, MainUi.Red)) {
                        Text(sh("ARAMAYI İPTAL ET", "CANCEL SEARCH"), color = MainUi.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
            notice?.let { PanSiegeNotice(it) }
            return@Column
        }

        PanSiegeBoard(
            gameId = game.id,
            board = game.board,
            rack = rack,
            placements = placements,
            myOwner = myOwner,
            enabled = canAct,
            lastMove = lastMove,
            modifier = Modifier.fillMaxWidth().weight(1f),
            onCell = onBoardCell,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(
                onClick = onChat,
                enabled = game.playerTwoId != null,
                modifier = Modifier.weight(1f).height(36.dp),
                border = BorderStroke(1.dp, MainUi.Blue),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Icon(Icons.Rounded.Chat, null, Modifier.size(15.dp), tint = MainUi.Blue)
                Spacer(Modifier.width(4.dp))
                Text(sh("SOHBET", "CHAT"), color = MainUi.Blue, fontSize = GameTypography.Action, fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                onClick = onForfeit,
                enabled = game.status == "playing" && !busy,
                modifier = Modifier.weight(1f).height(36.dp),
                border = BorderStroke(1.dp, MainUi.Red),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Icon(Icons.Rounded.Flag, null, Modifier.size(15.dp), tint = MainUi.Red)
                Spacer(Modifier.width(4.dp))
                Text(sh("PES ET", "FORFEIT"), color = MainUi.Red, fontSize = GameTypography.Action, fontWeight = FontWeight.Black)
            }
        }

        if (game.status == "playing") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (placements.isNotEmpty()) {
                    Text(readyFeedback.message, color = MainUi.Green, fontSize = GameTypography.Action, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sh(
                            "Alan +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS} • kelime puanı OYNA ile doğrulanır",
                            "Area +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS} • word score is verified on PLAY",
                        ),
                        color = MainUi.Muted,
                        fontSize = GameTypography.Metadata,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                } else Spacer(Modifier.weight(1f))
                Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = MainUi.Muted, fontSize = GameTypography.Metadata)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rackOrder.forEach { rackIndex ->
                    val letter = rack.getOrNull(rackIndex) ?: return@forEach
                    PanSiegeRackTile(
                        letter = letter,
                        selected = selectedRackIndex == rackIndex,
                        used = rackIndex in placements.values,
                        enabled = canAct,
                        modifier = Modifier.weight(1f),
                        onClick = { onRackTile(rackIndex) },
                    )
                }
                repeat((7 - rack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).height(48.dp)) }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(
                    onClick = { placements.keys.lastOrNull()?.let(onBoardCell) },
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) {
                    Icon(Icons.Rounded.Undo, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(sh("GERİ AL", "UNDO"), fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = { shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1 },
                    enabled = canAct && rack.length > 1,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onPass,
                    enabled = canAct,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) { Text(sh("PAS", "PASS"), fontSize = GameTypography.Action, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onExchange,
                    enabled = canAct && game.bag.isNotEmpty(),
                    modifier = Modifier.weight(1.15f).height(44.dp),
                    border = BorderStroke(1.dp, SiegePurple),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = GameTypography.Action, fontWeight = FontWeight.Black) }
                Button(
                    onClick = onSubmit,
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1.45f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainUi.Blue,
                        contentColor = Color.White,
                        disabledContainerColor = SonHarfTheme.DisabledBackground,
                        disabledContentColor = SonHarfTheme.DisabledContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 5.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("OYNA", "PLAY"), fontSize = GameTypography.Action, fontWeight = FontWeight.Black)
                }
            }
        } else {
            PanSiegeFinishedCard(game, me)
        }

        notice?.let { PanSiegeNotice(it) }
        lastMove?.let { PanSiegeLastMoveInfo(it) }
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
    lastMove: WordSiegeMoveDto?,
    modifier: Modifier = Modifier,
    onCell: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tilePx = with(density) { PanSiegeCellSize.toPx() }
    val boardPx = tilePx * WordSiegeBoardSpec.Size
    var viewport by remember(gameId) { mutableStateOf(IntSize.Zero) }
    var closePan by remember(gameId) { mutableStateOf(Offset.Zero) }
    var closeScale by remember(gameId) { mutableFloatStateOf(WORD_SIEGE_DEFAULT_CLOSE_SCALE) }
    var dragging by remember(gameId) { mutableStateOf(false) }
    var initialized by remember(gameId) { mutableStateOf(false) }
    var observedMoveId by remember(gameId) { mutableStateOf(lastMove?.id) }
    var actionVfxMoveId by remember(gameId) { mutableStateOf<Long?>(null) }
    var viewportMode by remember(gameId) { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }
    val lastMoveHighlightAlpha = remember(gameId) { Animatable(0f) }

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
    val highlightBorderWidth = wordSiegeBoardBorderWidthDp(transform.scale, desiredScreenWidthDp = 1.8f).dp

    fun clampClosePan(candidate: Offset, scale: Float = closeScale): Offset = clampWordSiegeBoardPan(
        candidate,
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        boardPx,
        scale,
    )

    fun centerCloseOn(index: Int, scale: Float = closeScale): Offset =
        wordSiegeCenteredClosePan(
            index = index,
            viewportWidthPx = viewport.width.toFloat(),
            viewportHeightPx = viewport.height.toFloat(),
            boardWidthPx = boardPx,
            cellSizePx = tilePx,
            scale = scale,
        )

    fun toggleViewport() {
        val nextMode = viewportMode.toggle()
        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {
            closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE
            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex, WORD_SIEGE_DEFAULT_CLOSE_SCALE)
        }
        viewportMode = nextMode
    }

    LaunchedEffect(viewport, gameId, boardPx) {
        if (!initialized && viewport.width > 0 && viewport.height > 0) {
            closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE
            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex, WORD_SIEGE_DEFAULT_CLOSE_SCALE)
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
            lastMoveHighlightAlpha.snapTo(0f)
            lastMoveHighlightAlpha.animateTo(1f, animationSpec = tween(180))
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
            delay(1_200)
            lastMoveHighlightAlpha.animateTo(0f, animationSpec = tween(200))
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
    LaunchedEffect(closePan, closeScale) {
        if (dragging) {
            delay(120)
            dragging = false
        }
    }

    val stableBoard = remember(board) {
        List(WordSiegeBoardSpec.Size * WordSiegeBoardSpec.Size) { index ->
            board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) }
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
        color = PanSiegeBoardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .clipToBounds()
                .onGloballyPositioned { viewport = it.size }
                .pointerInput(gameId, viewportMode, viewport, boardPx) {
                    if (viewportMode == WordSiegeBoardViewportMode.CLOSE) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            dragging = true
                            val previousScale = closeScale
                            val nextScale = (previousScale * zoom).coerceIn(WORD_SIEGE_MIN_CLOSE_SCALE, WORD_SIEGE_MAX_CLOSE_SCALE)
                            val scaleRatio = nextScale / previousScale
                            val zoomAdjustedPan = (closePan - centroid) * scaleRatio + centroid + pan
                            closeScale = nextScale
                            closePan = clampClosePan(zoomAdjustedPan, nextScale)
                        }
                    }
                },
        ) {
            Column(
                Modifier
                    // Keep the oversized board's layout origin aligned with transform.pan.
                    // Without this, requiredSize is coerced and Compose centers its layer implicitly.
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
                            val pendingRackIndex = placements[index]
                            val pending = pendingRackIndex != null
                            key(index) {
                                PanSiegeBoardCell(
                                    cell = stableBoard[index],
                                    pendingLetter = pendingRackIndex?.let(rack::getOrNull),
                                    pending = pending,
                                    myOwner = myOwner,
                                    enabled = enabled,
                                    size = PanSiegeCellSize,
                                    borderWidth = boardBorderWidth,
                                    highlightBorderWidth = highlightBorderWidth,
                                    lastMoveHighlightAlpha = if (index in resolvedIndices) lastMoveHighlightAlpha.value else 0f,
                                    onClick = { onCell(index) },
                                    onDoubleClick = ::toggleViewport,
                                )
                            }
                        }
                    }
                }
            }

            PurchasedBoardActionVfxOverlay(
                events = actionVfxEvents,
                transform = transform,
                cellSizePx = tilePx,
                modifier = Modifier.matchParentSize(),
            )

            SmallFloatingActionButton(
                onClick = {
                    viewportMode = WordSiegeBoardViewportMode.CLOSE
                    closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE
                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex, WORD_SIEGE_DEFAULT_CLOSE_SCALE)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(36.dp),
                shape = CircleShape,
                containerColor = Color.White.copy(alpha = .94f),
                contentColor = MainUi.Blue,
            ) {
                Icon(Icons.Rounded.CenterFocusStrong, sh("Merkeze dön", "Center board"), Modifier.size(19.dp))
            }

        }
    }
}

@Composable
private fun PanSiegeLastMoveInfo(move: WordSiegeMoveDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MainUi.Surface,
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        Text(
            sh(
                "Kelime +${move.wordScore}  •  Alan +${move.areaScore}  •  Toplam +${move.totalScore}",
                "Word +${move.wordScore}  •  Area +${move.areaScore}  •  Total +${move.totalScore}",
            ),
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = MainUi.Text,
            fontSize = GameTypography.Metadata,
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
    size: Dp,
    borderWidth: Dp,
    highlightBorderWidth: Dp,
    lastMoveHighlightAlpha: Float,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val owner = if (pending) myOwner else cell.owner
    val baseColor = when {
        owner == 0 -> PanSiegeNeutral
        owner == myOwner -> PanSiegeMine
        else -> PanSiegeRival
    }
    val border = when {
        pending -> PanSiegeTileBorder
        owner == myOwner -> PanSiegeMineBorder
        owner != 0 -> PanSiegeRivalBorder
        !cell.bonusUsed && cell.bonus != null -> PanSiegeBonusBorder
        else -> PanSiegeNeutralBorder
    }
    val letter = pendingLetter?.toString() ?: cell.letter
    val canPlace = enabled && (cell.letter == null || pending)

    Box(
        Modifier
            .size(size)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(baseColor)
            .combinedClickable(
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
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = when {
                pending -> PanSiegeTile.copy(alpha = .76f)
                lastMoveHighlightAlpha > 0f -> Color.White.copy(alpha = .07f * lastMoveHighlightAlpha)
                else -> Color.Transparent
            },
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(
                when {
                    pending -> maxOf(2.dp, borderWidth)
                    lastMoveHighlightAlpha > 0f -> highlightBorderWidth
                    else -> borderWidth
                },
                border.copy(alpha = .96f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (letter != null) {
                    Text(letter, color = Color.Black, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(
                        panSiegeLetterValue(letter),
                        color = Color.Black.copy(alpha = .78f),
                        fontSize = WordSiegeBoardAccessibility.BoardLetterPoint,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    )
                } else if (!cell.bonusUsed && cell.bonus != null) {
                    Text(
                        cell.bonus,
                        color = when (cell.bonus) { "2H", "3H" -> MainUi.Blue else -> SiegePurple },
                        fontSize = WordSiegeBoardAccessibility.BoardBonus,
                        fontWeight = FontWeight.Black,
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
    Surface(
        modifier = modifier.height(48.dp).combinedClickable(enabled = enabled, onClick = onClick),
        color = when {
            used -> MainUi.SurfaceSoft
            selected -> PanSiegeTile
            else -> Color(0xFFFFF1C9)
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MainUi.Blue else PanSiegeTileBorder.copy(alpha = .7f)),
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter.toString(), color = if (used) MainUi.Muted.copy(alpha = .45f) else MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                panSiegeLetterValue(letter.toString()),
                color = MainUi.Muted,
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
        color = MainUi.SurfaceSoft,
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        Text(message, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = MainUi.Text, fontSize = GameTypography.Metadata, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PanSiegePlayerCard(
    profile: ProfileDto?,
    fallbackName: String,
    score: Int,
    earnedCubePoints: Int,
    areaCount: Int,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = if (active) accent.copy(alpha = .08f) else MainUi.Surface,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = profile?.displayName ?: fallbackName,
                size = 50.dp,
                accent = accent,
                visible = profile?.avatarVisibility != "hidden",
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(profile?.displayName ?: fallbackName, color = MainUi.Text, fontSize = GameTypography.PlayerName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val displayedScore by animateIntAsState(score, animationSpec = tween(220), label = "siege-score")
                Text("$displayedScore", color = accent, fontSize = GameTypography.Score, fontWeight = FontWeight.Black)
                Text(
                    sh("Küp +$earnedCubePoints • Alan $areaCount • küp başına ±2", "Cubes +$earnedCubePoints • Area $areaCount • ±2 per cube"),
                    color = MainUi.Muted,
                    fontSize = GameTypography.Metadata,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PanSiegeFinishedCard(game: WordSiegeGameDto, me: String?) {
    val won = game.winnerId == me
    val draw = game.winnerId == null
    val accent = when { draw -> MainUi.Gold; won -> MainUi.Green; else -> MainUi.Red }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = .08f),
        border = BorderStroke(1.dp, accent.copy(alpha = .45f)),
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when { draw -> sh("BERABERE", "DRAW"); won -> sh("KUŞATMA SENİN!", "SIEGE WON!"); else -> sh("OYUN BİTTİ", "GAME OVER") },
                color = accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                sh("Sonuç = kelime puanı + şu an sahip olunan küpler (küp başına 2)", "Result = word score + currently owned cubes (2 per cube)"),
                color = MainUi.Muted,
                fontSize = GameTypography.Metadata,
            )
        }
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