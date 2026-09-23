package com.sonharf.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

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
    private var startedAt = SystemClock.uptimeMillis()
    private var lastBlink = startedAt
    private var lastMoveId: Long? = null
    private var hasInitialMove = false
    private var reactionUntil = 0L
    private var reaction = Reaction.CALM
    private var gazeX = 0f
    private var gazeY = 0f
    private var targetX = 0f
    private var targetY = 0f
    private var pendingCount = 0

    private enum class Reaction { CALM, FOCUS, HAPPY, SURPRISED, SAD }

    fun updateGame(moveId: Long?, lastMoveMine: Boolean, pendingCells: Collection<Int>, playerTurn: Boolean) {
        if (!hasInitialMove) {
            lastMoveId = moveId
            hasInitialMove = true
        } else if (moveId != null && moveId != lastMoveId) {
            lastMoveId = moveId
            reaction = if (lastMoveMine) Reaction.HAPPY else Reaction.SURPRISED
            reactionUntil = SystemClock.uptimeMillis() + 1_350L
        }
        pendingCount = pendingCells.size
        val cell = pendingCells.lastOrNull()
        targetX = if (cell != null) ((cell % WordSiegeBoardSpec.Size - WordSiegeBoardSpec.CenterColumn) / 7f).coerceIn(-1f, 1f) else 0f
        targetY = if (cell != null) ((cell / WordSiegeBoardSpec.Size - WordSiegeBoardSpec.CenterRow) / 7f).coerceIn(-1f, 1f) else .2f
        if (!playerTurn && cell == null && reactionUntil < SystemClock.uptimeMillis()) reaction = Reaction.CALM
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        val elapsed = now - startedAt
        if (now - lastBlink > 2_600L + ((elapsed / 2_600L) % 3L) * 450L) lastBlink = now
        val blinkProgress = (now - lastBlink) / 210f
        val blink = if (blinkProgress in 0f..1f) sin(blinkProgress * PI).toFloat().coerceAtLeast(0f) else 0f
        gazeX += (targetX - gazeX) * .14f
        gazeY += (targetY - gazeY) * .14f
        val mood = if (now < reactionUntil) reaction else if (pendingCount > 0) Reaction.FOCUS else Reaction.CALM
        val lid = max(blink, when (mood) {
            Reaction.CALM -> 0f
            Reaction.FOCUS -> .18f
            Reaction.HAPPY -> .32f
            Reaction.SURPRISED -> 0f
            Reaction.SAD -> .25f
        })
        val size = minOf(width, height).toFloat()
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(size / 1_254f, size / 1_254f)
        canvas.translate(0f, sin(elapsed / 640f) * 3f)
        drawLayer(canvas, "orb_face_base")
        drawLayer(canvas, "eye_left")
        drawLayer(canvas, "eye_right")
        drawLayer(canvas, "iris_left", gazeX * 22f, gazeY * 17f)
        drawLayer(canvas, "iris_right", gazeX * 22f, gazeY * 17f)
        drawLid(canvas, 515f, 653f, 127f, 132f, lid)
        drawLid(canvas, 877f, 653f, 126f, 131f, lid)
        val browY = when (mood) {
            Reaction.CALM -> 0f
            Reaction.FOCUS -> 9f
            Reaction.HAPPY -> -12f
            Reaction.SURPRISED -> -25f
            Reaction.SAD -> 12f
        }
        val browRotation = when (mood) {
            Reaction.FOCUS -> -.13f
            Reaction.SAD -> .15f
            else -> 0f
        }
        drawLayer(canvas, "brow_left", 0f, browY, 536f, 458f, browRotation)
        drawLayer(canvas, "brow_right", 0f, browY, 870f, 458f, -browRotation)
        drawLayer(canvas, "cheek_left")
        drawLayer(canvas, "cheek_right")
        canvas.save()
        canvas.translate(708f, 800f)
        val mouthX = when (mood) {
            Reaction.HAPPY -> 1.18f
            Reaction.SURPRISED -> .65f
            Reaction.SAD -> .82f
            else -> 1f
        }
        val mouthY = when (mood) {
            Reaction.FOCUS -> .65f
            Reaction.HAPPY -> 1.3f
            Reaction.SURPRISED -> 1.2f
            Reaction.SAD -> .35f
            else -> 1f
        }
        canvas.scale(mouthX, mouthY)
        canvas.translate(-708f, -800f)
        drawLayer(canvas, "mouth")
        canvas.restore()
        canvas.restore()
        if (isAttachedToWindow && visibility == VISIBLE) postInvalidateDelayed(33L)
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
    pendingCells: Collection<Int>,
    playerTurn: Boolean,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.size(42.dp),
        factory = { context -> WordSiegeMascotView(context) },
        update = { it.updateGame(moveId, lastMoveMine, pendingCells, playerTurn) },
    )
}
