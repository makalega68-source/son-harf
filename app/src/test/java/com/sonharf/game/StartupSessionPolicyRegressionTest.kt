package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSessionPolicyRegressionTest {
    @Test fun rememberMeCleanupRunsOncePerProcessAndCurrentBrandIsShown() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MainActivity.kt").readText()

        assertTrue(main.contains("private var startupSessionPolicyApplied = false"))
        assertTrue(main.contains("val applySessionPolicy = !startupSessionPolicyApplied"))
        assertTrue(main.contains("startupSessionPolicyApplied = true"))
        assertTrue(main.contains("applySessionPolicy && !rememberLogin && !authDeepLink"))
        assertFalse(main.contains("SupabaseProvider.configured && !rememberLogin && !authDeepLink\n"))
        assertTrue(main.contains("Kelime Tahtı hazırlanıyor"))
        assertTrue(main.contains("Preparing Word Throne"))
        assertFalse(main.contains("Kelime Kuşatması hazırlanıyor"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
