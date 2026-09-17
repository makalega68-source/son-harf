package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPanelNavigationRegressionTest {
    @Test
    fun premiumSettingsKeepsServerAuthorizedAdminPanelReachable() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val access = projectFile("app/src/main/java/com/sonharf/game/AdminAwareSettingsScreen.kt").readText()

        assertTrue(shell.contains("PremiumDestination.SETTINGS -> AdminAwareSettingsScreen("))
        assertTrue(access.contains("backend.getAdminDashboard()"))
        assertTrue(access.contains("if (adminChecked && isAdmin)"))
        assertTrue(access.contains("AdminConsoleScreen { showAdmin = false }"))
        assertTrue(access.contains("MainSettingsScreen("))

        val lowered = access.lowercase()
        assertFalse(lowered.contains("service_role"))
        assertFalse(lowered.contains("service-role"))
        assertFalse(lowered.contains("admin_secret"))
        assertFalse(lowered.contains("@gmail.com"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
