package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRoomTerminalTimestampContractTest {
    @Test fun everyTerminalSonHarfRoomGetsFinishedAt() {
        val migration = projectFile("supabase/migrations/20260920132000_game_room_terminal_timestamp_guard.sql").readText()
        assertTrue(migration.contains("new.status in ('finished','cancelled')"))
        assertTrue(migration.contains("new.finished_at := now()"))
        assertTrue(migration.contains("before insert or update of status, finished_at"))
        assertTrue(migration.contains("where status in ('finished','cancelled')"))
        assertTrue(migration.contains("and finished_at is null"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
