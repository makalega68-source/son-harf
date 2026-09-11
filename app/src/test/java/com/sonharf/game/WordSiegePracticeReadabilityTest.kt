package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeReadabilityTest {
    @Test
    fun `practice and online board labels stay readable and share the same tokens`() {
        assertTrue(WordSiegePracticeReadability.LetterPointSp >= 13)
        assertTrue(WordSiegePracticeReadability.BonusSp >= 17)
        assertTrue(WordSiegePracticeReadability.RackPointSp >= 13)
        assertTrue(WordSiegePracticeReadability.MinimumBoardCellDp >= 42)

        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertTrue(practice.contains("fontSize = WordSiegeBoardAccessibility.BoardLetterPoint"))
        assertTrue(practice.contains("fontSize = WordSiegeBoardAccessibility.BoardBonus"))
        assertTrue(practice.contains("fontSize = WordSiegeBoardAccessibility.RackPoint"))
        assertFalse(practice.contains("in 0..3 -> 10.sp"))
        assertFalse(practice.contains("4 -> 9.sp"))

        assertTrue(online.contains("fontSize = WordSiegeBoardAccessibility.BoardLetterPoint"))
        assertTrue(online.contains("fontSize = WordSiegeBoardAccessibility.BoardBonus"))
        assertTrue(online.contains("fontSize = WordSiegeBoardAccessibility.RackPoint"))
        assertFalse(online.contains("activeBonus == WordSiegeBoardSpec.StarBonus) 8.sp"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
