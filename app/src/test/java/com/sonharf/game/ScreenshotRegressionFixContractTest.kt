package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotRegressionFixContractTest {
    private fun read(path: String): String {
        val direct = File(path.removePrefix("app/"))
        val root = File(path)
        return when { direct.exists() -> direct.readText(); root.exists() -> root.readText(); else -> error("Missing $path") }
    }
    @Test fun duelSubmissionIsProtected() {
        val s = read("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        assertTrue(s.contains("latest.playerId == backend.currentUserId()"))
        assertTrue(s.contains("val alreadyUsed = words.any"))
        assertTrue(s.contains("withTimeout(7_000L)"))
        assertTrue(s.contains("finally {"))
    }
    @Test fun nightArenaAndQuizResultAreExplicit() {
        val s = read("app/src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(s.contains("SonHarfCosmetics.darkArenaTheme -> SonHarfTheme.Background"))
        assertTrue(s.contains("BERABERE • SEN"))
        assertTrue(s.contains("RAKİP ${'$'}{opponentAnswer"))
    }
    @Test fun siegeStartsReadableAndUsesCompactAccessibleNotice() {
        val board = read("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        val screen = read("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")
        assertTrue(board.contains("mutableStateOf(WordSiegeBoardViewportMode.CLOSE)"))
        assertTrue(screen.contains("Text(message, color = MainUi.Text"))
        assertTrue(screen.contains("maxLines = 2"))
        assertTrue(screen.contains("WordSiegeTempoBanner("))
    }
}
