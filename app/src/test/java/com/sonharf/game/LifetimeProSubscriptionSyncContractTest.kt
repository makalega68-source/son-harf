package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifetimeProSubscriptionSyncContractTest {
    @Test fun expiredSubscriptionCannotRevokeLifetimeProProfileFlag() {
        val migration = projectFile("supabase/migrations/20260920023500_preserve_lifetime_pro_during_subscription_sync.sql").readText()
        assertTrue(migration.contains("has_permanent_entitlement_v1(p_user_id,'pro_lifetime')"))
        assertTrue(migration.contains("or public.has_permanent_entitlement_v1"))
        assertTrue(migration.contains("where coalesce(p.is_vip,false)=false"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
