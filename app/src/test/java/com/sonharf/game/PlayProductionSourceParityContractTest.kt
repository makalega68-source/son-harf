package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayProductionSourceParityContractTest {
    @Test
    fun `google play production authority stays reproducible from source`() {
        val migration = repoFile("supabase/migrations/20260904083134_play_entitlement_reconciliation.sql").readText()
        assertTrue(migration.contains("create or replace function public.apply_verified_play_purchase_v2"))
        assertTrue(migration.contains("create or replace function public.reconcile_play_entitlement_v1"))
        assertTrue(migration.contains("grant execute on function public.apply_verified_play_purchase_v2"))
        assertTrue(migration.contains("to service_role"))
        assertTrue(migration.contains("revoke all on function public.apply_verified_play_purchase_v2"))
        assertTrue(migration.contains("from public,anon,authenticated"))

        val verify = repoFile("supabase/functions/verify-play-purchase/index.ts").readText()
        assertTrue(verify.contains("Cache-Control\": \"no-store"))
        assertTrue(verify.contains(".from(\"store_catalog_config\")"))
        assertTrue(verify.contains("product_disabled"))
        assertTrue(verify.contains("admin.rpc(\"apply_verified_play_purchase_v2\""))
        assertTrue(verify.contains("p_play_state: playState"))
        assertTrue(verify.contains("p_acknowledgement_state: acknowledgementState"))
        assertFalse(verify.contains("admin.rpc(\"apply_verified_play_purchase_v1\""))

        val rtdn = repoFile("supabase/functions/google-play-rtdn/index.ts").readText()
        assertTrue(rtdn.contains("GOOGLE_PLAY_RTDN_SECRET"))
        assertTrue(rtdn.contains("X-Son-Harf-RTDN-Secret"))
        assertTrue(rtdn.contains("event?.packageName && event.packageName !== packageName"))
        assertTrue(rtdn.contains("purchases/subscriptionsv2/tokens"))
        assertTrue(rtdn.contains("admin.rpc(\"reconcile_play_entitlement_v1\""))

        val audit = repoFile("docs/GOOGLE_PLAY_PRODUCTION_SOURCE_PARITY.md").readText()
        assertTrue(audit.contains("verify-play-purchase`: ACTIVE"))
        assertTrue(audit.contains("google-play-rtdn`: ACTIVE"))
        assertTrue(audit.contains("OIDC"))
        assertTrue(audit.contains("one-time clawback"))
        assertTrue(audit.contains("staging"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
