package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultApkV2ShellContractTest {
    @Test
    fun stableAppUsesAdultShellInsteadOfLegacyCanvaShell() {
        val stable = source("StableV1App.kt")
        assertTrue(stable.contains("PremiumAdultApp(onSignedOut"))
        assertFalse(stable.contains("PremiumCanvaAppV2(onSignedOut"))
    }

    @Test
    fun bottomNavigationContainsOnlyFourProductTabs() {
        val shell = source("PremiumAdultApp.kt")
        val bottomBar = shell.substringAfter("private fun AdultBottomBar(")
        assertTrue(bottomBar.contains("Ana Sayfa"))
        assertTrue(bottomBar.contains("Sosyal"))
        assertTrue(bottomBar.contains("Mağaza"))
        assertTrue(bottomBar.contains("Profil"))
        assertFalse(bottomBar.contains("Games"))
        assertFalse(bottomBar.contains("Oyunlar"))
        assertTrue(bottomBar.contains("height(64.dp)"))
    }

    @Test
    fun wordKeyboardHasNoClearKey() {
        val shared = source("SharedInputPrimitives.kt")
        val harfYolu = source("HarfYoluKeyboard.kt")
        assertFalse(shared.contains("TEMİZLE"))
        assertFalse(shared.contains("CLEAR"))
        assertFalse(harfYolu.contains("TEMİZLE"))
        assertFalse(harfYolu.contains("CLEAR"))
    }

    @Test
    fun paletteUsesApprovedNativeDarkProfessionalValues() {
        val colors = source("Color.kt")
        listOf(
            "0xFF141923",
            "0xFF1E2538",
            "0xFF2ECC71",
            "0xFF3498DB",
            "0xFF9B59B6",
            "0xFFE67E22",
            "0xFFECF0F1",
            "0xFF95A5A6",
        ).forEach { token -> assertTrue("Missing native palette token $token", colors.contains(token)) }

        val shell = source("PremiumAdultApp.kt")
        assertTrue(shell.contains("darkColorScheme("))
        assertTrue(shell.contains("typography = AppTypography"))
        assertTrue(shell.contains("imePadding()"))
    }

    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()
}
