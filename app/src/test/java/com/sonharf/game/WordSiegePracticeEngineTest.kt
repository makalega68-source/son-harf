package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import org.junit.After
import org.junit.Assert.assertEquals
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
                "İSİM", "LİMAN", "MİNİ", "SİNEK", "PARA", "SEL", "KAT", "MAKALE", "KART", "KARE",
                "KASA", "SIR", "SIRA", "ARA", "ARI", "TARİH", "NAR", "NİSAN", "TERİM", "METİN",
                "SİLİ", "LİSTE", "KİLİT", "KİRA", "KİRAZ", "KİTAP",
            ),
        )
    }

    @After
    fun clearDictionaryFixture() = SharedDictionaryService.clearForTests()

    @Test
    fun firstPracticeMoveCoversCenterAndUsesBonus() {
        val state = WordSiegePracticeEngine.newGame()

        val (next, move) = WordSiegePracticeEngine.applyMove(
            state = state,
            owner = 1,
            placements = linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4),
            horizontal = true,
        )

        assertEquals("KALEM", move.primaryWord)
        assertEquals(12, move.wordScore)
        assertEquals(5, next.playerArea)
        assertEquals(2, next.currentOwner)
        assertTrue(next.board[40].bonusUsed)
    }

    @Test
    fun botCanFindMoveAndCaptureAnExistingTile() {
        val opened = WordSiegePracticeEngine.applyMove(
            WordSiegePracticeEngine.newGame(),
            1,
            linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4),
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
    fun practiceStillAcceptsKalemWithoutAnyCachedServerSnapshot() {
        SharedDictionaryService.clearForTests()
        val state = WordSiegePracticeEngine.newGame()

        val (next, move) = WordSiegePracticeEngine.applyMove(
            state,
            1,
            linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4),
            true,
        )

        assertEquals("KALEM", move.primaryWord)
        assertEquals(2, next.currentOwner)
        assertTrue(SharedDictionaryService.practiceCandidates("tr", next.botRack).isNotEmpty())
    }

    @Test
    fun consecutivePassesFinishPractice() {
        val first = WordSiegePracticeEngine.pass(WordSiegePracticeEngine.newGame(), 1)
        val finished = WordSiegePracticeEngine.pass(first, 2)

        assertEquals("finished", finished.status)
        assertEquals("consecutive_passes", finished.lastAction)
    }

    @Test
    fun exchangeKeepsTileCountsAndChangesTurn() {
        val state = WordSiegePracticeEngine.newGame()
        val next = WordSiegePracticeEngine.exchange(state, 1, setOf(0, 2))

        assertEquals(7, next.playerRack.length)
        assertEquals(state.bag.length, next.bag.length)
        assertEquals(2, next.currentOwner)
    }
}
