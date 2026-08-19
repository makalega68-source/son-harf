package com.sonharf.game

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Lightweight procedural sound palette for Son Harf.
 * No bundled audio assets: tiny tones keep the APK small and avoid harsh notification sounds.
 */
object SonHarfSoundFx {
    private val tone by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, 42) }

    fun tap() = play(ToneGenerator.TONE_PROP_BEEP, 32)
    fun softNotify() = play(ToneGenerator.TONE_PROP_ACK, 70)
    fun wordAccepted() = play(ToneGenerator.TONE_DTMF_6, 45)
    fun warning() = play(ToneGenerator.TONE_PROP_NACK, 55)
    fun bonus() {
        play(ToneGenerator.TONE_DTMF_8, 65)
        playDelayed(ToneGenerator.TONE_DTMF_9, 75, 75)
    }
    fun victory() {
        play(ToneGenerator.TONE_DTMF_3, 85)
        playDelayed(ToneGenerator.TONE_DTMF_6, 90, 95)
        playDelayed(ToneGenerator.TONE_DTMF_9, 120, 205)
    }
    fun defeat() = play(ToneGenerator.TONE_DTMF_2, 95)

    private fun play(type: Int, durationMs: Int) {
        runCatching { tone.startTone(type, durationMs) }
    }

    private fun playDelayed(type: Int, durationMs: Int, delayMs: Long) {
        Thread {
            try {
                Thread.sleep(delayMs)
                play(type, durationMs)
            } catch (_: InterruptedException) {
            }
        }.start()
    }
}
