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
        listOf("OCEAN", "BOTANIC", "LILAC", "ROSE", "GOLDEN_AVATAR").forEach { assertTrue(src.contains(it)) }
        assertTrue(src.contains("only change appearance") || src.contains("yalnızca görünümü"))
    }

    @Test fun retiredFramesStayInProvenanceCodeButAreNotRenderedOrSold() {
        val preview = read("src/main/java/com/sonharf/game/StoreProductPreview.kt")
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        val avatar = read("src/main/java/com/sonharf/game/FramedProfileAvatar.kt")
        assertTrue(frames.contains("PROFILE_FRAMES_RETIRED = true"))
        assertTrue(frames.contains("if (PROFILE_FRAMES_RETIRED) return"))
        assertTrue(preview.contains("PurchasedProfileFrameOverlay("))
        assertTrue(avatar.contains("val legacyFrameId = frameId"))
        assertFalse(avatar.contains("PurchasedProfileFrameOverlay("))
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
        assertTrue(src.contains("PURCHASED_DUEL_WORD_VFX_MS = 720"))
        assertTrue(src.contains("PURCHASED_DUEL_WORD_MAX_ALPHA = .92f"))
        assertTrue(src.contains("PURCHASED_DUEL_WORD_STAR_COUNT = 6"))
        assertTrue(src.contains("Cosmetic-only"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_VFX_MS = 650"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_VFX_MS = 800"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_MAX_ALPHA = .94f"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_MAX_ALPHA = .98f"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_STAR_COUNT = 5"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_STAR_COUNT = 6"))
        assertTrue(src.contains("PURCHASED_BOARD_PLACE_MIN_STAR_DP = 14f"))
        assertTrue(src.contains("PURCHASED_BOARD_RESOLVE_MIN_STAR_DP = 16f"))
        assertTrue(src.contains("PurchasedBoardActionVfxOverlay"))
        assertTrue(src.contains("wordSiegeCellCenterInViewport"))
        assertTrue(src.contains("clipToBounds()"))
        assertTrue(src.contains("R.drawable.vfx_twinkle"))
        assertFalse(src.contains("infiniteRepeatable"))
        assertFalse(src.contains("pointerInput"))
        assertFalse(src.contains("combinedClickable"))
        assertFalse(src.contains("clickable"))
    }

    @Test fun purchasedBoardVfxIsWiredOnlineAndExplicitlyDisabledForPracticeSiege() {
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")

        assertTrue(online.contains("PurchasedBoardActionVfxOverlay("))
        assertTrue(online.contains("PurchasedBoardVfxKind.PLACEMENT"))
        assertTrue(online.contains("PurchasedBoardVfxKind.RESOLVED"))
        assertFalse(online.contains("PurchasedBoardActionVfx("))

        assertTrue(practice.contains("PurchasedBoardActionVfxOverlay("))
        assertTrue(practice.contains("emptyList<PurchasedBoardVfxEvent>()"))
        assertFalse(practice.contains("PurchasedBoardVfxKind.PLACEMENT"))
        assertFalse(practice.contains("PurchasedBoardVfxKind.RESOLVED"))
        assertFalse(practice.contains("PurchasedBoardActionVfx("))

        assertTrue(online.contains("wordSiegeBoardBorderWidthDp(transform.scale)"))
        assertFalse(practice.contains("wordSiegeBoardBorderWidthDp(transform.scale)"))
    }

    @Test fun onlineAndPracticeSiegeUseTheTerritoryFirstCalmPalette() {
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")

        listOf(
            "0xFFC9A560", "0xFF3E9F4D", "0xFFD0514A", "0xFF52B360", "0xFFE57A73",
            "0xFFCFE6F5", "0xFFF6D3E2", "0xFFD6ECCB", "0xFFF8DCC3", "0xFFE2D6F2", "0xFFFBEBB5",
        ).forEach { assertTrue(online.contains(it)) }
        assertTrue(online.contains("border.copy(alpha = .92f)"))
        assertTrue(online.contains("val regionGap = 1.25.dp"))

        assertTrue(practice.contains("PracticeSiegeBoardSurface = Color(0xFFD5CEBD)"))
        assertTrue(practice.contains("PracticeSiegeNeutral = Color(0xFFF3EEDF)"))
        assertTrue(practice.contains("PracticeSiegeEmpty = Color(0xFFF3EEDF)"))
        assertTrue(practice.contains("PracticeSiegeMine = Color(0xFF3E9F4D)"))
        assertTrue(practice.contains("PracticeSiegeRival = Color(0xFFD0514A)"))
        assertTrue(practice.contains("PracticeSiegeThreat = Color(0xFFD8903D)"))
        assertTrue(practice.contains("val regionGap = 1.25.dp"))
        assertTrue(practice.contains(".padding(regionGap)"))
        val cellStart = practice.indexOf("private fun WordSiegePracticeBoardCell")
        val rackStart = practice.indexOf("internal fun WordSiegePracticeRackTile")
        assertTrue(cellStart >= 0 && rackStart > cellStart)
    }

    @Test fun purchasedVfxTextureMatchesRegisteredPackageAsset() {
        val bytes = File("src/main/res/drawable-nodpi/vfx_twinkle.png").readBytes()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("4ed0e0f0c12df51c56f2145720031a55ca9db59a20d851d6fe47c1d632397b28", sha256)
    }
}
