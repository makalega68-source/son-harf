package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Runtime UI preferences shared by the whole Compose tree. */
object SonHarfUiState {
    var darkMode by mutableStateOf(false)
    var language by mutableStateOf("tr")
    var inMatch by mutableStateOf(false)
    var homeRequest by mutableStateOf(0)

    val isEnglish: Boolean get() = language == "en"
}

fun sh(tr: String, en: String): String = if (SonHarfUiState.isEnglish) en else tr
