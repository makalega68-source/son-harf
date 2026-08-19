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

    fun soundEnabled(context: Context): Boolean = prefs(context).getBoolean(SOUND, true)
    fun vibrationEnabled(context: Context): Boolean = prefs(context).getBoolean(VIBRATION, true)
    fun notificationsEnabled(context: Context): Boolean = prefs(context).getBoolean(NOTIFICATIONS, true)

    fun setSoundEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(SOUND, value).apply()
        SonHarfSoundFx.setEnabled(value)
    }

    fun setVibrationEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(VIBRATION, value).apply()

    fun setNotificationsEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(NOTIFICATIONS, value).apply()

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
