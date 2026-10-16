package com.obi.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.Calendar

/** Plays offline Higgsfield Luna recordings for user-requested alarm/reminder alerts. */
class ObiVoiceAlertService : Service() {
    companion object {
        const val ACTION_PREVIEW = "com.obi.local.PREVIEW_VOICE"
        const val ACTION_STOP = "com.obi.local.STOP_VOICE"
        private const val CHANNEL = "obi_voice_alert"
        private const val ID = 1302
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var activeStart = 0
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        activeStart = startId
        val kind = intent?.getStringExtra("kind") ?: "reminder"
        val morning = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 5..11
        val (phrase, sound) = when {
            intent?.action == ACTION_PREVIEW -> "Merhaba! Ben Obi. Obi, Obi!" to R.raw.obi_preview
            kind == "alarm" && morning -> "Sabah oldu, uyan hadi! Obi, Obi!" to R.raw.obi_morning
            kind == "alarm" -> "Alarm zamanı! Obi, Obi!" to R.raw.obi_alarm
            else -> "Hatırlatma var! Obi, Obi!" to R.raw.obi_reminder
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
        player?.release()
        player = null
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        runCatching {
            MediaPlayer.create(this, sound, attributes, 0)?.also { media ->
                player = media
                media.setOnCompletionListener { if (activeStart == startId) stopSelfResult(startId) }
                media.setOnErrorListener { _, _, _ ->
                    if (activeStart == startId) stopSelfResult(startId)
                    true
                }
                media.start()
            } ?: stopSelfResult(startId)
        }.onFailure { stopSelfResult(startId) }
        handler.postDelayed({ if (activeStart == startId) stopSelfResult(startId) }, 20_000L)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }
}
