package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayAdPolicyRegressionTest {
    @Test fun productionShellKeepsAllGameDestinationsBannerFreeAndRewardedAdsVerified() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val rewarded = projectFile("app/src/main/java/com/sonharf/game/RewardedAdController.kt").readText()
        val gameplayBlock = shell.substringAfter("val inGameplay =").substringBefore("val scheme =")

        assertTrue(gameplayBlock.contains("PremiumDestination.LAST_LETTER"))
        assertTrue(gameplayBlock.contains("PremiumDestination.SIEGE"))
        assertTrue(gameplayBlock.contains("PremiumDestination.LETTER_PATH"))
        assertTrue(shell.contains("if (!inGameplay) SonHarfTopAdBanner(isPremium = isPro)"))
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
