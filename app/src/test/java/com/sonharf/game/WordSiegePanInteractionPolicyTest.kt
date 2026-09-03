package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class WordSiegePanInteractionPolicyTest {
    @Test fun `double tap toggles close and fit`() {
        assertEquals(WordSiegeBoardViewportMode.FIT, WordSiegeBoardViewportMode.CLOSE.toggle())
        assertEquals(WordSiegeBoardViewportMode.CLOSE, WordSiegeBoardViewportMode.FIT.toggle())
    }

    @Test fun `fit scale follows the smaller viewport ratio`() {
        assertEquals(1200f / 468f, wordSiegeFitScale(1200f, 1200f, 468f), 0.001f)
    }

    @Test fun `captured territory remains two points per cube`() {
        assertEquals(6, WordSiegeFinalRules.cubeTransfer(3))
    }
}
