package com.sonharf.game

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Board-wide double tap. A cell only sees a double tap when both taps land on that same cell,
 * which on a small (fit) board is easy to miss; this watcher also catches a quick second tap on a
 * neighbouring cell. It only watches (never consumes), so taps, drags and pinches keep working.
 * [cellAt] maps a viewport position to a board index; same-cell double taps are left to the cell.
 */
internal fun Modifier.wordSiegeBoardDoubleTap(
    key: Any?,
    cellAt: (Offset) -> Int?,
    onDoubleTap: (Int) -> Unit,
): Modifier = pointerInput(key) {
    val slop = 56.dp.toPx()
    val moveTolerance = 14.dp.toPx()
    var lastUpTime = 0L
    var lastUpPosition = Offset.Zero
    var downPosition = Offset.Zero
    var multiTouch = false
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.size > 1) multiTouch = true
            val change = event.changes.firstOrNull() ?: continue
            if (change.pressed && !change.previousPressed) {
                downPosition = change.position
                if (event.changes.size == 1) multiTouch = false
            } else if (!change.pressed && change.previousPressed) {
                val isTap = !multiTouch && (change.position - downPosition).getDistance() < moveTolerance
                if (!isTap) {
                    lastUpTime = 0L
                    continue
                }
                val gap = change.uptimeMillis - lastUpTime
                if (lastUpTime > 0L && gap in 30L..380L && (change.position - lastUpPosition).getDistance() < slop) {
                    val first = cellAt(lastUpPosition)
                    val second = cellAt(change.position)
                    lastUpTime = 0L
                    // A double tap on one cell is the cell's own double click; only cover the misses.
                    if (second != null && first != second) onDoubleTap(second)
                } else {
                    lastUpTime = change.uptimeMillis
                    lastUpPosition = change.position
                }
            }
        }
    }
}

/** Board index under [position] (viewport coordinates) for the current pan/scale, or null. */
internal fun wordSiegeCellAt(position: Offset, transform: WordSiegeBoardTransform, cellSizePx: Float): Int? {
    val size = cellSizePx * transform.scale
    if (size <= 0f) return null
    val column = ((position.x - transform.pan.x) / size).toInt()
    val row = ((position.y - transform.pan.y) / size).toInt()
    if (position.x < transform.pan.x || position.y < transform.pan.y) return null
    if (row !in 0 until WordSiegeBoardSpec.Size || column !in 0 until WordSiegeBoardSpec.Size) return null
    return WordSiegeBoardSpec.index(row, column)
}
