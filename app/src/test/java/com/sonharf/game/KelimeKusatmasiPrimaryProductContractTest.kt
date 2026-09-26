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
        val siegeVector = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")
        val homeBadge = File("src/main/res/drawable/word_siege_home_badge.xml")

        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        // Resource/deep-link identifiers remain stable even though the visible product brand changes.
        assertTrue(manifest.contains("@mipmap/ic_kelime_tahti"))
        assertTrue(strings.contains("<string name=\"app_name\">Kelime Tahtı</string>"))
        assertTrue(logo.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(siegeVector.isFile)
        assertTrue(siegeVector.readText().contains("<vector"))
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.png").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.webp").exists())
        assertTrue(localization.contains("replace(\"Kelime Kuşatması\", \"Kelime Tahtı\")"))
        assertFalse(localization.contains("replace(\"Kelime Tahtı\", \"Kelime Kuşatması\")"))
        assertTrue(localization.contains("replace(\"Word Siege\", \"Word Throne\")"))

        assertTrue(
            home.contains("\"KELİME TAHTI\"") ||
                home.contains("\"KELİME\\nKUŞATMASI\"")
        )
        assertTrue(home.contains("R.drawable.kelime_tahti_brand_logo"))
        assertTrue(homeBadge.isFile)
        assertTrue(homeBadge.readText().contains("<vector"))
        assertFalse(File("src/main/res/drawable-nodpi/word_siege_home_badge.webp").exists())
        assertTrue(home.contains("onClick = onSiege"))
        assertTrue(home.contains("sh(\"OYNA\", \"PLAY\")"))
        assertTrue(home.contains("sh(\"Günlük Görevler\", \"Daily Tasks\")"))
        assertTrue(home.contains("getMetaProgressV2().dailyPlayStreak"))
        assertTrue(home.contains("getWeeklyTopV210(limit = 3)"))
        assertTrue(home.contains("sh(\"HAFTANIN İLK 3'Ü\", \"WEEKLY TOP 3\")"))
        assertTrue(home.contains("sh(\"Kelime Atölyesi\", \"Word Workshop\")"))
        assertTrue(shell.contains("PremiumBottomBar("))
        assertTrue(shell.contains("PremiumOtherGames(onLastLetter = onLastLetter, onWorkshop = onWorkshop)"))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(shell.contains("title = sh(\"KELİME TAHTI\", \"KELİME TAHTI\")"))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"KELİME ATÖLYESİ\", \"WORD WORKSHOP\")"))

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
        assertTrue(topLevel.contains("PremiumDestination.COMPETE"))
        assertTrue(shell.contains("sh(\"Ana Sayfa\", \"Home\")"))
        assertFalse("Bottom navigation no longer surfaces the club entry", shell.contains("sh(\"KULÜP\", \"CLUB\")"))
        assertTrue(shell.contains("sh(\"Arkadaşlar\", \"Friends\")"))
        assertTrue(shell.contains("sh(\"Mağaza\", \"Store\")"))
        assertTrue(shell.contains("sh(\"Profil\", \"Profile\")"))
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
