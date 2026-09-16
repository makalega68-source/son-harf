package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiVisualPlacementContractTest {
    @Test
    fun requestedBrandSurfacesUseApprovedAppLogoAndCompatibilityDrawables() {
        val source = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val appLogo = File("src/main/res/drawable-nodpi/app_logo.webp")
        val authAlias = File("src/main/res/drawable/son_harf_gold_teal_logo.xml")
        val gameEntryAlias = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")

        assertTrue(source.contains("R.drawable.app_logo"))
        assertTrue(source.contains("contentScale = ContentScale.Fit"))
        assertFalse(source.contains("R.drawable.kelime_tahti_app_icon"))
        assertTrue(appLogo.isFile)
        listOf(authAlias, gameEntryAlias).forEach {
            assertTrue(it.isFile)
            assertTrue(it.readText().contains("<vector"))
        }
        assertFalse(File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.png").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.png").exists())
    }

    @Test
    fun compatibilitySiegeLogoIdsRemainWithoutRasterFiles() {
        listOf(
            "src/main/res/drawable/kelime_kusatma_logo.xml",
            "src/main/res/drawable/kelime_kusatmasi_brand.xml",
        ).forEach { path ->
            val file = File(path)
            assertTrue(file.isFile)
            assertTrue(file.readText().contains("<vector"))
        }
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo.png").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatmasi_brand.png").exists())
    }

    @Test
    fun wordSiegeHubResourceResolvesToVectorAlias() {
        val screen = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        assertTrue(screen.contains("painterResource(R.drawable.kelime_kusatma_logo_hd)"))
        assertTrue(File("src/main/res/drawable/kelime_kusatma_logo_hd.xml").isFile)
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.png").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.webp").exists())
    }
}
