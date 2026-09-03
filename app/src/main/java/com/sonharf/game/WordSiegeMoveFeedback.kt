package com.sonharf.game

internal enum class WordSiegeValidationState { IDLE, READY, ACCEPTED, REJECTED }

internal data class WordSiegeMoveFeedback(
    val state: WordSiegeValidationState,
    val message: String,
)

internal fun wordSiegeValidationFeedback(
    placementsCount: Int,
    error: String? = null,
    acceptedWord: String? = null,
    turkish: Boolean,
): WordSiegeMoveFeedback = when {
    !error.isNullOrBlank() -> WordSiegeMoveFeedback(
        WordSiegeValidationState.REJECTED,
        if (turkish) "✕ $error" else "✕ $error",
    )
    !acceptedWord.isNullOrBlank() -> WordSiegeMoveFeedback(
        WordSiegeValidationState.ACCEPTED,
        if (turkish) "✓ $acceptedWord kabul edildi" else "✓ $acceptedWord accepted",
    )
    placementsCount > 0 -> WordSiegeMoveFeedback(
        WordSiegeValidationState.READY,
        if (turkish) "✓ Hamle hazır" else "✓ Move ready",
    )
    else -> WordSiegeMoveFeedback(WordSiegeValidationState.IDLE, "")
}
