package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.WordSiegeCellDto
import kotlinx.coroutines.delay

private val PracticeSiegeCellSize = 52.dp
private val PracticeSiegeTile = WordSiegeWalnutIvory.ivory
private val PracticeSiegeTileBorder = WordSiegeWalnutIvory.bevel
internal val PracticeSiegeBoardSurface = Color(0xFFD5CEBD)
internal val PracticeSiegeNeutral = WordSiegeWalnutIvory.empty
private val PracticeSiegeEmpty = WordSiegeWalnutIvory.empty
private val PracticeSiegeMine = WordSiegeWalnutIvory.mine
private val PracticeSiegeRival = WordSiegeWalnutIvory.rival
private val PracticeSiegeMineBorder = WordSiegeWalnutIvory.mine
private val PracticeSiegeRivalBorder = WordSiegeWalnutIvory.rival
private val PracticeSiegeThreat = Color(0xFFD8903D)
private val PracticeSiegeLightTileText = Color(0xFF4A3217)
private val PracticeZoneWatch = Color(0xFFCFE6F5)
private val PracticeZoneCritical = Color(0xFFF6D3E2)
private val PracticeZoneFort = Color(0xFFD6ECCB)
private val PracticeZoneSiege = Color(0xFFF8DCC3)
private val PracticeZoneCrown = Color(0xFFE2D6F2)
private val PracticeZoneReward = Color(0xFFFBEBB5)
private val PracticeLastMove = Color(0xFFE0A82E)
private val PracticeDefinitionBadge = Color(0xFF5C8299)
private val PracticeSiegeBonusLabel = Color(0xFF3F4A5A)

internal data class PracticeResolvedWord(
    val word: String,
    val badgeIndex: Int,
)

