package com.sonharf.game

import kotlin.random.Random

internal enum class MageCatEvent {
    IDLE,
    CORRECT_WORD,
    INVALID_WORD,
    TIME_PRESSURE,
    WIN_STREAK,
    VICTORY,
    DEFEAT,
    LEAGUE_PROMOTION,
    DAILY_QUEST,
}

internal enum class MageCatMotion {
    IDLE_LOOK,
    IDLE_BLINK,
    IDLE_WAND_CHECK,
    THINKING,
    WINK,
    HAPPY,
    EXCITED,
    WAND_CELEBRATE,
    PROUD,
    SAD,
    TIRED,
    ANGRY,
    SURPRISED,
    MAGIC_BURST,
}

internal class MageCatBehaviorSelector(
    private val random: Random = Random.Default,
    private val historySize: Int = 3,
) {
    private val recent = ArrayDeque<MageCatMotion>(historySize)

    fun choose(event: MageCatEvent): MageCatMotion {
        val pool = when (event) {
            MageCatEvent.IDLE -> listOf(
                MageCatMotion.IDLE_LOOK,
                MageCatMotion.IDLE_BLINK,
                MageCatMotion.THINKING,
                MageCatMotion.WINK,
            )
            MageCatEvent.CORRECT_WORD -> listOf(
                MageCatMotion.HAPPY,
                MageCatMotion.WAND_CELEBRATE,
                MageCatMotion.PROUD,
            )
            MageCatEvent.INVALID_WORD -> listOf(
                MageCatMotion.ANGRY,
                MageCatMotion.SAD,
                MageCatMotion.SURPRISED,
            )
            MageCatEvent.TIME_PRESSURE -> listOf(
                MageCatMotion.SURPRISED,
                MageCatMotion.THINKING,
            )
            MageCatEvent.WIN_STREAK -> listOf(
                MageCatMotion.EXCITED,
                MageCatMotion.WAND_CELEBRATE,
                MageCatMotion.MAGIC_BURST,
            )
            MageCatEvent.VICTORY -> listOf(
                MageCatMotion.EXCITED,
                MageCatMotion.MAGIC_BURST,
                MageCatMotion.PROUD,
            )
            MageCatEvent.DEFEAT -> listOf(
                MageCatMotion.SAD,
                MageCatMotion.TIRED,
            )
            MageCatEvent.LEAGUE_PROMOTION -> listOf(
                MageCatMotion.MAGIC_BURST,
                MageCatMotion.EXCITED,
                MageCatMotion.WAND_CELEBRATE,
            )
            MageCatEvent.DAILY_QUEST -> listOf(
                MageCatMotion.IDLE_WAND_CHECK,
                MageCatMotion.PROUD,
                MageCatMotion.HAPPY,
            )
        }

        val eligible = pool.filterNot(recent::contains).ifEmpty {
            pool.filterNot { it == recent.lastOrNull() }.ifEmpty { pool }
        }
        val selected = eligible[random.nextInt(eligible.size)]
        remember(selected)
        return selected
    }

    private fun remember(motion: MageCatMotion) {
        if (recent.size == historySize) recent.removeFirst()
        recent.addLast(motion)
    }
}
