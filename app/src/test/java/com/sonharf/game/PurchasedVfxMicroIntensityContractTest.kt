package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedVfxMicroIntensityContractTest {
    @Test
    fun duelWordFeedbackIsShortSparseAndOneShot() {
        assertTrue(PURCHASED_DUEL_WORD_VFX_MS in 600..800)
        assertTrue(PURCHASED_DUEL_WORD_MAX_ALPHA in 0.65f..0.80f)
        assertEquals(4, PURCHASED_DUEL_WORD_STAR_COUNT)
        assertTrue(PURCHASED_DUEL_WORD_CENTER_STAR_DP in 24f..32f)

        val source = projectFile("app/src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt").readText()
        assertTrue(source.contains("tween(PURCHASED_DUEL_WORD_VFX_MS)"))
        assertTrue(source.contains("repeat(PURCHASED_DUEL_WORD_STAR_COUNT)"))
        assertFalse(source.contains("rememberInfiniteTransition"))
        assertFalse(source.contains("infiniteRepeatable"))
        assertFalse(source.contains("UnityPlayer"))
        assertFalse(source.contains("com.unity3d"))
    }

    @Test
    fun boardFeedbackRemainsVisibleButBounded() {
        assertTrue(PURCHASED_BOARD_PLACE_VFX_MS in 600..700)
        assertTrue(PURCHASED_BOARD_RESOLVE_VFX_MS in 750..850)
        assertTrue(PURCHASED_BOARD_PLACE_MAX_ALPHA in 0.80f..0.85f)
        assertTrue(PURCHASED_BOARD_RESOLVE_MAX_ALPHA in 0.82f..0.88f)
        assertEquals(4, PURCHASED_BOARD_PLACE_STAR_COUNT)
        assertTrue(PURCHASED_BOARD_RESOLVE_STAR_COUNT in 4..5)
    }

    private fun projectFile(path: String): File {
        return listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
    }
}
