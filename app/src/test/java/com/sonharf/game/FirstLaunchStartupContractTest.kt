package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstLaunchStartupContractTest {
    @Test fun mainActivityNeverBlocksBeforeComposeAndHasTimeoutFallback() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MainActivity.kt").readText()
        assertFalse(main.contains("runBlocking"))
        assertTrue(main.contains("AppStartupGate"))
        assertTrue(main.contains("withTimeoutOrNull"))
        assertTrue(main.contains("StartupError"))
        assertTrue(main.contains("setContent"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
