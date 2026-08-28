package com.sonharf.game

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Debug-only Stage 9 smoke surface.
 *
 * Renders the real production MascotCompanionScreen with no backend dependency so CI can verify
 * Android IME behavior without mutating player/server state. The activity logs the IME inset and
 * the safe visible bottom used by the emulator assertion.
 */
class MascotCompanionImeSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val rootHeight = view.height
            val safeBottom = (rootHeight - imeInsets.bottom).coerceAtLeast(0)
            Log.i(
                TAG,
                "MASCOT_COMPANION_IME_VISIBLE=$imeVisible root_height=$rootHeight " +
                    "ime_bottom=${imeInsets.bottom} safe_bottom=$safeBottom",
            )
            insets
        }

        setContent {
            MaterialTheme {
                LaunchedEffect(Unit) {
                    Log.i(TAG, "MASCOT_COMPANION_SCREEN_READY")
                }
                MascotCompanionScreen(
                    backend = null,
                    onBack = {},
                    onOpenHistory = {},
                    onOpenRoom = {},
                    onOpenShop = {},
                )
            }
        }

        ViewCompat.requestApplyInsets(window.decorView)
    }

    private companion object {
        const val TAG = "MascotCompanionIme"
    }
}
