package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayRescueContractTest {
    @Test
    fun practiceMoveSupportsExternalDictionaryValidatorAndAutoDirection() {
        val state = WordSiegePracticeEngine.newGame().copy(playerRack = "KALEMTR")
        val placements = linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4)
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
    fun freshPracticeGamesDoNotAlwaysDealTheSameRack() {
        val openings = List(12) { WordSiegePracticeEngine.newGame().playerRack }.toSet()
        assertTrue("Opening rack should be randomized", openings.size > 1)
    }

    @Test
    fun exchangeReplacesSelectedTilesAndReturnsThemToBag() {
        val state = WordSiegePracticeEngine.newGame().copy(playerRack = "KALEMTR", bag = "ĞJABCÇDEFGHİKLMNOPRSTUVYZ")
        val next = WordSiegePracticeEngine.exchange(state, 1, setOf(0, 1))

        assertEquals(7, next.playerRack.length)
        assertEquals(state.bag.length, next.bag.length)
        assertEquals("exchange", next.lastAction)
        assertEquals(2, next.currentOwner)
        assertTrue('K' in next.bag)
        assertTrue('A' in next.bag)
    }

    @Test
    fun rareTurkishLettersAreWorthMore() {
        assertTrue(WordSiegePracticeEngine.tileValue('Ğ') > WordSiegePracticeEngine.tileValue('A'))
        assertTrue(WordSiegePracticeEngine.tileValue('J') > WordSiegePracticeEngine.tileValue('Ğ'))
    }

    @Test
    fun practiceScreenDoesNotRestoreManualDirectionOrScrolling() {
        val source = File("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val dictionary = File("src/main/java/com/sonharf/game/data/WordSiegePracticeDictionary.kt").readText()
        assertFalse(source.contains("LazyColumn"))
        assertFalse(source.contains("YATAY"))
        assertFalse(source.contains("DİKEY"))
        assertTrue(source.contains("detectWordSiegeDirection"))
        assertTrue(source.contains("ExchangeRackSelector"))
        assertTrue(source.contains("tileValue(char)"))
        assertTrue(source.contains("SOHBET"))
        assertTrue(dictionary.contains("decodeAs<Boolean>()"))
    }
}
