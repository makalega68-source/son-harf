package com.sonharf.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Store ids of the hats Obi can wear everywhere (bought with Son Coin). */
internal object MascotHats {
    const val BERET = "hat_beret"
    const val FLOWER = "hat_flower"
    const val WIZARD = "hat_wizard"
    const val TOP = "hat_top"
    val ids = setOf(BERET, FLOWER, WIZARD, TOP)

    fun hatFor(id: String?): WordSiegeMascotHat = when (id) {
        BERET -> WordSiegeMascotHat.BERET
        FLOWER -> WordSiegeMascotHat.FLOWER
        WIZARD -> WordSiegeMascotHat.WIZARD
        TOP -> WordSiegeMascotHat.TOP_HAT
        else -> WordSiegeMascotHat.NONE
    }
}

/**
 * Draws the purchasable hats (and the victory crown) in the mascot view's art space (1254 px,
 * orb centre 660/641), already translated/rotated by the caller so the hat sits on the crown of
 * the head. The hat IS the store image: the same PNG the shop sells is drawn on Obi, so what the
 * player buys and what Obi wears are identical. Shared by the live mascot and the store preview.
 */
internal class MascotHatPainter(context: Context) {
    private val app = context.applicationContext
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val src = Rect()
    private val dst = RectF()

    /**
     * Where each image sits on the head. [left]..[bottom] is the opaque part of the PNG as a
     * fraction of its side; [width] is the hat's width in art space, [baseY] where its bottom
     * edge rests, and [squash] flattens the flat-lying flower wreath into a crown seen from the side.
     */
    private class Placement(
        val res: Int,
        val left: Float, val top: Float, val right: Float, val bottom: Float,
        val width: Float, val baseY: Float, val squash: Float = 1f,
    )

    private fun placement(hat: WordSiegeMascotHat): Placement? = when (hat) {
        // Worn beret: its dark inside is cut away so Obi's head fills the opening instead of it facing out.
        WordSiegeMascotHat.BERET -> Placement(R.drawable.hat_beret_worn, 19 / 512f, 75 / 512f, 499 / 512f, 440 / 512f, 480f, 385f)
        // The wreath rests down on the head (not floating like a halo): wider, flatter, lower.
        WordSiegeMascotHat.FLOWER -> Placement(R.drawable.store_art_hat_flower, 14 / 512f, 50 / 512f, 497 / 512f, 447 / 512f, 610f, 395f, squash = .46f)
        WordSiegeMascotHat.WIZARD -> Placement(R.drawable.store_art_hat_wizard, 23 / 512f, 23 / 512f, 489 / 512f, 475 / 512f, 390f, 360f)
        WordSiegeMascotHat.TOP_HAT -> Placement(R.drawable.store_art_hat_top, 41 / 512f, 36 / 512f, 496 / 512f, 464 / 512f, 390f, 350f)
        WordSiegeMascotHat.CROWN -> Placement(R.drawable.store_art_victory_crown, 9 / 384f, 13 / 384f, 373 / 384f, 368 / 384f, 380f, 350f)
        else -> null
    }

    fun draws(hat: WordSiegeMascotHat): Boolean = placement(hat) != null

    fun draw(canvas: Canvas, hat: WordSiegeMascotHat, @Suppress("UNUSED_PARAMETER") now: Long) {
        val place = placement(hat) ?: return
        val bitmap = HatBitmaps.get(app, place.res) ?: return
        src.set(
            (place.left * bitmap.width).toInt(),
            (place.top * bitmap.height).toInt(),
            (place.right * bitmap.width).toInt(),
            (place.bottom * bitmap.height).toInt(),
        )
        val height = place.width * src.height() / src.width().toFloat() * place.squash
        dst.set(560f - place.width / 2f, place.baseY - height, 560f + place.width / 2f, place.baseY)
        canvas.drawBitmap(bitmap, src, dst, paint)
    }
}

/** Hat images are decoded once per process (at most 512 px) and shared by every mascot. */
private object HatBitmaps {
    private val cache = HashMap<Int, Bitmap?>()

    @Synchronized
    fun get(context: Context, res: Int): Bitmap? = cache.getOrPut(res) {
        runCatching {
            BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply { inScaled = false })
        }.getOrNull()
    }
}

/** Store/profile preview: smiling Obi wearing the hat, drawn with the live painter. */
@Composable
internal fun ObiHatPreview(hatId: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val painter = remember { MascotHatPainter(context) }
    val hat = MascotHats.hatFor(hatId)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.obi_launcher_happy),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(top = 10.dp),
            contentScale = ContentScale.Fit,
        )
        ComposeCanvas(Modifier.fillMaxSize().padding(top = 10.dp)) {
            // The launcher art is square; its orb spans ~62% of the side. Map the mascot's art
            // space (orb radius ~405 around 660/641) onto it, then pose the hat like the live view.
            val side = minOf(size.width, size.height)
            val scale = side * .31f / 405f
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f
            drawIntoCanvas { c ->
                val n = c.nativeCanvas
                n.save()
                n.translate(left + side * .5f, top + side * .51f)
                n.scale(scale, scale)
                n.translate(-660f, -641f)
                n.translate(0f, 28f)
                n.rotate(-14f, 560f, 250f)
                painter.draw(n, hat, 0L)
                n.restore()
            }
        }
    }
}
