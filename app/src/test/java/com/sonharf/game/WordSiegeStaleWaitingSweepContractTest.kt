package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeStaleWaitingSweepContractTest {
    @Test fun existingTimeoutSweepAlsoCleansStaleWaitingGames() {
        val migration = projectFile("supabase/migrations/20260920162500_word_siege_stale_waiting_sweep_v1.sql").readText()

        assertTrue(migration.contains("cleanup_stale_word_siege_waiting_v1"))
        assertTrue(migration.contains("status='waiting'"))
        assertTrue(migration.contains("interval '30 minutes'"))
        assertTrue(migration.contains("v_count := private.cleanup_stale_word_siege_waiting_v1()"))
        assertTrue(migration.contains("perform private.word_siege_finalize_timeout_v2(v_game_id)"))
        assertTrue(migration.contains("select private.cleanup_stale_word_siege_waiting_v1()"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
