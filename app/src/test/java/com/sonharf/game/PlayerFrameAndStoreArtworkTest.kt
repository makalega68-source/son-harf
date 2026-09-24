package com.sonharf.game

import com.sonharf.game.billing.ProductCatalog
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerFrameAndStoreArtworkTest {
    @Test
    fun normalPlayerGetsTheNeutralFrame() {
        assertEquals(PurchasedFrameCatalog.STARTER_NEUTRAL, PlayerFrameResolver.resolve(null, isPro = false))
        assertEquals(PurchasedFrameCatalog.STARTER_NEUTRAL, PlayerFrameResolver.resolve("", isPro = false))
    }

    @Test
    fun proPlayerGetsTheGoldenFrame() {
        assertEquals(PurchasedFrameCatalog.GOLDEN_AVATAR, PlayerFrameResolver.resolve(null, isPro = true))
    }

    @Test
    fun selectedCosmeticFrameWinsOverTheDefaults() {
        assertEquals(PurchasedFrameCatalog.OCEAN, PlayerFrameResolver.resolve(PurchasedFrameCatalog.OCEAN, isPro = false))
        assertEquals(PurchasedFrameCatalog.BOTANIC, PlayerFrameResolver.resolve(PurchasedFrameCatalog.BOTANIC, isPro = true))
    }

    @Test
    fun normalPlayerNeverWearsTheProGoldenFrame() {
        assertEquals(PurchasedFrameCatalog.STARTER_NEUTRAL, PlayerFrameResolver.resolve(PurchasedFrameCatalog.GOLDEN_AVATAR, isPro = false))
    }

    @Test
    fun unknownFrameIdsFallBackToTheDefaults() {
        assertEquals(PurchasedFrameCatalog.STARTER_NEUTRAL, PlayerFrameResolver.resolve("frame_does_not_exist", isPro = false))
        assertEquals(PurchasedFrameCatalog.GOLDEN_AVATAR, PlayerFrameResolver.resolve("frame_does_not_exist", isPro = true))
    }

    @Suppress("DEPRECATION")
    @Test
    fun everyStoreSkuMapsToItsOwnArtwork() {
        val expected = mapOf(
            ProductCatalog.COINS_500 to R.drawable.store_product_coins_500,
            ProductCatalog.COINS_1500 to R.drawable.store_product_coins_1500,
            ProductCatalog.COINS_3500 to R.drawable.store_product_coins_3500,
            ProductCatalog.COINS_8000 to R.drawable.store_product_coins_8000,
            ProductCatalog.VIP_MONTHLY to R.drawable.store_product_vip_monthly,
            ProductCatalog.VIP_YEARLY to R.drawable.store_product_vip_yearly,
            ProductCatalog.SEASON_PASS_MONTHLY to R.drawable.store_product_season_pass_monthly,
            ProductCatalog.STARTER_STYLE_PACK to R.drawable.store_product_starter_style_pack,
            ProductCatalog.THEME_NEON to R.drawable.store_product_theme_neon,
        )
        expected.forEach { (sku, drawable) -> assertEquals(sku, drawable, StoreProductArtwork.forProduct(sku)) }
        assertEquals(expected.size, expected.values.toSet().size)
        assertNull(StoreProductArtwork.forProduct("unknown_sku"))
    }

    @Test
    fun productIdsAreUnchanged() {
        assertEquals("coins_500", ProductCatalog.COINS_500)
        assertEquals("coins_1500", ProductCatalog.COINS_1500)
        assertEquals("coins_3500", ProductCatalog.COINS_3500)
        assertEquals("coins_8000", ProductCatalog.COINS_8000)
        assertEquals("vip_monthly", ProductCatalog.VIP_MONTHLY)
        assertEquals("vip_yearly", ProductCatalog.VIP_YEARLY)
        assertEquals("season_pass_monthly", ProductCatalog.SEASON_PASS_MONTHLY)
    }

    @Test
    fun storeCardsAskTheCentralArtworkMap() {
        val play = source("GooglePlayProductsCard.kt")
        listOf("COINS_500", "COINS_1500", "COINS_3500", "COINS_8000").forEach {
            assertTrue(play.contains("StoreProductArtwork.forProduct(ProductCatalog.$it"))
        }
        assertTrue(source("VipPurchaseDialog.kt").contains("StoreProductArtwork.forProduct(ProductCatalog.VIP_MONTHLY"))
        assertTrue(source("VipPurchaseDialog.kt").contains("StoreProductArtwork.forProduct(ProductCatalog.VIP_YEARLY"))
        assertTrue(source("SeasonPassPurchaseCard.kt").contains("StoreProductArtwork.forProduct(ProductCatalog.SEASON_PASS_MONTHLY"))
    }

    @Test
    fun siegeLeaderCrownOnlyForTheStrictLeader() {
        val board = source("WordSiegePanMatch.kt")
        assertTrue(board.contains("leading = myTargetScore > rivalTargetScore"))
        assertTrue(board.contains("leading = rivalTargetScore > myTargetScore"))
        assertFalse(board.contains(">= rivalTargetScore"))
    }

    @Test
    fun sharedAvatarDrawsTheResolvedFrame() {
        val avatar = source("ProfilePhotoRuntime.kt")
        assertTrue(avatar.contains("FramedPortrait(size, PlayerFrameResolver.resolve(frameId, isPro))"))
        assertTrue(avatar.contains("FramedPortrait(diameter, PlayerFrameResolver.resolve(frameId, isPro))"))
    }

    private fun source(name: String): String =
        listOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .first(File::exists)
            .readText()
}
