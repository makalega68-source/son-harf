package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.floor

/**
 * Kuşatma board viewport policy.
 * Close mode keeps the readable 52dp cells and allows free 2D panning.
 * Fit mode scales the whole 15x15 board into the available viewport.
 * Double-tap toggles between the two modes.
 */
internal enum class WordSiegeBoardViewportMode { CLOSE, FIT }

internal enum class WordSiegeBoardTapAction { PLACE, TOGGLE_VIEWPORT }

internal fun WordSiegeBoardViewportMode.toggle(): WordSiegeBoardViewportMode =
    if (this == WordSiegeBoardViewportMode.CLOSE) WordSiegeBoardViewportMode.FIT
    else WordSiegeBoardViewportMode.CLOSE

internal inline fun dispatchWordSiegeBoardTap(
    action: WordSiegeBoardTapAction,
    canPlace: Boolean,
    onPlace: () -> Unit,
    onToggleViewport: () -> Unit,
) {
    when (action) {
        WordSiegeBoardTapAction.PLACE -> if (canPlace) onPlace()
        WordSiegeBoardTapAction.TOGGLE_VIEWPORT -> onToggleViewport()
    }
}

internal fun wordSiegeFitScale(
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardWidthPx: Float,
    boardHeightPx: Float = boardWidthPx,
): Float {
    if (viewportWidthPx <= 0f || viewportHeightPx <= 0f || boardWidthPx <= 0f || boardHeightPx <= 0f) return 1f
    return minOf(viewportWidthPx / boardWidthPx, viewportHeightPx / boardHeightPx)
}

internal fun wordSiegeFitPan(
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardWidthPx: Float,
    scale: Float,
    boardHeightPx: Float = boardWidthPx,
): Offset {
    if (
        viewportWidthPx <= 0f || viewportHeightPx <= 0f ||
        boardWidthPx <= 0f || boardHeightPx <= 0f || scale <= 0f
    ) return Offset.Zero
    val renderedWidthPx = boardWidthPx * scale
    val renderedHeightPx = boardHeightPx * scale
    return Offset(
        x = (viewportWidthPx - renderedWidthPx) / 2f,
        y = (viewportHeightPx - renderedHeightPx) / 2f,
    )
}

internal fun clampWordSiegeBoardPan(
    candidate: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardWidthPx: Float,
    scale: Float,
    boardHeightPx: Float = boardWidthPx,
): Offset {
    if (
        viewportWidthPx <= 0f || viewportHeightPx <= 0f ||
        boardWidthPx <= 0f || boardHeightPx <= 0f || scale <= 0f
    ) return Offset.Zero
    val renderedWidthPx = boardWidthPx * scale
    val renderedHeightPx = boardHeightPx * scale

    fun clampAxis(value: Float, viewportPx: Float, renderedPx: Float): Float =
        if (renderedPx <= viewportPx) {
            (viewportPx - renderedPx) / 2f
        } else {
            value.coerceIn(viewportPx - renderedPx, 0f)
        }

    return Offset(
        x = clampAxis(candidate.x, viewportWidthPx, renderedWidthPx),
        y = clampAxis(candidate.y, viewportHeightPx, renderedHeightPx),
    )
}

internal data class WordSiegeBoardTransform(
    val scale: Float,
    val pan: Offset,
    val renderedWidthPx: Float,
    val renderedHeightPx: Float,
)

/**
 * Single source of truth for rendering and hit-test geometry. FIT never reuses mutable CLOSE pan.
 */
internal fun wordSiegeBoardTransform(
    mode: WordSiegeBoardViewportMode,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardWidthPx: Float,
    boardHeightPx: Float = boardWidthPx,
    closeScale: Float = 1f,
    closePan: Offset = Offset.Zero,
): WordSiegeBoardTransform {
    val scale = if (mode == WordSiegeBoardViewportMode.FIT) {
        wordSiegeFitScale(viewportWidthPx, viewportHeightPx, boardWidthPx, boardHeightPx)
    } else {
        closeScale
    }
    val pan = if (mode == WordSiegeBoardViewportMode.FIT) {
        wordSiegeFitPan(viewportWidthPx, viewportHeightPx, boardWidthPx, scale, boardHeightPx)
    } else {
        clampWordSiegeBoardPan(
            closePan,
            viewportWidthPx,
            viewportHeightPx,
            boardWidthPx,
            scale,
            boardHeightPx,
        )
    }
    return WordSiegeBoardTransform(
        scale = scale,
        pan = pan,
        renderedWidthPx = boardWidthPx * scale,
        renderedHeightPx = boardHeightPx * scale,
    )
}

internal fun wordSiegeCenteredClosePan(
    index: Int,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardWidthPx: Float,
    cellSizePx: Float,
    scale: Float = 1f,
): Offset {
    if (cellSizePx <= 0f || scale <= 0f) return Offset.Zero
    val safeIndex = index.coerceIn(0, WordSiegeBoardSpec.LastIndex)
    val candidate = Offset(
        x = viewportWidthPx / 2f - (WordSiegeBoardSpec.column(safeIndex) + .5f) * cellSizePx * scale,
        y = viewportHeightPx / 2f - (WordSiegeBoardSpec.row(safeIndex) + .5f) * cellSizePx * scale,
    )
    return clampWordSiegeBoardPan(
        candidate = candidate,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        boardWidthPx = boardWidthPx,
        scale = scale,
    )
}

internal fun wordSiegeBoardIndexAt(
    viewportPoint: Offset,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
): Int? {
    if (cellSizePx <= 0f || transform.scale <= 0f) return null
    val boardX = (viewportPoint.x - transform.pan.x) / transform.scale
    val boardY = (viewportPoint.y - transform.pan.y) / transform.scale
    if (boardX < 0f || boardY < 0f || boardX >= cellSizePx * WordSiegeBoardSpec.Size || boardY >= cellSizePx * WordSiegeBoardSpec.Size) {
        return null
    }
    val column = floor(boardX / cellSizePx).toInt()
    val row = floor(boardY / cellSizePx).toInt()
    return WordSiegeBoardSpec.index(row, column)
}
