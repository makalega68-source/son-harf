package com.sonharf.game

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ClassicCompetitionArenaContractTest {
    private fun source(path: String): String {
        val direct = File("src/main/java/com/sonharf/game/$path")
        val fromRoot = File("app/src/main/java/com/sonharf/game/$path")
        return when {
            direct.exists() -> direct.readText()
            fromRoot.exists() -> fromRoot.readText()
            else -> error("Missing source $path")
        }
    }

    private fun liveArena(): String {
        val text = source("LightDuelUi.kt")
        return text.substring(text.indexOf("internal fun LightDuelArena"), text.indexOf("private fun CompetitiveResult"))
    }

    @Test fun arenaKeepsPrimaryCompetitiveHierarchyVisibleWithoutDuplicateChrome() {
        val text = liveArena()
        assertTrue(text.contains("SIRA SENDE"))
        assertTrue(text.contains("RAKİBİN SIRASI"))
        assertTrue(text.contains("KRİTİK"))
        assertTrue(text.contains("DOĞRU"))
        assertTrue(text.contains("YANLIŞ"))
        assertTrue(text.contains("ÖNE GEÇTİN"))
        assertTrue(text.contains("SKOR EŞİTLENDİ"))
        assertFalse(text.contains("SON KABUL EDİLEN"))
        assertFalse(text.contains("SIRADAKİ ZORUNLU HARF"))
        assertFalse(text.contains("↓"))
        assertFalse(text.contains("➤"))
    }

    @Test fun statusLineIsBackgroundlessAndUsesOnlyFade() {
        val text = liveArena()
        val start = text.indexOf("private fun DuelStatusLine")
        val end = text.indexOf("private fun DuelWordStage")
        assertTrue(start >= 0 && end > start)
        val section = text.substring(start, end)
        assertTrue(section.contains("AnimatedVisibility("))
        assertTrue(section.contains("fadeIn()"))
        assertTrue(section.contains("fadeOut()"))
        assertTrue(section.contains("liveRegion = LiveRegionMode.Polite"))
        assertFalse(section.contains("Surface("))
        assertFalse(section.contains("Card("))
        assertFalse(section.contains(".background("))
        assertFalse(section.contains(".border("))
        assertFalse(section.contains("shadowElevation"))
    }

    @Test fun urgencyRespectsUserSoundAndHapticPreferencesAndSignalsEachSecondOnce() {
        val text = liveArena()
        assertTrue(text.contains("SonHarfPreferences.soundEnabled(context)"))
        assertTrue(text.contains("SonHarfPreferences.vibrationEnabled(context)"))
        assertTrue(text.contains("SonHarfSoundFx.heartbeat()"))
        assertTrue(text.contains("SonHarfSoundFx.countdown()"))
        assertTrue(text.contains("shown != lastSignalledSecond"))
        assertTrue(text.contains("timeoutHandled"))
        assertTrue(text.contains("if (!timeoutHandled)"))
        assertTrue(text.contains("<= 10"))
    }

    @Test fun leaderNotificationsStartFromCurrentServerScoreAndOnlyFireOnTransition() {
        val text = liveArena()
        assertTrue(text.contains("previousLeader by remember(room.id) { mutableIntStateOf(ClassicCompetitionRules.leader(myScore, oppScore)) }"))
        assertTrue(text.contains("if (newLeader != previousLeader)"))
        assertTrue(text.contains("newLeader == 0 && previousLeader != 0"))
        assertTrue(text.contains("newLeader > 0"))
        assertTrue(text.contains("newLeader < 0"))
    }

    @Test fun submitIsSingleAndLocallyLatchedAgainstFastDoubleTap() {
        val text = liveArena()
        assertTrue(text.contains("submitLatched"))
        assertTrue(text.contains("!submitLatched && myTurn"))
        assertEquals(1, Regex("sh\\(\\\"GÖNDER\\\", \\\"SEND\\\"\\)").findAll(text).count())
    }

    @Test fun resultRatingIsRefetchedAndVipDetailRemainsPostMatchOnly() {
        val text = source("LightDuelUi.kt")
        val result = text.substring(text.indexOf("private fun CompetitiveResult"))
        assertTrue(result.contains("OnlineGameBackend().getProfile(me).rating"))
        assertTrue(result.contains("Rating sonucu sunucudan doğrulanıyor"))
        assertTrue(result.contains("if (isVip && words.isNotEmpty())"))
        assertFalse(liveArena().contains("if (isVip"))
    }

    @Test fun compactArenaTargetsSmallPortraitsWithoutImeDependency() {
        val text = liveArena()
        assertTrue(text.contains("BoxWithConstraints("))
        assertTrue(text.contains("maxHeight < 700.dp"))
        assertTrue(text.contains("WindowInsets.safeDrawing"))
        assertTrue(text.contains("DuelGameKeyboard("))
        assertFalse(text.contains("imePadding()"))
        assertFalse(text.contains("verticalScroll("))
        assertTrue(text.contains("height(46.dp)"))
        assertTrue(text.contains("height(48.dp)"))
    }

    @Test fun secondaryActionsAreInOneMaterialOverflowMenu() {
        val text = liveArena()
        assertTrue(text.contains("Icons.Rounded.MoreVert"))
        assertTrue(text.contains("SOHBET"))
        assertTrue(text.contains("BONUS"))
        assertTrue(text.contains("SESLİ GİRİŞ"))
        assertTrue(text.contains("PES ET"))
        assertTrue(text.contains("AlertDialog("))
    }
}
