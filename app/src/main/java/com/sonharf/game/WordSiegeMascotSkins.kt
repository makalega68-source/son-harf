package com.sonharf.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** The mascot's characters. Every look shares the same animated rig; each has its own temperament. */
internal enum class WordSiegeMascotSkin(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val kindTr: String,
    val kindEn: String,
) {
    ORB("klasik", "Obi", "Obi", "Klasik ışıltı", "Classic glow"),
    PINK("pembe", "Pinki", "Pinky", "Pembe prenses", "Pink princess"),
    DEVIL_BLUE("mavi_seytancik", "Buzi", "Frosty", "Buz şeytancık", "Ice imp"),
    DEVIL_RED("kirmizi_seytancik", "Zıpır", "Blaze", "Ateş şeytancık", "Fire imp"),
    CAT("tekir", "Mırnav", "Purrnie", "Tekir kedi", "Tabby cat"),
    ROBOT("robot", "Bipbop", "Bipbop", "Robot", "Robot"),
    ASTRONAUT("astronot", "Nova", "Nova", "Astronot", "Astronaut"),
    ;

    /** Google Play product and server entitlement key of this character. */
    val productId: String get() = "mascot_$id"

    companion object {
        fun fromId(id: String?): WordSiegeMascotSkin? = entries.firstOrNull { it.id == id }
        fun fromProductId(productId: String?): WordSiegeMascotSkin? = entries.firstOrNull { it.productId == productId }
    }
}

/**
 * Recolouring by gradient map: each pixel's brightness is mapped onto the character's palette and
 * mixed with a little of the original, so the orb keeps its glassy shading and glow.
 */
internal class WordSiegeMascotPalette(private val stops: FloatArray, private val colors: IntArray, private val keep: Float) {
    fun recolor(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = p ushr 24
            if (a == 0) continue
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val l = (.299f * r + .587f * g + .114f * b) / 255f
            var k = 0
            while (k < stops.size - 2 && l > stops[k + 1]) k++
            val u = ((l - stops[k]) / (stops[k + 1] - stops[k])).coerceIn(0f, 1f)
            val c0 = colors[k]
            val c1 = colors[k + 1]
            fun ch(shift: Int, original: Int): Int {
                val from = (c0 shr shift) and 0xFF
                val to = (c1 shr shift) and 0xFF
                val mapped = from + (to - from) * u
                return (mapped * (1f - keep) + original * keep).toInt().coerceIn(0, 255)
            }
            pixels[i] = (a shl 24) or (ch(16, r) shl 16) or (ch(8, g) shl 8) or ch(0, b)
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
            setHasMipMap(true)
        }
    }

    companion object {
        private fun of(keep: Float, vararg stops: Pair<Float, Long>) = WordSiegeMascotPalette(
            FloatArray(stops.size) { stops[it].first },
            IntArray(stops.size) { stops[it].second.toInt() },
            keep,
        )

        fun forSkin(skin: WordSiegeMascotSkin): WordSiegeMascotPalette? = when (skin) {
            WordSiegeMascotSkin.ORB, WordSiegeMascotSkin.ASTRONAUT -> null
            WordSiegeMascotSkin.PINK -> of(.15f, 0f to 0xFF140310, .3f to 0xFF5E0C40, .55f to 0xFFE2408F, .78f to 0xFFFF9FD0, 1f to 0xFFFFF5FB)
            WordSiegeMascotSkin.DEVIL_BLUE -> of(.2f, 0f to 0xFF02051A, .3f to 0xFF0D2270, .55f to 0xFF2F6DFF, .78f to 0xFF86CFFF, 1f to 0xFFF3FBFF)
            WordSiegeMascotSkin.DEVIL_RED -> of(.1f, 0f to 0xFF12020A, .3f to 0xFF5C0718, .55f to 0xFFE0283A, .78f to 0xFFFF8D5E, 1f to 0xFFFFF0DE)
            WordSiegeMascotSkin.CAT -> of(.1f, 0f to 0xFF150802, .3f to 0xFF652806, .55f to 0xFFEA7A1C, .78f to 0xFFFFC46A, 1f to 0xFFFFFAF0)
            WordSiegeMascotSkin.ROBOT -> of(.35f, 0f to 0xFF05070C, .3f to 0xFF263140, .55f to 0xFF7A8CA4, .78f to 0xFFCFDBE8, 1f to 0xFFFFFFFF)
        }
    }
}

