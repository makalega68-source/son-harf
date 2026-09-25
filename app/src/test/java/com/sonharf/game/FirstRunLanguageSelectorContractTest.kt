package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunLanguageSelectorContractTest {
    @Test
    fun languageGatePrecedesAuthAndPersistsChoice() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val prefs = projectFile("app/src/main/java/com/sonharf/game/FirstRunLanguagePreferences.kt").readText()

        assertTrue(shell.contains("FirstRunLanguagePreferences.isComplete"))
        assertTrue(shell.indexOf("FirstRunLanguageScreen") < shell.indexOf("hasVerifiedMembershipSession"))
        assertTrue(shell.contains("Dilini seç / Choose your language"))
        assertTrue(shell.contains("TÜRKÇE"))
        assertTrue(shell.contains("ENGLISH"))
        assertTrue(shell.contains("GameColors.AppBackground"))
        assertTrue(shell.contains("GameColors.PrimaryBlue"))
        assertFalse(shell.contains("MainUi.Background"))
        assertFalse(shell.contains("MonsterUi.Background"))
        assertTrue(prefs.contains("language_complete"))
        assertTrue(prefs.contains("SonHarfPreferences.setLanguage"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
