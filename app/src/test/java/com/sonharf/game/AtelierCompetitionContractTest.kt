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
        // Same letters for everyone, one official try that starts on the server first.
        assertTrue(screen.contains("startRound(KelimeAtolyesiEngine.dailySeed(language, started.day))"))
        assertTrue(screen.contains("AtelierCompetitionBackend.startDaily(language)"))
        assertTrue(screen.contains("AtelierCompetitionBackend.finishDaily(language, finished.score, finished.words.size, finished.completedTasks)"))
        assertTrue(screen.contains("if (mode == AtelierMode.DAILY) AtelierRivalStrip(raceBoard, current.score)"))
        assertTrue(comp.contains("sh(\"Yarışa Başla\", \"Start the Race\")"))
        assertTrue(comp.contains("sh(\"Serbest Antrenman\", \"Free Practice\")"))
        listOf("start_atelier_daily_v1", "finish_atelier_daily_v1", "get_atelier_board_v1", "claim_atelier_weekly_reward_v1").forEach {
            assertTrue(backend.contains("\"$it\""))
            assertTrue(migration.contains("public.$it"))
        }
        assertTrue(migration.contains("primary key (user_id, language, day)"))
        assertTrue(migration.contains("started_at > now() - interval '15 minutes'"))
        assertTrue(migration.contains("p_score > 3000"))
        // The workshop still has no typed input and no progression tiers.
        assertFalse(screen.contains("TextField"))
        assertFalse(comp.contains("level", ignoreCase = true))
    }
}
