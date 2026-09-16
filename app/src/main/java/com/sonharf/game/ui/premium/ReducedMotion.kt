package com.sonharf.game.ui.premium

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the system "remove animations" preference is on.
 * When true, animations should collapse to a short flash + text.
 * (G3.0 / G5.0 rules.)
 */
object ReducedMotion {
    fun isEnabled(context: Context): Boolean {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        return scale == 0f
    }
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { ReducedMotion.isEnabled(context) }
}
