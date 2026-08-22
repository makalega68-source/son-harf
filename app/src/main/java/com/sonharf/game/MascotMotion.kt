package com.sonharf.game

internal enum class MascotMotion(
    val animationName: String,
    val looping: Boolean,
    val holdMs: Long,
) {
    IDLE("idle_base", true, 0L),
    CURIOUS("idle_alert", false, 1800L),
    HAPPY("react_correct", false, 1500L),
    SAD("react_wrong", false, 1800L),
    STREAK("react_streak", false, 2100L),
    OPPONENT("react_nervous", true, 0L),
    COLLECT("collect", false, 1500L),
    VICTORY("victory", false, 2600L),
}
