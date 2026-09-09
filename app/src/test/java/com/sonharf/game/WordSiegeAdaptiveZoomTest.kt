package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeAdaptiveZoomTest {
    private val cellPx = 52f
    private val boardPx = cellPx * WordSiegeBoardSpec.Size
    private val tolerance = 0.001f

    @Test fun `online close view grows from fit by 1_85x on a narrow phone`() {
        val fit = wordSiegeFitScale(
            viewportWidthPx = 390f,
            viewportHeightPx = 500f,
            boardWidthPx = boardPx,
        )
        val close = wordSiegeOnlineCloseScale(
            viewportWidthPx = 390f,
            viewportHeightPx = 500f,
            boardWidthPx = boardPx,
        )

        assertEquals(0.5f, fit, tolerance)
        assertEquals(fit * WORD_SIEGE_ONLINE_ZOOM_FACTOR, close, tolerance)
        val visibleColumns = 390f / (cellPx * close)
        assertTrue("Focused close view should show about eight columns", visibleColumns in 7.5f..9f)
    }

    @Test fun `online close scale never becomes smaller than fit and respects stable max when possible`() {
        val fit = wordSiegeFitScale(650f, 720f, boardPx)
        val close = wordSiegeOnlineCloseScale(650f, 720f, boardPx)

        assertTrue(close >= fit)
        assertTrue(close <= WORD_SIEGE_ONLINE_MAX_CLOSE_SCALE)
    }

    @Test fun `focused close pan keeps a central tapped cell centered`() {
        val close = wordSiegeOnlineCloseScale(390f, 500f, boardPx)
        val pan = wordSiegeCenteredClosePan(
            index = WordSiegeBoardSpec.CenterIndex,
            viewportWidthPx = 390f,
            viewportHeightPx = 500f,
            boardWidthPx = boardPx,
            cellSizePx = cellPx,
            scale = close,
        )
        val transform = wordSiegeBoardTransform(
            mode = WordSiegeBoardViewportMode.CLOSE,
            viewportWidthPx = 390f,
            viewportHeightPx = 500f,
            boardWidthPx = boardPx,
            closeScale = close,
            closePan = pan,
        )
        val center = wordSiegeCellCenterInViewport(WordSiegeBoardSpec.CenterIndex, transform, cellPx)

        assertEquals(195f, center.x, tolerance)
        assertEquals(250f, center.y, tolerance)
    }
}
