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
 * Keeps the launcher mascot in sync with the time since the player last opened the app.
 *
 * Requested five-day cycle:
 * day 0 = happy, day 1 = sad, days 2-3 = angry, day 4 = closed eyes, day 5 = happy again.
 * The same cycle then repeats indefinitely. The existing CURIOUS and CRYING aliases are retained
 * only for upgrade compatibility and are never selected by the new cycle.
 */
internal object MascotLauncherIcon {
    enum class Mood(val alias: String) {
        HAPPY(".LauncherMascotHappy"),
        CURIOUS(".LauncherMascotCurious"),
        SAD(".LauncherMascotSad"),
        ANGRY(".LauncherMascotAngry"),
        SLEEPY(".LauncherMascotSleepy"),
        CRYING(".LauncherMascotCrying"),
    }

    private const val PREFS = "mascot_launcher_icon"
    private const val KEY_LAST_OPEN = "last_open"
    private const val KEY_MOOD = "mood"
    private const val ALARM_REQUEST_CODE = 4_100
    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    private const val CYCLE_DAYS = 5L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(context: Context): Mood =
        runCatching { Mood.valueOf(prefs(context).getString(KEY_MOOD, Mood.HAPPY.name)!!) }
            .getOrDefault(Mood.HAPPY)

    /** App came to the foreground: this visit always resets the launcher face to happy. */
    fun onAppOpened(context: Context) {
        val before = current(context)
        prefs(context).edit().putLong(KEY_LAST_OPEN, System.currentTimeMillis()).apply()
        cancelAlarms(context)
        runCatching { apply(context, Mood.HAPPY) }
        if (before != Mood.HAPPY) greetBack(context, before)
    }

    /** App went to the background: keep the happy face and schedule only the next transition. */
    fun onAppBackground(context: Context) {
        runCatching { apply(context, Mood.HAPPY) }

        val storedLastOpen = prefs(context).getLong(KEY_LAST_OPEN, 0L)
        val lastOpen = if (storedLastOpen > 0L) {
            storedLastOpen
        } else {
            System.currentTimeMillis().also {
                prefs(context).edit().putLong(KEY_LAST_OPEN, it).apply()
            }
        }

        cancelAlarms(context)
        scheduleNextTransition(context, lastOpen, System.currentTimeMillis())
    }

    /**
     * Alarm target. The mood is calculated from actual elapsed time, so a delayed alarm still lands
     * directly on the correct face instead of replaying stale intermediate states.
     */
    fun onAlarm(context: Context) {
        val lastOpen = prefs(context).getLong(KEY_LAST_OPEN, 0L)
        if (lastOpen <= 0L) return

        val now = System.currentTimeMillis()
        val awayDays = ((now - lastOpen).coerceAtLeast(0L)) / DAY_MILLIS
        runCatching { apply(context, moodForAwayDays(awayDays)) }
        scheduleNextTransition(context, lastOpen, now)
    }

    internal fun moodForAwayDays(daysAway: Long): Mood = when (daysAway.coerceAtLeast(0L) % CYCLE_DAYS) {
        0L -> Mood.HAPPY
        1L -> Mood.SAD
        2L, 3L -> Mood.ANGRY
        4L -> Mood.SLEEPY
        else -> Mood.HAPPY
    }

    /** Absolute elapsed-day boundary at which the visible face next changes. */
    internal fun nextTransitionDay(daysAway: Long): Long {
        val safeDays = daysAway.coerceAtLeast(0L)
        return safeDays + when (safeDays % CYCLE_DAYS) {
            2L -> 2L // Angry intentionally covers both day 2 and the unspecified day 3.
            else -> 1L
        }
    }

    private fun scheduleNextTransition(context: Context, lastOpen: Long, now: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val awayDays = ((now - lastOpen).coerceAtLeast(0L)) / DAY_MILLIS
        val nextDay = nextTransitionDay(awayDays)
        val triggerAt = lastOpen + nextDay * DAY_MILLIS

        runCatching {
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC,
                triggerAt,
                pending(context, ALARM_REQUEST_CODE),
            )
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

        // Enable the new face first so the app always keeps one launcher entry, then hide the rest.
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

    private fun pending(context: Context, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, MascotIconMoodReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Cancels the new single alarm plus all request codes used by the previous finite mood system. */
    private fun cancelAlarms(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        (ALARM_REQUEST_CODE until ALARM_REQUEST_CODE + Mood.entries.size).forEach { requestCode ->
            runCatching { alarms.cancel(pending(context, requestCode)) }
        }
    }

    private fun greetBack(context: Context, mood: Mood) {
        val text = when (mood) {
            Mood.CURIOUS -> sh("Obi kapıyı gözlüyordu... hoş geldin!", "Obi was watching the door... welcome back!")
            Mood.SAD -> sh("Obi seni çok özlemişti... geri geldin!", "Obi missed you so much... you're back!")
            Mood.ANGRY -> sh("Hmph! Obi küsmüştü ama tamam, barıştık!", "Hmph! Obi was sulking, but okay, friends again!")
            Mood.SLEEPY -> sh("Obi uyuyakalmıştı... uyandın mı? Oyun zamanı!", "Obi fell asleep... wake up? Game time!")
            Mood.CRYING -> sh("Obi seni bekliyordu... hoş geldin!", "Obi was waiting for you... welcome back!")
            Mood.HAPPY -> return
        }
        runCatching {
            Toast.makeText(
                context,
                MascotVoice.style(text, WordSiegeMascotSkin.ORB, mood.ordinal),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}

/** Alarm target: recalculates the correct launcher face from the last-open timestamp. */
class MascotIconMoodReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MascotLauncherIcon.onAlarm(context)
    }
}
