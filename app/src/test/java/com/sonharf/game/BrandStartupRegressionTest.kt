package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandStartupRegressionTest {
    @Test
    fun startupBrandUsesCurrentCompatibilityAssetsAndApprovedLauncherIcon() {
        val authGate = File("src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val officialLogo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeHub = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val launcherIcon = File("src/main/res/drawable-nodpi/kelime_kusatmasi_app_icon.webp")
        val launcherVector = File("src/main/res/drawable/kelime_tahti_app_icon.xml")
        val homeVector = File("src/main/res/drawable/son_harf_app_icon_master.xml")
        val authVector = File("src/main/res/drawable/son_harf_gold_teal_logo.xml")
        val gameVector = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")
        val launcherBadge = File("src/main/res/drawable-nodpi/word_siege_home_badge.webp")

        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        assertTrue(manifest.contains("android:icon=\"@drawable/kelime_kusatmasi_app_icon\""))
        assertTrue(manifest.contains("android:roundIcon=\"@drawable/kelime_kusatmasi_app_icon\""))
        assertTrue("Approved launcher icon must exist", launcherIcon.isFile)
        assertTrue("Approved launcher icon must not be empty", launcherIcon.length() > 0L)
        assertTrue(launcherBadge.isFile)

        assertTrue(officialLogo.contains("painterResource(R.drawable.kelime_kusatma_logo_hd)"))
        assertTrue(authGate.contains("painterResource(R.drawable.son_harf_gold_teal_logo)"))
        assertTrue(siegeHub.contains("painterResource(R.drawable.kelime_kusatma_logo_hd)"))
        assertTrue(home.contains("R.drawable.son_harf_app_icon_master"))
        listOf(launcherVector, homeVector, authVector, gameVector).forEach {
            assertTrue("Missing compatibility vector drawable: ${it.path}", it.isFile)
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
