package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeApkV2ShellUsesCleanRealHomeWithoutChangingAuthoritativeFlows() {
        val premium = source("PremiumAdultApp.kt")
        val startup = source("StableV1App.kt")
        val modernHome = source("ModernHomeScreen.kt")
        val modernHomeNav = source("ModernHomeBottomBar.kt")
        val purchased = source("PurchasedGameTheme.kt")
        val primitives = source("AppUiPrimitives.kt")
        val cosmetics = source("CosmeticRuntime.kt")

        assertTrue(startup.contains("PremiumAdultApp"))
        assertFalse(startup.contains("PremiumCanvaAppV2(onSignedOut"))
        assertFalse(startup.contains("PremiumUnifiedProApp"))
        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))
        assertTrue(cosmetics.contains("BLACK_THEME_ID = \"theme_black\""))

        // Purchased resources still exist for screens that have not yet been redesigned,
        // but the user-approved Home work must not depend on the rejected purchased atlas shell.
        assertTrue(purchased.contains("real purchased-asset theme layer", ignoreCase = true))
        assertTrue(purchased.contains("R.raw.purchased_ui_atlas_00"))
        assertTrue(purchased.contains("R.raw.purchased_cta_atlas"))
        assertTrue(purchased.contains("drawPurchasedCrop"))
        assertTrue(purchased.contains("drawPurchasedHorizontalSlice"))
        assertTrue(primitives.contains("PurchasedPanel"))
        assertTrue(primitives.contains("PurchasedButton"))

        assertTrue(premium.contains("ModernAdultHome("))
        assertTrue(premium.contains("ModernHomeBottomNavigation("))
        assertFalse(premium.contains("PremiumHomeCommandDeckPolished"))
        assertTrue(modernHome.contains("ModernSiegeHero("))
        assertTrue(modernHome.contains("ModernWeeklyBest("))
        assertTrue(modernHome.contains("ModernDailyProgress("))
        assertTrue(modernHome.contains("getGrowthDashboard()"))
        assertTrue(modernHome.contains("getWeeklyTopV210(limit = 3)"))
        assertFalse(modernHome.contains("PurchasedPanel("))
        assertFalse(modernHome.contains("PurchasedAsset("))
        assertFalse(modernHome.contains("PurchasedButton("))
        assertFalse(modernHomeNav.contains("PurchasedPanel("))
        assertFalse(modernHomeNav.contains("PurchasedAsset("))
        assertFalse(modernHomeNav.contains("PurchasedNavItem("))

        assertTrue(premium.contains("OnlineGameBackend()"))
        assertTrue(premium.contains("getInventory()"))
        assertTrue(premium.contains("getVipEntitlements()"))
        assertTrue(premium.contains("WordSiegeExperienceScreen"))
        assertTrue(premium.contains("OnlineGameScreenV6"))
        assertTrue(premium.contains("LetterLadderGameScreen"))
        assertFalse(premium.contains("MageCatCompanion("))
        assertFalse(premium.contains("MonsterExperienceApp"))
    }

    @Test
    fun purchasedGraphicsRemainAvailableForUnredesignedScreensAndNoSourceArchivesArePackaged() {
        val raw = projectFile("app/src/main/res/raw")
        listOf(
            "purchased_ui_atlas_00.b64",
            "purchased_ui_atlas_01.b64",
            "purchased_ui_atlas_02.b64",
            "purchased_ui_atlas_03.b64",
            "purchased_cta_atlas.b64",
        ).forEach { name ->
            val file = File(raw, name)
            assertTrue("Missing purchased Android resource: $name", file.isFile && file.length() > 100L)
        }
        val appMain = projectFile("app/src/main")
        assertFalse(appMain.walkTopDown().any { it.isFile && it.extension.equals("zip", true) })
        assertFalse(appMain.walkTopDown().any { it.isFile && it.extension.equals("psd", true) })
    }

    @Test
    fun buildWorkflowsNeverMutateSourcesWithLegacyThemeScripts() {
        val workflows = projectFile(".github/workflows").walkTopDown()
            .filter { it.isFile && it.extension in setOf("yml", "yaml") }
            .joinToString("\n") { it.readText() }
        assertFalse(workflows.contains("apply_monster_duel_theme.py"))
        assertFalse(workflows.contains("rebuild_monster_duel_layout.py"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()
    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
