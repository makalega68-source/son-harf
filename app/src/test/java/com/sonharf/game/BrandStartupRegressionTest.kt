package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandStartupRegressionTest {
    @Test
    fun startupBrandUsesApprovedAppLogoAndCompatibilityAssets() {
        val authGate = File("src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val officialLogo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeHub = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val adaptiveIcon = File("src/main/res/mipmap-anydpi-v26/ic_kelime_tahti.xml")
        val adaptiveFallback = File("src/main/res/mipmap-anydpi/ic_kelime_tahti.xml")
        val appLogo = File("src/main/res/drawable-nodpi/app_logo.webp")
        val authVector = File("src/main/res/drawable/son_harf_gold_teal_logo.xml")
        val gameVector = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")

        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_kelime_tahti\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_kelime_tahti\""))
        assertTrue(adaptiveIcon.isFile)
        assertTrue(adaptiveFallback.isFile)
        assertTrue(adaptiveIcon.readText().contains("@drawable/app_logo"))
        assertTrue(adaptiveFallback.readText().contains("@drawable/app_logo"))
        assertTrue(appLogo.isFile)

        assertTrue(officialLogo.contains("painterResource(R.drawable.app_logo)"))
        assertTrue(officialLogo.contains("contentScale = ContentScale.Fit"))
        assertTrue(authGate.contains("painterResource(R.drawable.son_harf_gold_teal_logo)"))
        assertTrue(siegeHub.contains("painterResource(R.drawable.kelime_kusatma_logo_hd)"))
        assertTrue(home.contains("R.drawable.kelime_kusatma_logo_hd"))
        listOf(authVector, gameVector).forEach {
            assertTrue("Missing compatibility drawable: ${it.path}", it.isFile)
            assertTrue(it.readText().contains("<vector"))
        }

        listOf(
            "src/main/res/drawable-nodpi/kelime_tahti_app_icon.png",
            "src/main/res/drawable-nodpi/kelime_tahti_logo.png",
            "src/main/res/drawable-nodpi/kelime_tahti_logo_latest.png",
            "src/main/res/drawable-nodpi/son_harf_app_icon_master.webp",
            "src/main/res/drawable-nodpi/son_harf_gold_teal_logo.png",
            "src/main/res/drawable/kelime_kusatma_logo.png",
            "src/main/res/drawable/kelime_kusatma_logo_hd.png",
            "src/main/res/drawable/kelime_kusatmasi_brand.png",
            "../assets/kelime-tahti-icon.webp",
            "../assets/kelime_tahti_logo.webp",
        ).forEach { assertFalse("Legacy raster must stay removed: $it", File(it).exists()) }

        assertTrue(File("src/main/res/drawable-nodpi/vfx_twinkle.png").isFile)
        assertTrue(File("src/main/res/drawable-nodpi/profile_frame_round_starter_neutral.png").isFile)
    }
}
