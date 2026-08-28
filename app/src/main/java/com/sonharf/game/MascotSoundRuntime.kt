package com.sonharf.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal enum class MascotSoundCue {
    LAUGH,
    SAD,
    EXCITED,
    BORED,
    MISS_YOU,
}

internal object MascotSoundPolicy {
    const val VOLUME = 0.16f
    const val MIN_GAP_MS = 1_400L

    fun cueForMotion(motion: MascotMotion): MascotSoundCue? = when (motion) {
        MascotMotion.VICTORY -> MascotSoundCue.LAUGH
        MascotMotion.DEFEAT -> MascotSoundCue.SAD
        MascotMotion.CRITICAL,
        MascotMotion.RUN,
        MascotMotion.GREETING -> MascotSoundCue.EXCITED
        MascotMotion.SIT -> MascotSoundCue.BORED
        MascotMotion.LOOK_AT_PLAYER -> MascotSoundCue.MISS_YOU
        MascotMotion.IDLE,
        MascotMotion.WALK,
        MascotMotion.TURN_LEFT,
        MascotMotion.TURN_RIGHT,
        MascotMotion.THINKING -> null
    }
}

internal class MascotSoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = mutableSetOf<Int>()
    private val soundIds = mapOf(
        MascotSoundCue.LAUGH to soundPool.load(context.applicationContext, R.raw.mascot_laugh, 1),
        MascotSoundCue.SAD to soundPool.load(context.applicationContext, R.raw.mascot_sad, 1),
        MascotSoundCue.EXCITED to soundPool.load(context.applicationContext, R.raw.mascot_excited, 1),
        MascotSoundCue.BORED to soundPool.load(context.applicationContext, R.raw.mascot_bored, 1),
        MascotSoundCue.MISS_YOU to soundPool.load(context.applicationContext, R.raw.mascot_miss_you, 1),
    )

    private var lastPlayedAtMs = 0L

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded += sampleId
        }
    }

    fun play(cue: MascotSoundCue) {
        val soundId = soundIds[cue] ?: return
        if (soundId !in loaded) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayedAtMs < MascotSoundPolicy.MIN_GAP_MS) return

        val streamId = soundPool.play(
            soundId,
            MascotSoundPolicy.VOLUME,
            MascotSoundPolicy.VOLUME,
            1,
            0,
            1f,
        )
        if (streamId != 0) {
            lastPlayedAtMs = now
        } else if (BuildConfig.DEBUG) {
            Log.d("MASCOT_AUDIO", "SoundPool could not start cue=$cue")
        }
    }

    fun release() {
        soundPool.release()
    }
}

@Composable
internal fun MascotSoundBridge(motion: MascotMotion) {
    val context = LocalContext.current
    val player = remember(context.applicationContext) { MascotSoundPlayer(context.applicationContext) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    val mascotId = MascotSelectionRuntime.selectedId
    LaunchedEffect(motion, mascotId) {
        if (mascotId != MascotCatalog.CHIBI_WIZARD_ID) return@LaunchedEffect
        MascotSoundPolicy.cueForMotion(motion)?.let(player::play)
    }
}
