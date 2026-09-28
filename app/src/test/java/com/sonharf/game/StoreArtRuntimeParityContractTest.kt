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
        // The beret is the same image with its dark inside cut away, so it sits on Obi's head.
        listOf("hat_beret_worn", "store_art_hat_flower", "store_art_hat_wizard", "store_art_hat_top", "store_art_victory_crown")
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
        assertTrue(board.contains("SonHarfCosmetics.walnutTheme || SonHarfCosmetics.darkArenaTheme"))
        assertTrue(board.contains("ink = Color(0xFFF2C75C)"))
        assertTrue(source("MainPlayerProfileScreen.kt").contains("NameStyleEmblem(30.dp)"))
    }

    @Test fun paintedKeyboardPackIsWiredToTheExistingKeyboardProducts() {
        val runtime = source("CosmeticRuntime.kt")
        listOf("keyboard_crystal", "keyboard_obsidian", "keyboard_midnight", "keyboard_black_gold", "keyboard_premium_white").forEach { id ->
            assertTrue("$id key art", art("${id}_key").isFile)
            assertTrue("$id panel art", art("${id}_panel").isFile)
            assertTrue(runtime.contains("keyImage = R.drawable.${id}_key, panelImage = R.drawable.${id}_panel"))
        }
        val skin = source("KeyboardSkin.kt")
        // Panel behind the real buttons, sliced so crown and corners keep their shape.
        assertTrue(skin.contains("internal fun Modifier.keyboardTray(p: WordKeyboardPalette, shape: Shape, crownBand: Dp = 30.dp)"))
        assertTrue(skin.contains("val top = (ih * p.panelTop).toInt()"))
        assertTrue(skin.contains("placeable.place(l, t)"))
        assertTrue(skin.contains("colorFilter = if (pressed) PressedBrightness else null"))
        assertTrue(skin.contains("internal fun KeyboardSkinPreview(themeId: String"))
        assertTrue(source("StoreProductPreview.kt").contains("KeyboardSkinPreview(item.id"))
        // Letters, layout and send/delete logic stay in the existing keyboards.
        assertTrue(source("PremierWordDuelScreen.kt").contains("rememberSkinImage(palette.keyImage), pressed)"))
        assertTrue(source("SharedInputPrimitives.kt").contains("border = if (palette.panelImage == null) BorderStroke(1.dp, palette.border) else null"))
    }
}
