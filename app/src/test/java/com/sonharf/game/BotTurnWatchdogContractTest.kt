package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotTurnWatchdogContractTest {
    private val watchdog by lazy { projectFile("app/src/main/java/com/sonharf/game/BotTurnWatchdogOverlay.kt").readText() }
    private val mount by lazy { projectFile("app/src/main/java/com/sonharf/game/LiveDuelRuntimeShell.kt").readText() }

    @Test fun botTurnKeepsRecoveringUntilServerAdvances() {
        assertTrue(watchdog.contains("internal fun BotTurnWatchdogOverlay(roomId: String)"))
        assertTrue(watchdog.contains("LaunchedEffect(roomId)"))
        assertTrue(watchdog.contains("while (true)"))
        assertTrue(watchdog.contains("backend.getRoom(roomId)"))
        assertTrue(watchdog.contains("it.botTurn"))
        assertTrue(watchdog.contains("backend.botTakeTurn(candidate.id)"))
        assertTrue(watchdog.contains("withTimeoutOrNull(4_000L)"))
        assertTrue(watchdog.contains("withTimeoutOrNull(6_000L)"))
        assertTrue(watchdog.contains("delay(if (stillThinking) 1_200L else 700L)"))
    }

    @Test fun botThinkRecoveryIsMountedWithoutASecondVisibleOverlay() {
        assertFalse(watchdog.contains("\"BOT …\""))
        assertTrue(mount.contains("BotTurnWatchdogOverlay(roomId = state.roomId)"))
        assertTrue(
            mount.indexOf("BotTurnWatchdogOverlay(roomId = state.roomId)") >
                mount.indexOf("RefinedDuelOverlay()"),
        )
    }

    @Test fun watchdogDoesNotScanTheWholeRoomTable() {
        assertFalse(watchdog.contains("from(\"game_rooms\")"))
        assertFalse(watchdog.contains("decodeList<GameRoomDto>()"))
        assertTrue(watchdog.contains("backend.getRoom(roomId)"))
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
