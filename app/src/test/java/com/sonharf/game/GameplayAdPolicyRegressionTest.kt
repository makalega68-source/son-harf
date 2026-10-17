package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayAdPolicyRegressionTest {
    @Test fun productionShellKeepsAllGameDestinationsBannerFreeAndRewardedAdsVerified() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val rewarded = projectFile("app/src/main/java/com/sonharf/game/RewardedAdController.kt").readText()

        assertTrue(shell.contains("PremiumDestination.LAST_LETTER"))
        assertTrue(shell.contains("PremiumDestination.SIEGE"))
        assertTrue(shell.contains("PremiumDestination.WORD_WORKSHOP"))
        assertTrue(shell.contains("destination !in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP)"))
        assertTrue(shell.contains("SonHarfTopAdBanner(isPremium = isPro)"))
        assertTrue(rewarded.contains("verificationUserId.isNullOrBlank()"))
        assertTrue(rewarded.contains("verificationData.isNullOrBlank()"))
        assertTrue(rewarded.contains("setServerSideVerificationOptions"))
        assertTrue(rewarded.contains("ad.show(activity) { onEarned(verificationData) }"))
    }

    @Test fun bannerWaitsForVerifiedPremiumStateBeforeBecomingAdEligible() {
        val banner = projectFile("app/src/main/java/com/sonharf/game/NonGameBannerAd.kt").readText()
        assertTrue(banner.contains("resolvedPremium"))
        assertTrue(banner.contains("backend.getVipEntitlements().isPro"))
        assertTrue(banner.contains("val premium = resolvedPremium ?: return"))
        assertTrue(banner.contains("fails closed"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
