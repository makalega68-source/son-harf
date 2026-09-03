package com.sonharf.game

/**
 * Kuşatma board viewport policy.
 * Close mode keeps the readable 52dp cells and allows free 2D panning.
 * Fit mode scales the whole 9x9 board into the available viewport.
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
