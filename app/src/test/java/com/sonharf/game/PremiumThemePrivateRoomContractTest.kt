package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Regression guard for runtime Style palettes and keyboard-safe private-room controls.
class PremiumThemePrivateRoomContractTest {
    private fun source(name: String) = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun mainUiUsesEquippedRuntimePalette() {
        val src = source("MainExperienceApp.kt")
        assertTrue(src.contains("val Background: Color get() = SonHarfCosmetics.gamePalette.background"))
        assertTrue(src.contains("val Surface: Color get() = SonHarfCosmetics.gamePalette.surface"))
        assertTrue(src.contains("val Text: Color get() = SonHarfCosmetics.gamePalette.text"))
        assertTrue(src.contains("val Border: Color get() = SonHarfCosmetics.gamePalette.border"))
        assertTrue(src.contains("NavigationBar(containerColor = MainUi.Surface"))
        assertFalse(src.contains("NavigationBar(containerColor = Color.White"))
    }

    @Test fun duelLobbyIsImeAndNavigationBarSafe() {
        val src = source("LightDuelUi.kt")
        assertTrue(src.contains("Modifier.fillMaxSize().navigationBarsPadding().imePadding()"))
        assertTrue(src.contains("private val LCard: Color get() = SonHarfCosmetics.gamePalette.surface"))
        assertTrue(src.contains("private val LText: Color get() = SonHarfCosmetics.gamePalette.text"))
        assertTrue(src.contains("private val LBorder: Color get() = SonHarfCosmetics.gamePalette.border"))
        assertFalse(src.contains("containerColor = Color.White"))
        assertFalse(src.contains("Brush.verticalGradient(listOf(Color.White"))
    }
}
