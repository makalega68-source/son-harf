package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeShellUsesPremiumCanvaThemeAndKeepsOneThemeSource() {
        val premium = source("PremiumUnifiedProApp.kt")
        val startup = source("StableV1App.kt")
        val theme = source("SonHarfTheme.kt")
        val primitives = source("AppUiPrimitives.kt")

        assertTrue(premium.contains("SonHarfTheme.Background"))
        assertTrue(premium.contains("SonHarfTheme.NavigationSurface"))
        assertTrue(premium.contains("lightColorScheme("))
        assertTrue(startup.contains("PremiumUnifiedProApp"))
        assertTrue(startup.contains("PremiumScreenBackground"))
        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))

        assertTrue(theme.contains("val IsDark: Boolean get() = false"))
        assertTrue(theme.contains("Color(0xFF2563EB)"))
        assertTrue(theme.contains("Color(0xFF12B8A6)"))
        assertTrue(theme.contains("Color(0xFF7C3AED)"))
        assertTrue(theme.contains("Color(0xFFF97316)"))
        assertTrue(theme.contains("val SecondaryAccent: Color get()"))
        assertTrue(theme.contains("val NavigationSurface: Color get()"))
        assertTrue(theme.contains("val GameSurface: Color get()"))
        assertTrue(theme.contains("val GameTile: Color get()"))
        assertTrue(theme.contains("val HeroStart: Color get()"))
        assertFalse(theme.contains("MonsterLime"))
        assertFalse(theme.contains("MonsterPink"))
        assertFalse(theme.contains("Color(0xFFEFFF19)"))

        assertTrue(primitives.contains("PremiumCard"))
        assertTrue(primitives.contains("PremiumPrimaryButton"))
        assertTrue(primitives.contains("PremiumAccentPill"))
        assertTrue(primitives.contains("PremiumScreenBackground"))

        assertFalse(premium.contains("MageCatCompanion("))
        assertFalse(premium.contains("com.sonharf.game.mascot"))
        assertFalse(premium.contains("MonsterExperienceApp"))
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
