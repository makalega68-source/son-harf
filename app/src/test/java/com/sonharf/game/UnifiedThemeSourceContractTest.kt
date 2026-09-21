package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeShellUsesSiegeRoyaleThemeWithOptionalBlackCosmetic() {
        val premium = source("PremiumAdultApp.kt")
        val startup = source("StableV1App.kt")
        val theme = source("SonHarfTheme.kt")
        val cosmetics = source("CosmeticRuntime.kt")
        val primitives = source("AppUiPrimitives.kt")

        assertTrue(premium.contains("SonHarfTheme.Background"))
        assertTrue(premium.contains("SonHarfTheme.NavigationSurface"))
        assertTrue(startup.contains("PremiumAdultApp"))
        assertFalse(startup.contains("PremiumCanvaAppV2(onSignedOut"))
        assertFalse(startup.contains("PremiumUnifiedProApp"))
        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))

        assertTrue(theme.contains("internal object SiegeRoyalePalette"))
        assertTrue(theme.contains("val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive"))
        assertTrue(theme.contains("internal object BlackThemePalette"))
        assertTrue(cosmetics.contains("BLACK_THEME_ID = \"theme_black\""))
        assertTrue(theme.contains("Color(0xFF246EDB)"))
        assertTrue(theme.contains("Color(0xFF17A7B8)"))
        assertTrue(theme.contains("Color(0xFF3AAF50)"))
        assertTrue(theme.contains("Color(0xFFF0A128)"))
        assertTrue(theme.contains("Color(0xFFEAF3FF)"))
        assertTrue(theme.contains("val NavigationSurface: Color get()"))
        assertTrue(theme.contains("val GameSurface: Color get()"))
        assertTrue(theme.contains("val GameTile: Color get()"))
        assertTrue(theme.contains("val HeroStart: Color get()"))
        assertFalse(theme.contains("Color(0xFF365F53)"))
        assertFalse(theme.contains("Color(0xFFF4F2EC)"))
        assertFalse(theme.contains("MonsterLime"))
        assertFalse(theme.contains("MonsterPink"))

        assertTrue(primitives.contains("PremiumCard"))
        assertTrue(primitives.contains("PremiumPrimaryButton"))
        assertTrue(primitives.contains("PremiumAccentPill"))
        assertTrue(primitives.contains("SonHarfTheme.PlayGreen"))
        assertFalse(premium.contains("MageCatCompanion("))
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
