package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRuntimeTruthV3ContractTest {
    @Test
    fun retiredProfileFramesCannotReenterStorefronts() {
        val styleStore = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val mainShop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val ownedPolicy = projectFile("app/src/main/java/com/sonharf/game/OwnedStylePolicy.kt").readText()

        assertTrue(styleStore.contains("\"profile_frame\" -> false"))
        assertTrue(ownedPolicy.contains("\"profile_frame\" -> false"))
        assertTrue(mainShop.contains("items = b.getShopItems().filter { it.isRuntimeReadyStyle() }"))
        assertFalse(mainShop.contains("it.isRuntimeReadyStyle() || it.kind == \"profile_frame\""))
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
        assertTrue(billing.contains("if (!hasPurchasableOffer(productDetails))"))
        assertTrue(billing.contains("subscriptionOfferDetails?.any { it.offerToken.isNotBlank() } == true"))
        assertTrue(billing.contains("No eligible Google Play offer"))
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

    @Test
    fun purchaseNeedsLiveRuntimeButOwnedSupportedArchiveItemsRemainEquipable() {
        val migration = projectFile("supabase/migrations/20260920150000_store_runtime_equip_parity.sql").readText()

        assertTrue(migration.contains("create or replace function public.is_runtime_supported_shop_item_v1"))
        assertTrue(migration.contains("when 'profile_frame' then false"))
        assertTrue(migration.contains("p_item_id in ('theme_black','theme_dark_arena')"))
        assertTrue(migration.contains("when 'victory_effect' then p_item_id='victory_crown'"))
        assertTrue(migration.contains("when 'emoji_pack' then p_item_id='emoji_vip'"))
        assertTrue(migration.contains("shop_items_active_runtime_supported_v1"))
        assertTrue(migration.contains("check (not active or public.is_runtime_supported_shop_item_v1(id,kind))"))
        assertTrue(migration.contains("mascot_id=null"))

        val purchaseStart = migration.indexOf("create or replace function public.purchase_shop_item")
        val equipStart = migration.indexOf("create or replace function public.equip_shop_item")
        val purchaseBody = migration.substring(purchaseStart, equipStart)
        val equipBody = migration.substring(equipStart)

        assertTrue(purchaseBody.contains("and active=true"))
        assertTrue(purchaseBody.contains("is_runtime_supported_shop_item_v1(v_item.id,v_item.kind)"))
        assertTrue(purchaseBody.contains("raise exception 'item_runtime_unavailable'"))

        assertTrue(equipBody.contains("select * into v_item from public.shop_items where id=p_item_id;"))
        assertFalse(equipBody.contains("where id=p_item_id and active=true"))
        assertTrue(equipBody.contains("is_runtime_supported_shop_item_v1(v_item.id,v_item.kind)"))
        assertTrue(equipBody.contains("if not v_owned then raise exception 'not_owned'; end if;"))
        assertFalse(equipBody.contains("profile_frame_id="))
        assertFalse(equipBody.contains("mascot_id="))
        assertFalse(equipBody.contains("frame_round_golden_avatar"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing project file: $path")
}
