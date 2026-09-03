package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class WordSiegeBoardViewportTest {
    @Test
    fun `double tap policy toggles close and fit`() {
        assertEquals(WordSiegeBoardViewportMode.FIT, WordSiegeBoardViewportMode.CLOSE.toggle())
        assertEquals(WordSiegeBoardViewportMode.CLOSE, WordSiegeBoardViewportMode.FIT.toggle())
    }

    @Test
    fun `fit scale shows whole board without enlarging beyond native size`() {
        assertEquals(0.5f, wordSiegeFitScale(234f, 500f, 468f), 0.001f)
        assertEquals(1f, wordSiegeFitScale(700f, 700f, 468f), 0.001f)
    }
}