/**
 * Vector accessories for a character, built once in the rig's 1254-unit art space. Paths and paints
 * are prepared up front so drawing a frame allocates nothing.
 */
internal class WordSiegeMascotDecor private constructor(val skin: WordSiegeMascotSkin) {
    private class Op(val path: Path, val fill: Paint?, val stroke: Paint?, val clip: Path? = null)

    private val behind = ArrayList<Op>()
    private val face = ArrayList<Op>()
    private val tuft = ArrayList<Op>()
    private val front = ArrayList<Op>()
    private var target = front
    private val m = Matrix()

    /** Ring sweep colours and wing gradient colours for this character. */
    val ringColors: IntArray
    val wingColors: IntArray

    /** Floating round hands: light and dark tone, and whether they get pink paw pads. */
    val handColors: IntArray = when (skin) {
        WordSiegeMascotSkin.ORB -> intArrayOf(0xFF9CF3FF.toInt(), 0xFF2F6DFF.toInt())
        WordSiegeMascotSkin.PINK -> intArrayOf(0xFFFFD3EC.toInt(), 0xFFE2408F.toInt())
        WordSiegeMascotSkin.DEVIL_BLUE -> intArrayOf(0xFFA9DEFF.toInt(), 0xFF1D3FB8.toInt())
        WordSiegeMascotSkin.DEVIL_RED -> intArrayOf(0xFFFFB98A.toInt(), 0xFFB3121C.toInt())
        WordSiegeMascotSkin.CAT -> intArrayOf(0xFFFFF6E8.toInt(), 0xFFE0B27A.toInt())
        WordSiegeMascotSkin.ROBOT -> intArrayOf(0xFFF4F7FB.toInt(), 0xFF6D7A8C.toInt())
        WordSiegeMascotSkin.ASTRONAUT -> intArrayOf(0xFFFFFFFF.toInt(), 0xFFB8C6D8.toInt())
    }
    val pawPads = skin == WordSiegeMascotSkin.CAT

    /** Whether fangs are drawn (they hang from the upper lip, so the rig passes the mouth top). */
    val hasFangs = skin == WordSiegeMascotSkin.DEVIL_BLUE || skin == WordSiegeMascotSkin.DEVIL_RED
    private val fangs = ArrayList<Op>()

