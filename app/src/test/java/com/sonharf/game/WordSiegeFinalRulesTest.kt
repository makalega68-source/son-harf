package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.WordSiegeCellDto
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class WordSiegeFinalRulesTest {
    @Before fun installCanonicalDictionaryFixture() {
        SharedDictionaryService.installSnapshotForTests(
            "tr",
            listOf("ARA", "KARA", "KAT", "KALEM", "MAKALE", "MASA", "KAR", "MAL", "SEMA", "TER"),
        )
    }

    @After fun clearCanonicalDictionaryFixture() = SharedDictionaryService.clearForTests()

    @Test fun araPlusKBecomesKaraAndIsAccepted() {
        val board = emptyBoard().toMutableList().apply {
            this[40] = WordSiegeCellDto(letter = "A", owner = 2)
            this[41] = WordSiegeCellDto(letter = "R", owner = 2)
            this[42] = WordSiegeCellDto(letter = "A", owner = 2)
        }
        val state = state(board, rack = "KXXXXXX")

        val (next, move) = WordSiegePracticeEngine.applyMove(state, 1, mapOf(39 to 0))

        assertEquals("KARA", move.primaryWord)
        assertEquals(listOf("KARA"), move.formedWords)
        assertEquals(4, move.capturedCells) // K cube + three rival cubes are gained.
        assertEquals(8, next.playerAreaScore)
        assertEquals(1, next.board[39].owner)
        assertEquals(1, next.board[40].owner)
        assertEquals(2, next.currentOwner)
    }

    @Test fun makalePlusBReadsWholeContiguousWordAndRejectsMakaleb() {
        val board = emptyBoard().toMutableList()
        "MAKALE".forEachIndexed { offset, letter ->
            board[36 + offset] = WordSiegeCellDto(letter = letter.toString(), owner = 2)
        }
        val before = state(board, rack = "BXXXXXX")

        val error = expectPracticeError {
            WordSiegePracticeEngine.applyMove(before, 1, mapOf(42 to 0))
        }
        assertEquals("word_siege_invalid_word:MAKALEB", error.code)
        assertEquals(0, before.playerWordScore)
        assertEquals(0, before.playerAreaScore)
        assertEquals(1, before.currentOwner)
        assertEquals(2, before.board[36].owner)
    }

    @Test fun invalidSideWordRejectsEntireMoveAtomically() {
        val board = emptyBoard().toMutableList().apply {
            this[40] = WordSiegeCellDto(letter = "A", owner = 2)
            this[41] = WordSiegeCellDto(letter = "R", owner = 2)
            this[42] = WordSiegeCellDto(letter = "A", owner = 2)
            this[30] = WordSiegeCellDto(letter = "M", owner = 2) // Placing K at 39 also forms MK.
        }
        val before = state(board, rack = "KXXXXXX")

        val error = expectPracticeError {
            WordSiegePracticeEngine.applyMove(before, 1, mapOf(39 to 0))
        }
        assertEquals("word_siege_invalid_word:MK", error.code)
        assertEquals(0, before.playerWordScore)
        assertEquals(0, before.playerAreaScore)
        assertEquals(1, before.currentOwner)
        assertEquals(0, before.board[39].owner)
        assertEquals(null, before.board[39].letter)
    }

    @Test fun disconnectedWordRejectsAndConnectedWordAccepts() {
        val occupied = emptyBoard().toMutableList().apply {
            this[40] = WordSiegeCellDto(letter = "A", owner = 2)
            this[41] = WordSiegeCellDto(letter = "R", owner = 2)
            this[42] = WordSiegeCellDto(letter = "A", owner = 2)
        }
        val disconnected = state(occupied, rack = "KATXXXX")
        val error = expectPracticeError {
            WordSiegePracticeEngine.applyMove(disconnected, 1, mapOf(0 to 0, 1 to 1, 2 to 2))
        }
        assertEquals("word_siege_move_must_connect", error.code)

        val (_, connectedMove) = WordSiegePracticeEngine.applyMove(disconnected, 1, mapOf(39 to 0))
        assertEquals("KARA", connectedMove.primaryWord)
    }

    @Test fun cubeTransferIsTwoPointsEachAndNetScoreTransfersFromRival() {
        assertEquals(2, WordSiegeFinalRules.cubeTransfer(1))
        assertEquals(8, WordSiegeFinalRules.cubeTransfer(4))
        assertEquals(42, WordSiegeFinalRules.netScore(wordScore = 34, earnedCubePoints = 8, opponentEarnedCubePoints = 0))
        assertEquals(23, WordSiegeFinalRules.netScore(wordScore = 31, earnedCubePoints = 0, opponentEarnedCubePoints = 8))
    }

    @Test fun orientationIsDetectedWithoutPlayerDirectionSelection() {
        val board = emptyBoard()
        assertEquals(WordSiegeOrientation.HORIZONTAL, WordSiegeFinalRules.detectOrientation(board, listOf(39, 40, 41)))
        assertEquals(WordSiegeOrientation.VERTICAL, WordSiegeFinalRules.detectOrientation(board, listOf(31, 40, 49)))
    }

    @Test fun botAndHumanUseSameApplyMoveValidationAndUiLocksFinalOwnershipColors() {
        val engine = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()
        val sharedDictionary = projectFile("app/src/main/java/com/sonharf/game/data/SharedDictionaryService.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val sql = projectFile("supabase/migrations/20260902090000_word_siege_final_transfer_v2.sql").readText()

        assertTrue(engine.contains("applyMove(state, 2, placements)"))
        assertTrue(engine.contains("SharedDictionaryService.isValidWordBlocking"))
        assertTrue(engine.contains("SharedDictionaryService.practiceCandidates"))
        assertTrue(!engine.contains("practiceDictionary"))
        assertTrue(sharedDictionary.contains("get_dictionary_snapshot_v1"))
        assertTrue(practice.contains("Yön otomatik algılanır"))
        assertTrue(pan.contains("Yön otomatik algılanır"))
        assertTrue(experience.contains("WordSiegeFinalRules.detectOrientation"))
        assertTrue(pan.contains("0xFF35C878"))
        assertTrue(pan.contains("0xFFFF5F57"))
        assertTrue(experience.contains("0xFF35C878"))
        assertTrue(experience.contains("0xFFFF5F57"))
        assertTrue(practice.contains("delay(28)"))
        assertTrue(pan.contains("delay(28)"))
        assertTrue(sql.contains("(neutral_count + opponent_count) * 2"))
        assertTrue(sql.contains("r.player_one_word_score + r.player_one_area_score - r.player_two_area_score"))
    }

    private fun state(board: List<WordSiegeCellDto>, rack: String) = WordSiegePracticeState(
        board = board,
        bag = "ELMALİSTEKARTONURBİLGİSAYAR",
        playerRack = rack,
        botRack = "MASASİN",
        currentOwner = 1,
    )

    private fun emptyBoard(): List<WordSiegeCellDto> = List(81) { WordSiegeCellDto() }

    private fun expectPracticeError(block: () -> Unit): WordSiegePracticeError {
        try {
            block()
        } catch (error: WordSiegePracticeError) {
            return error
        }
        fail("Expected WordSiegePracticeError")
        throw AssertionError("unreachable")
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
