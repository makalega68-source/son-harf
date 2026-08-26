package com.sonharf.game

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Debug-only native Surface/IME regression surface.
 *
 * It intentionally keeps the exact production Eve3DStage alive at a stable full-screen size while
 * only the chat overlay consumes IME insets. The activity then opens/closes the real software IME
 * five times and records whether Android actually reported five visible IME transitions.
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
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    var text by remember { mutableStateOf("") }
    var imeOpenCount by remember { mutableIntStateOf(0) }
    var lastImeVisible by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeVisible = imeBottom > 0

    LaunchedEffect(imeVisible) {
        if (imeVisible && !lastImeVisible) {
            imeOpenCount += 1
            Log.i("EveImeSmoke", "IME_OPEN_$imeOpenCount bottom=$imeBottom")
        }
        lastImeVisible = imeVisible
    }

    LaunchedEffect(Unit) {
        EveMascotRuntime.calm()
        delay(6_000)
        repeat(5) { round ->
            focusRequester.requestFocus()
            keyboardController?.show()
            Log.i("EveImeSmoke", "REQUEST_OPEN_${round + 1}")
            delay(1_800)

            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            Log.i("EveImeSmoke", "REQUEST_CLOSE_${round + 1}")
            delay(1_800)
        }
        delay(1_000)
        completed = true
        val marker = if (imeOpenCount >= 5) "EVE_IME_PASS_5" else "EVE_IME_INCOMPLETE_$imeOpenCount"
        Log.i("EveImeSmoke", marker)
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
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = when {
                        completed && imeOpenCount >= 5 -> "EVE_IME_PASS_5"
                        completed -> "EVE_IME_INCOMPLETE_$imeOpenCount"
                        else -> "IME stress: $imeOpenCount/5"
                    },
                    color = Color.White,
                )
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("EVE IME smoke") },
                    singleLine = true,
                )
            }
        }
    }
}