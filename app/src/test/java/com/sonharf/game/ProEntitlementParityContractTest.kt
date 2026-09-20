package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProEntitlementParityContractTest {
    @Test fun subscriptionAndLifetimeProUnlockTheSameAdvertisedSiegeTools() {
        val migration = projectFile("supabase/migrations/20260920133500_unify_pro_entitlement_checks.sql").readText()

        assertTrue(migration.contains("create or replace function public.has_pro_access_v1"))
        assertTrue(migration.contains("p.is_vip"))
        assertTrue(migration.contains("has_permanent_entitlement_v1(p_user_id,'pro_lifetime')"))
        assertTrue(migration.contains("has_permanent_entitlement_v1(p_user_id,'series_game')"))
        assertTrue(migration.contains("public.has_pro_access_v1(p_user_id)"))
        assertTrue(migration.contains("has_permanent_entitlement_v1(u,'score_calculator')"))
        assertTrue(migration.contains("has_permanent_entitlement_v1(u,'letter_table')"))
        assertTrue(migration.contains("case when public.has_pro_access_v1(p_user_id) then 50 else 10 end"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
