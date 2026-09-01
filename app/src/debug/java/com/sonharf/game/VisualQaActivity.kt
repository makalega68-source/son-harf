package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.WordSiegeCellDto
import com.sonharf.game.data.WordSiegeGameDto

/** Debug-only runtime harness used by CI emulator visual/gesture checks. */
class VisualQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "siege"
        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().background(MainUi.Background)) {
                    when (screen) {
                        "duel" -> VisualQaDuel()
                        else -> VisualQaSiege()
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun VisualQaSiege() {
    val board = List(81) { index ->
        when (index) {
            40 -> WordSiegeCellDto(bonus = "2K")
            39 -> WordSiegeCellDto(letter = "S", owner = 1)
            41 -> WordSiegeCellDto(letter = "N", owner = 2)
            30 -> WordSiegeCellDto(bonus = "3H")
            50 -> WordSiegeCellDto(bonus = "2H")
            else -> WordSiegeCellDto()
        }
    }
    val game = WordSiegeGameDto(
        id = "qa-siege",
        playerOneId = "me",
        playerTwoId = "opponent",
        status = "playing",
        currentPlayerId = "me",
        board = board,
        bag = "ABCDEFGHIJKLMN",
        playerOneRack = "KALEMİN",
        playerTwoRack = "OYUNCUU",
        playerOneWordScore = 18,
        playerTwoWordScore = 15,
        playerOneAreaScore = 7,
        playerTwoAreaScore = 5,
        playerOneArea = 6,
        playerTwoArea = 4,
    )
    val profiles = mapOf(
        "me" to ProfileDto(id = "me", displayName = "Oyuncu", rating = 1120),
        "opponent" to ProfileDto(id = "opponent", displayName = "Rakip", rating = 1095),
    )
    WordSiegePanMatch(
        game = game,
        me = "me",
        profiles = profiles,
        moves = emptyList(),
        placements = emptyMap(),
        selectedRackIndex = 0,
        horizontal = true,
        busy = false,
        notice = sh("İlk hamle ortadaki 2K karesinden geçmeli.", "The first move must cross the center 2W cell."),
        onBack = {},
        onBoardCell = {},
        onRackTile = {},
        onHorizontal = {},
        onSubmit = {},
        onPass = {},
        onExchange = {},
        onChat = {},
        onForfeit = {},
        onCancelWaiting = {},
    )
}

@androidx.compose.runtime.Composable
private fun VisualQaDuel() {
    val room = GameRoomDto(
        id = "qa-duel",
        code = "QA1234",
        hostId = "me",
        guestId = "opponent",
        status = "playing",
        language = "tr",
        hostScore = 24,
        guestScore = 21,
        currentPlayerId = "me",
        roundNo = 2,
        roundWordCount = 4,
        hostRounds = 1,
        guestRounds = 0,
        turnDeadline = null,
    )
    LightDuelArena(
        room = room,
        me = "me",
        playerName = "Oyuncu",
        playerAvatarPath = null,
        playerGender = null,
        playerRating = 1120,
        opponentName = "Rakip",
        opponentAvatarPath = null,
        opponentGender = null,
        opponentRating = 1095,
        words = emptyList(),
        isVip = true,
        feedbackWord = null,
        feedbackCorrect = null,
        wordInput = "KALEM",
        onWordInput = {},
        notice = sh("Sıra sende.", "Your turn."),
        busy = false,
        triviaRound = null,
        triviaQuestion = null,
        triviaSelection = null,
        onSubmit = {},
        onTimeout = {},
        onTrivia = {},
        onTriviaTimeout = {},
        onChat = {},
        onForfeit = {},
        onExit = {},
        onRematch = {},
    )
}
