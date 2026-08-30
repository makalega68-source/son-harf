package com.sonharf.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * File-backed game SFX palette.
 *
 * The previous procedural click/noise generator was intentionally removed because it produced
 * harsh synthetic beeps on some Android devices. These effects are pre-rendered 44.1 kHz WAV
 * assets and played through SoundPool for low-latency gameplay feedback.
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
            R.raw.sfx_countdown,
            R.raw.sfx_heartbeat,
            R.raw.sfx_explosion,
            R.raw.sfx_victory,
            R.raw.sfx_defeat,
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

    fun tap() = play(R.raw.sfx_ui_tap, .42f)
    fun typingClick() = play(R.raw.sfx_key_click, .32f)
    fun scoreTick() = play(R.raw.sfx_ui_tap, .34f, 1.08f)
    fun leadChange() = play(R.raw.sfx_word_accepted, .55f, 1.04f)
    fun missionComplete() = play(R.raw.sfx_bonus, .62f)
    fun rematchReady() = play(R.raw.sfx_soft_notify, .46f)
    fun softNotify() = play(R.raw.sfx_soft_notify, .42f)
    fun wordAccepted() = play(R.raw.sfx_word_accepted, .62f)
    fun warning() = play(R.raw.sfx_warning, .50f)
    fun bonus() = play(R.raw.sfx_bonus, .62f)
    fun victory() = play(R.raw.sfx_victory, .68f)
    fun defeat() = play(R.raw.sfx_defeat, .56f)
    fun countdown() = play(R.raw.sfx_countdown, .34f)
    fun heartbeat() = play(R.raw.sfx_heartbeat, .42f)
    fun explosion() = play(R.raw.sfx_explosion, .72f)
    fun fireworks() = play(R.raw.sfx_victory, .48f)

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
