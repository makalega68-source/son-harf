package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MageCatAssetContractTest {
    @Test
    fun atlasDecodesWithNineTransparentFrames() {
        val file = File("src/main/res/drawable-nodpi/mage_cat_expressions.png")
        val image = requireNotNull(javax.imageio.ImageIO.read(file))
        assertTrue(image.colorModel.hasAlpha())
        assertTrue(image.width % 3 == 0 && image.height % 3 == 0)
        val cell = image.width / 3
        for (i in 0..8) {
            var opaque = 0
            for (y in 0 until cell) for (x in 0 until cell) {
                if ((image.getRGB(i % 3 * cell + x, i / 3 * cell + y) ushr 24) > 100) opaque++
            }
            assertTrue("Empty or solid frame $i", opaque > cell * cell / 5 && opaque < cell * cell * 9 / 10)
        }
    }

    @Test
    fun mascotUsesRealDrawableInsteadOfCanvasFallback() {
        val companion = File("src/main/java/com/sonharf/game/mascot/MageCatCompanion.kt").readText()
        val overlay = File("src/main/java/com/sonharf/game/mascot/ReactiveMageCatOverlay.kt").readText()
        assertTrue(companion.contains("R.drawable.mage_cat_expressions"))
        assertTrue(companion.contains("BitmapFactory.decodeStream"))
        assertFalse(companion.contains("Canvas("))
        assertFalse(companion.contains("drawCircle"))
        assertFalse(overlay.contains("OnlineGameBackend"))
        assertFalse(overlay.contains("findPremierActiveRoom"))
        assertFalse(overlay.contains("submitWord("))
        assertFalse(overlay.contains("claimTurnTimeout("))
        assertFalse(overlay.contains("botTakeTurn("))
    }
}
