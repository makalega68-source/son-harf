package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeUxAndBotRegressionTest {
    private fun source(path: String): String = File(path).readText()

    @Test
    fun practiceBotUsesBoardAnchorFallbackAndExchangesInsteadOfSilentlyLoopingPasses() {
        val screen = source("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")
        val planner = source("src/main/java/com/sonharf/game/WordSiegePracticeBotPlanner.kt")

        assertTrue(screen.contains("resilientPracticeBotMove("))
        assertTrue(screen.contains("practiceBotExchangeIndices(state)"))
        assertTrue(screen.contains("WordSiegePracticeEngine.exchange(state, 2, exchange)"))
        assertTrue(planner.contains("mapNotNull { it.letter?.firstOrNull() }"))
        assertTrue(planner.contains("SharedDictionaryService.practiceCandidates("))
        assertTrue(planner.contains("WordSiegePracticeEngine.applyMove(state, 2, placements)"))
    }

    @Test
    fun botExchangeFallbackChoosesUpToThreeTilesWithoutBreakingTurnRules() {
        val state = WordSiegePracticeState(
            board = List(WordSiegeBoardSpec.CellCount) { WordSiegeCellDto() },
            bag = "ABCDEFG",
            playerRack = "KALEMTR",
            botRack = "MASASİN",
            currentOwner = 2,
        )

        assertEquals(setOf(0, 1, 2), practiceBotExchangeIndices(state))
        assertEquals(emptySet<Int>(), practiceBotExchangeIndices(state.copy(currentOwner = 1)))
        assertEquals(setOf(0, 1), practiceBotExchangeIndices(state.copy(bag = "AB")))
    }

    @Test
    fun firstPracticeExplainsCoreLoopAndKeepsReplayableHelp() {
        val screen = source("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")
        val guidance = source("src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt")

        assertTrue(screen.contains("WordSiegePracticeTutorialPrefs.isCompleted(context)"))
        assertTrue(screen.contains("Icons.Rounded.HelpOutline"))
        assertTrue(screen.contains("WordSiegePracticeTutorialCard("))
        assertTrue(screen.contains("if (tutorialStep == 1) tutorialStep = 2"))
        assertTrue(screen.contains("if (tutorialStep == 2) tutorialStep = 3"))
        assertTrue(screen.contains("if (tutorialStep == 3) tutorialStep = 4"))
        assertTrue(guidance.contains("Harflerle kelime kur. Kelimenin geçtiği hücreleri ele geçir."))
        assertTrue(guidance.contains("Her sahip olduğun hücre 2 bölge puanı verir"))
        assertTrue(guidance.contains("Kelime puanın kalıcıdır"))
    }

    @Test
    fun bottomStatusIsReadableAndStrategicZonesExplainTheirRealEffect() {
        val screen = source("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")
        val guidance = source("src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt")

        assertTrue(screen.contains("WordSiegePracticeStatusBar(statusMessage, compact)"))
        assertTrue(screen.contains("onZoneClick = { zoneInfoCode = it }"))
        assertTrue(screen.contains("WordSiegeBoardSpec.StarBonus"))
        assertTrue(guidance.contains("WordSiegePracticeStatusBar"))
        assertTrue(guidance.contains("TAÇ • İlk kelime bu bölgeden geçmelidir"))
        assertTrue(guidance.contains("ÖDÜL • Bu sürpriz bölge hamlene +"))
        assertTrue(guidance.contains("maxLines = 2"))
    }
}
