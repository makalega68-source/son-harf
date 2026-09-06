package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotRegressionFixContractTest {
    // These contracts are intentionally source-level guards for the screenshot regressions fixed in this PR.
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
    @Test fun siegeStartsReadableAndUsesAccessibleNotice() {
        assertTrue(read("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").contains("mutableStateOf(WordSiegeBoardViewportMode.CLOSE)"))
        assertTrue(read("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").contains("WordSiegeNotice(message)"))
    }
}
