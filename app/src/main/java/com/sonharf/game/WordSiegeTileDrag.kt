package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** A hint tile flying from the rack to its cell. Positions are window coordinates. */
internal data class WordSiegeTileFlight(
    val key: String,
    val cell: Int,
    val letter: Char,
    val from: Offset,
    val to: Offset,
    val delayMs: Int,
)

/**
 * Finger-drag tile handling for the siege board: a tile is picked up with the finger, rides a
 * little above it (so the finger never hides it) and lands on the cell under the tile. The board
 * supplies the hit test; the screen draws [WordSiegeTileDragOverlay] on top of everything.
 */
internal class WordSiegeTileDrag {
    var rackIndex by mutableStateOf<Int?>(null)
        private set
    var fromCell by mutableStateOf<Int?>(null)
        private set
    var letter by mutableStateOf<Char?>(null)
        private set
    /** Finger position in window coordinates. */
    var pointer by mutableStateOf(Offset.Unspecified)
    var liftPx by mutableStateOf(0f)
        private set

    /** Board hit test: window position → board cell index. Set by the board. */
    var cellAt: (Offset) -> Int? = { null }
    /** Board cell centre in window coordinates. Set by the board. */
    var cellCenter: (Int) -> Offset? = { null }

    val active: Boolean get() = rackIndex != null

    /** Where the lifted tile is: it floats above the finger. */
    val tileCenter: Offset get() = if (pointer.isSpecified) Offset(pointer.x, pointer.y - liftPx) else pointer

    /** The cell the lifted tile would land on now. */
    val hoverCell: Int? get() = if (active && pointer.isSpecified) cellAt(tileCenter) else null

    fun start(rackIndex: Int, fromCell: Int?, letter: Char, at: Offset, liftPx: Float) {
        this.rackIndex = rackIndex
        this.fromCell = fromCell
        this.letter = letter
        this.liftPx = liftPx
        pointer = at
    }

    fun end() {
        rackIndex = null
        fromCell = null
        letter = null
        pointer = Offset.Unspecified
    }

    // Hint tiles in flight; their cells stay empty on the board until the tile lands.
    val flights = mutableStateListOf<WordSiegeTileFlight>()
    val flyingCells = mutableStateListOf<Int>()

    fun launchFlights(next: List<WordSiegeTileFlight>) {
        flights.clear()
        flyingCells.clear()
        flights.addAll(next)
        flyingCells.addAll(next.map { it.cell })
    }

    fun landed(cell: Int) {
        flyingCells.remove(cell)
        if (flyingCells.isEmpty()) flights.clear()
    }
}

/**
 * Makes a tile draggable. [onDrop] gets the rack index, the board cell it came from (null from the
 * rack) and the cell under the lifted tile (null when released off the board).
 */
internal fun Modifier.wordSiegeTileDragSource(
    drag: WordSiegeTileDrag?,
    enabled: Boolean,
    rackIndex: Int,
    fromCell: Int?,
    letter: Char,
    onDrop: (rackIndex: Int, fromCell: Int?, target: Int?) -> Unit,
): Modifier = if (drag == null || !enabled) this else composed {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentRack by rememberUpdatedState(rackIndex)
    val currentFrom by rememberUpdatedState(fromCell)
    val currentLetter by rememberUpdatedState(letter)
    val currentDrop by rememberUpdatedState(onDrop)
    this
        .onGloballyPositioned { coords = it }
        .pointerInput(drag) {
            detectDragGestures(
                onDragStart = { local ->
                    val c = coords
                    if (c != null && c.isAttached) {
                        drag.start(currentRack, currentFrom, currentLetter, c.localToWindow(local), 46.dp.toPx())
                    }
                },
                onDrag = { change, _ ->
                    change.consume()
                    val c = coords
                    if (drag.active && c != null && c.isAttached) drag.pointer = c.localToWindow(change.position)
                },
                onDragEnd = {
                    if (drag.active) {
                        val rack = drag.rackIndex
                        val from = drag.fromCell
                        val target = drag.hoverCell
                        drag.end()
                        if (rack != null) currentDrop(rack, from, target)
                    }
                },
                onDragCancel = { drag.end() },
            )
        }
}

