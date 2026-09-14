package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeBrandingAndVisibleActionContractTest {
    @Test
    fun homeMakesKelimeTahtiPrimaryAndKeepsSecondaryModesBranded() {
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val games = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val brand = projectFile("app/src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeRaster = projectFile("app/src/main/res/drawable/kelime_kusatma_logo_hd.png")

        assertTrue(home.contains("Button(onClick = onSiege"))
        assertTrue(home.contains("onClick = onPlay"))
        assertTrue(brand.contains("R.drawable.kelime_tahti_logo_latest"))
        assertTrue(siegeRaster.isFile)
        assertTrue(isPng(siegeRaster))
        assertFalse(projectFileOrNull("app/src/main/res/drawable/kelime_kusatma_logo_hd.webp")?.exists() == true)
        assertFalse(projectFileOrNull("app/src/main/res/drawable/kelime_kusatma_logo_hd.xml")?.exists() == true)
        assertTrue(games.contains("title = sh(\"KELİME KUŞATMASI\", \"WORD SIEGE\")"))
        assertTrue(games.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(games.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))
        assertTrue(games.contains("onClick = onSiege"))
        assertTrue(games.contains("onClick = onLastLetter"))
        assertTrue(games.contains("onClick = onLetterPath"))
    }

    @Test
    fun letterPathUsesItsLogoAndTriggersVisibleSuccessVfxOnlyOnAcceptedMoves() {
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        assertTrue(ladder.contains("R.drawable.harf_yolu_logo"))
        assertTrue(ladder.contains("successVfxNonce += 1"))
        assertTrue(ladder.contains("PurchasedVictoryVfx("))
        assertTrue(ladder.contains("eventKey = \"letter:${'$'}{puzzle?.id}:${'$'}successVfxNonce\""))
        assertFalse(ladder.contains("rememberInfiniteTransition"))
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

    private fun isPng(file: File): Boolean {
        val bytes = file.readBytes()
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        return bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(signature)
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")

    private fun projectFileOrNull(path: String): File? =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
}