    init {
        when (skin) {
            WordSiegeMascotSkin.ORB -> {
                ringColors = intArrayOf(0xFFFFAD72.toInt(), 0xFF56E4F7.toInt(), 0xFF307AF1.toInt(), 0xFFB266F5.toInt(), 0xFFFFAD72.toInt())
                wingColors = intArrayOf(0xE6A8F4FF.toInt(), 0xB39A7CFF.toInt())
                target = tuft; magicTuft(0xFF2F8DFF, 0xFF8AF1FF, 0xE678DCFF, 0xFF141A4A)
            }
            WordSiegeMascotSkin.PINK -> {
                ringColors = intArrayOf(0xFFFFC1E3.toInt(), 0xFFFF7AC8.toInt(), 0xFFE0409A.toInt(), 0xFFC77DFF.toInt(), 0xFFFFC1E3.toInt())
                wingColors = intArrayOf(0xE6FFD1EC.toInt(), 0xB3FF6FBF.toInt())
                target = tuft; magicTuft(0xFFE2408F, 0xFFFFC6E6, 0xE6FF78C8, 0xFF6D0F44)
                target = front; bow()
            }
            WordSiegeMascotSkin.DEVIL_BLUE -> {
                ringColors = intArrayOf(0xFF9FDCFF.toInt(), 0xFF2F6DFF.toInt(), 0xFF1D3FB8.toInt(), 0xFF86CFFF.toInt(), 0xFF9FDCFF.toInt())
                wingColors = intArrayOf(0xE69FDCFF.toInt(), 0xB32F6DFF.toInt())
                target = behind; tail(0xFF1D3FB8, 0xFF5FB4FF)
                target = tuft; magicTuft(0xFF1D3FB8, 0xFF9FDCFF, 0xE664AAFF, 0xFF0B1646)
                target = front
                horn(-1f, 0xFF7FA6D8, 0xFFDFEEFF, 0xFFFFFFFF)
                horn(1f, 0xFF7FA6D8, 0xFFDFEEFF, 0xFFFFFFFF)
                fangs()
            }
            WordSiegeMascotSkin.DEVIL_RED -> {
                ringColors = intArrayOf(0xFFFFB07A.toInt(), 0xFFE0283A.toInt(), 0xFF8E0F18.toInt(), 0xFFFF8D5E.toInt(), 0xFFFFB07A.toInt())
                wingColors = intArrayOf(0xE6FFB07A.toInt(), 0xB3E0283A.toInt())
                target = behind; tail(0xFF8E0F18, 0xFFFF5A4A)
                target = tuft; magicTuft(0xFFB3121C, 0xFFFFB07A, 0xE6FF6E50, 0xFF3A0610)
                target = front
                horn(-1f, 0xFF2A0610, 0xFF7A1420, 0xFFFF8A5C)
                horn(1f, 0xFF2A0610, 0xFF7A1420, 0xFFFF8A5C)
                fangs()
            }
            WordSiegeMascotSkin.CAT -> {
                ringColors = intArrayOf(0xFFFFD08A.toInt(), 0xFFEA7A1C.toInt(), 0xFFB4520E.toInt(), 0xFFFFC46A.toInt(), 0xFFFFD08A.toInt())
                wingColors = intArrayOf(0xE6FFD08A.toInt(), 0xB3EA7A1C.toInt())
                target = front; catEar(-1f); catEar(1f)
                target = tuft; magicTuft(0xFFC2560E, 0xFFFFD08A, 0xE6FFBE5A, 0xFF3F1A06)
                target = face; catFace()
            }
            WordSiegeMascotSkin.ROBOT -> {
                ringColors = intArrayOf(0xFFEAF4FF.toInt(), 0xFF7FF0FF.toInt(), 0xFF7A8CA4.toInt(), 0xFFCFDBE8.toInt(), 0xFFEAF4FF.toInt())
                wingColors = intArrayOf(0xE6EAF4FF.toInt(), 0xB37A8CA4.toInt())
                target = front; robotParts()
                target = tuft; magicTuft(0xFF5E6F86, 0xFFEAF4FF, 0xE678EBFF, 0xFF1C2533)
            }
            WordSiegeMascotSkin.ASTRONAUT -> {
                ringColors = intArrayOf(0xFFFFAD72.toInt(), 0xFF56E4F7.toInt(), 0xFF307AF1.toInt(), 0xFFB266F5.toInt(), 0xFFFFAD72.toInt())
                wingColors = intArrayOf(0xE6A8F4FF.toInt(), 0xB39A7CFF.toInt())
                target = tuft; magicTuft(0xFF56B8F7, 0xFFFFFFFF, 0xF2A0E6FF, 0xFF16305A)
                target = front; helmet()
            }
        }
    }

    fun ringShader(): Shader = SweepGradient(660f, 641f, ringColors, floatArrayOf(0f, .25f, .5f, .75f, 1f))

    fun wingShader(): Shader = LinearGradient(0f, 0f, 430f, -160f,
        intArrayOf(wingColors[0], wingColors[1], 0x66FFFFFF), floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP)

    fun drawBehind(canvas: Canvas) = draw(canvas, behind)

    fun drawFront(canvas: Canvas) = draw(canvas, front)

