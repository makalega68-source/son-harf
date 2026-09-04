package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumDuelArenaContractTest {

    @Test
    fun activeRouteUsesPremiumArenaWhileLatestFallbackRemainsAvailable() {
        val online = source("OnlineGameScreenV6.kt")
        val premium = source("PremiumDuelArena.kt")
        val legacy = source("LightDuelUi.kt")

        assertTrue(online.contains("PremiumDuelArena("))
        assertTrue(premium.contains("internal fun PremiumDuelArena("))
        assertTrue(legacy.contains("internal fun LightDuelArena("))
    }

    @Test
    fun hudKeepsMenuAtTheTopAndChatVisibleInCompetitionRail() {
        val premium = source("PremiumDuelArena.kt")

        assertTrue(premium.indexOf("PremiumDuelTopBar(") < premium.indexOf("PremiumDuelHud("))
        assertTrue(premium.contains("Icons.Rounded.MoreVert"))
        assertTrue(premium.contains("PremiumCompetitionRail("))
        assertTrue(premium.contains("Icons.Rounded.ChatBubbleOutline"))
        assertTrue(premium.contains("SOHBET"))
        assertTrue(premium.contains("unreadChatCount"))
    }

    @Test
    fun transientLeadAndErrorMessagesArePlainTextWithoutAContainer() {
        val premium = source("PremiumDuelArena.kt")
        val messageBlock = premium.substringAfter("private fun DuelTransientMessage(")
            .substringBefore("private fun DuelBalanceBar(")
        val sharedChrome = source("CompetitiveGameChrome.kt")
        val sharedAnnouncement = sharedChrome.substringAfter("announcement?.let { current ->")
            .substringBefore("@Composable\ninternal fun CompetitionMatchIntro")

        assertTrue(messageBlock.contains("Text("))
        assertFalse(messageBlock.contains("Surface("))
        assertFalse(messageBlock.contains(".background("))
        assertTrue(sharedAnnouncement.contains("Text("))
        assertFalse(sharedAnnouncement.contains("Surface("))
    }

    @Test
    fun androidGameKeyboardUsesTheApprovedTurkishQRowsAndOneSubmitAction() {
        val premium = source("PremiumDuelArena.kt")
        val keyboard = premium.substringAfter("private fun PremiumAndroidGameKeyboard(")

        assertTrue(keyboard.contains("listOf(\"Q\", \"W\", \"E\", \"R\", \"T\", \"Y\", \"U\", \"I\", \"O\", \"P\", \"Ğ\", \"Ü\")"))
        assertTrue(keyboard.contains("listOf(\"A\", \"S\", \"D\", \"F\", \"G\", \"H\", \"J\", \"K\", \"L\", \"Ş\", \"İ\")"))
        assertTrue(keyboard.contains("listOf(\"Z\", \"X\", \"C\", \"V\", \"B\", \"N\", \"M\", \"Ö\", \"Ç\")"))
        assertTrue(keyboard.contains("sh(\"SİL\", \"DELETE\")"))
        assertTrue(keyboard.contains("sh(\"TEMİZLE\", \"CLEAR\")"))
        assertTrue(keyboard.contains("sh(\"GÖNDER\", \"SEND\")"))
        assertFalse(keyboard.contains("⌫"))
        assertFalse(keyboard.contains("LocalSoftwareKeyboardController"))
    }

    @Test
    fun matchWordHistoryIsFairAndNeverTruncatesTheLiveChain() {
        val premium = source("PremiumDuelArena.kt")
        val history = premium.substringAfter("private fun PremiumMatchWordHistory(")
            .substringBefore("private fun PremiumWordInput(")

        assertTrue(history.contains("items(items = words, key = { it.id })"))
        assertTrue(history.contains("listState.animateScrollToItem(words.lastIndex)"))
        assertFalse(history.contains("takeLast("))
        assertFalse(history.contains("isVip"))
    }

    private fun source(name: String): String =
        projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
