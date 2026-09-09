package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProRuntimeContractTest {
    @Test
    fun startupRoutesOnlyToUnifiedProAndPremierDuel() {
        val startup = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val unified = File("src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()
        val integration = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        assertTrue(startup.contains("UnifiedProApp("))
        assertFalse(startup.contains("LiveDuelRuntimeShell("))
        assertFalse(startup.contains("MonsterExperienceApp("))
        assertTrue(unified.contains("Premier 1v1"))
        assertTrue(integration.contains("PremierWordDuelScreen()"))
        assertFalse(integration.contains("ReactiveMageCatOverlay()"))
        assertTrue(File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText().contains("ReactiveMageCatOverlay("))
        assertTrue(integration.contains("PremierBoosterOverlay()"))
        assertFalse(File("src/main/java/com/sonharf/game/LiveDuelRuntimeShell.kt").exists())
        assertFalse(File("src/main/java/com/sonharf/game/MonsterExperienceApp.kt").exists())
    }
}
