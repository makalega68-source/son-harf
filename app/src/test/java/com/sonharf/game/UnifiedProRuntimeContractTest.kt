package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProRuntimeContractTest {
    @Test
    fun startupRoutesOnlyToUnifiedProAndCleanPremierDuel() {
        val startup = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val unified = File("src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()
        val integration = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        assertTrue(startup.contains("UnifiedProApp("))
        assertFalse(startup.contains("LiveDuelRuntimeShell("))
        assertFalse(startup.contains("MonsterExperienceApp("))
        assertTrue(unified.contains("Premier 1v1"))
        assertFalse(unified.contains("MageCat"))
        assertTrue(integration.contains("PremierWordDuelScreen()"))
        assertFalse(integration.contains("ReactiveMageCatOverlay()"))
        assertFalse(integration.contains("PremierBoosterOverlay()"))
        assertTrue(integration.contains("Ranked Premier is skill-only"))
        assertFalse(File("src/main/java/com/sonharf/game/mascot").exists())
        assertFalse(File("src/main/res/drawable-nodpi/mage_cat_runtime.webp").exists())
        assertFalse(File("src/main/java/com/sonharf/game/LiveDuelRuntimeShell.kt").exists())
        assertFalse(File("src/main/java/com/sonharf/game/MonsterExperienceApp.kt").exists())
    }
}
