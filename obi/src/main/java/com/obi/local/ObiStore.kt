package com.obi.local

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
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal class ObiStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)
    private val mind = ObiMind(context)
    fun canHandle(raw: String): Boolean = ObiParse.isLocalCommand(raw)
    fun handle(raw: String): String {
        val text = raw.trim(); val normalized = text.lowercase(Locale("tr", "TR"))
        if (normalized.contains("doğum günüm") || normalized.contains("dogum gunum")) {
            val date = Regex("(\\d{1,2})\\s*(ocak|şubat|mart|nisan|mayıs|haziran|temmuz|ağustos|eylül|ekim|kasım|aralık)").find(normalized)
                ?: return "Doğum gününü ‘Doğum günüm 12 Mayıs’ diye yazabilirsin."
            val months = listOf("ocak", "şubat", "mart", "nisan", "mayıs", "haziran", "temmuz", "ağustos", "eylül", "ekim", "kasım", "aralık")
            val day = date.groupValues[1].toInt()
            val month = months.indexOf(date.groupValues[2]) + 1
            if (day !in 1..31) return "Günü 1 ile 31 arasında yazabilir misin?"
            prefs.edit().putInt("birth_day", day).putInt("birth_month", month).apply()
            scheduleDaily()
            return "Harika! $day ${date.groupValues[2].replaceFirstChar { it.uppercase() }} gününü kaydettim 🎂"
        }
        if (normalized.contains("günaydın mesajı") || normalized.contains("sabah motive et")) {
            prefs.edit().putBoolean("morning_enabled", true).apply()
            scheduleDaily()
            return "Tamam! Sabah 08.00 civarında sana güzel bir mesaj göndereceğim. Bildirim iznini açmayı unutma."
        }
        if (normalized.contains("sabah mesajını kapat")) {
            prefs.edit().putBoolean("morning_enabled", false).apply()
            return "Sabah mesajlarını kapattım."
        }
        if (normalized.contains("alarm") || normalized.contains("uyandır") || normalized.contains("uyandir")) {
            val time = ObiParse.triggerTime(normalized) ?: return "Saati anlayamadım. Örnek: ‘Sabah 9'a alarm kur’ veya ‘10 dakika sonra alarm kur’."
            schedule(time, "Obi alarmı: Uyanma zamanı!", "alarm")
            return if (canScheduleExact()) "Alarmı ${formatClock(time)} için kurdum. Obi sana seslenecek."
                else "Alarmı ${formatClock(time)} civarına kurdum. Tam saatinde çalması için menüden ‘Kesin alarm izni’ni aç."
        }
        if (normalized.contains("notlarımı göster") || normalized.contains("notlarimi göster")) return notes().takeLast(5).reversed().mapIndexed { i, item -> "${i + 1}. $item" }.joinToString("\n").ifBlank { "Henüz notun yok. ‘Not al …’ yazabilirsin." }
        ObiParse.noteText(normalized, text)?.let { note -> save(note); return "Tamam, bunu hatırlayacağım: ‘$note’" }
        ObiParse.timerMinutes(normalized)?.let { minutes -> schedule(System.currentTimeMillis() + minutes * 60_000L, "Obi zamanlayıcısı"); return "Tamam. $minutes dakika sonra sana haber vereceğim." }
        if (normalized.contains("hatırlat") || normalized.contains("hatirlat")) {
            val time = ObiParse.triggerTime(normalized) ?: return "Saat de yaz: örneğin ‘yarın saat 09.00’da annemi ara diye hatırlat’ veya ‘20 dakika sonra hatırlat’."
            val note = ObiParse.reminderNote(text)
            schedule(time, note); return "Tamam, ${formatClock(time)} için ‘$note’ hatırlatıcısını kurdum."
        }
        return mind.respond(text)
    }
    fun notes(): List<String> = runCatching { val v = JSONArray(prefs.getString("notes", "[]") ?: "[]"); List(v.length()) { v.getString(it) } }.getOrDefault(emptyList())
    private fun save(note: String) { val v = JSONArray(prefs.getString("notes", "[]") ?: "[]"); v.put(note); prefs.edit().putString("notes", v.toString()).apply() }
    private fun formatClock(time: Long): String = Calendar.getInstance().run { timeInMillis = time; String.format(Locale("tr", "TR"), "%02d:%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE)) }
    fun canScheduleExact(): Boolean = context.getSystemService(AlarmManager::class.java)?.let {
        Build.VERSION.SDK_INT < 31 || it.canScheduleExactAlarms()
    } ?: false
    private fun schedule(time: Long, text: String, kind: String = "reminder") {
        val id = (System.nanoTime() and 0x7fff_ffffL).toInt()
        val saved = savedAlarms().put(JSONObject().put("id", id).put("time", time).put("text", text).put("kind", kind))
        prefs.edit().putString("pending_alarms", saved.toString()).apply()
        arm(id, time, text, kind)
    }

    private fun arm(id: Int, time: Long, text: String, kind: String) {
        val intent = Intent(context, ObiReminderReceiver::class.java).putExtra("text", text)
            .putExtra("kind", kind).putExtra("id", id).setAction("com.obi.local.REMINDER.$id")
        val pending = PendingIntent.getBroadcast(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        if (canScheduleExact()) {
            val open = PendingIntent.getActivity(context, id,
                Intent(context, ObiEntryActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(time, open), pending)
        } else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }

    private fun savedAlarms(): JSONArray = runCatching { JSONArray(prefs.getString("pending_alarms", "[]") ?: "[]") }.getOrDefault(JSONArray())

    fun forgetAlarm(id: Int) {
        val kept = JSONArray()
        val saved = savedAlarms()
        for (index in 0 until saved.length()) saved.optJSONObject(index)?.takeIf { it.optInt("id") != id }?.let { kept.put(it) }
        prefs.edit().putString("pending_alarms", kept.toString()).apply()
    }

    /** Alarms do not survive a reboot or app update, so the saved list is armed again; recently missed ones ring now. */
    fun rearmSaved() {
        val now = System.currentTimeMillis()
        val kept = JSONArray()
        val saved = savedAlarms()
        for (index in 0 until saved.length()) {
            val item = saved.optJSONObject(index) ?: continue
            val time = item.optLong("time")
            if (time < now - 6 * 60 * 60_000L) continue
            kept.put(item)
            arm(item.optInt("id"), maxOf(time, now + 5_000L), item.optString("text", "Hatırlatıcı"), item.optString("kind", "reminder"))
        }
        prefs.edit().putString("pending_alarms", kept.toString()).apply()
    }
    fun scheduleDaily() {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val pending = PendingIntent.getBroadcast(context, 808, Intent(context, ObiReminderReceiver::class.java).setAction("com.obi.local.DAILY"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    }
}

class ObiReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.obi.local.DAILY") {
            val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)
            val today = Calendar.getInstance()
            val birthday = prefs.getInt("birth_day", 0) == today.get(Calendar.DAY_OF_MONTH) && prefs.getInt("birth_month", 0) == today.get(Calendar.MONTH) + 1
            val message = when {
                birthday -> "İyi ki doğdun! Bugün senin günün 🎂✨"
                prefs.getBoolean("morning_enabled", false) -> "Günaydın! Bugün küçük bir adım da büyük bir başlangıç 💜"
                else -> null
            }
            if (message != null) notify(context, message, "Obi günaydın diyor")
            if (message != null) context.sendBroadcast(Intent("com.obi.local.CELEBRATE").setPackage(context.packageName).putExtra("text", message))
            ObiStore(context).scheduleDaily()
            return
        }
        intent.getIntExtra("id", 0).takeIf { it != 0 }?.let { ObiStore(context).forgetAlarm(it) }
        notify(context, intent.getStringExtra("text") ?: "Hatırlatıcı zamanı geldi.", if (intent.getStringExtra("kind") == "alarm") "Obi alarmı" else "Obi hatırlatıyor")
        context.sendBroadcast(Intent("com.obi.local.CELEBRATE").setPackage(context.packageName).putExtra("text", intent.getStringExtra("text") ?: "Hatırlatıcı zamanı geldi."))
        if (context.getSharedPreferences("obi_local", Context.MODE_PRIVATE).getBoolean("voice_alerts", true)) {
            runCatching {
                context.startForegroundService(Intent(context, ObiVoiceAlertService::class.java)
                    .putExtra("kind", intent.getStringExtra("kind") ?: "reminder"))
            }
        }
    }

    private fun notify(context: Context, text: String, title: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return; val channel = "obi_reminders"
        manager.createNotificationChannel(NotificationChannel(channel, "Obi hatırlatıcıları", NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(context, 4, Intent(context, ObiEntryActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify((System.currentTimeMillis() and 0x7fff_ffffL).toInt(), Notification.Builder(context, channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build())
    }
}

class ObiBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        ObiStore(context).apply { scheduleDaily(); rearmSaved() }
    }
}
