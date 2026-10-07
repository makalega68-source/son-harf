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
        assertFalse(localization.contains("replace(\"Kelime Kuşatması\", \"Kelime Tahtı\")"))
        assertFalse(localization.contains("replace(\"Kelime Tahtı\", \"Kelime Kuşatması\")"))
        assertFalse(localization.contains("replace(\"Word Siege\", \"Word Throne\")"))

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
        assertTrue(home.contains("ThroneBackend.week().rows"))
        assertTrue(home.contains("GameWeeklyPodium(players)"))
        assertTrue(File("src/main/java/com/sonharf/game/ThroneExperience.kt").readText().contains("sh(\"Kelime Atölyesi\",\"Word Workshop\")"))
        // Lobby with a five-tab bar: Mağaza · Taht · Oyna · Arkadaşlar · Profil.
        assertFalse(shell.contains("PremiumBottomBar("))
        assertTrue(shell.contains("if (tab >= 0) LobbyBottomBar(selected = tab"))
        assertTrue(shell.contains("PremiumDestination.HOME -> HomeLobbyScreen("))
        assertTrue(shell.contains("onNewGame = { openGame(PremiumDestination.SIEGE, siegeLanguage) },"))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(shell.contains("title = sh(\"KELİME KUŞATMASI\", \"WORD SIEGE\")"))
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
        assertTrue(topLevel.contains("PremiumDestination.MY_GAMES"))
        assertTrue(topLevel.contains("PremiumDestination.SHOP"))
        assertTrue(topLevel.contains("PremiumDestination.PROFILE"))
        assertFalse(topLevel.contains("PremiumDestination.GAMES"))
        assertTrue(topLevel.contains("PremiumDestination.EVENTS"))
        assertFalse("Club never surfaces", shell.contains("sh(\"KULÜP\", \"CLUB\")"))
        val lobby = File("src/main/java/com/sonharf/game/HomeLobby.kt").readText()
        listOf("sh(\"Yeni Oyun\", \"New Game\")", "sh(\"Oyunlarım\", \"My Games\")", "R.drawable.kelime_tahti_brand_logo",
            "sh(\"Mağaza\", \"Store\")", "sh(\"Taht\", \"Throne\")", "sh(\"Oyna\", \"Play\")", "sh(\"Arkadaşlar\", \"Friends\")",
            "sh(\"Profil\", \"Profile\")", "rememberWorkshopStatus(), onWorkshop)", "sh(\"Son Harf\", \"Last Letter\")").forEach { assertTrue(it, lobby.contains(it)) }
        // Game lists live in Oyunlarım, not on the lobby.
        assertFalse(lobby.contains("SIRA SENDE"))
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
