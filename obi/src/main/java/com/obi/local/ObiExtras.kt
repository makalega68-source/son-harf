package com.obi.local

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.time.LocalDate
import java.time.ZoneId

/** Today's and tomorrow's calendar events, read only on the phone. */
internal object ObiCalendar {
    data class Event(val id: Long, val title: String, val start: Long, val allDay: Boolean)

    fun allowed(context: Context) = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun events(context: Context, day: LocalDate): List<Event> {
        if (!allowed(context)) return emptyList()
        val zone = ZoneId.systemDefault()
        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            android.content.ContentUris.appendId(it, from); android.content.ContentUris.appendId(it, to)
        }.build()
        return runCatching {
            val out = mutableListOf<Event>()
            context.contentResolver.query(uri,
                arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY),
                null, null, CalendarContract.Instances.BEGIN + " ASC",
            )?.use { c ->
                while (c.moveToNext()) out += Event(c.getLong(0), c.getString(1)?.takeIf { it.isNotBlank() } ?: "Etkinlik", c.getLong(2), c.getInt(3) == 1)
            }
            out
        }.getOrDefault(emptyList())
    }

    fun describe(context: Context, day: LocalDate, label: String): String {
        val list = events(context, day)
        if (list.isEmpty()) return "$label takviminde bir şey görünmüyor 🗓 Rahat bir gün!"
        val store = ObiStore(context)
        return "🗓 $label:\n" + list.joinToString("\n") { e ->
            "• ${if (e.allDay) "Tüm gün" else store.formatClock(e.start)} — ${e.title}"
        }
    }

    /** Sets a reminder 10 minutes before each of today's timed events that has none yet. */
    fun remindToday(context: Context): Int {
        val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("calendar_remind", false) || !allowed(context)) return 0
        val today = LocalDate.now().toString()
        val done = (prefs.getString("calendar_done", "") ?: "").split(',').filter { it.startsWith(today) }.toMutableSet()
        val store = ObiStore(context)
        var count = 0
        for (e in events(context, LocalDate.now())) {
            val at = e.start - 10 * 60_000L
            val key = "$today:${e.id}:${e.start}"
            if (e.allDay || at <= System.currentTimeMillis() || key in done) continue
            store.schedule(at, "10 dakika sonra: ${e.title}")
            done += key
            count++
        }
        prefs.edit().putString("calendar_done", done.joinToString(",")).apply()
        return count
    }
}

/** Night mode: Do Not Disturb (or vibrate) and a dim screen between two times, then back to normal. */
internal object ObiNight {
    fun prefs(context: Context) = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)

    fun canQuiet(context: Context) = context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun start(context: Context) {
        val p = prefs(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        val edit = p.edit().putBoolean("night_active", true)
        if (canQuiet(context)) {
            edit.putInt("night_prev_filter", nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL)
            runCatching { nm?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
        } else runCatching {
            val audio = context.getSystemService(android.media.AudioManager::class.java)
            edit.putInt("night_prev_ringer", audio?.ringerMode ?: android.media.AudioManager.RINGER_MODE_NORMAL)
            audio?.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE
        }
        if (Settings.System.canWrite(context)) runCatching {
            val r = context.contentResolver
            edit.putInt("night_prev_mode", Settings.System.getInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE))
                .putInt("night_prev_level", Settings.System.getInt(r, Settings.System.SCREEN_BRIGHTNESS))
            Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS, 10)
        }
        edit.apply()
    }

    fun stop(context: Context) {
        val p = prefs(context)
        if (!p.getBoolean("night_active", false)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (p.contains("night_prev_filter") && canQuiet(context)) runCatching { nm?.setInterruptionFilter(p.getInt("night_prev_filter", NotificationManager.INTERRUPTION_FILTER_ALL)) }
        if (p.contains("night_prev_ringer")) runCatching {
            context.getSystemService(android.media.AudioManager::class.java)?.ringerMode = p.getInt("night_prev_ringer", android.media.AudioManager.RINGER_MODE_NORMAL)
        }
        if (p.contains("night_prev_level") && Settings.System.canWrite(context)) runCatching {
            val r = context.contentResolver
            Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS, p.getInt("night_prev_level", 128))
            Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, p.getInt("night_prev_mode", Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC))
        }
        p.edit().putBoolean("night_active", false).remove("night_prev_filter").remove("night_prev_ringer")
            .remove("night_prev_level").remove("night_prev_mode").apply()
    }
}

/**
 * Sees new message notifications (WhatsApp, SMS, Telegram…) only after the user turns it on in
 * Android's "notification access" settings. Keeps the last few on the phone for "mesajlarımı oku".
 */
class ObiNotificationListener : NotificationListenerService() {
    companion object {
        const val ACTION_MESSAGE = "com.obi.local.MESSAGE"
        private val recent = ArrayDeque<String>()

        fun recentMessages(): List<String> = synchronized(recent) { recent.toList() }

        fun enabled(context: Context): Boolean =
            (Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: "").contains(context.packageName)

        private val messaging = setOf(
            "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "com.google.android.apps.messaging",
            "com.samsung.android.messaging", "com.android.mms", "com.facebook.orca", "com.instagram.android", "com.discord",
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        if (sbn.packageName == packageName || sbn.isOngoing) return
        if (n.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) return
        if (sbn.packageName !in messaging && n.category != android.app.Notification.CATEGORY_MESSAGE) return
        val title = n.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = n.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isBlank()) return
        val line = if (title.isNotBlank()) "$title: $text" else text
        synchronized(recent) {
            if (recent.lastOrNull() == line) return
            recent.addLast(line.take(200))
            while (recent.size > 15) recent.removeFirst()
        }
        if (getSharedPreferences("obi_local", MODE_PRIVATE).getBoolean("read_messages", false)) {
            sendBroadcast(Intent(ACTION_MESSAGE).setPackage(packageName).putExtra("text", line.take(200)))
        }
    }
}
