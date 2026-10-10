package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeContractTest {
    @Test fun siegeReplacesConquestAndIsPrimaryFromUnifiedPro() {
        val home = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()


        // Legacy UnifiedProApp remains untouched; the active StableV1App -> PremiumUnifiedProApp flow owns the current brand.
        assertTrue(home.contains("PremiumPlayButton(onClick = onSiege)"))
        assertTrue(home.contains("sh(\"KELİME TAHTI\", \"WORD THRONE\")"))
        assertTrue(home.contains("UnifiedDestination.SIEGE -> WordSiegeExperienceScreen"))
        assertTrue(home.contains("SAVAŞA GİR"))
        assertFalse(home.contains("KELİME FETHİ"))
        assertFalse(projectFile("app/src/main/java/com/sonharf/game").resolve("WordConquestGame.kt").exists())
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
