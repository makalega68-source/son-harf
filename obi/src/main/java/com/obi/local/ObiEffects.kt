package com.obi.local

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Full-screen moments Obi can throw over the phone for a few seconds; touches pass through. */
internal enum class ObiEffect(val label: String, val millis: Long) {
    CONFETTI("konfeti", 3_600L),
    HEARTS("kalpler", 3_800L),
    STARS("yıldızlar", 3_400L),
    SNOW("kar", 5_500L),
    BUBBLES("baloncuklar", 4_600L),
    FIREWORKS("havai fişek", 4_400L),
    PETALS("çiçek yaprakları", 5_000L),
}

/** Draws one [ObiEffect]; all particles are made up front so a frame only moves and paints them. */
@SuppressLint("ViewConstructor")
internal class ObiEffectView(context: Context, private val effect: ObiEffect, private val onDone: () -> Unit) : View(context) {
    private class Particle(
        var x: Float, var y: Float, val vx: Float, val vy: Float, val size: Float,
        val color: Int, val spin: Float, val phase: Float, val delay: Long,
    )

    private val random = Random(SystemClock.uptimeMillis())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private var started = 0L
    private var particles: List<Particle> = emptyList()
    private var bursts: List<Triple<Float, Float, Long>> = emptyList()
    private var done = false

    private val palette = when (effect) {
        ObiEffect.HEARTS -> intArrayOf(0xFFFF4F89.toInt(), 0xFFFF8CAE.toInt(), 0xFFFF2E63.toInt(), 0xFFFFB3C7.toInt())
        ObiEffect.SNOW -> intArrayOf(0xFFFFFFFF.toInt(), 0xFFE3F2FF.toInt(), 0xFFCFE8FF.toInt())
        ObiEffect.BUBBLES -> intArrayOf(0xFF8AF1FF.toInt(), 0xFFB9A8FF.toInt(), 0xFFFFC4F0.toInt(), 0xFFA6FFD9.toInt())
        ObiEffect.PETALS -> intArrayOf(0xFFFFB7D5.toInt(), 0xFFFFD1E3.toInt(), 0xFFFF9EC4.toInt(), 0xFFFFF0F6.toInt())
        ObiEffect.STARS -> intArrayOf(0xFFFFE08A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFC46B.toInt(), 0xFF8AF1FF.toInt())
        else -> intArrayOf(0xFFFF5E7E.toInt(), 0xFFFFC94D.toInt(), 0xFF5ED6FF.toInt(), 0xFF8C6CFF.toInt(), 0xFF62E39A.toInt(), 0xFFFF9F43.toInt())
    }

