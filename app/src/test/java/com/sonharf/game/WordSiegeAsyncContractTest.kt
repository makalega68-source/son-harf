package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeAsyncContractTest {

    @Test
    fun asyncExperienceContainsEveryLockedProductDecision() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()

        assertTrue(ui.contains("SÜRE YOK"))
        assertTrue(ui.contains("SIRA SENDE"))
        assertTrue(ui.contains("RAKİPTE"))
        assertTrue(ui.contains("UYUYAN OYUNLAR"))
        assertTrue(ui.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(ui.contains("SOHBET"))
        assertTrue(ui.contains("PES ET"))
        assertTrue(ui.contains("2H"))
        assertTrue(ui.contains("3H"))
        assertTrue(ui.contains("2K"))
        assertTrue(ui.contains("3K"))
        assertTrue(ui.contains("Kelime ${'$'}wordScore • Alan ${'$'}area"))
        assertTrue(ui.contains("repeat(9)"))
        assertTrue(backend.contains("submit_word_siege_move_v1"))
        assertTrue(backend.contains("exchange_word_siege_tiles_v1"))
        assertFalse(ui.contains("Countdown"))
        assertFalse(ui.contains("turnDeadline"))
    }

    @Test
    fun migrationKeepsRulesServerAuthoritativeAndTablesPrivateByDefault() {
        val sql = projectFile("supabase/migrations/20260830214430_word_siege_async_v1.sql").readText()

        assertTrue(sql.contains("jsonb_array_length(board) = 81"))
        assertTrue(sql.contains("v_active_count >= 10"))
        assertTrue(sql.contains("word_siege_first_word_must_cover_center"))
        assertTrue(sql.contains("private.word_siege_word_allowed_v1"))
        assertTrue(sql.contains("'bonus_used', true"))
        assertTrue(sql.contains("'owner', v_owner"))
        assertTrue(sql.contains("consecutive_passes >= 2"))
        assertTrue(sql.contains("player_one_word_score + r.player_one_area"))
        assertTrue(sql.contains("alter table public.word_siege_games enable row level security"))
        assertTrue(sql.contains("revoke all on public.word_siege_games from anon, authenticated"))
        assertTrue(sql.contains("grant execute on function public.submit_word_siege_move_v1"))
        assertFalse(sql.contains("turn_deadline"))
        assertFalse(sql.contains("service_role"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
