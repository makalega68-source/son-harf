package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotTurnWatchdogContractTest {
    private val watchdog by lazy { projectFile("app/src/main/java/com/sonharf/game/BotTurnWatchdogOverlay.kt").readText() }
    private val mount by lazy { projectFile("app/src/main/java/com/sonharf/game/SketchGameOverlayV9.kt").readText() }

    @Test fun botTurnKeepsRecoveringUntilServerAdvances() {
        assertTrue(watchdog.contains("while (true)"))
        assertTrue(watchdog.contains("eq(\"bot_turn\", true)"))
        assertTrue(watchdog.contains("backend.botTakeTurn(candidate.id)"))
        assertTrue(watchdog.contains("moved.botTurn"))
        assertTrue(watchdog.contains("delay(if (botThinking) 900L else 300L)"))
    }

    @Test fun botThinkStateDoesNotLookLikeExpiredPlayerClock() {
        assertTrue(watchdog.contains("\"BOT …\""))
        assertTrue(mount.contains("BotTurnWatchdogOverlay()"))
        assertTrue(mount.indexOf("BotTurnWatchdogOverlay()") > mount.indexOf("RefinedDuelOverlay()"))
    }

    @Test fun quizOwnershipRemainsUntouched() {
        assertFalse(mount.contains("\"quiz\""))
        assertTrue(mount.contains("\"playing\", \"final\", \"sudden_death\", \"paused\""))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
