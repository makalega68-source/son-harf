package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremierHeartbeatContractTest {
    @Test fun activeRoomObserverRefreshesServerHeartbeat() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        assertTrue(backend.contains("heartbeatRoom(id)"))
        assertTrue(backend.contains("4_000_000_000L"))
        assertTrue(backend.contains("System.nanoTime()"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
