package com.sonharf.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeBoardSpecTest {
    @Test
    fun `board has 225 cells and center is gold 4K`() {
        assertEquals(15, WordSiegeBoardSpec.Size)
        assertEquals(225, WordSiegeBoardSpec.CellCount)
        assertEquals(112, WordSiegeBoardSpec.CenterIndex)
        assertEquals(7, WordSiegeBoardSpec.row(112))
        assertEquals(7, WordSiegeBoardSpec.column(112))
        assertEquals("4K", WordSiegeBoardSpec.bonusAt(112))
    }

    @Test
    fun `static bonus layout is symmetric and follows siege topology`() {
        repeat(WordSiegeBoardSpec.CellCount) { index ->
            val row = WordSiegeBoardSpec.row(index)
            val column = WordSiegeBoardSpec.column(index)
            val horizontalMirror = WordSiegeBoardSpec.index(row, WordSiegeBoardSpec.Size - 1 - column)
            val verticalMirror = WordSiegeBoardSpec.index(WordSiegeBoardSpec.Size - 1 - row, column)
            assertEquals(WordSiegeBoardSpec.bonusAt(index), WordSiegeBoardSpec.bonusAt(horizontalMirror))
            assertEquals(WordSiegeBoardSpec.bonusAt(index), WordSiegeBoardSpec.bonusAt(verticalMirror))
        }
        assertEquals(33, (0 until WordSiegeBoardSpec.CellCount).count { WordSiegeBoardSpec.bonusAt(it) != null })
        assertEquals("3K", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(1, 7)))
        assertEquals("3H", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(2, 4)))
        assertEquals("2K", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(3, 6)))
        assertEquals("2H", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(5, 5)))
        assertEquals(null, WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(0, 0)))
    }

    @Test
    fun `every new game gets exactly one random three star bonus on a neutral cell`() {
        val starIndices = (1..24).map { seed ->
            val bonuses = WordSiegeBoardSpec.newGameBonuses(Random(seed))
            assertEquals(WordSiegeBoardSpec.CellCount, bonuses.size)
            assertEquals("4K", bonuses[WordSiegeBoardSpec.CenterIndex])
            assertEquals(1, bonuses.count { it == WordSiegeBoardSpec.StarBonus })
            val star = bonuses.indexOf(WordSiegeBoardSpec.StarBonus)
            assertTrue(star >= 0)
            assertNotEquals(WordSiegeBoardSpec.CenterIndex, star)
            assertEquals(null, WordSiegeBoardSpec.bonusAt(star))
            star
        }
        assertTrue("Star placement must vary between games", starIndices.distinct().size > 1)
    }
}
