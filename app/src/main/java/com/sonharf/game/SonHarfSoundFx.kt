package com.sonharf.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Procedural lightweight effects palette; no external audio asset is required. */
object SonHarfSoundFx {
    private const val SAMPLE_RATE = 24000
    @Volatile private var enabled = true
    fun setEnabled(value: Boolean) { enabled = value }

    fun tap() = click(18, 0.18, 0.42)
    fun softNotify() { click(20, 0.16, 0.48); delayedClick(55, 17, 0.13, 0.52) }
    fun wordAccepted() { click(17, 0.15, 0.50); delayedClick(42, 14, 0.11, 0.56) }
    fun warning() = click(24, 0.16, 0.30)
    fun bonus() { click(19, 0.17, 0.48); delayedClick(45, 18, 0.15, 0.54) }
    fun victory() { click(18, 0.17, 0.50); delayedClick(38, 16, 0.15, 0.56); delayedClick(76, 15, 0.13, 0.60) }
    fun defeat() = click(26, 0.14, 0.28)
    fun countdown() = click(13, 0.08, 0.38)

    /** Rising launch whistle + low boom + decaying crackle, closer to a real firework than the old click stack. */
    fun fireworks() {
        if (!enabled) return
        Thread {
            val durationSec = 1.65
            val count = (SAMPLE_RATE * durationSec).toInt()
            val pcm = ShortArray(count)
            var phase = 0.0
            var lp = 0.0
            for (i in pcm.indices) {
                val t = i.toDouble() / SAMPLE_RATE
                var sample = 0.0

                // Rocket launch / ascending whistle: 620 Hz -> ~2100 Hz.
                if (t < 0.58) {
                    val p = t / 0.58
                    val freq = 620.0 + 1480.0 * p * p
                    phase += 2.0 * PI * freq / SAMPLE_RATE
                    val whistleEnv = sin(PI * p).coerceAtLeast(0.0) * 0.22
                    val hiss = Random.nextDouble(-1.0, 1.0) * 0.045 * (1.0 - p * .35)
                    sample += sin(phase) * whistleEnv + hiss
                }

                // Main boom around 600ms with a strong low body and short noise transient.
                if (t >= 0.58) {
                    val x = t - 0.58
                    val boomEnv = exp(-x * 7.0)
                    val low = sin(2.0 * PI * 74.0 * x) * 0.48 + sin(2.0 * PI * 112.0 * x) * 0.22
                    val white = Random.nextDouble(-1.0, 1.0)
                    lp = lp * 0.86 + white * 0.14
                    val blast = (white * 0.42 + lp * 0.30) * exp(-x * 15.0)
                    sample += (low + blast) * boomEnv

                    // Spark crackles: random impulses distributed after the boom.
                    if (x in 0.05..0.95 && Random.nextDouble() < 0.013) {
                        sample += Random.nextDouble(0.20, 0.55) * (1.0 - x / 1.1).coerceAtLeast(0.0)
                    }
                    sample += Random.nextDouble(-1.0, 1.0) * 0.055 * exp(-x * 2.8)
                }

                pcm[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
            play(pcm)
        }.start()
    }

    private fun delayedClick(delayMs: Long, durationMs: Int, gain: Double, brightness: Double) {
        if (!enabled) return
        Thread { Thread.sleep(delayMs); click(durationMs, gain, brightness) }.start()
    }

    private fun click(durationMs: Int, gain: Double, brightness: Double) {
        if (!enabled) return
        Thread {
            val count = (SAMPLE_RATE * durationMs / 1000.0).toInt().coerceAtLeast(48)
            val pcm = ShortArray(count)
            var lp = 0.0
            var prev = 0.0
            for (i in pcm.indices) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 210.0)
                val white = Random.nextDouble(-1.0, 1.0)
                lp = lp * (0.58 + brightness * 0.18) + white * (0.42 - brightness * 0.18)
                val hp = white - prev * 0.72
                prev = white
                val transient = if (i < 5) (1.0 - i / 5.0) * 0.7 else 0.0
                val sample = (lp * 0.58 + hp * brightness * 0.35 + transient) * env * gain
                pcm[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
            play(pcm)
        }.start()
    }

    private fun play(pcm: ShortArray) {
        runCatching {
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            val format = AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            val bytes = pcm.size * 2
            val min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder().setAudioAttributes(attrs).setAudioFormat(format).setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(maxOf(bytes, min)).build()
            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(pcm.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(audioTrack: AudioTrack) { audioTrack.release() }
                override fun onPeriodicNotification(audioTrack: AudioTrack) = Unit
            })
            track.play()
        }
    }
}
