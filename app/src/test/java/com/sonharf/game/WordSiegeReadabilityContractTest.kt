package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeReadabilityContractTest {
    private fun read(path: String) = File(path).readText()

    @Test
    fun onlineAndPracticeBoardsShareLegiblePointAndBonusTokens() {
        val tokens = read("src/main/java/com/sonharf/game/WordSiegeBoardAccessibility.kt")
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")

        assertTrue(tokens.contains("BoardLetterPoint: TextUnit = 14.sp"))
        assertTrue(tokens.contains("BoardBonus: TextUnit = 18.sp"))
        assertTrue(tokens.contains("RackPoint: TextUnit = 14.sp"))

        listOf("BoardLetterPoint", "BoardBonus", "RackPoint").forEach { token ->
            assertTrue(online.contains("WordSiegeBoardAccessibility.$token"))
            assertTrue(practice.contains("WordSiegeBoardAccessibility.$token"))
        }
    }
}
