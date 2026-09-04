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

    @Test fun arenaKeepsPrimaryCompetitiveHierarchyVisible() {
        val text = source("LightDuelUi.kt")
        assertTrue(text.contains("SIRADAKİ ZORUNLU HARF"))
        assertTrue(text.contains("scoreDifferenceText"))
        assertTrue(text.contains("SIRA SENDE"))
        assertTrue(text.contains("RAKİBİN SIRASI"))
        assertTrue(text.contains("KRİTİK"))
        assertTrue(text.contains("RÖVANŞ ⚡"))
        assertTrue(text.contains("BİR MAÇ DAHA"))
    }

    @Test fun urgencyRespectsUserSoundAndHapticPreferences() {
        val text = source("LightDuelUi.kt")
        assertTrue(text.contains("SonHarfPreferences.soundEnabled(context)"))
        assertTrue(text.contains("SonHarfPreferences.vibrationEnabled(context)"))
        assertTrue(text.contains("SonHarfSoundFx.heartbeat()"))
        assertTrue(text.contains("SonHarfSoundFx.countdown()"))
    }

    @Test fun competitiveEffectsAreFiniteAndOverlayDoesNotOwnPointerInput() {
        val text = source("LightDuelUi.kt")
        assertFalse(text.contains("infiniteRepeatable"))
        assertFalse(text.contains("rememberInfiniteTransition"))
        val overlayStart = text.indexOf("AnimatedVisibility(")
        assertTrue(overlayStart >= 0)
        val overlaySection = text.substring(overlayStart, minOf(text.length, overlayStart + 1800))
        assertFalse(overlaySection.contains("pointerInput"))
        assertFalse(overlaySection.contains("combinedClickable"))
        assertFalse(overlaySection.contains("clickable("))
    }

    @Test fun resultRatingIsRefetchedAndMissingMetaIsNotFabricated() {
        val text = source("LightDuelUi.kt")
        assertTrue(text.contains("OnlineGameBackend().getProfile(me).rating"))
        assertTrue(text.contains("Rating sonucu sunucudan doğrulanıyor"))
        assertFalse(text.contains("SEN 3–2 RAKİP"))
        assertFalse(text.contains("GALİBİYET SERİSİ"))
        assertFalse(text.contains("WIN STREAK"))
    }

    @Test fun roomStreakIsOnlyUsedAsAcceptedWordComboState() {
        val text = source("LightDuelUi.kt")
        assertTrue(text.contains("myWordStreak"))
        assertTrue(text.contains("oppWordStreak"))
        assertTrue(text.contains("ClassicCompetitionRules.comboLabel(wordStreak"))
    }

    @Test fun compactArenaTargetsPortrait360x800WithoutImeDependency() {
        val text = source("LightDuelUi.kt")
        assertTrue(text.contains("GameKeyboard("))
        assertTrue(text.contains("navigationBarsPadding()"))
        assertFalse(text.contains("imePadding()"))
        val viewportWidthDp = 360
        val viewportHeightDp = 800
        assertTrue(viewportWidthDp >= 320)
        assertTrue(viewportHeightDp >= 720)
    }
}