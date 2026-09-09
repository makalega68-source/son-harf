package com.sonharf.game

import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import com.sonharf.game.data.WordSiegeCellDto

private val PracticeSiegeCellSize = 52.dp
private val PracticeSiegeTile = Color(0xFFFFE3A5)
private val PracticeSiegeTileBorder = Color(0xFFD99818)
internal val PracticeSiegeBoardSurface = Color(0xFFD9E4E7)
internal val PracticeSiegeNeutral = Color(0xFFF8FAF9)
private val PracticeSiegeEmpty = Color(0xFFFFF8EA)
private val PracticeSiegeMine = Color(0xFF65B58A)
private val PracticeSiegeRival = Color(0xFFD98286)
private val PracticeSiegeLightTileText = Color(0xFF2F2A1F)
private val PracticeBonus2H = Color(0xFFDCEFF8)
private val PracticeBonus3H = Color(0xFFDCEEDC)
private val PracticeBonus2K = Color(0xFFE9E0F2)
private val PracticeBonus3K = Color(0xFFDECBE9)
private val PracticeBonus4K = Color(0xFFF0C75A)
private val PracticeBonusStar = Color(0xFFF6B94A)
private val PracticeLastMove = Color(0xFFF1C75B)

@Composable
internal fun WordSiegePracticeBoard(
    board: List<WordSiegeCellDto>,
    rack: String,
    placements: Map<Int, Int>,
    myOwner: Int,
    enabled: Boolean,
    moveEventKey: Int? = null,
    resolvedIndices: Set<Int> = emptySet(),
    modifier: Modifier = Modifier,
    onCell: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tilePx = with(density) { PracticeSiegeCellSize.toPx() }
    val boardPx = tilePx * WordSiegeBoardSpec.Size
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var closePan by remember { mutableStateOf(Offset.Zero) }
    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_PRACTICE_CLOSE_SCALE) }
    var initialized by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }
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
            // Keep the most recently placed word subtly visible until the next move.
            highlightAlpha.animateTo(0.42f, tween(WORD_SIEGE_LAST_MOVE_EXIT_MS))
        }
    }

    val actionVfxEvents = emptyList<PurchasedBoardVfxEvent>()

    fun clampClosePan(candidate: Offset): Offset = clampWordSiegeBoardPan(
        candidate,
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        boardPx,
        closeScale,
    )

    fun centerClose(): Offset =
        wordSiegeCenteredClosePan(
            index = WordSiegeBoardSpec.CenterIndex,
            viewportWidthPx = viewport.width.toFloat(),
            viewportHeightPx = viewport.height.toFloat(),
            boardWidthPx = boardPx,
            cellSizePx = tilePx,
            scale = closeScale,
        )

    fun toggleMode() {
        val nextMode = mode.toggle()
        if (nextMode == WordSiegeBoardViewportMode.CLOSE) closePan = centerClose()
        mode = nextMode
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
        color = PracticeSiegeBoardSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MainUi.Border.copy(alpha = .70f)),
        shadowElevation = 1.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .clipToBounds()
                .onGloballyPositioned { viewport = it.size }
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
                            WordSiegePracticeBoardCell(
                                cell = board.getOrElse(index) { WordSiegeCellDto() },
                                pendingLetter = pendingRackIndex?.let(rack::getOrNull),
                                pending = pendingRackIndex != null,
                                myOwner = myOwner,
                                enabled = enabled,
                                lastMoveHighlight = if (index in highlightedIndices) highlightAlpha.value else 0f,
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
        }
    }
}

@Composable
private fun WordSiegePracticeBoardCell(
    cell: WordSiegeCellDto,
    pendingLetter: Char?,
    pending: Boolean,
    myOwner: Int,
    enabled: Boolean,
    lastMoveHighlight: Float,
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
    val activeBonus = if (letter == null && !cell.bonusUsed) cell.bonus else null
    val cellColor = when {
        pending -> PracticeSiegeTile
        letter != null -> territory
        activeBonus == "2H" -> PracticeBonus2H
        activeBonus == "3H" -> PracticeBonus3H
        activeBonus == "2K" -> PracticeBonus2K
        activeBonus == "3K" -> PracticeBonus3K
        activeBonus == WordSiegeBoardSpec.CenterBonus -> PracticeBonus4K
        activeBonus == WordSiegeBoardSpec.StarBonus -> PracticeBonusStar
        else -> PracticeSiegeEmpty
    }

    Box(
        Modifier
            .size(PracticeSiegeCellSize)
            .padding(1.25.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(cellColor)
            .border(
                width = if (lastMoveHighlight > 0f) 1.65.dp else 0.45.dp,
                color = if (lastMoveHighlight > 0f) {
                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)
                } else {
                    MainUi.Border.copy(alpha = .45f)
                },
                shape = RoundedCornerShape(6.dp),
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
        if (lastMoveHighlight > 0f) {
            Box(Modifier.matchParentSize().background(PracticeLastMove.copy(alpha = .045f * lastMoveHighlight)))
        }
        if (letter != null) {
            Text(letter, color = Color.Black, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(
                practiceLetterValue(letter),
                color = Color.Black.copy(alpha = .78f),
                fontSize = WordSiegeBoardAccessibility.BoardLetterPoint,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        } else if (activeBonus != null) {
            Text(
                WordSiegeBoardSpec.displayBonusLabel(activeBonus),
                color = when (activeBonus) {
                    "2H" -> Color(0xFF316B86)
                    "3H" -> Color(0xFF3E6B4C)
                    "2K", "3K" -> Color(0xFF6D5585)
                    WordSiegeBoardSpec.CenterBonus -> Color(0xFF6A4B12)
                    WordSiegeBoardSpec.StarBonus -> Color(0xFF744500)
                    else -> MainUi.Text
                },
                fontSize = if (activeBonus == WordSiegeBoardSpec.StarBonus) 8.sp else WordSiegeBoardAccessibility.BoardBonus,
                letterSpacing = if (activeBonus == WordSiegeBoardSpec.StarBonus) (-.4).sp else 0.sp,
                fontWeight = FontWeight.Black,
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
    Surface(
        modifier = modifier.height(48.dp).combinedClickable(onClick = onClick, enabled = enabled),
        color = when {
            used -> MainUi.SurfaceSoft
            selected -> PracticeSiegeTile
            else -> Color(0xFFFFF1C9)
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MainUi.Blue else PracticeSiegeTileBorder.copy(alpha = .7f)),
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                letter.toString(),
                color = if (used) MainUi.Muted.copy(alpha = .45f) else PracticeSiegeLightTileText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                practiceLetterValue(letter.toString()),
                color = if (used) MainUi.Muted.copy(alpha = .55f) else Color(0xFF5D4B20),
                fontSize = WordSiegeBoardAccessibility.RackPoint,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        }
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
