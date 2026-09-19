package com.sonharf.game

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileFramesV2ContractTest {
    private val productIds = listOf(
        "profile_frame_pink_blossom",
        "profile_frame_blue_royal",
        "profile_frame_amethyst",
        "profile_frame_emerald",
    )

    @Test
    fun activeV2CatalogContainsOnlyTheFourNewPlayFrameIds() {
        val catalog = File("src/main/java/com/sonharf/game/billing/ProductCatalog.kt").readText()
        productIds.forEach { id -> assertTrue("Missing V2 product $id", catalog.contains("\"$id\"")) }
        assertTrue(catalog.contains("PROFILE_FRAME_FALLBACK_PRICE_TRY = \"150 TL\""))
        assertTrue(
            catalog.contains(
                """val profileFrameProducts = listOf(
        PROFILE_FRAME_PINK_BLOSSOM,
        PROFILE_FRAME_BLUE_ROYAL,
        PROFILE_FRAME_AMETHYST,
        PROFILE_FRAME_EMERALD,
    )"""
            )
        )
    }

    @Test
    fun v2FramesUseVerifiedGooglePlayFlowAndPermanentOwnership() {
        val frames = File("src/main/java/com/sonharf/game/ProfileFramesV2.kt").readText()
        listOf(
            "const val PINK_BLOSSOM = ProductCatalog.PROFILE_FRAME_PINK_BLOSSOM",
            "const val BLUE_ROYAL = ProductCatalog.PROFILE_FRAME_BLUE_ROYAL",
            "const val AMETHYST = ProductCatalog.PROFILE_FRAME_AMETHYST",
            "const val EMERALD = ProductCatalog.PROFILE_FRAME_EMERALD",
        ).forEach { wiring -> assertTrue("Missing V2 frame wiring $wiring", frames.contains(wiring)) }
        assertTrue(frames.contains("PlayPurchaseVerification.verify(productId, purchase.purchaseToken)"))
        assertTrue(frames.contains("billing.launchProduct(host, product)"))
        assertTrue(frames.contains("equippedId?.takeIf { it in paidIds && it in ownedIds }"))
    }

    @Test
    fun v2FramesAreExcludedFromGenericSonCoinPurchaseList() {
        val store = File("src/main/java/com/sonharf/game/PremiumStoreScreen.kt").readText()
        assertTrue(store.contains("ProfileFramesV2StoreRow("))
        assertTrue(store.contains(".filterNot { it.id in ProfileFrameV2Catalog.paidIds }"))
    }

    @Test
    fun allSixRuntimeArtworkFilesExist() {
        val names = listOf(
            "profile_frame_default_gray.png",
            "profile_frame_pro_gold.png",
            "profile_frame_shop_pink_blossom.png",
            "profile_frame_shop_blue_royal.png",
            "profile_frame_shop_amethyst.png",
            "profile_frame_shop_emerald.png",
        )
        names.forEach { name ->
            assertTrue("Missing runtime frame asset $name", File("src/main/res/drawable/$name").isFile)
        }
    }

    @Test
    fun visualPrecedenceKeepsPaidSelectionAboveProAndDefault() {
        val frames = File("src/main/java/com/sonharf/game/ProfileFramesV2.kt").readText()
        assertTrue(frames.contains("paidVisuals[equippedPaidFrameId] ?: if (isPro) proVisual else defaultVisual"))
    }
}
