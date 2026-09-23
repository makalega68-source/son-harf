package com.sonharf.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Reusable expression vocabulary for future game events and mascot screens. */
internal enum class WordSiegeMascotEmotion {
    CALM, FOCUS, HAPPY, LAUGH, EXCITED, SURPRISED, SAD, ANGRY, STRESSED,
    PROUD, SPEAKING, TEARY, BOWED, JUMP,
}

/** Local event-driven reaction selection; deterministic per move, with no network or model cost. */
internal object WordSiegeMascotBehavior {
    fun choose(moveId: Long, mine: Boolean, score: Int, captured: Int, stolen: Int): WordSiegeMascotEmotion {
        val variation = Math.floorMod(moveId, 4L).toInt()
        val big = score >= 25 || captured >= 3 || stolen > 0
        return when {
            mine && big && variation % 2 == 0 -> WordSiegeMascotEmotion.JUMP
            mine && big -> WordSiegeMascotEmotion.PROUD
            mine && variation == 3 -> WordSiegeMascotEmotion.JUMP
            mine && variation == 1 -> WordSiegeMascotEmotion.LAUGH
            mine -> WordSiegeMascotEmotion.HAPPY
            big -> if (variation % 2 == 0) WordSiegeMascotEmotion.TEARY else WordSiegeMascotEmotion.BOWED
            else -> WordSiegeMascotEmotion.SURPRISED
        }
    }

    /** How long a one-shot reaction owns the face before the live game context takes over again. */
    fun durationMillis(emotion: WordSiegeMascotEmotion): Long = when (emotion) {
        WordSiegeMascotEmotion.SAD, WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED -> 2_400L
        WordSiegeMascotEmotion.JUMP -> 1_250L
        WordSiegeMascotEmotion.LAUGH -> 1_400L
        WordSiegeMascotEmotion.PROUD -> 1_600L
        WordSiegeMascotEmotion.SURPRISED -> 800L
        WordSiegeMascotEmotion.ANGRY -> 1_100L
        else -> 1_100L
    }
}

/** Decoded once per process; every mascot on screen shares the same immutable layers. */
private object WordSiegeMascotArt {
    val names = listOf(
        "orb_face_base", "eye_left", "eye_right", "iris_left", "iris_right",
        "brow_left", "brow_right", "cheek_left", "cheek_right", "mouth",
    )

    @Volatile private var cache: Map<String, Bitmap>? = null

    fun layers(context: Context): Map<String, Bitmap> = cache ?: synchronized(this) {
        cache ?: names.associateWith { name ->
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.applicationContext.assets.open("word_siege_mascot/$name.webp").use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }?.apply { setHasMipMap(true) } ?: error("Missing mascot layer: $name")
        }.also { cache = it }
    }
}

/**
 * Lightweight, independent 2D rig. The orb, eyes, irises, brows and cheeks come from the approved
 * artwork; eyelids, mouth, tears and effects are vector-drawn so every expression stays crisp.
 *
 * Every facial parameter is driven by a spring toward the current mood's pose, so expressions blend
 * instead of snapping. One-shot moves (hop, jump, bow, laugh) are layered on top as squash-and-stretch
 * body motion anchored at the orb's base.
 */
