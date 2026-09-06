package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.WordSiegeCellDto
import com.sonharf.game.data.WordSiegeMoveDto
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class WordSiegeFinalRulesTest {
    @Before fun installCanonicalDictionaryFixture() {
        SharedDictionaryService.installSnapshotForTests(
            "tr",
            listOf("AR", "AL", "EL", "ARA", "KARA", "KAT", "KALEM", "MAKALE", "MASA", "KAR", "MAL", "SEMA", "TER"),
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
        assertEquals(4, move.capturedCells)
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
            // 39 is column 9 on a 15x15 board, so 24 is the directly adjacent cell above it.
            this[24] = WordSiegeCellDto(letter = "M", owner = 2)
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

    @Test fun cubeTransferIsTwoPointsEachAndWordScoreNeverDropsForRivalNeutralCubes() {
        assertEquals(2, WordSiegeFinalRules.cubeTransfer(1))
        assertEquals(8, WordSiegeFinalRules.cubeTransfer(4))
        assertEquals(42, WordSiegeFinalRules.netScore(wordScore = 34, earnedCubePoints = 8, opponentEarnedCubePoints = 0))
        assertEquals(31, WordSiegeFinalRules.netScore(wordScore = 31, earnedCubePoints = 0, opponentEarnedCubePoints = 8))

        val first = WordSiegeMoveDto(
            id = 1,
            gameId = "game",
            playerId = "me",
            primaryWord = "KARA",
            neutralCaptured = 3,
        )
        val rivalNeutral = WordSiegeMoveDto(
            id = 2,
            gameId = "game",
            playerId = "rival",
            primaryWord = "MASA",
            neutralCaptured = 4,
        )
        val rivalTakesMine = WordSiegeMoveDto(
            id = 3,
            gameId = "game",
            playerId = "rival",
            primaryWord = "KALEM",
            opponentCaptured = 1,
        )
        val iTakeItBack = WordSiegeMoveDto(
            id = 4,
            gameId = "game",
            playerId = "me",
            primaryWord = "KAT",
            opponentCaptured = 1,
        )

        assertEquals(6, WordSiegeFinalRules.earnedCubePoints(listOf(first), "me"))
        assertEquals(6, WordSiegeFinalRules.earnedCubePoints(listOf(first, rivalNeutral), "me"))
        assertEquals(4, WordSiegeFinalRules.earnedCubePoints(listOf(first, rivalNeutral, rivalTakesMine), "me"))
        assertEquals(6, WordSiegeFinalRules.earnedCubePoints(listOf(first, rivalNeutral, rivalTakesMine, iTakeItBack), "me"))
        assertEquals(36, WordSiegeFinalRules.netScore(30, 6, 8))
        assertEquals(34, WordSiegeFinalRules.netScore(30, 4, 10))
        assertEquals(36, WordSiegeFinalRules.netScore(30, 6, 12))
    }

    @Test fun orientationIsDetectedWithoutPlayerDirectionSelection() {
        val board = emptyBoard()
        assertEquals(
            WordSiegeOrientation.HORIZONTAL,
            WordSiegeFinalRules.detectOrientation(board, listOf(111, 112, 113)),
        )
        assertEquals(
            WordSiegeOrientation.VERTICAL,
            WordSiegeFinalRules.detectOrientation(board, listOf(97, 112, 127)),
        )
    }

    @Test fun botAndHumanUseSameApplyMoveValidationAndUiLocksFinalOwnershipColors() {
        val engine = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()
        val sharedDictionary = projectFile("app/src/main/java/com/sonharf/game/data/SharedDictionaryService.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val scoringV3 = projectFile("supabase/migrations/20260903161000_word_siege_current_territory_scoring_v3.sql").readText()
        val boardV4 = projectFile("supabase/migrations/20260903230500_word_siege_15x15_v4.sql").readText()

        assertTrue(engine.contains("applyMove(state, 2, placements)"))
        assertTrue(engine.contains("SharedDictionaryService.isValidWordBlocking"))
        assertTrue(engine.contains("SharedDictionaryService.practiceCandidates"))
        assertTrue(!engine.contains("practiceDictionary"))
        assertTrue(sharedDictionary.contains("get_dictionary_snapshot_v3"))
        assertTrue(sharedDictionary.contains("MIN_CANONICAL_LENGTH = 2"))
        assertTrue(!practice.contains("Yön otomatik algılanır"))
        assertTrue(!pan.contains("Yön otomatik algılanır"))
        assertTrue(practice.contains("Torba ${state.bag.length}"))
        assertTrue(pan.contains("Torba ${game.bag.length}"))
        assertTrue(experience.contains("WordSiegeFinalRules.detectOrientation"))
        assertTrue(pan.contains("0xFF35C878"))
        assertTrue(pan.contains("0xFFFF5F57"))
        assertTrue(experience.contains("0xFF35C878"))
        assertTrue(experience.contains("0xFFFF5F57"))
        assertTrue(practice.contains("animateIntAsState"))
        assertTrue(pan.contains("animateIntAsState"))
        assertFalse(practice.contains("delay(28)"))
        assertFalse(pan.contains("delay(28)"))
        assertTrue(scoringV3.contains("r.player_one_word_score + (r.player_one_area * 2)"))
        assertTrue(scoringV3.contains("r.player_two_word_score + (r.player_two_area * 2)"))
        assertFalse(scoringV3.contains("- r.player_two_area_score"))
        assertFalse(scoringV3.contains("- r.player_one_area_score"))
        assertTrue(boardV4.contains("generate_series(0, 224)"))
        assertTrue(boardV4.contains("(neutral_count + opponent_count) * 2"))
        assertTrue(boardV4.contains("Scoring semantics are unchanged: word points are permanent and each currently owned cube is worth 2."))
    }

    private fun state(board: List<WordSiegeCellDto>, rack: String) = WordSiegePracticeState(
        board = board,
        bag = "ELMALİSTEKARTONURBİLGİSAYAR",
        playerRack = rack,
        botRack = "MASASİN",
        currentOwner = 1,
    )

    private fun emptyBoard(): List<WordSiegeCellDto> =
        List(WordSiegeBoardSpec.CellCount) { WordSiegeCellDto() }

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
