package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeChromeRegressionTest {
    @Test
    fun practiceHeaderProfilesAndFooterKeepCompactPhoneAlignment() {
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        assertTrue(shell.contains("topBar = { SonHarfTopAdBanner(isPremium = isPro) }"))
        assertFalse(practice.contains(".statusBarsPadding().navigationBarsPadding()"))
        assertTrue(practice.contains("height(if (compact) 46.dp else 52.dp)"))
        assertTrue(practice.contains("fontSize = if (compact) 8.sp else 9.sp"))
        assertTrue(practice.contains("lineHeight = if (compact) 10.sp else 11.sp"))
        assertTrue(practice.contains("Modifier.weight(1f).fillMaxHeight()"))
        assertTrue(practice.contains("height(if (compact) 18.dp else 22.dp)"))
        assertTrue(practice.contains("height(if (compact) 24.dp else 30.dp)"))
        assertTrue(practice.contains("if (notice != null || lastMove != null)"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
