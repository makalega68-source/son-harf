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

        assertTrue(home.contains("\"KELİME KUŞATMASI\"") || home.contains("\"KELİME\\nKUŞATMASI\""))
        assertTrue(home.contains("onClick = onSiege"))
        assertTrue(home.contains("sh(\"HEMEN OYNA\", \"PLAY NOW\")"))
        assertTrue(home.contains("onClick = onPlay"))
        assertTrue(brand.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(siegeDrawable.isFile)
        assertTrue(siegeDrawable.readText().contains("<vector"))
        assertFalse(projectFileOrNull("app/src/main/res/drawable/kelime_kusatma_logo_hd.png")?.exists() == true)
        assertFalse(projectFileOrNull("app/src/main/res/drawable/kelime_kusatma_logo_hd.webp")?.exists() == true)
        assertTrue(games.contains("title = sh(\"KELİME KUŞATMASI\", \"KELİME KUŞATMASI\")"))
        assertTrue(games.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(games.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))
        assertTrue(games.contains("PremiumOtherGames(onLastLetter = onLastLetter, onLetterPath = onLetterPath)"))
        assertTrue(games.contains("onClick = onSiege"))
        assertTrue(games.contains("onClick = onLastLetter"))
        assertTrue(games.contains("onClick = onLetterPath"))
    }

    @Test
    fun letterPathUsesItsLogoAndTriggersVisibleSuccessVfxOnlyOnAcceptedMoves() {
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        assertTrue(ladder.contains("R.drawable.harf_yolu_logo"))
        assertTrue(projectFile("app/src/main/res/drawable-nodpi/harf_yolu_logo.webp").isFile)
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
    fun purchasedSuccessVfxHasReadableRestrainedRingAndStillDoesNotCaptureInput() {
        val vfx = projectFile("app/src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt").readText()
        assertTrue(vfx.contains("PurchasedWordSuccessGreen"))
        assertTrue(vfx.contains("25f + 47f * p"))
        assertTrue(vfx.contains("drawCircle("))
        assertTrue(vfx.contains("R.drawable.vfx_twinkle"))
        assertTrue(vfx.contains("PURCHASED_DUEL_WORD_VFX_MS = 620"))
        assertTrue(vfx.contains("PURCHASED_DUEL_WORD_MAX_ALPHA = .78f"))
        assertFalse(vfx.contains("pointerInput"))
        assertFalse(vfx.contains("clickable"))
        assertFalse(vfx.contains("infiniteRepeatable"))
        assertFalse(vfx.contains("UnityPlayer"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")

    private fun projectFileOrNull(path: String): File? =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
}
