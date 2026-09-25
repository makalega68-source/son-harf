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
        listOf("0xFF5DADE2", "0xFFE573A5", "0xFF7DC36B", "0xFFE0914A", "0xFF9B6FD1", "0xFFF2C14E").forEach {
            assertTrue(online.contains(it))
            assertTrue(practice.contains(it))
        }
        assertTrue(online.contains("PanSiegeMine = Color(0xFF3E9F4D)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFD0514A)"))
    }
}
