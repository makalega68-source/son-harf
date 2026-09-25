package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeReadabilityV35ContractTest {
    private fun read(path: String) = File(path).readText()

    @Test fun remainingLettersUseRoomyPremiumCells() {
        val bag = read("src/main/java/com/sonharf/game/WordSiegeCompactBag.kt")
        assertTrue(bag.contains("rows.chunked(5)"))
        assertTrue(bag.contains("Modifier.weight(1f).height(58.dp)"))
        assertTrue(bag.contains("fontSize = 18.sp"))
        assertTrue(bag.contains("Torbada \$total harf"))
        assertTrue(bag.contains("×\$count"))
    }

    @Test fun practiceChatKeepsAndScrollsFullHistory() {
        val screen = read("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")
        assertTrue(screen.contains("LazyColumn("))
        assertTrue(screen.contains("rememberLazyListState()"))
        assertTrue(screen.contains("itemsIndexed(chatMessages.asReversed())"))
        assertTrue(screen.contains("reverseLayout = true"))
        assertFalse(screen.contains("chatMessages.takeLast(5)"))
        assertFalse(screen.contains("takeLast(8)"))
    }

    @Test fun bonusSquaresUseWordBoardColorsWhileOwnershipStaysGreenAndRed() {
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        listOf("0xFFCFE6F5", "0xFFF6D3E2", "0xFFD6ECCB", "0xFFF8DCC3", "0xFFE2D6F2", "0xFFFBEBB5").forEach {
            assertTrue(online.contains(it))
            assertTrue(practice.contains(it))
        }
        assertTrue(online.contains("PanSiegeMine = Color(0xFF5FAF73)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFD9776F)"))
    }
}
