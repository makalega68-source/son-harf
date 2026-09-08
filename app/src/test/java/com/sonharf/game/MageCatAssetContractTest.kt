package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MageCatAssetContractTest {
    @Test
    fun mascotUsesRealDrawableInsteadOfCanvasFallback() {
        val companion = File("src/main/java/com/sonharf/game/mascot/MageCatCompanion.kt").readText()
        val overlay = File("src/main/java/com/sonharf/game/mascot/ReactiveMageCatOverlay.kt").readText()
        assertTrue(companion.contains("R.drawable.mage_cat_runtime"))
        assertTrue(companion.contains("painterResource"))
        assertFalse(companion.contains("Canvas("))
        assertFalse(companion.contains("drawCircle"))
        assertTrue(overlay.contains("findPremierActiveRoom"))
        assertFalse(overlay.contains("submitWord("))
        assertFalse(overlay.contains("claimTurnTimeout("))
        assertFalse(overlay.contains("botTakeTurn("))
    }
}
