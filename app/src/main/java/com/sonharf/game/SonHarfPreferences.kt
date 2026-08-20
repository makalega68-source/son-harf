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
    private const val DARK_MODE = "dark_mode_enabled"
    private const val LANGUAGE = "app_language"

    fun soundEnabled(context: Context): Boolean = prefs(context).getBoolean(SOUND, true)
    fun vibrationEnabled(context: Context): Boolean = prefs(context).getBoolean(VIBRATION, true)
    fun darkModeEnabled(context: Context): Boolean = prefs(context).getBoolean(DARK_MODE, false)
    fun language(context: Context): String = prefs(context).getString(LANGUAGE, "tr")?.takeIf { it in setOf("tr", "en") } ?: "tr"

    fun notificationsEnabled(context: Context): Boolean =
        gameInviteNotificationsEnabled(context) || friendRequestNotificationsEnabled(context) || systemNotificationsEnabled(context)

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

    fun setVibrationEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(VIBRATION, value).apply()

    fun setDarkModeEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(DARK_MODE, value).apply()
        SonHarfUiState.darkMode = value
    }

    fun setLanguage(context: Context, value: String) {
        val normalized = if (value.lowercase() == "en") "en" else "tr"
        prefs(context).edit().putString(LANGUAGE, normalized).apply()
        SonHarfUiState.language = normalized
    }

    fun setNotificationsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(NOTIFICATIONS, value).putBoolean(GAME_INVITE_NOTIFICATIONS, value).putBoolean(FRIEND_REQUEST_NOTIFICATIONS, value).putBoolean(SYSTEM_NOTIFICATIONS, value).apply()
    }

    fun setGameInviteNotificationsEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(GAME_INVITE_NOTIFICATIONS, value).apply()
    fun setFriendRequestNotificationsEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(FRIEND_REQUEST_NOTIFICATIONS, value).apply()
    fun setSystemNotificationsEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(SYSTEM_NOTIFICATIONS, value).apply()

    fun syncSound(context: Context) = SonHarfSoundFx.setEnabled(soundEnabled(context))

    fun syncUi(context: Context) {
        SonHarfUiState.darkMode = darkModeEnabled(context)
        SonHarfUiState.language = language(context)
    }

    fun hapticTap(context: Context) {
        if (!vibrationEnabled(context)) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java).defaultVibrator
            else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(12L, 28))
            else @Suppress("DEPRECATION") vibrator.vibrate(12L)
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
