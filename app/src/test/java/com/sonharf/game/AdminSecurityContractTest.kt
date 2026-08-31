package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminSecurityContractTest {
    @Test
    fun adminEntryUsesServerCapabilityNotClientEmail() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()

        assertTrue(shell.contains("getAdminAccess().authorized"))
        assertTrue(shell.contains("AdminConsoleScreen"))
        assertTrue(shell.contains("Admin Paneli"))
        assertFalse(shell.contains("makalega58@gmail.com"))
        assertFalse(shell.contains("makalega68@gmail.com"))
        assertTrue(backend.contains("admin_access_v1"))
    }

    @Test
    fun hardeningMigrationProtectsAdminOwnerAndMaintenancePaths() {
        val sql = projectFile("supabase/migrations/20260831163500_admin_security_hardening_v3.sql").readText()
        val ownerFix = projectFile("supabase/migrations/20260831165000_owner_son_coin_purchase_fix.sql").readText()

        assertTrue(sql.contains("join auth.users u on u.id=a.user_id"))
        assertTrue(sql.contains("admin_access_v1"))
        assertTrue(sql.contains("revoke all on function public.admin_access_v1() from public, anon"))
        assertTrue(sql.contains("maintenance_mode"))
        assertTrue(sql.contains("matchmaking_enabled"))
        assertTrue(sql.contains("enforce_chat_admin_control_v1"))
        assertTrue(sql.contains("stale_rooms_detected"))
        assertFalse(sql.contains("set status='cancelled',turn_deadline=null,bot_turn=false,last_event='admin_stale_room_repair'"))
        assertTrue(ownerFix.contains("on conflict on constraint user_mascot_fruit_inventory_pkey"))
        assertTrue(ownerFix.contains("unlimited_son_coin"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
