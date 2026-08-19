package com.sonharf.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.exp
import kotlin.random.Random

/** Short dry key-click palette. No musical beeps/chimes. */
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
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
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
