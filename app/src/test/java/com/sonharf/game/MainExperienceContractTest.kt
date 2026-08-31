package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainExperienceContractTest {

    @Test
    fun authenticatedUsersEnterTheCompleteMainExperience() {
        val stable = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MainExperienceApp.kt").readText()

        assertTrue(stable.contains("SonHarfMainApp("))
        assertTrue(main.contains("OnlineGameScreenV6()"))
        assertTrue(main.contains("title = \"SON HARF\""))
        assertTrue(main.contains("MainDestination.WORD_SIEGE"))
        assertTrue(main.contains("WordSiegeExperienceScreen"))
        assertTrue(main.contains("KELİME KUŞATMASI"))
        assertTrue(main.contains("MainDestination.LEAGUE"))
        assertTrue(main.contains("MainDestination.SOCIAL"))
        assertTrue(main.contains("MainDestination.STYLE"))
        assertTrue(main.contains("MainDestination.PROFILE"))
        assertTrue(main.contains("MainDestination.SEASON"))
        assertTrue(main.contains("MainDestination.REWARDS"))
        assertFalse(main.contains("TargetNeonGameScreen"))
    }

    @Test
    fun homeUsesTwoFullWidthGameLaunchersInsteadOfCrampedHalfCards() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MainExperienceApp.kt").readText()
        val classic = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")

        assertTrue(main.contains("title = \"SON HARF\""))
        assertTrue(main.contains("title = sh(\"KELİME KUŞATMASI\", \"WORD SIEGE\")"))
        assertTrue(main.contains("badge = sh(\"ANLIK\", \"LIVE\")"))
        assertTrue(main.contains("Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp))"))
        assertTrue(main.contains("modifier = Modifier.fillMaxWidth(), onClick"))
        assertTrue(main.contains("modifier = modifier.height(112.dp)"))
        assertTrue(classic.isFile)
    }

    @Test
    fun activeNonMatchSurfacesUseRealBackendData() {
        val combined = listOf(
            "MainExperienceApp.kt", "MainPlayerProfileScreen.kt", "MainRetentionScreen.kt", "MainSocialScreen.kt",
            "MainSettingsVipScreen.kt", "SeasonCenterScreen.kt", "RewardCenterScreen.kt", "ProfileStyleInventoryScreen.kt",
        ).joinToString("\n") { name -> projectFile("app/src/main/java/com/sonharf/game/$name").readText() }

        assertTrue(combined.contains("backend.getProfile("))
        assertTrue(combined.contains("backend.getGrowthDashboard()"))
        assertTrue(combined.contains("backend.getUnifiedMissions()"))
        assertTrue(combined.contains("backend.getFriends()"))
        assertTrue(combined.contains("backend.getRivalHistory("))
        assertTrue(combined.contains("profile?.isVip == true"))
        assertTrue(combined.contains("backend.getMetaProgressV2()"))
        assertTrue(combined.contains("getRewardCenterStatus()"))
        assertFalse(combined.contains("Mock"))
        assertFalse(combined.contains("fakeProfile", ignoreCase = true))
        assertFalse(combined.contains("fakeMissions", ignoreCase = true))
        assertFalse(combined.contains("fakeRewards", ignoreCase = true))
    }

    @Test
    fun styleShopHasConcreteFramesKeyboardAndGameThemePreviews() {
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val runtime = projectFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/EmbeddedGameKeyboard.kt").readText()
        val vip = projectFile("app/src/main/java/com/sonharf/game/MainSettingsVipScreen.kt").readText()

        assertTrue(shop.contains("FrameItemPreview"))
        assertTrue(shop.contains("KeyboardItemPreview"))
        assertTrue(shop.contains("GameThemeItemPreview"))
        assertTrue(shop.contains("keyboardPaletteFor(item.id)"))
        assertTrue(shop.contains("gamePaletteFor(item.id)"))
        assertTrue(runtime.contains("val keyboardPalette"))
        assertTrue(runtime.contains("val gamePalette"))
        assertTrue(keyboard.contains("SonHarfCosmetics.keyboardPalette"))
        assertFalse(vip.contains("GOOGLE PLAY'DE YÖNET"))
        assertFalse(vip.contains("MANAGE ON GOOGLE PLAY"))
    }

    @Test
    fun profilePhotoVisibilityIsActuallyEnforced() {
        val avatar = projectFile("app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()
        assertTrue(avatar.contains("visible && !avatarPath.isNullOrBlank()"))
        assertTrue(avatar.contains("visible: Boolean = true"))
    }

    @Test
    fun requestedMatchFixesStayBoundedToReliabilityAndLegibility() {
        val online = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()

        assertTrue(online.contains("var attempt = 0"))
        assertTrue(online.contains("else minOf(5000L, 1200L + attempt * 600L)"))
        assertTrue(online.contains("You must FORFEIT before returning home."))
        assertTrue(online.contains("SonHarfUiState.homeRequest += 1"))
        assertTrue(arena.contains("gameUppercase("))
        assertTrue(arena.contains("duelScoreFontSize(score).sp"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
