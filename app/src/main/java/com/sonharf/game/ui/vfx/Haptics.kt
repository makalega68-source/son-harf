package com.sonharf.game.ui.vfx

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.sonharf.game.SonHarfPreferences

enum class HapticStrength { Light, Medium, Strong }

/**
 * Haptic controller respecting the user's sound/vibration preference.
 * Silent when the preference is off, or when the device has no vibrator.
 * (G3.0 rule: kapalıysa hiç titremez.)
 */
object Haptics {
    private fun vibrator(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun buzz(context: Context, strength: HapticStrength) {
        val allowed = runCatching { SonHarfPreferences.vibrationEnabled(context) }.getOrDefault(true)
        if (!allowed) return
        val v = vibrator(context) ?: return
        if (!v.hasVibrator()) return
        val (millis, amp) = when (strength) {
            HapticStrength.Light -> 12L to 60
            HapticStrength.Medium -> 25L to 120
            HapticStrength.Strong -> 55L to 220
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(millis, amp))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(millis)
            }
        }
    }
}
