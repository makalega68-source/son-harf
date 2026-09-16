package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.ui.vfx.LocalVfx
import com.sonharf.game.ui.vfx.VfxEvent

/**
 * G3.2 presentation-only bridge for the live Son Harf arena.
 *
 * It observes state the Premier screen already owns; it never mutates match state and never makes
 * network calls. Initial historical state is consumed silently so reopening an active match does
 * not replay old effects.
 */
@Composable
internal fun PremierVfxBridge(
    room: GameRoomDto,
    meId: String?,
    words: List<GameWordDto>,
    input: String,
    turnSeconds: Int,
    myTurn: Boolean,
) {
    val vfx = LocalVfx.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val widthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val heightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val center = Offset(widthPx * .5f, heightPx * .48f)
    val inputAnchor = Offset(widthPx * .5f, heightPx * .82f)
    val myCardAnchor = Offset(widthPx * .24f, heightPx * .16f)
    val rivalCardAnchor = Offset(widthPx * .76f, heightPx * .16f)

    var previousInputLength by remember(room.id) { mutableIntStateOf(input.length) }
    var previousRoom by remember(room.id) { mutableStateOf(room) }
    var previousLastWordId by remember(room.id) { mutableLongStateOf(words.lastOrNull()?.id ?: 0L) }
    var ownTurnFloor by remember(room.id) { mutableIntStateOf(15) }
    var wasMyTurn by remember(room.id) { mutableStateOf(myTurn) }

    LaunchedEffect(input, myTurn) {
        if (myTurn && input.length > previousInputLength) {
            vfx.play(VfxEvent.LetterDrop(inputAnchor))
        }
        previousInputLength = input.length
    }

    LaunchedEffect(turnSeconds, myTurn) {
        if (myTurn) {
            if (!wasMyTurn) ownTurnFloor = turnSeconds.coerceAtLeast(1)
            ownTurnFloor = minOf(ownTurnFloor, turnSeconds.coerceAtLeast(1))
            if (turnSeconds in 1..5) vfx.play(VfxEvent.LastSeconds)
        }
        wasMyTurn = myTurn
    }

    LaunchedEffect(room, words) {
        val before = previousRoom
        val amHost = meId == room.hostId
        val beforeAmHost = meId == before.hostId
        val myRoundsBefore = if (beforeAmHost) before.hostRounds else before.guestRounds
        val myRoundsNow = if (amHost) room.hostRounds else room.guestRounds
        val myStreakBefore = if (beforeAmHost) before.hostStreak else before.guestStreak
        val myStreakNow = if (amHost) room.hostStreak else room.guestStreak

        if (myRoundsNow > myRoundsBefore) {
            vfx.play(VfxEvent.RoundWin(myCardAnchor))
        }
        if (myStreakNow != myStreakBefore && myStreakNow in setOf(3, 5, 10)) {
            vfx.play(VfxEvent.Streak(myStreakNow, myCardAnchor))
        }

        val eventChanged = room.lastEvent != before.lastEvent ||
            room.lastEventPlayerId != before.lastEventPlayerId ||
            room.currentPlayerId != before.currentPlayerId ||
            room.turnDeadline != before.turnDeadline
        if (
            eventChanged &&
            !room.lastEventPlayerId.isNullOrBlank() &&
            room.lastEventPlayerId != meId &&
            premierVfxIsFailureEvent(room.lastEvent)
        ) {
            vfx.play(VfxEvent.OpponentError(rivalCardAnchor))
        }

        val newWords = words.filter { it.id > previousLastWordId }
        newWords.forEach { entry ->
            val word = entry.normalizedWord.ifBlank { entry.word }.trim()
            if (word.isBlank()) return@forEach
            val mine = entry.playerId == meId
            val wordAnchor = if (mine) myCardAnchor else rivalCardAnchor
            vfx.play(
                VfxEvent.WordAccepted(
                    score = 0,
                    anchor = wordAnchor,
                    tint = SonHarfTheme.SonHarfOrange,
                ),
            )

            val glyph = word.last()
            val from = if (mine) myCardAnchor else rivalCardAnchor
            val to = if (mine) rivalCardAnchor else myCardAnchor
            vfx.play(VfxEvent.LetterBridge(from = from, to = to, glyph = glyph))

            if (word.length >= 8) {
                val count = word.length.coerceAtMost(12)
                val anchors = List(count) { index ->
                    val fraction = if (count == 1) .5f else index.toFloat() / (count - 1).toFloat()
                    Offset(widthPx * (.30f + .40f * fraction), center.y)
                }
                vfx.play(VfxEvent.LongWord(anchors))
            }

            if (mine && ownTurnFloor <= 2) {
                vfx.play(VfxEvent.LastSecondSave(center))
            }
            if (mine) ownTurnFloor = 15
        }

        previousLastWordId = maxOf(previousLastWordId, words.lastOrNull()?.id ?: 0L)
        previousRoom = room
    }
}

private fun premierVfxIsFailureEvent(event: String?): Boolean = event in setOf(
    "invalid_length",
    "invalid_characters",
    "not_in_dictionary",
    "invalid_word",
    "abbreviation_not_allowed",
    "proper_noun_not_allowed",
    "not_game_allowed",
    "ends_with_soft_g",
    "wrong_start_letter",
    "word_already_used",
    "turn_expired",
    "timeout",
    "failed",
)
