package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryReleaseContractTest {
    @Test
    fun dictionaryReleaseIsImmutableHashedAndRollbackCapable() {
        val sql = projectFile("supabase/migrations/20260907093000_dictionary_release_snapshots_v1.sql").readText()
        assertTrue(sql.contains("create table if not exists public.dictionary_release_snapshots"))
        assertTrue(sql.contains("snapshot_sha256 text not null"))
        assertTrue(sql.contains("source_manifest jsonb not null"))
        assertTrue(sql.contains("words text[] not null"))
        assertTrue(sql.contains("extensions.digest"))
        assertTrue(sql.contains("array_to_string(v_words,E'\\n')"))
        assertTrue(sql.contains("previous_release_id"))
        assertTrue(sql.contains("admin_rollback_dictionary_release_v1"))
        assertTrue(sql.contains("get_dictionary_snapshot_v4"))
        assertTrue(sql.contains("return public.get_dictionary_snapshot_v3(v_language)"))
    }

    @Test
    fun publishingAndRollbackRequireAuthenticatedAdmin() {
        val sql = projectFile("supabase/migrations/20260907093000_dictionary_release_snapshots_v1.sql").readText()
        assertTrue(sql.contains("v_uid uuid:=auth.uid()"))
        assertTrue(sql.contains("v_uid is null or not public.is_admin(v_uid)"))
        assertTrue(sql.contains("grant execute on function public.admin_publish_dictionary_release_v1(text,text) to authenticated,service_role"))
        assertTrue(sql.contains("grant execute on function public.admin_rollback_dictionary_release_v1(text,uuid) to authenticated,service_role"))
        assertFalse(sql.contains("grant execute on function public.admin_publish_dictionary_release_v1(text,text) to anon"))
        assertFalse(sql.contains("grant execute on function public.admin_rollback_dictionary_release_v1(text,uuid) to anon"))
    }

    @Test
    fun clientRemainsOnV3UntilBackendReleasePointerExists() {
        val client = projectFile("app/src/main/java/com/sonharf/game/data/SharedDictionaryService.kt").readText()
        assertTrue(client.contains("get_dictionary_snapshot_v3"))
        assertFalse(client.contains("get_dictionary_snapshot_v4"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
