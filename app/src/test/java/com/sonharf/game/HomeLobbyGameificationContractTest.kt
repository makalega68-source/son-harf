package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLobbyGameificationContractTest {
    private fun projectFile(pathFromApp: String): File {
        val direct = File(pathFromApp)
        if (direct.exists()) return direct
        val underApp = File("app", pathFromApp)
        if (underApp.exists()) return underApp
        error("Could not resolve project file: $pathFromApp from ${File(".").absolutePath}")
    }

    private fun source(path: String): String = projectFile(path).readText()

    @Test
    fun activeV1ShellUsesOneGlobalBannerAndExcludesGameplay() {
        val app = source("src/main/java/com/sonharf/game/MonsterExperienceApp.kt")
        assertTrue(app.contains("SonHarfTopAdBanner(visible = !isGameplay, isPremium = isPremium)"))
        assertTrue(app.contains("MonsterDestination.GAME, MonsterDestination.WORD_SIEGE, MonsterDestination.DAILY_CHALLENGE"))
        assertTrue(app.contains("isPremium = runCatching { backend.getProfile(id).isVip }"))
    }

    @Test
    fun homeLobbyIsGameFirstAndUsesLiveWeeklyData() {
        val app = source("src/main/java/com/sonharf/game/MonsterExperienceApp.kt")
        assertTrue(app.contains("Kelimeyi Sürdür, Rakibini Geç"))
        assertTrue(app.contains("ARENANI SEÇ"))
        assertFalse(app.contains("MonsterQuickCard("))
        assertTrue(app.contains("MonsterSiegeQuickCard(Modifier.fillMaxWidth(), onSiege)"))
        assertTrue(app.contains("SonHarfBrandLogo("))
        assertTrue(app.contains("R.drawable.kelime_kusatma_logo"))
        assertTrue(app.contains("KELİME\\nKUŞATMASI"))
        assertTrue(app.contains("REKABET MERKEZİ"))
        assertTrue(app.contains("MonsterCompetitionStrip(tournament, archRival, stats.onlineFriends"))
        assertTrue(app.contains("getGrowthDashboard()"))
        assertTrue(app.contains("getCompetitiveSeason()"))
        assertTrue(app.contains("getAcceptedFriendProfiles()"))
        assertTrue(app.contains("getArchRival()"))
        assertTrue(app.contains("getWeeklyTournament()"))
        assertTrue(app.contains("HAFTANIN EN İYİLERİ"))
        assertTrue(app.contains("getLeaderboardV2(SonHarfUiState.language, \"week\", 3)"))
        assertTrue(app.contains("MonsterWeeklyTopThree(weeklyTop, onLeague)"))
        assertTrue(app.contains("BUGÜNKÜ HEDEF"))
        assertTrue(app.contains("backend.getGoals()"))
        assertTrue(app.contains("SC \${profile?.diamonds ?: 0}"))
    }

    @Test
    fun tournamentAndSocialCardsOpenExistingRealDestinations() {
        val app = source("src/main/java/com/sonharf/game/MonsterExperienceApp.kt")
        assertTrue(app.contains("MonsterDestination.COMPETITION -> CompetitionHubScreen"))
        assertTrue(app.contains("MonsterDestination.SOCIAL -> MainSocialScreen"))
        assertTrue(app.contains("MonsterDestination.WORD_SIEGE -> WordSiegeExperienceScreen"))
    }

    @Test
    fun dashboardNoiseAndFakeZeroStatsAreRemovedFromHome() {
        val app = source("src/main/java/com/sonharf/game/MonsterExperienceApp.kt")
        val homeStart = app.indexOf("private fun MonsterHomeScreen(")
        val nextFunction = app.indexOf("private fun MonsterLiveMatchCard", homeStart)
        assertTrue(homeStart >= 0 && nextFunction > homeStart)
        val home = app.substring(homeStart, nextFunction)
        assertFalse(home.contains("OYUNCU MERKEZİ"))
        assertFalse(home.contains("MonsterStatCard("))
        assertFalse(home.contains("MonsterHubRow("))
        assertFalse(home.contains("VIP"))
    }

    @Test
    fun playButtonRemainsPrimaryAndTouchResponsive() {
        val app = source("src/main/java/com/sonharf/game/MonsterExperienceApp.kt")
        assertTrue(app.contains("modifier = Modifier.fillMaxWidth().height(64.dp)"))
        assertTrue(app.contains("pressedElevation = 1.dp"))
        assertTrue(app.contains("Text(sh(\"OYNA\", \"PLAY\"), fontWeight = FontWeight.Black, fontSize = 20.sp"))
    }

    @Test
    fun classicArenaKeepsCompactCardsAndReconnectStateVisible() {
        val arena = source("src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(arena.contains("Modifier.fillMaxWidth().height(110.dp)"))
        assertTrue(arena.contains("modifier = modifier.height(96.dp)"))
        assertTrue(arena.contains("heightIn(max = 220.dp)"))
        assertTrue(arena.contains("BAĞLANTI YENİDEN KURULUYOR"))
        assertTrue(arena.contains("timerSynchronizing && !quizActive && seconds <= 0"))
    }
}
