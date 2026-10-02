package com.sonharf.game

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** One optional offline idle line. Never installs a voice or sends text to a network engine. */
internal class MascotAmbientVoice(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var closed = false
    private var onSpeaking: ((Boolean) -> Unit)? = null
    private var serial = 0L
    private var activeId: String? = null
    private val engine = TextToSpeech(appContext) { status ->
        main.post { if (!closed) ready = status == TextToSpeech.SUCCESS }
    }

    init {
        engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { report(id, true) }
            override fun onDone(id: String?) { report(id, false) }
            @Deprecated("Required by older TTS engines")
            override fun onError(id: String?) { report(id, false) }
        })
    }

    private fun report(id: String?, speaking: Boolean) {
        main.post { if (!closed && id == activeId) onSpeaking?.invoke(speaking) }
    }

    fun speak(text: String, language: String, speaking: (Boolean) -> Unit): Boolean {
        if (closed || !ready || !SonHarfPreferences.soundEnabled(appContext)) return false
        val locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
        val voice = runCatching { engine.voices?.filter {
            !it.isNetworkConnectionRequired && it.locale.language == locale.language
        }?.maxByOrNull { it.quality } }.getOrNull() ?: return false
        stop()
        if (engine.setVoice(voice) != TextToSpeech.SUCCESS) return false
        engine.setPitch(1.12f)
        engine.setSpeechRate(.88f)
        onSpeaking = speaking
        activeId = "mascot-idle-${++serial}"
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, activeId)
        if (result != TextToSpeech.SUCCESS) { stop(); return false }
        return true
    }

    fun stop() {
        activeId = null
        engine.stop()
        onSpeaking?.invoke(false)
        onSpeaking = null
    }

    fun close() {
        stop()
        closed = true
        ready = false
        main.removeCallbacksAndMessages(null)
        engine.shutdown()
    }
}
