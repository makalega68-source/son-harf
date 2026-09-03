package com.sonharf.game

import androidx.compose.ui.geometry.Offset

/**
 * Kuşatma board viewport policy.
 * Close mode keeps the readable 52dp cells and allows free 2D panning.
 * Fit mode scales the whole 15x15 board into the available viewport.
 * Double-tap toggles between the two modes.
 */
internal enum class WordSiegeBoardViewportMode { CLOSE, FIT }

internal fun WordSiegeBoardViewportMode.toggle(): WordSiegeBoardViewportMode =
    if (this == WordSiegeBoardViewportMode.CLOSE) WordSiegeBoardViewportMode.FIT
    else WordSiegeBoardViewportMode.CLOSE

internal fun wordSiegeFitScale(
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardSizePx: Float,
): Float {
    if (viewportWidthPx <= 0f || viewportHeightPx <= 0f || boardSizePx <= 0f) return 1f
    return minOf(viewportWidthPx / boardSizePx, viewportHeightPx / boardSizePx, 1f)
}

internal fun wordSiegeFitPan(
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardSizePx: Float,
    scale: Float,
): Offset {
    if (viewportWidthPx <= 0f || viewportHeightPx <= 0f || boardSizePx <= 0f || scale <= 0f) return Offset.Zero
    val renderedBoardPx = boardSizePx * scale
    return Offset(
        x = ((viewportWidthPx - renderedBoardPx) / 2f).coerceAtLeast(0f),
        y = ((viewportHeightPx - renderedBoardPx) / 2f).coerceAtLeast(0f),
    )
}

internal fun clampWordSiegeBoardPan(
    candidate: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardSizePx: Float,
    scale: Float,
): Offset {
    if (viewportWidthPx <= 0f || viewportHeightPx <= 0f || boardSizePx <= 0f || scale <= 0f) return Offset.Zero
    val renderedBoardPx = boardSizePx * scale

    fun clampAxis(value: Float, viewportPx: Float): Float =
        if (renderedBoardPx <= viewportPx) {
            (viewportPx - renderedBoardPx) / 2f
        } else {
            value.coerceIn(viewportPx - renderedBoardPx, 0f)
        }

    return Offset(
        x = clampAxis(candidate.x, viewportWidthPx),
        y = clampAxis(candidate.y, viewportHeightPx),
    )
}
