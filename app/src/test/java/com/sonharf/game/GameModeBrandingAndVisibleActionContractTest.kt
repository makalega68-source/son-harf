package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeBrandingAndVisibleActionContractTest {
    @Test
    fun homeMakesKelimeKusatmasiPrimaryAndKeepsSecondaryModesBranded() {
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeImageLayout.kt").readText()
        val games = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()

        assertTrue(home.contains("internal fun PremiumHomeProfileStrip("))
        assertTrue(home.contains("internal fun PremiumModeArtworkButton("))
        assertTrue(home.contains("internal fun PremiumHomeWeeklyTop3("))
        assertTrue(projectFile("app/src/main/res/drawable-nodpi/mode_kelime_kusatmasi.webp").isFile)
        assertTrue(projectFile("app/src/main/res/drawable-nodpi/mode_son_harf.webp").isFile)
        assertTrue(projectFile("app/src/main/res/drawable-nodpi/mode_kelime_yolu.webp").isFile)
        assertTrue(projectFile("app/src/main/res/drawable-nodpi/app_background.webp").isFile)

        assertTrue(games.contains("drawable = R.drawable.mode_kelime_kusatmasi"))
        assertTrue(games.contains("drawable = R.drawable.mode_son_harf"))
        assertTrue(games.contains("drawable = R.drawable.mode_kelime_yolu"))
        assertTrue(games.contains("PremiumDailyObjective(onClick = onTasks)"))
        assertTrue(games.contains("PremiumHomeWeeklyTop3("))
        assertFalse(games.contains("HomeBrandHeader("))

        val profileIndex = games.indexOf("item(key = \"profile_header\")")
        val siegeIndex = games.indexOf("item(key = \"kelime_kusatmasi\")")
        val tasksIndex = games.indexOf("item(key = \"daily_tasks\")")
        val lastLetterIndex = games.indexOf("item(key = \"son_harf\")")
        val letterPathIndex = games.indexOf("item(key = \"kelime_yolu\")")
        val weeklyIndex = games.indexOf("item(key = \"weekly_top_3\")")
        assertTrue(profileIndex >= 0)
        assertTrue(profileIndex < siegeIndex)
        assertTrue(siegeIndex < tasksIndex)
        assertTrue(tasksIndex < lastLetterIndex)
        assertTrue(lastLetterIndex < letterPathIndex)
        assertTrue(letterPathIndex < weeklyIndex)

        assertTrue(games.contains("title = sh(\"KELİME KUŞATMASI\", \"KELİME KUŞATMASI\")"))
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
