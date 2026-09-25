package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeBrandingAndVisibleActionContractTest {
    @Test
    fun homeMakesKelimeKusatmasiPrimaryAndKeepsSecondaryModesBranded() {
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val games = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val brand = projectFile("app/src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeDrawable = projectFile("app/src/main/res/drawable/kelime_kusatma_logo_hd.xml")

        assertTrue(
            home.contains("\"KELİME TAHTI\"") ||
                home.contains("\"KELİME\\nKUŞATMASI\"")
        )
        assertTrue(home.contains("onClick = onSiege"))
        assertTrue(home.contains("sh(\"OYNA\", \"PLAY\")"))
        assertTrue(home.contains("onClick = onPlay"))
        assertTrue(brand.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(siegeDrawable.isFile)
        assertTrue(siegeDrawable.readText().contains("<vector"))
        assertFalse(projectFileOrNull("app/src/main/res/drawable/kelime_kusatma_logo_hd.png")?.exists() == true)
        assertFalse(projectFileOrNull("app/src/main/res/drawable/kelime_kusatma_logo_hd.webp")?.exists() == true)
        assertTrue(games.contains("title = sh(\"KELİME TAHTI\", \"KELİME TAHTI\")"))
        assertTrue(games.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(games.contains("title = sh(\"KELİME ATÖLYESİ\", \"WORD WORKSHOP\")"))
        assertTrue(games.contains("PremiumOtherGames(onLastLetter = onLastLetter, onWorkshop = onWorkshop)"))
        assertTrue(games.contains("onClick = onSiege"))
        assertTrue(games.contains("onClick = onLastLetter"))
        assertTrue(games.contains("onClick = onWorkshop"))
    }

    @Test
    fun wordWorkshopReplacesLetterPathWithTapOnlyLettersAndACompanionMascot() {
        assertFalse(projectFileOrNull("app/src/main/java/com/sonharf/game/LetterLadderGame.kt")?.exists() == true)
        assertFalse(projectFileOrNull("app/src/main/java/com/sonharf/game/HarfYoluKeyboard.kt")?.exists() == true)
        val screen = projectFile("app/src/main/java/com/sonharf/game/KelimeAtolyesiScreen.kt").readText()
        // Letters are tapped from the pool: no system keyboard and no drawn keyboard.
        assertFalse(screen.contains("TextField"))
        assertFalse(screen.contains("Keyboard"))
        assertTrue(screen.contains("state = current.pick(tileId)"))
        assertTrue(screen.contains("state = current.unpickAt(index)"))
        assertTrue(screen.contains("sh(\"Temizle\", \"Clear\")"))
        assertTrue(screen.contains("sh(\"Gönder\", \"Submit\")"))
        // The existing mascot, as a visual companion only.
        assertTrue(screen.contains("WordSiegeMascot("))
        assertFalse(screen.contains("speaking = true"))
        // No level system in this game.
        assertFalse(screen.contains("level", ignoreCase = true))
    }

    @Test
    fun tactilePressFeedbackCannotCollapseBackToInvisibleOnePercentMotion() {
        val motion = projectFile("app/src/main/java/com/sonharf/game/SonHarfMicroMotion.kt").readText()
        assertTrue(motion.contains("pressedScale.coerceAtMost(0.955f)"))
        assertTrue(motion.contains("waitForUpOrCancellation()"))
        assertFalse(motion.contains("infiniteRepeatable"))
    }

    @Test
    fun purchasedSuccessVfxHasReadableRingAndStillDoesNotCaptureInput() {
        val vfx = projectFile("app/src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt").readText()
        assertTrue(vfx.contains("PurchasedWordSuccessGreen"))
        assertTrue(vfx.contains("30f + 58f * p"))
        assertTrue(vfx.contains("drawCircle("))
        assertTrue(vfx.contains("R.drawable.vfx_twinkle"))
        assertFalse(vfx.contains("pointerInput"))
        assertFalse(vfx.contains("clickable"))
        assertFalse(vfx.contains("infiniteRepeatable"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")

    private fun projectFileOrNull(path: String): File? =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
}
