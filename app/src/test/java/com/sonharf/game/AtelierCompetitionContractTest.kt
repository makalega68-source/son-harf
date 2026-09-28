package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtelierCompetitionContractTest {
    private fun source(path: String) = File(path).readText()

    @Test fun dailyRaceIsOneSeededOfficialRunWithLiveRankAndBoards() {
        val screen = source("src/main/java/com/sonharf/game/KelimeAtolyesiScreen.kt")
        val comp = source("src/main/java/com/sonharf/game/AtelierCompetition.kt")
        val backend = source("src/main/java/com/sonharf/game/data/AtelierCompetitionBackend.kt")
        val migration = source("../supabase/migrations/20260927130000_atelier_competition_v1.sql")
        val modes = source("../supabase/migrations/20260928180000_atelier_duration_modes_v1.sql")
        // Same letters for everyone, one official try that starts on the server first.
        assertTrue(screen.contains("startRound(KelimeAtolyesiEngine.dailySeed(language, started.day))"))
        assertTrue(screen.contains("AtelierCompetitionBackend.startDaily(language, roundSeconds)"))
        assertTrue(screen.contains("AtelierCompetitionBackend.finishDaily(language, roundSeconds, finished.score, finished.words.size, finished.completedTasks)"))
        assertTrue(screen.contains("if (mode == AtelierMode.DAILY) AtelierRivalStrip(raceBoard, current.score)"))
        assertTrue(comp.contains("sh(\"Yarışa Başla\", \"Start the Race\")"))
        assertTrue(comp.contains("sh(\"Serbest Antrenman\", \"Free Practice\")"))
        listOf("start_atelier_daily_v2", "finish_atelier_daily_v2", "get_atelier_board_v2").forEach {
            assertTrue(backend.contains("\"$it\""))
            assertTrue(modes.contains("public.$it"))
        }
        assertTrue(backend.contains("\"claim_atelier_weekly_reward_v1\""))
        assertTrue(migration.contains("public.claim_atelier_weekly_reward_v1"))
        // One official try per day in each length (1 and 2 minutes), each with its own board.
        assertTrue(modes.contains("add primary key (user_id, language, day, duration_seconds)"))
        assertTrue(modes.contains("started_at > now() - interval '15 minutes'"))
        assertTrue(modes.contains("v_max_tasks integer := case when v_secs = 120 then 15 else 6 end;"))
        assertTrue(comp.contains("sh(\"2 Dakika · 15 görev\", \"2 Minutes · 15 tasks\")"))
        // The round runs to the clock: finishing a task set never ends it early.
        assertFalse(screen.contains("if (result.state.allTasksDone) endRound"))
        assertFalse(screen.contains("secondsLeft += timeBonus"))
        // The workshop still has no typed input and no progression tiers.
        assertFalse(screen.contains("TextField"))
        assertFalse(comp.contains("level", ignoreCase = true))
    }
}
