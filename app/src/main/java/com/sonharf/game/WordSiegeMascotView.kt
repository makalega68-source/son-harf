package com.sonharf.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/** Reusable expression vocabulary for future game events and mascot screens. */
internal enum class WordSiegeMascotEmotion {
    CALM, FOCUS, HAPPY, LAUGH, EXCITED, SURPRISED, SAD, ANGRY, STRESSED,
    PROUD, SPEAKING, TEARY, BOWED, JUMP,
}

/** Local event-driven reaction selection; deterministic per move, with no network or model cost. */
internal object WordSiegeMascotBehavior {
    fun choose(moveId: Long, mine: Boolean, score: Int, captured: Int, stolen: Int): WordSiegeMascotEmotion {
        val variation = (moveId % 4L).toInt()
        return when {
            mine && (score >= 25 || captured >= 3 || stolen > 0) && variation == 0 -> WordSiegeMascotEmotion.JUMP
            mine && (score >= 25 || captured >= 3 || stolen > 0) -> WordSiegeMascotEmotion.PROUD
            mine && variation == 3 -> WordSiegeMascotEmotion.JUMP
            mine && variation == 1 -> WordSiegeMascotEmotion.LAUGH
            mine -> WordSiegeMascotEmotion.HAPPY
            score >= 25 || stolen > 0 -> if (variation % 2 == 0) WordSiegeMascotEmotion.TEARY else WordSiegeMascotEmotion.BOWED
            else -> WordSiegeMascotEmotion.SURPRISED
        }
    }
}

/** Lightweight, independent 2D rig. All face textures come from the approved orb artwork. */
internal class WordSiegeMascotView(context: Context) : View(context) {
    private val names = listOf(
        "orb_face_base", "eye_left", "eye_right", "iris_left", "iris_right",
        "brow_left", "brow_right", "cheek_left", "cheek_right", "mouth",
    )
    private val layers: Map<String, Bitmap> = names.associateWith { name ->
        context.assets.open("word_siege_mascot/$name.webp").use(BitmapFactory::decodeStream)
            ?: error("Missing mascot layer: $name")
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        shader = SweepGradient(660f, 630f, intArrayOf(
            0xFFFFAD72.toInt(), 0xFF56E4F7.toInt(), 0xFF307AF1.toInt(),
            0xFFB266F5.toInt(), 0xFFFFAD72.toInt(),
        ), floatArrayOf(0f, .25f, .5f, .75f, 1f))
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111A33.toInt()
    }
    private var startedAt = SystemClock.uptimeMillis()
    private var lastFrameAt = startedAt
    private var lastBlink = startedAt
    private var lastMoveId: Long? = null
    private var hasInitialMove = false
    private var reactionUntil = 0L
    private var heartsStartedAt = 0L
    private var reaction = WordSiegeMascotEmotion.CALM
    private var externalEmotion: WordSiegeMascotEmotion? = null
    private var gazeX = 0f
    private var gazeY = 0f
    private var targetX = 0f
    private var targetY = 0f
    private var pendingCount = 0
    private var reactionCell: Int? = null

    fun updateGame(
        moveId: Long?, lastMoveMine: Boolean, moveScore: Int, capturedCells: Int,
        opponentCaptured: Int, moveCell: Int?, pendingCells: Collection<Int>, playerTurn: Boolean,
        requestedEmotion: WordSiegeMascotEmotion?,
    ) {
        externalEmotion = requestedEmotion
        if (!hasInitialMove) {
            lastMoveId = moveId
            hasInitialMove = true
        } else if (moveId != null && moveId != lastMoveId) {
            lastMoveId = moveId
            reaction = WordSiegeMascotBehavior.choose(moveId, lastMoveMine, moveScore, capturedCells, opponentCaptured)
            val now = SystemClock.uptimeMillis()
            reactionUntil = now + if (reaction == WordSiegeMascotEmotion.TEARY || reaction == WordSiegeMascotEmotion.BOWED) 1_700L else 1_350L
            reactionCell = moveCell
            if (lastMoveMine) heartsStartedAt = now
        }
        pendingCount = pendingCells.size
        val cell = pendingCells.lastOrNull()
            ?: reactionCell?.takeIf { reactionUntil > SystemClock.uptimeMillis() }
        targetX = if (cell != null) ((cell % WordSiegeBoardSpec.Size - WordSiegeBoardSpec.CenterColumn) / 7f).coerceIn(-1f, 1f) else 0f
        targetY = if (cell != null) ((cell / WordSiegeBoardSpec.Size - WordSiegeBoardSpec.CenterRow) / 7f).coerceIn(-1f, 1f) else .2f
        if (!playerTurn && cell == null && reactionUntil < SystemClock.uptimeMillis()) reaction = WordSiegeMascotEmotion.CALM
        invalidate()
    }

