package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayRefundEntitlementContractTest {
    @Test
    fun oneTimePremiumRefundsRevokeOnlyTheRefundedReceiptGrants() {
        val migration = projectFile(
            "supabase/migrations/20260920121500_play_refund_entitlement_reconciliation_v1.sql",
        ).readText()

        listOf("series_game", "letter_table", "score_calculator", "pro_lifetime")
            .forEach { productId ->
                assertTrue("Missing premium refund reconciliation for $productId", migration.contains("'$productId'"))
            }

        assertTrue(migration.contains("if p_revoke then"))
        assertTrue(migration.contains("set status = 'revoked'"))
        assertTrue(migration.contains("source_id = trim(p_purchase_token)"))
        assertTrue(migration.contains("source_type = 'play'"))
        assertTrue(migration.contains("entitlement_key = v_purchase.product_id"))
    }

    @Test
    fun lifetimeProRefundAlsoRevokesBundleAndRecomputesProfileAccess() {
        val migration = projectFile(
            "supabase/migrations/20260920121500_play_refund_entitlement_reconciliation_v1.sql",
        ).readText()

        assertTrue(migration.contains("v_purchase.product_id = 'pro_lifetime'"))
        assertTrue(migration.contains("source_type = 'pro_bundle'"))
        assertTrue(migration.contains("entitlement_key in ('series_game', 'letter_table', 'score_calculator')"))
        assertTrue(migration.contains("status in ('active', 'grace', 'canceled')"))
        assertTrue(migration.contains("expires_at > now()"))
        assertTrue(migration.contains("public.has_permanent_entitlement_v1(v_purchase.user_id, 'pro_lifetime')"))
        assertTrue(migration.contains("set is_vip = ("))
    }

    @Test
    fun oneTimePurchaseNotificationDoesNotDowngradeVerifiedPurchaseToPending() {
        val migration = projectFile(
            "supabase/migrations/20260920121500_play_refund_entitlement_reconciliation_v1.sql",
        ).readText()

        assertTrue(migration.contains("when p_play_state = 'PURCHASED' then 'verified'"))
        assertFalse(migration.contains("when p_play_state = 'PURCHASED' then 'pending'"))
    }

    @Test
    fun googlePlayRtdnRoutesOneTimeCancellationIntoReconciliation() {
        val rtdn = projectFile("supabase/functions/google-play-rtdn/index.ts").readText()

        assertTrue(rtdn.contains("const oneTime = event?.oneTimeProductNotification"))
        assertTrue(rtdn.contains("const revoke = notificationType === 2"))
        assertTrue(rtdn.contains("p_play_state: revoke ? \"ONE_TIME_PRODUCT_CANCELED\" : \"PURCHASED\""))
        assertTrue(rtdn.contains("p_revoke: revoke"))
        assertTrue(rtdn.contains("reconcile_play_entitlement_v1"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing project file: $path")
}
