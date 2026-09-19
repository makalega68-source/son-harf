package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayAdPolicyRegressionTest {
    @Test fun productionShellKeepsAllGameDestinationsBannerFree() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val rewarded = projectFile("app/src/main/java/com/sonharf/game/RewardedAdController.kt").readText()

        assertTrue(shell.contains("PremiumDestination.LAST_LETTER"))
        assertTrue(shell.contains("PremiumDestination.SIEGE"))
        assertTrue(shell.contains("PremiumDestination.LETTER_PATH"))
        assertTrue(shell.contains("SonHarfTopAdBanner(isPremium = isPro)"))
        assertTrue(rewarded.contains("onUnavailable()"))
        assertFalse(rewarded.contains("onEarned(verificationData)"))
    }

    @Test fun bannerWaitsForVerifiedPremiumStateBeforeBecomingAdEligible() {
        val banner = projectFile("app/src/main/java/com/sonharf/game/NonGameBannerAd.kt").readText()
        assertTrue(banner.contains("resolvedPremium"))
        assertTrue(banner.contains("backend.getVipEntitlements().isPro"))
        assertTrue(banner.contains("val premium = resolvedPremium ?: return"))
        assertTrue(banner.contains("fail closed"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