@Composable
internal fun WordSiegePracticeBoard(
    board: List<WordSiegeCellDto>,
    rack: String,
    placements: Map<Int, Int>,
    myOwner: Int,
    enabled: Boolean,
    moveEventKey: Int? = null,
    lastMoveMine: Boolean = false,
    moveScore: Int = 0,
    capturedCells: Int = 0,
    opponentCaptured: Int = 0,
    moveCell: Int? = null,
    resolvedIndices: Set<Int> = emptySet(),
    captureEffect: WordSiegeCaptureEffect? = null,
    language: String = SonHarfUiState.language,
    modifier: Modifier = Modifier,
    mascotSignal: WordSiegeMascotSignal? = null,
    mascotOutcome: WordSiegeMascotOutcome? = null,
    playerName: String? = null,
    playerGender: String? = null,
    /** A mascot hint to show now (key, text). */
    hint: Pair<Int, String>? = null,
    onViewportModeChange: (WordSiegeBoardViewportMode) -> Unit = {},
    onCell: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tilePx = with(density) { PracticeSiegeCellSize.toPx() }
    val boardPx = tilePx * WordSiegeBoardSpec.Size
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var closePan by remember { mutableStateOf(Offset.Zero) }
    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE) }
    var initialized by remember { mutableStateOf(false) }
    var viewportOriginInWindow by remember { mutableStateOf(Offset.Unspecified) }
    val mascotTouches = remember { WordSiegeMascotTouchState() }
    var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }
    val transform by remember(mode, viewport, boardPx, closePan, closeScale) {
        derivedStateOf {
            wordSiegeBoardTransform(
                mode = mode,
                viewportWidthPx = viewport.width.toFloat(),
                viewportHeightPx = viewport.height.toFloat(),
                boardWidthPx = boardPx,
                closeScale = closeScale,
                closePan = closePan,
            )
        }
    }
    var consumedHighlightKey by remember { mutableStateOf(moveEventKey) }
    var highlightedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val highlightAlpha = remember { Animatable(0f) }

    LaunchedEffect(moveEventKey, resolvedIndices) {
        val key = moveEventKey
        if (key != null && key != consumedHighlightKey) {
            consumedHighlightKey = key
            highlightedIndices = resolvedIndices.filter(WordSiegeBoardSpec::isValidIndex).toSet()
            highlightAlpha.snapTo(0f)
            highlightAlpha.animateTo(1f, tween(WORD_SIEGE_LAST_MOVE_ENTER_MS))
            delay(WORD_SIEGE_LAST_MOVE_HOLD_MS.toLong())
            highlightAlpha.animateTo(0.42f, tween(WORD_SIEGE_LAST_MOVE_EXIT_MS))
        }
    }

    val resolvedWord = remember(board, resolvedIndices) { resolvePracticeLastWord(board, resolvedIndices) }
    var definitionWord by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(resolvedWord?.word) {
        if (definitionWord != null && definitionWord != resolvedWord?.word) definitionWord = null
    }

    val actionVfxEvents = emptyList<PurchasedBoardVfxEvent>()

    fun clampClosePan(candidate: Offset): Offset = clampWordSiegeBoardPan(
        candidate,
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        boardPx,
        closeScale,
    )

    fun centerClose(): Offset = wordSiegeCenteredClosePan(
        index = WordSiegeBoardSpec.CenterIndex,
        viewportWidthPx = viewport.width.toFloat(),
        viewportHeightPx = viewport.height.toFloat(),
        boardWidthPx = boardPx,
        cellSizePx = tilePx,
        scale = closeScale,
    )

    fun toggleMode() {
        val nextMode = mode.toggle()
        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {
            closeScale = WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE
            closePan = centerClose()
        }
        mode = nextMode
        onViewportModeChange(nextMode)
    }

    LaunchedEffect(viewport, boardPx) {
        if (!initialized && viewport.width > 0 && viewport.height > 0) {
            closePan = centerClose()
            initialized = true
        } else if (initialized) {
            closePan = clampClosePan(closePan)
        }
    }

    Surface(
        modifier = modifier,
        color = WordSiegeWalnutIvory.frame,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, WordSiegeWalnutIvory.frameEdge),
        shadowElevation = 7.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(WordSiegeWalnutIvory.boardGrain)
                .clipToBounds()
                .onGloballyPositioned {
                    viewport = it.size
                    viewportOriginInWindow = it.localToWindow(Offset.Zero)
                }
                .wordSiegeMascotTouchWatcher(mascotTouches)
                .pointerInput(mode, viewport, boardPx, closeScale) {
                    if (mode == WordSiegeBoardViewportMode.CLOSE) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = closeScale
                            val newScale = (oldScale * zoom).coerceIn(WORD_SIEGE_PRACTICE_MIN_SCALE, WORD_SIEGE_PRACTICE_MAX_SCALE)
                            val ratio = if (oldScale > 0f) newScale / oldScale else 1f
                            val candidate = centroid + (closePan - centroid) * ratio + pan
                            closeScale = newScale
                            closePan = clampWordSiegeBoardPan(
                                candidate,
                                viewport.width.toFloat(),
                                viewport.height.toFloat(),
                                boardPx,
                                newScale,
                            )
                        }
                    }
                },
        ) {
            Column(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(PracticeSiegeCellSize * WordSiegeBoardSpec.Size)
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
                            val cell = board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) }
                            WordSiegePracticeBoardCell(
                                cell = cell,
                                pendingLetter = pendingRackIndex?.let(rack::getOrNull),
                                pending = pendingRackIndex != null,
                                myOwner = myOwner,
                                enabled = enabled,
                                overview = mode == WordSiegeBoardViewportMode.FIT,
                                threatened = false,
                                lastMoveHighlight = if (index in highlightedIndices) highlightAlpha.value else 0f,
                                showDefinitionBadge = resolvedWord?.badgeIndex == index,
                                onDefinitionClick = { resolvedWord?.word?.let { definitionWord = it } },
                                onClick = { onCell(index) },
                                onDoubleClick = ::toggleMode,
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
            WordSiegeMascotCompanion(
                anchors = WordSiegeBoardMascotPerches,
                mascotSize = 76.dp,
                moveId = moveEventKey?.toLong(),
                lastMoveMine = lastMoveMine,
                playerTurn = enabled,
                modifier = Modifier.matchParentSize().padding(3.dp),
                moveScore = moveScore,
                capturedCells = capturedCells,
                opponentCaptured = opponentCaptured,
                moveCell = moveCell,
                pendingCells = placements.keys,
                signal = mascotSignal,
                outcome = mascotOutcome,
                playerName = playerName,
                playerGender = playerGender,
                touches = mascotTouches,
                hint = hint,
                visit = when {
                    // Fresh territory after the player's own strong move.
                    moveEventKey != null && lastMoveMine && capturedCells >= 2 && moveCell != null ->
                        wordSiegeMascotCellVisit(
                            key = "cap:$moveEventKey",
                            indices = listOf(moveCell),
                            transform = transform,
                            cellSizePx = tilePx,
                            viewportWidthPx = viewport.width.toFloat(),
                            viewportHeightPx = viewport.height.toFloat(),
                            kind = WordSiegeMascotVisitKind.CAPTURE,
                        )
                    // Practice only: a gentle pointer to a playable bonus square if the player is stuck.
                    enabled && placements.isEmpty() -> practiceBonusHint(board)?.let { index ->
                        wordSiegeMascotCellVisit(
                            key = "hint:$index:${moveEventKey ?: 0}",
                            indices = listOf(index),
                            transform = transform,
                            cellSizePx = tilePx,
                            viewportWidthPx = viewport.width.toFloat(),
                            viewportHeightPx = viewport.height.toFloat(),
                            kind = WordSiegeMascotVisitKind.HINT,
                        )
                    }
                    else -> null
                },
            )
        }
    }

    definitionWord?.let { word ->
        WordDefinitionDialog(
            word = word,
            language = language,
            onDismiss = { definitionWord = null },
        )
    }
}

