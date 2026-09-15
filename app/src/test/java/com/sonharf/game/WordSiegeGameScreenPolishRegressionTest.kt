package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeGameScreenPolishRegressionTest {
    @Test
    fun playerAvatarsStayProminent() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt").readText()

        assertTrue(ui.contains("size = 36.dp, accent = accent, visible = avatarVisible"))
    }

    @Test
    fun chatLivesInMatchChromeInsteadOfCoveringTheBoard() {
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertTrue(practice.contains("Icons.Rounded.Chat"))
        assertTrue(practice.contains("Sohbet çevrimiçi maçlarda kullanılabilir."))
        assertEquals(1, Regex("Icons\\.Rounded\\.Chat").findAll(online).count())
        assertTrue(online.contains("enabled = game.playerTwoId != null"))
        assertFalse(online.contains("Modifier.align(Alignment.BottomEnd).padding(7.dp).size(42.dp)"))
    }

    @Test
    fun boardBonusesKeepTwoLevelProfessionalTypography() {
        val practiceBoard = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        listOf(practiceBoard, online).forEach { source ->
            assertTrue(source.contains("fontWeight = FontWeight.SemiBold"))
            assertTrue(source.contains("fontWeight = FontWeight.Black"))
            assertTrue(source.contains("letterSpacing = .12.sp"))
            assertTrue(source.contains("fontSize = if (overview) 14.sp else 16.sp"))
        }
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
