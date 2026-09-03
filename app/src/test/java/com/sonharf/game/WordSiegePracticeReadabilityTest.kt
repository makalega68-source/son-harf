package com.sonharf.game

import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeReadabilityTest {
    @Test
    fun `practice score and bonus labels stay readable`() {
        assertTrue(WordSiegePracticeReadability.LetterPointSp >= 8)
        assertTrue(WordSiegePracticeReadability.BonusSp >= 10)
        assertTrue(WordSiegePracticeReadability.RackPointSp >= 9)
        assertTrue(WordSiegePracticeReadability.MinimumBoardCellDp >= 42)
    }
}
