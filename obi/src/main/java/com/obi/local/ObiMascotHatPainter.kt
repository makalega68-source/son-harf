package com.obi.local

import android.content.Context
import android.graphics.Canvas

/** The standalone Obi starts hat-free; this keeps the shared mascot rig independent of game cosmetics. */
internal class MascotHatPainter(@Suppress("UNUSED_PARAMETER") context: Context) {
    fun draws(@Suppress("UNUSED_PARAMETER") hat: WordSiegeMascotHat): Boolean = false
    fun draw(@Suppress("UNUSED_PARAMETER") canvas: Canvas, @Suppress("UNUSED_PARAMETER") hat: WordSiegeMascotHat, @Suppress("UNUSED_PARAMETER") now: Long) = Unit
}
