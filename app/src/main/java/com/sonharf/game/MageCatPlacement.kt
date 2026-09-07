package com.sonharf.game

internal data class MageCatPlacement(
    val anchor: MageCatAnchor,
    val heightFraction: Float,
    val reactionHeightFraction: Float = heightFraction,
)

internal enum class MageCatAnchor {
    BOTTOM_END,
    TOP_END,
    RESULT_SIDE,
    CARD_SIDE,
    PREVIEW_CENTER,
}

internal object MageCatPlacementPolicy {
    fun forScreen(screen: MageCatScreen): MageCatPlacement = when (screen) {
        MageCatScreen.HOME -> MageCatPlacement(
            anchor = MageCatAnchor.BOTTOM_END,
            heightFraction = 0.20f,
            reactionHeightFraction = 0.22f,
        )
        MageCatScreen.MATCH -> MageCatPlacement(
            anchor = MageCatAnchor.TOP_END,
            heightFraction = 0.115f,
            reactionHeightFraction = 0.16f,
        )
        MageCatScreen.RESULT -> MageCatPlacement(
            anchor = MageCatAnchor.RESULT_SIDE,
            heightFraction = 0.25f,
            reactionHeightFraction = 0.28f,
        )
        MageCatScreen.QUESTS,
        MageCatScreen.LEAGUE -> MageCatPlacement(
            anchor = MageCatAnchor.CARD_SIDE,
            heightFraction = 0.18f,
            reactionHeightFraction = 0.20f,
        )
        MageCatScreen.SHOP,
        MageCatScreen.PROFILE -> MageCatPlacement(
            anchor = MageCatAnchor.PREVIEW_CENTER,
            heightFraction = 0.27f,
            reactionHeightFraction = 0.30f,
        )
    }
}
