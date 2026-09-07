package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPurchaseClawbackContractTest {
    @Test
    fun playVerificationUsesV2ServerAuthority() {
        val edge = projectFile("supabase/functions/verify-play-purchase/index.ts").readText()
        assertTrue(edge.contains("apply_verified_play_purchase_v2"))
        assertFalse(edge.contains("apply_verified_play_purchase_v1\""))
        assertTrue(edge.contains("purchase_not_completed"))
        assertTrue(edge.contains("subscription_not_entitled"))
    }

    @Test
    fun clawbackIsServiceRoleOnlyIdempotentAndProvenanceBacked() {
        val sql = projectFile("supabase/migrations/20260907084500_play_purchase_clawback_v2.sql").readText()
        assertTrue(sql.contains("create table if not exists public.play_purchase_grants"))
        assertTrue(sql.contains("primary key (purchase_token, grant_type, grant_key)"))
        assertTrue(sql.contains("reversed_at is null"))
        assertTrue(sql.contains("google_play_reversal:"))
        assertTrue(sql.contains("diamonds=coalesce(diamonds,0)-v_grant.amount"))
        assertTrue(sql.contains("other.purchase_token<>trim(p_purchase_token)"))
        assertTrue(sql.contains("revoke all on function public.reconcile_play_entitlement_v2"))
        assertTrue(sql.contains("grant execute on function public.reconcile_play_entitlement_v2(text,text,timestamptz,boolean) to service_role"))
        assertFalse(sql.contains("grant execute on function public.reconcile_play_entitlement_v2(text,text,timestamptz,boolean) to anon"))
        assertFalse(sql.contains("greatest(0"))
    }

    @Test
    fun rtdnUsesVerifiedPushIdentityAndDoesNotAutoRevokePendingReview() {
        val edge = projectFile("supabase/functions/google-play-rtdn/index.ts").readText()
        val sql = projectFile("supabase/migrations/20260907090000_play_rtdn_event_dedupe.sql").readText()

        assertTrue(edge.contains("verifyIdToken"))
        assertTrue(edge.contains("GOOGLE_PLAY_RTDN_AUDIENCE"))
        assertTrue(edge.contains("GOOGLE_PLAY_RTDN_PUSH_SERVICE_ACCOUNT"))
        assertTrue(edge.contains("package_mismatch"))
        assertTrue(edge.contains("claim_play_rtdn_event_v1"))
        assertTrue(edge.contains("if (voided && purchaseToken)"))
        assertTrue(edge.contains("reconcile_play_entitlement_v2"))
        assertTrue(edge.contains("pendingRefund ? \"pending_refund_review\""))
        assertFalse(edge.contains("if (pendingRefund && purchaseToken)"))
        assertTrue(sql.contains("message_id text primary key"))
        assertTrue(sql.contains("grant execute on function public.claim_play_rtdn_event_v1(text,text,text,text,timestamptz) to service_role"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
