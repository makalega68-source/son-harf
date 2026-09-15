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

/**
 * Shared UI localization.
 *
 * Kelime Kuşatması is the canonical user-visible product brand. Older source strings may still
 * contain the previous Kelime Tahtı / Word Throne labels for compatibility with untouched screens,
 * but they are normalized here before rendering. Technical identifiers (WordSiege, package names,
 * database contracts and deep links) are intentionally unaffected.
 */
fun sh(tr: String, en: String): String {
    val localized = if (SonHarfUiState.isEnglish) en else tr
    val primaryTagline = if (SonHarfUiState.isEnglish) {
        "Build your word, beat your rival"
    } else {
        "Kelimeyi kur, rakibini geç"
    }
    if (localized == "Kelimeyi Sürdür, Rakibini Geç" || localized == "Continue the Word, Beat Your Rival") {
        return primaryTagline
    }

    if (localized == "Kelime kur • alanı ele geçir • haritayı kontrol et") {
        return "OYNA • Harflerini yerleştir, kelimeni oluştur"
    }
    if (localized == "Build words • capture territory • control the map") {
        return "PLAY • Place your tiles, build your word"
    }

    return localized
        .replace("KELİME TAHTI", "KELİME KUŞATMASI")
        .replace("Kelime Tahtı", "Kelime Kuşatması")
        .replace("WORD THRONE", "KELİME KUŞATMASI")
        .replace("Word Throne", "Kelime Kuşatması")
        .replace("TAHT SENİN!", "KUŞATMA SENİN!")
        .replace("THE THRONE IS YOURS!", "SIEGE WON!")
}