    /** Face details that ride with the features; fangs follow the top of the (animated) mouth. */
    fun drawFace(canvas: Canvas, mouthTop: Float, mouthOpen: Float) {
        draw(canvas, face)
        if (hasFangs && mouthOpen > .18f) {
            canvas.save()
            canvas.translate(0f, mouthTop - 776f)
            draw(canvas, fangs)
            canvas.restore()
        }
    }

    /** The magic hair tuft sways gently around its root. */
    fun drawTuft(canvas: Canvas, now: Long) {
        canvas.save()
        canvas.rotate(sin(now / 900.0).toFloat() * 4f, 700f, 200f)
        draw(canvas, tuft)
        canvas.restore()
    }

    private fun draw(canvas: Canvas, ops: List<Op>) {
        for (op in ops) {
            if (op.clip != null) {
                canvas.save()
                canvas.clipPath(op.clip)
            }
            op.fill?.let { canvas.drawPath(op.path, it) }
            op.stroke?.let { canvas.drawPath(op.path, it) }
            if (op.clip != null) canvas.restore()
        }
    }

    // ---- building blocks -------------------------------------------------------------------------

    private fun add(path: Path, fill: Paint?, stroke: Paint?, clip: Path? = null) {
        path.transform(m)
        target.add(Op(path, fill, stroke, clip))
    }

    private fun shader(s: Shader): Shader = s.also { it.setLocalMatrix(m) }

    private fun lin(x0: Float, y0: Float, x1: Float, y1: Float, vararg stops: Pair<Float, Long>): Shader = shader(
        LinearGradient(x0, y0, x1, y1, IntArray(stops.size) { stops[it].second.toInt() },
            FloatArray(stops.size) { stops[it].first }, Shader.TileMode.CLAMP),
    )

    private fun rad(cx: Float, cy: Float, r: Float, vararg stops: Pair<Float, Long>): Shader = shader(
        RadialGradient(cx, cy, r, IntArray(stops.size) { stops[it].second.toInt() },
            FloatArray(stops.size) { stops[it].first }, Shader.TileMode.CLAMP),
    )

