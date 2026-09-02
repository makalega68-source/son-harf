package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPanelSecurityRegressionTest {
    @Test
    fun adminPanelStaysFailClosedAndUsesBackendAuthorization() {
        val panel = projectFile("app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()

        assertTrue(panel.contains("backend.getAdminDashboard()"))
        assertTrue(panel.contains("backend.getAdminTopStoreItems()"))
        assertTrue(panel.contains("backend.getAdminGameControls()"))
        assertTrue(panel.contains("backend.getAdminAnnouncement()"))
        assertTrue(panel.contains("backend.adminSearchPlayers"))
        assertTrue(panel.contains("backend.adminSetPlayerVip"))
        assertTrue(panel.contains("backend.adminSetOwnerAccount"))
        assertTrue(panel.contains("dashboard = null"))
        assertTrue(panel.contains("yalnızca yetkili yönetici"))

        val combined = (panel + "\n" + backend).lowercase()
        assertFalse(combined.contains("service_role"))
        assertFalse(combined.contains("service-role"))
        assertFalse(combined.contains("supabase_service"))
        assertFalse(combined.contains("admin_secret"))
        assertTrue(backend.contains("postgrest.rpc"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