@Composable
private fun WordSiegePracticeBoardCell(
    cell: WordSiegeCellDto,
    pendingLetter: Char?,
    pending: Boolean,
    myOwner: Int,
    enabled: Boolean,
    overview: Boolean,
    threatened: Boolean,
    lastMoveHighlight: Float,
    showDefinitionBadge: Boolean,
    onDefinitionClick: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val owner = if (pending) myOwner else cell.owner
    val territory = when {
        owner == 0 -> PracticeSiegeNeutral
        owner == myOwner -> PracticeSiegeMine
        else -> PracticeSiegeRival
    }
    val letter = pendingLetter?.toString() ?: cell.letter
    val canPlace = enabled && (cell.letter == null || pending)
    val activeZone = if (letter == null && !cell.bonusUsed) cell.bonus else null
    val zoneSurface = when (activeZone) {
        "2H" -> PracticeZoneWatch
        "3H" -> PracticeZoneCritical
        "2K" -> PracticeZoneFort
        "3K" -> PracticeZoneSiege
        WordSiegeBoardSpec.CenterBonus -> PracticeZoneCrown
        WordSiegeBoardSpec.StarBonus -> PracticeZoneReward
        else -> null
    }

    // Territory is the primary visual layer. Strategic-zone tint is only dominant on neutral cells.
    val cellColor = when {
        pending -> territory
        letter != null -> territory
        zoneSurface != null -> zoneSurface
        else -> PracticeSiegeEmpty
    }
    val displayCellColor = cellColor
    val borderColor = when {
        threatened && owner != 0 -> PracticeSiegeThreat
        pending -> PracticeSiegeTileBorder
        owner == myOwner -> PracticeSiegeMineBorder
        owner != 0 -> PracticeSiegeRivalBorder
        activeZone == WordSiegeBoardSpec.CenterBonus || activeZone == WordSiegeBoardSpec.StarBonus -> Color(0xFFB07F1E)
        activeZone != null -> WordSiegeGameUi.Border.copy(alpha = .8f)
        else -> WordSiegeGameUi.Border.copy(alpha = .45f)
    }
    val regionGap = 1.25.dp
    val cellInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by cellInteraction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .94f else 1f, tween(if (pressed) 65 else 150), label = "practice cell press")

    Box(
        Modifier
            .size(PracticeSiegeCellSize)
            .combinedClickable(
                interactionSource = cellInteraction,
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
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.White, .18f),
                        displayCellColor,
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.Black, .07f),
                    )
                )
            )
            .border(
                width = if (lastMoveHighlight > 0f) 1.7.dp else .45.dp,
                color = if (lastMoveHighlight > 0f) {
                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)
                } else WordSiegeWalnutIvory.emptyEdge,
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            Box(
                Modifier.matchParentSize()
                    .padding(if (owner != 0) 4.dp else .75.dp)
                    .shadow(1.5.dp, RoundedCornerShape(7.dp))
                    .clip(RoundedCornerShape(7.dp))
                    .background(WordSiegeWalnutIvory.tile)
                    .border(
                        if (pending) 1.6.dp else .65.dp,
                        if (pending) WordSiegeWalnutIvory.selection else WordSiegeWalnutIvory.bevel,
                        RoundedCornerShape(7.dp),
                    ),
            )
        }
        if (lastMoveHighlight > 0f) {
            Box(Modifier.matchParentSize().background(PracticeLastMove.copy(alpha = .045f * lastMoveHighlight)))
        }

        if (owner != 0 && !pending) {
            Box(
                Modifier
                    .align(if (owner == myOwner) Alignment.TopStart else Alignment.TopEnd)
                    .padding(3.dp)
                    .size(7.dp)
                    .clip(if (owner == myOwner) CircleShape else RoundedCornerShape(1.dp))
                    .background(if (owner == myOwner) PracticeSiegeMineBorder else PracticeSiegeRivalBorder),
            )
        }

        if (letter != null) {
            Text(
                letter,
                color = WordSiegeWalnutIvory.ink,
                fontSize = if (overview) 24.sp else 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = if (overview) .10.sp else .25.sp,
            )
            Text(
                practiceLetterValue(letter),
                color = WordSiegeWalnutIvory.secondaryInk,
                fontSize = WordSiegeBoardAccessibility.BoardLetterPoint,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
            if (showDefinitionBadge && !pending) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomEnd = 5.dp))
                        .background(PracticeDefinitionBadge)
                        .clickable(onClick = onDefinitionClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        } else if (activeZone != null) {
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    val label = WordSiegeBoardSpec.displayBonusLabel(activeZone, !SonHarfUiState.isEnglish)
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
                color = PracticeSiegeBonusLabel,
                fontSize = WordSiegeBoardAccessibility.BoardBonus,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp,
                fontWeight = if (overview) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun WordSiegePracticeRackTile(
    letter: Char,
    selected: Boolean,
    used: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .95f else 1f, tween(if (pressed) 65 else 150), label = "practice rack press")
    Surface(
        modifier = modifier.height(48.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, enabled = enabled),
        color = when {
            used -> WordSiegeGameUi.SurfaceSoft
            else -> WordSiegeWalnutIvory.ivory
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) WordSiegeWalnutIvory.selection else WordSiegeWalnutIvory.bevel),
        shadowElevation = if (pressed) 0.dp else if (selected) 4.dp else 2.dp,
    ) {
        Box(Modifier.background(WordSiegeWalnutIvory.tile), contentAlignment = Alignment.Center) {
            Text(
                letter.toString(),
                color = if (used) WordSiegeWalnutIvory.ink.copy(alpha = .35f) else WordSiegeWalnutIvory.ink,
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = .35.sp,
            )
            Text(
                practiceLetterValue(letter.toString()),
                color = if (used) WordSiegeWalnutIvory.secondaryInk.copy(alpha = .45f) else WordSiegeWalnutIvory.secondaryInk,
                fontSize = WordSiegeBoardAccessibility.RackPoint,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        }
    }
}

