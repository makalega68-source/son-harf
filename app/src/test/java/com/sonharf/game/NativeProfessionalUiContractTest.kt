package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeProfessionalUiContractTest {
    @Test
    fun nativeShellFollowsApprovedPaletteImeAndNavigationContract() {
        val colors = source("Color.kt")
        val shell = source("PremiumAdultApp.kt")
        val siege = source("WordSiegeExperience.kt")
        val pack = source("NativePackVisuals.kt")

        listOf("141923", "1E2538", "2ECC71", "3498DB", "9B59B6", "E67E22", "ECF0F1", "95A5A6")
            .forEach { token -> assertTrue("Missing native UI token $token", colors.contains(token)) }
        assertTrue(shell.contains("darkColorScheme("))
        assertTrue(shell.contains("typography = AppTypography"))
        assertTrue(shell.contains("height(64.dp)"))
        assertTrue(shell.contains("imePadding()"))
        assertTrue(siege.contains("ImeAction.Send"))
        assertTrue(siege.contains("keyboardActions = KeyboardActions"))
        assertFalse(siege.contains("0xFFFFE3A5"))
        assertFalse(siege.contains("0xFFD99818"))

        assertTrue(pack.contains("ui_ranking_icon"))
        assertTrue(pack.contains("ui_legacy_rank_frame"))
        assertTrue(pack.contains("vfx_twinkle"))
    }

    @Test
    fun currentGameRulesAndMonetizationRemainUntouched() {
        val rules = source("WordSiegeFinalRules.kt")
        val economy = source("EconomyShopScreen.kt")
        assertTrue(rules.contains("CUBE_TRANSFER_POINTS: Int = 2"))
        assertTrue(rules.contains("wordScore + cubeTransfer(ownedCubes)"))
        assertTrue(rules.contains("Word points are permanent"))
        assertTrue(economy.contains("Mağaza ürünleri maç gücü, skor veya rating avantajı sağlamaz."))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()
    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
