package com.sonharf.game

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainExperienceContractTest {

    @Test
    fun authenticatedUsersEnterTheNewMonsterExperience() {
        val stable = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt").readText()

        assertTrue(stable.contains("MonsterExperienceApp("))
        assertTrue(main.contains("OnlineGameScreenV6()"))
        assertTrue(main.contains("MonsterDestination.WORD_SIEGE"))
        assertTrue(main.contains("WordSiegeExperienceScreen"))
        assertTrue(main.contains("MonsterDestination.LEAGUE"))
        assertTrue(main.contains("MonsterDestination.SOCIAL"))
        assertTrue(main.contains("MonsterDestination.STYLE"))
        assertTrue(main.contains("MonsterStyleStoreScreen()"))
        assertTrue(main.contains("MonsterDestination.PROFILE"))
        assertFalse(main.contains("TargetNeonGameScreen"))
    }

    @Test
    fun homeUsesPurchasedMonsterShellAndCompetitiveEntrances() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt").readText()
        val classic = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")

        assertTrue(main.contains("MonsterLiveMatchCard("))
        assertTrue(main.contains("KELİME KUŞATMASI"))
        assertTrue(main.contains("MonsterProgressStrip("))
        assertTrue(main.contains("STANDART DÜELLO"))
        assertTrue(main.contains("Günlük görevler"))
        assertTrue(main.contains("Sosyal & arkadaşlar"))
        assertTrue(classic.isFile)
    }

    @Test
    fun activeShellUsesUnifiedBlueWhitePaletteAndStoreThemeRuntime() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt").readText()
        val store = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val cosmetics = projectFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()

        assertTrue(main.contains("SON HARF ACTION UI ADAPTATION"))
        assertTrue(main.contains("SonHarfTheme.Background"))
        assertTrue(main.contains("SonHarfTheme.PrimaryBlue"))
        assertFalse(main.contains("Color(0xFF07111F)"))
        assertFalse(main.contains("Color(0xFF111D2E)"))
        assertTrue(store.contains("theme_monster_blue"))
        assertTrue(store.contains("Mavi Beyaz Arena"))
        assertTrue(store.contains("SATIN AL"))
        assertTrue(cosmetics.contains("monsterBlueTheme"))
        assertTrue(cosmetics.contains("gameThemeId == \"theme_monster_blue\""))
    }

    @Test
    fun activeNonMatchSurfacesUseRealBackendData() {
        val combined = listOf(
            "MonsterExperienceApp.kt",
            "MainPlayerProfileScreen.kt",
            "MainRetentionScreen.kt",
            "MainSocialScreen.kt",
            "MainSettingsVipScreen.kt",
        ).joinToString("\n") { name ->
            projectFile("app/src/main/java/com/sonharf/game/$name").readText()
        }

        assertTrue(combined.contains("backend.getProfile("))
        assertTrue(combined.contains("backend.getUnifiedMissions()"))
        assertTrue(combined.contains("backend.getFriends()"))
        assertTrue(combined.contains("profile?.isVip == true"))
        assertFalse(combined.contains("Mock"))
        assertFalse(combined.contains("fake", ignoreCase = true))
    }

    @Test
    fun legacyThemeIsNotTheActiveStyleDestination() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt").readText()
        val store = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val catalog = projectFile("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt").readText()
        val offeredProducts = catalog.substringAfter("val oneTimeProducts = listOf(")

        assertTrue(main.contains("MonsterDestination.STYLE -> MonsterStyleStoreScreen()"))
        assertTrue(store.contains("theme_monster_blue"))
        assertFalse(store.contains("theme_aurora"))
        assertFalse(store.contains("theme_midnight"))
        assertFalse(store.contains("theme_neon"))
        assertFalse(offeredProducts.contains("THEME_NEON"))
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
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/EmbeddedGameKeyboard.kt")

        assertTrue(online.contains("var attempt = 0"))
        assertTrue(online.contains("else minOf(5000L, 1200L + attempt * 600L)"))
        assertTrue(online.contains("You must FORFEIT before returning home."))
        assertTrue(online.contains("SonHarfUiState.homeRequest += 1"))
        assertTrue(arena.contains("gameUppercase("))
        assertTrue(arena.contains("duelScoreFontSize(score).sp"))
        assertEquals(
            "Frozen keyboard changed",
            "f5143f6701c3bff95119aa6ce61d5f64acc15f8803fd2c6b48bcee4b5625d4a2",
            keyboard.sha256(),
        )
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { "%02x".format(it) }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
