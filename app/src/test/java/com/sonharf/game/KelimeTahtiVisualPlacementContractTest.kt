package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiVisualPlacementContractTest {
    @Test
    fun compatibilityBrandAssetsRemainRasterFree() {
        val source = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val authAlias = File("src/main/res/drawable/son_harf_gold_teal_logo.xml")
        val gameEntryAlias = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")

        assertTrue(source.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertFalse(source.contains("R.drawable.kelime_tahti_app_icon"))
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
    fun wordSiegeEntryAndLobbyUseNoLogoArtwork() {
        val screen = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()

        assertFalse(screen.contains("painterResource(R.drawable.kelime_kusatma_logo_hd)"))
        assertTrue(screen.contains("ALAN SAVAŞI"))
        assertTrue(screen.contains("Kelime kur • alan ele geçir"))
        assertTrue(screen.contains("Icons.Rounded.GridView"))
        assertTrue(screen.contains("onLanguageChange = { SonHarfUiState.language = it }"))
        assertFalse(File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").exists())
        assertTrue(File("src/main/res/drawable/kelime_kusatma_logo_hd.xml").isFile)
    }
}
