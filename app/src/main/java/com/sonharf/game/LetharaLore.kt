package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Compatibility color palette kept for older archived UI files.
 * It contains no active product lore.
 */
internal object LetharaPalette {
    val Night = Color(0xFF071229)
    val Night2 = Color(0xFF0D1A3A)
    val Panel = Color(0xE6142447)
    val PanelStrong = Color(0xF20E1A36)
    val Cyan = Color(0xFF56D6FF)
    val Violet = Color(0xFF9C7CFF)
    val Gold = Color(0xFFFFD36A)
    val Text = Color(0xFFF4F0FF)
    val Muted = Color(0xFFB8B5D4)
    val Green = Color(0xFF62D9A7)
    val Red = Color(0xFFFF7D96)
}

internal data class WizardLoreCharacter(
    val key: String,
    val name: String,
    val titleTr: String,
    val titleEn: String,
    val nameMeaningTr: String,
    val nameMeaningEn: String,
    val archetypeTr: String,
    val archetypeEn: String,
    val temperamentTr: String,
    val temperamentEn: String,
    val color: Color,
    val mascotId: String?,
    val whisperTr: List<String>,
    val whisperEn: List<String>,
)

internal data class WizardLoreChapter(
    val id: String,
    val order: Int,
    val titleTr: String,
    val titleEn: String,
    val unlockLevel: Int,
    val summaryTr: String,
    val summaryEn: String,
    val bodyTr: String,
    val bodyEn: String,
)

/**
 * Legacy API compatibility only.
 *
 * The old mascot story, named world, multiple characters, memory chapters and enemy canon have
 * been removed from the product. Older archived screens can still compile against this object,
 * but they receive only the single neutral game companion and no story chapters.
 */
internal object LetharaLore {
    const val WORLD = ""
    const val ENEMY = ""
    const val PLAYER_ROLE_TR = "Oyuncu"
    const val PLAYER_ROLE_EN = "Player"

    val characters = listOf(
        WizardLoreCharacter(
            key = "companion",
            name = "Dost",
            titleTr = "Oyun Yardımcısı",
            titleEn = "Game Companion",
            nameMeaningTr = "",
            nameMeaningEn = "",
            archetypeTr = "Eğlenceli • Destekleyici • Kısa",
            archetypeEn = "Playful • Supportive • Concise",
            temperamentTr = "Eğlenceli, kısa ve destekleyici.",
            temperamentEn = "Playful, concise and supportive.",
            color = Color(0xFF1769E0),
            mascotId = MascotCatalog.CHIBI_WIZARD_ID,
            whisperTr = listOf(
                "Önce son harfi kontrol et.",
                "Bir güvenli kelimeyi yedekte tut.",
                "Sakin oyna, doğru kelimeyi seç.",
            ),
            whisperEn = listOf(
                "Check the final letter first.",
                "Keep one safe backup word.",
                "Stay calm and choose a valid word.",
            ),
        ),
    )

    val chapters: List<WizardLoreChapter> = emptyList()
    val introTr = ""
    val introEn = ""

    fun character(key: String?): WizardLoreCharacter = characters.first()

    fun characterForMascot(mascotId: String?): WizardLoreCharacter = characters.first()

    fun randomWhisper(character: WizardLoreCharacter, language: String, seed: Int): String {
        val list = if (language == "en") character.whisperEn else character.whisperTr
        return list[Math.floorMod(seed, list.size)]
    }
}
