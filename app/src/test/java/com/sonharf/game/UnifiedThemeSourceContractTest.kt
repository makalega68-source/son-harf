package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeProfessionalShellAndPremierUseProductionGamePalette() {
        val unified = source("ProfessionalUnifiedApp.kt")
        val premier = source("PremierWordDuelScreen.kt")
        val startup = source("StableV1App.kt")
        val design = source("GameDesignSystem.kt")

        assertTrue(unified.contains("GameTheme {"))
        assertTrue(unified.contains("containerColor = GameColors.AppBackground"))
        assertTrue(unified.contains("GameBottomNavigation("))
        assertTrue(unified.contains("ProfessionalDestination.LAST_LETTER -> OnlineGameScreenV6()"))
        assertTrue(unified.contains("ProfessionalDestination.SIEGE -> WordSiegeEntryScreen"))
        assertTrue(unified.contains("ProfessionalDestination.LETTER_PATH -> LetterLadderGameScreen"))
        assertFalse(unified.contains("MonsterUi"))
        assertFalse(unified.contains("MonsterExperienceApp"))
        assertFalse(unified.contains("MageCatCompanion("))
        assertFalse(unified.contains("com.sonharf.game.mascot"))

        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))
        assertTrue(startup.contains("GameTheme {"))
        assertTrue(startup.contains("GameColors.AppBackground"))

        assertTrue(design.contains("val AppBackground = Color(0xFF101722)"))
        assertTrue(design.contains("val PrimarySurface = Color(0xFF1C2939)"))
        assertTrue(design.contains("val PrimaryBlue = Color(0xFF3D8BFF)"))
        assertTrue(design.contains("val TacticalTurquoise = Color(0xFF20B6B0)"))
        assertTrue(design.contains("val PlayGreen = Color(0xFF38C970)"))
        assertTrue(design.contains("val RewardAmber = Color(0xFFF2A73B)"))

        assertTrue(premier.contains("val Background = GameColors.AppBackground"))
        assertTrue(premier.contains("val Surface = GameColors.PrimarySurface"))
        assertTrue(premier.contains("val Ink = GameColors.TextPrimary"))
        assertTrue(premier.contains("val Ocean = GameColors.PrimaryBlue"))
        assertTrue(premier.contains("val Sky = GameColors.TacticalTurquoise"))
        assertTrue(premier.contains("val Green = GameColors.PlayGreen"))
        assertTrue(premier.contains("val Red = GameColors.Danger"))
        assertTrue(premier.contains("Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background))"))
        assertFalse(premier.contains("val Background = Color(0xFFF1F5F2)"))
        assertFalse(premier.contains("val Surface = Color(0xFFFFFDF7)"))
        assertFalse(premier.contains("MageCatCompanion("))
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
