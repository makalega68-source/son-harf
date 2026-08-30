package com.sonharf.game

data class RatingLeagueProgress(
    val leagueName: String,
    val nextLeagueName: String,
    val floor: Int,
    val nextAt: Int?,
    val progress: Float,
    val pointsToNext: Int,
)

fun ratingLeagueProgress(rating: Int): RatingLeagueProgress {
    val safe = rating.coerceAtLeast(100)
    val league = when {
        safe >= 1800 -> "EFSANE"
        safe >= 1600 -> "ELMAS"
        safe >= 1400 -> "PLATİN"
        safe >= 1250 -> "ALTIN"
        safe >= 1100 -> "GÜMÜŞ"
        else -> "BRONZ"
    }
    val floor = when (league) {
        "EFSANE" -> 1800
        "ELMAS" -> 1600
        "PLATİN" -> 1400
        "ALTIN" -> 1250
        "GÜMÜŞ" -> 1100
        else -> 100
    }
    val next = when (league) {
        "BRONZ" -> 1100
        "GÜMÜŞ" -> 1250
        "ALTIN" -> 1400
        "PLATİN" -> 1600
        "ELMAS" -> 1800
        else -> null
    }
    val nextLeague = when (league) {
        "BRONZ" -> "GÜMÜŞ"
        "GÜMÜŞ" -> "ALTIN"
        "ALTIN" -> "PLATİN"
        "PLATİN" -> "ELMAS"
        "ELMAS" -> "EFSANE"
        else -> "EFSANE"
    }
    val progress = if (next == null) 1f else ((safe-floor).toFloat()/(next-floor).coerceAtLeast(1)).coerceIn(0f,1f)
    return RatingLeagueProgress(
        leagueName = league,
        nextLeagueName = nextLeague,
        floor = floor,
        nextAt = next,
        progress = progress,
        pointsToNext = next?.let { (it-safe).coerceAtLeast(0) } ?: 0,
    )
}
