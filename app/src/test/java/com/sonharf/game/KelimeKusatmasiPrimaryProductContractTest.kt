package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiPrimaryProductContractTest {
    @Test
    fun kelimeKusatmasiIsFlagshipAndTopLevelNavigationMatchesMasterGdd() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()
        val logo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val appLogo = File("src/main/res/drawable-nodpi/app_logo.webp")
        val siegeVector = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")

        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        // Resource/deep-link identifiers remain stable even though the visible product brand changes.
        assertTrue(manifest.contains("@mipmap/ic_kelime_tahti"))
        assertTrue(strings.contains("<string name=\"app_name\">Kelime Kuşatması</string>"))
        assertTrue(logo.contains("R.drawable.app_logo"))
        assertTrue(logo.contains("contentScale = ContentScale.Fit"))
        assertTrue(appLogo.isFile)
        assertTrue(siegeVector.isFile)
        assertTrue(siegeVector.readText().contains("<vector"))
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.png").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.webp").exists())
        assertTrue(localization.contains("replace(\"Kelime Tahtı\", \"Kelime Kuşatması\")"))
        assertFalse(localization.contains("replace(\"Kelime Kuşatması\", \"Kelime Tahtı\")"))
        assertFalse(localization.contains("replace(\"Word Siege\", \"Word Throne\")"))

        assertTrue(home.contains("Text(\"KELİME KUŞATMASI\""))
        assertTrue(home.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(home.contains("Button(onClick = onSiege"))
        assertTrue(home.contains("sh(\"SAVAŞA GİR\", \"ENTER BATTLE\")"))
        assertTrue(shell.contains("PremiumBottomBar("))
        assertTrue(shell.contains("PremiumOtherGames(onLastLetter = onLastLetter, onLetterPath = onLetterPath)"))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(shell.contains("title = sh(\"KELİME KUŞATMASI\", \"KELİME KUŞATMASI\")"))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))

        assertTrue(shell.contains("PremiumDestination.HOME"))
        assertTrue(shell.contains("PremiumDestination.GAMES"))
        assertTrue(shell.contains("PremiumDestination.COMPETE"))
        assertTrue(shell.contains("PremiumDestination.SOCIAL"))
        assertTrue(shell.contains("PremiumDestination.PROFILE"))
        assertFalse(shell.contains("PremiumDestination.LEAGUE"))
        assertFalse(shell.contains("PremiumDestination.COMPETITION"))
        assertTrue(shell.contains("PremiumDestination.SHOP -> EconomyShopScreen"))

        val topLevel = shell.substringAfter("val topLevel =").substringBefore("val scheme =")
        assertTrue(topLevel.contains("PremiumDestination.HOME"))
        assertFalse("Club is retired and must not appear in the top-level bar", topLevel.contains("PremiumDestination.CLUB"))
        assertTrue(topLevel.contains("PremiumDestination.SOCIAL"))
        assertTrue(topLevel.contains("PremiumDestination.SHOP"))
        assertTrue(topLevel.contains("PremiumDestination.PROFILE"))
        assertFalse(topLevel.contains("PremiumDestination.GAMES"))
        assertFalse(topLevel.contains("PremiumDestination.COMPETE"))
        assertTrue(shell.contains("sh(\"ANA SAYFA\", \"HOME\")"))
        assertFalse("Bottom navigation no longer surfaces the club entry", shell.contains("sh(\"KULÜP\", \"CLUB\")"))
        assertTrue(shell.contains("sh(\"ARKADAŞLAR\", \"FRIENDS\")"))
        assertTrue(shell.contains("sh(\"MAĞAZA\", \"STORE\")"))
        assertTrue(shell.contains("sh(\"PROFİL\", \"PROFILE\")"))
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
}
