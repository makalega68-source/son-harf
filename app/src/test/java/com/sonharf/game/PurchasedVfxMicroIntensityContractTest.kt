package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedVfxMicroIntensityContractTest {
    @Test
    fun duelWordFeedbackIsShortVisibleAndOneShot() {
        assertTrue(PURCHASED_DUEL_WORD_VFX_MS in 500..750)
        assertTrue(PURCHASED_DUEL_WORD_MAX_ALPHA in 0.65f..0.85f)
        assertTrue(PURCHASED_DUEL_WORD_STAR_COUNT in 3..6)
        assertTrue(PURCHASED_DUEL_WORD_CENTER_STAR_DP in 24f..30f)

        val source = projectFile("app/src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt").readText()
        assertTrue(source.contains("tween(PURCHASED_DUEL_WORD_VFX_MS)"))
        assertTrue(source.contains("repeat(PURCHASED_DUEL_WORD_STAR_COUNT)"))
        assertFalse(source.contains("rememberInfiniteTransition"))
        assertFalse(source.contains("infiniteRepeatable"))
        assertFalse(source.contains("UnityPlayer"))
        assertFalse(source.contains("com.unity3d"))
        assertFalse(source.contains("pointerInput"))
    }

    @Test
    fun boardFeedbackIsRestrainedAndCaptureResolvesUnderOneAndHalfSeconds() {
        assertTrue(PURCHASED_BOARD_PLACE_VFX_MS in 400..650)
        assertTrue(PURCHASED_BOARD_RESOLVE_VFX_MS in 600..900)
        assertTrue(PURCHASED_BOARD_RESOLVE_VFX_MS <= 1_500)
        assertTrue(PURCHASED_BOARD_PLACE_MAX_ALPHA in 0.60f..0.80f)
        assertTrue(PURCHASED_BOARD_RESOLVE_MAX_ALPHA in 0.70f..0.90f)
        assertTrue(PURCHASED_BOARD_PLACE_STAR_COUNT in 3..5)
        assertTrue(PURCHASED_BOARD_RESOLVE_STAR_COUNT in 4..6)
        assertTrue(PURCHASED_BOARD_PLACE_MIN_STAR_DP in 10f..13f)
        assertTrue(PURCHASED_BOARD_RESOLVE_MIN_STAR_DP in 12f..15f)
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
