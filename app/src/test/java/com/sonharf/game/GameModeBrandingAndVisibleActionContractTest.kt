package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeBrandingAndVisibleActionContractTest {
    @Test
    fun homeUsesRealGameModeLogosInsteadOfOnlyGenericIcons() {
        val home = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()
        assertTrue(home.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(home.contains("R.drawable.harf_yolu_logo"))
        assertTrue(home.contains("logoRes = R.drawable.kelime_kusatma_logo_hd"))
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
