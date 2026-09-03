package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.WordSiegeCellDto

private val PracticeSiegeCellSize = 52.dp
private val PracticeSiegeTile = Color(0xFFFFE3A5)
private val PracticeSiegeTileBorder = Color(0xFFD99818)
private val PracticeSiegeBoardSurface = Color(0xFFE7EDF5)
private val PracticeSiegeMine = Color(0xFF35C878)
private val PracticeSiegeRival = Color(0xFFFF5F57)
private val PracticeSiegeNeutralBorder = Color(0xFF7890A8)
private val PracticeSiegeBonusBorder = Color(0xFF5279A6)
private val PracticeSiegeMineBorder = Color(0xFF147A48)
private val PracticeSiegeRivalBorder = Color(0xFFB72E35)

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
    var initialized by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }
    val transform by remember(mode, viewport, boardPx, closePan) {
        derivedStateOf {
            wordSiegeBoardTransform(
                mode = mode,
                viewportWidthPx = viewport.width.toFloat(),
                viewportHeightPx = viewport.height.toFloat(),
                boardWidthPx = boardPx,
                closePan = closePan,
            )
        }
    }
    val boardBorderWidth = wordSiegeBoardBorderWidthDp(transform.scale).dp
    val actionVfxEvents = remember(placements, moveEventKey, resolvedIndices) {
        buildList {
            placements.toSortedMap().forEach { (index, rackIndex) ->
                if (WordSiegeBoardSpec.isValidIndex(index)) {
                    add(PurchasedBoardVfxEvent("placement:$index:$rackIndex", index, PurchasedBoardVfxKind.PLACEMENT))
                }
            }
            if (moveEventKey != null) {
                resolvedIndices.sorted().forEach { index ->
                    if (WordSiegeBoardSpec.isValidIndex(index)) {
                        add(PurchasedBoardVfxEvent("resolved:$moveEventKey:$index", index, PurchasedBoardVfxKind.RESOLVED))
                    }
                }
            }
        }
    }

    fun clampClosePan(candidate: Offset): Offset = clampWordSiegeBoardPan(
        candidate,
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        boardPx,
        1f,
    )

    fun centerClose(): Offset =
        wordSiegeCenteredClosePan(
            index = WordSiegeBoardSpec.CenterIndex,
            viewportWidthPx = viewport.width.toFloat(),
            viewportHeightPx = viewport.height.toFloat(),
            boardWidthPx = boardPx,
            cellSizePx = tilePx,
        )

    fun toggleMode() {
        val nextMode = mode.toggle()
        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {
            closePan = centerClose()
        }
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
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .clipToBounds()
                .onGloballyPositioned { viewport = it.size }
                .pointerInput(mode, viewport, boardPx) {
                    if (mode == WordSiegeBoardViewportMode.CLOSE) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            closePan = clampClosePan(closePan + dragAmount)
                        }
                    }
                },
        ) {
            Column(
                Modifier
                    // Keep the oversized board's layout origin aligned with transform.pan.
                    // Without this, requiredSize is coerced and Compose centers its layer implicitly.
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
                            val pending = pendingRackIndex != null
                            WordSiegePracticeBoardCell(
                                cell = board.getOrElse(index) { WordSiegeCellDto() },
                                pendingLetter = pendingRackIndex?.let(rack::getOrNull),
                                pending = pending,
                                myOwner = myOwner,
                                enabled = enabled,
                                size = PracticeSiegeCellSize,
                                borderWidth = boardBorderWidth,
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

            SmallFloatingActionButton(
                onClick = {
                    mode = WordSiegeBoardViewportMode.CLOSE
                    closePan = centerClose()
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
private fun WordSiegePracticeBoardCell(
    cell: WordSiegeCellDto,
    pendingLetter: Char?,
    pending: Boolean,
    myOwner: Int,
    enabled: Boolean,
    size: Dp,
    borderWidth: Dp,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val owner = if (pending) myOwner else cell.owner
    val territory = when {
        owner == 0 -> MainUi.Surface
        owner == myOwner -> PracticeSiegeMine
        else -> PracticeSiegeRival
    }
    val border = when {
        pending -> PracticeSiegeTileBorder
        owner == myOwner -> PracticeSiegeMineBorder
        owner != 0 -> PracticeSiegeRivalBorder
        !cell.bonusUsed && cell.bonus != null -> PracticeSiegeBonusBorder
        else -> PracticeSiegeNeutralBorder
    }
    val letter = pendingLetter?.toString() ?: cell.letter
    val canPlace = enabled && (cell.letter == null || pending)

    Box(
        Modifier
            .size(size)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (letter != null) territory else MainUi.Surface)
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
            color = if (pending) PracticeSiegeTile.copy(alpha = .82f) else Color.Transparent,
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(if (pending) maxOf(2.dp, borderWidth) else borderWidth, border.copy(alpha = .96f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
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
                color = MainUi.Muted,
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
