package com.sonharf.game

/**
 * Son Harf gameplay audio is intentionally disabled.
 *
 * The public API is preserved so existing callers remain binary/source compatible,
 * but no method may create AudioTrack, MediaPlayer, SoundPool, threads, or audio output.
 * Haptic feedback is managed separately by SonHarfPreferences and is unaffected.
 */
object SonHarfSoundFx {
    @Suppress("UNUSED_PARAMETER")
    fun setEnabled(value: Boolean) = Unit

    fun tap() = Unit
    fun softNotify() = Unit
    fun wordAccepted() = Unit
    fun warning() = Unit
    fun bonus() = Unit
    fun victory() = Unit
    fun defeat() = Unit
    fun countdown() = Unit
    fun fireworks() = Unit
}
