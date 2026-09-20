package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicWordSiegeTimeoutRefreshContractTest {
    @Test fun openClassicMatchUsesServerRefreshSoExpiredTurnsFinalize() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        assertTrue(screen.contains("backend.refreshWordSiegeGame(gameId)"))
        assertFalse(screen.contains("runCatching { backend.getWordSiegeGame(gameId) }"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
