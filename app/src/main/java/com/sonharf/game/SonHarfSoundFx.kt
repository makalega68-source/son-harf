package com.sonharf.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Son Harf sound palette.
 *
 * The UI tap is intentionally not a beep: it is a tiny, damped tactile impulse designed
 * to resemble a soft pen/stylus tip touching glass. All sounds are generated locally so
 * the APK stays small and there are no external audio assets to load or fail.
 */
object SonHarfSoundFx {
    private const val SAMPLE_RATE = 22050
    @Volatile private var enabled = true

    fun setEnabled(value: Boolean) { enabled = value }

    fun tap() = impulse(durationMs = 34, bodyHz = 520.0, noise = 0.20, gain = 0.17)
    fun softNotify() = chime(doubleArrayOf(660.0, 825.0), intArrayOf(70, 95), 0.12)
    fun wordAccepted() = chime(doubleArrayOf(740.0, 990.0), intArrayOf(45, 72), 0.11)
    fun warning() = chime(doubleArrayOf(360.0, 300.0), intArrayOf(55, 80), 0.10)
    fun bonus() = chime(doubleArrayOf(720.0, 900.0, 1120.0), intArrayOf(60, 65, 95), 0.13)
    fun victory() = chime(doubleArrayOf(660.0, 825.0, 1040.0, 1320.0), intArrayOf(65, 70, 80, 120), 0.14)
    fun defeat() = chime(doubleArrayOf(430.0, 350.0), intArrayOf(95, 130), 0.09)
    fun countdown() = impulse(durationMs = 24, bodyHz = 430.0, noise = 0.10, gain = 0.09)

    private fun impulse(durationMs: Int, bodyHz: Double, noise: Double, gain: Double) {
        if (!enabled) return
        Thread {
            val count = (SAMPLE_RATE * durationMs / 1000.0).toInt().coerceAtLeast(32)
            val pcm = ShortArray(count)
            var smoothNoise = 0.0
            for (i in pcm.indices) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 85.0)
                val rawNoise = Random.nextDouble(-1.0, 1.0)
                smoothNoise = smoothNoise * 0.72 + rawNoise * 0.28
                val body = sin(2.0 * PI * bodyHz * t) * 0.55 + sin(2.0 * PI * bodyHz * 1.75 * t) * 0.18
                val click = (body + smoothNoise * noise) * env * gain
                pcm[i] = (click.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(pcm)
        }.start()
    }

    private fun chime(freqs: DoubleArray, durations: IntArray, gain: Double) {
        if (!enabled) return
        Thread {
            val gapMs = 22
            val totalSamples = freqs.indices.sumOf { (SAMPLE_RATE * durations[it] / 1000.0).toInt() } +
                (freqs.size - 1).coerceAtLeast(0) * (SAMPLE_RATE * gapMs / 1000)
            val pcm = ShortArray(totalSamples.coerceAtLeast(32))
            var cursor = 0
            freqs.indices.forEach { idx ->
                val n = (SAMPLE_RATE * durations[idx] / 1000.0).toInt()
                for (i in 0 until n) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val attack = (i / (SAMPLE_RATE * 0.008)).coerceIn(0.0, 1.0)
                    val release = ((n - i).toDouble() / (SAMPLE_RATE * 0.035)).coerceIn(0.0, 1.0)
                    val env = attack * release
                    val s = (sin(2.0 * PI * freqs[idx] * t) + 0.22 * sin(2.0 * PI * freqs[idx] * 2.0 * t)) * env * gain
                    if (cursor + i < pcm.size) pcm[cursor + i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                cursor += n
                cursor += if (idx != freqs.lastIndex) SAMPLE_RATE * gapMs / 1000 else 0
            }
            playPcm(pcm)
        }.start()
    }

    private fun playPcm(pcm: ShortArray) {
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val bytes = pcm.size * 2
            val min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(maxOf(bytes, min))
                .build()
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
