package com.sonharf.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * File-backed game SFX palette.
 *
 * The previous procedural click/noise generator was intentionally removed because it produced
 * harsh synthetic beeps on some Android devices. These effects are pre-rendered 44.1 kHz WAV
 * assets (layered bell chimes, soft mallets, warm brass and filtered air with a small room
 * reverb) played through SoundPool for low-latency gameplay feedback.
 */
object SonHarfSoundFx {
    @Volatile private var enabled = true
    private var pool: SoundPool? = null
    private val sounds = mutableMapOf<Int, Int>()

    fun init(context: Context) {
        release()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()
        listOf(
            R.raw.sfx_key_click,
            R.raw.sfx_ui_tap,
            R.raw.sfx_word_accepted,
            R.raw.sfx_soft_notify,
            R.raw.sfx_bonus,
            R.raw.sfx_warning,
            R.raw.sfx_heartbeat,
            R.raw.sfx_victory,
            R.raw.sfx_defeat,
            R.raw.sfx_countdown,
            R.raw.sfx_turn_start,
            R.raw.sfx_rival_move,
            R.raw.sfx_round_win,
            R.raw.sfx_round_lost,
            R.raw.sfx_streak,
        ).forEach { resId ->
            sounds[resId] = pool!!.load(context.applicationContext, resId, 1)
        }
    }

    fun release() {
        runCatching { pool?.release() }
        pool = null
        sounds.clear()
    }

    fun setEnabled(value: Boolean) { enabled = value }

    fun tap() = play(R.raw.sfx_ui_tap, .18f)
    fun typingClick() = play(R.raw.sfx_key_click, .12f)
    fun scoreTick() = play(R.raw.sfx_ui_tap, .16f, 1.02f)
    fun leadChange() = play(R.raw.sfx_word_accepted, .28f, 1.02f)
    fun missionComplete() = play(R.raw.sfx_bonus, .34f)
    fun rematchReady() = play(R.raw.sfx_soft_notify, .22f)
    fun softNotify() = play(R.raw.sfx_soft_notify, .20f)
    fun wordAccepted() = play(R.raw.sfx_word_accepted, .30f)
    fun warning() = play(R.raw.sfx_ui_tap, .16f, .92f)
    fun bonus() = play(R.raw.sfx_bonus, .32f)
    fun victory() = play(R.raw.sfx_victory, .42f)
    fun defeat() = play(R.raw.sfx_defeat, .34f)
    fun countdown() { /* countdown uses heartbeat/haptic only; no beep */ }

    /** Son Harf duel cues. */
    fun turnStart() = play(R.raw.sfx_turn_start, .30f)
    fun rivalMove() = play(R.raw.sfx_rival_move, .30f)
    fun wrongWord() = play(R.raw.sfx_warning, .26f)
    fun clockTick() = play(R.raw.sfx_countdown, .22f)
    fun streak() = play(R.raw.sfx_streak, .36f)
    fun roundWon() = play(R.raw.sfx_round_win, .40f)
    fun roundLost() = play(R.raw.sfx_round_lost, .32f)
    fun heartbeat() = play(R.raw.sfx_heartbeat, .24f)
    fun explosion() { /* intentionally disabled */ }
    fun fireworks() = play(R.raw.sfx_victory, .28f)

    /** Harf Yolu uses a deliberately quieter, softer micro-feedback palette. */
    fun puzzleKey() = play(R.raw.sfx_ui_tap, .045f, 1.16f)
    fun puzzleTap() = play(R.raw.sfx_ui_tap, .065f, 1.04f)
    fun puzzleHint() = play(R.raw.sfx_soft_notify, .075f, 1.08f)
    fun puzzleError() = play(R.raw.sfx_soft_notify, .070f, .88f)
    fun puzzleSuccess() = play(R.raw.sfx_word_accepted, .14f, 1.04f)

    private fun play(resId: Int, volume: Float, rate: Float = 1f) {
        if (!enabled) return
        val soundPool = pool ?: return
        val soundId = sounds[resId] ?: return
        runCatching {
            soundPool.play(
                soundId,
                volume.coerceIn(0f, 1f),
                volume.coerceIn(0f, 1f),
                1,
                0,
                rate.coerceIn(.5f, 2f),
            )
        }
    }
}
