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
 * Shared UI localization with a visible-brand compatibility layer.
 * Internal WordSiege/SonHarf technical identifiers intentionally remain unchanged.
 */
fun sh(tr: String, en: String): String {
    val localized = if (SonHarfUiState.isEnglish) en else tr
    val primaryTagline = if (SonHarfUiState.isEnglish) {
        "Build words, control territory, beat your rival"
    } else {
        "Kelimeyi kur, alanı kuşat, rakibini geç"
    }
    if (localized == "Kelimeyi Sürdür, Rakibini Geç" || localized == "Continue the Word, Beat Your Rival") {
        return primaryTagline
    }

    // The flagship home CTA keeps the product name dominant while making the action explicit.
    if (localized == "Kelime kur • alanı ele geçir • haritayı kontrol et") {
        return "OYNA • Kelime kur • alanı ele geçir • haritayı kontrol et"
    }
    if (localized == "Build words • capture territory • control the map") {
        return "PLAY • Build words • capture territory • control the map"
    }

    // Online move recap: territory gain is the signature siege feedback. Keep backend scoring untouched.
    val territoryScore = if (SonHarfUiState.isEnglish) {
        Regex("Territory \\+(\\d+)").find(localized)?.groupValues?.getOrNull(1)?.toIntOrNull()
    } else {
        Regex("Bölge \\+(\\d+)").find(localized)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
    if (territoryScore != null && territoryScore > 0 &&
        ((SonHarfUiState.isEnglish && localized.startsWith("Word +")) ||
            (!SonHarfUiState.isEnglish && localized.startsWith("Kelime +")))
    ) {
        return if (SonHarfUiState.isEnglish) "$localized  •  SIEGE +$territoryScore"
        else "$localized  •  KUŞATMA +$territoryScore"
    }

    return localized
        .replace("KELİME KUŞATMASI", "KELİME TAHTI")
        .replace("Kelime Kuşatması", "Kelime Tahtı")
        .replace("kelime kuşatması", "kelime tahtı")
        .replace("WORD SIEGE", "WORD THRONE")
        .replace("Word Siege", "Word Throne")
        .replace("word siege", "word throne")
        .replace("KUŞATMA SENİN!", "TAHT SENİN!")
        .replace("SIEGE WON!", "THE THRONE IS YOURS!")
}