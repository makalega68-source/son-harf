package com.sonharf.game

import kotlin.math.abs

data class BilRoundRules(
    val questionNo: Int,
    val secondsLeft: Int,
    val riskEnabled: Boolean = false,
    val streak: Int = 0,
) {
    val isBoss: Boolean get() = questionNo == 5
    val isFinal: Boolean get() = questionNo == 10
    val multiplier: Int get() = (if (riskEnabled) 2 else 1) * (if (isFinal) 2 else 1)
}

data class BilRoundOutcome(
    val won: Boolean,
    val points: Int,
    val speedBonus: Int,
    val streakBonus: Int,
    val title: String?,
)

object BilBakalimCompetitionEngine {
    fun resolve(player: Double?, rival: Double, correct: Double, rules: BilRoundRules): BilRoundOutcome {
        val playerDiff = player?.let { abs(it - correct) } ?: Double.POSITIVE_INFINITY
        val rivalDiff = abs(rival - correct)
        val won = playerDiff <= rivalDiff
        if (!won) return BilRoundOutcome(false, 0, 0, 0, null)
        val speed = (rules.secondsLeft / 4).coerceIn(0, 5)
        val streakBonus = when {
            rules.streak >= 9 -> 5
            rules.streak >= 4 -> 3
            rules.streak >= 2 -> 1
            else -> 0
        }
        val bossBonus = if (rules.isBoss) 5 else 0
        val points = (10 + speed + streakBonus + bossBonus) * rules.multiplier
        val title = when {
            rules.isFinal -> "Final Ustası"
            rules.isBoss -> "Boss Avcısı"
            speed == 5 -> "Hız Ustası"
            rules.streak >= 9 -> "10'da 10"
            else -> null
        }
        return BilRoundOutcome(true, points, speed, streakBonus, title)
    }

    fun league(rating: Int): String = when {
        rating >= 1600 -> "BİLGE"
        rating >= 1400 -> "ELMAS"
        rating >= 1200 -> "ALTIN"
        rating >= 1000 -> "GÜMÜŞ"
        else -> "BRONZ"
    }

    fun categoryMastery(correct: Int, played: Int): Int =
        if (played <= 0) 0 else ((correct.toDouble() / played) * 100).toInt().coerceIn(0, 100)

    fun performanceText(playerScore: Int, rivalScore: Int): String = when (playerScore - rivalScore) {
        in 31..Int.MAX_VALUE -> "Baskın galibiyet"
        in 11..30 -> "Net galibiyet"
        in 1..10 -> "Kıl payı galibiyet"
        0 -> "Başa baş"
        in -10..-1 -> "Kıl payı mağlubiyet"
        else -> "Rövanş zamanı"
    }

    fun surpriseReward(wins: Int): String = when {
        wins > 0 && wins % 7 == 0 -> "Style Sandığı"
        wins > 0 && wins % 5 == 0 -> "Prestij Unvanı"
        wins > 0 && wins % 3 == 0 -> "Son Coin Sandığı"
        else -> "XP"
    }

    fun dailyChallenge(dayOfYear: Int): Int = (dayOfYear * 37 + 11) % bilBakalimQuestions.size

    fun jokerHint(answer: Double): Pair<Double, Double> {
        val margin = (abs(answer) * .20).coerceAtLeast(2.0)
        return (answer - margin) to (answer + margin)
    }
}
