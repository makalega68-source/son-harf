package com.sonharf.game

import androidx.compose.foundation.BorderStroke
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
    val myEarnedCubePoints = WordSiegeFinalRules.earnedCubePoints(moves, me)
    val rivalEarnedCubePoints = WordSiegeFinalRules.earnedCubePoints(moves, opponentId)
    val myTargetScore = WordSiegeFinalRules.netScore(panSiegeWordScore(game, myOwner), myEarnedCubePoints, rivalEarnedCubePoints)
    val rivalTargetScore = WordSiegeFinalRules.netScore(panSiegeWordScore(game, rivalOwner), rivalEarnedCubePoints, myEarnedCubePoints)
    var displayedMyScore by remember(game.id) { mutableIntStateOf(myTargetScore) }
    var displayedRivalScore by remember(game.id) { mutableIntStateOf(rivalTargetScore) }
    var displayedCurrentPlayerId by remember(game.id) { mutableStateOf(game.currentPlayerId) }
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
        WordSiegePracticeScreen(onExit = { fallbackPracticeActive = false })
        return
    }

    LaunchedEffect(myTargetScore, rivalTargetScore, game.currentPlayerId) {
        while (displayedMyScore != myTargetScore || displayedRivalScore != rivalTargetScore) {
            displayedMyScore += (myTargetScore - displayedMyScore).coerceIn(-1, 1)
            displayedRivalScore += (rivalTargetScore - displayedRivalScore).coerceIn(-1, 1)
            delay(28)
        }
        displayedCurrentPlayerId = game.currentPlayerId
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
                earnedCubePoints = myEarnedCubePoints,
                areaCount = panSiegeAreaCount(game, myOwner),
                accent = MainUi.Green,
                active = displayedCurrentPlayerId == me,
                modifier = Modifier.weight(1f),
            )
            PanSiegePlayerCard(
                profile = opponent,
                fallbackName = if (game.status == "waiting") sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"),
                score = displayedRivalScore,
                earnedCubePoints = rivalEarnedCubePoints,
                areaCount = panSiegeAreaCount(game, rivalOwner),
                accent = MainUi.Red,
                active = displayedCurrentPlayerId == opponentId,
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
                Text(sh("SOHBET", "CHAT"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
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
                Text(sh("PES ET", "FORFEIT"), color = MainUi.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }

        if (game.status == "playing") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (placements.isNotEmpty()) {
                    Text(readyFeedback.message, color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sh(
                            "Alan +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS} • kelime puanı OYNA ile doğrulanır",
                            "Area +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS} • word score is verified on PLAY",
                        ),
                        color = MainUi.Muted,
                        fontSize = 8.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                } else Spacer(Modifier.weight(1f))
                Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = MainUi.Muted, fontSize = 8.sp)
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
                    Text(sh("GERİ AL", "UNDO"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = { shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1 },
                    enabled = canAct && rack.length > 1,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onPass,
                    enabled = canAct,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) { Text(sh("PAS", "PASS"), fontSize = 10.sp, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onExchange,
                    enabled = canAct && game.bag.isNotEmpty(),
                    modifier = Modifier.weight(1.15f).height(44.dp),
                    border = BorderStroke(1.dp, SiegePurple),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 9.sp, fontWeight = FontWeight.Black) }
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
                    else Text(sh("OYNA", "PLAY"), fontSize = 12.sp, fontWeight = FontWeight.Black)
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
    var dragging by remember(gameId) { mutableStateOf(false) }
    var initialized by remember(gameId) { mutableStateOf(false) }
    var observedMoveId by remember(gameId) { mutableStateOf(lastMove?.id) }
    var viewportMode by remember(gameId) { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }

    val transform by remember(viewportMode, viewport, boardPx, closePan) {
        derivedStateOf {
            wordSiegeBoardTransform(
                mode = viewportMode,
                viewportWidthPx = viewport.width.toFloat(),
                viewportHeightPx = viewport.height.toFloat(),
                boardWidthPx = boardPx,
                closePan = closePan,
            )
        }
    }

    fun clampClosePan(candidate: Offset): Offset = clampWordSiegeBoardPan(
        candidate,
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        boardPx,
        1f,
    )

    fun centerCloseOn(index: Int): Offset =
        wordSiegeCenteredClosePan(
            index = index,
            viewportWidthPx = viewport.width.toFloat(),
            viewportHeightPx = viewport.height.toFloat(),
            boardWidthPx = boardPx,
            cellSizePx = tilePx,
        )

    fun toggleViewport() {
        val nextMode = viewportMode.toggle()
        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {
            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)
        }
        viewportMode = nextMode
    }

    LaunchedEffect(viewport, gameId, boardPx) {
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
                            PanSiegeBoardCell(
                                cell = board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) },
                                pendingLetter = placements[index]?.let(rack::getOrNull),
                                pending = placements.containsKey(index),
                                myOwner = myOwner,
                                enabled = enabled,
                                size = PanSiegeCellSize,
                                onClick = { onCell(index) },
                                onDoubleClick = ::toggleViewport,
                            )
                        }
                    }
                }
            }

            SmallFloatingActionButton(
                onClick = {
                    viewportMode = WordSiegeBoardViewportMode.CLOSE
                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)
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
        owner == myOwner -> MainUi.Green
        owner != 0 -> MainUi.Red
        else -> MainUi.Border
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
            color = if (pending) PanSiegeTile.copy(alpha = .76f) else Color.Transparent,
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(if (pending) 2.dp else 1.dp, border.copy(alpha = .78f)),
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
        Text(message, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = MainUi.Text, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                size = 36.dp,
                accent = accent,
                visible = profile?.avatarVisibility != "hidden",
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(profile?.displayName ?: fallbackName, color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$score", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    sh("Küp +$earnedCubePoints • Alan $areaCount • küp başına ±2", "Cubes +$earnedCubePoints • Area $areaCount • ±2 per cube"),
                    color = MainUi.Muted,
                    fontSize = 7.sp,
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
