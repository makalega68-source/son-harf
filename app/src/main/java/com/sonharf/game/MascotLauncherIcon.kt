package com.sonharf.game

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

/**
 * The home-screen icon shows the mascot's mood, like a pet that misses you: happy while you
 * play, sad after a day away, cross after three days, and asleep (sulking) after a week.
 *
 * Each mood is an activity-alias with its own icon; exactly one is enabled. Switching happens
 * only while the app is in the background (onStop) or from an alarm, never while it is in use.
 */
internal object MascotLauncherIcon {
    enum class Mood(val alias: String, val afterHours: Long) {
        HAPPY(".LauncherMascotHappy", 0),
        SAD(".LauncherMascotSad", 24),
        ANGRY(".LauncherMascotAngry", 72),
        SLEEPY(".LauncherMascotSleepy", 168),
    }

    private const val PREFS = "mascot_launcher_icon"
    private const val KEY_LAST_OPEN = "last_open"
    private const val KEY_MOOD = "mood"
    internal const val EXTRA_MOOD = "mood"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(context: Context): Mood =
        runCatching { Mood.valueOf(prefs(context).getString(KEY_MOOD, Mood.HAPPY.name)!!) }.getOrDefault(Mood.HAPPY)

    /** App came to the foreground: note the visit and greet if the mascot had been sulking. */
    fun onAppOpened(context: Context) {
        val before = current(context)
        prefs(context).edit().putLong(KEY_LAST_OPEN, System.currentTimeMillis()).apply()
        cancelAlarms(context)
        if (before != Mood.HAPPY) greetBack(context, before)
    }

    /** App went to the background: smile again and schedule the moods for a long absence. */
    fun onAppBackground(context: Context) {
        runCatching { apply(context, Mood.HAPPY) }
        val lastOpen = prefs(context).getLong(KEY_LAST_OPEN, System.currentTimeMillis())
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        Mood.entries.filter { it != Mood.HAPPY }.forEach { mood ->
            runCatching {
                alarms.set(AlarmManager.RTC, lastOpen + mood.afterHours * 3_600_000L, pending(context, mood))
            }
        }
    }

    /** Called by the alarm: only changes the face if the player really has been away that long. */
    fun onAlarm(context: Context, mood: Mood) {
        val lastOpen = prefs(context).getLong(KEY_LAST_OPEN, 0L)
        if (lastOpen == 0L) return
        val away = System.currentTimeMillis() - lastOpen
        if (away >= mood.afterHours * 3_600_000L - 60_000L && mood.ordinal > current(context).ordinal) {
            runCatching { apply(context, mood) }
        }
    }

    private fun apply(context: Context, mood: Mood) {
        val pm = context.packageManager
        val pkg = context.packageName
        // Touching components makes launchers refresh; do nothing when the face is already right.
        val already = Mood.entries.all {
            val state = pm.getComponentEnabledSetting(ComponentName(pkg, pkg + it.alias))
            val on = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && it == Mood.HAPPY)
            on == (it == mood)
        }
        if (already) {
            prefs(context).edit().putString(KEY_MOOD, mood.name).apply()
            return
        }
        // Enable the new face first so the app always has a launcher entry, then hide the rest.
        pm.setComponentEnabledSetting(
            ComponentName(pkg, pkg + mood.alias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        Mood.entries.filter { it != mood }.forEach {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, pkg + it.alias),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        prefs(context).edit().putString(KEY_MOOD, mood.name).apply()
    }

    private fun pending(context: Context, mood: Mood): PendingIntent = PendingIntent.getBroadcast(
        context,
        4_100 + mood.ordinal,
        Intent(context, MascotIconMoodReceiver::class.java).putExtra(EXTRA_MOOD, mood.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelAlarms(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        Mood.entries.filter { it != Mood.HAPPY }.forEach { runCatching { alarms.cancel(pending(context, it)) } }
    }

    private fun greetBack(context: Context, mood: Mood) {
        val text = when (mood) {
            Mood.SAD -> sh("Obi seni çok özlemişti... geri geldin!", "Obi missed you so much... you're back!")
            Mood.ANGRY -> sh("Hmph! Obi küsmüştü ama tamam, barıştık!", "Hmph! Obi was sulking, but okay, friends again!")
            else -> sh("Obi uyuyakalmıştı... uyandın mı? Oyun zamanı!", "Obi fell asleep... wake up? Game time!")
        }
        runCatching { Toast.makeText(context, MascotVoice.style(text, WordSiegeMascotSkin.ORB, mood.ordinal), Toast.LENGTH_LONG).show() }
    }
}

/** Alarm target: moves the launcher icon to the next mood while the player is away. */
class MascotIconMoodReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mood = intent.getStringExtra(MascotLauncherIcon.EXTRA_MOOD)
            ?.let { runCatching { MascotLauncherIcon.Mood.valueOf(it) }.getOrNull() } ?: return
        MascotLauncherIcon.onAlarm(context, mood)
    }
}
