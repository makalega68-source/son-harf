package com.obi.local

import android.content.Context
import android.graphics.Canvas

/** The game's board size, which the mascot uses to look at a cell. Obi has no board. */
internal object WordSiegeBoardSpec {
    const val Size = 15
    const val CenterRow = Size / 2
    const val CenterColumn = Size / 2
}

/** Obi sells no hats, so no bought hat is ever drawn; the mascot draws its own built-in ones. */
@Suppress("UNUSED_PARAMETER")
internal class MascotHatPainter(context: Context) {
    fun draws(hat: WordSiegeMascotHat): Boolean = false
    fun draw(canvas: Canvas, hat: WordSiegeMascotHat, now: Long) = Unit
}
