package com.obi.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Calendar
import java.util.Locale

/** Speaks only short user-requested alarm/reminder alerts; never listens to the microphone. */
class ObiVoiceAlertService : Service() {
    companion object {
        const val ACTION_PREVIEW = "com.obi.local.PREVIEW_VOICE"
        const val ACTION_STOP = "com.obi.local.STOP_VOICE"
        private const val CHANNEL = "obi_voice_alert"
        private const val ID = 1302
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var activeStart = 0
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        activeStart = startId
        val kind = intent?.getStringExtra("kind") ?: "reminder"
        val morning = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 5..11
        val phrase = when {
            intent?.action == ACTION_PREVIEW -> "Merhaba! Ben Obi. Obi, Obi!"
            kind == "alarm" && morning -> "Sabah oldu, uyan hadi! Obi, Obi!"
            kind == "alarm" -> "Alarm zamanı! Obi, Obi!"
            else -> "Hatırlatma var! Obi, Obi!"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Obi sesli uyarı", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 12,
            Intent(this, ObiVoiceAlertService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Obi sesleniyor")
            .setContentText(phrase)
            .addAction(Notification.Action.Builder(null, "Sesi durdur", stop).build())
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(ID, notification)
        tts?.stop(); tts?.shutdown(); tts = null
        tts = TextToSpeech(this) { status ->
            val engine = tts ?: return@TextToSpeech
            if (startId != activeStart) return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) { stopSelfResult(startId); return@TextToSpeech }
            engine.language = Locale("tr", "TR")
            engine.setPitch(1.35f)
            engine.setSpeechRate(0.94f)
            engine.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { handler.post { stopSelfResult(startId) } }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { handler.post { stopSelfResult(startId) } }
            })
            engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "obi-voice-$startId")
        }
        handler.postDelayed({ if (activeStart == startId) stopSelfResult(startId) }, 20_000L)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
