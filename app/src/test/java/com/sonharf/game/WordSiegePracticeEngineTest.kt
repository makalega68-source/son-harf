package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WordSiegePracticeEngineTest {
    @Before
    fun installDictionaryFixture() {
        SharedDictionaryService.installSnapshotForTests(
            "tr",
            listOf(
                "KAR", "MAL", "SEMA", "TER", "MASA", "KALEM", "KARA", "KALE", "ELMA", "SİMA",
                "İSİM", "LİMAN", "MİNİ", "SİNEK", "PARA", "SEL", "SER", "KAT", "MAKALE", "KART", "KARE",
                "KASA", "SIR", "SIRA", "ARA", "ARI", "TARİH", "NAR", "NİSAN", "TERİM", "METİN",
                "SİLİ", "LİSTE", "KİLİT", "KİRA", "KİRAZ", "KİTAP",
            ),
        )
    }

    @After
    fun clearDictionaryFixture() = SharedDictionaryService.clearForTests()

    @Test
    fun firstPracticeMoveCovers15x15CenterAndUsesBonus() {
        val state = WordSiegePracticeEngine.newGame(random = Random(1)).copy(playerRack = "KALEMTR")

        val (next, move) = WordSiegePracticeEngine.applyMove(
            state = state,
            owner = 1,
            placements = linkedMapOf(110 to 0, 111 to 1, 112 to 2, 113 to 3, 114 to 4),
            horizontal = true,
        )

        assertEquals("KALEM", move.primaryWord)
        assertEquals(12, move.wordScore)
        assertEquals(5, next.playerArea)
        assertEquals(2, next.currentOwner)
        assertTrue(next.board[WordSiegeBoardSpec.CenterIndex].bonusUsed)
    }

    @Test
    fun newGameHas225CellsAndFreshRackComesFromCanonicalBag() {
        val first = WordSiegePracticeEngine.newGame(random = Random(11))
        val second = WordSiegePracticeEngine.newGame(random = Random(29))

        assertEquals(225, first.board.size)
        assertEquals(7, first.playerRack.length)
        assertNotEquals(first.playerRack, second.playerRack)
        assertEquals(
            WordSiegeBoardSpec.canonicalBag("tr").toList().sorted(),
            (first.playerRack + first.botRack + first.bag).toList().sorted(),
        )
    }

    @Test
    fun botCanFindMoveAndCaptureAnExistingTile() {
        val initial = WordSiegePracticeEngine.newGame(random = Random(1)).copy(
            playerRack = "KALEMTR",
            botRack = "MASASİN",
        )
        val opened = WordSiegePracticeEngine.applyMove(
            initial,
            1,
            linkedMapOf(110 to 0, 111 to 1, 112 to 2, 113 to 3, 114 to 4),
            true,
        ).first
        val planned = WordSiegePracticeEngine.bestBotMove(opened)

        assertNotNull(planned)
        val botPlan = requireNotNull(planned)
        val (afterBot, botMove) = WordSiegePracticeEngine.applyMove(
            opened,
            2,
            botPlan.placements,
            botPlan.horizontal,
        )

        assertTrue(botMove.formedWords.isNotEmpty())
        assertTrue(afterBot.botArea > 0)
        assertTrue(afterBot.board.any { it.owner == 2 })
        assertEquals(1, afterBot.currentOwner)
    }

    @Test
    fun practiceDoesNotUseASeparateMiniDictionaryWhenCanonicalSnapshotIsMissing() {
        SharedDictionaryService.clearForTests()
        assertFalse(SharedDictionaryService.hasSnapshot("tr"))
        assertFalse(SharedDictionaryService.isValidWordBlocking("KALEM", "tr"))
        assertTrue(SharedDictionaryService.practiceCandidates("tr", "KALEMTR").isEmpty())
    }

    @Test
    fun canonicalSnapshotAcceptsSelAndSerLikeMainDictionary() {
        assertTrue(SharedDictionaryService.isValidWordBlocking("SEL", "tr"))
        assertTrue(SharedDictionaryService.isValidWordBlocking("SER", "tr"))
    }

    @Test
    fun consecutivePassesFinishPractice() {
        val first = WordSiegePracticeEngine.pass(WordSiegePracticeEngine.newGame(random = Random(1)), 1)
        val finished = WordSiegePracticeEngine.pass(first, 2)

        assertEquals("finished", finished.status)
        assertEquals("consecutive_passes", finished.lastAction)
    }

    @Test
    fun exchangeKeepsTileCountsAndChangesTurn() {
        val state = WordSiegePracticeEngine.newGame(random = Random(1))
        val next = WordSiegePracticeEngine.exchange(state, 1, setOf(0, 2))

        assertEquals(7, next.playerRack.length)
        assertEquals(state.bag.length, next.bag.length)
        assertEquals(2, next.currentOwner)
    }
}
