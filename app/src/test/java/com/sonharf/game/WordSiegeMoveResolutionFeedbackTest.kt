package com.sonharf.game

import com.sonharf.game.data.WordSiegeMoveDto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeMoveResolutionFeedbackTest {
    @Test
    fun moveFeedbackUsesCurrentTwoPointsPerCapturedCubeRule() {
        val move = WordSiegeMoveDto(
            id = 41L,
            gameId = "game",
            playerId = "player",
            primaryWord = "KALE",
            wordScore = 14,
            capturedCells = 3,
            neutralCaptured = 2,
            opponentCaptured = 1,
            areaScore = 999,
            totalScore = 999,
        )

        assertEquals(6, wordSiegeTerritoryGainPoints(move))
        assertEquals(20, wordSiegeMoveGainTotal(move))
        assertTrue(WORD_SIEGE_MOVE_FEEDBACK_TOTAL_MS in 1_000L..1_500L)
    }

    @Test
    fun feedbackStaysPresentationOnlyAndBoardOwnershipIsStaged() {
        val feedback = projectFile("app/src/main/java/com/sonharf/game/WordSiegeMoveResolutionFeedback.kt").readText()
        val match = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertTrue(feedback.contains("WordSiegeFinalRules.cubeTransfer(move.capturedCells)"))
        assertTrue(feedback.contains("ownershipChanges.sorted().forEach"))
        assertTrue(feedback.contains("presentedBoard = staged.toList()"))
        assertTrue(feedback.contains("KUŞATMA +$territoryPoints"))
        assertTrue(match.contains("WordSiegeMoveResolutionOverlay("))
        assertTrue(match.contains("presentedBoard.getOrElse(index)"))
        assertTrue(match.contains("wordSiegeTerritoryGainPoints(move)"))
        assertFalse(match.contains("Bölge +${move.areaScore}"))

        val lowered = feedback.lowercase()
        assertFalse(lowered.contains("onlinegamebackend"))
        assertFalse(lowered.contains("supabase"))
        assertFalse(lowered.contains("submitwordsiegemove"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
