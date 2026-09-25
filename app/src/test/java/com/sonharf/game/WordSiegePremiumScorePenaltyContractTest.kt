package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePremiumScorePenaltyContractTest {
    @Test fun requestedSiegePresentationAndPenaltyAreLocked() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val practiceBoard = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val migration = projectFile("supabase/migrations/20260923131500_word_siege_capture_loss_penalty_v8.sql").readText()

        assertTrue(ui.contains("size = 52.dp"))
        assertTrue(ui.contains("modifier = modifier.height(92.dp)"))
        assertFalse(practice.contains("WordSiegeOwnershipLegend()"))
        assertFalse(pan.contains("WordSiegeOwnershipLegend()"))
        assertTrue(practice.contains("mutableIntStateOf(-1)"))
        assertTrue(practiceBoard.contains("Color.White.copy(alpha = .95f)"))
        assertTrue(pan.contains("Color.White.copy(alpha = .95f)"))
        assertTrue(practice.contains("pendingBotLossPoints"))
        assertTrue(pan.contains("pendingRivalLossPoints"))
        assertTrue(practice.contains("WordSiegeTurnStrip("))
        assertTrue(pan.contains("WordSiegeTurnStrip("))
        assertTrue(practice.contains("WordSiegePracticeBagButton("))
        assertTrue(pan.contains("WordSiegeOnlineBagButton("))
        assertTrue(migration.contains("-v_opponent_captured"))
    }

    private fun projectFile(path: String): File = listOf(File(path), File("../$path"))
        .firstOrNull(File::exists) ?: error("Project path missing: $path")
}
