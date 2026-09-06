package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreVipProductionContractTest {

    @Test
    fun billingCatalogKeepsSingleCurrencyAndAllRequiredCoinPacks() {
        val source = projectFile("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt").readText()
        listOf("coins_500", "coins_1500", "coins_3500", "coins_8000").forEach {
            assertTrue("missing $it", source.contains("\"$it\""))
        }
        assertFalse(source.contains("gems_"))
        assertFalse(source.contains("premium_currency"))
    }

    @Test
    fun restoreAlwaysReusesServerVerificationPath() {
        val billing = projectFile("app/src/main/java/com/sonharf/game/billing/BillingManager.kt").readText()
        val vip = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()
        assertTrue(billing.contains("fun restorePurchases"))
        assertTrue(billing.contains("onPurchase(purchase)"))
        assertTrue(vip.contains("PlayPurchaseVerification.verify(productId, token)"))
        assertTrue(vip.contains("SATIN ALMALARI GERİ YÜKLE"))
    }

    @Test
    fun vipOfferUsesRequiredMessageAndSixPrimaryBenefits() {
        val source = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()
        assertTrue(source.contains("Daha kolay. Daha detaylı. Daha kişisel."))
        listOf(
            "Arkadaş Listesi",
            "Otomatik Puan Hesabı",
            "Maç Kelimeleri",
            "Gelişmiş İstatistik",
            "Reklamsızlık",
            "VIP Style",
            "TÜM VIP ÖZELLİKLERİNİ GÖR",
            "EN AVANTAJLI",
        ).forEach { assertTrue("missing VIP copy: $it", source.contains(it)) }
    }

    @Test
    fun rankedVipCannotBuyPower() {
        val migration = projectFile("supabase/migrations/20260904090000_store_vip_production_hardening.sql").readText()
        assertTrue(migration.contains("'freezer_count',0"))
        assertTrue(migration.contains("'swap_count',0"))
        assertTrue(migration.contains("'hint_count',0"))
        assertTrue(migration.contains("'streak_shield_count',0"))
        assertTrue(migration.contains("'xp_multiplier',1"))
        assertTrue(migration.contains("'diamond_multiplier',1"))
        assertTrue(migration.contains("'rewarded_ad_bypass',false"))
        assertTrue(migration.contains("'ranked_live_assist',false"))
    }

    @Test
    fun coinLedgerAndPurchaseTokensAreServerProtected() {
        val base = projectFile("supabase/migrations/20260904090000_store_vip_production_hardening.sql").readText()
        val reconcile = projectFile("supabase/migrations/20260904090500_play_entitlement_reconciliation.sql").readText()
        assertTrue(base.contains("son_coin_ledger_is_immutable"))
        assertTrue(base.contains("revoke all on public.purchases from anon, authenticated"))
        assertTrue(reconcile.contains("on conflict(purchase_token) do nothing"))
        assertTrue(reconcile.contains("v_inserted := found"))
        assertTrue(reconcile.contains("if v_inserted then"))
        assertTrue(reconcile.contains("purchase_token_user_mismatch"))
        assertTrue(reconcile.contains("purchase_token_product_mismatch"))
        assertTrue(reconcile.contains("v_purchase_user_id<>p_user_id"))
        assertTrue(reconcile.contains("v_purchase_product_id<>p_product_id"))
        assertTrue(reconcile.contains("grant execute on function public.apply_verified_play_purchase_v2"))
    }

    @Test
    fun piggyIsDeterministicAndLegacyRandomChestIsDisabled() {
        val source = projectFile("supabase/migrations/20260904090000_store_vip_production_hardening.sql").readText()
        assertTrue(source.contains("bonus_sc in (0,200,400,600,800)"))
        assertTrue(source.contains("replaced_by_deterministic_piggy"))
        val openPiggy = source.substringAfter("create or replace function public.open_piggy_bank_v1")
            .substringBefore("-- Disable the legacy random chest economy")
        assertFalse(openPiggy.contains("random()"))
    }

    @Test
    fun analyticsWhitelistContainsRequiredEventsAndStripsSensitiveFields() {
        val source = projectFile("supabase/migrations/20260904090000_store_vip_production_hardening.sql").readText()
        listOf(
            "store_view", "product_view", "preview_start", "checkout_start",
            "purchase_success", "purchase_cancel", "purchase_failure", "purchase_pending",
            "restore", "equip", "vip_start", "vip_renew", "vip_cancel", "vip_expire",
            "season_upgrade", "rewarded_ad_complete", "piggy_open",
        ).forEach { assertTrue("missing analytics event: $it", source.contains("'$it'")) }
        listOf("purchaseToken", "purchase_token", "orderId", "order_id", "email", "phone", "message").forEach {
            assertTrue("sensitive metadata key is not stripped: $it", source.contains("'$it'"))
        }
    }

    @Test
    fun rtdnAlwaysRefetchesPlayBeforeReconciliation() {
        val source = projectFile("supabase/functions/google-play-rtdn/index.ts").readText()
        assertTrue(source.contains("purchases/subscriptionsv2/tokens"))
        assertTrue(source.contains("reconcile_play_entitlement_v1"))
        assertTrue(source.contains("GOOGLE_PLAY_RTDN_SECRET"))
        assertFalse(source.contains("purchaseToken }"))
    }

    @Test
    fun billingUsesCurrentLibraryPinnedByProject() {
        val gradle = projectFile("app/build.gradle.kts").readText()
        assertTrue(gradle.contains("billing-ktx:9.1.0"))
        assertTrue(gradle.contains("versionCode = 19"))
        assertTrue(gradle.contains("versionName = \"0.10.0\""))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
