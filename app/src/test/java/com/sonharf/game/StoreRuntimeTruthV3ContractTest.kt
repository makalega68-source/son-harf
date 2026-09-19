package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRuntimeTruthV3ContractTest {
    @Test
    fun retiredProfileFramesCannotReenterStyleStore() {
        val styleStore = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()

        assertTrue(styleStore.contains("\"profile_frame\" -> false"))
        assertFalse(styleStore.contains("StoreTab(sh(\"ÇERÇEVELER\", \"FRAMES\")"))
        assertTrue(styleStore.contains("catalog.any { it.kind == \"game_theme\" || it.kind == \"keyboard_theme\" }"))
        assertTrue(styleStore.contains("catalog.any { it.kind == \"victory_effect\" || it.kind == \"emoji_pack\" }"))
        assertTrue(styleStore.contains("catalog.any { it.kind == \"name_style\" }"))
    }

    @Test
    fun subscriptionsRequireEligibleGooglePlayOffer() {
        val billing = projectFile("app/src/main/java/com/sonharf/game/billing/BillingManager.kt").readText()
        val vip = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()
        val season = projectFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()

        assertTrue(billing.contains("fun hasPurchasableOffer(productDetails: ProductDetails?)"))
        assertTrue(billing.contains("subscriptionOfferDetails?.any { it.offerToken.isNotBlank() } == true"))
        assertTrue(billing.contains("No eligible subscription offer token"))
        assertTrue(vip.contains("selectedPurchasable = BillingManager.hasPurchasableOffer(selectedProduct)"))
        assertTrue(vip.contains("enabled = !busy && selectedPurchasable"))
        assertTrue(season.contains("enabled = !busy && BillingManager.hasPurchasableOffer(product)"))
        assertTrue(vip.contains("PLAY'DE YOK"))
        assertTrue(season.contains("PLAY'DE YOK"))
    }

    @Test
    fun lifetimeProNoLongerGrantsOrEquipsRetiredGoldenFrame() {
        val migration = projectFile("supabase/migrations/20260920024500_store_runtime_truth_v3.sql").readText()

        assertTrue(migration.contains("shop_items_no_active_profile_frames_v3"))
        assertTrue(migration.contains("check (not (kind = 'profile_frame' and active = true))"))
        assertTrue(migration.contains("set profile_frame_id = null"))
        assertTrue(migration.contains("set default_profile_frame_id = null"))
        assertTrue(migration.contains("array['series_game','letter_table','score_calculator']"))
        assertTrue(migration.contains("google_play_pro_lifetime:"))
        assertFalse(migration.contains("values(p_user_id,'frame_round_golden_avatar')"))
        assertFalse(migration.contains("set profile_frame_id='frame_round_golden_avatar'"))
        assertFalse(migration.contains("delete from public.user_inventory"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing project file: $path")
}
