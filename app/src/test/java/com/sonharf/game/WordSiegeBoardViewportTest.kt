package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeBoardViewportTest {
    private val cellPx = 52f
    private val boardPx = cellPx * WordSiegeBoardSpec.Size
    private val tolerance = 0.001f

    @Test
    fun `15x15 geometry remains 225 cells with center index 112`() {
        assertEquals(15, WordSiegeBoardSpec.Size)
        assertEquals(225, WordSiegeBoardSpec.CellCount)
        assertEquals(112, WordSiegeBoardSpec.CenterIndex)
    }

    @Test
    fun `fit scale uses exact minimum viewport ratio and centers the full board`() {
        val transform = wordSiegeBoardTransform(
            mode = WordSiegeBoardViewportMode.FIT,
            viewportWidthPx = 390f,
            viewportHeightPx = 500f,
            boardWidthPx = boardPx,
        )

        assertEquals(0.5f, transform.scale, tolerance)
        assertEquals(390f, transform.renderedWidthPx, tolerance)
        assertEquals(390f, transform.renderedHeightPx, tolerance)
        assertEquals(0f, transform.pan.x, tolerance)
        assertEquals(55f, transform.pan.y, tolerance)
        assertBoardInsideAndCentered(transform, 390f, 500f)
        assertEquals(900f / boardPx, wordSiegeFitScale(900f, 900f, boardPx), tolerance)
    }

    @Test
    fun `real device portrait fit cannot stick to top left or show only a board fragment`() {
        val viewportWidth = 650f
        val viewportHeight = 720f
        val transform = wordSiegeBoardTransform(
            mode = WordSiegeBoardViewportMode.FIT,
            viewportWidthPx = viewportWidth,
            viewportHeightPx = viewportHeight,
            boardWidthPx = boardPx,
        )

        assertEquals(650f / boardPx, transform.scale, tolerance)
        assertEquals(0f, transform.pan.x, tolerance)
        assertEquals(35f, transform.pan.y, tolerance)
        assertEquals(viewportWidth, transform.renderedWidthPx, tolerance)
        assertEquals(viewportWidth, transform.renderedHeightPx, tolerance)
        assertBoardInsideAndCentered(transform, viewportWidth, viewportHeight)
        assertTrue(transform.renderedWidthPx / (cellPx * transform.scale) >= 15f - tolerance)
    }

    @Test
    fun `close pan min and max expose every edge without background gaps`() {
        val viewportWidth = 390f
        val viewportHeight = 420f
        val topLeft = wordSiegeBoardTransform(
            WordSiegeBoardViewportMode.CLOSE,
            viewportWidth,
            viewportHeight,
            boardPx,
            closePan = Offset(500f, 500f),
        )
        val bottomRight = wordSiegeBoardTransform(
            WordSiegeBoardViewportMode.CLOSE,
            viewportWidth,
            viewportHeight,
            boardPx,
            closePan = Offset(-999f, -999f),
        )

        assertEquals(0f, topLeft.pan.x, tolerance)
        assertEquals(0f, topLeft.pan.y, tolerance)
        assertEquals(viewportWidth - boardPx, bottomRight.pan.x, tolerance)
        assertEquals(viewportHeight - boardPx, bottomRight.pan.y, tolerance)
        assertNoBackgroundGap(topLeft, viewportWidth, viewportHeight)
        assertNoBackgroundGap(bottomRight, viewportWidth, viewportHeight)
    }

    @Test
    fun `board smaller than viewport is automatically centered on each axis`() {
        val transform = wordSiegeBoardTransform(
            mode = WordSiegeBoardViewportMode.CLOSE,
            viewportWidthPx = 900f,
            viewportHeightPx = 1000f,
            boardWidthPx = boardPx,
            closePan = Offset(-500f, 600f),
        )

        assertEquals((900f - boardPx) / 2f, transform.pan.x, tolerance)
        assertEquals((1000f - boardPx) / 2f, transform.pan.y, tolerance)
    }

    @Test
    fun `fit close fit always returns the same centered fit transform`() {
        val firstFit = wordSiegeBoardTransform(
            WordSiegeBoardViewportMode.FIT,
            650f,
            720f,
            boardPx,
        )
        wordSiegeBoardTransform(
            WordSiegeBoardViewportMode.CLOSE,
            650f,
            720f,
            boardPx,
            closePan = Offset(-430f, -220f),
        )
        val secondFit = wordSiegeBoardTransform(
            WordSiegeBoardViewportMode.FIT,
            650f,
            720f,
            boardPx,
            closePan = Offset(-430f, -220f),
        )

        assertEquals(firstFit, secondFit)
    }

    @Test
    fun `viewport resize recomputes fit scale and centered pan`() {
        val resized = wordSiegeBoardTransform(
            WordSiegeBoardViewportMode.FIT,
            720f,
            650f,
            boardPx,
        )

        assertEquals(650f / boardPx, resized.scale, tolerance)
        assertEquals(35f, resized.pan.x, tolerance)
        assertEquals(0f, resized.pan.y, tolerance)
        assertBoardInsideAndCentered(resized, 720f, 650f)
    }

    @Test
    fun `double tap toggles viewport and never dispatches placement`() {
        var placements = 0
        var toggles = 0

        dispatchWordSiegeBoardTap(
            action = WordSiegeBoardTapAction.TOGGLE_VIEWPORT,
            canPlace = true,
            onPlace = { placements += 1 },
            onToggleViewport = { toggles += 1 },
        )

        assertEquals(0, placements)
        assertEquals(1, toggles)
        assertEquals(WordSiegeBoardViewportMode.FIT, WordSiegeBoardViewportMode.CLOSE.toggle())
        assertEquals(WordSiegeBoardViewportMode.CLOSE, WordSiegeBoardViewportMode.FIT.toggle())
    }

    @Test
    fun `single tap placement respects placement eligibility`() {
        var placements = 0
        dispatchWordSiegeBoardTap(WordSiegeBoardTapAction.PLACE, false, { placements += 1 }, {})
        dispatchWordSiegeBoardTap(WordSiegeBoardTapAction.PLACE, true, { placements += 1 }, {})
        assertEquals(1, placements)
    }

    @Test
    fun `hit test preserves exact cell after pan and scale`() {
        val transform = wordSiegeBoardTransform(
            WordSiegeBoardViewportMode.CLOSE,
            390f,
            420f,
            boardPx,
            closePan = Offset(-260f, -104f),
        )
        val row = 8
        val column = 10
        val viewportPoint = Offset(
            x = transform.pan.x + (column + .5f) * cellPx * transform.scale,
            y = transform.pan.y + (row + .5f) * cellPx * transform.scale,
        )

        assertEquals(
            WordSiegeBoardSpec.index(row, column),
            wordSiegeBoardIndexAt(viewportPoint, transform, cellPx),
        )
        assertNotNull(wordSiegeBoardIndexAt(Offset(0f, 0f), transform, cellPx))
        assertNull(wordSiegeBoardIndexAt(Offset(5000f, 5000f), transform, cellPx))
    }

    private fun assertBoardInsideAndCentered(
        transform: WordSiegeBoardTransform,
        viewportWidth: Float,
        viewportHeight: Float,
    ) {
        assertTrue(transform.pan.x >= -tolerance)
        assertTrue(transform.pan.y >= -tolerance)
        assertTrue(transform.pan.x + transform.renderedWidthPx <= viewportWidth + tolerance)
        assertTrue(transform.pan.y + transform.renderedHeightPx <= viewportHeight + tolerance)
        assertEquals(0f, transform.pan.x + transform.renderedWidthPx / 2f - viewportWidth / 2f, tolerance)
        assertEquals(0f, transform.pan.y + transform.renderedHeightPx / 2f - viewportHeight / 2f, tolerance)
    }

    private fun assertNoBackgroundGap(
        transform: WordSiegeBoardTransform,
        viewportWidth: Float,
        viewportHeight: Float,
    ) {
        assertTrue(transform.pan.x <= tolerance)
        assertTrue(transform.pan.y <= tolerance)
        assertTrue(transform.pan.x + transform.renderedWidthPx >= viewportWidth - tolerance)
        assertTrue(transform.pan.y + transform.renderedHeightPx >= viewportHeight - tolerance)
    }
}
