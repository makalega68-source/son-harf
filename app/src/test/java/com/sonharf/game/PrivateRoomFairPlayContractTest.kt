package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateRoomFairPlayContractTest {
    @Test fun privateRoomsAreReachableButNeverRankedOrRewardFarmable() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/PrivateRoomBackend.kt").readText()
        val screen = projectFile("app/src/main/java/com/sonharf/game/PrivateRoomCenterScreen.kt").readText()
        val migration = projectFile("supabase/migrations/20260920125500_private_rooms_unranked_no_rewards.sql").readText()

        assertTrue(backend.contains("create_room_normal_v1"))
        assertTrue(backend.contains("join_room_by_code"))
        assertTrue(backend.contains("cancel_private_room"))
        assertTrue(screen.contains("onRoomReady"))
        assertTrue(screen.contains("ODA KODU"))
        assertTrue(migration.contains("'private','private'"))
        assertTrue(migration.contains("if r.room_mode='private' or r.room_type='private' then"))
        assertTrue(migration.contains("return query select v_won,0,0,0"))
        assertTrue(migration.contains("set room_mode='private'"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
