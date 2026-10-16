package com.obi.local

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

/**
 * Obi's own wardrobe hats, drawn in the rig's hat space (head top around x 400–720, brim near y 250).
 * The rig itself draws the party cone and the crown.
 */
internal class MascotHatPainter(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 9f; color = 0x66000000; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()

    fun draws(hat: WordSiegeMascotHat): Boolean = hat == WordSiegeMascotHat.BERET || hat == WordSiegeMascotHat.FLOWER ||
        hat == WordSiegeMascotHat.WIZARD || hat == WordSiegeMascotHat.TOP_HAT

    fun draw(canvas: Canvas, hat: WordSiegeMascotHat, now: Long) {
        when (hat) {
            WordSiegeMascotHat.BERET -> beret(canvas)
            WordSiegeMascotHat.FLOWER -> flowers(canvas, now)
            WordSiegeMascotHat.WIZARD -> wizard(canvas, now)
            WordSiegeMascotHat.TOP_HAT -> topHat(canvas)
            else -> Unit
        }
        fill.shader = null
    }

    private fun beret(canvas: Canvas) {
        fill.shader = LinearGradient(420f, 150f, 700f, 260f, 0xFFF0566F.toInt(), 0xFFB82E4B.toInt(), Shader.TileMode.CLAMP)
        rect.set(405f, 140f, 720f, 262f)
        canvas.drawOval(rect, fill)
        canvas.drawOval(rect, line)
        fill.shader = null
        fill.color = 0xFF9E2440.toInt()
        rect.set(430f, 228f, 695f, 262f)
        canvas.drawRoundRect(rect, 20f, 20f, fill)
        fill.color = 0xFFB82E4B.toInt()
        canvas.drawCircle(575f, 140f, 18f, fill)
        fill.color = 0x40FFFFFF
        rect.set(470f, 160f, 600f, 195f)
        canvas.drawOval(rect, fill)
    }

    private fun flowers(canvas: Canvas, now: Long) {
        val colors = intArrayOf(0xFFFF8FB8.toInt(), 0xFFFFE07A.toInt(), 0xFFFFFFFF.toInt(), 0xFFB79BFF.toInt(), 0xFFFF9F6B.toInt())
        // Leaves along the band first.
        fill.color = 0xFF5FC27E.toInt()
        for (i in 0 until 6) {
            val x = 430f + i * 52f
            rect.set(x, 232f, x + 46f, 256f)
            canvas.drawOval(rect, fill)
        }
        for (i in 0 until 5) {
            val x = 440f + i * 60f
            val y = 236f - sin((i / 4f) * Math.PI).toFloat() * 26f
            val sway = sin(now / 600f + i) * 3f
            fill.color = colors[i]
            for (p in 0 until 5) {
                val angle = p * 72f * (Math.PI / 180f).toFloat() + now / 4000f
                canvas.drawCircle(x + sway + cos(angle) * 17f, y + sin(angle) * 17f, 14f, fill)
            }
            fill.color = 0xFFFFC23D.toInt()
            canvas.drawCircle(x + sway, y, 11f, fill)
        }
    }

    private fun wizard(canvas: Canvas, now: Long) {
        path.rewind()
        path.moveTo(440f, 250f)
        path.quadTo(520f, 140f, 560f, 30f)
        path.quadTo(600f, 0f, 640f, 18f)
        path.quadTo(600f, 40f, 590f, 80f)
        path.quadTo(640f, 170f, 680f, 250f)
        path.close()
        fill.shader = LinearGradient(440f, 30f, 680f, 250f, 0xFF5B5FD6.toInt(), 0xFF2E2A7A.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, fill)
        canvas.drawPath(path, line)
        fill.shader = null
        fill.color = 0xFF3A3592.toInt()
        rect.set(395f, 228f, 725f, 274f)
        canvas.drawOval(rect, fill)
        canvas.drawOval(rect, line)
        // Twinkling stars and a moon.
        val twinkle = (sin(now / 300f) * .5f + .5f)
        fill.color = 0xFFFFE27A.toInt()
        star(canvas, 530f, 180f, 18f + twinkle * 5f)
        star(canvas, 600f, 120f, 13f + (1f - twinkle) * 4f)
        star(canvas, 560f, 220f, 10f)
        canvas.drawCircle(585f, 175f, 16f, fill)
        fill.color = 0xFF3F3A99.toInt()
        canvas.drawCircle(593f, 170f, 14f, fill)
    }

    private fun star(canvas: Canvas, x: Float, y: Float, r: Float) {
        path.rewind()
        path.moveTo(x, y - r)
        path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r)
        path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        path.close()
        canvas.drawPath(path, fill)
    }

    private fun topHat(canvas: Canvas) {
        fill.shader = LinearGradient(460f, 60f, 660f, 60f, intArrayOf(0xFF23232E.toInt(), 0xFF44445A.toInt(), 0xFF1A1A22.toInt()), null, Shader.TileMode.CLAMP)
        rect.set(462f, 55f, 658f, 248f)
        canvas.drawRoundRect(rect, 18f, 18f, fill)
        canvas.drawRoundRect(rect, 18f, 18f, line)
        fill.shader = null
        fill.color = 0xFFD94A64.toInt()
        rect.set(462f, 196f, 658f, 232f)
        canvas.drawRect(rect, fill)
        fill.color = 0xFF1E1E28.toInt()
        rect.set(400f, 230f, 720f, 270f)
        canvas.drawOval(rect, fill)
        canvas.drawOval(rect, line)
        fill.color = 0x30FFFFFF
        rect.set(485f, 70f, 515f, 190f)
        canvas.drawRoundRect(rect, 12f, 12f, fill)
    }
}
