package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedVfxMicroIntensityContractTest {
    @Test
    fun duelWordFeedbackIsShortVisibleAndOneShot() {
        assertTrue(PURCHASED_DUEL_WORD_VFX_MS in 600..800)
        assertTrue(PURCHASED_DUEL_WORD_MAX_ALPHA in 0.88f..0.95f)
        assertEquals(6, PURCHASED_DUEL_WORD_STAR_COUNT)
        assertTrue(PURCHASED_DUEL_WORD_CENTER_STAR_DP in 32f..36f)

        val source = projectFile("app/src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt").readText()
        assertTrue(source.contains("tween(PURCHASED_DUEL_WORD_VFX_MS)"))
        assertTrue(source.contains("repeat(PURCHASED_DUEL_WORD_STAR_COUNT)"))
        assertFalse(source.contains("rememberInfiniteTransition"))
        assertFalse(source.contains("infiniteRepeatable"))
        assertFalse(source.contains("UnityPlayer"))
        assertFalse(source.contains("com.unity3d"))
    }

    @Test
    fun boardFeedbackIsClearlyVisibleButStillBounded() {
        assertTrue(PURCHASED_BOARD_PLACE_VFX_MS in 600..700)
        assertTrue(PURCHASED_BOARD_RESOLVE_VFX_MS in 750..850)
        assertTrue(PURCHASED_BOARD_PLACE_MAX_ALPHA in 0.90f..0.96f)
        assertTrue(PURCHASED_BOARD_RESOLVE_MAX_ALPHA in 0.95f..1.00f)
        assertEquals(5, PURCHASED_BOARD_PLACE_STAR_COUNT)
        assertEquals(6, PURCHASED_BOARD_RESOLVE_STAR_COUNT)
        assertTrue(PURCHASED_BOARD_PLACE_MIN_STAR_DP in 13f..15f)
        assertTrue(PURCHASED_BOARD_RESOLVE_MIN_STAR_DP in 15f..17f)
    }

    private fun projectFile(path: String): File {
        return listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
    }
}
