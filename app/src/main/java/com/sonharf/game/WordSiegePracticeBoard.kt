package com.sonharf.game

import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
private val PracticeSiegeTile = Color(0xFFF5F0E4)
private val PracticeSiegeTileBorder = Color(0xFF8EA697)
internal val PracticeSiegeBoardSurface = Color(0xFFE8ECE8)
internal val PracticeSiegeNeutral = Color(0xFFFAF7EF)
private val PracticeSiegeEmpty = Color(0xFFFAF7EF)
private val PracticeSiegeMine = Color(0xFFA8C7B1)
private val PracticeSiegeRival = Color(0xFFAFCDE0)
private val PracticeSiegeLightTileText = Color(0xFF17372C)
private val PracticeBonus2H = Color(0xFFDCEAF2)
private val PracticeBonus3H = Color(0xFFDDEBDD)
private val PracticeBonus2K = Color(0xFFEAE2F0)
private val PracticeBonus3K = Color(0xFFDED4E8)
private val PracticeBonus4K = Color(0xFFE7DDBB)
private val PracticeBonusStar = Color(0xFFEAD59B)
private val PracticeLastMove = Color(0xFFE7B95E)
private val PracticeDefinitionBadge = Color(0xFF5C8299)

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
    resolvedIndices: Set<Int> = emptySet(),
    language: String = SonHarfUiState.language,
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
        border = BorderStroke(1.dp, MainUi.Border.copy(alpha = .70f)),
        shadowElevation = 1.dp,
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
                                showDefinitionBadge = resolvedWord?.badgeIndex == index,
                                onDefinitionClick = {
                                    resolvedWord?.word?.let { definitionWord = it }
                                },
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
    val regionGap = if (letter != null && owner != 0) .55.dp else 1.25.dp

    Box(
        Modifier
            .size(PracticeSiegeCellSize)
            .padding(regionGap)
            .clip(RoundedCornerShape(7.dp))
            .background(cellColor)
            .border(
                width = if (lastMoveHighlight > 0f) 1.65.dp else 0.45.dp,
                color = if (lastMoveHighlight > 0f) {
                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)
                } else {
                    MainUi.Border.copy(alpha = .45f)
                },
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
        if (lastMoveHighlight > 0f) {
            Box(Modifier.matchParentSize().background(PracticeLastMove.copy(alpha = .045f * lastMoveHighlight)))
        }
        if (letter != null) {
            Text(letter, color = PracticeSiegeLightTileText, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(
                practiceLetterValue(letter),
                color = PracticeSiegeLightTileText.copy(alpha = .78f),
                fontSize = WordSiegeBoardAccessibility.BoardLetterPoint,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
            if (showDefinitionBadge && !pending) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 0.dp,
                                bottomEnd = 5.dp,
                                bottomStart = 0.dp,
                            ),
                        )
                        .background(PracticeDefinitionBadge)
                        .clickable(onClick = onDefinitionClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        } else if (activeBonus != null) {
            Text(
                WordSiegeBoardSpec.displayBonusLabel(activeBonus),
                color = when (activeBonus) {
                    "2H" -> Color(0xFF456F83)
                    "3H" -> Color(0xFF4F735A)
                    "2K", "3K" -> Color(0xFF6D5A7B)
                    WordSiegeBoardSpec.CenterBonus -> Color(0xFF6B5A2D)
                    WordSiegeBoardSpec.StarBonus -> Color(0xFF755E21)
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
            selected -> Color(0xFFE1ECE4)
            else -> PracticeSiegeTile
        },
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF567A64) else PracticeSiegeTileBorder.copy(alpha = .7f)),
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
                color = if (used) MainUi.Muted.copy(alpha = .55f) else PracticeSiegeLightTileText.copy(alpha = .72f),
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
