package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalRestorationRegressionTest {
    @Test
    fun stagedRestorationKeepsCriticalProductContracts() {
        val activity = source("MainActivity.kt")
        val shell = source("StableV1App.kt")
        val duel = source("LightDuelUi.kt")
        val siege = source("WordSiegePanMatch.kt")
        val siegePractice = source("WordSiegePracticeScreen.kt")
        val siegeExperience = source("WordSiegeExperience.kt")
        val siegeRules = source("WordSiegeFinalRules.kt")
        val style = source("MonsterStyleStoreScreen.kt")
        val settings = source("MainSettingsVipScreen.kt")
        val admin = source("AdminConsoleScreen.kt")
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()

        // First launch must render before network/auth cleanup and always have a fallback.
        assertFalse(activity.contains("runBlocking"))
        assertTrue(activity.contains("setContent"))
        assertTrue(activity.contains("AppStartupGate"))
        assertTrue(activity.contains("withTimeoutOrNull(6_000)"))
        assertTrue(activity.contains("StartupError"))
        assertTrue(activity.contains("onRetry"))

        // First-install language is mandatory before authentication and remains changeable later.
        assertTrue(shell.contains("FirstRunLanguagePreferences.isComplete"))
        assertTrue(shell.indexOf("FirstRunLanguageScreen") < shell.indexOf("hasVerifiedMembershipSession"))
        assertTrue(shell.contains("Dilini seç / Choose your language"))
        assertTrue(settings.contains("SonHarfPreferences.setLanguage(context, \"tr\")"))
        assertTrue(settings.contains("SonHarfPreferences.setLanguage(context, \"en\")"))

        // Duel keeps separate player/rival photos and the approved light Son Harf palette.
        assertTrue(duel.contains("playerAvatarPath"))
        assertTrue(duel.contains("opponentAvatarPath"))
        assertTrue(duel.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(duel.contains("Color(0xFFF7F9FC)"))
        assertTrue(duel.contains("Color(0xFF1769E0)"))
        assertTrue(duel.contains("Color.White"))
        assertFalse(duel.contains("Color(0xFF101114)"))
        assertFalse(duel.contains("Color(0xFFEAFB17)"))
        assertFalse(duel.contains("Color(0xFF0D0E11)"))

        // Word Siege keeps actions/profiles while direction remains automatic without technical helper text.
        assertTrue(siege.contains("onPass"))
        assertTrue(siege.contains("onExchange"))
        assertTrue(siege.contains("ProfilePhotoAvatarWithGender"))
        assertFalse(siege.contains("Yön otomatik algılanır"))
        assertTrue(siege.contains("Torba ${'$'}{game.bag.length}"))
        assertTrue(siege.contains("WordSiegeFinalRules.netScore"))
        assertTrue(siegePractice.contains("showPass"))
        assertTrue(siegePractice.contains("showExchange"))
        assertFalse(siegePractice.contains("Yön otomatik algılanır"))
        assertTrue(siegePractice.contains("Torba ${'$'}{state.bag.length}"))
        assertTrue(siegeExperience.contains("WordSiegeFinalRules.detectOrientation"))
        assertTrue(siegeRules.contains("CUBE_TRANSFER_POINTS: Int = 2"))

        // Style remains cosmetic-only while preserving the real theme purchase/equip backend.
        assertTrue(style.contains("theme_dark_arena"))
        assertTrue(style.contains("purchaseShopItem"))
        assertTrue(style.contains("equipShopItem"))
        assertTrue(style.contains("PROFILE STYLE"))
        assertTrue(style.contains("MATCH STYLE"))
        assertTrue(style.contains("isRuntimeReadyStyle"))
        assertTrue(style.contains("FAIR PLAY PROMISE"))

        // Admin remains fail-closed, RPC-backed and on the unified light palette.
        assertTrue(admin.contains("backend.getAdminDashboard()"))
        assertTrue(admin.contains("dashboard = null"))
        assertTrue(admin.contains("Color(0xFFF5F8FC)"))
        assertTrue(admin.contains("Color(0xFFEEF5FF)"))
        assertTrue(admin.contains("Color(0xFF1677FF)"))
        val privilegedSurface = (admin + backend).lowercase()
        assertFalse(privilegedSurface.contains("service_role"))
        assertFalse(privilegedSurface.contains("admin_secret"))
        assertTrue(backend.contains("postgrest.rpc"))
    }

    private fun source(name: String): String =
        projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
