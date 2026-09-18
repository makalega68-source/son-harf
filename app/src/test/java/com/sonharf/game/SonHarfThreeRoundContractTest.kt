package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonHarfThreeRoundContractTest {
    @Test
    fun clientShowsPerPlayerQuotaAndNoEmptyInputPlaceholder() {
        val screen = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val inputStart = screen.indexOf("private fun PremierInputBar(")
        val inputEnd = screen.indexOf("private fun PremierKeyboard(", inputStart)
        val inputBar = screen.substring(inputStart, inputEnd)

        assertTrue(screen.contains("PREMIER_WORDS_PER_PLAYER_PER_ROUND = 10"))
        assertTrue(screen.contains("room.hostRoundWords"))
        assertTrue(screen.contains("room.guestRoundWords"))
        assertTrue(screen.contains("/10 · R"))
        assertTrue(screen.contains("/3 ·"))
        assertTrue(screen.contains("15→13→11 sn"))
        assertTrue(screen.contains("backend.activatePremierOpeningTurn(active.id)"))
        assertTrue(inputBar.contains("Text(\n                input,"))
        assertFalse(inputBar.contains("Kelimeyi yaz"))
        assertFalse(inputBar.contains("Type a word"))
        assertFalse(inputBar.contains("Kontrol ediliyor"))
        assertFalse(inputBar.contains("Checking"))
    }

    @Test
    fun dtoBackendAndMigrationShareTheSameAuthoritativeRules() {
        val dto = File("src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        val backend = File("src/main/java/com/sonharf/game/data/PremierDuelBackend.kt").readText()
        val migration = File("../supabase/migrations/20260918000500_sonharf_three_round_quota_clock_v1.sql").readText()

        assertTrue(dto.contains("@SerialName(\"host_round_words\") val hostRoundWords: Int = 0"))
        assertTrue(dto.contains("@SerialName(\"guest_round_words\") val guestRoundWords: Int = 0"))
        assertTrue(backend.contains("\"activate_premier_opening_turn_v1\""))
        assertTrue(migration.contains("when greatest(coalesce(p_round_no,1),1)=1 then 15"))
        assertTrue(migration.contains("when greatest(coalesce(p_round_no,1),1)=2 then 13"))
        assertTrue(migration.contains("else 11"))
        assertTrue(migration.contains("r.host_round_words>=10 and r.guest_round_words>=10"))
        assertTrue(migration.contains("host_round_words=0"))
        assertTrue(migration.contains("guest_round_words=0"))
        assertFalse(migration.contains("r.round_word_count>=10"))
        assertFalse(migration.contains("r.round_word_count>=15"))
        assertFalse(migration.contains("trivia_questions"))
    }

    @Test
    fun adaptiveNormalBotNeverCallsExpertOnlyTurnFunction() {
        val migration = File("../supabase/migrations/20260918000500_sonharf_three_round_quota_clock_v1.sql").readText()
        val dispatcherStart = migration.indexOf("create or replace function public.bot_take_turn(p_room_id uuid)")
        val dispatcherEnd = migration.indexOf("create or replace function public.switch_turn_after_failure", dispatcherStart)
        val dispatcher = migration.substring(dispatcherStart, dispatcherEnd)

        assertTrue(dispatcher.contains("if r.game_mode='expert' then"))
        assertTrue(dispatcher.contains("return public.bot_take_turn_expert_v1(p_room_id);"))
        assertTrue(dispatcher.contains("return public.bot_take_turn_normal_v1(p_room_id);"))
        assertFalse(dispatcher.contains("sonharf_adaptive_bot_tier_v1"))
    }
}
