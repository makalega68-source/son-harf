package com.sonharf.game

/**
 * Sunucudan gelen oyun durumunu maskotun sınırlı olay sözlüğüne çevirir.
 *
 * UI katmanı bu sınıfta tutulmaz. Böylece aynı sunucu olayı yeniden geldiğinde veya
 * ekran yeniden compose olduğunda hangi maskot olayının üretileceği saf ve test edilebilir kalır.
 */
internal object MageCatGameEventMapper {
    fun acceptedWord(
        isMine: Boolean,
        streak: Int,
    ): MageCatEvent? {
        if (!isMine) return null
        return if (streak >= WIN_STREAK_THRESHOLD) MageCatEvent.WIN_STREAK else MageCatEvent.CORRECT_WORD
    }

    fun matchResult(
        winnerId: String?,
        myPlayerId: String?,
    ): MageCatEvent? {
        if (winnerId == null || myPlayerId == null) return null
        return if (winnerId == myPlayerId) MageCatEvent.VICTORY else MageCatEvent.DEFEAT
    }

    private const val WIN_STREAK_THRESHOLD = 3
}
