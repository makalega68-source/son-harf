package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminHelpPrivacyRegressionTest {
    @Test
    fun adminEntryIsServerAuthorizedAndHomeButtonIsNotEmailHardcoded() {
        val router = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val api = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()
        assertTrue(router.contains("backend.isCurrentUserAdmin()"))
        assertTrue(api.contains("admin_access_v1"))
        assertTrue(home.contains("if (isAdmin)"))
        assertFalse(home.contains("UnreadChatIcon("))
        assertFalse((router + home).contains("makalega58@gmail.com"))
        assertFalse((router + home).contains("makalega68@gmail.com"))
    }

    @Test
    fun privateChatSecureFlagHasScopedLifecycle() {
        val secure = projectFile("app/src/main/java/com/sonharf/game/PrivateChatSecurity.kt").readText()
        val social = projectFile("app/src/main/java/com/sonharf/game/SocialExperience.kt").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MainActivity.kt").readText()
        assertTrue(secure.contains("DisposableEffect"))
        assertTrue(secure.contains("FLAG_SECURE"))
        assertTrue(secure.contains("clearFlags"))
        assertTrue(secure.contains("activity?.window"))
        assertTrue(social.contains("PrivateChatSecureEffect(enabled = selected != null)"))
        assertFalse(main.contains("FLAG_SECURE"))
    }

    @Test
    fun allThreeGamesUseOneReusableHelpDialogWithoutNavigation() {
        val help = projectFile("app/src/main/java/com/sonharf/game/GameHelpButton.kt").readText()
        val router = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        assertTrue(help.contains("enum class GameHelpType"))
        assertTrue(help.contains("SIEGE"))
        assertTrue(help.contains("LAST_LETTER"))
        assertTrue(help.contains("LETTER_PATH"))
        assertTrue(help.contains("Nasıl Oynanır?"))
        assertTrue(help.contains("How to Play"))
        assertTrue(router.contains("GameHelpButton(GameHelpType.SIEGE"))
        assertTrue(router.contains("GameHelpButton(GameHelpType.LAST_LETTER"))
        assertTrue(router.contains("GameHelpButton(GameHelpType.LETTER_PATH"))
    }

    @Test
    fun lastLetterSecondaryArenaActionsAreGoneButCoreScreenRemains() {
        val duel = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val wrapper = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        assertFalse(duel.contains("Modifier.clickable(onClick = onForfeit)"))
        assertFalse(duel.contains("Modifier.clickable(onClick = onQuickChat)"))
        assertTrue(wrapper.contains("PremierWordDuelScreen()"))
    }

    @Test
    fun migrationBootstrapsRealAdminsAndAllMutationsAreFailClosed() {
        val sql = projectFile("supabase/migrations/20260917130000_admin_help_privacy_package.sql").readText().lowercase()
        assertTrue(sql.contains("makalega58@gmail.com"))
        assertTrue(sql.contains("makalega68@gmail.com"))
        assertTrue(sql.contains("if not public.is_admin()"))
        assertTrue(sql.contains("admin_adjust_player_diamonds_v1"))
        assertTrue(sql.contains("admin_set_player_blocked_v1"))
        assertTrue(sql.contains("admin_audit_log"))
        assertTrue(sql.contains("revoke all"))
        assertFalse(sql.contains("service_role"))
        assertFalse(sql.contains("github_token"))
        assertFalse(sql.contains("admin_secret"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
