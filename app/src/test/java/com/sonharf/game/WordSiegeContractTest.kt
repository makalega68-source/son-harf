package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeContractTest {
    @Test fun siegeNavigationUsesCanonical15x15ExperienceAndOldDamageModeIsGone() {
        val entry = projectFile("app/src/main/java/com/sonharf/game/WordSiegeGame.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val zoneRules = projectFile("app/src/main/java/com/sonharf/game/WordSiegeZoneRules.kt").readText()
        val home = projectFile("app/src/main/java/com/sonharf/game/LightWordThemeApp.kt").readText()

        assertTrue(entry.contains("WordSiegeExperienceScreen(onExit = onExit)"))
        assertTrue(experience.contains("WordSiegePanMatch("))
        assertTrue(practice.contains("WordSiegePracticeBoard("))
        assertTrue(zoneRules.contains("ZoneCount"))
        assertTrue(zoneRules.contains("FortressZoneIds"))
        assertTrue(zoneRules.contains("ConquestMeterMax"))
        assertTrue(home.contains("KELİME KUŞATMASI"))
        assertFalse(home.contains("KELİME FETHİ"))

        assertFalse(entry.contains("SiegeImpact"))
        assertFalse(entry.contains("myHp"))
        assertFalse(entry.contains("botHp"))
        assertFalse(entry.contains("SiegeBonus.FOG"))
        assertFalse(entry.contains("SiegeBonus.BRIDGE"))
        assertFalse(entry.contains("claimSiegeTerritory"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
