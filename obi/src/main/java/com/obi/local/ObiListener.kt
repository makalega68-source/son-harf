package com.obi.local

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Turkish speech input for Obi through the phone's speech recogniser.
 *
 * - [listenOnce]: push-to-talk (long-press Obi): the whole sentence is the command.
 * - [startWakeMode]: keeps listening; a sentence that starts with (or contains) "Obi" is a command.
 *   This keeps the microphone in use, so Android shows its microphone dot and it costs battery.
 */
/** How the recogniser may write "Obi" in Turkish: "Obi", "obiş", "hobi", "o bi", "opi"… */
private const val WAKE = "obi|obiş|obicim|obiciğim|ubi|obii|obby|obie|hobi|o bi|opi|ovi|oby"
private val wakeWord = Regex("(?<!\\p{L})(?:$WAKE)(?!\\p{L})")

internal class ObiListener(
    private val context: Context,
    /** A command heard, without the wake word; empty when only "Obi" was said. */
    private val onCommand: (String) -> Unit,
    /** Listening state for the mascot ("listening", "idle", or an error text). */
    private val onState: (String) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var wakeMode = false
    private var oneShot = false
    private var awaitingCommand = false
    private var consecutiveErrors = 0
    private var lastHeard = ""
    private var lastProblem = ""
    private var sessions = 0

    /** For "dinleme durumu": what the microphone is doing and what it last heard. */
    fun status(): String = buildString {
        append(if (wakeMode) "‘Obi’ diye seslenmeni dinliyorum ✓" else "Sürekli dinleme kapalı (açmak için ‘beni dinle’).")
        append("\nMikrofon izni: ${if (hasPermission()) "var" else "yok"} • Konuşma tanıma: ${if (available()) "var" else "yok"}")
        if (sessions > 0) append("\nDinleme turu: $sessions")
        if (lastHeard.isNotBlank()) append("\nSon duyduğum: “$lastHeard”")
        if (lastProblem.isNotBlank()) append("\nSon sorun: $lastProblem")
    }

    val isWakeMode: Boolean get() = wakeMode

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listenOnce() {
        oneShot = true
        awaitingCommand = true
        start()
    }

    fun startWakeMode() {
        wakeMode = true
        consecutiveErrors = 0
        if (recognizer == null) start()
    }

    fun stopWakeMode() {
        wakeMode = false
        if (!oneShot) stop()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.let { runCatching { it.cancel(); it.destroy() } }
        recognizer = null
    }

    private fun start() {
        if (!hasPermission() || !available()) { onState("Mikrofon izni ya da konuşma tanıma yok"); return }
        stop()
        val engine = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull() ?: return
        recognizer = engine
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (oneShot || awaitingCommand) onState("listening") }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onError(error: Int) {
                // Silence is not a fault: in a quiet room each turn ends with "no speech", and it must
                // not slow down listening. Only real faults (network, busy, permission) back off.
                val silence = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                if (!silence) {
                    consecutiveErrors++
                    lastProblem = when (error) {
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "internet yok ya da yavaş"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "mikrofon izni yok"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "mikrofon başka bir uygulamada"
                        SpeechRecognizer.ERROR_AUDIO -> "mikrofon açılamadı"
                        else -> "konuşma tanıma hatası ($error)"
                    }
                }
                val wasOneShot = oneShot
                oneShot = false
                if (wasOneShot) {
                    awaitingCommand = false
                    onState(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "Mikrofon izni yok" else "idle")
                }
                restartLater()
            }

            override fun onResults(results: Bundle?) {
                consecutiveErrors = 0
                val all = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                // Prefer an alternative that contains the wake word ("hobi" → "Obi").
                val heard = (all.firstOrNull { wakeWord.containsMatchIn(ObiLife.lower(it)) } ?: all.firstOrNull()).orEmpty().trim()
                if (heard.isNotBlank()) lastHeard = heard.take(60)
                val wasOneShot = oneShot
                oneShot = false
                handle(heard, wasOneShot)
                restartLater()
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        sessions++
        runCatching { engine.startListening(intent) }.onFailure { onState("idle"); restartLater() }
    }

    private fun handle(heard: String, wasOneShot: Boolean) {
        if (heard.isBlank()) { if (wasOneShot) { awaitingCommand = false; onState("idle") }; return }
        val lower = ObiLife.lower(heard)
        if (wasOneShot || awaitingCommand) {
            awaitingCommand = false
            onCommand(stripWakeWord(heard))
            return
        }
        // Wake mode: only sentences addressed to Obi count.
        val wake = wakeWord.find(lower) ?: return
        val rest = stripWakeWord(heard.substring(wake.range.first))
        if (rest.isBlank()) { awaitingCommand = true; onCommand("") } else onCommand(rest)
    }

    private fun stripWakeWord(text: String): String =
        text.replace(Regex("(?i)^\\s*(?:hey\\s+|hadi\\s+)?(?:$WAKE)[,!.\\s]*"), "").trim()

    private fun restartLater() {
        if (!wakeMode && !awaitingCommand) { stop(); return }
        // Back off after repeated errors (e.g. no network) so the phone is not kept busy.
        val delay = when {
            consecutiveErrors >= 8 -> 20_000L
            consecutiveErrors >= 3 -> 4_000L
            else -> 400L
        }
        handler.postDelayed({ if (wakeMode || awaitingCommand) start() }, delay)
    }
}
