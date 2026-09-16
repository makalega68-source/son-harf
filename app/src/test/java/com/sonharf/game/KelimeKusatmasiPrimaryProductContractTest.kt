package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiPrimaryProductContractTest {
    @Test
    fun kelimeKusatmasiIsFlagshipAndTopLevelNavigationMatchesMasterGdd() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeImageLayout.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()
        val logo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeVector = File("src/main/res/drawable/kelime_kusatma_logo_hd.xml")

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

        assertTrue(home.contains("internal fun PremiumHomeProfileStrip("))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(home.contains("internal fun PremiumModeArtworkButton("))
        assertTrue(home.contains("internal fun PremiumHomeWeeklyTop3("))
        assertTrue(shell.contains("PremiumBottomBar("))
        assertTrue(shell.contains("drawable = R.drawable.mode_kelime_kusatmasi"))
        assertTrue(shell.contains("drawable = R.drawable.mode_son_harf"))
        assertTrue(shell.contains("drawable = R.drawable.mode_kelime_yolu"))
        assertTrue(shell.contains("PremiumDailyObjective(onClick = onTasks)"))
        assertTrue(shell.contains("PremiumHomeWeeklyTop3("))
        assertTrue(shell.contains("title = sh(\"KELİME KUŞATMASI\", \"KELİME KUŞATMASI\")"))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))

        assertTrue(shell.contains("PremiumDestination.HOME"))
        assertTrue(shell.contains("PremiumDestination.GAMES"))
        assertTrue(shell.contains("PremiumDestination.COMPETE"))
        assertTrue(shell.contains("PremiumDestination.SOCIAL"))
        assertTrue(shell.contains("PremiumDestination.PROFILE"))
        assertTrue(shell.contains("PremiumDestination.LEAGUE"))
        assertTrue(shell.contains("PremiumDestination.TASKS"))
        assertTrue(shell.contains("PremiumDestination.DAILY"))
        assertFalse(shell.contains("PremiumDestination.COMPETITION"))
        assertTrue(shell.contains("PremiumDestination.SHOP -> EconomyShopScreen"))
        assertTrue(shell.contains("PremiumDestination.LEAGUE -> LeaderboardExperienceScreen"))
        assertTrue(shell.contains("PremiumDestination.TASKS -> MainRetentionScreen"))

        val topLevel = shell.substringAfter("val topLevel =").substringBefore("val scheme =")
        assertTrue(topLevel.contains("PremiumDestination.HOME"))
        assertFalse("Club is retired and must not appear in the top-level bar", topLevel.contains("PremiumDestination.CLUB"))
        assertTrue(topLevel.contains("PremiumDestination.SOCIAL"))
        assertTrue(topLevel.contains("PremiumDestination.SHOP"))
        assertTrue(topLevel.contains("PremiumDestination.PROFILE"))
        assertFalse(topLevel.contains("PremiumDestination.GAMES"))
        assertFalse(topLevel.contains("PremiumDestination.COMPETE"))
        assertFalse(topLevel.contains("PremiumDestination.LEAGUE"))
        assertFalse(topLevel.contains("PremiumDestination.TASKS"))
        assertTrue(shell.contains("sh(\"ANA SAYFA\", \"HOME\")"))
        assertFalse("Bottom navigation no longer surfaces the club entry", shell.contains("sh(\"KULÜP\", \"CLUB\")"))
        assertTrue(shell.contains("sh(\"ARKADAŞLAR\", \"FRIENDS\")"))
        assertTrue(shell.contains("sh(\"MAĞAZA\", \"STORE\")"))
        assertTrue(shell.contains("sh(\"PROFİL\", \"PROFILE\")"))
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
