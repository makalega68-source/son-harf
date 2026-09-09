package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySecurityDefinerGrantContractTest {
    @Test fun legacySecurityDefinersAreNotAnonymousButRemainSignedInCompatible() {
        val migration = projectFile(
            "supabase/migrations/20260909094500_revoke_legacy_anon_security_definer.sql",
        ).readText()

        assertTrue(migration.contains("revoke execute on function public.equip_default_game_theme() from public, anon"))
        assertTrue(migration.contains("grant execute on function public.equip_default_game_theme() to authenticated, service_role"))
        assertTrue(migration.contains("revoke execute on function public.get_dictionary_snapshot_v3(text) from public, anon"))
        assertTrue(migration.contains("grant execute on function public.get_dictionary_snapshot_v3(text) to authenticated, service_role"))
        assertTrue(migration.contains("set search_path = pg_catalog, public, pg_temp"))
    }

    @Test fun currentDictionaryClientUsesAuthenticatedInvokerV4NotLegacyV3() {
        val source = projectFile(
            "app/src/main/java/com/sonharf/game/data/SharedDictionaryService.kt",
        ).readText()

        assertTrue(source.contains("\"get_dictionary_snapshot_v4\""))
        assertFalse(source.contains("\"get_dictionary_snapshot_v3\""))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
