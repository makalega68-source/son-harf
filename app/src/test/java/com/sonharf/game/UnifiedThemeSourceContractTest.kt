package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeUnifiedShellFollowsEquippedThemeAndPremierKeepsCalmHighLegibilityArenaPalette() {
        val unified = source("UnifiedProApp.kt")
        val premier = source("PremierWordDuelScreen.kt")
        val startup = source("StableV1App.kt")
        val theme = source("SonHarfTheme.kt")

        assertTrue(unified.contains("val Background: Color get() = SonHarfTheme.Background"))
        assertTrue(unified.contains("val Surface: Color get() = SonHarfTheme.Surface"))
        assertTrue(unified.contains("val Navigation: Color get() = SonHarfTheme.NavigationSurface"))
        assertTrue(unified.contains("lightColorScheme("))
        assertTrue(unified.contains("darkColorScheme("))
        assertFalse(unified.contains("MageCatCompanion("))
        assertFalse(unified.contains("MageCatDirector.onLobbyGreet()"))
        assertFalse(unified.contains("com.sonharf.game.mascot"))
        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))
        assertTrue(theme.contains("val IsDark: Boolean get() = dark"))
        assertTrue(theme.contains("val SecondaryAccent: Color get()"))
        assertTrue(theme.contains("val NavigationSurface: Color get()"))
        assertTrue(theme.contains("val GameSurface: Color get()"))
        assertTrue(theme.contains("val GameTile: Color get()"))
        assertTrue(theme.contains("val HeroStart: Color get()"))

        // Premier remains a fixed high-legibility competitive surface, but now belongs to the
        // same calm sage / cream / gray-blue visual family as the application shell.
        assertTrue(premier.contains("val Background = Color(0xFFF1F5F2)"))
        assertTrue(premier.contains("val Surface = Color(0xFFFFFDF7)"))
        assertTrue(premier.contains("val Ocean = Color(0xFF4F725E)"))
        assertTrue(premier.contains("val Sky = Color(0xFF4A6E83)"))
        assertTrue(premier.contains("Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background))"))
        assertFalse(premier.contains("val Ocean = Color(0xFF2563EB)"))
        assertFalse(premier.contains("val Background = Color(0xFF020617)"))
        assertFalse(premier.contains("val Surface = Color(0xFF0F172A)"))
        assertFalse(premier.contains("MageCatCompanion("))
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
