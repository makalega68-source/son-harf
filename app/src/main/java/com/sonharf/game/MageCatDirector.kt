package com.sonharf.game

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal data class MageCatContext(
    val nowMs: Long,
    val screen: MageCatScreen,
    val blockingUiVisible: Boolean = false,
    val playerInputActive: Boolean = false,
    val matchFinished: Boolean = false,
)

internal enum class MageCatScreen {
    HOME,
    MATCH,
    RESULT,
    QUESTS,
    LEAGUE,
    SHOP,
    PROFILE,
}

internal data class MageCatCue(
    val motion: MageCatMotion,
    val prominence: MageCatProminence,
    val duration: Duration,
)

internal enum class MageCatProminence {
    AMBIENT,
    REACTION,
    HERO,
}

/**
 * Oyun olaylarını maskot hareketlerine dönüştüren yerel yönetici.
 *
 * Temel hareket seçimini çevrimiçi yapay zekâya bırakmaz. Böylece davranışlar
 * düşük gecikmeli, test edilebilir ve çevrimdışı çalışır. Aynı olay için
 * [MageCatBehaviorSelector] çeşitlilik sağlar; bu sınıf ise zamanlama,
 * görünürlük ve öncelik kurallarını uygular.
 */
internal class MageCatDirector(
    private val selector: MageCatBehaviorSelector = MageCatBehaviorSelector(),
    private val reactionCooldownMs: Long = 2_800L,
    private val ambientCooldownMs: Long = 9_000L,
) {
    private var lastReactionAtMs: Long = Long.MIN_VALUE / 4
    private var lastAmbientAtMs: Long = Long.MIN_VALUE / 4

    fun cue(event: MageCatEvent, context: MageCatContext): MageCatCue? {
        if (context.blockingUiVisible) return null

        return when (event) {
            MageCatEvent.IDLE -> ambientCue(context)
            MageCatEvent.CORRECT_WORD -> reactionCue(event, context, 1.6.seconds)
            MageCatEvent.WIN_STREAK -> reactionCue(event, context, 2.0.seconds)
            MageCatEvent.VICTORY -> heroCue(event, context, 2.8.seconds)
            MageCatEvent.DEFEAT -> heroCue(event, context, 2.4.seconds)
            MageCatEvent.LEAGUE_PROMOTION -> heroCue(event, context, 3.0.seconds)
            MageCatEvent.DAILY_QUEST -> reactionCue(event, context, 1.8.seconds)
        }
    }

    private fun ambientCue(context: MageCatContext): MageCatCue? {
        if (context.playerInputActive || context.screen == MageCatScreen.RESULT) return null
        if (context.nowMs - lastAmbientAtMs < ambientCooldownMs) return null

        lastAmbientAtMs = context.nowMs
        return MageCatCue(
            motion = selector.choose(MageCatEvent.IDLE),
            prominence = MageCatProminence.AMBIENT,
            duration = 1.5.seconds,
        )
    }

    private fun reactionCue(
        event: MageCatEvent,
        context: MageCatContext,
        duration: Duration,
    ): MageCatCue? {
        if (context.playerInputActive && context.screen == MageCatScreen.MATCH) return null
        if (context.nowMs - lastReactionAtMs < reactionCooldownMs) return null

        lastReactionAtMs = context.nowMs
        return MageCatCue(
            motion = selector.choose(event),
            prominence = MageCatProminence.REACTION,
            duration = duration,
        )
    }

    private fun heroCue(
        event: MageCatEvent,
        context: MageCatContext,
        duration: Duration,
    ): MageCatCue? {
        // Sonuç/lig gibi yüksek değerli anlar cooldown'dan bağımsız oynayabilir.
        lastReactionAtMs = context.nowMs
        return MageCatCue(
            motion = selector.choose(event),
            prominence = MageCatProminence.HERO,
            duration = duration,
        )
    }
}
