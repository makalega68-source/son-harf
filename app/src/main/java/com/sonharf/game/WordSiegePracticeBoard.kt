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
import androidx.compose.material.icons.Icons
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
internal val PracticeSiegeBoardSurface = Color(0xFFDDE6EB)
internal val PracticeSiegeNeutral = Color(0xFFF8FAF9)
private val PracticeSiegeEmpty = Color(0xFFFFF7E6)
private val PracticeSiegeMine = Color(0xFF35C878)
private val PracticeSiegeRival = Color(0xFFFF5F57)

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
            highlightAlpha.animateTo(0f, tween(WORD_SIEGE_LAST_MOVE_EXIT_MS))
            highlightedIndices = emptySet()
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MainUi.Border.copy(alpha = .55f)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
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
    val cellColor = when {
        pending -> PracticeSiegeTile
        letter != null -> territory
        else -> PracticeSiegeEmpty
    }

    Box(
        Modifier
            .size(PracticeSiegeCellSize)
            .padding(1.6.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(cellColor)
            .border(
                width = if (lastMoveHighlight > 0f) 1.75.dp else 0.dp,
                color = Color.White.copy(alpha = .25f + .65f * lastMoveHighlight),
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
        if (lastMoveHighlight > 0f) Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .06f * lastMoveHighlight)))
        if (letter != null) {
            Text(letter, color = Color.Black, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(
                practiceLetterValue(letter),
                color = Color.Black.copy(alpha = .78f),
                fontSize = WordSiegeBoardAccessibility.BoardLetterPoint,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        } else if (!cell.bonusUsed && cell.bonus != null) {
            Text(
                cell.bonus,
                color = if (cell.bonus == "2H" || cell.bonus == "3H") MainUi.Blue else SiegePurple,
                fontSize = WordSiegeBoardAccessibility.BoardBonus,
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
            Text(letter.toString(), color = if (used) MainUi.Muted.copy(alpha = .45f) else MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