    private fun build(w: Float, h: Float) {
        val d = resources.displayMetrics.density
        fun color() = palette[random.nextInt(palette.size)]
        particles = when (effect) {
            ObiEffect.CONFETTI -> List(90) {
                Particle(w / 2 + (random.nextFloat() - .5f) * w * .2f, h * .62f, (random.nextFloat() - .5f) * w * 1.1f,
                    -(h * (.9f + random.nextFloat() * .7f)), (5f + random.nextFloat() * 5f) * d, color(),
                    random.nextFloat() * 720f - 360f, random.nextFloat() * 6f, random.nextLong(250L))
            }
            ObiEffect.HEARTS, ObiEffect.BUBBLES -> List(if (effect == ObiEffect.HEARTS) 26 else 30) {
                Particle(random.nextFloat() * w, h + 40f * d, 0f, -(h * (.28f + random.nextFloat() * .25f)),
                    (10f + random.nextFloat() * 14f) * d, color(), 0f, random.nextFloat() * 6f, random.nextLong(1_600L))
            }
            ObiEffect.SNOW, ObiEffect.PETALS -> List(if (effect == ObiEffect.SNOW) 70 else 40) {
                Particle(random.nextFloat() * w, -20f * d - random.nextFloat() * h * .3f, (random.nextFloat() - .3f) * 40f * d,
                    h * (.18f + random.nextFloat() * .16f), (3f + random.nextFloat() * (if (effect == ObiEffect.SNOW) 4f else 7f)) * d,
                    color(), random.nextFloat() * 300f - 150f, random.nextFloat() * 6f, random.nextLong(2_000L))
            }
            ObiEffect.STARS -> List(34) {
                Particle(random.nextFloat() * w, random.nextFloat() * h, 0f, 0f, (6f + random.nextFloat() * 12f) * d, color(),
                    0f, random.nextFloat() * 6f, random.nextLong(2_200L))
            }
            ObiEffect.FIREWORKS -> emptyList()
        }
        if (effect == ObiEffect.FIREWORKS) {
            bursts = List(6) { i -> Triple(w * (.18f + random.nextFloat() * .64f), h * (.14f + random.nextFloat() * .36f), i * 520L + random.nextLong(200L)) }
            particles = bursts.flatMapIndexed { b, (_, _, at) ->
                val c = palette[b % palette.size]
                List(28) { k ->
                    val angle = k / 28f * 2f * PI.toFloat()
                    val speed = (120f + random.nextFloat() * 60f) * d
                    Particle(0f, 0f, cos(angle) * speed, sin(angle) * speed, 3.2f * d, c, 0f, b.toFloat(), at)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        if (started == 0L) { started = now; build(width.toFloat(), height.toFloat()) }
        val age = now - started
        if (age > effect.millis) {
            if (!done) { done = true; post(onDone) }
            return
        }
        val fadeOut = ((effect.millis - age) / 600f).coerceIn(0f, 1f)
        val h = height.toFloat()
        for (p in particles) {
            val t = (age - p.delay) / 1000f
            if (t < 0f) continue
            paint.color = p.color
            when (effect) {
                ObiEffect.CONFETTI -> {
                    val x = p.x + p.vx * t * .55f
                    val y = p.y + p.vy * t + .5f * (h * 1.6f) * t * t
                    if (y > h + 50f) continue
                    paint.alpha = (255f * fadeOut).toInt()
                    canvas.save()
                    canvas.rotate(p.spin * t, x, y)
                    val flip = kotlin.math.abs(sin(t * 6f + p.phase))
                    canvas.drawRect(x - p.size, y - p.size * .5f * flip, x + p.size, y + p.size * .5f * flip, paint)
                    canvas.restore()
                }
                ObiEffect.HEARTS -> {
                    val x = p.x + sin(t * 2f + p.phase) * 18f * resources.displayMetrics.density
                    val y = p.y + p.vy * t
                    paint.alpha = (230f * fadeOut).toInt()
                    heart(canvas, x, y, p.size)
                }
                ObiEffect.BUBBLES -> {
                    val x = p.x + sin(t * 1.6f + p.phase) * 24f * resources.displayMetrics.density
                    val y = p.y + p.vy * t
                    stroke.color = p.color; stroke.strokeWidth = p.size * .14f; stroke.alpha = (220f * fadeOut).toInt()
                    canvas.drawCircle(x, y, p.size, stroke)
                    paint.color = 0xFFFFFFFF.toInt(); paint.alpha = (170f * fadeOut).toInt()
                    canvas.drawCircle(x - p.size * .35f, y - p.size * .35f, p.size * .18f, paint)
                }
                ObiEffect.SNOW, ObiEffect.PETALS -> {
                    val x = p.x + p.vx * t + sin(t * 1.3f + p.phase) * 22f * resources.displayMetrics.density
                    val y = p.y + p.vy * t
                    paint.alpha = (235f * fadeOut).toInt()
                    if (effect == ObiEffect.SNOW) canvas.drawCircle(x, y, p.size, paint)
                    else {
                        canvas.save()
                        canvas.rotate(p.spin * t, x, y)
                        canvas.drawOval(x - p.size, y - p.size * .55f, x + p.size, y + p.size * .55f, paint)
                        canvas.restore()
                    }
                }
                ObiEffect.STARS -> {
                    val life = ((t * 1000f) % 1_400f) / 1_400f
                    val r = p.size * sin(life * PI.toFloat())
                    paint.alpha = (255f * sin(life * PI.toFloat()) * fadeOut).toInt()
                    star(canvas, p.x, p.y, r)
                }
                ObiEffect.FIREWORKS -> {
                    if (t > 1.3f) continue
                    val burst = bursts[p.phase.toInt()]
                    val drag = 1f - t * .45f
                    val x = burst.first + p.vx * t * drag
                    val y = burst.second + p.vy * t * drag + 60f * t * t * resources.displayMetrics.density
                    paint.alpha = (255f * (1f - t / 1.3f) * fadeOut).toInt()
                    canvas.drawCircle(x, y, p.size * (1f - t * .4f), paint)
                    stroke.color = p.color; stroke.strokeWidth = p.size * .6f; stroke.alpha = paint.alpha / 2
                    canvas.drawLine(x, y, x - p.vx * .06f, y - p.vy * .06f, stroke)
                }
            }
        }
        postInvalidateOnAnimation()
    }

    private fun heart(canvas: Canvas, x: Float, y: Float, r: Float) {
        path.rewind()
        path.moveTo(x, y + r)
        path.cubicTo(x - r * 2f, y - r * .1f, x - r, y - r * 1.5f, x, y - r * .38f)
        path.cubicTo(x + r, y - r * 1.5f, x + r * 2f, y - r * .1f, x, y + r)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun star(canvas: Canvas, x: Float, y: Float, r: Float) {
        path.rewind()
        path.moveTo(x, y - r)
        path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r)
        path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        path.close()
        canvas.drawPath(path, paint)
    }

    companion object {
        /** Adds the effect over everything; removed by itself when it ends. Returns false if not allowed. */
        fun show(context: Context, windows: WindowManager, effect: ObiEffect): Boolean = runCatching {
            var view: View? = null
            view = ObiEffectView(context, effect) { view?.let { v -> runCatching { windows.removeView(v) } } }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                // Android 12+ only lets touches pass through an overlay that is at most 80% opaque.
                alpha = 0.8f
            }
            windows.addView(view, params)
            true
        }.getOrDefault(false)
    }
}
