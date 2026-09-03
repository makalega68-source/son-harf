package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WordSiegePendingMoveToolsTest {
    @Test
    fun `undo removes only one pending tile`() {
        val pending = linkedMapOf(40 to 0, 41 to 1, 42 to 2)
        assertEquals(mapOf(40 to 0, 41 to 1), wordSiegeUndoPendingPlacement(pending))
        assertEquals(mapOf(40 to 0, 42 to 2), wordSiegeUndoPendingPlacement(pending, 41))
    }

    @Test
    fun `shuffle preserves backend rack indices`() {
        val shuffled = wordSiegeShuffledRackIndices(7, seed = 20260903)
        assertEquals((0..6).toSet(), shuffled.toSet())
        assertEquals(7, shuffled.size)
        assertNotEquals((0..6).toList(), shuffled)
    }
}
