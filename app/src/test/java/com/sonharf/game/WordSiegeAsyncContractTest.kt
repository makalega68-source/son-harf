package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeAsyncContractTest {

    @Test
    fun asyncExperienceContainsDurationHubAndSeparatedExitForfeit() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()

        assertTrue(ui.contains("Tur süresini seç"))
        assertTrue(ui.contains("12 ${'$'}{sh(\"SAAT\"" ) || ui.contains("12 SAAT"))
        assertTrue(ui.contains("72 ${'$'}{sh(\"SAAT\"" ) || ui.contains("72 SAAT"))
        assertTrue(ui.contains("OYUNLARIM"))
        assertTrue(ui.contains("AKTİF"))
        assertTrue(ui.contains("BİTEN"))
        assertTrue(ui.contains("DAVETLER"))
        assertTrue(ui.contains("DEVAM ET"))
        assertTrue(ui.contains("SIRA SENDE"))
        assertTrue(ui.contains("RAKİP BEKLENİYOR"))
        assertTrue(ui.contains("Kalan:"))
        assertTrue(ui.contains("GERİ AL"))
        assertTrue(ui.contains("KARIŞTIR"))
        assertTrue(ui.contains("SOHBET"))
        assertTrue(ui.contains("Hamleler / Hamle Geçmişi"))
        assertTrue(ui.contains("Rakip Profili"))
        assertTrue(ui.contains("Nasıl Oynanır / Kurallar"))
        assertTrue(ui.contains("Ses ve Müzik"))
        assertTrue(ui.contains("Şikâyet Et"))
        assertTrue(ui.contains("Teslim Ol"))
        assertTrue(ui.contains("Oyundan Çık"))
        assertTrue(ui.contains("fun leaveMatchScreen()"))
        assertFalse(ui.substringAfter("fun leaveMatchScreen()").substringBefore("fun runGameAction").contains("forfeitWordSiegeGame"))
        assertTrue(ui.contains("backend.forfeitWordSiegeGame"))
        assertTrue(backend.contains("refresh_my_word_siege_games_v2"))
        assertTrue(backend.contains("refresh_word_siege_game_v2"))
        assertTrue(backend.contains("find_or_create_word_siege_game_v2"))
        assertTrue(backend.contains("p_turn_duration_hours"))
        assertTrue(backend.contains("turnDeadline"))
    }

    @Test
    fun deadlineMigrationIsServerAuthoritativeAtomicAndPoolSafe() {
        val base = projectFile("supabase/migrations/20260830214430_word_siege_async_v1.sql").readText()
        val validation = projectFile("supabase/migrations/20260831124000_word_siege_kelmelik_validation.sql").readText()
        val deadline = asyncDeadlineMigration().readText()

        assertTrue(base.contains("jsonb_array_length(board) = 81"))
        assertTrue(base.contains("alter table public.word_siege_games enable row level security"))
        assertTrue(validation.contains("word_siege_prevalidate_move_v2"))

        assertTrue(deadline.contains("turn_duration_hours"))
        assertTrue(deadline.contains("turn_started_at"))
        assertTrue(deadline.contains("turn_deadline"))
        assertTrue(deadline.contains("loser_id"))
        assertTrue(deadline.contains("turn_duration_hours in (12, 72)"))
        assertTrue(deadline.contains("clock_timestamp()"))
        assertTrue(deadline.contains("for update"))
        assertTrue(deadline.contains("word_siege_finalize_timeout_v2"))
        assertTrue(deadline.contains("finish_reason = 'timeout'"))
        assertTrue(deadline.contains("status = 'playing'"))
        assertTrue(deadline.contains("current_player_id = v_loser"))
        assertTrue(deadline.contains("clock_timestamp() >= turn_deadline"))
        assertTrue(deadline.contains("g.turn_duration_hours = v_duration"))
        assertTrue(deadline.contains("'word_siege:' || v_language || ':' || v_duration::text"))
        assertTrue(deadline.contains("word_siege_prevalidate_move_v2"))
        assertTrue(deadline.contains("word_siege_arm_next_turn_v2"))
        assertTrue(deadline.contains("sweep_word_siege_timeouts_v2"))
        assertTrue(deadline.contains("cron.schedule"))
        assertFalse(deadline.contains("service_role"))
    }

    private fun asyncDeadlineMigration(): File {
        val directory = projectFile("supabase/migrations")
        val file = directory.listFiles().orEmpty().singleOrNull { it.name.endsWith("_word_siege_async_deadlines_v2.sql") }
        assertNotNull("Async deadline migration missing", file)
        return requireNotNull(file)
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
