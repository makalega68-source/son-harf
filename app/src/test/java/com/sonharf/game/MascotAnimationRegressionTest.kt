package com.sonharf.game

import java.io.File
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class MascotAnimationRegressionTest {
    @Test
    fun jumpAnticipatesThenFliesLandsAndPreservesVolumeWithoutSpinning() {
        val pose = FloatArray(5)
        mascotJumpPose(.20f, 88f, .12f, 3f, pose)
        assertTrue("Crouch before takeoff", pose[1] > 0f && pose[3] < 1f)
        mascotJumpPose(.44f, 88f, .12f, 3f, pose)
        assertTrue("Airborne arc", pose[1] < -80f)
        mascotJumpPose(.74f, 88f, .12f, 3f, pose)
        assertTrue("Squash on landing", pose[1] > 0f && pose[3] < 1f)
        for (frame in 0..600) {
            mascotJumpPose(frame / 600f, 88f, .12f, 3f, pose)
            assertTrue(pose.all { it.isFinite() })
            assertEquals(1f, pose[2] * pose[3], .00001f)
            assertTrue("Only a gentle tilt", abs(pose[4]) <= 8f)
        }
        assertArrayEquals(floatArrayOf(0f, 0f, 1f, 1f, 0f), pose, .0001f)
        for (boundary in floatArrayOf(.22f, .66f, .82f)) {
            val before = FloatArray(5)
            mascotJumpPose(boundary - .00001f, 88f, .12f, 3f, before)
            mascotJumpPose(boundary + .00001f, 88f, .12f, 3f, pose)
            for (i in pose.indices) assertEquals("Continuous channel $i at $boundary", before[i], pose[i], .05f)
        }
    }

    @Test
    fun interruptedSpringKeepsItsPoseAndSettlesAtDifferentFrameRates() {
        for (fps in intArrayOf(20, 30, 60, 120)) {
            val value = floatArrayOf(0f)
            val velocity = floatArrayOf(0f)
            val target = floatArrayOf(-80f)
            repeat(fps / 2) { stepMascotSprings(target, value, velocity, 1f / fps, 260f, 27f) }
            val previous = value[0]
            target[0] = 14f // A landing interrupts a hop; no reset of position or velocity.
            stepMascotSprings(target, value, velocity, 1f / fps, 260f, 27f)
            assertTrue("No jump to neutral or next target", value[0] < -40f)
            assertTrue(value[0] > previous)
            repeat(fps * 2) { stepMascotSprings(target, value, velocity, 1f / fps, 260f, 27f) }
            assertEquals(14f, value[0], .01f)
            assertEquals(0f, velocity[0], .02f)
        }
    }

    @Test
    fun eatingHasFourChewsAndContinuousOpenAndSwallowPhases() {
        var peaks = 0
        for (i in 1 until 519) {
            val t = .28f + i / 1000f
            val open = mascotEatingOpen(t)
            if (open > mascotEatingOpen(t - .001f) && open >= mascotEatingOpen(t + .001f)) peaks++
            assertTrue(open in 0f..1f)
        }
        assertEquals(4, peaks)
        assertEquals(0f, mascotEatingOpen(1f), .0001f)
        for (boundary in floatArrayOf(.16f, .28f, .80f)) {
            assertEquals(mascotEatingOpen(boundary - .00001f), mascotEatingOpen(boundary + .00001f), .001f)
        }
    }

    @Test
    fun screenKnockHasExactlyThreeSeparatedContactsAndNoBoundaryJump() {
        var peaks = 0
        for (i in 1 until 999) {
            val t = i / 1000f
            val pulse = mascotKnockPulse(t)
            assertTrue(pulse in 0f..1f)
            if (pulse > mascotKnockPulse(t - .001f) && pulse >= mascotKnockPulse(t + .001f)) peaks++
        }
        assertEquals(3, peaks)
        assertEquals(0f, mascotKnockPulse(0f), .0001f)
        assertEquals(0f, mascotKnockPulse(1f), .0001f)
        for (boundary in floatArrayOf(.25f, .68f)) {
            assertEquals(mascotKnockPulse(boundary - .00001f), mascotKnockPulse(boundary + .00001f), .001f)
        }
    }

    @Test
    fun headTurnsOccludeOnlyTheDistantEyeContinuouslyAndSymmetrically() {
        assertEquals(1f, mascotFarEyeVisibility(0f, true), .0001f)
        assertEquals(1f, mascotFarEyeVisibility(0f, false), .0001f)
        assertEquals(0f, mascotFarEyeVisibility(1f, true), .0001f)
        assertEquals(1f, mascotFarEyeVisibility(1f, false), .0001f)
        for (i in -1000..1000) {
            val yaw = i / 1000f
            val left = mascotFarEyeVisibility(yaw, true)
            val right = mascotFarEyeVisibility(yaw, false)
            assertTrue(left in 0f..1f && right in 0f..1f)
            assertEquals(left, mascotFarEyeVisibility(-yaw, false), .0001f)
            assertEquals(1f, maxOf(left, right), .0001f)
            assertTrue(abs(left - mascotFarEyeVisibility(yaw + .001f, true)) < .004f)
        }
    }

    @Test
    fun mascotActionsHaveSaneDurations() {
        for (action in WordSiegeMascotAction.entries) assertTrue(mascotActionMillis(action) in 800L..6_000L)
        for (idle in listOf(WordSiegeMascotAction.LOOK_AROUND, WordSiegeMascotAction.STRETCH,
            WordSiegeMascotAction.PEEK, WordSiegeMascotAction.SWAY, WordSiegeMascotAction.THINK,
            WordSiegeMascotAction.YAWN, WordSiegeMascotAction.DOZE)) {
            assertTrue(mascotActionMillis(idle) in 2_000L..6_000L)
        }
    }

    @Test
    fun drawLoopUsesPersistentPoseAndReusesEffectBuffers() {
        val view = File("src/main/java/com/sonharf/game/WordSiegeMascotView.kt").readText()
        val draw = view.substringAfter("override fun onDraw(canvas: Canvas)").substringBefore("// ---- Expression poses")
        assertTrue(draw.contains("stepSprings(bodyTarget, bodyValue, bodyVelocity"))
        assertTrue(draw.contains("delayedBodyPose(now - 90L, followTarget)"))
        assertFalse("The tuft's root must not trail the head", draw.contains("tuftTarget"))
        // A worn hat hides the tuft and the robot's antenna.
        assertTrue(draw.contains("drawHat(canvas, now)"))
        assertTrue(draw.contains("decor.drawTuft(canvas, now)\n            decor.drawCrown(canvas)"))
        assertTrue(draw.contains("rotation.coerceIn(-8f, 8f)"))
        for (allocation in listOf("floatArrayOf(", "intArrayOf(", "listOf(", "Paint(", "Path(", "RectF(", "Matrix(")) {
            assertFalse(allocation, Regex("(?<![A-Za-z0-9_])" + Regex.escape(allocation)).containsMatchIn(draw))
        }
        assertFalse(view.contains("for ((ix, side) in listOf("))
        assertFalse(view.contains("for (cx in floatArrayOf("))
    }

    @Test
    fun mascotRoomIsGoneAndCelebrationsStayUpright() {
        assertFalse(File("src/main/java/com/sonharf/game/MascotRoomScreen.kt").exists())
        val home = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        assertFalse(home.contains("MascotRoom"))
        val view = File("src/main/java/com/sonharf/game/WordSiegeMascotView.kt").readText()
        assertTrue(view.contains("rotation.coerceIn(-8f, 8f)"))
    }
}
