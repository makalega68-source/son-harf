package com.sonharf.game

import android.os.Bundle
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

/**
 * Debug-only native Surface/IME regression surface.
 *
 * The production Eve3DStage keeps a stable full-screen viewport. Only the TextField overlay consumes
 * IME insets. GitHub Actions taps the field like a real user and verifies mInputShown=true before
 * hiding the keyboard with Back. This avoids programmatic keyboard-controller timing artifacts.
 */
class EveImeSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EveImeStressSurface()
            }
        }
    }
}

@Composable
private fun EveImeStressSurface() {
    var text by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        EveMascotRuntime.calm()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B4639)),
    ) {
        Eve3DStage(
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