package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeBoardSpecTest {
    @Test
    fun `board has 225 cells and center is index 112`() {
        assertEquals(15, WordSiegeBoardSpec.Size)
        assertEquals(225, WordSiegeBoardSpec.CellCount)
        assertEquals(112, WordSiegeBoardSpec.CenterIndex)
        assertEquals(7, WordSiegeBoardSpec.row(112))
        assertEquals(7, WordSiegeBoardSpec.column(112))
        assertEquals("2K", WordSiegeBoardSpec.bonusAt(112))
    }

    @Test
    fun `bonus layout is symmetric on both axes`() {
        repeat(WordSiegeBoardSpec.CellCount) { index ->
            val row = WordSiegeBoardSpec.row(index)
            val column = WordSiegeBoardSpec.column(index)
            val horizontalMirror = WordSiegeBoardSpec.index(row, WordSiegeBoardSpec.Size - 1 - column)
            val verticalMirror = WordSiegeBoardSpec.index(WordSiegeBoardSpec.Size - 1 - row, column)
            assertEquals(WordSiegeBoardSpec.bonusAt(index), WordSiegeBoardSpec.bonusAt(horizontalMirror))
            assertEquals(WordSiegeBoardSpec.bonusAt(index), WordSiegeBoardSpec.bonusAt(verticalMirror))
        }
        assertTrue((0 until WordSiegeBoardSpec.CellCount).count { WordSiegeBoardSpec.bonusAt(it) != null } >= 50)
    }
}
