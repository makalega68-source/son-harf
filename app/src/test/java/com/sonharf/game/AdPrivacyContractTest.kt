package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPrivacyContractTest {

    @Test
    fun umpDependencyAndLaunchRefreshArePresent() {
        val gradle = projectFile("app/build.gradle.kts").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MainActivity.kt").readText()

        assertTrue(gradle.contains("com.google.android.ump:user-messaging-platform:4.0.0"))
        assertTrue(main.contains("AdPrivacyManager.requestConsent(this)"))
    }

    @Test
    fun adRequestsAreGatedByPrivacyManager() {
        val rewarded = projectFile("app/src/main/java/com/sonharf/game/RewardedAdController.kt").readText()
        val banner = projectFile("app/src/main/java/com/sonharf/game/NonGameBannerAd.kt").readText()
        val rewardCenter = projectFile("app/src/main/java/com/sonharf/game/RewardCenterScreen.kt").readText()

        assertTrue(rewarded.contains("if (!AdPrivacyManager.adsAllowed)"))
        assertTrue(rewarded.contains("fun clear()"))
        assertFalse(rewarded.contains("MobileAds.initialize"))

        assertTrue(banner.contains("AdPrivacyManager.adsAllowed"))
        assertFalse(banner.contains("MobileAds.initialize"))

        assertTrue(rewardCenter.contains("val adsAllowed = AdPrivacyManager.adsAllowed"))
        assertTrue(rewardCenter.contains("LaunchedEffect(adsAllowed"))
        assertTrue(rewardCenter.contains("if (adsAllowed && !showKasaOnly)"))
        assertTrue(rewardCenter.contains("adController.clear()"))
    }

    @Test
    fun privacyOptionsEntryPointIsAvailableWhenRequired() {
        val manager = projectFile("app/src/main/java/com/sonharf/game/AdPrivacyManager.kt").readText()
        val profile = projectFile("app/src/main/java/com/sonharf/game/CompleteProfileScreen.kt").readText()

        assertTrue(manager.contains("ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED"))
        assertTrue(manager.contains("UserMessagingPlatform.showPrivacyOptionsForm"))
        assertTrue(profile.contains("AdPrivacyManager.privacyOptionsRequired"))
        assertTrue(profile.contains("AdPrivacyManager.showPrivacyOptions"))
    }

    @Test
    fun productionReleaseRejectsTestAdIdsAndMissingSigning() {
        val gradle = projectFile("app/build.gradle.kts").readText()
        val workflow = projectFile(".github/workflows/android.yml").readText()

        assertTrue(gradle.contains("Production release blocked: configure SON_HARF_ADMOB_APP_ID"))
        assertTrue(gradle.contains("Production release blocked: configure SON_HARF_ADMOB_REWARDED_ID"))
        assertTrue(gradle.contains("Production release blocked: release keystore"))
        assertTrue(gradle.contains("appId != googleTestAdMobAppId"))
        assertTrue(gradle.contains("rewardedId != googleTestRewardedAdUnitId"))
        assertFalse(workflow.contains("son-harf-release-v0.8.1"))
        assertTrue(workflow.contains("name: son-harf-release-"))
        assertTrue(workflow.contains("github.run_number"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(
            File(path),
            File("../$path"),
        )
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
