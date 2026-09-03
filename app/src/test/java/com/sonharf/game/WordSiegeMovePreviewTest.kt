package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class WordSiegeMovePreviewTest {
    @Test
    fun `preview keeps word score permanent and values captured area at two points`() {
        val preview = WordSiegeMovePreview(wordScore = 18, capturedCells = 3)
        assertEquals(6, preview.areaScore)
        assertEquals(24, preview.totalScore)
        assertEquals("Kelime +18 • Alan +6 → Toplam +24", wordSiegeMovePreviewLabel(preview, turkish = true))
    }
}