internal fun resolvePracticeLastWord(
    board: List<WordSiegeCellDto>,
    resolvedIndices: Set<Int>,
): PracticeResolvedWord? {
    val placed = resolvedIndices
        .filter(WordSiegeBoardSpec::isValidIndex)
        .filter { board.getOrNull(it)?.letter?.isNotBlank() == true }
        .sorted()
    if (placed.isEmpty()) return null

    val anchor = placed.first()
    val allSameRow = placed.all { WordSiegeBoardSpec.row(it) == WordSiegeBoardSpec.row(anchor) }
    val allSameColumn = placed.all { WordSiegeBoardSpec.column(it) == WordSiegeBoardSpec.column(anchor) }

    fun contiguousCells(startIndex: Int, delta: Int): List<Int> {
        var start = startIndex
        while (true) {
            val previous = start - delta
            val crossedRow = delta == WordSiegeBoardSpec.HorizontalDelta &&
                WordSiegeBoardSpec.isValidIndex(previous) &&
                WordSiegeBoardSpec.row(previous) != WordSiegeBoardSpec.row(start)
            if (!WordSiegeBoardSpec.isValidIndex(previous) || crossedRow || board.getOrNull(previous)?.letter.isNullOrBlank()) break
            start = previous
        }

        val result = mutableListOf<Int>()
        var current = start
        while (WordSiegeBoardSpec.isValidIndex(current) && board.getOrNull(current)?.letter?.isNotBlank() == true) {
            result += current
            val next = current + delta
            if (!WordSiegeBoardSpec.isValidIndex(next)) break
            if (delta == WordSiegeBoardSpec.HorizontalDelta && WordSiegeBoardSpec.row(next) != WordSiegeBoardSpec.row(current)) break
            current = next
        }
        return result
    }

    val horizontalCells = contiguousCells(anchor, WordSiegeBoardSpec.HorizontalDelta)
    val verticalCells = contiguousCells(anchor, WordSiegeBoardSpec.VerticalDelta)
    val wordCells = when {
        placed.size > 1 && allSameRow -> horizontalCells
        placed.size > 1 && allSameColumn -> verticalCells
        horizontalCells.size >= 2 -> horizontalCells
        verticalCells.size >= 2 -> verticalCells
        else -> return null
    }
    if (wordCells.size < 2) return null

    val word = wordCells.joinToString("") { board.getOrNull(it)?.letter.orEmpty() }
    if (word.length < 2) return null
    return PracticeResolvedWord(word = word, badgeIndex = placed.maxOrNull() ?: anchor)
}

