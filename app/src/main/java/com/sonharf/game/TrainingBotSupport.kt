package com.sonharf.game

import kotlin.random.Random

internal enum class TrainingBotDifficulty { EASY, MEDIUM, HARD }

internal enum class WordSiegeOwnershipRelation { NEUTRAL, SELF, OPPONENT }

internal object TrainingBotSupport {
    // Light-medium ownership fills chosen to keep black tile letters highly readable.
    const val OWN_FILL_ARGB: Long = 0xFFA8DDB5
    const val OPPONENT_FILL_ARGB: Long = 0xFFF1AAAA
    const val NEUTRAL_FILL_ARGB: Long = 0xFFF4F6F8
    const val OWN_BORDER_ARGB: Long = 0xFF3F8B59
    const val OPPONENT_BORDER_ARGB: Long = 0xFFB95353

    private val femaleBotNames = listOf(
        "Elif", "Zeynep", "Defne", "Ece", "Duru", "İrem", "Selin", "Melis",
    )
    private val maleBotNames = listOf(
        "Mert", "Emir", "Kerem", "Arda", "Eren", "Can", "Kaan", "Berk",
    )
    val turkishBotNames = femaleBotNames + maleBotNames

    fun ownershipRelation(owner: Int, myOwner: Int): WordSiegeOwnershipRelation = when {
        owner == 0 -> WordSiegeOwnershipRelation.NEUTRAL
        owner == myOwner -> WordSiegeOwnershipRelation.SELF
        else -> WordSiegeOwnershipRelation.OPPONENT
    }

    fun chooseBotName(previous: String? = null, random: Random = Random.Default): String {
        val candidates = turkishBotNames.filterNot { it == previous }
        return candidates[random.nextInt(candidates.size)]
    }

    fun botGender(name: String): String = if (name in femaleBotNames) "female" else "male"

    fun botAvatarPath(name: String): String = if (name in femaleBotNames) "bot:female" else "bot:male"

    fun reactionDelayMs(
        difficulty: TrainingBotDifficulty,
        complexity: Int = 1,
        random: Random = Random.Default,
    ): Long {
        val bounded = complexity.coerceIn(1, 8)
        val base = when (difficulty) {
            TrainingBotDifficulty.EASY -> 1_650L
            TrainingBotDifficulty.MEDIUM -> 1_150L
            TrainingBotDifficulty.HARD -> 800L
        }
        val spread = when (difficulty) {
            TrainingBotDifficulty.EASY -> 900
            TrainingBotDifficulty.MEDIUM -> 650
            TrainingBotDifficulty.HARD -> 450
        }
        return base + bounded * 70L + random.nextInt(spread)
    }

    fun blackContrastRatio(argb: Long): Double {
        val r = (argb shr 16 and 0xFF).toDouble() / 255.0
        val g = (argb shr 8 and 0xFF).toDouble() / 255.0
        val b = (argb and 0xFF).toDouble() / 255.0
        fun linear(v: Double) = if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        val luminance = 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
        return (luminance + 0.05) / 0.05
    }
}
