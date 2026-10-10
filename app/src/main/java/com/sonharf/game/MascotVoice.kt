package com.sonharf.game

/**
 * Mascot speech. Lines are shown exactly as written: no signature cries ("pıt pıt"), closing
 * sounds or dotted kaomoji faces. The mascot talks like a clear, helpful companion.
 */
internal object MascotVoice {
    /** Kept as the single entry point for mascot lines; returns the trimmed text unchanged. */
    @Suppress("UNUSED_PARAMETER")
    fun style(text: String, skin: WordSiegeMascotSkin, seed: Int): String = text.trim()
}
