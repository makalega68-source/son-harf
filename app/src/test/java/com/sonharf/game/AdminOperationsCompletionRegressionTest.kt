package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminOperationsCompletionRegressionTest {
    @Test
    fun gameControlsAreAllowListedAndServerEnforced() {
        val api = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()
        val sql = projectFile("supabase/migrations/20260917140000_admin_operations_completion.sql").readText()
        assertTrue(api.contains("admin_set_game_control_v1"))
        assertTrue(sql.contains("son_harf_bot_fallback_enabled"))
        assertTrue(sql.contains("word_siege_enabled"))
        assertTrue(sql.contains("son_harf_enabled"))
        assertTrue(sql.contains("unsupported_game_control"))
        assertTrue(sql.contains("select public.find_or_create_word_siege_game_v2"))
    }

    @Test
    fun announcementsAreBilingualAndMaintenanceAware() {
        val panel = projectFile("app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt").readText()
        val api = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()
        assertTrue(panel.contains("Türkçe duyuru"))
        assertTrue(panel.contains("English announcement"))
        assertTrue(panel.contains("Bakım duyurusu"))
        assertTrue(api.contains("admin_get_announcement_v2"))
        assertTrue(api.contains("admin_set_announcement_v2"))
    }

    @Test
    fun storeAndTestToolsRemainCosmeticAndRatingSafe() {
        val sql = projectFile("supabase/migrations/20260917140000_admin_operations_completion.sql").readText().lowercase()
        assertTrue(sql.contains("admin_set_shop_item_v1"))
        assertTrue(sql.contains("admin_grant_test_item_v1"))
        assertTrue(sql.contains("non_cosmetic_item_blocked"))
        assertTrue(sql.contains("test_account_required"))
        assertFalse(sql.contains("update public.profiles set rating"))
    }

    @Test
    fun buildMetadataContainsNoGithubToken() {
        val gradle = projectFile("app/build.gradle.kts").readText()
        val panel = projectFile("app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt").readText()
        assertTrue(gradle.contains("GIT_COMMIT_SHA"))
        assertTrue(gradle.contains("GIT_BRANCH"))
        assertTrue(panel.contains("BuildConfig.GIT_COMMIT_SHA"))
        assertFalse((gradle + panel).lowercase().contains("github_token"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
