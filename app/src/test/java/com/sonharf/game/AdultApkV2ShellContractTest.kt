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
    fun paletteUsesRestrainedWarmNeutralAndForestValues() {
        val theme = source("SonHarfTheme.kt")
        assertTrue(theme.contains("0xFFF4F2EC"))
        assertTrue(theme.contains("0xFF365F53"))
        assertTrue(theme.contains("0xFFAD6A57"))
        assertFalse(theme.contains("0xFF7C3AED"))
    }

    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()
}
