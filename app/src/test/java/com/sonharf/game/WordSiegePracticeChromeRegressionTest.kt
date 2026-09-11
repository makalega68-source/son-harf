package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeChromeRegressionTest {
    @Test
    fun practiceHeaderProfilesMapControlAndFooterKeepCompactPhoneAlignment() {
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        assertTrue(shell.contains("topBar = { SonHarfTopAdBanner(isPremium = isPro) }"))
        assertFalse(practice.contains(".statusBarsPadding().navigationBarsPadding()"))
        assertTrue(practice.contains("height(if (compact) 46.dp else 52.dp)"))
        assertTrue(practice.contains("KELİME TAHTI"))
        assertTrue(practice.contains("PracticeMapControlBar("))
        assertTrue(practice.contains("HARİTA KONTROLÜ"))
        assertTrue(practice.contains("PracticeStrategicZoneLegend"))
        assertTrue(practice.contains("HAMLEYİ ONAYLA"))
        assertTrue(practice.contains("KUŞATMA +"))
        assertTrue(practice.contains("if (notice != null || lastMove != null)"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
