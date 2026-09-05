package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualRegressionContractTest {
    @Test
    fun criticalSurfacesAndCtasUseSharedAccessibleTokens() {
        val profile = source("MainPlayerProfileScreen.kt")
        val practice = source("WordSiegePracticeScreen.kt")
        val siege = source("WordSiegeExperience.kt")
        assertTrue(profile.contains("color = MainUi.Text"))
        assertTrue(profile.contains("color = MainUi.Muted"))
        assertTrue(practice.contains("disabledContainerColor = SonHarfTheme.DisabledBackground"))
        assertTrue(practice.contains("containerColor = MainUi.Blue"))
        assertTrue(siege.contains("disabledContentColor = SonHarfTheme.DisabledContent"))
        assertFalse(practice.contains("color = SiegeBlueSoft") && practice.contains("color = Color.White"))
    }

    @Test
    fun botPortraitNeverUsesUserStorageAndSharedAvatarLayerIsRetained() {
        val avatars = source("ProfilePhotoRuntime.kt")
        val duel = source("LightDuelUi.kt")
        assertTrue(avatars.contains("internal fun SyntheticBotPortrait"))
        assertTrue(avatars.contains("avatarPath = null"))
        assertTrue(duel.contains("SyntheticBotPortrait("))
        assertTrue(avatars.contains("internal fun ProfilePhotoAvatarWithGender"))
    }

    @Test
    fun matchmakingUsesExclusiveStateModel() {
        val duel = source("OnlineGameScreenV6.kt")
        assertTrue(duel.contains("enum class MatchmakingUiState"))
        assertTrue(duel.contains("Idle, Searching, Matched, Error, Cancelled"))
        assertTrue(duel.contains("matchmakingState == MatchmakingUiState.Searching"))
    }

    @Test
    fun finalUiStagesRemainActiveAndAccessible() {
        val auth = source("RequiredAuthGate.kt")
        val profile = source("MainPlayerProfileScreen.kt")
        val retention = source("MainRetentionScreen.kt")
        val store = source("MonsterStyleStoreScreen.kt")
        val settings = source("MainSettingsVipScreen.kt")
        val cipher = source("DailyCipherScreen.kt")
        val league = source("LeaderboardExperience.kt")

        assertTrue(auth.contains("registerStep"))
        assertTrue(auth.contains("DEVAM ET"))
        assertTrue(profile.contains("ProfileDisclosureHeader"))
        assertTrue(profile.contains("Modifier.width(124.dp).height(136.dp)"))
        assertTrue(retention.contains("RetentionSectionRail"))
        assertTrue(retention.contains("sortedByDescending"))
        assertTrue(store.contains("selectedCategory"))
        assertTrue(settings.contains("VipComparisonTable"))
        assertFalse(settings.contains("8.5.sp"))
        assertTrue(cipher.contains("CutCornerShape"))
        assertFalse(cipher.contains("6.sp else 7.sp"))
        assertTrue(league.contains("ProfilePhotoAvatarRectWithGender"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()
    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
