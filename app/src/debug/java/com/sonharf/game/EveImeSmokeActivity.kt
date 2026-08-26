package com.sonharf.game

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Debug-only native Surface/IME regression surface.
 *
 * Uses the exact production live Eve stage. Only the TextField overlay consumes IME insets, so CI
 * stresses Vulkan/Surface lifecycle without resizing the 3D viewport itself.
 */
class EveImeSmokeActivity : ComponentActivity() {
    private var lastImeVisible: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (visible != lastImeVisible) {
                lastImeVisible = visible
                Log.i("EveImeSmoke", "EVE_IME_VISIBLE=$visible")
            }
            insets
        }

        setContent {
            MaterialTheme {
                EveImeStressSurface()
            }
        }
        ViewCompat.requestApplyInsets(window.decorView)
    }
}

@Composable
private fun EveImeStressSurface() {
    var text by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        EveMascotRuntime.play(
            cue = EveAnimationCue.IDLE_LOOK_AROUND,
            returnToIdleAfterMs = 0,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B4639)),
    ) {
        EveLive3DStage(
            modifier = Modifier.fillMaxSize(),
            compact = false,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .padding(12.dp),
            color = Color(0xEE10261D),
            shape = RoundedCornerShape(24.dp),
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("EVE ile konuş...") },
                singleLine = true,
            )
        }
    }
}
