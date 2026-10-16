package com.sonharf.game

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

/**
 * Obi's no-cloud assistant layer. Notes stay in the application's private storage and alarms are
 * scheduled by Android on the player's own device. A message that is not a local assistant command
 * deliberately falls through to the optional on-device Nano conversation.
 */
internal object ObiLocalAssistant {
    private const val PREFS = "obi_local_assistant"
    private const val NOTES = "notes"
    private const val CHANNEL = "obi_personal_reminders"
    internal const val EXTRA_TEXT = "obi_personal_reminder_text"
    private const val REMINDER_ACTION = "com.sonharf.game.OBI_PERSONAL_REMINDER"

    internal data class Result(val handled: Boolean, val reply: String? = null)

    fun handle(context: Context, rawText: String, isTurkish: Boolean): Result {
        val text = rawText.trim()
        val normalized = text.lowercase(Locale("tr", "TR"))
        if (text.isBlank()) return Result(false)

        if (normalized.contains("notlarımı göster") || normalized.contains("notlarimi göster") ||
            normalized.contains("notlarım ne") || normalized.contains("notlarimi ne")
        ) {
            return Result(true, notesReply(context, isTurkish))
        }

        noteText(normalized, text)?.let { note ->
            saveNote(context, note)
            return Result(true, if (isTurkish) "Tamam, bunu notlarına ekledim: \"$note\"" else "Done, I saved this in your notes: \"$note\"")
        }

        timerMinutes(normalized)?.let { minutes ->
            val label = timerLabel(text, isTurkish)
            schedule(context, System.currentTimeMillis() + minutes * 60_000L, label)
            return Result(true, if (isTurkish) "Tamam. ${formatDuration(minutes)} için zamanlayıcı başlattım; süresi dolunca haber vereceğim." else "Done. I started a ${formatDuration(minutes)} timer and will let you know when it is over.")
        }

        if (normalized.contains("hatırlat") || normalized.contains("hatirlat")) {
            val time = reminderTime(normalized)
            if (time == null) {
                return Result(true, if (isTurkish) "Hatırlatıcıyı kurabilirim. Saati de söyle: örneğin \"yarın saat 09.00'da annemi ara diye hatırlat\"." else "I can set that reminder. Tell me the time too, for example: \"remind me tomorrow at 9 to call my mom\".")
            }
            val label = reminderLabel(text, isTurkish)
            schedule(context, time, label)
            return Result(true, if (isTurkish) "Tamam, ${describeTime(time)} için \"$label\" hatırlatıcısını kurdum." else "Done. I set a reminder for ${describeTime(time)}: \"$label\".")
        }

        return Result(false)
    }

    private fun noteText(normalized: String, original: String): String? {
        val starts = listOf("not al", "şunu not et", "şunu not al", "bunu not et", "bunu not al", "not et")
        val match = starts.firstOrNull { normalized.startsWith(it) } ?: return null
        return original.drop(match.length).trim(' ', ':', '-', '"').takeIf { it.isNotBlank() }
    }

    private fun timerMinutes(normalized: String): Int? {
        if (!normalized.contains("zamanlayıcı") && !normalized.contains("zamanlayici") && !normalized.contains("timer")) return null
        val match = Regex("(\\d{1,3})\\s*(dakika|dk|saat)").find(normalized) ?: return null
        val amount = match.groupValues[1].toIntOrNull() ?: return null
        val minutes = if (match.groupValues[2] == "saat") amount * 60 else amount
        return minutes.takeIf { it in 1..1_440 }
    }

    private fun reminderTime(normalized: String): Long? {
        val hourMatch = Regex("saat\\s*(\\d{1,2})(?:[:.]?(\\d{2}))?").find(normalized) ?: return null
        val hour = hourMatch.groupValues[1].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = hourMatch.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: 0
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when {
            normalized.contains("yarın") || normalized.contains("yarin") -> target.add(Calendar.DAY_OF_YEAR, 1)
            target.timeInMillis <= now.timeInMillis -> target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun timerLabel(text: String, isTurkish: Boolean): String =
        if (isTurkish) "Obi'nin zamanlayıcısı" else "Obi's timer"

    private fun reminderLabel(text: String, isTurkish: Boolean): String {
        val clean = text
            .replace(Regex("(?i)beni\\s*"), "")
            .replace(Regex("(?i)(yarın|yarin)\\s*"), "")
            .replace(Regex("(?i)saat\\s*\\d{1,2}(?:[:.]?\\d{2})?\\s*"), "")
            .replace(Regex("(?i)(diye\\s*)?hatırlat(ır mısın|ir misin)?\\s*"), "")
            .trim(' ', ':', '-', '.', '"')
        return clean.ifBlank { if (isTurkish) "Hatırlatıcı" else "Reminder" }.take(120)
    }

    private fun formatDuration(minutes: Int): String = when {
        minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60} saat"
        else -> "$minutes dakika"
    }

    private fun describeTime(time: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        return String.format(Locale("tr", "TR"), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    private fun notesReply(context: Context, isTurkish: Boolean): String {
        val notes = readNotes(context)
        if (notes.isEmpty()) return if (isTurkish) "Henüz notun yok. \"Not al ...\" diyerek bana bırakabilirsin." else "You have no notes yet. Say \"take a note ...\" and I'll keep it here."
        val list = notes.takeLast(5).reversed().mapIndexed { index, note -> "${index + 1}. $note" }.joinToString("\n")
        return if (isTurkish) "Son notların:\n$list" else "Your latest notes:\n$list"
    }

    private fun saveNote(context: Context, note: String) {
        val notes = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NOTES, "[]") ?: "[]")
        notes.put(JSONObject().put("text", note).put("createdAt", System.currentTimeMillis()))
        while (notes.length() > 50) {
            val trimmed = JSONArray()
            for (index in 1 until notes.length()) trimmed.put(notes.get(index))
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(NOTES, trimmed.toString()).apply()
            return
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(NOTES, notes.toString()).apply()
    }

    private fun readNotes(context: Context): List<String> = runCatching {
        val stored = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NOTES, "[]") ?: "[]")
        List(stored.length()) { index -> stored.getJSONObject(index).optString("text") }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun schedule(context: Context, time: Long, text: String) {
        val requestCode = (System.nanoTime() and 0x7fff_ffffL).toInt()
        val intent = Intent(context, ObiLocalReminderReceiver::class.java)
            .setAction("$REMINDER_ACTION.$requestCode")
            .putExtra(EXTRA_TEXT, text)
        val pending = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)?.let { alarms ->
            runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending) }
        }
    }

    internal fun showReminder(context: Context, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Obi", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(
            context,
            5_201,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_obi)
            .setContentTitle("Obi hatırlatıyor")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() and 0x7fff_ffffL).toInt(), notification)
    }
}

class ObiLocalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ObiLocalAssistant.showReminder(context, intent.getStringExtra(ObiLocalAssistant.EXTRA_TEXT).orEmpty().ifBlank { "Hatırlatıcı zamanı geldi." })
    }
}
