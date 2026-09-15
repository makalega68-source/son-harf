package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreProfileFramesRegressionTest {
    private val storeFrameIds = listOf(
        "frame_asset_red",
        "frame_asset_green",
        "frame_asset_mint",
        "frame_asset_purple",
    )

    @Test
    fun fourPackagedFramesAreDiscoverableByTheStoreClient() {
        val economy = projectFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        storeFrameIds.forEach { id -> assertTrue(economy.contains("\"$id\"")) }
    }

    @Test
    fun storeCardsUseTheRealPackagedArtwork() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt").readText()

        assertTrue(ui.contains("RED -> R.drawable.style_frame_red"))
        assertTrue(ui.contains("GREEN -> R.drawable.style_frame_green"))
        assertTrue(ui.contains("MINT -> R.drawable.style_frame_mint"))
        assertTrue(ui.contains("PURPLE -> R.drawable.style_frame_purple"))
        assertTrue(ui.contains("PurchasedFrameCatalog.RED, \"Kırmızı Hat\""))
        assertTrue(ui.contains("PurchasedFrameCatalog.GREEN, \"Zümrüt Hat\""))
        assertTrue(ui.contains("PurchasedFrameCatalog.MINT, \"Buz Mint\""))
        assertTrue(ui.contains("PurchasedFrameCatalog.PURPLE, \"Mor Spektrum\""))
        assertTrue(ui.contains("\"MAĞAZA\", \"SHOP\", R.drawable.style_icon_coin"))
    }

    @Test
    fun sonCoinPurchaseUsesServerAuthorityBeforeEquip() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt").readText()

        assertTrue(ui.contains("val canBuyWithCoin = !owned && coinPriced && assetReady"))
        assertTrue(ui.contains("b.purchaseShopItem(spec.id)"))
        assertTrue(ui.contains("b.equipShopItem(spec.id)"))
        assertTrue(ui.contains("sh(\"SATIN AL\", \"BUY\")"))
    }

    @Test
    fun migrationActivatesOnlyTheChosenFourAndKeepsProGoldSeparate() {
        val migration = projectFile("supabase/migrations/20260915152500_store_four_profile_frames_v1.sql").readText()

        storeFrameIds.forEach { id -> assertTrue(migration.contains("'$id'")) }
        assertTrue(migration.contains("active = true"))
        assertTrue(migration.contains("when 'frame_asset_red' then 120"))
        assertTrue(migration.contains("when 'frame_asset_green' then 240"))
        assertTrue(migration.contains("when 'frame_asset_mint' then 220"))
        assertTrue(migration.contains("when 'frame_asset_purple' then 280"))
        assertFalse(migration.contains("frame_asset_gold' then"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
