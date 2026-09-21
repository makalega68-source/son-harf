package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activePremiumShellUsesPurchasedLightThemeAndPremierKeepsHighLegibilityArenaPalette() {
        val active = source("PremiumUnifiedProApp.kt")
        val unified = source("UnifiedProApp.kt")
        val premier = source("PremierWordDuelScreen.kt")
        val startup = source("StableV1App.kt")
        val theme = source("SonHarfTheme.kt")

        assertTrue(unified.contains("val Background: Color get() = SonHarfTheme.Background"))
        assertTrue(unified.contains("val Surface: Color get() = SonHarfTheme.Surface"))
        assertTrue(unified.contains("val Navigation: Color get() = SonHarfTheme.NavigationSurface"))
        assertTrue(active.contains("lightColorScheme("))
        assertTrue(active.contains("darkColorScheme("))
        assertTrue(active.contains("containerColor = SonHarfTheme.Background"))
        assertTrue(active.contains("PurchasedGameBackdrop(Modifier.matchParentSize())"))
        assertFalse(active.contains("MageCatCompanion("))
        assertFalse(active.contains("com.sonharf.game.mascot"))
        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))
        assertTrue(theme.contains("val IsDark: Boolean get() = false"))
        assertTrue(theme.contains("val SecondaryAccent: Color get()"))
        assertTrue(theme.contains("val NavigationSurface: Color get()"))
        assertTrue(theme.contains("val GameSurface: Color get()"))
        assertTrue(theme.contains("val GameTile: Color get()"))
        assertTrue(theme.contains("val HeroStart: Color get()"))
        assertTrue(theme.contains("Color(0xFF52AD56)"))
        assertTrue(theme.contains("Color(0xFF4D83DA)"))
        assertTrue(theme.contains("Color(0xFFF5A623)"))

        assertTrue(premier.contains("val Background = Color(0xFFF4F8FC)"))
        assertTrue(premier.contains("val Surface = Color(0xFFFFFFFF)"))
        assertTrue(premier.contains("val Ocean = Color(0xFF52AD56)"))
        assertTrue(premier.contains("val Sky = Color(0xFF4D83DA)"))
        assertTrue(premier.contains("val Ink = Color(0xFF102A56)"))
        assertFalse(premier.contains("val Background = Color(0xFF020617)"))
        assertFalse(premier.contains("val Surface = Color(0xFF0F172A)"))
        assertFalse(premier.contains("MageCatCompanion("))
        assertFalse(active.contains("MonsterExperienceApp"))
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
