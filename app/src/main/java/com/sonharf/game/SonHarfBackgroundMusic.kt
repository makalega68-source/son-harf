package com.sonharf.game

import android.content.Context
import android.media.MediaPlayer

/**
 * Single background-music runtime for Son Harf.
 *
 * Only Warm Beginnings is used. It loops across the app and follows the existing sound preference.
 */
internal object SonHarfBackgroundMusic {
    private var player: MediaPlayer? = null

    fun start(context: Context) {
        if (!SonHarfPreferences.soundEnabled(context)) {
            pause()
            return
        }

        val active = player ?: MediaPlayer.create(
            context.applicationContext,
            R.raw.warm_beginnings,
        )?.also {
            it.isLooping = true
            it.setVolume(0.24f, 0.24f)
            player = it
        } ?: return

        if (!active.isPlaying) {
            runCatching { active.start() }
        }
    }

    fun pause() {
        val active = player ?: return
        if (active.isPlaying) {
            runCatching { active.pause() }
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        if (enabled) start(context) else pause()
    }

    fun release() {
        player?.let { runCatching { it.release() } }
        player = null
    }
}
