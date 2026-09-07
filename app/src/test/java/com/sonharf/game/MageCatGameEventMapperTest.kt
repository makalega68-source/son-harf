package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MageCatGameEventMapperTest {
    @Test
    fun `own accepted word maps to correct word before streak threshold`() {
        assertEquals(
            MageCatEvent.CORRECT_WORD,
            MageCatGameEventMapper.acceptedWord(isMine = true, streak = 2),
        )
    }

    @Test
    fun `own accepted word maps to streak at threshold`() {
        assertEquals(
            MageCatEvent.WIN_STREAK,
            MageCatGameEventMapper.acceptedWord(isMine = true, streak = 3),
        )
    }

    @Test
    fun `opponent accepted word does not trigger mascot celebration`() {
        assertNull(MageCatGameEventMapper.acceptedWord(isMine = false, streak = 9))
    }

    @Test
    fun `match result maps winner relative to current player`() {
        assertEquals(
            MageCatEvent.VICTORY,
            MageCatGameEventMapper.matchResult(winnerId = "me", myPlayerId = "me"),
        )
        assertEquals(
            MageCatEvent.DEFEAT,
            MageCatGameEventMapper.matchResult(winnerId = "opponent", myPlayerId = "me"),
        )
    }

    @Test
    fun `draw or unresolved identity produces no hero reaction`() {
        assertNull(MageCatGameEventMapper.matchResult(winnerId = null, myPlayerId = "me"))
        assertNull(MageCatGameEventMapper.matchResult(winnerId = "winner", myPlayerId = null))
    }
}
