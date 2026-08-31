package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingBotCompetitiveGuardContractTest {
    @Test
    fun botRoomIsServerTruthAndCompetitiveProcessorSkipsProfileStats() {
        val migration = projectFile("supabase/migrations/20260831224000_training_bot_competitive_guard_v1.sql").readText()
        val dto = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()

        assertTrue(dto.contains("@SerialName(\"is_bot\") val isBot: Boolean = false"))
        assertTrue(dto.contains("@SerialName(\"bot_difficulty\")"))
        assertTrue(migration.contains("if coalesce(r.is_bot,false) then"))
        assertTrue(migration.contains("competitive_match_requires_two_humans"))
        assertTrue(migration.contains("stats_applied=true"))
        val botBranch = migration.substringAfter("if coalesce(r.is_bot,false) then").substringBefore("else")
        assertFalse(botBranch.contains("rating="))
        assertFalse(botBranch.contains("wins="))
        assertFalse(botBranch.contains("losses="))
        assertFalse(botBranch.contains("total_matches="))
    }

    @Test
    fun streakLeagueAndTournamentRejectBotCompetition() {
        val migration = projectFile("supabase/migrations/20260831224000_training_bot_competitive_guard_v1.sql").readText()
        assertTrue(migration.contains("coalesce(g.is_bot,false)=false"))
        assertTrue(migration.contains("when coalesce(r.is_bot,false) then 0"))
        assertTrue(migration.contains("bot_match_not_competitive"))
        assertTrue(migration.contains("weekly_tournament_no_bot_room_v1"))
    }

    @Test
    fun SonHarfBotStillUsesServerValidationAndDifficulty() {
        val bot = projectFile("supabase/migrations/20260820_bot_difficulty_v1.sql").readText()
        assertTrue(bot.contains("sonharf_word_allowed"))
        assertTrue(bot.contains("normalized_word"))
        assertTrue(bot.contains("easy"))
        assertTrue(bot.contains("hard"))
        assertTrue(bot.contains("order by random()"))
    }

    @Test
    fun WordSiegeBotCandidatesStillPassSharedPracticeValidator() {
        val engine = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()
        assertTrue(engine.contains("runCatching { validateMove(state, 2, placements, horizontal) }"))
        assertTrue(engine.contains("botCandidateScore"))
        assertTrue(engine.contains("TrainingBotDifficulty.EASY"))
        assertTrue(engine.contains("TrainingBotDifficulty.HARD"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
