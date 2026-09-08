package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeUnifiedShellFollowsEquippedThemeAndPremierKeepsFocusedArenaPalette() {
        val unified = source("UnifiedProApp.kt")
        val premier = source("PremierWordDuelScreen.kt")
        val startup = source("StableV1App.kt")
        val theme = source("SonHarfTheme.kt")

        assertTrue(unified.contains("val Background: Color get() = SonHarfTheme.Background"))
        assertTrue(unified.contains("val Surface: Color get() = SonHarfTheme.Surface"))
        assertTrue(unified.contains("lightColorScheme("))
        assertTrue(unified.contains("darkColorScheme("))
        assertTrue(unified.contains("MageCatCompanion("))
        assertTrue(unified.contains("MageCatDirector.onLobbyGreet()"))
        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))
        assertTrue(theme.contains("val IsDark: Boolean get() = dark"))
        assertTrue(theme.contains("val SecondaryAccent: Color get()"))

        // Premier gameplay intentionally keeps its focused dark arena while the app shell
        // follows the user's equipped Blue/White or Night Arena theme.
        assertTrue(premier.contains("val Background = Color(0xFF020617)"))
        assertTrue(premier.contains("val Surface = Color(0xFF0F172A)"))
        assertFalse(unified.contains("MonsterUi"))
        assertFalse(unified.contains("MonsterExperienceApp"))
    }

    @Test
    fun buildWorkflowsNeverMutateSourcesWithLegacyThemeScripts() {
        val workflows = projectFile(".github/workflows").walkTopDown()
            .filter { it.isFile && it.extension in setOf("yml", "yaml") }
            .joinToString("\n") { it.readText() }
        assertFalse(workflows.contains("apply_monster_duel_theme.py"))
        assertFalse(workflows.contains("rebuild_monster_duel_layout.py"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
