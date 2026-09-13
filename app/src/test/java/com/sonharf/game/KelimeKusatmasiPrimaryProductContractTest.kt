package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeTahtiPrimaryProductContractTest {
    @Test
    fun kelimeTahtiIsFlagshipAndTopLevelNavigationStaysSimple() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()
        val logo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeRaster = File("src/main/res/drawable/kelime_kusatma_logo_hd.png")

        assertTrue(manifest.contains("android:label=\"Kelime Tahtı\""))
        assertTrue(manifest.contains("@mipmap/ic_kelime_tahti"))
        assertTrue(logo.contains("R.drawable.kelime_tahti_logo_latest"))
        assertTrue(siegeRaster.isFile)
        assertTrue(isPng(siegeRaster))
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.webp").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.xml").exists())
        assertFalse(localization.contains("replace(\"Kelime Kuşatması\", \"Kelime Tahtı\")"))
        assertFalse(localization.contains("replace(\"Word Siege\", \"Word Throne\")"))

        assertTrue(home.contains("Text(\"KELİME TAHTI\""))
        assertTrue(home.contains("Button(onClick = onSiege"))
        assertTrue(shell.contains("PremiumHomeCommandDeck(profile, onProfile, onPrimary, onShop, onSocial)"))
        assertTrue(shell.contains("backend.getLeaderboardV2(language, \"week\", 3)"))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(shell.contains("title = sh(\"KELİME KUŞATMASI\", \"WORD SIEGE\")"))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))

        assertTrue(shell.contains("PremiumDestination.HOME"))
        assertTrue(shell.contains("PremiumDestination.GAMES"))
        assertTrue(shell.contains("PremiumDestination.COMPETE"))
        assertTrue(shell.contains("PremiumDestination.PROFILE"))
        assertFalse(shell.contains("PremiumDestination.LEAGUE"))
        assertFalse(shell.contains("PremiumDestination.COMPETITION"))
        assertTrue(shell.contains("PremiumDestination.SHOP -> EconomyShopScreen"))
        val topLevel = shell.substringAfter("val topLevel =").substringBefore("val scheme =")
        assertFalse(topLevel.contains("PremiumDestination.SHOP"))
        assertFalse(shell.contains("PremiumDestination.TASKS"))
    }

    @Test
    fun technicalCompatibilityIdentifiersRemainStable() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:scheme=\"sonharf\""))
        assertTrue(manifest.contains("android:host=\"auth\""))
    }

    @Test
    fun siegeBoardNoLongerUsesTheClassicCornerAndDiagonalBonusTopology() {
        val spec = File("src/main/java/com/sonharf/game/WordSiegeBoardSpec.kt").readText()

        assertTrue(spec.contains("SiegeMajorZones"))
        assertTrue(spec.contains("SiegeWatchZones"))
        assertTrue(spec.contains("SiegeFortZones"))
        assertTrue(spec.contains("SiegeTacticalZones"))
        assertFalse(spec.contains("private val TripleWord"))
        assertFalse(spec.contains("private val DoubleWord"))
    }

    @Test
    fun matchScreenKeepsWordAndTerritoryScoresSeparateWithoutOldMapControlChrome() {
        val match = File("src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertTrue(match.contains("wordPoints = myWordPoints"))
        assertTrue(match.contains("territoryPoints = myTerritoryPoints"))
        assertFalse(match.contains("HARİTA KONTROLÜ"))
        assertTrue(match.contains("HAMLEYİ ONAYLA"))
    }

    private fun isPng(file: File): Boolean {
        val bytes = file.readBytes()
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        return bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(signature)
    }
}
