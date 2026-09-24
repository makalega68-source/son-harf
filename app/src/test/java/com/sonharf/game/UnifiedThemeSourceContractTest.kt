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

        assertTrue(design.contains("val AppBackground = Color(0xFFEAF6F8)"))
        assertTrue(design.contains("val PrimarySurface = Color(0xFFFFFFFF)"))
        assertTrue(design.contains("val PrimaryBlue = Color(0xFF14B8B0)"))
        assertTrue(design.contains("val TacticalTurquoise = Color(0xFF22C3C9)"))
        assertTrue(design.contains("val PlayGreen = Color(0xFF38C970)"))
        assertTrue(design.contains("val RewardAmber = Color(0xFFFF8A2A)"))

        assertTrue(design.contains("val SonHarfBackground = Color(0xFFF3EEE5)"))
        assertTrue(design.contains("val SonHarfSurface = Color(0xFFFFFBF4)"))
        assertTrue(design.contains("val SonHarfInk = Color(0xFF173247)"))
        assertTrue(design.contains("val SonHarfOcean = Color(0xFF4F8F96)"))
        assertTrue(design.contains("val SonHarfGreen = Color(0xFF789B73)"))
        assertTrue(design.contains("val SonHarfRival = Color(0xFFD27869)"))

        // Son Harf keeps the PR #459 board and light sky arena; the mascot is moment-only.
        assertTrue(premier.contains("private object PremierArenaSky"))
        assertTrue(premier.contains("Color(0xFFEAF8FF)"))
        assertTrue(premier.contains("PremierKeyboard(language, input"))
        assertTrue(premier.contains("rememberWordSiegeMascotMoment("))
        assertFalse(premier.contains("val Background = Color(0xFFF3EEE5)"))
        assertFalse(premier.contains("MageCatCompanion("))
    }

    @Test
    fun legacyThemeFacadeCannotReintroduceRetiredNeonPalette() {
        val legacy = source("SonHarfTheme.kt")

        listOf(
            "val Background: Color get() = if (alternateDark)",
            "else GameColors.AppBackground",
            "val Surface: Color get() = if (alternateDark)",
            "else GameColors.PrimarySurface",
            "val Primary: Color get() = GameColors.PrimaryBlue",
            "val Turquoise: Color get() = GameColors.TacticalTurquoise",
            "val Success: Color get() = GameColors.PlayGreen",
            "val Error: Color get() = GameColors.Danger",
            "val PremiumGold: Color get() = GameColors.PrestigeGold",
            "val HeroStart: Color get() = GameColors.HeroStart",
        ).forEach { token -> assertTrue("Missing professional legacy-theme mapping: $token", legacy.contains(token)) }

        listOf(
            "0xFFEFFF19",
            "0xFFFF245C",
            "0xFFFF3B30",
            "Monster UI kit'ten türetilen",
            "Signature Monster red",
        ).forEach { retired -> assertFalse("Retired theme token remains: $retired", legacy.contains(retired)) }
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
