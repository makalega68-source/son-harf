package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeBrandingAndVisibleActionContractTest {
    @Test
    fun homeMakesKelimeKusatmasiPrimaryAndRoutesEachModeToItsOwnEntry() {
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val entries = projectFile("app/src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()

        assertTrue(home.contains("Text(\"KELİME KUŞATMASI\""))
        assertTrue(home.contains("Button(onClick = onSiege"))
        assertTrue(home.contains("PremiumOtherGames(onLastLetter = onLastLetter, onLetterPath = onLetterPath)"))
        assertTrue(shell.contains("PremiumDestination.SIEGE_ENTRY"))
        assertTrue(shell.contains("PremiumDestination.LAST_LETTER_ENTRY"))
        assertTrue(shell.contains("PremiumDestination.LETTER_PATH_ENTRY"))
        assertTrue(entries.contains("PremiumSiegeEntryScreen"))
        assertTrue(entries.contains("PremiumLastLetterEntryScreen"))
        assertTrue(entries.contains("PremiumLetterPathEntryScreen"))
        assertFalse(shell.contains("PremiumGameCenter("))
        assertFalse(entries.contains("painterResource"))
        assertFalse(entries.contains("R.drawable"))
    }

    @Test
    fun letterPathKeepsGameplaySuccessVfxIndependentFromItsEntryScreen() {
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val entries = projectFile("app/src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()
        assertTrue(ladder.contains("successVfxNonce += 1"))
        assertTrue(ladder.contains("PurchasedVictoryVfx("))
        assertTrue(ladder.contains("eventKey = \"letter:${'$'}{puzzle?.id}:${'$'}successVfxNonce\""))
        assertFalse(ladder.contains("rememberInfiniteTransition"))
        assertTrue(entries.contains("PremiumLetterPathEntryScreen"))
        assertFalse(entries.contains("R.drawable.harf_yolu_logo"))
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
