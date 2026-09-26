package com.sonharf.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
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
import kotlin.math.cos
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

/** Whole-body moves; existing names remain compatible with the companion brain. */
internal enum class WordSiegeMascotAction {
    HOP, DANCE, STRETCH, LOOK_AROUND, NOD, SPARKLE, CHEER, LAND, SHRUG, PEEK, FLINCH, YAWN,
    /** Waves hello with the right hand. */
    WAVE,
    /** Hand to chin, eyes up: working something out (used before a hint). */
    THINK,
    /** Leans in and points: "look here" (used when giving a hint). */
    POINT,
    /** Claps along with a small bounce. */
    CLAP,
    NUZZLE, SWAY, GROOM, FOOD_LOOK, EAT, DOZE,
}

/** One clock shared by the rig and room scenes; never cut a move short with a second duration table. */
internal fun mascotActionMillis(action: WordSiegeMascotAction): Long = when (action) {
    WordSiegeMascotAction.HOP -> 1_800L
    WordSiegeMascotAction.DANCE -> 3_600L
    WordSiegeMascotAction.STRETCH -> 4_200L
    WordSiegeMascotAction.LOOK_AROUND -> 4_600L
    WordSiegeMascotAction.NOD -> 2_000L
    WordSiegeMascotAction.SPARKLE -> 2_400L
    WordSiegeMascotAction.CHEER -> 2_800L
    WordSiegeMascotAction.LAND -> 900L
    WordSiegeMascotAction.SHRUG -> 2_200L
    WordSiegeMascotAction.PEEK -> 3_600L
    WordSiegeMascotAction.FLINCH -> 800L
    WordSiegeMascotAction.YAWN -> 4_800L
    WordSiegeMascotAction.WAVE -> 3_200L
    WordSiegeMascotAction.THINK -> 4_200L
    WordSiegeMascotAction.POINT -> 3_200L
    WordSiegeMascotAction.CLAP -> 2_800L
    WordSiegeMascotAction.NUZZLE -> 3_200L
    WordSiegeMascotAction.SWAY -> 4_800L
    WordSiegeMascotAction.GROOM -> 3_600L
    WordSiegeMascotAction.FOOD_LOOK -> 2_000L
    WordSiegeMascotAction.EAT -> 4_400L
    WordSiegeMascotAction.DOZE -> 5_600L
}

/** Four complete chew cycles between opening the mouth and swallowing. */
internal fun mascotEatingOpen(t: Float): Float = when {
    t < .16f -> .08f + .8f * WordSiegeMascotView.easeInOut(t / .16f)
    t < .28f -> .1f + .78f * (1f - WordSiegeMascotView.easeInOut((t - .16f) / .12f))
    t < .80f -> .10f + .42f * (.5f - .5f * cos((t - .28f) / .52f * 8f * PI.toFloat()))
    else -> .1f * (1f - WordSiegeMascotView.easeInOut((t - .80f) / .20f))
}

