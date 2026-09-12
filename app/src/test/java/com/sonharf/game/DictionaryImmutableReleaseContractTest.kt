package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryImmutableReleaseContractTest {
    private val stagingPath = "supabase/staging-migrations/20260912_dictionary_immutable_releases_v1.sql"
    private val digestFixPath = "supabase/staging-migrations/20260912161000_dictionary_release_digest_schema_fix.sql"

    @Test
    fun `dictionary releases remain staging only and immutable`() {
        val sql = repoFile(stagingPath).readText()
        assertTrue(sql.contains("STAGING ONLY"))
        assertFalse(repoFileOrNull("supabase/migrations/20260912_dictionary_immutable_releases_v1.sql")?.exists() == true)
        assertTrue(sql.contains("create table if not exists public.dictionary_releases_v1"))
        assertTrue(sql.contains("checksum_sha256"))
        assertTrue(sql.contains("source_manifest"))
        assertTrue(sql.contains("cardinality(words)=word_count"))
        assertTrue(sql.contains("before update or delete on public.dictionary_releases_v1"))
        assertTrue(sql.contains("dictionary_release_immutable"))
    }

    @Test
    fun `release creation uses deterministic provenance and current eligibility rules`() {
        val sql = repoFile(stagingPath).readText().lowercase()
        val digestFix = repoFile(digestFixPath).readText().lowercase()

        assertTrue(sql.contains("select distinct d.normalized_word"))
        assertTrue(sql.contains("order by normalized_word collate \"c\""))
        assertTrue(sql.contains("octet_length(convert_to(normalized_word,'utf8'))::text || ':' || normalized_word"))
        assertTrue(digestFix.contains("extensions.digest("))
        assertTrue(sql.contains("char_length(d.normalized_word) between 2 and 15"))
        assertTrue(sql.contains("right(d.normalized_word,1)='ğ'"))
        assertTrue(sql.contains("coalesce(d.game_allowed,true)"))
        assertTrue(sql.contains("not coalesce(d.is_abbreviation,false)"))
        assertTrue(sql.contains("not coalesce(d.is_proper_noun,false)"))
        assertTrue(sql.contains("dictionary_source_provenance_missing"))
        assertTrue(sql.contains("'checksum_format','sha256(sorted_distinct_utf8_byte_length_prefixed_lines_v1)'"))
    }

    @Test
    fun `admin mutation and client read surfaces stay least privilege`() {
        val compact = repoFile(stagingPath).readText().replace(Regex("\\s+"), "").lowercase()

        assertTrue(compact.contains("auth.uid()"))
        assertTrue(compact.contains("notpublic.is_admin()"))
        assertTrue(compact.contains("revokeallonfunctionpublic.admin_create_dictionary_release_v1(text)frompublic,anon,service_role"))
        assertTrue(compact.contains("revokeallonfunctionpublic.admin_activate_dictionary_release_v1(uuid)frompublic,anon,service_role"))
        assertTrue(compact.contains("revokeallonfunctionpublic.admin_rollback_dictionary_release_v1(text,uuid)frompublic,anon,service_role"))
        assertTrue(compact.contains("grantexecuteonfunctionpublic.admin_rollback_dictionary_release_v1(text,uuid)toauthenticated"))

        assertTrue(compact.contains("revokeallonfunctionpublic.get_dictionary_release_meta_v1(text)frompublic,anon"))
        assertTrue(compact.contains("grantexecuteonfunctionpublic.get_dictionary_release_meta_v1(text)toauthenticated,service_role"))
        assertTrue(compact.contains("revokeallonfunctionpublic.get_dictionary_snapshot_v4(text)frompublic,anon"))
        assertTrue(compact.contains("grantexecuteonfunctionpublic.get_dictionary_snapshot_v4(text)toauthenticated,service_role"))
        assertTrue(compact.contains("revokeallonfunctionpublic.validate_game_word_v2(text,text)frompublic,anon"))
    }

    @Test
    fun `active release pins snapshot and authoritative validation with legacy fallback`() {
        val sql = repoFile(stagingPath).readText().lowercase()
        assertTrue(sql.contains("join public.dictionary_releases_v1 r on r.release_id=s.active_release_id"))
        assertTrue(sql.contains("if v_release_words is not null then"))
        assertTrue(sql.contains("v_norm=any(v_release_words)"))
        assertTrue(sql.contains("char_length(v_norm)>15"))
        assertTrue(sql.contains("exact legacy fallback until the first release is activated"))
        assertTrue(sql.contains("char_length(v_norm)>30"))
        assertTrue(sql.contains("from public.dictionary_words x"))
    }

    @Test
    fun `android startup compares release identity checksum and word count`() {
        val coordinator = repoFile("app/src/main/java/com/sonharf/game/data/DictionaryReleaseCacheCoordinator.kt").readText()
        val startup = repoFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        assertTrue(coordinator.contains("get_dictionary_release_meta_v1"))
        assertTrue(coordinator.contains("storedReleaseId != activeReleaseId"))
        assertTrue(coordinator.contains("localChecksum != activeChecksum"))
        assertTrue(coordinator.contains("dictionary_release_word_count_mismatch"))
        assertTrue(coordinator.contains("dictionary_release_checksum_mismatch"))
        assertTrue(startup.contains("DictionaryReleaseCacheCoordinator.refreshIfNeeded"))
        assertTrue(startup.contains("runCatching"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")

    private fun repoFileOrNull(path: String): File? = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
}
