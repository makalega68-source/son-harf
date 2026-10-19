package com.sonharf.game

/** The public launch opens Last Letter first. Reopen other games in a tested app update. */
internal object GameAvailability {
    const val wordSiege = false
    const val wordWorkshop = false
    const val dailyPuzzles = false

    fun isAvailable(mode: String): Boolean = when (mode) {
        "son_harf", "last_letter", "normal", "expert" -> true
        "siege", "series", "classic", "word_siege" -> wordSiege
        "atelier", "workshop", "word_workshop" -> wordWorkshop
        else -> false
    }
}
