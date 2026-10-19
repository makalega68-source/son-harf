package com.obi.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Calendar
import java.util.Locale

/**
 * Speaks user-requested alarms and reminders in Turkish with the phone's own speech engine, in a
 * small, high voice. Falls back to the bundled recordings only when no Turkish voice is installed.
 */
class ObiVoiceAlertService : Service() {
    companion object {
        const val ACTION_PREVIEW = "com.obi.local.PREVIEW_VOICE"
        const val ACTION_STOP = "com.obi.local.STOP_VOICE"
        const val ACTION_STATUS = "com.obi.local.VOICE_STATUS"
        private const val CHANNEL = "obi_voice_alert"
        private const val ID = 1302
        private val TURKISH = Locale("tr", "TR")

        /** Voice pitch (1.0 = engine default); Obi talks a little higher and brighter. */
        fun pitch(context: Context): Float = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE).getFloat("voice_pitch2", 1.2f)

        fun setPitch(context: Context, value: Float) {
            context.getSharedPreferences("obi_local", Context.MODE_PRIVATE).edit().putFloat("voice_pitch2", value.coerceIn(0.8f, 2.0f)).apply()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var track: android.media.AudioTrack? = null
    private var activeStart = 0
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        activeStart = startId
        val kind = intent?.getStringExtra("kind") ?: "reminder"
        val text = intent?.getStringExtra("text")?.takeIf { it.isNotBlank() && !it.startsWith("Obi alarmı") }
        val morning = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 5..11
        val (phrase, sound) = when {
            intent?.action == ACTION_PREVIEW -> "Merhaba! Ben Obi. Sesim böyle, beğendin mi?" to 0
            kind == "alarm" && morning -> "Günaydın! Sabah oldu, hadi uyan bakalım. Yeni bir gün seni bekliyor!" to 0
            kind == "alarm" -> "Alarm zamanı geldi! Hadi bakalım, kalkıyoruz!" to 0
            text != null -> "Hatırlatma var: ${text.trimEnd('.', '!')}." to 0
            else -> "Hatırlatma zamanı geldi!" to 0
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Obi sesli uyarı", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 12,
            Intent(this, ObiVoiceAlertService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_obi_notification)
            .setContentTitle("Obi sesleniyor")
            .setContentText(phrase)
            .addAction(Notification.Action.Builder(null, "Sesi durdur", stop).build())
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(ID, notification)
        stopSound()
        // An alarm is said twice so it can wake someone; a reminder once.
        val twice = kind == "alarm" && intent?.action != ACTION_PREVIEW
        val id = intent?.getIntExtra("id", 0) ?: 0
        val (key, line) = when {
            intent?.action == ACTION_PREVIEW -> "preview" to ObiGeminiVoice.FIXED.getValue("preview")
            kind == "alarm" && morning -> "alarm_morning" to ObiGeminiVoice.FIXED.getValue("alarm_morning")
            kind == "alarm" -> "alarm" to ObiGeminiVoice.FIXED.getValue("alarm")
            text != null -> ObiGeminiVoice.reminderLine(text).let { ObiGeminiVoice(this).keyFor(it) to it }
            else -> "reminder" to ObiGeminiVoice.FIXED.getValue("reminder")
        }
        val voice = ObiGeminiVoice(this)
        val preview = intent?.action == ACTION_PREVIEW
        // The phone's own Turkish voice sounds poor, so it is only used when the user turns it on;
        // otherwise a failed Gemini clip leaves just the notification sound.
        val usePhone = getSharedPreferences("obi_local", MODE_PRIVATE).getBoolean("phone_voice", false)
        // When this reminder's own clip could not be made, Obi still speaks with its Gemini voice
        // using the general line ("Hatırlatma zamanı geldi!" / the alarm line); the words are in the notification.
        val general = if (kind == "alarm") (if (morning) "alarm_morning" else "alarm") else "reminder"
        val phoneVoice: () -> Unit = {
            if (preview) report(voice.lastStatus() + if (usePhone) "\nTelefonun sesiyle okuyorum." else "")
            if (!preview && key != general && voice.has(general)) playGemini(voice, general, twice, startId) {
                if (usePhone) speak(phrase, sound, startId) else stopSelfResult(startId)
            }
            else if (usePhone) speak(if (twice) "$phrase … $phrase" else phrase, sound, startId)
            // Never stay silent on a real alert: the bundled Obi recording is the last resort.
            else if (!preview) playRecording(sound, startId)
            else stopSelfResult(startId)
        }
        if (voice.has(key)) {
            if (preview) report("Gemini sesi: ${voice.voiceName()}, ton ${String.format(Locale.ROOT, "%.2f", pitch(this))} ♪")
            playGemini(voice, key, twice, startId, phoneVoice)
        }
        else if (ObiGemini(this).connected()) Thread {
            // Not made yet (e.g. a new phrase): try now, briefly; otherwise use the phone's voice.
            val ready = runCatching { voice.prepareBlocking(key, line) }.getOrDefault(false)
            handler.post {
                if (activeStart != startId) return@post
                if (ready) { if (preview) report(voice.lastStatus()); playGemini(voice, key, twice, startId, phoneVoice) } else phoneVoice()
            }
        }.start()
        else {
            if (preview) report("Gemini bağlı değil. ‘Gemini bağla’ yazarak bağlayabilirsin.")
            if (usePhone || !preview) phoneVoice() else stopSelfResult(startId)
        }
        handler.postDelayed({ if (activeStart == startId) stopSelfResult(startId) }, 75_000L)
        return START_NOT_STICKY
    }

    private fun report(text: String) {
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).putExtra("text", text))
    }

    private fun playGemini(voice: ObiGeminiVoice, key: String, twice: Boolean, startId: Int, fallback: () -> Unit) {
        track = voice.play(key, pitch(this), if (twice) 2 else 1) {
            // Clips stay on the phone, so the same sentence never costs another Gemini request.
            handler.post { if (activeStart == startId) stopSelfResult(startId) }
        }
        if (track == null) fallback()
    }

    private fun speak(phrase: String, fallback: Int, startId: Int) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(this) { status ->
            val ready = engine
            val turkish = status == TextToSpeech.SUCCESS && ready != null &&
                ready.isLanguageAvailable(TURKISH) >= TextToSpeech.LANG_AVAILABLE
            if (!turkish || ready == null) {
                ready?.shutdown()
                if (tts === ready) tts = null
                playRecording(fallback, startId)
                return@TextToSpeech
            }
            ready.language = TURKISH
            bestTurkishVoice(ready)?.let { ready.voice = it }
            ready.setPitch(pitch(this))
            ready.setSpeechRate(1.0f)
            ready.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            ready.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { handler.post { if (activeStart == startId) stopSelfResult(startId) } }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { handler.post { playRecording(fallback, startId) } }
            })
            val result = ready.speak(phrase, TextToSpeech.QUEUE_FLUSH, Bundle(), "obi_$startId")
            if (result != TextToSpeech.SUCCESS) playRecording(fallback, startId)
        }
        tts = engine
    }

    /** Prefers an installed (offline), high-quality Turkish voice; a female one when the engine names it. */
    private fun bestTurkishVoice(engine: TextToSpeech): Voice? = runCatching {
        engine.voices.orEmpty()
            .filter { it.locale.language == "tr" && !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(compareBy<Voice>({ it.isNetworkConnectionRequired }, { -it.quality },
                { if (it.name.contains("female", true) || it.name.contains("-tra-") || it.name.contains("-tre-")) 0 else 1 }))
            .firstOrNull()
    }.getOrNull()

    /**
     * The old English-accented recordings are gone: when no Gemini clip could be made, the alert
     * stays a normal notification (with its sound) and the reason is kept for "ses durumu".
     */
    @Suppress("UNUSED_PARAMETER")
    private fun playRecording(sound: Int, startId: Int) {
        getSharedPreferences("obi_gemini", MODE_PRIVATE).edit()
            .putString("voice_status", "Son uyarı Gemini sesi olmadan geldi. " + ObiGeminiVoice(this).lastStatus()).apply()
        stopSelfResult(startId)
    }

    private fun stopSound() {
        player?.release()
        player = null
        track?.let { runCatching { it.stop() } }
        track = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopSound()
        tts?.let { runCatching { it.stop(); it.shutdown() } }
        tts = null
        super.onDestroy()
    }
}
