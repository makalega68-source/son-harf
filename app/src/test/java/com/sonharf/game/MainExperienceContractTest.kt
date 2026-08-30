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
    fun authenticatedUsersEnterTheCompleteMainExperience() {
        val stable = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MainExperienceApp.kt").readText()

        assertTrue(stable.contains("SonHarfMainApp("))
        assertTrue(main.contains("OnlineGameScreenV6()"))
        assertTrue(main.contains("sh(\"OYNA\", \"PLAY\")"))
        assertTrue(main.contains("MainDestination.LEAGUE"))
        assertTrue(main.contains("MainDestination.SOCIAL"))
        assertTrue(main.contains("MainDestination.STYLE"))
        assertTrue(main.contains("MainDestination.PROFILE"))
        assertFalse(main.contains("TargetNeonGameScreen"))
    }

    @Test
    fun activeNonMatchSurfacesUseRealBackendData() {
        val combined = listOf(
            "MainExperienceApp.kt",
            "MainPlayerProfileScreen.kt",
            "MainRetentionScreen.kt",
            "MainSocialScreen.kt",
            "MainSettingsVipScreen.kt",
        ).joinToString("\n") { name ->
            projectFile("app/src/main/java/com/sonharf/game/$name").readText()
        }

        assertTrue(combined.contains("backend.getProfile("))
        assertTrue(combined.contains("backend.getGrowthDashboard()"))
        assertTrue(combined.contains("backend.getUnifiedMissions()"))
        assertTrue(combined.contains("backend.getFriends()"))
        assertTrue(combined.contains("backend.getRivalHistory("))
        assertTrue(combined.contains("profile?.isVip == true"))
        assertFalse(combined.contains("Mock"))
        assertFalse(combined.contains("fake", ignoreCase = true))
    }

    @Test
    fun styleShopDoesNotOfferTheLegacyNeonTheme() {
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val playProducts = projectFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()
        val catalog = projectFile("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt").readText()
        val offeredProducts = catalog.substringAfter("val oneTimeProducts = listOf(")

        assertTrue(shop.contains("STYLE"))
        assertFalse(shop.contains("game_theme"))
        assertFalse(shop.contains("keyboard_theme"))
        assertFalse(playProducts.contains("THEME_NEON"))
        assertFalse(offeredProducts.contains("THEME_NEON"))
        assertFalse(shop.contains("kozmetik", ignoreCase = true))
    }

    @Test
    fun frozenMatchSurfaceFilesRemainByteForByteStable() {
        val expected = mapOf(
            "OnlineGameScreenV6.kt" to "e01d2715a95cd78b70bf5f299ad88a24759a1cff0b4c823e1b2e3ede3629e393",
            "LightDuelUi.kt" to "f38c6533b07cd101a405a960adab219f14ef432a28998a453b6ae9c4f4249da2",
            "EmbeddedGameKeyboard.kt" to "f5143f6701c3bff95119aa6ce61d5f64acc15f8803fd2c6b48bcee4b5625d4a2",
        )

        expected.forEach { (name, checksum) ->
            val file = projectFile("app/src/main/java/com/sonharf/game/$name")
            assertEquals("Frozen match file changed: $name", checksum, file.sha256())
        }
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
