package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeUnifiedShellKeepsMetaThemeAndPremierUsesSkyMascotArena()  {
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
        assertTrue(theme.contains("val IsDark: Boolean get() = false"))
        assertTrue(theme.contains("val SecondaryAccent: Color get()"))
        assertTrue(theme.contains("val NavigationSurface: Color get()"))
        assertTrue(theme.contains("val GameSurface: Color get()"))
        assertTrue(theme.contains("val GameTile: Color get()"))
        assertTrue(theme.contains("val HeroStart: Color get()"))
        assertTrue(theme.contains("Color(0xFF14B8B0)"))
        assertTrue(theme.contains("Color(0xFF8B6CF0)"))

        // The approved meta shell stays light; Son Harf now uses a scoped sky-blue arena
        // with the shared in-game mascot while preserving the application theme elsewhere.
        assertTrue(premier.contains("private object PremierArenaSky"))
        assertTrue(premier.contains("Color(0xFFEAF8FF)"))
        assertTrue(premier.contains("Color(0xFFDDF3FC)"))
        assertTrue(premier.contains("Color(0xFFCFEAF7)"))
        assertTrue(premier.contains("Brush.verticalGradient("))
        assertTrue(premier.contains("WordSiegeMascot("))
        assertTrue(premier.contains("PremierKeyboard(language, input"))
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
