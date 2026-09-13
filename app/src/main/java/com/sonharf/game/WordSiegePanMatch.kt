package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PanSiegeTile = Color(0xFFF4F7F5)
private val PanSiegeTileBorder = Color(0xFF8EA697)
private val PanSiegeBoardSurface = Color(0xFFE8ECE8)
private val PanSiegeNeutral = Color(0xFFFAF7EF)
private val PanSiegeMine = Color(0xFFA8D5B5)
private val PanSiegeRival = Color(0xFFE4AEAA)
private val PanSiegeNeutralBorder = Color(0xFFB8C3BC)
private val PanSiegeBonusBorder = Color(0xFFA79BB2)
private val PanSiegeMineBorder = Color(0xFF3F7C53)
private val PanSiegeRivalBorder = Color(0xFF9B4D4A)
private val PanSiegeBonus2H = Color(0xFFDCEAF2)
private val PanSiegeBonus3H = Color(0xFFDCEAF2)
private val PanSiegeBonus2K = Color(0xFFEAE2F0)
private val PanSiegeBonus3K = Color(0xFFEAE2F0)
private val PanSiegeBonus4K = Color(0xFFE7DDBB)
private val PanSiegeBonusStar = Color(0xFFEAD59B)
private val PanSiegeLastMove = Color(0xFFE7B95E)
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
    val myAreaCount = panSiegeAreaCount(game, myOwner)
    val rivalAreaCount = panSiegeAreaCount(game, rivalOwner)
    val myWordPoints = panSiegeWordScore(game, myOwner)
    val rivalWordPoints = panSiegeWordScore(game, rivalOwner)
    val myTerritoryPoints = WordSiegeFinalRules.cubeTransfer(myAreaCount)
    val rivalTerritoryPoints = WordSiegeFinalRules.cubeTransfer(rivalAreaCount)
    val myTargetScore = WordSiegeFinalRules.currentTerritoryScore(myWordPoints, myAreaCount)
    val rivalTargetScore = WordSiegeFinalRules.currentTerritoryScore(rivalWordPoints, rivalAreaCount)
    val displayedMyScore by animateIntAsState(myTargetScore, tween(260), label = "siege-my-score")
    val displayedRivalScore by animateIntAsState(rivalTargetScore, tween(260), label = "siege-rival-score")
    val myMapControl = ((myAreaCount * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val rivalMapControl = ((rivalAreaCount * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val displayedCurrentPlayerId = game.currentPlayerId
    var fallbackPracticeActive by remember(game.id) { mutableStateOf(false) }
    var shuffleSeed by remember(game.id) { mutableIntStateOf(0) }
    val visualMyTurn = game.status == "playing" && displayedCurrentPlayerId == me
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
        WordSiegePracticeScreen(
            onExit = { fallbackPracticeActive = false },
            matchmakingFallback = true,
        )
        return
    }

    WordSiegeGameTheme {
    Column(
        Modifier
            .fillMaxSize()
            .background(WordSiegeGameUi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, sh("Oyunlar", "Games"), tint = WordSiegeGameUi.Text)
            }
            Column(Modifier.weight(1f)) {
                Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = WordSiegeGameUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    if (game.status == "playing") {
                        if (visualMyTurn) sh("SIRA SENDE", "YOUR TURN") else sh("RAKİPTE", "RIVAL'S TURN")
                    } else panSiegeStatusLabel(game, me),
                    color = if (visualMyTurn) PanSiegeMineBorder else PanSiegeRivalBorder,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Surface(shape = RoundedCornerShape(99.dp), color = SiegePurpleSoft) {
                Text(sh("SÜRE YOK", "NO TIMER"), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black)
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
            )
        }

        WordSiegeOwnershipLegend()

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
                            "15 saniye içinde rakip bulunmazsa geçici bot maçı hemen başlayacak. Gerçek rakip araması arka planda sürecek.",
                            "If no rival is found within 15 seconds, a temporary bot match starts immediately while real matchmaking continues in the background.",
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
                border = BorderStroke(1.dp, WordSiegeGameUi.Blue),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Icon(Icons.Rounded.Chat, null, Modifier.size(15.dp), tint = WordSiegeGameUi.Blue)
                Spacer(Modifier.width(4.dp))
                Text(sh("SOHBET", "CHAT"), color = WordSiegeGameUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                onClick = onForfeit,
                enabled = game.status == "playing" && !busy,
                modifier = Modifier.weight(1f).height(36.dp),
                border = BorderStroke(1.dp, WordSiegeGameUi.Red),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Icon(Icons.Rounded.Flag, null, Modifier.size(15.dp), tint = WordSiegeGameUi.Red)
                Spacer(Modifier.width(4.dp))
                Text(sh("PES ET", "FORFEIT"), color = WordSiegeGameUi.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }

        if (game.status == "playing") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (placements.isNotEmpty()) {
                    Text(readyFeedback.message, color = PanSiegeMineBorder, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sh(
                            "Puan hamle onayında hesaplanır",
                            "Score is calculated when confirmed",
                        ),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 8.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                } else Spacer(Modifier.weight(1f))
                Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = WordSiegeGameUi.Muted, fontSize = 8.sp)
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
                    Text(sh("GERİ AL", "UNDO"), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = { shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1 },
                    enabled = canAct && rack.length > 1,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onPass,
                    enabled = canAct,
                    modifier = Modifier.weight(.82f).height(46.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) { Text(sh("PAS", "PASS"), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onExchange,
                    enabled = canAct && game.bag.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(46.dp),
                    border = BorderStroke(1.dp, SiegePurple),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 11.sp, fontWeight = FontWeight.Black) }
            }
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFE9D9A5), Color(0xFFAF8C45), Color(0xFFF6EAC7)))),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanSiegeMineBorder,
                        contentColor = Color.White,
                        disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                        disabledContentColor = WordSiegeGameUi.DisabledContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 5.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            PanSiegeFinishedCard(game, me)
        }

        notice?.let { PanSiegeNotice(it) }
        lastMove?.let { PanSiegeLastMoveInfo(it) }
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
    lastMove: WordSiegeMoveDto?,
    modifier: Modifier = Modifier,
    onCell: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tilePx = with(density) { PanSiegeCellSize.toPx() }
    val boardPx = tilePx * WordSiegeBoardSpec.Size
    var viewport by remember(gameId) { mutableStateOf(IntSize.Zero) }
    var closePan by remember(gameId) { mutableStateOf(Offset.Zero) }
    var dragging by remember(gameId) { mutableStateOf(false) }
    var initialized by remember(gameId) { mutableStateOf(false) }
    var observedMoveId by remember(gameId) { mutableStateOf(lastMove?.id) }
    var actionVfxMoveId by remember(gameId) { mutableStateOf<Long?>(null) }
    var highlightedIndices by remember(gameId) { mutableStateOf<Set<Int>>(emptySet()) }
    val highlightAlpha = remember(gameId) { Animatable(0f) }
    var viewportMode by remember(gameId) { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }
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
        viewportMode = nextMode
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
        color = PanSiegeBoardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .75f)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .clipToBounds()
                .onGloballyPositioned { viewport = it.size }
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
                            val pendingRackIndex = placements[index]
                            val pending = pendingRackIndex != null
                            PanSiegeBoardCell(
                                cell = board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) },
                                pendingLetter = pendingRackIndex?.let(rack::getOrNull),
                                pending = pending,
                                myOwner = myOwner,
                                enabled = enabled,
                                size = PanSiegeCellSize,
                                borderWidth = boardBorderWidth,
                                lastMoveHighlight = if (index in highlightedIndices) highlightAlpha.value else 0f,
                                onClick = { onCell(index) },
                                onDoubleClick = { toggleViewport(index) },
                            )
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
                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(36.dp),
                shape = CircleShape,
                containerColor = Color.White.copy(alpha = .94f),
                contentColor = WordSiegeGameUi.Blue,
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
    size: Dp,
    borderWidth: Dp,
    lastMoveHighlight: Float,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
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
        pending -> PanSiegeTile
        letter != null -> territoryColor
        bonusSurface != null -> bonusSurface
        else -> PanSiegeNeutral
    }
    val border = when {
        pending -> PanSiegeTileBorder
        letter != null && owner == myOwner -> PanSiegeMineBorder
        letter != null && owner != 0 -> PanSiegeRivalBorder
        activeBonus == WordSiegeBoardSpec.CenterBonus || activeBonus == WordSiegeBoardSpec.StarBonus -> Color(0xFF8D7438)
        activeBonus != null -> PanSiegeBonusBorder
        else -> PanSiegeNeutralBorder
    }
    val regionGap = 1.25.dp

    Box(
        Modifier
            .size(size)
            .padding(regionGap)
            .clip(RoundedCornerShape(7.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(baseColor, Color.White, .12f), baseColor)))
            .border(
                width = if (lastMoveHighlight > 0f) 1.75.dp else 0.dp,
                color = PanSiegeLastMove.copy(alpha = .45f + .45f * lastMoveHighlight),
                shape = RoundedCornerShape(7.dp),
            )
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
            color = Color.Transparent,
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(if (pending) maxOf(2.dp, borderWidth) else borderWidth, border.copy(alpha = .92f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (lastMoveHighlight > 0f) Box(Modifier.matchParentSize().background(PanSiegeLastMove.copy(alpha = .045f * lastMoveHighlight)))
                if (letter != null) {
                    Text(letter, color = Color(0xFF17372C), fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(
                        panSiegeLetterValue(letter),
                        color = Color(0xFF17372C).copy(alpha = .78f),
                        fontSize = WordSiegeBoardAccessibility.BoardLetterPoint,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    )
                } else if (activeBonus != null) {
                    Text(
                        androidx.compose.ui.text.buildAnnotatedString {
                    val label = WordSiegeBoardSpec.displayBonusLabel(activeBonus, !SonHarfUiState.isEnglish)
                    val parts = label.split("\n")
                    if (parts.size > 1) {
                        withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 11.sp)) { append(parts.first()) }
                        append("\n")
                        append(parts.last())
                    } else append(label)
                },
                        color = when (activeBonus) {
                            "2H" -> Color(0xFF456F83)
                            "3H" -> Color(0xFF4F735A)
                            "2K", "3K" -> Color(0xFF6D5A7B)
                            WordSiegeBoardSpec.CenterBonus -> Color(0xFF6B5A2D)
                            WordSiegeBoardSpec.StarBonus -> Color(0xFF755E21)
                            else -> WordSiegeGameUi.Text
                        },
                        fontSize = WordSiegeBoardAccessibility.BoardBonus,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp,
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
            used -> WordSiegeGameUi.SurfaceSoft
            selected -> Color(0xFFE1ECE4)
            else -> PanSiegeTile
        },
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PanSiegeMineBorder else PanSiegeTileBorder.copy(alpha = .7f)),
        shadowElevation = if (selected) 3.dp else 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter.toString(), color = if (used) WordSiegeGameUi.Muted.copy(alpha = .45f) else WordSiegeGameUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                panSiegeLetterValue(letter.toString()),
                color = WordSiegeGameUi.Muted,
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
) {
    WordSiegeScoreCard(
        name = profile?.displayName ?: fallbackName,
        score = score, wordPoints = wordPoints, territoryPoints = territoryPoints,
        area = areaCount, accent = accent, active = active, leading = leading,
        avatarPath = profile?.avatarPath, gender = profile?.gender,
        avatarVisible = profile?.avatarVisibility != "hidden", isBot = false,
        modifier = modifier,
    )
}

@Composable
private fun PanSiegeFinishedCard(game: WordSiegeGameDto, me: String?) {
    val won = game.winnerId == me
    val draw = game.winnerId == null
    val accent = when { draw -> WordSiegeGameUi.Gold; won -> PanSiegeMineBorder; else -> PanSiegeRivalBorder }
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
                color = WordSiegeGameUi.Muted,
                fontSize = 9.sp,
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
