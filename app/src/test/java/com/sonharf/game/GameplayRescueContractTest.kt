package com.sonharf.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameplayRescueContractTest {
    @Test
    fun practiceMoveSupportsExternalDictionaryValidatorAndAutoDirection() {
        val state = WordSiegePracticeEngine.newGame()
        val placements = linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4) // KALEM through center
        assertEquals(WordSiegeDirection.HORIZONTAL, detectWordSiegeDirection(state.board, placements.keys))

        val move = WordSiegePracticeEngine.validateMove(
            state = state,
            owner = 1,
            placements = placements,
            horizontal = true,
            wordValidator = { it == "KALEM" },
        )
        assertEquals("KALEM", move.primaryWord)
        assertTrue("KALEM" in move.formedWords)
    }

    @Test
    fun practiceScreenDoesNotRestoreManualDirectionOrScrolling() {
        val source = File("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        assertFalse(source.contains("LazyColumn"))
        assertFalse(source.contains("YATAY"))
        assertFalse(source.contains("DİKEY"))
        assertTrue(source.contains("detectWordSiegeDirection"))
        assertTrue(source.contains("validateWordSiegeDictionaryWord"))
        assertTrue(source.contains("SOHBET"))
    }
}
