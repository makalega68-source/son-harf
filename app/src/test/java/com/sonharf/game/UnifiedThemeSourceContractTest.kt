package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeUnifiedShellUsesPremiumDarkPaletteAndNoLegacyMonsterTheme() {
        val unified = source("UnifiedProApp.kt")
        val premier = source("PremierWordDuelScreen.kt")

        assertTrue(unified.contains("val Background = Color(0xFF020617)"))
        assertTrue(unified.contains("val Surface = Color(0xFF0F172A)"))
        assertTrue(unified.contains("darkColorScheme("))
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