    fun reactToTap() {
        reaction = WordSiegeMascotEmotion.SURPRISED
        reactionUntil = SystemClock.uptimeMillis() + 650L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        val elapsed = now - startedAt
        val frameDeltaSeconds = ((now - lastFrameAt).coerceIn(0L, 80L)) / 1_000.0
        lastFrameAt = now
        if (now - lastBlink > 2_600L + ((elapsed / 2_600L) % 3L) * 450L) lastBlink = now
        val blinkProgress = (now - lastBlink) / 210f
        val blink = if (blinkProgress in 0f..1f) sin(blinkProgress * PI).toFloat().coerceAtLeast(0f) else 0f
        if (now >= reactionUntil && pendingCount == 0) {
            targetX = 0f
            targetY = .2f
        }
        // Time-based smoothing keeps gaze speed stable when rendering cadence changes.
        val gazeBlend = (1.0 - exp(-5.0 * frameDeltaSeconds)).toFloat().coerceIn(0f, 1f)
        gazeX += (targetX - gazeX) * gazeBlend
        gazeY += (targetY - gazeY) * gazeBlend
        val mood = if (now < reactionUntil) reaction
            else externalEmotion ?: if (pendingCount > 0) WordSiegeMascotEmotion.FOCUS else WordSiegeMascotEmotion.CALM
        val lid = max(blink, when (mood) {
            WordSiegeMascotEmotion.FOCUS, WordSiegeMascotEmotion.STRESSED -> .26f
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.LAUGH, WordSiegeMascotEmotion.JUMP -> .42f
            WordSiegeMascotEmotion.PROUD -> .25f
            WordSiegeMascotEmotion.SAD, WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED -> .52f
            WordSiegeMascotEmotion.ANGRY -> .38f
            else -> 0f
        })
        val size = minOf(width, height).toFloat()
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(size / 1_254f, size / 1_254f)
        val reactionProgress = ((now - (reactionUntil - 1_350L)) / 1_350f).coerceIn(0f, 1f)
        val jump = if (mood == WordSiegeMascotEmotion.JUMP) sin(reactionProgress * PI * 3).toFloat().coerceAtLeast(0f) * 94f else 0f
        val idleBob = sin(elapsed / 640f) * 3f
        drawGroundShadow(canvas, elapsed, jump)
        canvas.translate(0f, idleBob - jump + if (mood == WordSiegeMascotEmotion.BOWED) 25f else 0f)
        if (mood == WordSiegeMascotEmotion.BOWED) canvas.rotate(5f, 650f, 650f)
        drawLayer(canvas, "orb_face_base")
        // A fine color-matched contour softens the dark silhouette without changing the orb.
        edgePaint.alpha = (175 + 25 * sin(elapsed / 950f) + if (mood == WordSiegeMascotEmotion.PROUD) 35 else 0).toInt().coerceIn(0, 255)
        canvas.drawOval(RectF(258f, 231f, 1057f, 1052f), edgePaint)
        drawLayer(canvas, "eye_left")
        drawLayer(canvas, "eye_right")
        drawLayer(canvas, "iris_left", gazeX * 22f, gazeY * 17f)
        drawLayer(canvas, "iris_right", gazeX * 22f, gazeY * 17f)
        drawEyeHighlights(canvas, elapsed)
        drawLid(canvas, 515f, 653f, 127f, 132f, lid)
        drawLid(canvas, 877f, 653f, 126f, 131f, lid)
        val browY = when (mood) {
            WordSiegeMascotEmotion.FOCUS, WordSiegeMascotEmotion.STRESSED -> 12f
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.LAUGH, WordSiegeMascotEmotion.JUMP -> -22f
            WordSiegeMascotEmotion.PROUD, WordSiegeMascotEmotion.EXCITED -> -27f
            WordSiegeMascotEmotion.SURPRISED -> -34f
            WordSiegeMascotEmotion.SAD, WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED -> 18f
            else -> 0f
        }
        val browRotation = when (mood) {
            WordSiegeMascotEmotion.FOCUS, WordSiegeMascotEmotion.STRESSED, WordSiegeMascotEmotion.ANGRY -> -.21f
            WordSiegeMascotEmotion.PROUD -> .12f
            WordSiegeMascotEmotion.SAD, WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED -> .2f
            else -> 0f
        }
        drawLayer(canvas, "brow_left", 0f, browY, 536f, 458f, browRotation)
        drawLayer(canvas, "brow_right", 0f, browY, 870f, 458f, -browRotation)
        val cheekStrength = when (mood) {
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.LAUGH, WordSiegeMascotEmotion.JUMP -> 65
            WordSiegeMascotEmotion.PROUD, WordSiegeMascotEmotion.EXCITED -> 72
            WordSiegeMascotEmotion.SURPRISED -> 35
            else -> 16
        }
        drawCheekLight(canvas, 403f, 770f, cheekStrength)
        drawCheekLight(canvas, 951f, 770f, cheekStrength)
        drawLayer(canvas, "cheek_left")
        drawLayer(canvas, "cheek_right")
        canvas.save()
        canvas.translate(708f, 800f)
        val mouthX = when (mood) {
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.LAUGH, WordSiegeMascotEmotion.JUMP -> 1.38f
            WordSiegeMascotEmotion.PROUD, WordSiegeMascotEmotion.EXCITED -> 1.3f
            WordSiegeMascotEmotion.SURPRISED, WordSiegeMascotEmotion.SPEAKING -> .72f
            WordSiegeMascotEmotion.SAD, WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED -> .74f
            else -> 1f
        }
        val mouthY = when (mood) {
            WordSiegeMascotEmotion.FOCUS, WordSiegeMascotEmotion.STRESSED, WordSiegeMascotEmotion.ANGRY -> .55f
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.LAUGH, WordSiegeMascotEmotion.JUMP -> 1.42f
            WordSiegeMascotEmotion.PROUD, WordSiegeMascotEmotion.EXCITED -> 1.24f
            WordSiegeMascotEmotion.SURPRISED -> 1.4f
            WordSiegeMascotEmotion.SPEAKING -> .8f + .5f * sin(elapsed / 115f)
            WordSiegeMascotEmotion.SAD, WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED -> .4f
            else -> 1f
        }
        canvas.scale(mouthX, mouthY)
        canvas.translate(-708f, -800f)
        drawLayer(canvas, "mouth")
        canvas.restore()
        if (mood == WordSiegeMascotEmotion.TEARY) drawTears(canvas, now)
        drawHearts(canvas, now)
        canvas.restore()
        if (isAttachedToWindow && visibility == VISIBLE) postInvalidateDelayed(33L)
    }

