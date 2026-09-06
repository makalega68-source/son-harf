package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefinedDuelOverlayContractTest {
    private val source by lazy { projectFile("app/src/main/java/com/sonharf/game/RefinedDuelOverlay.kt").readText() }

    @Test fun topControlsAndHierarchyStayMinimal() {
        assertTrue(source.contains("Pes Et"))
        assertTrue(source.contains("Sohbet"))
        assertTrue(source.contains("SON KELİME"))
        assertTrue(source.contains("BU HARFLE BAŞLA"))
        assertTrue(source.contains("fontSize = 70.sp"))
        assertTrue(source.contains("TurnStatusBar("))
        assertTrue(source.contains("PlayedWordsStrip("))
        assertFalse(source.contains("BONUS"))
        assertFalse(source.contains("TEMİZLE"))
    }

    @Test fun androidTurkishKeyboardKeepsExpectedRowsAndDeletePlacement() {
        assertTrue(source.contains("listOf(\"Q\", \"W\", \"E\", \"R\", \"T\", \"Y\", \"U\", \"I\", \"O\", \"P\", \"Ğ\", \"Ü\")"))
        assertTrue(source.contains("listOf(\"A\", \"S\", \"D\", \"F\", \"G\", \"H\", \"J\", \"K\", \"L\", \"Ş\", \"İ\")"))
        assertTrue(source.contains("listOf(\"Z\", \"X\", \"C\", \"V\", \"B\", \"N\", \"M\", \"Ö\", \"Ç\")"))
        assertTrue(source.contains("DuelKeyButton(\"⌫\""))
        assertTrue(source.contains("Modifier.fillMaxWidth().height(45.dp)"))
        assertTrue(source.contains("GÖNDER"))
    }

    @Test fun feedbackAndSyncUseServerAuthority() {
        assertTrue(source.contains("shouldAcceptClassicSnapshot"))
        assertTrue(source.contains("classicDeadlineEventKey"))
        assertTrue(source.contains("backend.claimTurnTimeout"))
        assertTrue(source.contains("deadlineExpired(active.turnDeadline)"))
        assertTrue(source.contains("backend.submitWord(active.id, submitted)"))
        assertTrue(source.contains("backend.submitVoiceWord(active.id, submitted, voiceToken)"))
        assertTrue(source.contains("withTimeout(DuelSubmitTimeoutMs)"))
        assertTrue(source.contains("finally {\n                busy = false"))
        assertTrue(source.contains("gameUppercase(it, active.language)"))
        assertFalse(source.contains("uppercaseChar()"))
        assertTrue(source.contains("✓ +"))
        assertTrue(source.contains("✕ -5"))
        assertFalse(source.contains("SENKRONİZE EDİLİYOR"))
    }

    @Test fun playedWordHistoryDoesNotCreateVipGameplayAdvantage() {
        assertTrue(source.contains("val recent = words.takeLast(24)"))
        assertTrue(source.contains("PlayedWordsStrip(words = words, isVip = myProfile?.isVip == true"))
        assertTrue(source.contains("if (isVip) \"VIP\" else sh(\"KELİMELER\", \"WORDS\")"))
        assertFalse(source.contains("if (!isVip) return"))
    }

    @Test fun turnAndVoiceStatesRemainVisibleAndBounded() {
        assertTrue(source.contains("SIRA SENDE"))
        assertTrue(source.contains("RAKİBİN SIRASI"))
        assertTrue(source.contains("BOT DÜŞÜNÜYOR"))
        assertTrue(source.contains("voiceUses >= 5"))
        assertTrue(source.contains("backend.getVoiceUses(active.id)"))
        assertTrue(source.contains("(5 - voiceUses).coerceAtLeast(0)"))
    }

    @Test fun actionPackageIsVisualAndIntegrated() {
        assertTrue(source.contains("LİDERLİK SENDE"))
        assertTrue(source.contains("KELİME FIRTINASI"))
        assertTrue(source.contains("SERİ x"))
        assertTrue(source.contains("UZUN KELİME"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
