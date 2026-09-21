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
    fun paletteUsesNewRoyalBlueAquaEmeraldGameDirection() {
        val theme = source("SonHarfTheme.kt")
        assertTrue(theme.contains("0xFFEAF3FF"))
        assertTrue(theme.contains("0xFF246EDB"))
        assertTrue(theme.contains("0xFF17A7B8"))
        assertTrue(theme.contains("0xFF3AAF50"))
        assertTrue(theme.contains("internal object SiegeRoyalePalette"))
        assertFalse(theme.contains("0xFFF4F2EC"))
        assertFalse(theme.contains("0xFF365F53"))
    }

    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()
}
