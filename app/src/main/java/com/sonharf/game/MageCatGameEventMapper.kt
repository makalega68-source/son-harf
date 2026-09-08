package com.sonharf.game

internal object MageCatGameEventMapper {
    fun acceptedWord(isMine: Boolean, streak: Int): MageCatEvent? {
        if (!isMine) return null
        return if (streak >= WIN_STREAK_THRESHOLD) MageCatEvent.WIN_STREAK else MageCatEvent.CORRECT_WORD
    }

    fun rejectedWord(isMine: Boolean): MageCatEvent? =
        if (isMine) MageCatEvent.INVALID_WORD else null

    fun timePressure(isMine: Boolean): MageCatEvent? =
        if (isMine) MageCatEvent.TIME_PRESSURE else null

    fun matchResult(winnerId: String?, myPlayerId: String?): MageCatEvent? {
        if (winnerId == null || myPlayerId == null) return null
        return if (winnerId == myPlayerId) MageCatEvent.VICTORY else MageCatEvent.DEFEAT
    }

    private const val WIN_STREAK_THRESHOLD = 3
}
