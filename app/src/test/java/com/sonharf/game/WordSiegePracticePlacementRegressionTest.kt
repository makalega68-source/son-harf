package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticePlacementRegressionTest {
    private fun source(path: String): String = File(path).readText()

    @Test
    fun practicePlacementIsNotBlockedByDictionaryWarmup() {
        val screen = source("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")

        assertTrue(screen.contains("val canPlayerAct = state.status == \"playing\" && state.currentOwner == 1 && !botThinking"))
        assertFalse(screen.contains("val canPlayerAct = dictionaryReady &&"))
        assertTrue(screen.contains("if (!dictionaryReady)"))
        assertTrue(screen.contains("Sözlük hazırlanıyor. Harflerini yerleştirebilirsin"))
    }

    @Test
    fun practiceBoardParentGestureDoesNotSwallowPlacementTapsDuringPlayerTurn() {
        val board = source("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")

        assertTrue(board.contains(".pointerInput(mode, viewport, boardPx, closeScale, enabled)"))
        assertTrue(board.contains("if (mode == WordSiegeBoardViewportMode.CLOSE && !enabled)"))
        assertTrue(board.contains("combinedClickable("))
        assertTrue(board.contains("WordSiegeBoardTapAction.PLACE"))
    }
}
