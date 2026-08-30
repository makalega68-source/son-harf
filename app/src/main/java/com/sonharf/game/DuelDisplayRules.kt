package com.sonharf.game

import java.util.Locale

private val TurkishGameLocale: Locale = Locale.forLanguageTag("tr-TR")

/** Keeps dotted and dotless Turkish I distinct regardless of the device locale. */
internal fun gameUppercase(value: String, language: String): String =
    value.uppercase(if (language.equals("tr", ignoreCase = true)) TurkishGameLocale else Locale.ROOT)

/** Keeps large duel scores on one line inside the compact player card. */
internal fun duelScoreFontSize(score: Int): Int = when (score.toString().length) {
    0, 1, 2 -> 28
    3 -> 21
    4 -> 17
    else -> 14
}