/** Draws the lifted tile and the flying hint tiles. Never takes touches. */
@Composable
internal fun WordSiegeTileDragOverlay(drag: WordSiegeTileDrag, modifier: Modifier = Modifier) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.localToWindow(Offset.Zero) }) {
        val letter = drag.letter
        val center = drag.tileCenter
        if (drag.active && letter != null && center.isSpecified) {
            val half = with(density) { 31.dp.toPx() }
            WordSiegeFloatingTile(
                letter = letter,
                lifted = true,
                modifier = Modifier
                    .offset { IntOffset((center.x - origin.x - half).roundToInt(), (center.y - origin.y - half).roundToInt()) }
                    .size(62.dp),
            )
        }
        drag.flights.forEach { flight ->
            key(flight.key) {
                val t = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(flight.delayMs.toLong())
                    t.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
                    drag.landed(flight.cell)
                }
                if (t.value > 0f || flight.delayMs == 0) {
                    val half = with(density) { 24.dp.toPx() }
                    val arc = with(density) { 70.dp.toPx() } * sin(PI * t.value).toFloat()
                    val x = flight.from.x + (flight.to.x - flight.from.x) * t.value
                    val y = flight.from.y + (flight.to.y - flight.from.y) * t.value - arc
                    WordSiegeFloatingTile(
                        letter = flight.letter,
                        lifted = true,
                        modifier = Modifier
                            .offset { IntOffset((x - origin.x - half).roundToInt(), (y - origin.y - half).roundToInt()) }
                            .size(48.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun WordSiegeFloatingTile(letter: Char, lifted: Boolean, modifier: Modifier = Modifier) {
    val walnut = WordSiegeWalnutIvory.enabled
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .shadow(if (lifted) 12.dp else 3.dp, shape)
            .clip(shape)
            .background(
                if (walnut) WordSiegeWalnutIvory.tile
                else Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFF2C230), Color(0xFFD9A514))),
            )
            .border(1.5.dp, if (walnut) WordSiegeWalnutIvory.selection else Color(0xFFB8860B), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter.toString(),
            color = if (walnut) WordSiegeWalnutIvory.ink else Color(0xFF5A3A0A),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            practiceLetterValue(letter.toString()),
            color = if (walnut) WordSiegeWalnutIvory.secondaryInk else Color(0xFF5A3A0A).copy(alpha = .8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
        )
    }
}

/**
 * Where the pending tiles end up after a drop. A tile dropped off the board goes back to the rack;
 * dropped on another pending tile they swap (from the rack, the other one returns to the rack).
 */
internal fun wordSiegeDropTile(
    placements: Map<Int, Int>,
    board: List<com.sonharf.game.data.WordSiegeCellDto>,
    rackIndex: Int,
    fromCell: Int?,
    target: Int?,
    allowRackReplacement: Boolean = true,
): Map<Int, Int> {
    // A rack tile cannot replace a pending tile. Leave both intact on an occupied drop.
    if (!allowRackReplacement && fromCell == null && target != null && placements[target]?.let { it != rackIndex } == true) return placements
    var next = placements
    if (fromCell != null) next = next - fromCell
    if (target != null && board.getOrNull(target)?.letter == null) {
        val displaced = next[target]?.takeIf { it != rackIndex }
        next = next.filterValues { it != rackIndex } - target + (target to rackIndex)
        if (displaced != null && fromCell != null) next = next + (fromCell to displaced)
    }
    return next
}

/** Lets the drag layer find board cells: window position ↔ cell, through the board's pan and zoom. */
@Composable
internal fun WordSiegeRegisterBoardHitTest(
    drag: WordSiegeTileDrag?,
    viewportOriginInWindow: Offset,
    viewport: androidx.compose.ui.unit.IntSize,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
) {
    if (drag == null) return
    androidx.compose.runtime.SideEffect {
        drag.cellAt = { window ->
            val origin = viewportOriginInWindow
            val step = cellSizePx * transform.scale
            if (!origin.isSpecified || step <= 0f) null else {
                val local = window - origin
                if (local.x < 0f || local.y < 0f || local.x > viewport.width || local.y > viewport.height) null else {
                    val column = kotlin.math.floor((local.x - transform.pan.x) / step).toInt()
                    val row = kotlin.math.floor((local.y - transform.pan.y) / step).toInt()
                    if (row in 0 until WordSiegeBoardSpec.Size && column in 0 until WordSiegeBoardSpec.Size) {
                        WordSiegeBoardSpec.index(row, column).takeIf(WordSiegeBoardSpec::isValidIndex)
                    } else null
                }
            }
        }
        drag.cellCenter = { index ->
            val origin = viewportOriginInWindow
            if (!origin.isSpecified) null else origin + wordSiegeCellCenterInViewport(index, transform, cellSizePx)
        }
    }
}

/**
 * Move feedback on the board: a green check on the last tile of a valid word and,
 * when [score] is given, the move's points floating above the word. Draw inside the board viewport.
 */
@Composable
internal fun WordSiegePendingMoveBadges(
    cells: List<Int>,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
    valid: Boolean,
    score: Int?,
) {
    if (cells.isEmpty() || !valid) return
    val density = LocalDensity.current
    val step = cellSizePx * transform.scale
    val centres = cells.map { wordSiegeCellCenterInViewport(it, transform, cellSizePx) }
    val top = centres.minOf { it.y } - step / 2f
    val midX = (centres.minOf { it.x } + centres.maxOf { it.x }) / 2f
    val last = wordSiegeCellCenterInViewport(cells.max(), transform, cellSizePx)
    if (score != null) {
        val bubbleW = with(density) { 62.dp.toPx() }
        val bubbleH = with(density) { 28.dp.toPx() }
        Box(
            Modifier
                .offset { IntOffset((midX - bubbleW / 2f).roundToInt(), (top - bubbleH - 4f).coerceAtLeast(0f).roundToInt()) }
                .size(62.dp, 28.dp)
                .shadow(5.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2E9A62))
                .border(1.5.dp, Color.White, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+$score", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
    val badge = with(density) { 24.dp.toPx() }
    Box(
        Modifier
            .offset { IntOffset((last.x + step / 2f - badge * .6f).roundToInt(), (last.y + step / 2f - badge * .6f).roundToInt()) }
            .size(24.dp)
            .shadow(3.dp, androidx.compose.foundation.shape.CircleShape)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0xFF2FB36A))
            .border(1.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Rounded.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp),
        )
    }
}
