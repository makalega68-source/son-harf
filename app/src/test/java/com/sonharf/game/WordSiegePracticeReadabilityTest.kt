package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeReadabilityTest {
    @Test
    fun `practice and online board labels stay readable with two-level bonus hierarchy`() {
        assertTrue(WordSiegePracticeReadability.LetterPointSp >= 13)
        assertTrue(WordSiegePracticeReadability.BonusSp >= 16)
        assertTrue(WordSiegePracticeReadability.RackPointSp >= 13)
        assertTrue(WordSiegePracticeReadability.MinimumBoardCellDp >= 42)

        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        listOf(practice, online).forEach { board ->
            assertTrue(board.contains("fontSize = WordSiegeBoardAccessibility.BoardLetterPoint"))
            assertTrue(board.contains("fontSize = WordSiegeBoardAccessibility.RackPoint"))
            assertTrue(board.contains("fontSize = if (overview) 8.5.sp else 10.sp"))
            assertTrue(board.contains("fontSize = if (overview) 14.sp else 16.sp"))
            assertTrue(board.contains("fontWeight = FontWeight.SemiBold"))
            assertTrue(board.contains("fontWeight = FontWeight.Black"))
            assertTrue(board.contains("letterSpacing = .12.sp"))
        }

        assertFalse(practice.contains("in 0..3 -> 10.sp"))
        assertFalse(practice.contains("4 -> 9.sp"))
        assertFalse(online.contains("activeBonus == WordSiegeBoardSpec.StarBonus) 8.sp"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