    private fun fill(shader: Shader? = null, color: Long = 0xFFFFFFFF, shadow: Boolean = false, glow: Long = 0L) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toInt()
            this.shader = shader
            when {
                glow != 0L -> setShadowLayer(26f, 0f, 0f, glow.toInt())
                shadow -> setShadowLayer(22f, 0f, 10f, 0x520C061E)
            }
        }

    /** Outlines are drawn soft: slightly thinner, lighter and feathered rather than inked. */
    private fun line(color: Long, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width * .8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        val alpha = ((color ushr 24) and 0xFF) * 3 / 4
        this.color = ((alpha shl 24) or (color and 0xFFFFFF)).toInt()
        maskFilter = android.graphics.BlurMaskFilter((width * .22f).coerceAtLeast(1f), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private fun cubic(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        Path().apply {
            moveTo(x0, y0)
            cubicTo(x1, y1, x2, y2, x3, y3)
        }

    /** A filled stroke that tapers from [w0] to [w1] along a cubic curve. */
    private fun taper(p: FloatArray, w0: Float, w1: Float): Path {
        val n = 40
        val left = FloatArray((n + 1) * 2)
        val right = FloatArray((n + 1) * 2)
        fun b(t: Float, i: Int): Float {
            val u = 1f - t
            return u * u * u * p[i] + 3 * u * u * t * p[i + 2] + 3 * u * t * t * p[i + 4] + t * t * t * p[i + 6]
        }
        for (k in 0..n) {
            val t = k / n.toFloat()
            val px = b(t, 0)
            val py = b(t, 1)
            val qx = b(minOf(1f, t + .01f), 0) - b(maxOf(0f, t - .01f), 0)
            val qy = b(minOf(1f, t + .01f), 1) - b(maxOf(0f, t - .01f), 1)
            val len = hypot(qx, qy).takeIf { it > 0f } ?: 1f
            val w = (w0 + (w1 - w0) * t) / 2f
            left[k * 2] = px - qy / len * w; left[k * 2 + 1] = py + qx / len * w
            right[k * 2] = px + qy / len * w; right[k * 2 + 1] = py - qx / len * w
        }
        return Path().apply {
            moveTo(left[0], left[1])
            for (k in 1..n) lineTo(left[k * 2], left[k * 2 + 1])
            for (k in n downTo 0) lineTo(right[k * 2], right[k * 2 + 1])
            close()
        }
    }

    private fun glint(cx: Float, cy: Float, r: Float, alpha: Float, glow: Long = 0L) {
        val p = Path().apply {
            moveTo(cx, cy - r)
            quadTo(cx + r * .12f, cy - r * .12f, cx + r, cy)
            quadTo(cx + r * .12f, cy + r * .12f, cx, cy + r)
            quadTo(cx - r * .12f, cy + r * .12f, cx - r, cy)
            quadTo(cx - r * .12f, cy - r * .12f, cx, cy - r)
            close()
        }
        add(p, fill(color = (((alpha * 255).toLong() shl 24) or 0xFFFFFF), glow = glow), null)
    }

    private fun streak(pts: FloatArray, width: Float, alpha: Float) {
        add(cubic(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5], pts[6], pts[7]), null,
            line(((alpha * 255).toLong() shl 24) or 0xFFFFFF, width))
    }

    private fun resetMatrix() = m.reset()

    // ---- accessories (shapes match the approved concept sheet) ----------------------------------

    private fun magicTuft(c0: Long, c1: Long, glow: Long, out: Long) {
        resetMatrix()
        val g = { lin(700f, 200f, 760f, 50f, 0f to c0, 1f to c1) }
        add(taper(floatArrayOf(676f, 196f, 640f, 150f, 600f, 140f, 590f, 168f), 46f, 4f), fill(g(), glow = glow), line(out, 8f))
        add(taper(floatArrayOf(724f, 198f, 770f, 160f, 812f, 160f, 818f, 190f), 42f, 4f), fill(g(), glow = glow), line(out, 8f))
        add(taper(floatArrayOf(694f, 200f, 680f, 120f, 720f, 50f, 790f, 58f), 74f, 6f), fill(g(), glow = glow), line(out, 8f))
        val curl = Path().apply {
            addArc(RectF(770f, 60f, 814f, 104f), Math.toDegrees(-1.6).toFloat(), Math.toDegrees(3.8).toFloat())
        }
        add(curl, null, line(out, 8f))
        add(Path(curl), null, line(c1, 5f))
        streak(floatArrayOf(690f, 170f, 688f, 120f, 708f, 82f, 736f, 64f), 8f, .75f)
        glint(836f, 56f, 20f, .95f, glow)
        glint(610f, 110f, 13f, .9f, glow)
        glint(850f, 120f, 9f, .85f, glow)
    }

    private fun horn(side: Float, c0: Long, c1: Long, c2: Long) {
        resetMatrix()
        m.preTranslate(660f + side * 215f, 262f)
        m.preScale(side, 1f)
        m.preRotate(Math.toDegrees(.12).toFloat())
        val shape = { Path().apply {
            moveTo(-58f, 34f)
            cubicTo(-58f, -40f, -10f, -110f, 62f, -168f)
            cubicTo(44f, -110f, 58f, -30f, 58f, 34f)
            cubicTo(20f, 50f, -20f, 50f, -58f, 34f)
            close()
        } }
        val body = shape()
        add(body, fill(lin(0f, 40f, 60f, -168f, 0f to c0, .55f to c1, 1f to c2), shadow = true), line(0xFF140A2A, 9f))
        val clip = Path(body)
        for ((y, wd) in listOf(4f to 60f, -40f to 48f, -82f to 34f)) {
            val cx = 4f + (-y) * .25f
            val ridge = Path().apply { addArc(RectF(cx - wd, y - 12f, cx + wd, y + 12f), 0f, 180f) }
            ridge.transform(Matrix().apply { setRotate(Math.toDegrees(-.25).toFloat(), cx, y) })
            add(ridge, null, line(0x47140A2A, 7f), clip)
        }
        add(shape(), fill(rad(-30f, 10f, 90f, 0f to 0x00000000L, 1f to 0x590A001EL)), null)
        streak(floatArrayOf(-34f, 20f, -34f, -40f, 0f, -100f, 40f, -140f), 9f, .75f)
        glint(44f, -140f, 14f, .9f)
    }

    private fun tail(c0: Long, c1: Long) {
        resetMatrix()
        add(taper(floatArrayOf(1000f, 930f, 1120f, 990f, 1215f, 900f, 1150f, 790f), 40f, 16f),
            fill(lin(1000f, 930f, 1150f, 790f, 0f to c0, 1f to c1), shadow = true), line(0xFF140A2A, 8f))
        m.preTranslate(1146f, 760f)
        m.preRotate(Math.toDegrees(-.5).toFloat())
        val heart = Path().apply {
            moveTo(0f, 40f)
            cubicTo(-58f, 0f, -50f, -50f, -10f, -44f)
            quadTo(0f, -42f, 0f, -30f)
            quadTo(0f, -42f, 10f, -44f)
            cubicTo(50f, -50f, 58f, 0f, 0f, 40f)
            close()
        }
        add(heart, fill(lin(0f, -50f, 0f, 40f, 0f to c1, 1f to c0), shadow = true), line(0xFF140A2A, 9f))
        glint(-18f, -20f, 12f, .85f)
    }

    private fun fangs() {
        resetMatrix()
        val saved = target
        target = fangs
        for (fx in floatArrayOf(666f, 722f)) {
            val p = Path().apply {
                moveTo(fx - 11f, 776f)
                lineTo(fx + 11f, 776f)
                quadTo(fx + 3f, 796f, fx, 806f)
                quadTo(fx - 3f, 796f, fx - 11f, 776f)
                close()
            }
            add(p, fill(color = 0xFFFFFFFF), line(0xFF140A2A, 4f))
        }
        target = saved
    }

    private fun catEar(side: Float) {
        resetMatrix()
        m.preTranslate(660f + side * 238f, 318f)
        m.preScale(side, 1f)
        m.preRotate(Math.toDegrees(.18).toFloat())
        val outer = Path().apply {
            moveTo(-95f, 40f)
            cubicTo(-60f, -60f, 0f, -160f, 30f, -190f)
            cubicTo(70f, -120f, 105f, -30f, 110f, 40f)
            cubicTo(40f, 58f, -30f, 58f, -95f, 40f)
            close()
        }
        add(outer, fill(lin(0f, 40f, 30f, -190f, 0f to 0xFFB4520E, .6f to 0xFFF08A2A, 1f to 0xFFFFC27A), shadow = true), line(0xFF3F1A06, 9f))
        val inner = Path().apply {
            moveTo(-50f, 30f)
            cubicTo(-25f, -40f, 10f, -110f, 28f, -130f)
            cubicTo(55f, -80f, 72f, -20f, 72f, 30f)
            cubicTo(30f, 40f, -10f, 40f, -50f, 30f)
            close()
        }
        add(inner, fill(lin(0f, 30f, 20f, -130f, 0f to 0xFFFF8FB1, 1f to 0xFFFFD3E0)), null)
        for (f in listOf(floatArrayOf(-20f, 20f, -10f, -30f), floatArrayOf(10f, 24f, 14f, -40f), floatArrayOf(40f, 22f, 34f, -20f))) {
            add(Path().apply { moveTo(f[0], f[1]); lineTo(f[2], f[3]) }, null, line(0xD9FFFFFF, 5f))
        }
        streak(floatArrayOf(-70f, 20f, -50f, -50f, -10f, -120f, 20f, -160f), 8f, .6f)
    }

    private fun catFace() {
        resetMatrix()
        for (s in floatArrayOf(-1f, 1f)) {
            for (i in 0 until 3) {
                val y0 = 806f + i * 22f
                val p = Path().apply {
                    moveTo(708f + s * 200f, y0)
                    quadTo(708f + s * 290f, y0 - 14f + i * 10f, 708f + s * 370f, y0 - 30f + i * 26f)
                }
                add(p, null, line(0xE6FFF6E6, 6f - i))
            }
        }
        val nose = Path().apply {
            moveTo(690f, 754f)
            quadTo(708f, 746f, 726f, 754f)
            quadTo(720f, 772f, 708f, 776f)
            quadTo(696f, 772f, 690f, 754f)
            close()
        }
        add(nose, fill(lin(0f, 746f, 0f, 776f, 0f to 0xFFFFB3C8, 1f to 0xFFE8638D)), line(0xFF3F1A06, 4f))
    }

    private fun metal(x0: Float, y0: Float, x1: Float, y1: Float) =
        lin(x0, y0, x1, y1, 0f to 0xFFF4F7FB, .35f to 0xFFAAB6C6, .6f to 0xFF6D7A8C, 1f to 0xFFDFE6EE)

    private fun robotParts() {
        resetMatrix()
        // Antenna stands a little left of centre so the magic tuft has room.
        val ax = 600f
        add(Path().apply { addRect(ax - 14f, 70f, ax + 14f, 175f, Path.Direction.CW) }, fill(metal(ax - 14f, 0f, ax + 14f, 0f), shadow = true), line(0xFF140A2A, 6f))
        add(Path().apply { moveTo(ax - 60f, 190f); cubicTo(ax - 60f, 142f, ax + 60f, 142f, ax + 60f, 190f); close() },
            fill(metal(ax - 60f, 150f, ax + 60f, 190f), shadow = true), line(0xFF140A2A, 7f))
        val bulb = Path().apply { addCircle(ax, 60f, 34f, Path.Direction.CW) }
        add(bulb, fill(rad(ax - 10f, 50f, 40f, 0f to 0xFFFFF1F1, .35f to 0xFFFF6B6B, 1f to 0xFFB3121C), glow = 0xF2FF4646), line(0xFF140A2A, 7f))
        glint(ax - 12f, 46f, 12f, .9f)
        for (s in floatArrayOf(-1f, 1f)) {
            val cx = 660f + s * 455f
            val cy = 640f
            add(Path().apply { addOval(RectF(cx - 58f, cy - 96f, cx + 58f, cy + 96f), Path.Direction.CW) },
                fill(metal(cx - 58f, cy - 96f, cx + 58f, cy + 96f), shadow = true), line(0xFF140A2A, 8f))
            add(Path().apply { addOval(RectF(cx - 34f, cy - 62f, cx + 34f, cy + 62f), Path.Direction.CW) },
                fill(lin(0f, cy - 62f, 0f, cy + 62f, 0f to 0xFF5E6B7D, 1f to 0xFF2B3340)), line(0x59FFFFFF, 4f))
            add(Path().apply { addCircle(cx, cy, 12f, Path.Direction.CW) }, fill(color = 0xFF7FF0FF, glow = 0xF250E6FF), null)
        }
        for (a in floatArrayOf(-2.5f, -2.0f, -1.1f, -.6f)) {
            val rx = 660f + cos(a) * 420f
            val ry = 610f + sin(a) * 425f
            add(Path().apply { addCircle(rx, ry, 14f, Path.Direction.CW) },
                fill(rad(rx - 5f, ry - 5f, 16f, 0f to 0xFFFFFFFF, 1f to 0xFF7D8A9C)), line(0xFF140A2A, 4f))
        }
    }

    private fun helmet() {
        resetMatrix()
        add(Path().apply { addOval(RectF(260f, 1046f, 1060f, 1210f), Path.Direction.CW) },
            fill(metal(260f, 1060f, 1060f, 1200f), shadow = true), line(0xFF140A2A, 8f))
        add(Path().apply { addOval(RectF(330f, 1066f, 990f, 1170f), Path.Direction.CW) },
            fill(lin(0f, 1070f, 0f, 1170f, 0f to 0xFF3B4658, 1f to 0xFF8795AA)), null)
        add(Path().apply { addCircle(470f, 1150f, 15f, Path.Direction.CW) }, fill(color = 0xFFFF6B4A, glow = 0xE6FF6B4A), null)
        add(Path().apply { addCircle(850f, 1150f, 15f, Path.Direction.CW) }, fill(color = 0xFF5FE3FF, glow = 0xE65FE3FF), null)
        val glass = Path().apply { addCircle(660f, 610f, 560f, Path.Direction.CW) }
        add(glass, fill(rad(660f, 610f, 560f, 0f to 0x00D2F0FFL, .8f to 0x1ABEE6FFL, 1f to 0x59AAD7FFL)),
            line(0xF0FFFFFF, 12f).apply { shader = lin(100f, 50f, 1220f, 1170f, 0f to 0xF2FFFFFF, .5f to 0xCC8CC8FF, 1f to 0xE6FFFFFF) })
        fun arc(r: Float, from: Double, to: Double, w: Float, color: Long) {
            val p = Path().apply {
                addArc(RectF(660f - r, 610f - r, 660f + r, 610f + r), Math.toDegrees(from * PI).toFloat(), Math.toDegrees((to - from) * PI).toFloat())
            }
            add(p, null, line(color, w))
        }
        arc(500f, 1.08, 1.33, 30f, 0xB3FFFFFF)
        arc(500f, 1.40, 1.48, 12f, 0xB3FFFFFF)
        arc(510f, .10, .30, 10f, 0x59FFFFFF)
        glint(980f, 300f, 24f, .85f)
    }

    private fun bow() {
        resetMatrix()
        m.preTranslate(882f, 236f)
        m.preRotate(Math.toDegrees(.35).toFloat())
        for (s in floatArrayOf(-1f, 1f)) {
            val base = Matrix(m)
            m.preScale(s, 1f)
            add(taper(floatArrayOf(10f, 16f, 24f, 60f, 44f, 90f, 40f, 124f), 44f, 30f),
                fill(lin(0f, 10f, 40f, 124f, 0f to 0xFFFF5FAE, 1f to 0xFFC42A7A)), line(0xFF6D0F44, 8f))
            val loop = { Path().apply {
                moveTo(0f, 0f)
                cubicTo(50f, -86f, 160f, -62f, 150f, 6f)
                cubicTo(144f, 70f, 56f, 56f, 0f, 0f)
                close()
            } }
            val loopPath = loop()
            add(loopPath, fill(lin(0f, -70f, 150f, 60f, 0f to 0xFFFFB3DB, .5f to 0xFFFF5FAE, 1f to 0xFFD6307F), shadow = true), line(0xFF6D0F44, 9f))
            val clip = Path(loopPath)
            add(Path().apply { moveTo(20f, -6f); quadTo(80f, -20f, 125f, -6f) }, null, line(0x59780A3C, 9f), clip)
            add(Path().apply { moveTo(24f, 8f); quadTo(80f, 24f, 120f, 30f) }, null, line(0x59780A3C, 9f), clip)
            streak(floatArrayOf(40f, -38f, 70f, -56f, 110f, -52f, 134f, -30f), 10f, .7f)
            m.set(base)
        }
        add(Path().apply { addOval(RectF(-30f, -30f, 30f, 38f), Path.Direction.CW) },
            fill(rad(-8f, -6f, 40f, 0f to 0xFFFFD0E8, 1f to 0xFFE8489A), shadow = true), line(0xFF6D0F44, 9f))
        glint(-8f, -8f, 9f, .85f)
    }

    companion object {
        private val cache = HashMap<WordSiegeMascotSkin, WordSiegeMascotDecor>()

        fun of(skin: WordSiegeMascotSkin): WordSiegeMascotDecor = cache.getOrPut(skin) { WordSiegeMascotDecor(skin) }
    }
}
