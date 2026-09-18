package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiPrimaryProductContractTest {
    @Test
    fun kelimeKusatmasiIsFlagshipAndTopLevelNavigationMatchesMasterGdd() {
        val shell = File("src/main/java/com/sonharf/game/PremiumCanvaApp.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()
        val logo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeVector = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")
        val homeBadge = File("src/main/res/drawable-nodpi/word_siege_home_badge.webp")

        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        // Resource/deep-link identifiers remain stable even though the visible product brand changes.
        assertTrue(manifest.contains("@mipmap/ic_kelime_tahti"))
        assertTrue(strings.contains("<string name=\"app_name\">Kelime Kuşatması</string>"))
        assertTrue(logo.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(siegeVector.isFile)
        assertTrue(siegeVector.readText().contains("<vector"))
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.png").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.webp").exists())
        assertTrue(localization.contains("replace(\"Kelime Tahtı\", \"Kelime Kuşatması\")"))
        assertFalse(localization.contains("replace(\"Kelime Kuşatması\", \"Kelime Tahtı\")"))
        assertFalse(localization.contains("replace(\"Word Siege\", \"Word Throne\")"))

        assertTrue(home.contains("\"KELİME KUŞATMASI\"") || home.contains("\"KELİME\\nKUŞATMASI\""))
        assertTrue(home.contains("R.drawable.word_siege_home_badge"))
        assertTrue(homeBadge.isFile)
        assertTrue(home.contains("onClick = onSiege"))
        assertTrue(home.contains("sh(\"HEMEN OYNA\", \"PLAY NOW\")"))
        assertTrue(home.contains("sh(\"GÜNLÜK GÖREVLER\", \"DAILY TASKS\")"))
        assertTrue(home.contains("getMetaProgressV2().dailyPlayStreak"))
        assertTrue(home.contains("getWeeklyTopV210(limit = 3)"))
        assertTrue(home.contains("sh(\"HAFTANIN İLK 3 OYUNCUSU\", \"WEEKLY TOP 3\")"))
        assertTrue(home.contains("sh(\"HARF YOLU\", \"LETTER PATH\")"))
        assertFalse(home.contains("sh(\"KELİME YOLU\", \"WORD PATH\")"))
        assertTrue(shell.contains("PremiumCanvaBottomBar("))
        assertTrue(shell.contains("PremiumCanvaSecondaryModes(onLastLetter, onLetterPath)"))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(shell.contains("title = \"KELİME KUŞATMASI\""))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))

        assertTrue(shell.contains("PremiumCanvaDestination.HOME"))
        assertTrue(shell.contains("PremiumCanvaDestination.GAMES"))
        assertTrue(shell.contains("PremiumCanvaDestination.COMPETE"))
        assertTrue(shell.contains("PremiumCanvaDestination.SOCIAL"))
        assertTrue(shell.contains("PremiumCanvaDestination.PROFILE"))
        assertFalse(shell.contains("PremiumCanvaDestination.LEAGUE"))
        assertFalse(shell.contains("PremiumCanvaDestination.COMPETITION"))
        assertTrue(shell.contains("PremiumCanvaDestination.SHOP -> EconomyShopScreen"))

        val topLevel = shell.substringAfter("val topLevel =").substringBefore("val scheme =")
        assertTrue(topLevel.contains("PremiumCanvaDestination.HOME"))
        assertFalse("Club is retired and must not appear in the top-level bar", topLevel.contains("PremiumCanvaDestination.CLUB"))
        assertTrue(topLevel.contains("PremiumCanvaDestination.SOCIAL"))
        assertTrue(topLevel.contains("PremiumCanvaDestination.GAMES"))
        assertTrue(topLevel.contains("PremiumCanvaDestination.SHOP"))
        assertTrue(topLevel.contains("PremiumCanvaDestination.PROFILE"))
        assertFalse(topLevel.contains("PremiumCanvaDestination.COMPETE"))
        assertTrue(shell.contains("sh(\"ANA SAYFA\", \"HOME\")"))
        assertTrue(shell.contains("sh(\"SOSYAL\", \"SOCIAL\")"))
        assertTrue(shell.contains("sh(\"OYNA\", \"PLAY\")"))
        assertTrue(shell.contains("sh(\"MAĞAZA\", \"STORE\")"))
        assertTrue(shell.contains("sh(\"PROFİL\", \"PROFILE\")"))
        assertFalse("Bottom navigation no longer surfaces the club entry", shell.contains("sh(\"KULÜP\", \"CLUB\")"))
        assertFalse(shell.contains("PremiumCanvaDestination.TASKS"))
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
