package com.sonharf.game

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.UUID

internal data class VoiceWordInput(
    val supported: Boolean,
    val launch: () -> Unit,
)

@Composable
internal fun rememberVoiceWordInput(
    language: String,
    onRecognized: (text: String, requestId: String) -> Unit,
): VoiceWordInput {
    val context = LocalContext.current
    val recognizerIntent = remember(language) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language.lowercase() == "tr") "tr-TR" else "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (language.lowercase() == "tr") "Kelimeyi söyle" else "Say the word",
            )
        }
    }
    val supported = remember(recognizerIntent) {
        recognizerIntent.resolveActivity(context.packageManager) != null
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val recognized = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (recognized.isNotBlank()) {
            onRecognized(recognized, UUID.randomUUID().toString())
        }
    }
    return VoiceWordInput(
        supported = supported,
        launch = {
            if (supported) runCatching { launcher.launch(recognizerIntent) }
        },
    )
}
