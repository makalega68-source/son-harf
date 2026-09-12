package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayRtdnRefundStagingContractTest {
    @Test
    fun `hardening remains staging only until paid staging gate passes`() {
        val staging = repoFile("supabase/staging-migrations/20260912_play_rtdn_refund_hardening.sql")
        assertTrue(staging.exists())
        assertFalse(repoFileOrNull("supabase/migrations/20260912_play_rtdn_refund_hardening.sql")?.exists() == true)
        assertTrue(staging.readText().contains("STAGING ONLY"))
    }

    @Test
    fun `one time clawback is provenance based idempotent and non negative`() {
        val sql = repoFile("supabase/staging-migrations/20260912_play_rtdn_refund_hardening.sql").readText()
        assertTrue(sql.contains("create table if not exists public.play_purchase_grants"))
        assertTrue(sql.contains("reversal_shortfall"))
        assertTrue(sql.contains("reversed_at is null"))
        assertTrue(sql.contains("greatest(0,coalesce(diamonds,0)-v_grant.amount)"))
        assertTrue(sql.contains("v_recovered := least(v_balance,v_grant.amount)"))
        assertTrue(sql.contains("google_play_reversal:"))
        assertTrue(sql.contains("other.purchase_token" ).not())
        assertTrue(sql.contains("other.reversed_at is null"))
        assertTrue(sql.contains("other.owns_inventory"))
        assertTrue(sql.contains("grant execute on function public.reconcile_play_entitlement_v2"))
        assertTrue(sql.contains("to service_role"))
        assertTrue(sql.contains("from public,anon,authenticated"))
    }

    @Test
    fun `rtdn uses oidc identity and retry safe event ledger`() {
        val edge = repoFile("supabase/functions/google-play-rtdn/index.ts").readText()
        val sql = repoFile("supabase/staging-migrations/20260912_play_rtdn_refund_hardening.sql").readText()

        assertTrue(edge.contains("verifyIdToken"))
        assertTrue(edge.contains("audience: expectedAudience"))
        assertTrue(edge.contains("payload?.email_verified !== true"))
        assertTrue(edge.contains("payload.email !== expectedPushServiceAccount"))
        assertTrue(edge.contains("claim_play_rtdn_event_v2"))
        assertTrue(edge.contains("finish_play_rtdn_event_v2"))
        assertTrue(edge.contains("reconcile_play_entitlement_v2"))
        assertTrue(edge.contains("voided_purchase"))
        assertTrue(edge.contains("pending_refund_review"))
        assertTrue(edge.contains("p_revoke: false"))
        assertTrue(edge.contains("event_processing_busy"))
        assertFalse(edge.contains("GOOGLE_PLAY_RTDN_SECRET"))
        assertFalse(edge.contains("X-Son-Harf-RTDN-Secret"))
        assertFalse(edge.contains("const revoke = notificationType === 2"))

        assertTrue(sql.contains("message_id text primary key"))
        assertTrue(sql.contains("attempt_count"))
        assertTrue(sql.contains("interval '5 minutes'"))
        assertTrue(sql.contains("processed_at is not null and v_event.processing_error is null"))
    }

    @Test
    fun `user verification stays the only one time grant path`() {
        val verify = repoFile("supabase/functions/verify-play-purchase/index.ts").readText()
        val edge = repoFile("supabase/functions/google-play-rtdn/index.ts").readText()
        assertTrue(verify.contains("apply_verified_play_purchase_v2"))
        assertTrue(verify.contains("userClient.auth.getUser()"))
        assertFalse(edge.contains("apply_verified_play_purchase_v2"))
        assertTrue(edge.contains("RTDN does not bind the event to an app user"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")

    private fun repoFileOrNull(path: String): File? = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
}
