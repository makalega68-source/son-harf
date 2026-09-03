package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeVfxVisibilityTest {
    private val cellPx = 52f
    private val boardPx = cellPx * WordSiegeBoardSpec.Size
    private val tolerance = 0.001f

    @Test
    fun `360x800 fit keeps every 15x15 cell center inside the viewport`() {
        val transform = wordSiegeBoardTransform(
            mode = WordSiegeBoardViewportMode.FIT,
            viewportWidthPx = 360f,
            viewportHeightPx = 800f,
            boardWidthPx = boardPx,
        )

        assertEquals(360f / 780f, transform.scale, tolerance)
        assertEquals(0f, transform.pan.x, tolerance)
        assertEquals(220f, transform.pan.y, tolerance)
        assertEquals(Offset(180f, 400f), wordSiegeCellCenterInViewport(WordSiegeBoardSpec.CenterIndex, transform, cellPx))

        repeat(WordSiegeBoardSpec.CellCount) { index ->
            val center = wordSiegeCellCenterInViewport(index, transform, cellPx)
            assertTrue(center.x in 0f..360f)
            assertTrue(center.y in 0f..800f)
        }
    }

    @Test
    fun `screen-space VFX center follows exact pan scale transform in close and fit`() {
        val close = wordSiegeBoardTransform(
            mode = WordSiegeBoardViewportMode.CLOSE,
            viewportWidthPx = 360f,
            viewportHeightPx = 420f,
            boardWidthPx = boardPx,
            closePan = Offset(-208f, -104f),
        )
        val index = WordSiegeBoardSpec.index(6, 9)
        assertEquals(
            Offset(
                close.pan.x + (9.5f * cellPx * close.scale),
                close.pan.y + (6.5f * cellPx * close.scale),
            ),
            wordSiegeCellCenterInViewport(index, close, cellPx),
        )

        val fit = wordSiegeBoardTransform(
            mode = WordSiegeBoardViewportMode.FIT,
            viewportWidthPx = 360f,
            viewportHeightPx = 420f,
            boardWidthPx = boardPx,
        )
        assertEquals(
            Offset(
                fit.pan.x + (9.5f * cellPx * fit.scale),
                fit.pan.y + (6.5f * cellPx * fit.scale),
            ),
            wordSiegeCellCenterInViewport(index, fit, cellPx),
        )
    }

    @Test
    fun `effective board border stays 1_3dp on screen in close and fit`() {
        val closeScale = 1f
        val fitScale = 360f / boardPx
        val closeBoardDp = wordSiegeBoardBorderWidthDp(closeScale)
        val fitBoardDp = wordSiegeBoardBorderWidthDp(fitScale)

        assertEquals(1.3f, closeBoardDp * closeScale, tolerance)
        assertEquals(1.3f, fitBoardDp * fitScale, tolerance)
        assertTrue(closeBoardDp * closeScale >= WORD_SIEGE_MIN_SCREEN_BORDER_DP)
        assertTrue(fitBoardDp * fitScale >= WORD_SIEGE_MIN_SCREEN_BORDER_DP)
        assertTrue(closeBoardDp in 1.2f..1.5f)
    }

    @Test
    fun `action VFX visibility thresholds stay within acceptance ranges`() {
        assertTrue(PURCHASED_BOARD_PLACE_VFX_MS in 600..700)
        assertTrue(PURCHASED_BOARD_RESOLVE_VFX_MS in 750..850)
        assertTrue(PURCHASED_BOARD_PLACE_MAX_ALPHA in .80f..90f)
        assertTrue(PURCHASED_BOARD_RESOLVE_MAX_ALPHA in .80f..90f)
        assertEquals(4, PURCHASED_BOARD_PLACE_STAR_COUNT)
        assertTrue(PURCHASED_BOARD_RESOLVE_STAR_COUNT in 4..6)
        assertTrue(PURCHASED_BOARD_PLACE_MIN_STAR_DP >= 12f)
        assertTrue(PURCHASED_BOARD_RESOLVE_MIN_STAR_DP >= 12f)
    }
}
