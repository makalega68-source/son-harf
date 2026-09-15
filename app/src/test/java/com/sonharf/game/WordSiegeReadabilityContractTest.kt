package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeReadabilityContractTest {
    private fun read(path: String) = File(path).readText()

    @Test
    fun onlineAndPracticeBoardsPreserveLegiblePointsAndTwoLevelBonusTypography() {
        val tokens = read("src/main/java/com/sonharf/game/WordSiegeBoardAccessibility.kt")
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")

        assertTrue(tokens.contains("BoardLetterPoint: TextUnit = 14.sp"))
        assertTrue(tokens.contains("BoardBonus: TextUnit = 17.sp"))
        assertTrue(tokens.contains("RackPoint: TextUnit = 14.sp"))

        listOf("BoardLetterPoint", "RackPoint").forEach { token ->
            assertTrue(online.contains("WordSiegeBoardAccessibility.$token"))
            assertTrue(practice.contains("WordSiegeBoardAccessibility.$token"))
        }

        assertTrue(online.contains("PanSiegeBonusLabel = Color(0xFF68716D)"))
        assertTrue(practice.contains("PracticeSiegeBonusLabel = Color(0xFF68716D)"))
        listOf(online, practice).forEach { board ->
            assertTrue(board.contains("fontSize = if (overview) 8.5.sp else 10.sp"))
            assertTrue(board.contains("fontSize = if (overview) 14.sp else 16.sp"))
            assertTrue(board.contains("fontWeight = FontWeight.SemiBold"))
            assertTrue(board.contains("fontWeight = FontWeight.Black"))
            assertTrue(board.contains("letterSpacing = .12.sp"))
        }
    }
}
