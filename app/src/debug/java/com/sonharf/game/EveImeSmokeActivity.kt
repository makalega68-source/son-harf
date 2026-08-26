package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/**
 * Debug-only CI surface that exercises the production Eve room together with the software IME.
 * The workflow repeatedly opens/closes the keyboard while the real SceneView/GLB stage remains alive.
 */
class EveImeSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EveForestScreen(onNavigateBack = { finish() })
            }
        }
    }
}
