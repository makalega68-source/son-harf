package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/**
 * Debug-only smoke surface for CI. It renders the exact interactive production Eve forest screen,
 * including the real Eve3DStage, progression UI and keyboard-aware chat surface.
 */
class Eve3DSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EveMascotRuntime.calm()
        setContent {
            MaterialTheme {
                EveForestScreen(onNavigateBack = { finish() })
            }
        }
    }
}
