package com.sonharf.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

/** Debug-only contact sheets of the real Canvas rig, never a second drawing implementation. */
internal class MascotRigInspectionView(context: Context, private val stage: String) : View(context) {
    private data class Sample(val yaw: Float, val pitch: Float, val mood: WordSiegeMascotEmotion,
        val skin: WordSiegeMascotSkin = WordSiegeMascotSkin.ORB, val blink: Boolean = false)
    private val samples = when (stage) {
        "skins" -> WordSiegeMascotSkin.entries.flatMap { skin ->
            listOf(Sample(-1f, 0f, WordSiegeMascotEmotion.HAPPY, skin), Sample(0f, 0f, WordSiegeMascotEmotion.HAPPY, skin), Sample(1f, 0f, WordSiegeMascotEmotion.HAPPY, skin))
        }
        "expressions" -> WordSiegeMascotEmotion.entries.mapIndexed { i, mood -> Sample(if (i % 3 == 0) -.9f else if (i % 3 == 2) .9f else 0f, 0f, mood) } +
            listOf(Sample(-1f, 0f, WordSiegeMascotEmotion.CALM, blink = true), Sample(0f, 0f, WordSiegeMascotEmotion.CALM, blink = true), Sample(1f, 0f, WordSiegeMascotEmotion.CALM, blink = true))
        else -> listOf(-1f, -.5f, 0f, .5f, 1f).flatMap { yaw -> listOf(-1f, 0f, 1f).map { pitch -> Sample(yaw, pitch, WordSiegeMascotEmotion.HAPPY) } }
    }
    private val rigs = samples.map { sample -> WordSiegeMascotView(context).apply { setSkin(sample.skin) } }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF243E32.toInt(); textAlign = Paint.Align.CENTER }
    private val fields = WordSiegeMascotView::class.java.declaredFields.associateBy { it.name }.onEach { (_, field) -> field.isAccessible = true }
    private val poseMethod = WordSiegeMascotView::class.java.getDeclaredMethod("writePose", WordSiegeMascotEmotion::class.java, FloatArray::class.java).apply { isAccessible = true }
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFFF3EEE4.toInt())
        val cell = minOf(width / 3f, (height - 70f) / ((samples.size + 2) / 3))
        labelPaint.textSize = 24f
        canvas.drawText("MASKOT · $stage", width / 2f, 34f, labelPaint)
        val now = SystemClock.uptimeMillis()
        samples.forEachIndexed { i, sample ->
            val rig = rigs[i]
            val yaw = if (stage == "motion") sin(now / 1500f) else sample.yaw
            for ((name, value) in mapOf("headX" to yaw, "headY" to sample.pitch, "gazeX" to yaw, "gazeY" to sample.pitch, "idleGazeX" to yaw, "idleGazeY" to sample.pitch, "headVelX" to 0f, "headVelY" to 0f)) fields.getValue(name).setFloat(rig, value)
            fields.getValue("externalEmotion").set(rig, sample.mood)
            fields.getValue("lastMood").set(rig, sample.mood)
            fields.getValue("nextBlinkAt").setLong(rig, Long.MAX_VALUE)
            fields.getValue("blinkStartedAt").setLong(rig, if (sample.blink) now - 85L else -1L)
            val pose = fields.getValue("poseValue").get(rig) as FloatArray
            poseMethod.invoke(rig, sample.mood, pose)
            val mouth = fields.getValue("mouthValue").get(rig) as FloatArray
            mouth[0] = pose[WordSiegeMascotView.P_SMILE]; mouth[1] = pose[WordSiegeMascotView.P_OPEN]; mouth[2] = pose[WordSiegeMascotView.P_WIDTH]
            rig.layout(0, 0, cell.toInt(), cell.toInt())
            canvas.save()
            canvas.translate((i % 3) * cell, 50f + (i / 3) * cell)
            rig.draw(canvas)
            labelPaint.textSize = 17f
            canvas.drawText(if (stage == "skins") sample.skin.titleTr else if (stage == "expressions") if(sample.blink) "BLINK" else sample.mood.name else "${sample.yaw} / ${sample.pitch}", cell / 2f, cell - 4f, labelPaint)
            canvas.restore()
        }
        if (stage == "motion") postInvalidateOnAnimation()
    }
}
