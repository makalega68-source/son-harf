package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BotFreezeThemeContrastRegressionTest {
    private fun source(name: String): String =
        File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun activeDuelKeepsContinuousBotRecoveryMounted() {
        val runtime = source("LiveDuelRuntimeShell.kt")
        val watchdog = source("BotTurnWatchdogOverlay.kt")

        assertTrue(runtime.contains("RefinedDuelOverlay()"))
        assertTrue(runtime.contains("BotTurnWatchdogOverlay()"))
        assertTrue(watchdog.contains("withTimeoutOrNull(4_000L)"))
        assertTrue(watchdog.contains("withTimeoutOrNull(6_000L)"))
        assertTrue(watchdog.contains("while (true)"))
        assertFalse(watchdog.contains("BOT …"))
    }

    @Test fun wordSiegeLightSurfacesRemainReadableInDarkTheme() {
        val experience = source("WordSiegeExperience.kt")
        val practiceBoard = source("WordSiegePracticeBoard.kt")

        assertTrue(experience.contains("SiegePurpleSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) MainUi.SurfaceSoft"))
        assertTrue(experience.contains("private val SiegeLightTileText = Color(0xFF2F2A1F)"))
        assertTrue(experience.contains("color = if (used) MainUi.Muted.copy(alpha = .45f) else SiegeLightTileText"))
        assertTrue(practiceBoard.contains("private val PracticeSiegeLightTileText = Color(0xFF2F2A1F)"))
        assertTrue(practiceBoard.contains("else PracticeSiegeLightTileText"))
    }
}
