package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCatalogTruthV2ContractTest {
    @Test
    fun shopNavigationHasNoDeadMascotTabOrStaleBrand() {
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        assertTrue(shop.contains("initialTab.coerceIn(0, 3)"))
        assertTrue(shop.contains("StoreProBanner("))
        assertTrue(shop.contains("onSection(3)"))
        assertTrue(shop.contains("if (section == 3)"))
        // Mascots are sold as real, runtime-backed Google Play characters, not as a dead catalog tab.
        assertTrue(shop.contains("MascotStoreSection()"))
        assertFalse(shop.contains("items.filter { it.kind == \"mascot\" }"))
    }

    @Test
    fun featuredStoreUsesCurrentlySellableRuntimeFamilies() {
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        assertTrue(shop.contains("items.firstOrNull { it.kind == \"game_theme\" }"))
        assertTrue(shop.contains("items.firstOrNull { it.kind == \"keyboard_theme\" }"))
        assertTrue(shop.contains("items.firstOrNull { it.kind == \"name_style\" }"))
        assertFalse(shop.contains("val featured = items.filter { it.kind == \"profile_frame\" }"))
    }

    @Test
    fun googlePlayRowsNeverPresentFallbackPriceAsARealOffer() {
        val card = projectFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()

        assertFalse(card.contains("fallbackPrice"))
        assertTrue(card.contains("product?.oneTimePurchaseOfferDetails == null"))
        assertTrue(card.contains("offer?.formattedPrice ?: sh(\"PLAY'DE YOK\", \"NOT ON PLAY\")"))
        assertTrue(card.contains("offer != null -> offer.formattedPrice"))
    }

    @Test
    fun proStoreBrandAndPromisesMatchActiveRuntime() {
        val dialog = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()
        val pro = projectFile("app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt").readText()
        val card = projectFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()
        val benefits = projectFile("app/src/main/java/com/sonharf/game/StorefrontCards.kt").readText()
        val frames = projectFile("app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt").readText()

        assertTrue(dialog.contains("KELİME KUŞATMASI PRO"))
        assertTrue(pro.contains("KELİME KUŞATMASI PRO"))
        assertFalse(dialog.contains("Kelime Tahtı PRO"))
        assertFalse(pro.contains("SON HARF PRO"))

        // Profile-frame sale/equip surface is explicitly retired, so PRO copy must not sell it.
        assertTrue(frames.contains("PROFILE_FRAMES_RETIRED = true"))
        assertFalse(card.contains("PRO çerçevesini"))
        assertFalse(card.contains("the PRO frame"))
        assertFalse(dialog.contains("PRO STYLE"))
        assertFalse(dialog.contains("Exclusive appearance"))
        assertFalse(pro.contains("PRO Style"))
        assertFalse(pro.contains("cosmetic, social"))
        assertTrue(benefits.contains("PRO rozeti ve profil ayrıcalıkları"))
    }

    @Test
    fun playVerificationRequiresExplicitEnabledCatalogRow() {
        val edge = projectFile("supabase/functions/verify-play-purchase/index.ts").readText()
        val migration = projectFile("supabase/migrations/20260920013000_store_catalog_truth_v2.sql").readText()

        assertTrue(edge.contains("catalogRows?.length !== 1"))
        assertTrue(edge.contains("product_disabled"))
        assertTrue(edge.contains("profile_frame_amethyst"))
        assertTrue(edge.contains("profile_frame_emerald"))

        listOf(
            "vip_monthly",
            "vip_yearly",
            "season_pass_monthly",
            "series_game",
            "letter_table",
            "score_calculator",
            "pro_lifetime",
            "coins_500",
            "coins_1500",
            "coins_3500",
            "coins_8000",
        ).forEach { id -> assertTrue("Missing explicit active catalog row for $id", migration.contains("('$id', true")) }

        listOf(
            "starter_style_pack",
            "theme_neon",
            "profile_frame_ocean",
            "profile_frame_pink_blossom",
            "profile_frame_blue_royal",
            "profile_frame_amethyst",
            "profile_frame_emerald",
        ).forEach { id -> assertTrue("Missing explicit retired catalog row for $id", migration.contains("('$id', false")) }

        assertTrue(migration.contains("store_catalog_enabled_product_guard_v2"))
        assertTrue(migration.contains("enabled = false"))
        assertTrue(migration.contains("update public.store_bundles b"))
        assertTrue(migration.contains("set active = false"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing project file: $path")
}
