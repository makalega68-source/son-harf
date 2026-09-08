package com.sonharf.game.mascot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class MageCatMood {
    IDLE, HAPPY, EXCITED, PANIC, ANGRY, SAD, CRYING, WINK, TIRED
}

object MageCatDirector {
    var currentMood by mutableStateOf(MageCatMood.IDLE)
        private set

    var eyeColorVariant by mutableStateOf("blue")

    fun onMatchStart() { currentMood = MageCatMood.IDLE }

    fun onCorrectWord(length: Int, streak: Int) {
        currentMood = if (streak >= 3 || length >= 7) MageCatMood.EXCITED else MageCatMood.HAPPY
    }

    fun onWrongWordOrTimeout() { currentMood = MageCatMood.SAD }

    fun onTimeUrgent(secondsLeft: Int) {
        currentMood = when {
            secondsLeft <= 4 -> MageCatMood.PANIC
            secondsLeft <= 8 -> MageCatMood.ANGRY
            currentMood in setOf(MageCatMood.PANIC, MageCatMood.ANGRY) -> MageCatMood.IDLE
            else -> currentMood
        }
    }

    fun onMatchVictory() { currentMood = MageCatMood.EXCITED }
    fun onMatchDefeat() { currentMood = MageCatMood.CRYING }
    fun onLobbyGreet() { currentMood = MageCatMood.WINK }
    fun resetToIdle() { currentMood = MageCatMood.IDLE }
}
