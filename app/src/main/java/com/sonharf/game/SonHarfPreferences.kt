package com.sonharf.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SonHarfPreferences {
    private const val FILE = "son_harf_preferences"
    private const val SOUND = "sound_enabled"
    private const val VIBRATION = "vibration_enabled"
    private const val NOTIFICATIONS = "notifications_enabled"
    private const val GAME_INVITE_NOTIFICATIONS = "game_invite_notifications_enabled"
    private const val FRIEND_REQUEST_NOTIFICATIONS = "friend_request_notifications_enabled"
    private const val SYSTEM_NOTIFICATIONS = "system_notifications_enabled"

    fun soundEnabled(context: Context): Boolean = prefs(context).getBoolean(SOUND, true)
    fun vibrationEnabled(context: Context): Boolean = prefs(context).getBoolean(VIBRATION, true)

    // Kept for backward compatibility with older code. It now reflects whether
    // at least one notification category is enabled instead of acting as one
    // shared switch for every category.
    fun notificationsEnabled(context: Context): Boolean =
        gameInviteNotificationsEnabled(context) ||
            friendRequestNotificationsEnabled(context) ||
            systemNotificationsEnabled(context)

    fun gameInviteNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(GAME_INVITE_NOTIFICATIONS, prefs(context).getBoolean(NOTIFICATIONS, true))

    fun friendRequestNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(FRIEND_REQUEST_NOTIFICATIONS, prefs(context).getBoolean(NOTIFICATIONS, true))

    fun systemNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(SYSTEM_NOTIFICATIONS, prefs(context).getBoolean(NOTIFICATIONS, true))

    fun setSoundEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(SOUND, value).apply()
        SonHarfSoundFx.setEnabled(value)
    }

    fun setVibrationEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(VIBRATION, value).apply()

    // Backward-compatible master setter for callers that intentionally want to
    // set all categories together. The settings screen no longer uses it.
    fun setNotificationsEnabled(context: Context, value: Boolean) {
        prefs(context).edit()
            .putBoolean(NOTIFICATIONS, value)
            .putBoolean(GAME_INVITE_NOTIFICATIONS, value)
            .putBoolean(FRIEND_REQUEST_NOTIFICATIONS, value)
            .putBoolean(SYSTEM_NOTIFICATIONS, value)
            .apply()
    }

    fun setGameInviteNotificationsEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(GAME_INVITE_NOTIFICATIONS, value).apply()

    fun setFriendRequestNotificationsEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(FRIEND_REQUEST_NOTIFICATIONS, value).apply()

    fun setSystemNotificationsEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(SYSTEM_NOTIFICATIONS, value).apply()

    fun syncSound(context: Context) = SonHarfSoundFx.setEnabled(soundEnabled(context))

    fun hapticTap(context: Context) {
        if (!vibrationEnabled(context)) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12L, 28))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(12L)
            }
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