    private fun drawGroundShadow(canvas: Canvas, elapsed: Long, jump: Float) {
        val jumpFraction = (jump / 94f).coerceIn(0f, 1f)
        val breathing = abs(sin(elapsed / 900f))
        val halfWidth = 245f * (1f + jumpFraction * .18f + breathing * .025f)
        val halfHeight = 27f * (1f - jumpFraction * .16f)
        val centerX = 658f
        val centerY = 1_070f
        val alpha = (58f - 25f * jumpFraction).toInt().coerceIn(24, 58)
        shadowPaint.alpha = alpha / 2
        canvas.drawOval(
            RectF(centerX - halfWidth * 1.08f, centerY - halfHeight * 1.35f, centerX + halfWidth * 1.08f, centerY + halfHeight * 1.35f),
            shadowPaint,
        )
        shadowPaint.alpha = alpha
        canvas.drawOval(
            RectF(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight),
            shadowPaint,
        )
        shadowPaint.alpha = 255
    }

    private fun drawEyeHighlights(canvas: Canvas, elapsed: Long) {
        val shimmer = (.92f + .08f * sin(elapsed / 760f)).coerceIn(.84f, 1f)
        val gazeDx = gazeX * 22f
        val gazeDy = gazeY * 17f
        detailPaint.shader = null
        detailPaint.color = 0xFFFFFFFF.toInt()
        for (cx in floatArrayOf(515f, 877f)) {
            detailPaint.alpha = (218f * shimmer).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx + gazeDx - 30f, 653f + gazeDy - 31f, 13.5f, detailPaint)
            detailPaint.alpha = (128f * shimmer).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx + gazeDx + 15f, 653f + gazeDy + 14f, 5.5f, detailPaint)
        }
        detailPaint.alpha = 255
    }

    private fun drawTears(canvas: Canvas, now: Long) {
        val drift = ((now - (reactionUntil - 1_700L)) / 1_700f).coerceIn(0f, 1f) * 130f
        detailPaint.color = 0xFF99E9FF.toInt()
        detailPaint.alpha = 190
        for (x in floatArrayOf(468f, 938f)) {
            val y = 730f + drift
            val tear = Path().apply {
                moveTo(x, y - 22f)
                cubicTo(x - 12f, y + 5f, x - 15f, y + 27f, x, y + 29f)
                cubicTo(x + 15f, y + 27f, x + 12f, y + 5f, x, y - 22f)
            }
            canvas.drawPath(tear, detailPaint)
        }
        detailPaint.alpha = 255
    }

    private fun drawCheekLight(canvas: Canvas, x: Float, y: Float, strength: Int) {
        detailPaint.shader = RadialGradient(x, y, 92f,
            intArrayOf((strength shl 24) or 0xFF70B4, 0x00FF70B4), null,
            Shader.TileMode.CLAMP)
        canvas.drawCircle(x, y, 92f, detailPaint)
        detailPaint.shader = null
    }

    private fun drawHearts(canvas: Canvas, now: Long) {
        val age = now - heartsStartedAt
        if (heartsStartedAt == 0L || age !in 0L..1_350L) return
        val colors = intArrayOf(0xFFFF4F89.toInt(), 0xFFFF8CAE.toInt(), 0xFFFF658C.toInt(), 0xFFFF649E.toInt())
        repeat(4) { index ->
            val progress = ((age - index * 125L) / 900f).coerceIn(0f, 1f)
            if (progress <= 0f || progress >= 1f) return@repeat
            val alpha = (255f * minOf(1f, progress * 5f, (1f - progress) * 3f)).toInt()
            val x = 330f + index * 190f + sin(progress * 5f + index) * 22f
            val y = 370f - progress * (240f + index * 35f)
            val radius = 42f + index * 5f
            val heart = Path().apply {
                moveTo(x, y + radius)
                cubicTo(x - radius * 2f, y - radius * .1f, x - radius, y - radius * 1.5f, x, y - radius * .38f)
                cubicTo(x + radius, y - radius * 1.5f, x + radius * 2f, y - radius * .1f, x, y + radius)
                close()
            }
            detailPaint.color = colors[index]
            detailPaint.alpha = alpha
            canvas.drawPath(heart, detailPaint)
        }
        detailPaint.alpha = 255
    }

    private fun drawLayer(canvas: Canvas, name: String, dx: Float = 0f, dy: Float = 0f,
                          pivotX: Float = 0f, pivotY: Float = 0f, rotation: Float = 0f) {
        canvas.save()
        canvas.translate(dx, dy)
        if (rotation != 0f) canvas.rotate(Math.toDegrees(rotation.toDouble()).toFloat(), pivotX, pivotY)
        layers[name]?.let { canvas.drawBitmap(it, null, android.graphics.RectF(0f, 0f, 1_254f, 1_254f), paint) }
        canvas.restore()
    }

    private fun drawLid(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, amount: Float) {
        if (amount < .015f) return
        val y = cy - ry + amount * ry * 1.65f
        val shape = Path().apply {
            moveTo(cx - rx - 5f, cy - ry - 8f)
            lineTo(cx + rx + 5f, cy - ry - 8f)
            lineTo(cx + rx + 5f, y - 20f)
            quadTo(cx, y + 19f, cx - rx - 5f, y - 20f)
            close()
        }
        canvas.save()
        canvas.clipPath(shape)
        drawLayer(canvas, "orb_face_base")
        canvas.restore()
    }

}

@Composable
internal fun WordSiegeMascot(
    moveId: Long?,
    lastMoveMine: Boolean,
    moveScore: Int = 0,
    capturedCells: Int = 0,
    opponentCaptured: Int = 0,
    moveCell: Int? = null,
    pendingCells: Collection<Int>,
    playerTurn: Boolean,
    requestedEmotion: WordSiegeMascotEmotion? = null,
    modifier: Modifier = Modifier.size(42.dp),
    onTap: () -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> WordSiegeMascotView(context).apply { isClickable = true } },
        update = {
            it.updateGame(moveId, lastMoveMine, moveScore, capturedCells, opponentCaptured, moveCell, pendingCells, playerTurn, requestedEmotion)
            it.setOnClickListener { view ->
                (view as WordSiegeMascotView).reactToTap()
                onTap()
            }
        },
    )
}