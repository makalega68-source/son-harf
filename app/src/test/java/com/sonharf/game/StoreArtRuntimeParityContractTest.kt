package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the store shows is what the player gets: the in-use look is built from the same art. */
class StoreArtRuntimeParityContractTest {
    private fun source(name: String) = File("src/main/java/com/sonharf/game/$name").readText()
    private fun art(name: String) = File("src/main/res/drawable-nodpi/$name.png")

    @Test fun everyProductWithoutPaintedArtNowHasIt() {
        listOf(
            "store_art_theme_black", "store_art_theme_walnut_ivory", "store_art_keyboard_crystal",
            "store_art_hat_beret", "store_art_hat_flower", "store_art_hat_wizard", "store_art_hat_top", "premium_pro",
        ).forEach { assertTrue("Missing $it", art(it).isFile) }
        listOf("store_art_theme_black", "store_art_keyboard_crystal", "premium_pro").forEach {
            assertFalse("Old vector $it must be gone", File("src/main/res/drawable/$it.xml").exists())
        }
        val preview = source("StoreProductPreview.kt")
        assertTrue(preview.contains("WALNUT_IVORY_THEME_ID -> R.drawable.store_art_theme_walnut_ivory"))
        assertTrue(preview.contains("MascotHats.WIZARD -> R.drawable.store_art_hat_wizard"))
    }

    @Test fun obiWearsTheVeryHatImageTheStoreSells() {
        val painter = source("MascotHatPainter.kt")
        listOf("store_art_hat_beret", "store_art_hat_flower", "store_art_hat_wizard", "store_art_hat_top", "store_art_victory_crown")
            .forEach { assertTrue(painter.contains("R.drawable.$it")) }
        assertTrue(painter.contains("canvas.drawBitmap(bitmap, src, dst, paint)"))
        assertTrue(source("WordSiegeMascotView.kt").contains("if (boughtHats.draws(hat)) {"))
    }

    @Test fun keyboardsAndBoardsAreStyledFromTheirArt() {
        val runtime = source("CosmeticRuntime.kt")
        assertTrue(runtime.contains("glow = Color(0xFF2EC8FF), trayTop = Color(0xFFF8F9FC), rim = Color(0xFFD4AF37), crystal = true"))
        val skin = source("KeyboardSkin.kt")
        assertTrue(skin.contains("internal fun Modifier.keyFace("))
        assertTrue(skin.contains("if (p.crystal) {"))
        assertTrue(source("SharedInputPrimitives.kt").contains("SkinKey(kind, enabled, 11.dp"))
        assertTrue(source("PremierWordDuelScreen.kt").contains("SkinKey(kind, enabled, 6.dp"))
        val board = source("WordSiegeWalnutIvory.kt")
        assertTrue(board.contains("SonHarfCosmetics.gameThemeId == WALNUT_IVORY_THEME_ID || SonHarfCosmetics.darkArenaTheme"))
        assertTrue(board.contains("ink = Color(0xFFF2C75C)"))
        assertTrue(source("MainPlayerProfileScreen.kt").contains("NameStyleEmblem(30.dp)"))
    }
}
