package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBannerPolicyContractTest {
    private fun projectFile(pathFromApp: String): File {
        val direct = File(pathFromApp)
        if (direct.exists()) return direct
        val underApp = File("app", pathFromApp)
        if (underApp.exists()) return underApp
        error("Could not resolve project file: $pathFromApp from ${File(".").absolutePath}")
    }

    private fun source(path: String): String = projectFile(path).readText()

    @Test
    fun bannerIsAdaptiveVisibleWhileLoadingAndLifecycleManaged() {
        val banner = source("src/main/java/com/sonharf/game/NonGameBannerAd.kt")
        assertTrue(banner.contains("getCurrentOrientationAnchoredAdaptiveBannerAdSize"))
        assertTrue(banner.contains("canReserveBanner"))
        assertTrue(banner.contains("val canLoadAd = policyAllows && adUnitId.isNotBlank()"))
        assertTrue(banner.contains("remember(context, adUnitId, canLoadAd)"))
        assertTrue(banner.contains("if (!canLoadAd)"))
        assertTrue(banner.contains("runCatching"))
        assertTrue(banner.contains("if (!slotVisible) return"))
        assertTrue(banner.contains(".height(50.dp)"))
        assertTrue(banner.contains("if (loaded && canLoadAd && adView != null)"))
        assertTrue(banner.contains("sh(\"REKLAM\", \"AD\")"))
        assertTrue(banner.contains("view.pause()"))
        assertTrue(banner.contains("view.resume()"))
        assertTrue(banner.contains("adView?.destroy()"))
        assertTrue(banner.contains("isPremium"))
        assertTrue(banner.contains("adsEnabled"))
    }

    @Test
    fun gameplayRouteExplicitlyDisablesBanner() {
        val app = source("src/main/java/com/sonharf/game/SonHarfIntegratedApp.kt")
        assertTrue(app.contains("visible = screen != AppScreen.GAME"))
        assertTrue(app.contains("AppScreen.GAME -> key(gameKey) { OnlineGameScreenV6() }"))
    }

    @Test
    fun gameplayImplementationsDoNotInstantiateBannerDirectly() {
        val sourceRoot = projectFile("src/main/java/com/sonharf/game")
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && (it.name.contains("Game", ignoreCase = true) || it.name.contains("Arena", ignoreCase = true)) }
            .filter { it.name != "SonHarfIntegratedApp.kt" && it.name != "NonGameBannerAd.kt" }
            .filter { it.readText().contains("SonHarfTopAdBanner(") }
            .toList()
        assertFalse("Gameplay files must never host banner ads: $offenders", offenders.isNotEmpty())
    }

    @Test
    fun debugUsesOfficialAdaptiveBannerTestUnit() {
        val gradle = projectFile("build.gradle.kts").readText()
        assertTrue(gradle.contains("ca-app-pub-3940256099942544/9214589741"))
        assertTrue(gradle.contains("SON_HARF_ADMOB_BANNER_ID"))
        assertTrue(gradle.contains("Production release blocked: configure SON_HARF_ADMOB_BANNER_ID"))
    }
}
