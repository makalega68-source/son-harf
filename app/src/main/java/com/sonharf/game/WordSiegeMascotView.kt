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

/** Whole-body moves the companion brain can ask for; each one is rare and short. */
internal enum class WordSiegeMascotAction {
    HOP, TWIRL, FLIP, LOOK_AROUND, NOD, SPARKLE, CHEER, LAND, SHRUG, PEEK, FLINCH, YAWN,
}

/** Costume worn over the orb; purely cosmetic. */
internal enum class WordSiegeMascotHat { NONE, PARTY, CROWN }

/** Look of the mascot: the original blue orb, or the pink girl with a bow and eyelashes. */
internal enum class WordSiegeMascotSkin { ORB, PINK }

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

    // Translucent glowing wings, only visible while flying.
    private val wingPath = Path().apply {
        moveTo(0f, 0f)
        cubicTo(110f, -230f, 330f, -270f, 430f, -160f)
        cubicTo(370f, -100f, 310f, -50f, 240f, -26f)
        cubicTo(310f, 6f, 270f, 72f, 175f, 62f)
        cubicTo(110f, 56f, 40f, 40f, 0f, 0f)
        close()
    }
    private val wingFeatherPath = Path().apply {
        moveTo(30f, -10f)
        quadTo(200f, -150f, 380f, -150f)
        moveTo(40f, 5f)
        quadTo(190f, -60f, 250f, -30f)
    }
    private val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, 430f, -160f,
            intArrayOf(0xE6A8F4FF.toInt(), 0xB39A7CFF.toInt(), 0x66FFFFFF),
            floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP,
        )
    }
    // Skin: the pink girl recolours the artwork (eye whites stay white) and adds a bow and lashes.
    private var skin = WordSiegeMascotSkin.ORB
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pinkFilter = android.graphics.ColorMatrixColorFilter(PINK_MATRIX)
    private val orbEdgeShader: Shader? = edgePaint.shader
    private val pinkEdgeShader = SweepGradient(ORB_CX, ORB_CY, intArrayOf(
        0xFFFFC1E3.toInt(), 0xFFFF7AC8.toInt(), 0xFFE0409A.toInt(),
        0xFFC77DFF.toInt(), 0xFFFFC1E3.toInt(),
    ), floatArrayOf(0f, .25f, .5f, .75f, 1f))
    private val orbWingShader: Shader? = wingPaint.shader
    private val pinkWingShader = android.graphics.LinearGradient(
        0f, 0f, 430f, -160f,
        intArrayOf(0xE6FFD1EC.toInt(), 0xB3FF6FBF.toInt(), 0x66FFFFFF),
        floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP,
    )
    private val girlLashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
        color = 0xFF1A0A2E.toInt()
    }
    private val bowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bowLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 8f
        color = 0xFF7A1450.toInt()
    }
    private val wingLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        color = 0xCCFFFFFF.toInt()
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
    private var actionKind: WordSiegeMascotAction? = null
    private var actionKey = 0L
    private var actionStartedAt = 0L
    private var actionUntil = 0L
    private var flying = false
    private var flightDirection = 0f
    private var speaking = false
    private var watching = false
    private var wing = 0f
    private var wingVelocity = 0f
    private var nextTwinkleAt = SystemClock.uptimeMillis() + 6_000L
    private var twinkleStartedAt = 0L
    private val twinkleX = FloatArray(3)
    private val twinkleY = FloatArray(3)
    private var glanceKey = 0
    private var glanceUntil = 0L
    private var glanceX = 0f
    private var glanceY = 0f
    private var hat = WordSiegeMascotHat.NONE
    private var sleeping = false
    private var nextYawnAt = 0L
    private val zPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B5CFF.toInt()
        textSize = 120f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val hatPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hatLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 10f
        color = 0xFF3A1F5C.toInt()
    }
    private var heartsStartedAt = 0L
    private var sparklesStartedAt = 0L
    private var reactionCell: Int? = null
    private var moveGlanceUntil = 0L
    private var lastMoveReactionAt = 0L
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
            // Big moments (win, loss, round) always move; everyday moods only occasionally.
            val major = requestedEmotion == WordSiegeMascotEmotion.EXCITED || requestedEmotion == WordSiegeMascotEmotion.BOWED ||
                requestedEmotion == WordSiegeMascotEmotion.TEARY || requestedEmotion == WordSiegeMascotEmotion.JUMP
            if (requestedEmotion != null && requestedEmotion.hasBodyMotion() && now >= reactionUntil &&
                (major || now - lastMoveReactionAt > 20_000L)
            ) {
                if (!major) lastMoveReactionAt = now
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
            reactionCell = moveCell
            moveGlanceUntil = now + 1_300L
            // Mostly it just watches: a full reaction only for strong moves and not too often;
            // an ordinary move gets, at most now and then, a small change of face.
            val big = moveScore >= 25 || capturedCells >= 3 || opponentCaptured > 0
            val sinceLast = now - lastMoveReactionAt
            if (big && sinceLast > 12_000L) {
                lastMoveReactionAt = now
                startReaction(WordSiegeMascotBehavior.choose(moveId, lastMoveMine, moveScore, capturedCells, opponentCaptured), now)
                if (lastMoveMine) heartsStartedAt = now
            } else if (!big && sinceLast > 35_000L && random.nextFloat() < .3f) {
                lastMoveReactionAt = now
                reaction = if (lastMoveMine) WordSiegeMascotEmotion.HAPPY else WordSiegeMascotEmotion.SURPRISED
                reactionStartedAt = now
                reactionUntil = now + 1_000L
            }
            markActive(now)
        }
        if (pendingCells.size != pendingCount) markActive(now)
        if (playerTurn != this.playerTurn) markActive(now)
        pendingCount = pendingCells.size
        pendingCell = pendingCells.lastOrNull()
        this.playerTurn = playerTurn
        invalidate()
    }

    /**
     * Requests from the companion brain. [actionKey] changes each time a new [action] should play;
     * [flying] shows the wings, [speaking] animates the mouth while a speech bubble is visible and
     * [watching] makes the mascot calm and attentive instead of fidgeting.
     */
    fun updateCompanion(
        actionKey: Long, action: WordSiegeMascotAction?, flying: Boolean, flightDirection: Float,
        speaking: Boolean, watching: Boolean,
        glanceKey: Int = 0, glanceX: Float = 0f, glanceY: Float = 0f,
        hat: WordSiegeMascotHat = WordSiegeMascotHat.NONE,
    ) {
        val now = SystemClock.uptimeMillis()
        if (actionKey != this.actionKey) {
            this.actionKey = actionKey
            if (action != null) perform(action, now)
        }
        if (glanceKey != this.glanceKey) {
            // The player touched somewhere: a quick look in that direction.
            this.glanceKey = glanceKey
            this.glanceX = glanceX.coerceIn(-1f, 1f)
            this.glanceY = glanceY.coerceIn(-1f, 1f)
            glanceUntil = now + 850L
            markActive(now)
        }
        this.hat = hat
        if (flying != this.flying) markActive(now)
        this.flying = flying
        this.flightDirection = flightDirection.coerceIn(-1f, 1f)
        this.speaking = speaking
        this.watching = watching
    }

    private fun perform(action: WordSiegeMascotAction, now: Long) {
        val duration = when (action) {
            WordSiegeMascotAction.HOP -> 850L
            WordSiegeMascotAction.TWIRL -> 1_800L
            WordSiegeMascotAction.FLIP -> 3_000L
            WordSiegeMascotAction.LOOK_AROUND -> 2_100L
            WordSiegeMascotAction.NOD -> 800L
            WordSiegeMascotAction.SPARKLE -> 1_300L
            WordSiegeMascotAction.CHEER -> 1_500L
            WordSiegeMascotAction.LAND -> 520L
            WordSiegeMascotAction.SHRUG -> 950L
            WordSiegeMascotAction.PEEK -> 1_600L
            WordSiegeMascotAction.FLINCH -> 520L
            WordSiegeMascotAction.YAWN -> 2_300L
        }
        actionKind = action
        actionStartedAt = now
        actionUntil = now + duration
        // The face follows the move unless a stronger game reaction is already showing.
        val face = when (action) {
            WordSiegeMascotAction.HOP, WordSiegeMascotAction.TWIRL -> WordSiegeMascotEmotion.HAPPY
            WordSiegeMascotAction.FLIP -> WordSiegeMascotEmotion.LAUGH
            WordSiegeMascotAction.SPARKLE -> WordSiegeMascotEmotion.PROUD
            WordSiegeMascotAction.CHEER -> WordSiegeMascotEmotion.EXCITED
            WordSiegeMascotAction.FLINCH -> WordSiegeMascotEmotion.SURPRISED
            else -> null
        }
        if (action == WordSiegeMascotAction.FLINCH && blinkStartedAt < 0L) nextBlinkAt = now
        if (face != null && now >= reactionUntil) {
            reaction = face
            reactionStartedAt = now
            reactionUntil = now + duration
        }
        if (action == WordSiegeMascotAction.TWIRL || action == WordSiegeMascotAction.SPARKLE ||
            action == WordSiegeMascotAction.CHEER || action == WordSiegeMascotAction.FLIP
        ) sparklesStartedAt = now
        invalidate()
    }

    fun setSkin(next: WordSiegeMascotSkin) {
        if (next == skin) return
        skin = next
        val filter = if (next == WordSiegeMascotSkin.PINK) pinkFilter else null
        paint.colorFilter = filter
        atopPaint.colorFilter = filter
        skinPaint.colorFilter = filter
        edgePaint.shader = if (next == WordSiegeMascotSkin.PINK) pinkEdgeShader else orbEdgeShader
        wingPaint.shader = if (next == WordSiegeMascotSkin.PINK) pinkWingShader else orbWingShader
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
            // Just a happy face; the companion flies it away, so no extra body motion here.
            reaction = WordSiegeMascotEmotion.HAPPY
            reactionStartedAt = now
            reactionUntil = now + 900L
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
        if (sleeping) {
            // Woken up: a little startle, then back to normal.
            sleeping = false
            startReaction(WordSiegeMascotEmotion.SURPRISED, now)
        }
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
        // Long quiet stretches: an occasional yawn, then it dozes off until something happens.
        val restful = mood == WordSiegeMascotEmotion.CALM && !playerTurn && !flying
        sleeping = restful && idleMillis > SLEEP_AFTER
        if (restful && !sleeping && idleMillis > DROWSY_AFTER + 4_000L && now >= nextYawnAt) {
            perform(WordSiegeMascotAction.YAWN, now)
            nextYawnAt = now + 14_000L + random.nextLong(12_000L)
        }
        if (sleeping) {
            pose[P_LID] = 1f
            pose[P_SMILE] = .25f
            pose[P_OPEN] = .12f
            pose[P_BOW] = .25f
        }
        if (mood == WordSiegeMascotEmotion.SPEAKING) pose[P_OPEN] = .22f + .38f * abs(sin(now / 105f))
        if (mood == WordSiegeMascotEmotion.LAUGH) pose[P_OPEN] = .7f + .3f * abs(sin(now / 70f))
        val slow = mood.isSorrow()
        val stiffness = if (slow) 55f else 190f
        val damping = if (slow) 2f * sqrt(stiffness) else 1.45f * sqrt(stiffness)
        stepSprings(pose, poseValue, poseVelocity, dt, stiffness, damping)
        val bow = poseValue[P_BOW]

        // ---- Gaze: smart saccades toward what matters in the game ------------------------------
        val focusCell = pendingCell ?: reactionCell?.takeIf { now < max(reactionUntil, moveGlanceUntil) }
        var targetX: Float
        var targetY: Float
        val lookAround = actionKind == WordSiegeMascotAction.LOOK_AROUND && now < actionUntil
        when {
            flying -> {
                targetX = flightDirection
                targetY = -.15f
            }
            now < glanceUntil -> {
                targetX = glanceX
                targetY = glanceY
            }
            actionKind == WordSiegeMascotAction.PEEK && now < actionUntil -> {
                targetX = if (idleGazeX >= 0f) .9f else -.9f
                targetY = -.6f
            }
            lookAround -> {
                val t = (now - actionStartedAt).toFloat() / (actionUntil - actionStartedAt)
                targetX = when {
                    t < .3f -> -.95f
                    t < .38f -> 0f
                    t < .7f -> .95f
                    else -> 0f
                }
                targetY = if (t < .7f) -.2f else .15f
            }
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
                    val bored = idleMillis > 8_000L && !watching
                    val spread = when {
                        urgency > .3f -> .55f
                        watching -> .16f
                        bored -> .75f
                        else -> .38f
                    }
                    if (random.nextFloat() < if (watching) .1f else .22f) {
                        saccadeX = 0f
                        saccadeY = 0f
                    } else {
                        saccadeX = (idleGazeX + (random.nextFloat() * 2f - 1f) * spread).coerceIn(-1f, 1f)
                        saccadeY = (idleGazeY + (random.nextFloat() * 2f - 1f) * spread * .6f).coerceIn(-1f, 1f)
                    }
                    val interval = when {
                        urgency > .3f -> 350L + random.nextLong(450L)
                        // Watching the game: long steady looks, few eye jumps.
                        watching -> 2_400L + random.nextLong(3_200L)
                        else -> 900L + random.nextLong(2_300L)
                    }
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
            sleeping -> .75f
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
        val sway = if (watching) .35f else 1.1f
        var rotation = poseValue[P_TILT] + gazeX * (if (lookAround) 4.5f else 2.2f) + sin(now / 2_300f) * sway
        var spin = 0f

        // Wings unfold with a slightly bouncy spring and fold away after landing.
        val wingK = 110f
        wingVelocity += (wingK * ((if (flying) 1f else 0f) - wing) - 2f * sqrt(wingK) * .7f * wingVelocity) * dt
        wing = (wing + wingVelocity * dt).coerceIn(0f, 1.15f)
        if (wing > .01f) {
            rotation += flightDirection * 7f * min(1f, wing)
            dy += sin(now / 140f) * 9f * min(1f, wing)
            // The body draws in a little while airborne so the wings fit inside the view.
            sx *= 1f - .19f * min(1f, wing)
            sy *= 1f - .13f * min(1f, wing)
        }

        if (now < motionUntil) {
            val motionT = ((now - motionStartedAt).toFloat() / (motionUntil - motionStartedAt)).coerceIn(0f, 1f)
            val m = bodyMotion(motionKind, motionT)
            dx += m[0]; dy += m[1]; sx *= m[2]; sy *= m[3]; rotation += m[4]; spin += m[5]
        }
        val activeAction = actionKind
        if (activeAction != null && now < actionUntil) {
            val actionT = ((now - actionStartedAt).toFloat() / (actionUntil - actionStartedAt)).coerceIn(0f, 1f)
            val m = actionMotion(activeAction, actionT)
            dx += m[0]; dy += m[1]; sx *= m[2]; sy *= m[3]; rotation += m[4]; spin += m[5]
        }
        if (mood == WordSiegeMascotEmotion.STRESSED || urgency > .3f) {
            dx += sin(now / 40f) * 1.1f * max(urgency, .4f)
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
        if (spin != 0f) canvas.rotate(spin, ORB_CX, ORB_CY)

        if (wing > .01f) drawWings(canvas, now)
        drawLayer(canvas, "orb_face_base")
        val sparkling = actionKind == WordSiegeMascotAction.SPARKLE && now < actionUntil
        val glow = if (mood == WordSiegeMascotEmotion.PROUD || mood == WordSiegeMascotEmotion.EXCITED || sparkling) 45f else 0f
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

        val faceT = if (actionKind != null && now < actionUntil) {
            ((now - actionStartedAt).toFloat() / (actionUntil - actionStartedAt)).coerceIn(0f, 1f)
        } else {
            -1f
        }
        val yawn = if (actionKind == WordSiegeMascotAction.YAWN && faceT >= 0f) sin(faceT * PI.toFloat()).let { it * it } else 0f
        val shrug = if (actionKind == WordSiegeMascotAction.SHRUG && faceT >= 0f) sin(faceT * PI.toFloat()) else 0f
        val lid = max(max(blink, poseValue[P_LID]), yawn * .9f).coerceIn(0f, 1f)
        val lower = max(poseValue[P_LOWER], blink * .25f).coerceIn(0f, 1f)
        val water = poseValue[P_WATER].coerceIn(0f, 1f)
        val slant = poseValue[P_SLANT]
        drawEye(canvas, "eye_left", "iris_left", LEFT_EYE_X, LEFT_IRIS_X, gazeX + tremor, gazeY, water)
        drawLids(canvas, LEFT_EYE_X, EYE_Y, 128f, 128f, lid, slant, -1f, lower)
        canvas.restoreToCount(eyeLayer)
        if (skin == WordSiegeMascotSkin.PINK) drawGirlLashes(canvas, LEFT_EYE_X, EYE_Y, 128f, 128f, lid, -1f)
        drawEye(canvas, "eye_right", "iris_right", RIGHT_EYE_X, RIGHT_IRIS_X, gazeX + tremor, gazeY, water)
        drawLids(canvas, RIGHT_EYE_X, EYE_Y, 122f, 126f, lid, slant, 1f, lower)
        canvas.restoreToCount(eyeLayer)
        if (skin == WordSiegeMascotSkin.PINK) drawGirlLashes(canvas, RIGHT_EYE_X, EYE_Y, 122f, 126f, lid, 1f)

        val browY = poseValue[P_BROW_Y] - lid * 6f - yawn * 22f - shrug * 26f
        val browTilt = Math.toDegrees(poseValue[P_BROW_TILT].toDouble()).toFloat()
        drawLayer(canvas, "brow_left", 0f, browY, LEFT_BROW_X, BROW_Y, -browTilt)
        drawLayer(canvas, "brow_right", 0f, browY, RIGHT_BROW_X, BROW_Y, browTilt)

        val cheek = poseValue[P_CHEEK].coerceIn(0f, 1f)
        cheekPaints.forEachIndexed { index, cheekPaint ->
            val blush = if (skin == WordSiegeMascotSkin.PINK) 40f else 18f
            cheekPaint.alpha = (blush + cheek * 92f).toInt().coerceIn(0, 255)
            canvas.drawCircle(if (index == 0) LEFT_CHEEK_X else RIGHT_CHEEK_X, CHEEK_Y, 96f, cheekPaint)
        }
        drawLayer(canvas, "cheek_left", 0f, -lower * 10f)
        drawLayer(canvas, "cheek_right", 0f, -lower * 10f)

        // Talking: irregular syllables layered on the current expression.
        val mouthOpen = if (speaking) {
            max(.14f, .16f + .46f * abs(sin(now / 88f) * sin(now / 231f + 1.3f)))
        } else {
            poseValue[P_OPEN].coerceIn(0f, 1f)
        }
        val mouthWidth = if (speaking) min(poseValue[P_WIDTH], 1.08f) else poseValue[P_WIDTH]
        drawMouth(
            canvas,
            poseValue[P_SMILE] * (1f - yawn) - .1f * yawn + (.1f - poseValue[P_SMILE]) * shrug * .7f,
            max(mouthOpen, yawn),
            mouthWidth + (.62f - mouthWidth) * yawn,
        )
        if (mood == WordSiegeMascotEmotion.TEARY) drawTears(canvas, now)
        canvas.restore()

        if (mood == WordSiegeMascotEmotion.STRESSED || urgency > .45f) drawSweat(canvas, now)
        if (hat != WordSiegeMascotHat.NONE) {
            drawHat(canvas, now)
        } else if (skin == WordSiegeMascotSkin.PINK) {
            drawBow(canvas, now)
        }
        if (sleeping) drawSleepZ(canvas, now)
        drawSparkles(canvas, now)
        drawTwinkles(canvas, now)
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

    private val motion = FloatArray(6)

    private fun resetMotion() {
        motion[0] = 0f; motion[1] = 0f; motion[2] = 1f; motion[3] = 1f; motion[4] = 0f; motion[5] = 0f
    }

    /** Returns dx, dy, scaleX, scaleY, rotation, spin for a one-shot reaction at normalized time [t]. */
    private fun bodyMotion(emotion: WordSiegeMascotEmotion, t: Float): FloatArray {
        resetMotion()
        when (emotion) {
            WordSiegeMascotEmotion.JUMP -> jump(t, height = 105f, crouch = .13f, spin = 3f)
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.EXCITED -> {
                jump(t, height = 56f, crouch = .08f, spin = 0f)
                motion[4] += 3f * sin(t * 2f * PI.toFloat()) * (1f - t)
            }
            WordSiegeMascotEmotion.LAUGH -> {
                // A few relaxed giggle bounces, not a frantic shake.
                val shake = abs(sin(t * 3f * PI.toFloat()))
                motion[1] = -shake * 12f * (1f - t * .5f)
                motion[3] = 1f - shake * .035f
                motion[2] = 1f + shake * .025f
                motion[4] = 1.5f * sin(t * 3f * PI.toFloat()) * (1f - t)
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
                motion[0] = 4f * sin(t * 6f * PI.toFloat()) * (1f - t)
                motion[3] = 1f - .03f * (1f - t)
            }
            else -> Unit
        }
        return motion
    }

    /** Body motion for a companion action, in the same format as [bodyMotion]. */
    private fun actionMotion(action: WordSiegeMascotAction, t: Float): FloatArray {
        resetMotion()
        val pi = PI.toFloat()
        when (action) {
            WordSiegeMascotAction.HOP -> jump(t, height = 58f, crouch = .09f, spin = 0f)
            WordSiegeMascotAction.CHEER -> {
                jump(t, height = 88f, crouch = .12f, spin = 0f)
                motion[4] += 3f * sin(t * 2f * pi) * (1f - t)
            }
            WordSiegeMascotAction.TWIRL -> {
                // A playful pirouette around its own centre with a little lift and a landing squash.
                val e = easeInOut(t)
                motion[5] = 360f * e
                motion[1] = -36f * sin(t * pi)
                val land = if (t > .82f) sin((t - .82f) / .18f * pi) else 0f
                motion[2] = 1f + .07f * land
                motion[3] = 1f - .08f * land
            }
            WordSiegeMascotAction.FLIP -> {
                // Hops up, turns upside down, wiggles there giggling, then rolls back upright.
                when {
                    t < .1f -> {
                        val e = easeOut(t / .1f)
                        motion[1] = 16f * e
                        motion[3] = 1f - .1f * e
                        motion[2] = 1f + .07f * e
                    }
                    t < .3f -> {
                        val u = (t - .1f) / .2f
                        motion[5] = 180f * easeInOut(u)
                        motion[1] = -60f * easeOut(u)
                    }
                    t < .72f -> {
                        val u = (t - .3f) / .42f
                        motion[5] = 180f + 5f * sin(u * 2f * pi) * (1f - u * .5f)
                        motion[1] = -60f + 6f * sin(u * 2f * pi)
                    }
                    t < .9f -> {
                        val u = (t - .72f) / .18f
                        motion[5] = 180f + 180f * easeInOut(u)
                        motion[1] = -60f * (1f - easeIn(u))
                    }
                    else -> {
                        val e = sin((t - .9f) / .1f * pi)
                        motion[5] = 360f
                        motion[1] = 10f * e
                        motion[2] = 1f + .07f * e
                        motion[3] = 1f - .09f * e
                    }
                }
            }
            WordSiegeMascotAction.NOD -> {
                val dip = abs(sin(t * 2f * pi))
                motion[1] = 12f * dip
                motion[3] = 1f - .025f * dip
            }
            WordSiegeMascotAction.SPARKLE -> {
                val puff = sin(t * pi)
                motion[2] = 1f + .03f * puff
                motion[3] = 1f + .04f * puff
                motion[1] = -10f * puff
            }
            WordSiegeMascotAction.LAND -> {
                val e = sin(t * pi) * (1f - t * .3f)
                motion[1] = 12f * e
                motion[2] = 1f + .09f * e
                motion[3] = 1f - .11f * e
            }
            WordSiegeMascotAction.SHRUG -> {
                val bump = sin(t * pi)
                motion[1] = -16f * bump
                motion[3] = 1f + .05f * bump
                motion[4] = 3f * sin(t * 2f * pi)
            }
            WordSiegeMascotAction.PEEK -> {
                // Stretches up on tiptoe and leans toward whatever it is curious about.
                val lean = sin(min(1f, t / .35f) * pi / 2f) * (1f - easeIn(max(0f, t - .75f) / .25f))
                val side = if (idleGazeX >= 0f) 1f else -1f
                motion[4] = side * 9f * lean
                motion[1] = -18f * lean
                motion[3] = 1f + .07f * lean
                motion[2] = 1f - .03f * lean
            }
            WordSiegeMascotAction.FLINCH -> {
                val e = sin(t * pi)
                motion[1] = 14f * e
                motion[3] = 1f - .1f * e
                motion[2] = 1f + .06f * e
                motion[0] = -6f * e
            }
            WordSiegeMascotAction.YAWN -> {
                val e = sin(t * pi)
                motion[3] = 1f + .06f * e * e
                motion[2] = 1f - .02f * e
                motion[1] = -10f * e
                motion[4] = -3f * e
            }
            WordSiegeMascotAction.LOOK_AROUND -> Unit
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
        // Drops keep coming while the mascot cries; the two eyes run out of phase.
        val age = now - motionStartedAt
        drawTear(canvas, ((age % TEAR_CYCLE) / TEAR_CYCLE.toFloat()), LEFT_EYE_X - 62f, -1f)
        drawTear(canvas, (((age + TEAR_CYCLE * 45 / 100) % TEAR_CYCLE) / TEAR_CYCLE.toFloat()), RIGHT_EYE_X + 58f, 1f)
        detailPaint.alpha = 255
    }

    private fun drawTear(canvas: Canvas, t: Float, x: Float, side: Float) {
        // The drop swells on the lid, then runs down the cheek and fades.
        val grow = easeOut(min(1f, t / .32f))
        val fall = easeIn(max(0f, t - .32f) / .68f)
        val alpha = (215f * (1f - max(0f, t - .82f) / .18f)).toInt().coerceIn(0, 255)
        detailPaint.color = 0xFF99E9FF.toInt()
        detailPaint.alpha = alpha
        val r = 10f + 11f * grow
        val tx = x + side * fall * 16f
        val ty = EYE_Y + 96f + fall * 175f
        drawDrop(canvas, tx, ty, r)
        // A tiny highlight makes the drop read as water.
        detailPaint.color = 0xFFFFFFFF.toInt()
        detailPaint.alpha = (alpha * .8f).toInt()
        canvas.drawCircle(tx - r * .3f, ty - r * .1f, r * .28f, detailPaint)
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

    private fun drawWings(canvas: Canvas, now: Long) {
        // Fast flaps in flight; the wings spread as they unfold.
        val open = min(1f, wing)
        val flap = (sin(now / 75f) * 26f + 8f) * open
        val alpha = (open * 255f).toInt().coerceIn(0, 255)
        wingPaint.alpha = alpha
        wingLinePaint.alpha = (alpha * .7f).toInt()
        var side = -1
        while (side <= 1) {
            canvas.save()
            canvas.translate(ORB_CX + side * 395f, 520f)
            canvas.scale(side * (.48f + .14f * open), .48f + .14f * open)
            canvas.rotate(-flap)
            canvas.drawPath(wingPath, wingPaint)
            canvas.drawPath(wingFeatherPath, wingLinePaint)
            canvas.restore()
            side += 2
        }
    }

    /** Three curled lashes on the outer upper corner; they follow the lid and droop when closed. */
    private fun drawGirlLashes(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, lid: Float, side: Float) {
        for (i in 0 until 3) {
            val angle = (-.18f - i * .3f).toDouble()
            val baseX = cx + side * rx * kotlin.math.cos(angle).toFloat() * .98f
            val openY = cy + ry * sin(angle).toFloat() * .98f
            val baseY = openY + lid * (cy + ry * .55f - openY)
            // Open: flick outward and up. Closed: hang outward and down.
            val lift = 1f - 2f * lid.coerceIn(0f, 1f)
            val length = 44f - i * 5f
            val dirX = side * (1.05f - i * .25f)
            val dirY = -(.35f + i * .35f) * lift
            path.rewind()
            path.moveTo(baseX, baseY)
            path.quadTo(
                baseX + dirX * length * .7f, baseY + dirY * length * .2f - 8f * lift,
                baseX + dirX * length, baseY + dirY * length,
            )
            canvas.drawPath(path, girlLashPaint)
        }
    }

    /** A pink ribbon bow on the top-right of the head. */
    private fun drawBow(canvas: Canvas, now: Long) {
        canvas.save()
        canvas.translate(880f, 232f)
        canvas.rotate(20f + sin(now / 900f) * 3f)
        for (side in BOW_SIDES) {
            canvas.save()
            canvas.scale(side, 1f)
            // Ribbon tail.
            path.rewind()
            path.moveTo(8f, 14f)
            path.lineTo(46f, 112f)
            path.lineTo(26f, 100f)
            path.lineTo(10f, 118f)
            path.lineTo(-6f, 18f)
            path.close()
            bowPaint.color = 0xFFE0409A.toInt()
            canvas.drawPath(path, bowPaint)
            canvas.drawPath(path, bowLinePaint)
            // Loop.
            path.rewind()
            path.moveTo(0f, 0f)
            path.cubicTo(55f, -78f, 150f, -52f, 142f, 8f)
            path.cubicTo(136f, 66f, 52f, 52f, 0f, 0f)
            path.close()
            bowPaint.color = 0xFFFF5FAE.toInt()
            canvas.drawPath(path, bowPaint)
            canvas.drawPath(path, bowLinePaint)
            bowPaint.color = 0x88FFFFFF.toInt()
            ovalRect.set(62f, -32f, 112f, -10f)
            canvas.drawOval(ovalRect, bowPaint)
            canvas.restore()
        }
        bowPaint.color = 0xFFFF86C4.toInt()
        canvas.drawCircle(0f, 4f, 27f, bowPaint)
        canvas.drawCircle(0f, 4f, 27f, bowLinePaint)
        canvas.restore()
    }

    private fun drawSleepZ(canvas: Canvas, now: Long) {
        // Three soft "z" letters drift up from the side of the head.
        for (i in 0 until 3) {
            val p = ((now + i * 700L) % 2_100L) / 2_100f
            zPaint.textSize = 70f + 40f * p
            zPaint.alpha = (sin(p * PI.toFloat()) * 220f).toInt()
            canvas.drawText("z", 1_000f + p * 90f + sin(p * 6f) * 14f, 360f - p * 260f, zPaint)
        }
    }

    private fun drawHat(canvas: Canvas, now: Long) {
        canvas.save()
        // Sits a little into the rim so the whole hat stays inside the view.
        canvas.translate(0f, 28f)
        canvas.rotate(-14f + sin(now / 700f) * 2f, 560f, 250f)
        if (hat == WordSiegeMascotHat.CROWN) {
            path.rewind()
            path.moveTo(400f, 260f)
            path.lineTo(420f, 110f)
            path.lineTo(490f, 190f)
            path.lineTo(560f, 70f)
            path.lineTo(630f, 190f)
            path.lineTo(700f, 110f)
            path.lineTo(720f, 260f)
            path.close()
            hatPaint.shader = null
            hatPaint.color = 0xFFFFC94A.toInt()
            canvas.drawPath(path, hatPaint)
            canvas.drawPath(path, hatLinePaint)
            hatPaint.color = 0xFFFF4F89.toInt()
            canvas.drawCircle(560f, 200f, 20f, hatPaint)
            hatPaint.color = 0xFF56E4F7.toInt()
            canvas.drawCircle(470f, 220f, 14f, hatPaint)
            canvas.drawCircle(650f, 220f, 14f, hatPaint)
        } else {
            // Party cone with stripes and a pom-pom.
            path.rewind()
            path.moveTo(440f, 260f)
            path.lineTo(560f, 20f)
            path.lineTo(680f, 260f)
            path.close()
            hatPaint.color = 0xFFB266F5.toInt()
            canvas.drawPath(path, hatPaint)
            hatPaint.color = 0xFF56E4F7.toInt()
            canvas.save()
            canvas.clipPath(path)
            for (i in 0 until 3) canvas.drawRect(420f, 90f + i * 70f, 700f, 115f + i * 70f, hatPaint)
            canvas.restore()
            canvas.drawPath(path, hatLinePaint)
            hatPaint.color = 0xFFFFE08A.toInt()
            canvas.drawCircle(560f, 22f, 30f, hatPaint)
        }
        canvas.restore()
    }

    private fun drawTwinkles(canvas: Canvas, now: Long) {
        // Now and then a few tiny glints drift around the orb, like it is made of light.
        if (now >= nextTwinkleAt) {
            twinkleStartedAt = now
            nextTwinkleAt = now + 7_000L + random.nextLong(9_000L)
            for (i in twinkleX.indices) {
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val radius = 430f + random.nextFloat() * 90f
                twinkleX[i] = ORB_CX + kotlin.math.cos(angle) * radius
                twinkleY[i] = ORB_CY + sin(angle) * radius
            }
        }
        val age = now - twinkleStartedAt
        if (twinkleStartedAt == 0L || age !in 0L..1_100L) return
        for (i in twinkleX.indices) {
            val p = ((age - i * 180L) / 700f)
            if (p <= 0f || p >= 1f) continue
            drawStar(canvas, twinkleX[i], twinkleY[i] - p * 24f, 20f * sin(p * PI.toFloat()), 0xFFFFF4C2.toInt(), (sin(p * PI.toFloat()) * 230f).toInt())
        }
        detailPaint.alpha = 255
    }

    private fun drawStar(canvas: Canvas, x: Float, y: Float, r: Float, color: Int, alpha: Int) {
        detailPaint.color = color
        detailPaint.alpha = alpha.coerceIn(0, 255)
        path.rewind()
        path.moveTo(x, y - r)
        path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r)
        path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        path.close()
        canvas.drawPath(path, detailPaint)
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
        // Eye whites keep their natural colour in every skin.
        val layerPaint = if (name.startsWith("eye_")) eyePaint else paint
        if (dx == 0f && dy == 0f && rotation == 0f) {
            canvas.drawBitmap(bitmap, null, artRect, layerPaint)
            return
        }
        canvas.save()
        canvas.translate(dx, dy)
        if (rotation != 0f) canvas.rotate(rotation, pivotX, pivotY)
        canvas.drawBitmap(bitmap, null, artRect, layerPaint)
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
        const val SLEEP_AFTER = 50_000L
        val BOW_SIDES = floatArrayOf(-1f, 1f)

        /** Pink recolour: mostly a warm pink tint, keeping a quarter of the original iridescence. */
        val PINK_MATRIX = floatArrayOf(
            .57625f, .641625f, .119625f, 0f, 15f,
            .12375f, .493375f, .045375f, 0f, 0f,
            .21375f, .420375f, .328375f, 0f, 18.75f,
            0f, 0f, 0f, 1f, 0f,
        )
        const val TEAR_CYCLE = 1_700L
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
    actionKey: Long = 0L,
    action: WordSiegeMascotAction? = null,
    flying: Boolean = false,
    flightDirection: Float = 0f,
    speaking: Boolean = false,
    watching: Boolean = false,
    glanceKey: Int = 0,
    glanceX: Float = 0f,
    glanceY: Float = 0f,
    hat: WordSiegeMascotHat = WordSiegeMascotHat.NONE,
    skin: WordSiegeMascotSkin = WordSiegeMascotSkin.ORB,
    onLongPress: (() -> Unit)? = null,
    onTap: () -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> WordSiegeMascotView(context).apply { isClickable = true } },
        update = {
            it.updateContext(urgency, momentum, idleGazeX, idleGazeY, typingKey)
            it.updateCompanion(actionKey, action, flying, flightDirection, speaking, watching, glanceKey, glanceX, glanceY, hat)
            it.setSkin(skin)
            if (onLongPress != null) {
                it.setOnLongClickListener {
                    onLongPress()
                    true
                }
            } else {
                it.setOnLongClickListener(null)
            }
            it.updateGame(moveId, lastMoveMine, moveScore, capturedCells, opponentCaptured, moveCell, pendingCells, playerTurn, requestedEmotion)
            it.setOnClickListener { view ->
                (view as WordSiegeMascotView).reactToTap()
                onTap()
            }
        },
    )
}