private fun practiceCellThreatened(board: List<WordSiegeCellDto>, index: Int, owner: Int): Boolean {
    if (owner == 0 || !WordSiegeBoardSpec.isValidIndex(index)) return false
    val row = WordSiegeBoardSpec.row(index)
    val column = WordSiegeBoardSpec.column(index)
    val neighbours = buildList {
        if (row > 0) add(WordSiegeBoardSpec.index(row - 1, column))
        if (row < WordSiegeBoardSpec.Size - 1) add(WordSiegeBoardSpec.index(row + 1, column))
        if (column > 0) add(WordSiegeBoardSpec.index(row, column - 1))
        if (column < WordSiegeBoardSpec.Size - 1) add(WordSiegeBoardSpec.index(row, column + 1))
    }
    return neighbours.any { neighbour ->
        val neighbourOwner = board.getOrNull(neighbour)?.owner ?: 0
        neighbourOwner != 0 && neighbourOwner != owner
    }
}

private fun practiceLetterValue(letter: String): String = when (letter) {
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

/** The most valuable empty bonus square that touches an existing letter, if any. */
private fun practiceBonusHint(board: List<WordSiegeCellDto>): Int? {
    val rank = mapOf("3K" to 4, "3H" to 3, "2K" to 2, "2H" to 1)
    return board.indices
        .filter { index ->
            val cell = board[index]
            val bonus = cell.bonus ?: WordSiegeBoardSpec.bonusAt(index)
            cell.letter == null && !cell.bonusUsed && bonus in rank && practiceHasLetterNeighbour(board, index)
        }
        .maxByOrNull { rank[board[it].bonus ?: WordSiegeBoardSpec.bonusAt(it)] ?: 0 }
}

private fun practiceHasLetterNeighbour(board: List<WordSiegeCellDto>, index: Int): Boolean {
    val row = WordSiegeBoardSpec.row(index)
    val column = WordSiegeBoardSpec.column(index)
    return listOf(row - 1 to column, row + 1 to column, row to column - 1, row to column + 1).any { (r, c) ->
        r in 0 until WordSiegeBoardSpec.Size && c in 0 until WordSiegeBoardSpec.Size &&
            board.getOrNull(WordSiegeBoardSpec.index(r, c))?.letter != null
    }
}
