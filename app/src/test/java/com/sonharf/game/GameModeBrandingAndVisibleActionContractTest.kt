package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeBrandingAndVisibleActionContractTest {
    @Test
    fun homeMakesKelimeTahtiPrimaryAndKeepsSecondaryModesBranded() {
        val home = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()
        val brand = projectFile("app/src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val localization = projectFile("app/src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()
        val siegeAlias = projectFile("app/src/main/res/drawable/kelime_kusatma_logo_hd.xml").readText()

        assertTrue(home.contains("SonHarfOfficialLogo("))
        assertTrue(home.contains("PremiumPlayButton(onClick = onSiege)"))
        assertTrue(brand.contains("R.drawable.kelime_tahti_app_icon"))
        assertTrue(brand.contains("KELİME\\nTAHTI"))
        assertTrue(siegeAlias.contains("@drawable/kelime_tahti_logo"))
        assertTrue(localization.contains("KELİME TAHTI"))
        assertTrue(localization.contains("WORD THRONE"))
        assertTrue(home.contains("logoRes = R.drawable.son_harf_app_icon_master"))
        assertTrue(home.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(home.contains("logoRes = R.drawable.harf_yolu_logo"))
        assertTrue(home.contains("painterResource(logoRes)"))
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

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
