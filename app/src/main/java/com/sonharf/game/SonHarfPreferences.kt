package com.sonharf.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SonHarfPreferences {
    private const val FILE = "son_harf_preferences"
    private const val SOUND = "sound_enabled"
    private const val MUSIC = "music_enabled"
    private const val VIBRATION = "vibration_enabled"
    private const val NOTIFICATIONS = "notifications_enabled"
    private const val GAME_INVITES = "game_invites_enabled"
    private const val FRIEND_REQUESTS = "friend_requests_enabled"
    private const val SYSTEM_NOTIFICATIONS = "system_notifications_enabled"
    private const val DARK_MODE = "dark_mode_enabled"
    private const val LANGUAGE = "language"

    fun soundEnabled(context: Context): Boolean = prefs(context).getBoolean(SOUND, true)
    fun musicEnabled(context: Context): Boolean = prefs(context).getBoolean(MUSIC, false)
    fun vibrationEnabled(context: Context): Boolean = prefs(context).getBoolean(VIBRATION, true)
    fun notificationsEnabled(context: Context): Boolean = prefs(context).getBoolean(NOTIFICATIONS, true)
    fun gameInvitesEnabled(context: Context): Boolean = prefs(context).getBoolean(GAME_INVITES, true)
    fun friendRequestsEnabled(context: Context): Boolean = prefs(context).getBoolean(FRIEND_REQUESTS, true)
    fun systemNotificationsEnabled(context: Context): Boolean = prefs(context).getBoolean(SYSTEM_NOTIFICATIONS, true)
    fun darkModeEnabled(context: Context): Boolean = prefs(context).getBoolean(DARK_MODE, true)
    fun language(context: Context): String = prefs(context).getString(LANGUAGE, "tr") ?: "tr"

    fun setSoundEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(SOUND, value).apply()
        SonHarfSoundFx.setEnabled(value)
    }
    fun setMusicEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(MUSIC, value).apply()
    fun setVibrationEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(VIBRATION, value).apply()
    fun setNotificationsEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(NOTIFICATIONS, value).apply()
    fun setGameInvitesEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(GAME_INVITES, value).apply()
    fun setFriendRequestsEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(FRIEND_REQUESTS, value).apply()
    fun setSystemNotificationsEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(SYSTEM_NOTIFICATIONS, value).apply()
    fun setDarkModeEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(DARK_MODE, value).apply()
    fun setLanguage(context: Context, value: String) = prefs(context).edit().putString(LANGUAGE, if (value == "en") "en" else "tr").apply()

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
