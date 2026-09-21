package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun purchasedThemeUsesRealAssetAtlasAndReusableGameComponents() {
        val purchased = source("PurchasedGameTheme.kt")
        val primitives = source("AppUiPrimitives.kt")

        listOf(
            "PANEL_LARGE",
            "PANEL_MEDIUM",
            "PANEL_SMALL",
            "BUTTON_GREEN",
            "BUTTON_BLUE",
            "NAV_HOME",
            "NAV_SOCIAL",
            "NAV_SHOP",
            "NAV_PROFILE",
            "LEADERBOARD_ROW",
            "OLD_MISSION_ROW",
            "OLD_DAILY_REWARD",
            "SHOP_SHELVES",
            "SEASON_BANNER",
        ).forEach { asset -> assertTrue("Missing purchased asset crop: $asset", purchased.contains(asset)) }

        listOf(
            "PurchasedPanel",
            "PurchasedButton",
            "PurchasedIconButton",
            "PurchasedSectionHeader",
            "PurchasedCurrencyBar",
            "PurchasedAvatarFrame",
            "PurchasedProgress",
            "PurchasedNavItem",
        ).forEach { component -> assertTrue("Missing purchased UI component: $component", purchased.contains("fun $component")) }

        assertTrue(primitives.contains("PurchasedGameBackdrop"))
        assertTrue(primitives.contains("PurchasedPanel"))
        assertTrue(primitives.contains("PurchasedButton"))
        assertFalse(primitives.contains("Surface(onClick"))
    }

    @Test
    fun atlasIsSelectedProductionAssetSetNotWholePurchasedPacks() {
        val assetsDir = projectFile("app/src/main/assets")
        val atlasParts = assetsDir.listFiles().orEmpty().filter { it.name.startsWith("purchased_ui_atlas_") && it.extension == "b64" }
        assertTrue("Purchased atlas chunks missing", atlasParts.size == 4)
        assertTrue("Atlas chunks unexpectedly empty", atlasParts.all { it.length() > 100L })
        assertFalse("Purchased ZIP must not be packaged", assetsDir.walkTopDown().any { it.extension.equals("zip", true) })
        assertFalse("PSD sources must not be packaged", assetsDir.walkTopDown().any { it.extension.equals("psd", true) })
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()
    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
