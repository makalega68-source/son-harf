package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeBoardViewportTest {
    @Test
    fun `double tap policy toggles close and fit`() {
        assertEquals(WordSiegeBoardViewportMode.FIT, WordSiegeBoardViewportMode.CLOSE.toggle())
        assertEquals(WordSiegeBoardViewportMode.CLOSE, WordSiegeBoardViewportMode.FIT.toggle())
    }

    @Test
    fun `fit scale shows whole 15x15 board without enlarging beyond native size`() {
        val boardPx = 52f * WordSiegeBoardSpec.Size
        assertEquals(0.5f, wordSiegeFitScale(390f, 500f, boardPx), 0.001f)
        assertEquals(1f, wordSiegeFitScale(900f, 900f, boardPx), 0.001f)
        val pan = wordSiegeFitPan(390f, 500f, boardPx, 0.5f)
        assertEquals(0f, pan.x, 0.001f)
        assertEquals(55f, pan.y, 0.001f)
    }

    @Test
    fun `close pan clamps every edge so no empty background is exposed`() {
        val boardPx = 52f * WordSiegeBoardSpec.Size
        val topLeft = clampWordSiegeBoardPan(Offset(500f, 500f), 390f, 420f, boardPx, 1f)
        val bottomRight = clampWordSiegeBoardPan(Offset(-999f, -999f), 390f, 420f, boardPx, 1f)

        assertEquals(0f, topLeft.x, 0.001f)
        assertEquals(0f, topLeft.y, 0.001f)
        assertEquals(390f - boardPx, bottomRight.x, 0.001f)
        assertEquals(420f - boardPx, bottomRight.y, 0.001f)
        assertTrue(bottomRight.x < 0f && bottomRight.y < 0f)
    }
}
