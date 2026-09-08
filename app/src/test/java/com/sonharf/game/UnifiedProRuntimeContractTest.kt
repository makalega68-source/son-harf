package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProRuntimeContractTest {
    @Test
    fun startupRoutesToUnifiedProAndPremierDuel() {
        val startup = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val unified = File("src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()
        val integration = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        assertTrue(startup.contains("UnifiedProApp("))
        assertFalse(startup.contains("LiveDuelRuntimeShell("))
        assertTrue(unified.contains("Premier 1v1"))
        assertTrue(integration.contains("PremierWordDuelScreen()"))
    }
}
