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

/** Stable ordering used by both the HUD and transient lead-change feedback. */
internal fun duelLeader(myScore: Int, opponentScore: Int): Int = when {
    myScore > opponentScore -> 1
    myScore < opponentScore -> -1
    else -> 0
}

private val FailedDuelEvents = setOf(
    "word_already_used",
    "wrong_start_letter",
    "not_in_dictionary",
    "invalid_word",
    "ends_with_soft_g",
    "turn_expired",
)

/**
 * A successful RPC can arrive after an older room event. The valid-word count
 * is therefore checked first so stale failure codes never reject a new word.
 */
internal fun shouldTreatSubmissionAsFailure(
    previousValidWordCount: Int,
    resultValidWordCount: Int,
    eventCode: String?,
    eventPlayerId: String?,
    currentPlayerId: String?,
): Boolean =
    resultValidWordCount <= previousValidWordCount &&
        currentPlayerId != null &&
        eventPlayerId == currentPlayerId &&
        eventCode in FailedDuelEvents

/**
 * The soft-g explanation is deliberately fail-closed. A stale room event,
 * another player's event or a backend fallback must never surface this reason.
 */
internal fun shouldShowSoftGReason(
    eventCode: String?,
    eventPlayerId: String?,
    currentPlayerId: String?,
    submittedWord: String,
    language: String,
): Boolean =
    eventCode == "ends_with_soft_g" &&
        currentPlayerId != null &&
        eventPlayerId == currentPlayerId &&
        gameUppercase(submittedWord.trim(), language).endsWith("Ğ")

/** Same guard for RPC failures where PostgREST only exposes an error string. */
internal fun shouldShowSoftGReasonFromFailure(
    rawError: String?,
    submittedWord: String,
    language: String,
): Boolean =
    rawError?.contains("ends_with_soft_g", ignoreCase = true) == true &&
        gameUppercase(submittedWord.trim(), language).endsWith("Ğ")

internal enum class DuelEventTone { Player, Opponent, Tie, Error, Milestone }

internal data class DuelTransientEvent(
    val id: Long,
    val text: String,
    val tone: DuelEventTone,
    val durationMs: Long = 1_150L,
)
