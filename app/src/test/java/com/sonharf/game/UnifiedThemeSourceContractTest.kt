package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeShellUsesSingleEquippedThemeSource() {
        val theme = source("SonHarfTheme.kt")
        val shell = source("MonsterExperienceApp.kt")
        val aliases = source("MainExperienceApp.kt")

        listOf("Background", "Surface", "SurfaceSecondary", "PrimaryBlue", "PrimaryBlueSoft",
            "TextPrimary", "TextSecondary", "Border", "Success", "Error", "Warning",
            "DisabledBackground", "DisabledContent").forEach { assertTrue(theme.contains("val $it")) }
        assertTrue(theme.contains("SonHarfCosmetics.darkArenaTheme"))
        assertTrue(shell.contains("val Background: Color get() = SonHarfTheme.Background"))
        assertTrue(aliases.contains("val Background: Color get() = SonHarfTheme.Background"))
        assertFalse(shell.contains("Color(0xFF07111F)"))
        assertFalse(shell.contains("Color(0xFF111D2E)"))
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