internal class WordSiegeMascotView(context: Context) : View(context) {
    private val layers: Map<String, Bitmap> = WordSiegeMascotArt.layers(context)
    private val artScale = ART / (layers.getValue("orb_face_base").width.toFloat())
    private val artRect = RectF(0f, 0f, ART, ART)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val atopPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    // Eyelids are painted with the orb's own face texture so they read as the same surface.
    private val skinShader = BitmapShader(layers.getValue("orb_face_base"), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        shader = skinShader
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private val featureMatrix = Matrix()
    private val skinMatrix = Matrix()
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        shader = SweepGradient(ORB_CX, ORB_CY, intArrayOf(
            0xFFFFAD72.toInt(), 0xFF56E4F7.toInt(), 0xFF307AF1.toInt(),
            0xFFB266F5.toInt(), 0xFFFFAD72.toInt(),
        ), floatArrayOf(0f, .25f, .5f, .75f, 1f))
    }
    private val lashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0xFF050A26.toInt()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private val mouthLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF14052A.toInt()
    }
    private val mouthFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF74104E.toInt() }
    private val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cheekPaints = listOf(LEFT_CHEEK_X to CHEEK_Y, RIGHT_CHEEK_X to CHEEK_Y).map { (x, y) ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x, y, 96f, intArrayOf(0xFFFF70B4.toInt(), 0x00FF70B4), null, Shader.TileMode.CLAMP)
        }
    }

    // Reused drawing scratch objects: nothing is allocated per frame.
    private val path = Path()
    private val mouthPath = Path()
    private val layerRect = RectF()
    private val ovalRect = RectF()
    private var eyeLayer = 0

    private val random = Random(SystemClock.uptimeMillis())
    private val pose = FloatArray(POSE_SIZE)
    private val poseValue = FloatArray(POSE_SIZE).also { writePose(WordSiegeMascotEmotion.CALM, it) }
    private val poseVelocity = FloatArray(POSE_SIZE)

    private var lastFrame = 0L
    private var breathPhase = 0f
    private var gazeX = 0f
    private var gazeY = .2f
    private var gazeVelX = 0f
    private var gazeVelY = 0f

    private var nextBlinkAt = SystemClock.uptimeMillis() + 1_600L
    private var blinkStartedAt = -1L
    private var doubleBlinkPending = false
    private var nextSaccadeAt = 0L
    private var saccadeX = 0f
    private var saccadeY = .2f

    private var lastMoveId: Long? = null
    private var hasInitialMove = false
    private var reaction = WordSiegeMascotEmotion.CALM
    private var reactionStartedAt = 0L
    private var reactionUntil = 0L
    private var motionKind = WordSiegeMascotEmotion.CALM
    private var motionStartedAt = 0L
    private var motionUntil = 0L
    private var heartsStartedAt = 0L
    private var sparklesStartedAt = 0L
    private var reactionCell: Int? = null
    private var externalEmotion: WordSiegeMascotEmotion? = null
    private var lastMood = WordSiegeMascotEmotion.CALM

    private var pendingCount = 0
    private var pendingCell: Int? = null
    private var playerTurn = false
    private var urgency = 0f
    private var momentum = 0f
    private var idleGazeX = 0f
    private var idleGazeY = .2f
    private var typingKey = 0
    private var typingAt = 0L
    private var lastActivityAt = SystemClock.uptimeMillis()
    private val tapTimes = LongArray(4)
    private var tapCount = 0

    init {
        contentDescription = "Maskot"
    }

    fun updateGame(
        moveId: Long?, lastMoveMine: Boolean, moveScore: Int, capturedCells: Int,
        opponentCaptured: Int, moveCell: Int?, pendingCells: Collection<Int>, playerTurn: Boolean,
        requestedEmotion: WordSiegeMascotEmotion?,
    ) {
        val now = SystemClock.uptimeMillis()
        if (requestedEmotion != externalEmotion) {
            // A newly requested mood gets the matching body motion once, like a move reaction.
            if (requestedEmotion != null && requestedEmotion.hasBodyMotion() && now >= reactionUntil) {
                startMotion(requestedEmotion, now)
            }
            externalEmotion = requestedEmotion
            markActive(now)
        }
        if (!hasInitialMove) {
            lastMoveId = moveId
            hasInitialMove = true
        } else if (moveId != null && moveId != lastMoveId) {
            lastMoveId = moveId
            startReaction(WordSiegeMascotBehavior.choose(moveId, lastMoveMine, moveScore, capturedCells, opponentCaptured), now)
            reactionCell = moveCell
            if (lastMoveMine) heartsStartedAt = now
            markActive(now)
        }
        if (pendingCells.size != pendingCount) markActive(now)
        if (playerTurn != this.playerTurn) markActive(now)
        pendingCount = pendingCells.size
        pendingCell = pendingCells.lastOrNull()
        this.playerTurn = playerTurn
        invalidate()
    }

    /** Game context that is not tied to a single move: clock pressure, who is ahead, typing. */
    fun updateContext(urgency: Float, momentum: Float, idleGazeX: Float, idleGazeY: Float, typingKey: Int) {
        this.urgency = urgency.coerceIn(0f, 1f)
        this.momentum = momentum.coerceIn(-1f, 1f)
        this.idleGazeX = idleGazeX.coerceIn(-1f, 1f)
        this.idleGazeY = idleGazeY.coerceIn(-1f, 1f)
        if (typingKey != this.typingKey) {
            this.typingKey = typingKey
            val now = SystemClock.uptimeMillis()
            typingAt = now
            markActive(now)
        }
    }

    fun reactToTap() {
        val now = SystemClock.uptimeMillis()
        tapTimes[tapCount % tapTimes.size] = now
        tapCount++
        val poked = tapCount >= tapTimes.size && tapTimes.all { now - it < 2_000L }
        if (poked) {
            tapCount = 0
            startReaction(WordSiegeMascotEmotion.ANGRY, now)
        } else {
            startReaction(WordSiegeMascotEmotion.LAUGH, now)
            reactionUntil = now + 700L
            motionUntil = reactionUntil
        }
        markActive(now)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrame = 0L
        invalidate()
    }

    private fun markActive(now: Long) {
        lastActivityAt = now
    }

    private fun startReaction(emotion: WordSiegeMascotEmotion, now: Long) {
        reaction = emotion
        reactionStartedAt = now
        reactionUntil = now + WordSiegeMascotBehavior.durationMillis(emotion)
        startMotion(emotion, now)
    }

    /** Body motion only; the face keeps following the current mood. */
    private fun startMotion(emotion: WordSiegeMascotEmotion, now: Long) {
        motionKind = emotion
        motionStartedAt = now
        motionUntil = now + WordSiegeMascotBehavior.durationMillis(emotion)
        if (emotion == WordSiegeMascotEmotion.PROUD || emotion == WordSiegeMascotEmotion.JUMP) sparklesStartedAt = now
        // A real face often blinks as it changes expression.
        if (blinkStartedAt < 0L && random.nextFloat() < .55f) nextBlinkAt = now + 40L
    }

    private fun WordSiegeMascotEmotion.hasBodyMotion(): Boolean = when (this) {
        WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.LAUGH, WordSiegeMascotEmotion.JUMP,
        WordSiegeMascotEmotion.PROUD, WordSiegeMascotEmotion.SURPRISED, WordSiegeMascotEmotion.SAD,
        WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED, WordSiegeMascotEmotion.EXCITED -> true
        else -> false
    }

    private fun currentMood(now: Long): WordSiegeMascotEmotion = when {
        now < reactionUntil -> reaction
        externalEmotion != null -> externalEmotion!!
        urgency >= .5f -> WordSiegeMascotEmotion.STRESSED
        pendingCount > 0 -> WordSiegeMascotEmotion.FOCUS
        else -> WordSiegeMascotEmotion.CALM
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 1f / 60f else ((now - lastFrame) / 1000f).coerceIn(0f, .05f)
        lastFrame = now
        val mood = currentMood(now)
        if (mood != lastMood) {
            if (blinkStartedAt < 0L && random.nextFloat() < .35f) nextBlinkAt = now + 30L
            lastMood = mood
        }
        val idleMillis = now - lastActivityAt

        // ---- Pose springs --------------------------------------------------------------------
        writePose(mood, pose)
        if (mood == WordSiegeMascotEmotion.CALM || mood == WordSiegeMascotEmotion.FOCUS) {
            // Being ahead relaxes the face into a smile; falling behind adds a hint of worry.
            pose[P_SMILE] += momentum * .3f
            pose[P_BROW_TILT] += max(0f, -momentum) * .14f
            pose[P_CHEEK] += max(0f, momentum) * .25f
            if (mood == WordSiegeMascotEmotion.CALM && !playerTurn && idleMillis > DROWSY_AFTER) {
                pose[P_LID] = max(pose[P_LID], min(.46f, (idleMillis - DROWSY_AFTER) / 12_000f * .46f))
            }
        }
        if (mood == WordSiegeMascotEmotion.SPEAKING) pose[P_OPEN] = .22f + .38f * abs(sin(now / 105f))
        if (mood == WordSiegeMascotEmotion.LAUGH) pose[P_OPEN] = .7f + .3f * abs(sin(now / 70f))
        val slow = mood.isSorrow()
        val stiffness = if (slow) 55f else 190f
        val damping = if (slow) 2f * sqrt(stiffness) else 1.45f * sqrt(stiffness)
        stepSprings(pose, poseValue, poseVelocity, dt, stiffness, damping)
        val bow = poseValue[P_BOW]

        // ---- Gaze: smart saccades toward what matters in the game ------------------------------
        val focusCell = pendingCell ?: reactionCell?.takeIf { now < reactionUntil }
        var targetX: Float
        var targetY: Float
        when {
            focusCell != null -> {
                targetX = ((focusCell % WordSiegeBoardSpec.Size - WordSiegeBoardSpec.CenterColumn) / 7f).coerceIn(-1f, 1f)
                targetY = ((focusCell / WordSiegeBoardSpec.Size - WordSiegeBoardSpec.CenterRow) / 7f).coerceIn(-1f, 1f)
            }
            now - typingAt < 900L -> {
                targetX = idleGazeX * .4f
                targetY = .95f
            }
            mood == WordSiegeMascotEmotion.SURPRISED || mood == WordSiegeMascotEmotion.JUMP ||
                mood == WordSiegeMascotEmotion.LAUGH || mood == WordSiegeMascotEmotion.HAPPY -> {
                // Celebrations look at the player.
                targetX = 0f
                targetY = -.05f
            }
            else -> {
                if (now >= nextSaccadeAt) {
                    val bored = idleMillis > 8_000L
                    val spread = when {
                        urgency > .3f -> .55f
                        bored -> .75f
                        else -> .38f
                    }
                    if (random.nextFloat() < .22f) {
                        saccadeX = 0f
                        saccadeY = 0f
                    } else {
                        saccadeX = (idleGazeX + (random.nextFloat() * 2f - 1f) * spread).coerceIn(-1f, 1f)
                        saccadeY = (idleGazeY + (random.nextFloat() * 2f - 1f) * spread * .6f).coerceIn(-1f, 1f)
                    }
                    val interval = if (urgency > .3f) 350L + random.nextLong(450L) else 900L + random.nextLong(2_300L)
                    nextSaccadeAt = now + interval
                    // Large eye jumps are often accompanied by a blink.
                    if (blinkStartedAt < 0L && random.nextFloat() < .18f) nextBlinkAt = now + 20L
                }
                targetX = saccadeX
                targetY = saccadeY
            }
        }
        if (mood == WordSiegeMascotEmotion.PROUD) targetY = min(targetY, -.25f)
        targetY = max(targetY, bow * .95f)
        val gazeK = 520f
        val gazeC = 2f * sqrt(gazeK) * .9f
        gazeVelX += (gazeK * (targetX - gazeX) - gazeC * gazeVelX) * dt
        gazeVelY += (gazeK * (targetY - gazeY) - gazeC * gazeVelY) * dt
        gazeX += gazeVelX * dt
        gazeY += gazeVelY * dt
        val tremor = sin(now / 47f) * .012f

        // ---- Blink scheduler: quick close, slower open, occasional double blink ---------------
        if (blinkStartedAt < 0L && now >= nextBlinkAt) {
            blinkStartedAt = now
        }
        var blink = 0f
        if (blinkStartedAt >= 0L) {
            val t = (now - blinkStartedAt).toFloat()
            blink = when {
                t < 70f -> easeIn(t / 70f)
                t < 100f -> 1f
                t < 230f -> 1f - easeOut((t - 100f) / 130f)
                else -> 0f
            }
            if (t >= 230f) {
                blinkStartedAt = -1L
                if (doubleBlinkPending) {
                    doubleBlinkPending = false
                    nextBlinkAt = now + 70L
                } else {
                    doubleBlinkPending = random.nextFloat() < .2f
                    val drowsy = idleMillis > DROWSY_AFTER && !playerTurn
                    nextBlinkAt = if (doubleBlinkPending) now + 70L
                        else now + (if (drowsy) 1_300L else 2_200L) + random.nextLong(if (drowsy) 1_500L else 3_600L)
                }
            }
        }

        // ---- Body motion: breathing + one-shot reaction timeline --------------------------------
        val breathRate = when {
            mood == WordSiegeMascotEmotion.STRESSED || urgency > .3f -> 5.4f
            slow -> 1.1f
            idleMillis > DROWSY_AFTER -> 1.0f
            else -> 1.6f
        }
        breathPhase = (breathPhase + dt * breathRate) % (2f * PI.toFloat())
        val breathDepth = if (slow) 1.6f else 1f
        val breath = sin(breathPhase)
        var dx = 0f
        var dy = -breath * 4f * breathDepth + bow * 34f
        var sx = 1f - breath * .006f * breathDepth + poseValue[P_PUFF] + bow * .02f
        var sy = 1f + breath * .013f * breathDepth + poseValue[P_PUFF] - bow * .045f
        var rotation = poseValue[P_TILT] + gazeX * 2.2f + sin(now / 2_300f) * 1.1f

        if (now < motionUntil) {
            val motionT = ((now - motionStartedAt).toFloat() / (motionUntil - motionStartedAt)).coerceIn(0f, 1f)
            val m = bodyMotion(motionKind, motionT)
            dx += m[0]; dy += m[1]; sx *= m[2]; sy *= m[3]; rotation += m[4]
        }
        if (mood == WordSiegeMascotEmotion.STRESSED || urgency > .3f) {
            dx += sin(now / 26f) * 1.8f * max(urgency, .4f)
        }

        // ---- Draw -------------------------------------------------------------------------------
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(size / ART, size / ART)
        canvas.translate(dx, dy)
        canvas.rotate(rotation, ORB_CX, ORB_BOTTOM)
        canvas.scale(sx, sy, ORB_CX, ORB_BOTTOM)

        drawLayer(canvas, "orb_face_base")
        val glow = if (mood == WordSiegeMascotEmotion.PROUD || mood == WordSiegeMascotEmotion.EXCITED) 45f else 0f
        edgePaint.alpha = (180f + 25f * sin(now / 950f) + glow).toInt().coerceIn(0, 255)
        ovalRect.set(ORB_CX - ORB_RX, ORB_CY - ORB_RY, ORB_CX + ORB_RX, ORB_CY + ORB_RY)
        canvas.drawOval(ovalRect, edgePaint)

        // Face features ride slightly ahead of the orb: they follow the gaze and fold downward when
        // the head bows, which gives the round body a sense of depth.
        featureMatrix.setTranslate((gazeX + tremor) * 16f, gazeY * 10f + bow * 62f)
        featureMatrix.preScale(1f, 1f - bow * .12f, FACE_CX, FACE_CY)
        // The lid texture is counter-transformed so it always lines up with the orb underneath,
        // otherwise the moving features would reveal seams at the lid edges.
        if (featureMatrix.invert(skinMatrix)) {
            skinMatrix.preScale(artScale, artScale)
            skinShader.setLocalMatrix(skinMatrix)
        }
        canvas.save()
        canvas.concat(featureMatrix)

        val lid = max(blink, poseValue[P_LID]).coerceIn(0f, 1f)
        val lower = max(poseValue[P_LOWER], blink * .25f).coerceIn(0f, 1f)
        val water = poseValue[P_WATER].coerceIn(0f, 1f)
        val slant = poseValue[P_SLANT]
        drawEye(canvas, "eye_left", "iris_left", LEFT_EYE_X, LEFT_IRIS_X, gazeX + tremor, gazeY, water)
        drawLids(canvas, LEFT_EYE_X, EYE_Y, 128f, 128f, lid, slant, -1f, lower)
        canvas.restoreToCount(eyeLayer)
        drawEye(canvas, "eye_right", "iris_right", RIGHT_EYE_X, RIGHT_IRIS_X, gazeX + tremor, gazeY, water)
        drawLids(canvas, RIGHT_EYE_X, EYE_Y, 122f, 126f, lid, slant, 1f, lower)
        canvas.restoreToCount(eyeLayer)

        val browY = poseValue[P_BROW_Y] - lid * 6f
        val browTilt = Math.toDegrees(poseValue[P_BROW_TILT].toDouble()).toFloat()
        drawLayer(canvas, "brow_left", 0f, browY, LEFT_BROW_X, BROW_Y, -browTilt)
        drawLayer(canvas, "brow_right", 0f, browY, RIGHT_BROW_X, BROW_Y, browTilt)

        val cheek = poseValue[P_CHEEK].coerceIn(0f, 1f)
        cheekPaints.forEachIndexed { index, cheekPaint ->
            cheekPaint.alpha = (18f + cheek * 92f).toInt()
            canvas.drawCircle(if (index == 0) LEFT_CHEEK_X else RIGHT_CHEEK_X, CHEEK_Y, 96f, cheekPaint)
        }
        drawLayer(canvas, "cheek_left", 0f, -lower * 10f)
        drawLayer(canvas, "cheek_right", 0f, -lower * 10f)

        drawMouth(canvas, poseValue[P_SMILE], poseValue[P_OPEN].coerceIn(0f, 1f), poseValue[P_WIDTH])
        if (mood == WordSiegeMascotEmotion.TEARY) drawTears(canvas, now)
        canvas.restore()

        if (mood == WordSiegeMascotEmotion.STRESSED || urgency > .45f) drawSweat(canvas, now)
        drawSparkles(canvas, now)
        drawHearts(canvas, now)
        canvas.restore()

        if (isAttachedToWindow && isShown) postInvalidateOnAnimation()
    }

    // ---- Expression poses ------------------------------------------------------------------------

    private fun writePose(mood: WordSiegeMascotEmotion, out: FloatArray) {
        val values = when (mood) {
            WordSiegeMascotEmotion.CALM -> CALM_POSE
            WordSiegeMascotEmotion.FOCUS -> FOCUS_POSE
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.JUMP -> HAPPY_POSE
            WordSiegeMascotEmotion.LAUGH -> LAUGH_POSE
            WordSiegeMascotEmotion.EXCITED -> EXCITED_POSE
            WordSiegeMascotEmotion.SURPRISED -> SURPRISED_POSE
            WordSiegeMascotEmotion.SAD -> SAD_POSE
            WordSiegeMascotEmotion.TEARY -> TEARY_POSE
            WordSiegeMascotEmotion.BOWED -> BOWED_POSE
            WordSiegeMascotEmotion.ANGRY -> ANGRY_POSE
            WordSiegeMascotEmotion.STRESSED -> STRESSED_POSE
            WordSiegeMascotEmotion.PROUD -> PROUD_POSE
            WordSiegeMascotEmotion.SPEAKING -> SPEAKING_POSE
        }
        values.copyInto(out)
    }

    private fun WordSiegeMascotEmotion.isSorrow(): Boolean =
        this == WordSiegeMascotEmotion.SAD || this == WordSiegeMascotEmotion.TEARY || this == WordSiegeMascotEmotion.BOWED

    private fun stepSprings(target: FloatArray, value: FloatArray, velocity: FloatArray, dt: Float, k: Float, c: Float) {
        var remaining = dt
        while (remaining > 0f) {
            val h = min(remaining, 1f / 120f)
            for (i in value.indices) {
                velocity[i] += (k * (target[i] - value[i]) - c * velocity[i]) * h
                value[i] += velocity[i] * h
            }
            remaining -= h
        }
    }

    private val motion = FloatArray(5)

    /** Returns dx, dy, scaleX, scaleY, rotation for a one-shot reaction at normalized time [t]. */
    private fun bodyMotion(emotion: WordSiegeMascotEmotion, t: Float): FloatArray {
        motion[0] = 0f; motion[1] = 0f; motion[2] = 1f; motion[3] = 1f; motion[4] = 0f
        when (emotion) {
            WordSiegeMascotEmotion.JUMP -> jump(t, height = 112f, crouch = .15f, spin = 7f)
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.EXCITED -> {
                jump(t, height = 62f, crouch = .09f, spin = 0f)
                motion[4] += 6f * sin(t * 4f * PI.toFloat()) * (1f - t)
            }
            WordSiegeMascotEmotion.LAUGH -> {
                val shake = abs(sin(t * 7f * PI.toFloat()))
                motion[1] = -shake * 16f * (1f - t * .5f)
                motion[3] = 1f - shake * .045f
                motion[2] = 1f + shake * .03f
                motion[4] = 3.2f * sin(t * 7f * PI.toFloat()) * (1f - t)
            }
            WordSiegeMascotEmotion.PROUD -> {
                val rise = easeOut(min(1f, t * 3f)) * (1f - easeIn(max(0f, t - .75f) / .25f))
                motion[1] = -14f * rise
                motion[3] = 1f + .03f * rise
            }
            WordSiegeMascotEmotion.SURPRISED -> {
                val pop = if (t < .14f) easeOut(t / .14f) else 1f - easeInOut((t - .14f) / .86f)
                motion[1] = -22f * pop
                motion[2] = 1f - .04f * pop
                motion[3] = 1f + .08f * pop
            }
            WordSiegeMascotEmotion.SAD, WordSiegeMascotEmotion.TEARY, WordSiegeMascotEmotion.BOWED -> {
                // A heavy sigh: the body deflates as the head goes down, then settles.
                val sigh = sin(min(1f, t / .55f) * PI.toFloat())
                motion[1] = 10f * sigh
                motion[2] = 1f + .025f * sigh
                motion[3] = 1f - .04f * sigh
            }
            WordSiegeMascotEmotion.ANGRY -> {
                motion[0] = 7f * sin(t * 12f * PI.toFloat()) * (1f - t)
                motion[3] = 1f - .03f * (1f - t)
            }
            else -> Unit
        }
        return motion
    }

    /** Anticipation crouch -> stretched take-off -> airborne arc -> squash landing -> damped wobble. */
    private fun jump(t: Float, height: Float, crouch: Float, spin: Float) {
        val takeOff = .16f
        val land = .62f
        val settle = .74f
        when {
            t < takeOff -> {
                val e = easeOut(t / takeOff)
                motion[1] = 20f * e
                motion[2] = 1f + crouch * .75f * e
                motion[3] = 1f - crouch * e
            }
            t < land -> {
                val u = (t - takeOff) / (land - takeOff)
                val arc = 4f * u * (1f - u)
                val stretch = abs(1f - 2f * u).pow(1.5f)
                motion[1] = -height * arc + 20f * (1f - min(1f, u * 5f))
                motion[2] = 1f - .07f * stretch
                motion[3] = 1f + .11f * stretch
                motion[4] = spin * sin(u * 2f * PI.toFloat())
            }
            t < settle -> {
                val e = sin((t - land) / (settle - land) * PI.toFloat())
                motion[1] = 14f * e
                motion[2] = 1f + crouch * .7f * e
                motion[3] = 1f - crouch * .85f * e
            }
            else -> {
                val w = (t - settle) / (1f - settle)
                val wobble = sin(w * 3f * PI.toFloat()) * (1f - w) * (1f - w)
                motion[2] = 1f - .04f * wobble
                motion[3] = 1f + .05f * wobble
            }
        }
    }

    // ---- Drawing helpers ---------------------------------------------------------------------------

    private fun drawEye(
        canvas: Canvas, eye: String, iris: String, eyeX: Float, irisX: Float,
        lookX: Float, lookY: Float, water: Float,
    ) {
        // Iris and lids are composited SRC_ATOP onto the eye, so neither can spill outside it.
        // The caller draws the lids into the same layer, then restores [eyeLayer].
        layerRect.set(eyeX - 150f, EYE_Y - 150f, eyeX + 150f, EYE_Y + 150f)
        eyeLayer = canvas.saveLayer(layerRect, null)
        drawLayer(canvas, eye)
        val irisScale = poseValue[P_IRIS]
        canvas.save()
        canvas.translate(lookX.coerceIn(-1f, 1f) * 24f, lookY.coerceIn(-1f, 1f) * 20f)
        canvas.scale(irisScale, irisScale, irisX, IRIS_Y)
        canvas.drawBitmap(layers.getValue(iris), null, artRect, atopPaint)
        canvas.restore()
        if (water > .02f) {
            // Glassy tear film along the lower lid.
            detailPaint.color = 0xFFBDF4FF.toInt()
            detailPaint.alpha = (water * 150f).toInt()
            detailPaint.xfermode = atopPaint.xfermode
            ovalRect.set(eyeX - 92f, EYE_Y + 56f, eyeX + 92f, EYE_Y + 104f)
            canvas.drawOval(ovalRect, detailPaint)
            detailPaint.xfermode = null
            detailPaint.alpha = 255
        }
    }

    private fun drawLids(
        canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float,
        amount: Float, slant: Float, outerSide: Float, lower: Float,
    ) {
        val left = cx - rx - 12f
        val right = cx + rx + 12f
        if (amount > .01f) {
            // Upper lid edge: an arc that sags in the middle; slant lowers the outer corner (sad)
            // or the inner corner (angry).
            val edge = cy - ry + amount * ry * 1.72f
            val outerDrop = slant * 38f
            val innerDrop = -slant * 18f
            val yLeft = edge - 20f + if (outerSide < 0f) outerDrop else innerDrop
            val yRight = edge - 20f + if (outerSide > 0f) outerDrop else innerDrop
            val sag = 20f + 10f * (1f - amount)
            path.rewind()
            path.moveTo(left, cy - ry - 40f)
            path.lineTo(right, cy - ry - 40f)
            path.lineTo(right, yRight)
            path.quadTo(cx, edge + sag, left, yLeft)
            path.close()
            canvas.drawPath(path, skinPaint)
            path.rewind()
            path.moveTo(right - 18f, yRight + 4f)
            path.quadTo(cx, edge + sag, left + 18f, yLeft + 4f)
            lashPaint.strokeWidth = 9f
            lashPaint.alpha = (min(1f, amount * 6f) * 255f).toInt()
            canvas.drawPath(path, lashPaint)
        }
        if (lower > .01f) {
            // Lower lid rises into a "^" shape: the cheek push of a genuine smile.
            val edge = cy + ry - lower * ry * 1.1f
            path.rewind()
            path.moveTo(left, cy + ry + 40f)
            path.lineTo(right, cy + ry + 40f)
            path.lineTo(right, edge + 22f)
            path.quadTo(cx, edge - 30f * lower, left, edge + 22f)
            path.close()
            canvas.drawPath(path, skinPaint)
            path.rewind()
            path.moveTo(right - 22f, edge + 18f)
            path.quadTo(cx, edge - 30f * lower, left + 22f, edge + 18f)
            lashPaint.strokeWidth = 7f
            lashPaint.alpha = (min(1f, lower * 5f) * 200f).toInt()
            canvas.drawPath(path, lashPaint)
        }
    }

    /**
     * Vector mouth matched to the artwork's colours. [smile] runs from -1 (frown) to 1 (grin),
     * [open] from a closed line to a wide open "D", [widthScale] stretches the corners.
     */
    private fun drawMouth(canvas: Canvas, smile: Float, open: Float, widthScale: Float) {
        val halfW = 78f * widthScale.coerceIn(.45f, 1.5f)
        val corner = MOUTH_Y - smile * 30f
        val top = MOUTH_Y - smile * 8f - open * 26f + max(0f, -smile) * -18f
        val bottom = max(top + 10f + open * 40f, MOUTH_Y + smile * 34f + open * 108f - max(0f, -smile) * 22f)
        mouthPath.rewind()
        mouthPath.moveTo(MOUTH_X - halfW, corner)
        mouthPath.cubicTo(MOUTH_X - halfW * .45f, top, MOUTH_X + halfW * .45f, top, MOUTH_X + halfW, corner)
        mouthPath.cubicTo(MOUTH_X + halfW * .5f, bottom, MOUTH_X - halfW * .5f, bottom, MOUTH_X - halfW, corner)
        mouthPath.close()

        if (open < .1f) {
            // Nearly closed: a single soft line keeps the expression readable.
            mouthLinePaint.strokeWidth = 12f
            canvas.drawPath(mouthPath, mouthLinePaint)
            return
        }
        layerRect.set(MOUTH_X - halfW - 20f, min(top, corner) - 20f, MOUTH_X + halfW + 20f, bottom + 20f)
        val checkpoint = canvas.saveLayer(layerRect, null)
        canvas.drawPath(mouthPath, mouthFillPaint)
        val tongueTop = bottom - (bottom - top) * .52f
        tonguePaint.shader = null
        tonguePaint.color = 0xFFFC77D9.toInt()
        tonguePaint.alpha = (min(1f, (open - .1f) * 4f) * 255f).toInt()
        ovalRect.set(MOUTH_X - halfW * .62f, tongueTop, MOUTH_X + halfW * .62f, bottom + (bottom - top) * .45f)
        canvas.drawOval(ovalRect, tonguePaint)
        canvas.restoreToCount(checkpoint)
        mouthLinePaint.strokeWidth = 10f
        canvas.drawPath(mouthPath, mouthLinePaint)
    }

    private fun drawTears(canvas: Canvas, now: Long) {
        val t = ((now - motionStartedAt) / WordSiegeMascotBehavior.durationMillis(WordSiegeMascotEmotion.TEARY).toFloat()).coerceIn(0f, 1f)
        // The drop swells on the lid, then runs down the cheek and fades.
        val grow = easeOut(min(1f, t / .3f))
        val fall = easeIn(max(0f, t - .3f) / .7f)
        detailPaint.color = 0xFF99E9FF.toInt()
        detailPaint.alpha = (215f * (1f - max(0f, t - .85f) / .15f)).toInt().coerceIn(0, 255)
        val r = 11f + 11f * grow
        drawDrop(canvas, LEFT_EYE_X - 62f - fall * 14f, EYE_Y + 96f + fall * 170f, r)
        drawDrop(canvas, RIGHT_EYE_X + 58f + fall * 14f, EYE_Y + 96f + fall * 170f, r)
        detailPaint.alpha = 255
    }

    private fun drawDrop(canvas: Canvas, x: Float, y: Float, r: Float) {
        path.rewind()
        path.moveTo(x, y - r * 1.9f)
        path.cubicTo(x - r * .9f, y - r * .3f, x - r, y + r, x, y + r)
        path.cubicTo(x + r, y + r, x + r * .9f, y - r * .3f, x, y - r * 1.9f)
        path.close()
        canvas.drawPath(path, detailPaint)
    }

    private fun drawSweat(canvas: Canvas, now: Long) {
        val t = (now % 1_300L) / 1_300f
        val x = 1_010f
        val y = 440f + easeIn(t) * 90f
        detailPaint.color = 0xFFA6ECFF.toInt()
        detailPaint.alpha = (230f * (1f - t)).toInt()
        drawDrop(canvas, x, y, 17f)
        detailPaint.alpha = 255
    }

    private fun drawSparkles(canvas: Canvas, now: Long) {
        val age = now - sparklesStartedAt
        if (sparklesStartedAt == 0L || age !in 0L..1_500L) return
        val colors = intArrayOf(0xFFFFE08A.toInt(), 0xFFFFFFFF.toInt(), 0xFF8AF1FF.toInt(), 0xFFFFC46B.toInt())
        for (i in 0 until 4) {
            val p = ((age - i * 110L) / 1_000f)
            if (p <= 0f || p >= 1f) continue
            val a = sin(p * PI.toFloat())
            val x = SPARKLE_X[i]
            val y = SPARKLE_Y[i] - p * 40f
            val r = 34f * a
            detailPaint.color = colors[i]
            detailPaint.alpha = (a * 255f).toInt()
            path.rewind()
            path.moveTo(x, y - r)
            path.quadTo(x, y, x + r, y)
            path.quadTo(x, y, x, y + r)
            path.quadTo(x, y, x - r, y)
            path.quadTo(x, y, x, y - r)
            path.close()
            canvas.drawPath(path, detailPaint)
        }
        detailPaint.alpha = 255
    }

    private fun drawHearts(canvas: Canvas, now: Long) {
        val age = now - heartsStartedAt
        if (heartsStartedAt == 0L || age !in 0L..1_350L) return
        val colors = intArrayOf(0xFFFF4F89.toInt(), 0xFFFF8CAE.toInt(), 0xFFFF658C.toInt(), 0xFFFF649E.toInt())
        for (index in 0 until 4) {
            val progress = ((age - index * 125L) / 900f).coerceIn(0f, 1f)
            if (progress <= 0f || progress >= 1f) continue
            val alpha = (255f * minOf(1f, progress * 5f, (1f - progress) * 3f)).toInt()
            val x = 330f + index * 190f + sin(progress * 5f + index) * 22f
            val y = 330f - progress * (220f + index * 30f)
            val radius = (38f + index * 4f) * (.7f + .3f * easeOut(min(1f, progress * 3f)))
            path.rewind()
            path.moveTo(x, y + radius)
            path.cubicTo(x - radius * 2f, y - radius * .1f, x - radius, y - radius * 1.5f, x, y - radius * .38f)
            path.cubicTo(x + radius, y - radius * 1.5f, x + radius * 2f, y - radius * .1f, x, y + radius)
            path.close()
            detailPaint.color = colors[index]
            detailPaint.alpha = alpha
            canvas.drawPath(path, detailPaint)
        }
        detailPaint.alpha = 255
    }

    private fun drawLayer(
        canvas: Canvas, name: String, dx: Float = 0f, dy: Float = 0f,
        pivotX: Float = 0f, pivotY: Float = 0f, rotation: Float = 0f,
    ) {
        val bitmap = layers[name] ?: return
        if (dx == 0f && dy == 0f && rotation == 0f) {
            canvas.drawBitmap(bitmap, null, artRect, paint)
            return
        }
        canvas.save()
        canvas.translate(dx, dy)
        if (rotation != 0f) canvas.rotate(rotation, pivotX, pivotY)
        canvas.drawBitmap(bitmap, null, artRect, paint)
        canvas.restore()
    }

    private companion object {
        const val ART = 1_254f
        const val ORB_CX = 660f
        const val ORB_CY = 641f
        const val ORB_RX = 400f
        const val ORB_RY = 411f
        const val ORB_BOTTOM = 1_050f
        const val FACE_CX = 700f
        const val FACE_CY = 680f
        const val LEFT_EYE_X = 510f
        const val RIGHT_EYE_X = 890f
        const val EYE_Y = 650f
        const val LEFT_IRIS_X = 544f
        const val RIGHT_IRIS_X = 859f
        const val IRIS_Y = 662f
        const val LEFT_BROW_X = 536f
        const val RIGHT_BROW_X = 873f
        const val BROW_Y = 458f
        const val LEFT_CHEEK_X = 470f
        const val RIGHT_CHEEK_X = 928f
        const val CHEEK_Y = 800f
        const val MOUTH_X = 708f
        const val MOUTH_Y = 790f
        const val DROWSY_AFTER = 22_000L
        val SPARKLE_X = floatArrayOf(250f, 1_080f, 330f, 1_010f)
        val SPARKLE_Y = floatArrayOf(330f, 360f, 900f, 880f)

        const val P_BROW_Y = 0
        const val P_BROW_TILT = 1
        const val P_LID = 2
        const val P_SLANT = 3
        const val P_LOWER = 4
        const val P_SMILE = 5
        const val P_OPEN = 6
        const val P_WIDTH = 7
        const val P_CHEEK = 8
        const val P_BOW = 9
        const val P_PUFF = 10
        const val P_TILT = 11
        const val P_WATER = 12
        const val P_IRIS = 13
        const val POSE_SIZE = 14

        //                                   browY  tilt   lid   slant  lower smile  open  width cheek bow    puff   tilt water iris
        val CALM_POSE = floatArrayOf(        0f,    0f,   .06f,  0f,   .06f,  .55f, .45f, 1f,   .3f,  0f,    0f,    0f,  0f,  1f)
        val FOCUS_POSE = floatArrayOf(       10f,  -.12f, .24f, -.12f, .04f,  .08f, .06f, .78f, .15f, .05f,   0f,    0f,  0f,  .96f)
        val HAPPY_POSE = floatArrayOf(      -24f,   .04f,  0f,   0f,   .5f,   1f,   .78f, 1.28f, .85f, 0f,   .01f,   0f,  0f,  1.02f)
        val LAUGH_POSE = floatArrayOf(      -28f,   .08f, .12f,  0f,   .82f,  1f,   .9f,  1.32f, 1f,  -.05f, .015f,  0f,  0f,  1f)
        val EXCITED_POSE = floatArrayOf(    -32f,   .02f,  0f,   0f,   .22f,  .95f, .92f, 1.2f, .95f, -.1f,  .025f,  0f,  0f,  1.05f)
        val SURPRISED_POSE = floatArrayOf(  -38f,   .1f,   0f,   0f,   0f,    0f,   .95f, .74f, .35f, -.12f, .02f,   0f,  0f,  .84f)
        val SAD_POSE = floatArrayOf(         14f,   .34f, .4f,   .42f, .1f,  -.85f, .08f, .8f,  .08f, .65f,  -.01f,  5f,  .6f, 1f)
        val TEARY_POSE = floatArrayOf(       16f,   .38f, .44f,  .46f, .22f, -.9f,  .14f, .78f, .12f, .6f,   -.01f,  4f,  1f,  1.03f)
        val BOWED_POSE = floatArrayOf(       18f,   .36f, .56f,  .48f, .12f, -.8f,  .06f, .74f, .06f, 1f,    -.02f,  7f,  .7f, 1f)
        val ANGRY_POSE = floatArrayOf(       18f,  -.38f, .3f,  -.45f, .15f, -.55f, .18f, .92f, .45f, .1f,    .02f, -2f,  0f,  .9f)
        val STRESSED_POSE = floatArrayOf(     6f,   .24f, .1f,   .14f, .06f, -.4f,  .34f, .88f, .1f,  .1f,    0f,    0f,  .2f, .88f)
        val PROUD_POSE = floatArrayOf(      -20f,  -.06f, .3f,   0f,   .3f,   .9f,  .22f, 1.22f, .7f, -.35f,  .04f, -3f,  0f,  1f)
        val SPEAKING_POSE = floatArrayOf(    -6f,    0f,  .04f,  0f,   .06f,  .3f,  .4f,  .9f,  .3f,  0f,    0f,    0f,  0f,  1f)

        fun easeIn(t: Float): Float = t.coerceIn(0f, 1f).let { it * it }
        fun easeOut(t: Float): Float = t.coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) }
        fun easeInOut(t: Float): Float = t.coerceIn(0f, 1f).let { .5f - .5f * kotlin.math.cos(it * PI.toFloat()) }
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
    urgency: Float = 0f,
    momentum: Float = 0f,
    idleGazeX: Float = 0f,
    idleGazeY: Float = .2f,
    typingKey: Int = 0,
    onTap: () -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> WordSiegeMascotView(context).apply { isClickable = true } },
        update = {
            it.updateContext(urgency, momentum, idleGazeX, idleGazeY, typingKey)
            it.updateGame(moveId, lastMoveMine, moveScore, capturedCells, opponentCaptured, moveCell, pendingCells, playerTurn, requestedEmotion)
            it.setOnClickListener { view ->
                (view as WordSiegeMascotView).reactToTap()
                onTap()
            }
        },
    )
}
