package com.sonharf.game

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

/**
 * Local reminders that bring the player back: today's race, a streak about to break, and Obi
 * missing you after a couple of days away. Nothing is sent while the app is open: reminders are
 * scheduled when the app goes to the background and cancelled when it comes back. Players turn
 * them off with the "Hatırlatmalar" switch in Settings.
 */
internal object ReminderNotifications {
    enum class Reminder(val dayOffset: Int, val hour: Int, val minute: Int) {
        DAILY_RACE(1, 12, 30),
        STREAK(1, 20, 30),
        MISS_YOU(2, 18, 0),
        GIFT(4, 18, 0),
    }

    private const val CHANNEL = "obi_reminders"
    private const val PREFS = "obi_reminders"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STREAK = "streak"
    private const val KEY_ASKED = "permission_asked"
    internal const val EXTRA_REMINDER = "reminder"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
        if (!value) cancelAll(context)
    }

    /** Remembers the current daily-play streak so the evening reminder can name it. */
    fun rememberStreak(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_STREAK, days.coerceAtLeast(0)).apply()
    }

    /** Android 13+ needs a runtime permission; ask once, the first time the home screen shows. */
    fun shouldAskPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 && enabled(context) && !prefs(context).getBoolean(KEY_ASKED, false) &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun markPermissionAsked(context: Context) {
        prefs(context).edit().putBoolean(KEY_ASKED, true).apply()
    }

    fun onAppOpened(context: Context) = cancelAll(context)

    fun onAppBackground(context: Context) {
        if (!enabled(context)) return
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        Reminder.entries.forEach { reminder ->
            if (reminder == Reminder.STREAK && prefs(context).getInt(KEY_STREAK, 0) < 2) return@forEach
            runCatching { alarms.set(AlarmManager.RTC_WAKEUP, fireAt(reminder), pending(context, reminder)) }
        }
    }

    fun onAlarm(context: Context, reminder: Reminder) {
        if (!enabled(context)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        SonHarfUiState.language = SonHarfPreferences.language(context)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, sh("Hatırlatmalar", "Reminders"), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val (title, text) = message(reminder, prefs(context).getInt(KEY_STREAK, 0))
        val open = PendingIntent.getActivity(
            context,
            4_200,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_obi)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(4_300 + reminder.ordinal, notification) }
    }

    internal fun message(reminder: Reminder, streak: Int): Pair<String, String> = when (reminder) {
        Reminder.DAILY_RACE -> sh("🏁 Günlük Yarış başladı", "🏁 The Daily Race is on") to
            sh("Bugünün harfleri herkes için aynı. Kelime Atölyesi'nde sıralamaya gir!", "Today's letters are the same for everyone. Climb the Word Workshop board!")
        Reminder.STREAK -> sh("🔥 $streak günlük serin yanmak üzere", "🔥 Your $streak-day streak is about to break") to
            sh("Bugün bir oyun oyna, serini koru.", "Play one game today to keep it alive.")
        Reminder.MISS_YOU -> sh("Obi seni özledi 🥺", "Obi misses you 🥺") to
            sh("Tahta hazır, rakiplerin bekliyor. Bir tur atalım mı?", "The board is ready and your rivals are waiting. One round?")
        Reminder.GIFT -> sh("Obi sana hediye hazırladı 🎁", "Obi has a gift for you 🎁") to
            sh("Geri dön, Son Coin hediyeni al.", "Come back and collect your Son Coin gift.")
    }

    private fun fireAt(reminder: Reminder): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, reminder.dayOffset)
        set(Calendar.HOUR_OF_DAY, reminder.hour)
        set(Calendar.MINUTE, reminder.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun pending(context: Context, reminder: Reminder): PendingIntent = PendingIntent.getBroadcast(
        context,
        4_400 + reminder.ordinal,
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_REMINDER, reminder.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelAll(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        Reminder.entries.forEach { runCatching { alarms.cancel(pending(context, it)) } }
    }
}

/** Alarm target: posts one reminder notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminder = intent.getStringExtra(ReminderNotifications.EXTRA_REMINDER)
            ?.let { runCatching { ReminderNotifications.Reminder.valueOf(it) }.getOrNull() } ?: return
        ReminderNotifications.onAlarm(context, reminder)
    }
}
