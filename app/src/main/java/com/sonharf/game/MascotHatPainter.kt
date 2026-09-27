package com.sonharf.game

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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
import kotlin.math.cos
import kotlin.math.sin

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
 * Draws the purchasable hats in the mascot view's art space (1254 px, orb centre 660/641),
 * already translated/rotated by the caller so the hat sits on the crown of the head.
 * Shared by the live mascot and the store preview so both always look the same.
 */
internal class MascotHatPainter {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeJoin = Paint.Join.ROUND
        color = 0x55000000
    }
    private val path = Path()
    private val oval = RectF()

    fun draw(canvas: Canvas, hat: WordSiegeMascotHat, now: Long) {
        when (hat) {
            WordSiegeMascotHat.BERET -> beret(canvas)
            WordSiegeMascotHat.FLOWER -> flowers(canvas, now)
            WordSiegeMascotHat.WIZARD -> wizard(canvas, now)
            WordSiegeMascotHat.TOP_HAT -> topHat(canvas)
            else -> Unit
        }
    }

    private fun beret(canvas: Canvas) {
        oval.set(390f, 120f, 740f, 280f)
        fill.shader = LinearGradient(0f, 120f, 0f, 280f, 0xFFE8505B.toInt(), 0xFFA3222F.toInt(), Shader.TileMode.CLAMP)
        canvas.drawOval(oval, fill)
        fill.shader = null
        canvas.drawOval(oval, line)
        // headband and the little stalk on top
        fill.color = 0xFF7E1823.toInt()
        oval.set(430f, 236f, 700f, 280f)
        canvas.drawOval(oval, fill)
        fill.color = 0xFFA3222F.toInt()
        canvas.drawRoundRect(RectF(548f, 86f, 572f, 136f), 12f, 12f, fill)
        fill.color = 0x55FFFFFF
        oval.set(450f, 145f, 590f, 185f)
        canvas.drawOval(oval, fill)
    }

    private fun flowers(canvas: Canvas, now: Long) {
        // Leaves first, then a ring of daisies and roses along the crown.
        fill.color = 0xFF4FAE62.toInt()
        for (i in 0 until 8) {
            val t = i / 7f
            val x = 390f + t * 340f
            val y = 250f - sin(t * Math.PI).toFloat() * 60f
            canvas.save()
            canvas.rotate(-40f + t * 80f, x, y)
            oval.set(x - 16f, y - 44f, x + 16f, y + 6f)
            canvas.drawOval(oval, fill)
            canvas.restore()
        }
        val colors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFF8FB8.toInt(), 0xFFFFD35C.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF6F91.toInt(), 0xFFB9A6FF.toInt())
        for (i in 0 until 6) {
            val t = (i + .5f) / 6f
            val x = 400f + t * 320f
            val y = 244f - sin(t * Math.PI).toFloat() * 62f + sin(now / 600f + i) * 2f
            val r = if (i % 2 == 0) 30f else 25f
            fill.color = colors[i]
            for (p in 0 until 6) {
                val a = p * Math.PI / 3
                canvas.drawCircle(x + cos(a).toFloat() * r * .62f, y + sin(a).toFloat() * r * .62f, r * .48f, fill)
            }
            fill.color = if (colors[i] == 0xFFFFD35C.toInt()) 0xFFE0892E.toInt() else 0xFFFFC43D.toInt()
            canvas.drawCircle(x, y, r * .36f, fill)
        }
    }

    private fun wizard(canvas: Canvas, now: Long) {
        // Brim
        oval.set(360f, 222f, 760f, 290f)
        fill.color = 0xFF3B2A86.toInt()
        canvas.drawOval(oval, fill)
        canvas.drawOval(oval, line)
        // Cone with a floppy tip
        path.rewind()
        path.moveTo(440f, 256f)
        path.cubicTo(480f, 150f, 520f, 40f, 600f, -10f)
        path.cubicTo(640f, -30f, 690f, 0f, 700f, 30f)
        path.cubicTo(650f, 20f, 630f, 60f, 640f, 110f)
        path.lineTo(680f, 256f)
        path.close()
        fill.shader = LinearGradient(440f, 0f, 700f, 256f, 0xFF7B5CFF.toInt(), 0xFF4A2FA8.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, fill)
        fill.shader = null
        canvas.drawPath(path, line)
        // Gold band
        fill.color = 0xFFFFC94A.toInt()
        canvas.drawRoundRect(RectF(446f, 210f, 676f, 244f), 14f, 14f, fill)
        // Twinkling stars
        val twinkle = .75f + .25f * sin(now / 380f)
        star(canvas, 540f, 150f, 20f * twinkle, 0xFFFFF1A8.toInt())
        star(canvas, 610f, 90f, 14f * (1.6f - twinkle), 0xFFFFF1A8.toInt())
        star(canvas, 590f, 190f, 11f * twinkle, 0xFFFFFFFF.toInt())
        star(canvas, 700f, 30f, 12f, 0xFFFFE08A.toInt())
    }

    private fun topHat(canvas: Canvas) {
        // Crown of the hat
        fill.shader = LinearGradient(460f, 0f, 660f, 0f, intArrayOf(0xFF1B1D24.toInt(), 0xFF3A3E4A.toInt(), 0xFF15161B.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(462f, 50f, 658f, 246f), 14f, 14f, fill)
        fill.shader = null
        canvas.drawRoundRect(RectF(462f, 50f, 658f, 246f), 14f, 14f, line)
        fill.color = 0xFF2A2D36.toInt()
        oval.set(462f, 36f, 658f, 66f)
        canvas.drawOval(oval, fill)
        // Gold band with a small gem
        fill.color = 0xFFE0B45C.toInt()
        canvas.drawRect(462f, 190f, 658f, 222f, fill)
        fill.color = 0xFF56E4F7.toInt()
        canvas.drawCircle(600f, 206f, 11f, fill)
        // Brim
        fill.color = 0xFF111217.toInt()
        oval.set(392f, 228f, 728f, 278f)
        canvas.drawOval(oval, fill)
        canvas.drawOval(oval, line)
        fill.color = 0x33FFFFFF
        canvas.drawRect(480f, 64f, 500f, 186f, fill)
    }

    private fun star(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        fill.color = color
        path.rewind()
        path.moveTo(x, y - r)
        path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r)
        path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        path.close()
        canvas.drawPath(path, fill)
    }
}

/** Store/profile preview: smiling Obi wearing the hat, drawn with the live painter. */
@Composable
internal fun ObiHatPreview(hatId: String, modifier: Modifier = Modifier) {
    val painter = remember { MascotHatPainter() }
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