internal fun stepMascotSprings(target: FloatArray, value: FloatArray, velocity: FloatArray, dt: Float, k: Float, c: Float) {
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


/** Continuous jump curve; the caller owns the reusable five-channel buffer. */
internal fun mascotJumpPose(t: Float, height: Float, crouch: Float, tilt: Float, motion: FloatArray) {
    motion.fill(0f)
    motion[2] = 1f; motion[3] = 1f
    val takeOff = .22f
    val land = .66f
    val settle = .82f
    when {
        t < takeOff -> {
            val e = WordSiegeMascotView.easeInOut(t / takeOff)
            motion[1] = 20f * e
            motion[2] = 1f + crouch * .75f * e
            motion[3] = 1f - crouch * e
        }
        t < land -> {
            val u = (t - takeOff) / (land - takeOff)
            val arc = 4f * u * (1f - u)
            val stretch = abs(1f - 2f * u).pow(1.5f)
            val release = WordSiegeMascotView.easeInOut(min(1f, u / .16f))
            motion[1] = -height * arc + 20f * (1f - min(1f, u * 5f))
            motion[2] = 1f - .07f * stretch
            val landingEase = WordSiegeMascotView.easeInOut((u - .85f) / .15f)
            val flightStretch = 1f + .11f * stretch * (1f - landingEase)
            motion[3] = (1f - crouch) * (1f - release) + flightStretch * release
            // A lateral airborne arc, never a spin.
            motion[0] = 9f * sin(u * PI.toFloat())
            motion[4] = (tilt + 2f) * sin(u * 2f * PI.toFloat())
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
    motion[2] = 1f / motion[3]
}

/** Costume worn over the orb; purely cosmetic. */
internal enum class WordSiegeMascotHat { NONE, PARTY, CROWN }


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
    private val skinCache = HashMap<WordSiegeMascotSkin, Map<String, Bitmap>>()

    /** Layers recoloured for [skin]; eye whites always keep the original art. Baked once per skin. */
    fun layers(context: Context, skin: WordSiegeMascotSkin): Map<String, Bitmap> {
        val base = layers(context)
        val palette = WordSiegeMascotPalette.forSkin(skin) ?: return base
        return synchronized(skinCache) {
            skinCache.getOrPut(skin) {
                base.mapValues { (name, bitmap) -> if (name.startsWith("eye_")) bitmap else palette.recolor(bitmap) }
            }
        }
    }

    fun layers(context: Context): Map<String, Bitmap> = cache ?: synchronized(this) {
        cache ?: names.associateWith { name ->
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.applicationContext.assets.open("word_siege_mascot/$name.webp").use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: error("Missing mascot layer: $name")
            (if (name == "orb_face_base") feathered(decoded) else decoded).apply { setHasMipMap(true) }
        }.also { cache = it }
    }

    /**
     * Softens the orb's hard outer outline: the artwork fades out over a short band just outside
     * the body instead of ending on a dark drawn line.
     */
    private fun feathered(source: Bitmap): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val k = out.width / WordSiegeMascotView.ART
        val cx = WordSiegeMascotView.ORB_CX * k
        val cy = WordSiegeMascotView.ORB_CY * k
        val r = WordSiegeMascotView.ORB_RY * k * 1.07f
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r, intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0x00000000),
                floatArrayOf(0f, .9f, 1f), Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), mask)
        return out
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
    private var layers: Map<String, Bitmap> = WordSiegeMascotArt.layers(context)
    private val artScale = ART / (layers.getValue("orb_face_base").width.toFloat())
    private val artRect = RectF(0f, 0f, ART, ART)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val atopPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    // Eyelids are painted with the orb's own face texture so they read as the same surface.
    private var skinShader = BitmapShader(layers.getValue("orb_face_base"), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        shader = skinShader
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private val featureMatrix = Matrix()
    private val skinMatrix = Matrix()
    // A soft glowing rim instead of a hard outline.
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        maskFilter = BlurMaskFilter(9f, BlurMaskFilter.Blur.NORMAL)
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
    // Skin: recoloured artwork plus the character's vector accessories.
    private var skin = WordSiegeMascotSkin.ORB
    private var decor = WordSiegeMascotDecor.of(WordSiegeMascotSkin.ORB)
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Two small round hands float beside the body (never touching it) and follow the mood.
    private val hand = FloatArray(4).also { it[0] = ORB_CX - HAND_OUT; it[1] = HAND_Y; it[2] = ORB_CX + HAND_OUT; it[3] = HAND_Y }
    private val handVelocity = FloatArray(4)
    private val handTarget = FloatArray(4)
    private val handPath = Path()
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { setShadowLayer(14f, 0f, 8f, 0x400C061E) }
    private val handLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = 0x80140A2A.toInt()
        maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
    }

    // ---- 3D lighting: a soft key light from the upper left, a floor shadow and a bounce rim ----
    private val floorShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(ORB_CX, ORB_BOTTOM + 14f, ORB_RX * .92f,
            intArrayOf(0x5A0A0F1C, 0x2A0A0F1C, 0x000A0F1C), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
    }
    private val bodyShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(ORB_CX - ORB_RX * .30f, ORB_CY - ORB_RY * .36f, ORB_RX * 1.42f,
            intArrayOf(0x00000000, 0x00000000, 0x2A060414, 0x5C060414), floatArrayOf(0f, .48f, .82f, 1f), Shader.TileMode.CLAMP)
    }
    private val keyLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, ORB_RX * .55f,
            intArrayOf(0x4DFFFFFF, 0x1AFFFFFF, 0x00FFFFFF), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
    }
    private val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 44f, intArrayOf(0xE6FFFFFF.toInt(), 0x00FFFFFF), null, Shader.TileMode.CLAMP)
    }
    private val rimLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(ORB_CX - ORB_RX * .08f, ORB_CY - ORB_RY * .1f, ORB_RX * 1.04f,
            intArrayOf(0x00FFFFFF, 0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF), floatArrayOf(0f, .84f, .95f, 1f), Shader.TileMode.CLAMP)
    }
    private val lightMatrix = Matrix()
    private val handCoverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val padPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF9FC0.toInt() }
    private val girlLashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
        color = 0xFF1A0A2E.toInt()
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

    // Persistent body springs blend from the actually displayed pose, including its momentum.
    private val bodyTarget = floatArrayOf(0f, 0f, 1f, 1f, 0f)
    private val bodyValue = bodyTarget.copyOf()
    private val bodyVelocity = FloatArray(5)
    private val followTarget = bodyTarget.copyOf()
    private val followValue = bodyTarget.copyOf()
    private val followVelocity = FloatArray(5)
    private val tuftTarget = bodyTarget.copyOf()
    private val tuftValue = bodyTarget.copyOf()
    private val tuftVelocity = FloatArray(5)
    private val historyTimes = LongArray(64)
    private val historyPoses = FloatArray(64 * 5)
    private var historyWrite = 0
    private var historyCount = 0
    private val bodyMatrix = Matrix()
    private val bodyInverse = Matrix()
    private val followMatrix = Matrix()
    private val mouthTarget = floatArrayOf(.55f, .45f, 1f)
    private val mouthValue = mouthTarget.copyOf()
    private var fixationX = 0f
    private var fixationY = 0f
    private var fixationAt = 0L
    private var nextMicroAt = 0L
    private var microX = 0f
    private var microY = 0f
    private var lastFrame = 0L
    private var breathPhase = 0f
    private var gazeX = 0f
    private var gazeY = .2f
    private var gazeVelX = 0f
    private var gazeVelY = 0f
    // The head follows the eyes slowly, like a real head: eyes dart, the head turns calmly.
    private var headX = 0f
    private var headY = .2f
    private var headVelX = 0f
    private var headVelY = 0f

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
        updateHandShader()
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

    private fun perform(action: WordSiegeMascotAction, now: Long, markActivity: Boolean = true) {
        // Moves play long and smooth, like animation, not short twitches.
        val duration = mascotActionMillis(action)
        if (markActivity) markActive(now)
        // An explicit scene owns the body; keep the current spring position/velocity when interrupted.
        motionUntil = now
        actionKind = action
        actionStartedAt = now
        actionUntil = now + duration
        // The face follows the move unless a stronger game reaction is already showing.
        val face = when (action) {
            WordSiegeMascotAction.HOP, WordSiegeMascotAction.DANCE -> WordSiegeMascotEmotion.HAPPY
            WordSiegeMascotAction.STRETCH, WordSiegeMascotAction.WAVE -> WordSiegeMascotEmotion.HAPPY
            WordSiegeMascotAction.NUZZLE, WordSiegeMascotAction.GROOM, WordSiegeMascotAction.SWAY -> WordSiegeMascotEmotion.HAPPY
            WordSiegeMascotAction.EAT -> WordSiegeMascotEmotion.HAPPY
            WordSiegeMascotAction.DOZE, WordSiegeMascotAction.YAWN -> WordSiegeMascotEmotion.CALM
            WordSiegeMascotAction.THINK, WordSiegeMascotAction.FOOD_LOOK -> WordSiegeMascotEmotion.FOCUS
            WordSiegeMascotAction.POINT -> WordSiegeMascotEmotion.PROUD
            WordSiegeMascotAction.CLAP -> WordSiegeMascotEmotion.LAUGH
            WordSiegeMascotAction.SPARKLE -> WordSiegeMascotEmotion.PROUD
            WordSiegeMascotAction.CHEER -> WordSiegeMascotEmotion.EXCITED
            WordSiegeMascotAction.FLINCH -> WordSiegeMascotEmotion.SURPRISED
            else -> null
        }
        if (action == WordSiegeMascotAction.NUZZLE) heartsStartedAt = now + 1_000L
        if (action == WordSiegeMascotAction.FLINCH && blinkStartedAt < 0L) nextBlinkAt = now
        if (face != null && now >= reactionUntil) {
            reaction = face
            reactionStartedAt = now
            reactionUntil = now + duration
        }
        if (action == WordSiegeMascotAction.DANCE || action == WordSiegeMascotAction.SPARKLE ||
            action == WordSiegeMascotAction.CHEER || action == WordSiegeMascotAction.STRETCH
        ) sparklesStartedAt = now
        invalidate()
    }

    fun setSkin(next: WordSiegeMascotSkin) {
        if (next == skin) return
        skin = next
        layers = WordSiegeMascotArt.layers(context, next)
        skinShader = BitmapShader(layers.getValue("orb_face_base"), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        skinPaint.shader = skinShader
        decor = WordSiegeMascotDecor.of(next)
        updateHandShader()
        edgePaint.shader = decor.ringShader()
        wingPaint.shader = decor.wingShader()
        invalidate()
    }

    private fun updateHandShader() {
        handPaint.shader = RadialGradient(-18f, -23f, 74f, decor.handColors, null, Shader.TileMode.CLAMP)
        handCoverPaint.shader = handPaint.shader
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

    private var jellyAt = 0L

    fun reactToTap() {
        val now = SystemClock.uptimeMillis()
        jellyAt = now
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
        historyCount = 0
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
        sleeping = (restful && idleMillis > SLEEP_AFTER) ||
            (actionKind == WordSiegeMascotAction.DOZE && now < actionUntil)
        if (restful && !sleeping && idleMillis > DROWSY_AFTER + 4_000L && now >= nextYawnAt && now >= actionUntil) {
            perform(WordSiegeMascotAction.YAWN, now, markActivity = false)
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
        val faceT = if (actionKind != null && now < actionUntil) {
            ((now - actionStartedAt).toFloat() / (actionUntil - actionStartedAt)).coerceIn(0f, 1f)
        } else -1f
        val envelope = if (faceT >= 0f) actionEnvelope(faceT) else 0f
        if (faceT >= 0f) when (actionKind) {
            WordSiegeMascotAction.NUZZLE, WordSiegeMascotAction.GROOM -> {
                pose[P_LID] = .98f * envelope
                pose[P_SMILE] = .95f
                pose[P_OPEN] = .12f
                pose[P_CHEEK] = .95f
            }
            WordSiegeMascotAction.EAT -> {
                pose[P_OPEN] = mascotEatingOpen(faceT)
                pose[P_WIDTH] = .72f
                pose[P_LID] = .16f + .36f * envelope
                pose[P_SMILE] = .65f
            }
            else -> Unit
        }
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
            (actionKind == WordSiegeMascotAction.FOOD_LOOK || actionKind == WordSiegeMascotAction.EAT) && now < actionUntil -> {
                targetX = .35f; targetY = .65f
            }
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
        // Thinking looks up and aside; pointing looks where the hand points.
        if (now < actionUntil) when (actionKind) {
            WordSiegeMascotAction.THINK -> { targetX = .45f; targetY = -.6f }
            WordSiegeMascotAction.POINT -> { targetX = .8f; targetY = .1f }
            WordSiegeMascotAction.WAVE -> { targetX = 0f; targetY = .05f }
            else -> Unit
        }
        targetY = max(targetY, bow * .95f)
        // Fixate first; only then add tiny, infrequent saccades around that exact target.
        if (abs(targetX - fixationX) + abs(targetY - fixationY) > .08f) {
            fixationX = targetX; fixationY = targetY
            fixationAt = now; nextMicroAt = now + 650L
            microX = 0f; microY = 0f
        } else if (now >= nextMicroAt && now - fixationAt >= 650L) {
            microX = (random.nextFloat() - .5f) * .035f
            microY = (random.nextFloat() - .5f) * .025f
            nextMicroAt = now + 700L + random.nextLong(900L)
        }
        targetX += microX; targetY += microY
        val gazeK = 520f
        val gazeC = 2f * sqrt(gazeK) * .9f
        var gazeRemaining = dt
        while (gazeRemaining > 0f) {
            val h = min(gazeRemaining, 1f / 120f)
            gazeVelX += (gazeK * (targetX - gazeX) - gazeC * gazeVelX) * h
            gazeVelY += (gazeK * (targetY - gazeY) - gazeC * gazeVelY) * h
            gazeX += gazeVelX * h; gazeY += gazeVelY * h
            gazeRemaining -= h
        }
        val tremor = 0f // Stable fixation, with discrete micro-saccades above.
        val headK = 16f
        val headC = 2f * sqrt(headK)
        headVelX += (headK * (gazeX - headX) - headC * headVelX) * dt
        headVelY += (headK * (gazeY - headY) - headC * headVelY) * dt
        headX += headVelX * dt
        headY += headVelY * dt

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
        var rotation = poseValue[P_TILT] + headX * (if (lookAround) 3.5f else 2f) + sin(now / 2_300f) * sway

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
            dx += m[0]; dy += m[1]; sx *= m[2]; sy *= m[3]; rotation += m[4]
        }
        val activeAction = actionKind
        if (activeAction != null && now < actionUntil) {
            val actionT = ((now - actionStartedAt).toFloat() / (actionUntil - actionStartedAt)).coerceIn(0f, 1f)
            val m = actionMotion(activeAction, actionT)
            dx += m[0]; dy += m[1]; sx *= m[2]; sy *= m[3]; rotation += m[4]
        }
        if (jellyAt != 0L && now - jellyAt < 900L) {
            // Jelly squish on a tap: a damped, volume-keeping wobble like a soft toy.
            val t = (now - jellyAt) / 1_000f
            val w = .16f * kotlin.math.exp(-5.5f * t) * cos(t * 26f)
            sx *= 1f + w
            sy /= 1f + w
        }
        if (mood == WordSiegeMascotEmotion.STRESSED || urgency > .3f) {
            // A slight nervous shiver, not a head shake.
            dy += sin(now / 60f) * .8f * max(urgency, .4f)
        }

        bodyTarget[0] = dx; bodyTarget[1] = dy
        // Preserve the apparent volume of the orb (cross-sectional area in the 2D rig).
        // The deliberate flight shrink remains a uniform change of viewing distance.
        val flightScale = 1f - .16f * min(1f, wing)
        bodyTarget[3] = sy.coerceIn(.72f, 1.28f)
        bodyTarget[2] = flightScale * flightScale / bodyTarget[3]
        bodyTarget[4] = rotation.coerceIn(-8f, 8f)
        stepSprings(bodyTarget, bodyValue, bodyVelocity, dt, 260f, 27f)
        dx = bodyValue[0]; dy = bodyValue[1]
        sy = bodyValue[3].coerceIn(.72f, 1.28f)
        sx = flightScale * flightScale / sy
        bodyValue[2] = sx
        rotation = bodyValue[4].coerceIn(-8f, 8f)
        bodyValue[4] = rotation
        recordBodyPose(now)
        delayedBodyPose(now - 90L, followTarget)
        delayedBodyPose(now - 120L, tuftTarget)
        stepSprings(followTarget, followValue, followVelocity, dt, 380f, 30f)
        stepSprings(tuftTarget, tuftValue, tuftVelocity, dt, 320f, 25f)
        setBodyMatrix(bodyMatrix, bodyValue)
        bodyMatrix.invert(bodyInverse)

        // ---- Draw -------------------------------------------------------------------------------
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(size / ART, size / ART)
        // Floor shadow: stays on the ground and shrinks and fades as the body lifts off.
        val lift = ((-dy).coerceAtLeast(0f) / 150f).coerceIn(0f, 1f)
        if (wing < .5f) {
            canvas.save()
            canvas.translate(dx * .8f, 0f)
            canvas.scale(sx * (1f - .35f * lift), .2f * (1f - .25f * lift), ORB_CX, ORB_BOTTOM + 14f)
            floorShadowPaint.alpha = (255 * (1f - .6f * lift)).toInt()
            canvas.drawCircle(ORB_CX, ORB_BOTTOM + 14f, ORB_RX * .92f, floorShadowPaint)
            canvas.restore()
        }
        canvas.concat(bodyMatrix)

        if (wing > .01f) drawWings(canvas, now)
        decor.drawBehind(canvas)
        drawLayer(canvas, "orb_face_base")
        val sparkling = actionKind == WordSiegeMascotAction.SPARKLE && now < actionUntil
        val glow = if (mood == WordSiegeMascotEmotion.PROUD || mood == WordSiegeMascotEmotion.EXCITED || sparkling) 45f else 0f
        ovalRect.set(ORB_CX - ORB_RX, ORB_CY - ORB_RY, ORB_CX + ORB_RX, ORB_CY + ORB_RY)
        // Sphere shading: darker away from the light, a soft key light and a small glint that
        // slide opposite to where the head turns, and a thin bounce light on the lower rim.
        canvas.drawOval(ovalRect, bodyShadePaint)
        val lightX = ORB_CX - ORB_RX * .36f - headX * 26f
        val lightY = ORB_CY - ORB_RY * .44f - headY * 14f
        lightMatrix.setTranslate(lightX, lightY)
        keyLightPaint.shader.setLocalMatrix(lightMatrix)
        canvas.drawOval(ovalRect, keyLightPaint)
        lightMatrix.setScale(1.5f, .8f)
        lightMatrix.postTranslate(lightX - ORB_RX * .08f, lightY - ORB_RY * .1f)
        glintPaint.shader.setLocalMatrix(lightMatrix)
        canvas.drawCircle(lightX - ORB_RX * .08f, lightY - ORB_RY * .1f, 66f, glintPaint)
        canvas.drawOval(ovalRect, rimLightPaint)
        edgePaint.alpha = (120f + 20f * sin(now / 950f) + glow).toInt().coerceIn(0, 255)
        canvas.drawOval(ovalRect, edgePaint)

        // Face features ride slightly ahead of the orb: they follow the gaze and fold downward when
        // the head bows, which gives the round body a sense of depth.
        // Features shift gently with the (slow) head and only a little when bowing, so the eyes
        // stay in their sockets; the bow itself is carried by the body tilt and the gaze.
        featureMatrix.setTranslate(headX * 12f, headY * 6f + bow * 18f)
        featureMatrix.preScale(1f, 1f - bow * .05f, FACE_CX, FACE_CY)
        // The lid texture is counter-transformed so it always lines up with the orb underneath,
        // otherwise the moving features would reveal seams at the lid edges.
        if (featureMatrix.invert(skinMatrix)) {
            skinMatrix.preScale(artScale, artScale)
            skinShader.setLocalMatrix(skinMatrix)
        }
        canvas.save()
        canvas.concat(featureMatrix)

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
        drawAnimeEyes(canvas, now, mood, lid, gazeX + tremor, gazeY)

        val browY = poseValue[P_BROW_Y] - lid * 6f - yawn * 22f - shrug * 26f
        val browTilt = Math.toDegrees(poseValue[P_BROW_TILT].toDouble()).toFloat()
        drawLayer(canvas, "brow_left", 0f, browY, LEFT_BROW_X, BROW_Y, -browTilt)
        drawLayer(canvas, "brow_right", 0f, browY, RIGHT_BROW_X, BROW_Y, browTilt)

        val cheek = poseValue[P_CHEEK].coerceIn(0f, 1f)
        for (index in 0 until 2) {
            val cheekPaint = cheekPaints[index]
            val blush = if (skin == WordSiegeMascotSkin.PINK) 40f else 18f
            cheekPaint.alpha = (blush + cheek * 92f).toInt().coerceIn(0, 255)
            canvas.drawCircle(if (index == 0) LEFT_CHEEK_X else RIGHT_CHEEK_X, CHEEK_Y, 96f, cheekPaint)
        }
        drawLayer(canvas, "cheek_left", 0f, -lower * 10f)
        drawLayer(canvas, "cheek_right", 0f, -lower * 10f)
        drawBlushLines(canvas, mood, cheek)

        // Talking: irregular syllables layered on the current expression.
        val mouthOpen = if (speaking && actionKind != WordSiegeMascotAction.EAT) {
            max(.14f, .16f + .46f * abs(sin(now / 88f) * sin(now / 231f + 1.3f)))
        } else {
            pose[P_OPEN].coerceIn(0f, 1f)
        }
        val mouthWidth = if (speaking) min(pose[P_WIDTH], 1.08f) else pose[P_WIDTH]
        mouthTarget[0] = pose[P_SMILE] * (1f - yawn) - .1f * yawn + (.1f - pose[P_SMILE]) * shrug * .7f
        mouthTarget[1] = max(mouthOpen, yawn)
        mouthTarget[2] = mouthWidth + (.62f - mouthWidth) * yawn
        // Raw mouth targets avoid stacking two filters: ~95% response in 200 ms, including speech/yawns.
        val mouthBlend = 1f - kotlin.math.exp(-dt * 15f)
        for (i in 0 until 3) mouthValue[i] += (mouthTarget[i] - mouthValue[i]) * mouthBlend
        drawMouth(canvas, mouthValue[0], mouthValue[1], mouthValue[2])
        decor.drawFace(canvas, mouthTopY(mouthValue[0], mouthValue[1]), mouthValue[1])
        if (mood == WordSiegeMascotEmotion.TEARY) drawTears(canvas, now)
        canvas.restore()

        if (mood == WordSiegeMascotEmotion.STRESSED || urgency > .45f) drawSweat(canvas, now)
        canvas.save()
        canvas.concat(bodyInverse)
        setBodyMatrix(followMatrix, followValue)
        canvas.concat(followMatrix)
        drawHands(canvas, now, mood, dt)
        canvas.restore()
        canvas.save()
        canvas.concat(bodyInverse)
        setBodyMatrix(followMatrix, tuftValue)
        canvas.concat(followMatrix)
        if (hat != WordSiegeMascotHat.NONE) drawHat(canvas, now) else decor.drawTuft(canvas, now)
        canvas.restore()
        decor.drawFront(canvas)
        if (sleeping) drawSleepZ(canvas, now)
        drawAnimeMarks(canvas, now, mood)
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

    private fun stepSprings(target: FloatArray, value: FloatArray, velocity: FloatArray, dt: Float, k: Float, c: Float) =
        stepMascotSprings(target, value, velocity, dt, k, c)

    private fun actionEnvelope(t: Float): Float = easeInOut(t / .14f) * (1f - easeInOut((t - .84f) / .16f))

    private fun recordBodyPose(now: Long) {
        historyTimes[historyWrite] = now
        bodyValue.copyInto(historyPoses, historyWrite * 5)
        historyWrite = (historyWrite + 1) % historyTimes.size
        historyCount = min(historyCount + 1, historyTimes.size)
    }

    private fun delayedBodyPose(at: Long, out: FloatArray) {
        val oldest = (historyWrite - historyCount + historyTimes.size) % historyTimes.size
        var index = oldest
        for (offset in 1 until historyCount) {
            val next = (oldest + offset) % historyTimes.size
            if (historyTimes[next] > at) {
                val span = (historyTimes[next] - historyTimes[index]).coerceAtLeast(1L)
                val t = ((at - historyTimes[index]).toFloat() / span).coerceIn(0f, 1f)
                for (i in 0 until 5) out[i] = historyPoses[index * 5 + i] + (historyPoses[next * 5 + i] - historyPoses[index * 5 + i]) * t
                return
            }
            index = next
        }
        for (i in 0 until 5) out[i] = historyPoses[index * 5 + i]
    }

    private fun setBodyMatrix(out: Matrix, values: FloatArray) {
        out.setTranslate(values[0], values[1])
        out.preRotate(values[4].coerceIn(-8f, 8f), ORB_CX, ORB_BOTTOM)
        out.preScale(values[2], values[3], ORB_CX, ORB_BOTTOM)
    }

    private val motion = FloatArray(5)

    private fun resetMotion() {
        motion[0] = 0f; motion[1] = 0f; motion[2] = 1f; motion[3] = 1f; motion[4] = 0f
    }

    /** Returns dx, dy, scaleX, scaleY, tilt for a one-shot reaction at normalized time [t]. */
    private fun bodyMotion(emotion: WordSiegeMascotEmotion, t: Float): FloatArray {
        resetMotion()
        when (emotion) {
            WordSiegeMascotEmotion.JUMP -> jump(t, height = 105f, crouch = .13f, tilt = 3f)
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.EXCITED -> {
                jump(t, height = 56f, crouch = .08f, tilt = 0f)
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
            WordSiegeMascotAction.HOP -> jump(t, height = 58f, crouch = .09f, tilt = 0f)
            WordSiegeMascotAction.CHEER -> {
                jump(t, height = 88f, crouch = .12f, tilt = 0f)
                motion[4] += 3f * sin(t * 2f * pi) * (1f - t)
            }
            WordSiegeMascotAction.DANCE -> {
                // A happy side-step dance: weight shifts left and right with a small hop on each
                // step and a squash on every landing. The body never spins.
                val beat = sin(t * 4f * pi)
                val hop = abs(beat)
                val fade = 1f - easeIn(max(0f, t - .8f) / .2f)
                motion[0] = 24f * beat * fade
                motion[1] = -16f * hop * fade
                motion[4] = 6f * beat * fade
                motion[2] = 1f + .035f * (1f - hop) * fade
                motion[3] = 1f - .045f * (1f - hop) * fade
            }
            WordSiegeMascotAction.STRETCH -> {
                // Rises onto tiptoe with the arms up, holds with a small quiver, then lets go
                // with a soft squash and settles.
                when {
                    t < .35f -> {
                        val e = easeInOut(t / .35f)
                        motion[1] = -22f * e
                        motion[2] = 1f - .05f * e
                        motion[3] = 1f + .12f * e
                    }
                    t < .7f -> {
                        val u = (t - .35f) / .35f
                        motion[1] = -22f + 1.5f * sin(u * 8f * pi)
                        motion[2] = .95f
                        motion[3] = 1.12f + .008f * sin(u * 8f * pi)
                    }
                    else -> {
                        val u = (t - .7f) / .3f
                        val drop = easeIn(min(1f, u * 1.6f))
                        val squash = sin(min(1f, u * 1.4f) * pi) * (1f - u)
                        motion[1] = -22f * (1f - drop) + 10f * squash
                        motion[2] = .95f + .05f * drop + .06f * squash
                        motion[3] = 1.12f - .12f * drop - .08f * squash
                    }
                }
            }
            WordSiegeMascotAction.WAVE -> {
                // A friendly lean towards the waving hand with a gentle bob.
                val lean = sin(min(1f, t / .2f) * pi / 2f) * (1f - easeIn(max(0f, t - .8f) / .2f))
                motion[4] = 5f * lean
                motion[1] = -8f * lean + 3f * sin(t * 6f * pi) * lean
            }
            WordSiegeMascotAction.THINK -> {
                // Slow weight shift, a small tilt of the head and a thoughtful rise.
                val hold = sin(min(1f, t / .3f) * pi / 2f) * (1f - easeIn(max(0f, t - .82f) / .18f))
                motion[4] = -7f * hold + 1.2f * sin(t * 3f * pi) * hold
                motion[1] = -6f * hold
                motion[0] = -6f * hold
            }
            WordSiegeMascotAction.POINT -> {
                // Anticipation back, then a decisive lean and step towards the hint.
                val back = if (t < .18f) sin(t / .18f * pi) else 0f
                val lean = if (t < .18f) 0f else sin(min(1f, (t - .18f) / .25f) * pi / 2f) * (1f - easeIn(max(0f, t - .8f) / .2f))
                motion[0] = -8f * back + 18f * lean
                motion[4] = -3f * back + 9f * lean
                motion[1] = -10f * lean
                motion[3] = 1f + .03f * lean
            }
            WordSiegeMascotAction.CLAP -> {
                val clap = abs(sin(t * 5f * pi)) * (1f - t * .4f)
                motion[1] = -10f * clap
                motion[3] = 1f + .03f * clap
                motion[2] = 1f - .02f * clap
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
            WordSiegeMascotAction.NUZZLE -> {
                val melt = actionEnvelope(t)
                motion[1] = 18f * melt
                motion[3] = 1f - .11f * melt
                motion[4] = -5f * melt + 1.5f * sin(t * 2f * pi) * melt
            }
            WordSiegeMascotAction.GROOM -> {
                val pleasure = actionEnvelope(t)
                motion[1] = -4f * pleasure + 1.6f * sin(t * 18f * pi) * pleasure
                motion[3] = 1f - .035f * pleasure
                motion[4] = 3f * pleasure
            }
            WordSiegeMascotAction.SWAY -> {
                val wave = sin(t * 2f * pi) * actionEnvelope(t)
                motion[0] = 12f * wave; motion[4] = 4f * wave
                motion[1] = -3f * actionEnvelope(t)
            }
            WordSiegeMascotAction.EAT -> {
                val chew = if (t >= .28f && t <= .80f) sin((t - .28f) / .52f * 8f * pi) else 0f
                val swallow = if (t > .80f) sin((t - .80f) / .20f * pi) else 0f
                motion[1] = 2.5f * chew + 9f * swallow
                motion[3] = 1f - .016f * chew - .035f * swallow
                motion[4] = 2f * actionEnvelope(t)
            }
            WordSiegeMascotAction.FOOD_LOOK -> {
                val lean = actionEnvelope(t)
                motion[0] = 5f * lean; motion[4] = 3f * lean
            }
            WordSiegeMascotAction.DOZE -> {
                val nod = actionEnvelope(t)
                motion[1] = 14f * nod; motion[3] = 1f - .025f * nod
                motion[4] = 4f * nod
            }
            WordSiegeMascotAction.LOOK_AROUND -> {
                // Eyes acquire each target before the body follows it.
                motion[4] = headX * 2f * actionEnvelope(t)
                motion[1] = -3f * actionEnvelope(t)
            }
        }
        val blend = actionEnvelope(t)
        motion[0] *= blend; motion[1] *= blend; motion[4] *= blend
        motion[3] = 1f + (motion[3] - 1f) * blend
        motion[2] = 1f / motion[3]
        return motion
    }

    /** Anticipation crouch -> stretched take-off -> airborne arc -> squash landing -> damped wobble. */
    private fun jump(t: Float, height: Float, crouch: Float, tilt: Float) =
        mascotJumpPose(t, height, crouch, tilt, motion)

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

    /** Where the upper lip sits at the centre, for details that hang from it (fangs). */
    private fun mouthTopY(smile: Float, open: Float): Float {
        val corner = MOUTH_Y - smile * 30f
        val top = MOUTH_Y - smile * 8f - open * 26f + max(0f, -smile) * -18f
        return .25f * corner + .75f * top
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

    // ---- Anime visual language -----------------------------------------------------------------

    private val animePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /**
     * Kira-kira eyes: a second catchlight always, four-point stars in the irises when thrilled,
     * and "^ ^" smiling crescents when it laughs with its eyes closed.
     */
    private fun drawAnimeEyes(canvas: Canvas, now: Long, mood: WordSiegeMascotEmotion, lid: Float, lookX: Float, lookY: Float) {
        val open = (1f - lid).coerceIn(0f, 1f)
        val dx = lookX.coerceIn(-1f, 1f) * 24f
        val dy = lookY.coerceIn(-1f, 1f) * 20f
        val thrilled = mood == WordSiegeMascotEmotion.EXCITED || mood == WordSiegeMascotEmotion.PROUD ||
            (actionKind == WordSiegeMascotAction.SPARKLE || actionKind == WordSiegeMascotAction.CHEER) && now < actionUntil
        for (i in 0 until 2) {
            val ix = if (i == 0) LEFT_IRIS_X else RIGHT_IRIS_X
            val side = if (i == 0) -1f else 1f
            if (open > .25f) {
                // Small lower catchlight, the second highlight every anime eye has.
                detailPaint.color = 0xFFFFFFFF.toInt()
                detailPaint.alpha = (200f * open).toInt()
                canvas.drawCircle(ix + dx + 30f, IRIS_Y + dy + 42f, 13f, detailPaint)
            }
            if (thrilled && open > .35f) {
                val pulse = .8f + .2f * sin(now / 140f + side)
                drawStar(canvas, ix + dx - 6f, IRIS_Y + dy - 8f, 46f * pulse * open, 0xFFFFF4C2.toInt(), (240f * open).toInt())
            }
        }
        detailPaint.alpha = 255
        if (mood == WordSiegeMascotEmotion.LAUGH && lid > .7f) {
            animePaint.color = 0xFF2A1740.toInt()
            animePaint.strokeWidth = 16f
            animePaint.alpha = (255f * ((lid - .7f) / .3f).coerceIn(0f, 1f)).toInt()
            for (i in 0 until 2) {
                val cx = if (i == 0) LEFT_EYE_X else RIGHT_EYE_X
                path.rewind()
                path.moveTo(cx - 70f, EYE_Y + 20f)
                path.quadTo(cx, EYE_Y - 60f, cx + 70f, EYE_Y + 20f)
                canvas.drawPath(path, animePaint)
            }
            animePaint.alpha = 255
        }
    }

    /** "///" blush strokes over the cheeks when it is happy, proud or being petted. */
    private fun drawBlushLines(canvas: Canvas, mood: WordSiegeMascotEmotion, cheek: Float) {
        val shy = when (mood) {
            WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.PROUD, WordSiegeMascotEmotion.LAUGH -> .6f
            else -> 0f
        }.coerceAtLeast(cheek)
        if (shy < .35f) return
        animePaint.color = 0xFFE8436E.toInt()
        animePaint.strokeWidth = 7f
        animePaint.alpha = (200f * shy).toInt()
        for (side in 0 until 2) {
            val cx = if (side == 0) LEFT_CHEEK_X else RIGHT_CHEEK_X
            for (i in -1..1) {
                val x = cx + i * 26f
                canvas.drawLine(x - 8f, CHEEK_Y + 18f, x + 10f, CHEEK_Y - 18f, animePaint)
            }
        }
        animePaint.alpha = 255
    }

    /**
     * Manga marks around the head: a throbbing cross vein when angry, a big sliding sweat drop
     * when surprised or shrugging, and musical notes while dancing or clapping.
     */
    private fun drawAnimeMarks(canvas: Canvas, now: Long, mood: WordSiegeMascotEmotion) {
        if (mood == WordSiegeMascotEmotion.ANGRY) {
            val beat = 1f + .12f * abs(sin(now / 120f))
            val cx = ORB_CX + 250f
            val cy = ORB_CY - 330f
            animePaint.color = 0xFFE5304A.toInt()
            animePaint.strokeWidth = 15f
            val r = 34f * beat
            for (q in 0 until 4) {
                val sx = if (q % 2 == 0) -1f else 1f
                val sy = if (q < 2) -1f else 1f
                path.rewind()
                path.moveTo(cx + sx * r * .25f, cy + sy * r)
                path.quadTo(cx + sx * r * .25f, cy + sy * r * .25f, cx + sx * r, cy + sy * r * .25f)
                canvas.drawPath(path, animePaint)
            }
        }
        val action = actionKind.takeIf { now < actionUntil }
        if (mood == WordSiegeMascotEmotion.SURPRISED || action == WordSiegeMascotAction.SHRUG) {
            val t = (now % 1_600L) / 1_600f
            detailPaint.color = 0xFF9FE3FF.toInt()
            detailPaint.alpha = (235f * (1f - t * .6f)).toInt()
            drawDrop(canvas, ORB_CX + 330f, ORB_CY - 250f + easeIn(t) * 60f, 34f)
            detailPaint.color = 0xFFFFFFFF.toInt()
            detailPaint.alpha = 190
            canvas.drawCircle(ORB_CX + 322f, ORB_CY - 250f + easeIn(t) * 60f + 10f, 8f, detailPaint)
            detailPaint.alpha = 255
        }
        if (action == WordSiegeMascotAction.DANCE || action == WordSiegeMascotAction.CLAP) {
            val base = actionStartedAt
            for (i in 0 until 3) {
                val p = (((now - base) - i * 260L) % 1_200L) / 1_200f
                if (p < 0f) continue
                val x = ORB_CX + (if (i % 2 == 0) -360f else 360f) + sin(p * 6f) * 20f
                val y = ORB_CY - 200f - p * 220f
                animePaint.color = if (i == 1) 0xFFFF7EB6.toInt() else 0xFF7C6CFF.toInt()
                animePaint.alpha = (255f * sin(p * PI.toFloat())).toInt()
                animePaint.strokeWidth = 9f
                canvas.drawLine(x + 20f, y, x + 20f, y - 70f, animePaint)
                canvas.drawLine(x + 20f, y - 70f, x + 50f, y - 55f, animePaint)
                detailPaint.color = animePaint.color
                detailPaint.alpha = animePaint.alpha
                ovalRect.set(x - 12f, y - 14f, x + 24f, y + 12f)
                canvas.drawOval(ovalRect, detailPaint)
            }
            animePaint.alpha = 255
            detailPaint.alpha = 255
        }
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
        val colors = SPARKLE_COLORS
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

    /**
     * Hands: bob gently at rest, go up when happy (waving), hang low when sad, lift beside the
     * face when surprised, flutter in flight and gesture while talking. They stay clear of the body.
     */
    private fun drawHands(canvas: Canvas, now: Long, mood: WordSiegeMascotEmotion, dt: Float) {
        val cheering = actionKind == WordSiegeMascotAction.CHEER && now < actionUntil
        var out = HAND_OUT
        var y = HAND_Y
        var waveL = 0f
        var waveR = 0f
        when {
            flying -> {
                y = 650f
                waveL = sin(now / 90f) * 16f
                waveR = sin(now / 90f + 1f) * 16f
            }
            cheering || mood == WordSiegeMascotEmotion.HAPPY || mood == WordSiegeMascotEmotion.LAUGH ||
                mood == WordSiegeMascotEmotion.EXCITED || mood == WordSiegeMascotEmotion.JUMP -> {
                y = 470f
                out = 520f
                waveL = sin(now / 130f) * 18f
                waveR = sin(now / 130f + 1.6f) * 18f
            }
            mood == WordSiegeMascotEmotion.SURPRISED -> {
                y = 600f
                out = 545f
            }
            mood.isSorrow() || sleeping -> {
                y = 935f
                out = 500f
            }
            mood == WordSiegeMascotEmotion.STRESSED -> {
                y = 720f
                waveL = sin(now / 70f) * 3f
                waveR = sin(now / 70f + 2f) * 3f
            }
            mood == WordSiegeMascotEmotion.PROUD -> y = 780f
        }
        val bobSpeed = if (sleeping) 1_300f else 650f
        handTarget[0] = ORB_CX - out
        handTarget[1] = y + sin(now / bobSpeed) * 9f + waveL
        handTarget[2] = ORB_CX + out
        handTarget[3] = y + sin(now / bobSpeed + 1.3f) * 9f + waveR
        // While talking (and not cheering) the right hand gestures along.
        if (speaking && y > 600f) {
            handTarget[3] = 700f + sin(now / 180f) * 30f
            handTarget[2] = ORB_CX + out + sin(now / 260f) * 14f
        }
        // Hand choreography for the expressive moves.
        val act = actionKind?.takeIf { now < actionUntil }
        when (act) {
            WordSiegeMascotAction.WAVE -> {
                handTarget[2] = ORB_CX + 470f + sin(now / 85f) * 55f
                handTarget[3] = 360f + cos(now / 85f) * 14f
            }
            WordSiegeMascotAction.THINK -> {
                handTarget[2] = ORB_CX + 170f
                handTarget[3] = 935f + sin(now / 400f) * 6f
                handTarget[0] = ORB_CX - HAND_OUT
                handTarget[1] = 820f
            }
            WordSiegeMascotAction.POINT -> {
                handTarget[2] = ORB_CX + 610f
                handTarget[3] = 560f + sin(now / 160f) * 6f
            }
            WordSiegeMascotAction.CLAP -> {
                val open = abs(sin(now / 95f))
                handTarget[0] = ORB_CX - 110f - 150f * open
                handTarget[2] = ORB_CX + 110f + 150f * open
                handTarget[1] = 720f
                handTarget[3] = 720f
            }
            WordSiegeMascotAction.NUZZLE, WordSiegeMascotAction.GROOM -> {
                handTarget[0] = ORB_CX - 430f; handTarget[2] = ORB_CX + 430f
                handTarget[1] = 790f; handTarget[3] = 790f
            }
            WordSiegeMascotAction.EAT -> {
                handTarget[2] = ORB_CX + 190f; handTarget[3] = 890f
            }
            WordSiegeMascotAction.SWAY, WordSiegeMascotAction.DOZE -> {
                handTarget[1] = 890f; handTarget[3] = 890f
            }
            WordSiegeMascotAction.STRETCH -> {
                handTarget[0] = ORB_CX - 430f
                handTarget[2] = ORB_CX + 430f
                handTarget[1] = 250f
                handTarget[3] = 250f
            }
            else -> Unit
        }
        val k = 70f
        val c = 2f * sqrt(k) * .8f
        stepSprings(handTarget, hand, handVelocity, dt, k, c)
        drawHand(canvas, hand[0].coerceIn(70f, 1_184f), hand[1], -1f)
        drawHand(canvas, hand[2].coerceIn(70f, 1_184f), hand[3], 1f)
    }

    private fun drawHand(canvas: Canvas, x: Float, y: Float, side: Float) {
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(side, 1f)
        // A round mitten with a small thumb pointing up towards the body.
        handPath.rewind()
        handPath.addCircle(0f, 0f, 58f, Path.Direction.CW)
        handPath.addCircle(-42f, -35f, 24f, Path.Direction.CW)
        canvas.drawPath(handPath, handPaint)
        canvas.drawCircle(0f, 0f, 58f, handLinePaint)
        canvas.drawCircle(-42f, -35f, 24f, handLinePaint)
        // Hide the outline where the thumb meets the palm.
        canvas.drawCircle(-28f, -21f, 18f, handCoverPaint)
        if (decor.pawPads) {
            canvas.drawCircle(5f, 12f, 20f, padPaint)
            canvas.drawCircle(-16f, -18f, 9f, padPaint)
            canvas.drawCircle(9f, -25f, 9f, padPaint)
            canvas.drawCircle(30f, -9f, 9f, padPaint)
        }
        detailPaint.color = 0xCCFFFFFF.toInt()
        canvas.drawCircle(18f, -23f, 12f, detailPaint)
        detailPaint.alpha = 255
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
        val colors = HEART_COLORS
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

    companion object {
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
        const val HAND_OUT = 535f
        const val HAND_Y = 820f

        const val TEAR_CYCLE = 1_700L
        val SPARKLE_COLORS = intArrayOf(0xFFFFE08A.toInt(), 0xFFFFFFFF.toInt(), 0xFF8AF1FF.toInt(), 0xFFFFC46B.toInt())
        val HEART_COLORS = intArrayOf(0xFFFF4F89.toInt(), 0xFFFF8CAE.toInt(), 0xFFFF658C.toInt(), 0xFFFF649E.toInt())
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
        val HAPPY_POSE = floatArrayOf(      -24f,   .04f, .28f,  0f,   .5f,   1f,   .78f, 1.28f, .85f, 0f,   .01f,   0f,  0f,  1.02f)
        val LAUGH_POSE = floatArrayOf(      -28f,   .08f, .78f,  0f,   .82f,  1f,   .9f,  1.32f, 1f,  -.05f, .015f,  0f,  0f,  1f)
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
