package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalActiveShellContractTest {
    @Test
    fun `all active non-gameplay routes use the professional design system`() {
        val activeSources = listOf(
            "ProfessionalHomeScreen.kt",
            "ProfessionalLeaderboardScreen.kt",
            "ProfessionalCompetitionHubScreen.kt",
            "ProfessionalRetentionScreen.kt",
            "ProfessionalProfileScreen.kt",
            "ProfessionalProfileIdentityScreen.kt",
            "ProfessionalProfileProgressScreen.kt",
            "ProfessionalCollectionScreen.kt",
            "ProfileOwnedThemesSection.kt",
            "EconomyShopScreen.kt",
            "UnifiedProVipScreen.kt",
            "PrivateRoomCenterScreen.kt",
            "ProfessionalSocialScreen.kt",
            "ProfessionalSettingsScreen.kt",
            "CompleteProfileScreen.kt",
        )

        activeSources.forEach { name ->
            val path = "app/src/main/java/com/sonharf/game/$name"
            val source = repoFile(path).readText()
            assertFalse("$name still uses MainUi", source.contains("MainUi."))
            assertFalse("$name still uses SonHarfTheme", source.contains("SonHarfTheme."))
            assertFalse("$name still uses Monster theme", source.contains("Monster"))
            assertTrue("$name must use professional Game tokens/components", source.contains("GameColors.") || source.contains("GameTopBar(") || source.contains("GameSurface("))
        }
    }

    @Test
    fun `production shell routes only to the professional menu surfaces`() {
        val shell = repoFile("app/src/main/java/com/sonharf/game/ProfessionalUnifiedApp.kt").readText()
        val profileEditor = repoFile("app/src/main/java/com/sonharf/game/CompleteProfileScreen.kt").readText()

        listOf(
            "ProfessionalHomeScreen(",
            "ProfessionalLeaderboardScreen(",
            "ProfessionalCompetitionHubScreen(",
            "ProfessionalRetentionScreen(",
            "ProfessionalProfileScreen(",
            "ProfessionalProfileProgressScreen(",
            "ProfessionalCollectionScreen(",
            "EconomyShopScreen(",
            "UnifiedProVipScreen(",
            "PrivateRoomCenterScreen(",
            "ProfessionalSocialScreen(",
            "ProfessionalSettingsScreen(",
            "CompleteProfileScreen(",
        ).forEach { expected -> assertTrue("Missing active route: $expected", shell.contains(expected)) }

        assertTrue(profileEditor.contains("ProfessionalProfileIdentityScreen()"))
        assertFalse(profileEditor.contains("ProfileExperienceV2Screen()"))
        assertFalse(shell.contains("PremiumHomeV3("))
        assertFalse(shell.contains("PremiumUnifiedProApp("))
        assertFalse(shell.contains("MainRetentionScreen("))
        assertFalse(shell.contains("MainSettingsScreen("))
        assertFalse(shell.contains("MainVipScreen("))
        assertFalse(shell.contains("MainSocialScreen("))
        assertFalse(shell.contains("MonsterStyleStoreScreen("))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
