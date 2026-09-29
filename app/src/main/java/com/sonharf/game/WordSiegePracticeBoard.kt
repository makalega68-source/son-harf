package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.draw.drawBehind
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
private val PracticeSiegeTile get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.ivory else Color(0xFFF7E3A6)
private val PracticeSiegeTileBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bevel else Color(0xFFC9A560)
internal val PracticeSiegeBoardSurface = Color(0xFFD5CEBD)
internal val PracticeSiegeNeutral get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.empty else Color(0xFFF3EEDF)
private val PracticeSiegeEmpty get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.empty else Color(0xFFF3EEDF)
private val PracticeSiegeMine get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.mine else Color(0xFF5FAF73)
private val PracticeSiegeRival get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.rival else Color(0xFFD9776F)
private val PracticeSiegeMineBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.mine else Color(0xFF7FC391)
private val PracticeSiegeRivalBorder get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.rival else Color(0xFFEB9E97)
private val PracticeSiegeThreat = Color(0xFFD8903D)
private val PracticeSiegeLightTileText = Color(0xFF4A3217)
private val PracticeZoneWatch get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus2H else Color(0xFFE0F3EF)
private val PracticeZoneCritical get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus3H else Color(0xFFC3E7DF)
private val PracticeZoneFort get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus2K else Color(0xFFFFF0D3)
private val PracticeZoneSiege get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus3K else Color(0xFFF6D596)
private val PracticeZoneCrown get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonus4K else Color(0xFF24304B)
private val PracticeZoneReward get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonusStar else Color(0xFFFBEBB5)
private val PracticeLastMove = Color(0xFFE0A82E)
private val PracticeDefinitionBadge = Color(0xFF5C8299)
private val PracticeSiegeBonusLabel get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.bonusLabel else Color(0xFF3F4A5A)

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
    /** Cells of the mascot's hint move: it flies there while it says the word. */
    hintCells: List<Int> = emptyList(),
    onViewportModeChange: (WordSiegeBoardViewportMode) -> Unit = {},
    /** Finger dragging of tiles (rack → board, board → board/rack). */
    tileDrag: WordSiegeTileDrag? = null,
    onTileDrop: (rackIndex: Int, fromCell: Int?, target: Int?) -> Unit = { _, _, _ -> },
    /** The pending tiles form a valid move: a green check sits on the last tile. */
    pendingValid: Boolean = false,
    /** What the pending move scores, shown above the word (null hides it). */
    pendingScore: Int? = null,
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

    /** Double tap anywhere: zooming in centres on the tapped cell. */
    fun toggleModeAt(index: Int) {
        val nextMode = mode.toggle()
        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {
            closeScale = WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE
            closePan = wordSiegeCenteredClosePan(
                index = index,
                viewportWidthPx = viewport.width.toFloat(),
                viewportHeightPx = viewport.height.toFloat(),
                boardWidthPx = boardPx,
                cellSizePx = tilePx,
                scale = closeScale,
            )
        }
        mode = nextMode
        onViewportModeChange(nextMode)
    }

    WordSiegeRegisterBoardHitTest(tileDrag, viewportOriginInWindow, viewport, transform, tilePx)
    // Only the hovered cell matters, so the board recomposes when it changes, not every finger move.
    val dragHover by remember(tileDrag) { derivedStateOf { tileDrag?.hoverCell } }
    val hiddenCells: Set<Int> = buildSet {
        tileDrag?.fromCell?.let { add(it) }
        tileDrag?.flyingCells?.let { addAll(it) }
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
        color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frame else Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frameEdge else Color(0xFFD2DBE5)),
        shadowElevation = if (WordSiegeWalnutIvory.enabled) 7.dp else 12.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.boardGrain else Brush.linearGradient(listOf(Color(0xFFEFE7D2), Color(0xFFECE3CB), Color(0xFFEFE7D2), Color(0xFFE9DFC5))))
                .clipToBounds()
                .onGloballyPositioned {
                    viewport = it.size
                    viewportOriginInWindow = it.localToWindow(Offset.Zero)
                }
                .wordSiegeMascotTouchWatcher(mascotTouches)
                .wordSiegeBoardDoubleTap(
                    key = Pair(mode, transform),
                    cellAt = { wordSiegeCellAt(it, transform, tilePx) },
                    onDoubleTap = { toggleModeAt(it) },
                )
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
                // The mascot's answer tiles pulse gold while they sit on the board, so it is clear
                // which letters the hint placed.
                val hintSet = if (hint != null && hintCells.isNotEmpty() && placements.keys.containsAll(hintCells)) hintCells.toSet() else emptySet()
                // The pulse runs only while a hint is on the board and is read at draw time, so the
                // board is not recomposed every frame.
                val hintPulse = remember { Animatable(.35f) }
                LaunchedEffect(hintSet.isNotEmpty()) {
                    if (hintSet.isNotEmpty()) {
                        hintPulse.animateTo(1f, androidx.compose.animation.core.infiniteRepeatable(tween(650), androidx.compose.animation.core.RepeatMode.Reverse))
                    } else hintPulse.snapTo(.35f)
                }
                repeat(WordSiegeBoardSpec.Size) { row ->
                    Row {
                        repeat(WordSiegeBoardSpec.Size) { column ->
                            val index = WordSiegeBoardSpec.index(row, column)
                            val placedRackIndex = placements[index]
                            val pendingRackIndex = placedRackIndex?.takeIf { index !in hiddenCells }
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
                                hintGlow = if (index in hintSet) ({ hintPulse.value }) else null,
                                dropTarget = dragHover == index && (cell.letter == null),
                                // Keyed on the placed tile, not the shown one: the tile hides while it is being
                                // carried and the gesture must survive that.
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
                cells = placements.keys.filter { it !in hiddenCells }.takeIf { it.size == placements.size }.orEmpty(),
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
                    // A requested hint: fly to the exact spot of the answer word.
                    hint != null && hintCells.isNotEmpty() && placements.keys.containsAll(hintCells) ->
                        wordSiegeMascotCellVisit(
                            key = "hintmove:${hint.first}",
                            indices = hintCells,
                            transform = transform,
                            cellSizePx = tilePx,
                            viewportWidthPx = viewport.width.toFloat(),
                            viewportHeightPx = viewport.height.toFloat(),
                            kind = WordSiegeMascotVisitKind.ANSWER,
                        )
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
    hintGlow: (() -> Float)? = null,
    dropTarget: Boolean = false,
    dragSource: Modifier = Modifier,
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
        pending -> if (WordSiegeWalnutIvory.enabled) territory else PracticeSiegeTile
        letter != null -> territory
        zoneSurface != null -> zoneSurface
        else -> PracticeSiegeEmpty
    }
    val displayCellColor = if (pending && !WordSiegeWalnutIvory.enabled) Color(0xFFE3D6B0) else cellColor
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
            .then(dragSource)
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
            .drawBehind {
                // Hint glow: a soft gold halo around the answer tile.
                if (hintGlow != null) {
                    drawRoundRect(
                        color = Color(0xFFFFD54F).copy(alpha = .55f * hintGlow()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
                    )
                }
            }
            .padding(regionGap)
            .graphicsLayer { scaleX = if (WordSiegeWalnutIvory.enabled) pressScale else 1f; scaleY = if (WordSiegeWalnutIvory.enabled) pressScale else 1f }
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
                width = if (dropTarget) 3.dp else if (hintGlow != null) 2.2.dp else if (lastMoveHighlight > 0f) 1.7.dp else .45.dp,
                color = if (dropTarget) {
                    Color(0xFF2FB36A)
                } else if (hintGlow != null) {
                    Color(0xFFFFE082)
                } else if (lastMoveHighlight > 0f) {
                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)
                } else if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.emptyEdge else Color(0xFFCDBF9F),
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (WordSiegeWalnutIvory.enabled && letter != null) {
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
                    .align(if (WordSiegeWalnutIvory.enabled && owner != myOwner) Alignment.TopEnd else Alignment.TopStart)
                    .padding(if (WordSiegeWalnutIvory.enabled) 3.dp else 4.dp)
                    .size(if (WordSiegeWalnutIvory.enabled) 7.dp else 6.dp)
                    .clip(if (WordSiegeWalnutIvory.enabled && owner != myOwner) RoundedCornerShape(1.dp) else CircleShape)
                    .background(if (owner == myOwner) PracticeSiegeMineBorder else PracticeSiegeRivalBorder),
            )
        }

        if (letter != null) {
            Text(
                letter,
                color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.ink else if (!pending && owner != 0) Color.White else PracticeSiegeLightTileText,
                fontSize = if (overview) 24.sp else 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = if (overview) .10.sp else .25.sp,
            )
            Text(
                practiceLetterValue(letter),
                color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.secondaryInk else (if (!pending && owner != 0) Color.White else PracticeSiegeLightTileText).copy(alpha = .78f),
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
            val label = WordSiegeBoardSpec.displayBonusLabel(activeZone, !SonHarfUiState.isEnglish)
            WordSiegeBonusMark(activeZone, label, overview, PracticeSiegeBonusLabel, WordSiegeBoardAccessibility.BoardBonus)
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
        modifier = modifier.height(48.dp).graphicsLayer { scaleX = if (WordSiegeWalnutIvory.enabled) pressScale else 1f; scaleY = if (WordSiegeWalnutIvory.enabled) pressScale else 1f }
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, enabled = enabled),
        color = when {
            used -> WordSiegeGameUi.SurfaceSoft
            WordSiegeWalnutIvory.enabled -> WordSiegeWalnutIvory.ivory
            selected -> Color(0xFFD6C38D)
            else -> Color(0xFFE3D6B0)
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (WordSiegeWalnutIvory.enabled) (if (selected) WordSiegeWalnutIvory.selection else WordSiegeWalnutIvory.bevel) else (if (selected) PracticeSiegeMineBorder else PracticeSiegeTileBorder.copy(alpha = .7f))),
        shadowElevation = if (WordSiegeWalnutIvory.enabled && pressed) 0.dp else if (selected) 4.dp else 2.dp,
    ) {
        Box(if (WordSiegeWalnutIvory.enabled) Modifier.background(WordSiegeWalnutIvory.tile) else Modifier, contentAlignment = Alignment.Center) {
            Text(
                letter.toString(),
                color = if (WordSiegeWalnutIvory.enabled) (if (used) WordSiegeWalnutIvory.ink.copy(alpha = .35f) else WordSiegeWalnutIvory.ink) else (if (used) WordSiegeGameUi.Muted.copy(alpha = .45f) else PracticeSiegeLightTileText),
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = .35.sp,
            )
            Text(
                practiceLetterValue(letter.toString()),
                color = if (WordSiegeWalnutIvory.enabled) (if (used) WordSiegeWalnutIvory.secondaryInk.copy(alpha = .45f) else WordSiegeWalnutIvory.secondaryInk) else (if (used) WordSiegeGameUi.Muted.copy(alpha = .55f) else PracticeSiegeLightTileText.copy(alpha = .72f)),
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

internal fun practiceLetterValue(letter: String): String = when (letter) {
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
