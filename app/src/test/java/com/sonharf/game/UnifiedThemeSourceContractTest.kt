package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedThemeSourceContractTest {
    @Test
    fun activeApkV2ShellUsesPurchasedGameUiWithoutChangingAuthoritativeFlows() {
        val premium = source("PremiumAdultApp.kt")
        val startup = source("StableV1App.kt")
        val purchased = source("PurchasedGameTheme.kt")
        val primitives = source("AppUiPrimitives.kt")
        val cosmetics = source("CosmeticRuntime.kt")

        assertTrue(startup.contains("PremiumAdultApp"))
        assertFalse(startup.contains("PremiumCanvaAppV2(onSignedOut"))
        assertFalse(startup.contains("PremiumUnifiedProApp"))
        assertTrue(startup.contains("SonHarfCosmetics.restore(context)"))
        assertTrue(cosmetics.contains("BLACK_THEME_ID = \"theme_black\""))

        assertTrue(purchased.contains("real purchased-asset theme layer", ignoreCase = true))
        assertTrue(purchased.contains("purchased_ui_atlas_00.b64"))
        assertTrue(purchased.contains("drawPurchasedCrop"))
        assertTrue(primitives.contains("PurchasedPanel"))
        assertTrue(primitives.contains("PurchasedButton"))
        assertTrue(premium.contains("PurchasedNavItem"))
        assertTrue(premium.contains("PurchasedUiAsset.NAV_SHOP"))
        assertTrue(premium.contains("PurchasedUiAsset.ICON_SWORDS"))

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
