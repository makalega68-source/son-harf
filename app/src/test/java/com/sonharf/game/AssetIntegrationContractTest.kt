package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class AssetIntegrationContractTest {
    private fun read(path: String) = File(path).readText()

    @Test fun frameCatalogContainsIntegratedPurchasedVariantsAndNoPowerFields() {
        val src = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        listOf("RED", "GREEN", "MINT", "PURPLE", "GOLD", "GOLD_CROWN").forEach { assertTrue(src.contains(it)) }
        assertTrue(src.contains("only change appearance") || src.contains("yalnızca görünümü"))
    }

    @Test fun styleStoreUsesOneRememberedBackendAndSharesItWithFrames() {
        val shop = read("src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt")
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(shop.contains("runCatching { OnlineGameBackend() }.getOrNull()"))
        assertTrue(shop.contains("PurchasedProfileFramesStoreRow(backend = backend)"))
        assertTrue(frames.contains("PurchasedProfileFramesStoreRow(backend: OnlineGameBackend?)"))
        assertFalse(frames.contains("OnlineGameBackend()"))
    }

    @Test fun styleStoreUsesDecodeStreamAndNeverPaintsBrokenImageOverAvatar() {
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(frames.contains("openRawResource(drawable)"))
        assertTrue(frames.contains("BitmapFactory.decodeStream(stream)"))
        assertTrue(frames.contains("SafeFrameArtwork"))
        assertTrue(frames.contains("Orijinal görsel onarılıyor") || frames.contains("Original artwork is being repaired"))
        assertTrue(frames.contains("!assetReady && !owned ->"))
        assertFalse(frames.contains("Icons.Rounded.BrokenImage"))
        assertFalse(frames.contains("ImageBitmap.imageResource(resources"))
        assertFalse(frames.contains("painterResource(drawable"))
    }

    @Test fun styleCardsKeepMobileActionsInsideViewportAndActiveItemHasNoDisabledActionButton() {
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(frames.contains("Modifier.width(164.dp)"))
        assertTrue(frames.contains("equipped -> Icon(Icons.Rounded.CheckCircle"))
        assertTrue(frames.contains("!assetReady && !owned -> Text"))
    }

    @Test fun purchasedVfxIsVisibleOneShotAndInputTransparent() {
        val src = read("src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt")
        assertTrue(src.contains("1050"))
        assertTrue(src.contains("Cosmetic-only"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_VFX_MS = 650"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_VFX_MS = 800"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_MAX_ALPHA = .86f"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_MAX_ALPHA = .88f"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_STAR_COUNT = 4"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_STAR_COUNT = 5"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_MIN_STAR_DP = 12f"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_MIN_STAR_DP = 13f"))
        assertTrue(src.contains("PurchasedBoardActionVfxOverlay"))
        assertTrue(src.contains("wordSiegeCellCenterInViewport"))
        assertTrue(src.contains("clipToBounds()"))
        assertTrue(src.contains("R.drawable.vfx_twinkle"))
        assertFalse(src.contains("infiniteRepeatable"))
        assertFalse(src.contains("pointerInput"))
        assertFalse(src.contains("combinedClickable"))
        assertFalse(src.contains("clickable"))
    }

    @Test fun purchasedBoardVfxIsWiredAsViewportOverlayForOnlineAndPracticeSiege() {
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        listOf(online, practice).forEach { source ->
            assertTrue(source.contains("PurchasedBoardActionVfxOverlay("))
            assertTrue(source.contains("PurchasedBoardVfxKind.PLACEMENT"))
            assertTrue(source.contains("PurchasedBoardVfxKind.RESOLVED"))
            assertTrue(source.contains("wordSiegeBoardBorderWidthDp(transform.scale)"))
            assertFalse(source.contains("PurchasedBoardActionVfx("))
        }
    }

    @Test fun siegeCellBorderPaletteIsDistinctAndHighContrastInBothBoards() {
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        listOf(online, practice).forEach { source ->
            assertTrue(source.contains("0xFF7890A8"))
            assertTrue(source.contains("0xFF5279A6"))
            assertTrue(source.contains("0xFF147A48"))
            assertTrue(source.contains("0xFFB72E35"))
            assertTrue(source.contains("0xFFD99818"))
            assertTrue(source.contains("border.copy(alpha = .96f)"))
        }
    }

    @Test fun purchasedVfxTextureMatchesRegisteredPackageAsset() {
        val bytes = File("src/main/res/drawable-nodpi/vfx_twinkle.png").readBytes()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("4ed0e0f0c12df51c56f2145720031a55ca9db59a20d851d6fe47c1d632397b28", sha256)
    }
}
