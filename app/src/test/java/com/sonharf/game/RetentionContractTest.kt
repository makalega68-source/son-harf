package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionContractTest {
    private fun source(path: String) = File(path).readText()

    @Test fun remindersAreScheduledOnlyWhileAwayAndCanBeTurnedOff() {
        val reminders = source("src/main/java/com/sonharf/game/ReminderNotifications.kt")
        val activity = source("src/main/java/com/sonharf/game/MainActivity.kt")
        val manifest = source("src/main/AndroidManifest.xml")
        val settings = source("src/main/java/com/sonharf/game/MainSettingsVipScreen.kt")
        val home = source("src/main/java/com/sonharf/game/PremiumHomeV3.kt")
        assertTrue(activity.contains("ReminderNotifications.onAppOpened(this)"))
        assertTrue(activity.contains("ReminderNotifications.onAppBackground(this)"))
        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(manifest.contains("android:name=\".ReminderReceiver\""))
        assertTrue(settings.contains("ReminderNotifications.setEnabled(context, it)"))
        assertTrue(home.contains("ReminderNotifications.rememberStreak(context, streakDays)"))
        // Streak reminder only for a streak worth protecting; nothing fires with reminders off.
        assertTrue(reminders.contains("prefs(context).getInt(KEY_STREAK, 0) < 2"))
        assertTrue(reminders.contains("if (!enabled(context)) return"))
        assertTrue(reminders.contains("DAILY_RACE(1, 12, 30)"))
        assertTrue(reminders.contains("GIFT(4, 18, 0)"))
        // No compat library needed: platform notification API (minSdk 26 always has channels).
        assertFalse(reminders.contains("NotificationCompat"))
    }

    @Test fun comebackGiftIsDecidedAndCreditedByTheServer() {
        val app = source("src/main/java/com/sonharf/game/StableV1App.kt")
        val backend = source("src/main/java/com/sonharf/game/data/PresenceBackend.kt")
        val migration = source("../supabase/migrations/20260927150000_comeback_gift_v1.sql")
        assertTrue(backend.contains("\"touch_presence_v1\""))
        assertTrue(app.contains("PresenceBackend.touch()"))
        assertTrue(app.contains("ComebackGiftDialog(gift)"))
        assertTrue(app.contains("ReminderNotifications.shouldAskPermission(context)"))
        assertTrue(migration.contains("v_days >= 3"))
        assertTrue(migration.contains("interval '14 days'"))
        assertTrue(migration.contains("'comeback_gift'"))
        assertTrue(migration.contains("revoke all on public.user_presence from anon, authenticated"))
        assertTrue(migration.contains("grant execute on function public.touch_presence_v1() to authenticated"))
    }
}
