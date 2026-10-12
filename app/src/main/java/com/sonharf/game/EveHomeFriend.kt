package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.os.Build
import com.google.android.filament.gltfio.Animator
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

/** Eve's skeletal clips that stay in place (the walk/run/combat clips move the root and are not used). */
internal enum class EveClip(val clipName: String, val loop: Boolean) {
    BREATHE("IdleBreathe", true),
    LOOK_AROUND("IdleLookAround", false),
    GRAZE("IdleGraze", true),
    GRAZE_ONCE("GrazeOnce", false),
    LIE_DOWN("GoToRest", false),
    REST("Rest", true),
    STAND_UP("RestToGoBackUp", false),
}

internal object EveAsset {
    const val MODEL = "models/eve/eve.glb"
}

/** Eve's lines: a calm forest friend who loves words. */
internal object EveLines {
    val hello = listOf(
        "Merhaba %s! Ben Eve, ormandan geldim 🌿" to "Hi %s! I'm Eve, I came from the forest 🌿",
        "Selam %s! Yapraklarım seni görünce hışırdadı 🍃" to "Hey %s! My leaves rustle when I see you 🍃",
    )
    val tap = listOf(
        "Hihi, kulaklarım gıdıklandı! 🌱" to "Hehe, that tickles my ears! 🌱",
        "Bir maç yapalım mı? Ben buradan izlerim 👀" to "Shall we play a match? I'll watch from here 👀",
        "Ormanda kelimeler ağaçlarda yetişir, biliyor muydun? 🌳" to "In the forest, words grow on trees, did you know? 🌳",
        "Sen oyna, ben şans getireyim 🍀" to "You play, I'll bring the luck 🍀",
        "Yapraklarımı okşadın, ne güzel! 💚" to "You petted my leaves, how nice! 💚",
    )
    val wake = listOf(
        "Hıı? Uyuyakalmışım… buradayım! 😊" to "Hm? I dozed off… I'm here! 😊",
        "Esneyerek günaydın! 🌞" to "A big yawn and good morning! 🌞",
    )
    val idle = listOf(
        "Şu yapraklar çok lezzetli 🌿" to "These leaves are delicious 🌿",
        "Bugün hangi kelimeyi bulacaksın acaba? 🤔" to "I wonder which word you'll find today 🤔",
        "Orman sessiz, tam oyun havası 🍃" to "The forest is quiet, perfect for a game 🍃",
    )
}

/**
 * Eve: the home screen's 3D forest friend. She breathes, looks around, nibbles leaves and lies
 * down for a nap when left alone; a tap gets a little hop, a turn of the head and a speech
 * bubble (never voice audio). Clip changes are skeletal cross-fades, not image swaps. If the
 * model cannot be read the friend simply stays hidden, so the home screen never breaks.
 */
@Composable
internal fun EveHomeFriend(
    playerName: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val available = remember { runCatching { context.assets.open(EveAsset.MODEL).use { } }.isSuccess }
    if (!available) return

    // Emulators (software GPU) cannot keep a live Filament view stable: they get Eve as a still.
    val live3d = remember { !EveDevice.isEmulator }
    val mixer = remember { EveClipMixer(blendNanos = 320_000_000L) }
    var node by remember { mutableStateOf<io.github.sceneview.node.ModelNode?>(null) }
    val ready = node != null || !live3d
    var clip by remember { mutableStateOf(EveClip.BREATHE) }
    var clipKey by remember { mutableIntStateOf(0) }
    var speech by remember { mutableStateOf<String?>(null) }
    var speechKey by remember { mutableIntStateOf(0) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var resting by remember { mutableStateOf(false) }
    var tapKey by remember { mutableIntStateOf(0) }
    // Her face lives in body language: head turn/tilt/nod and ears, driven per frame by EveRig.
    var mood by remember { mutableStateOf(EveMood.NEUTRAL) }
    var moodKey by remember { mutableIntStateOf(0) }
    var gesture by remember { mutableStateOf(EveGesture.NONE) }
    var gestureKey by remember { mutableIntStateOf(0) }
    val hop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val firstName = playerName?.trim()?.split(' ')?.firstOrNull()?.takeIf { it.isNotBlank() && it.length <= 14 }

    fun line(options: List<Pair<String, String>>): String {
        val usable = options.filter { firstName != null || !it.first.contains("%s") }.ifEmpty { options }
        val chosen = usable[Random.nextInt(usable.size)]
        return sh(chosen.first, chosen.second).replace("%s", firstName.orEmpty()).replace(" !", "!").trim()
    }
    fun play(next: EveClip) {
        clip = next
        clipKey++
    }
    fun say(text: String) {
        speech = text
        speechKey++
    }
    fun feel(next: EveMood) {
        mood = next
        moodKey++
    }
    fun doGesture(next: EveGesture) {
        gesture = next
        gestureKey++
    }

    // A feeling shows for a few seconds, then she settles back to her calm look (or stays sleepy).
    LaunchedEffect(moodKey) {
        if (mood == EveMood.NEUTRAL || mood == EveMood.SLEEPY) return@LaunchedEffect
        delay(3_600L)
        if (!resting) mood = EveMood.NEUTRAL
    }

    // The speech bubble stays long enough to read, then hides.
    LaunchedEffect(speechKey) {
        if (speech == null) return@LaunchedEffect
        delay((2_200L + (speech?.length ?: 0) * 45L).coerceAtMost(6_000L))
        speech = null
    }

    // Hello once per app session, after she has appeared.
    LaunchedEffect(ready) {
        if (!ready || !EveSession.claimHello()) return@LaunchedEffect
        delay(1_200L)
        feel(EveMood.CURIOUS)
        delay(900L)
        feel(EveMood.HAPPY)
        say(line(EveLines.hello))
    }

    // Her own little life: mostly breathing, sometimes looking around or grazing; a nap when alone.
    LaunchedEffect(ready, tapKey) {
        if (!ready) return@LaunchedEffect
        var lastAct = -1
        while (true) {
            delay(4_500L + Random.nextLong(5_000L))
            val idleFor = System.currentTimeMillis() - lastTouch
            if (resting) continue
            if (idleFor > 75_000L) {
                resting = true
                mood = EveMood.SLEEPY
                play(EveClip.LIE_DOWN)
                delay(1_300L)
                play(EveClip.REST)
                continue
            }
            // A varied little life: never the same act twice in a row.
            var act: Int
            do { act = Random.nextInt(9) } while (act == lastAct)
            lastAct = act
            when (act) {
                0 -> doGesture(EveGesture.TILT)
                1 -> doGesture(EveGesture.EAR_PLAY)
                2 -> doGesture(EveGesture.SNIFF)
                3 -> doGesture(EveGesture.LISTEN)
                4 -> doGesture(EveGesture.LOOK_UP)
                5 -> feel(if (Random.nextBoolean()) EveMood.CURIOUS else EveMood.HAPPY)
                6 -> {
                    play(EveClip.GRAZE_ONCE)
                    delay(3_000L)
                    play(EveClip.BREATHE)
                }
                7 -> {
                    doGesture(EveGesture.NOD)
                    if (Random.nextInt(2) == 0) say(line(EveLines.idle))
                }
                else -> doGesture(EveGesture.SHAKE)
            }
        }
    }

    LaunchedEffect(node, clipKey) {
        val live = node ?: return@LaunchedEffect
        runCatching { mixer.transition(live.animator, clip.clipName, clip.loop) }
    }

    // The bubble floats above her in its own slot, so showing it never shifts Eve sideways.
    Box(modifier) {
        Box(Modifier.align(Alignment.BottomStart).padding(bottom = 122.dp).widthIn(max = 220.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = speech != null,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = .8f),
                exit = fadeOut(tween(220)) + scaleOut(targetScale = .9f),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFF7FB069)),
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        speech.orEmpty(),
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color(0xFF1F3B2C),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .size(width = 118.dp, height = 128.dp)
                .graphicsLayer { translationY = -hop.value * 26.dp.toPx() },
        ) {
            if (live3d) {
                EveLiveModel(
                    mixer,
                    onNode = { node = it },
                    nodeProvider = { node },
                    expression = { EveExpression(mood, talking = speech != null, gesture = gesture, gestureKey = gestureKey) },
                )
            } else {
                Image(
                    painterResource(R.drawable.eve_still), sh("Eve", "Eve"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Taps land on this layer above the 3D view, which would otherwise keep the touches.
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = sh("Eve ile oyna", "Play with Eve"),
                    ) {
                        lastTouch = System.currentTimeMillis()
                        tapKey++
                        scope.launch {
                            hop.snapTo(0f)
                            hop.animateTo(1f, tween(170))
                            hop.animateTo(0f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMediumLow))
                        }
                        if (resting) {
                            resting = false
                            play(EveClip.STAND_UP)
                            feel(EveMood.SHY)
                            say(line(EveLines.wake))
                            scope.launch {
                                delay(1_400L)
                                play(EveClip.BREATHE)
                            }
                        } else {
                            play(EveClip.BREATHE)
                            feel(listOf(EveMood.HAPPY, EveMood.HAPPY, EveMood.CURIOUS, EveMood.SHY).random())
                            doGesture(listOf(EveGesture.EAR_PLAY, EveGesture.NOD, EveGesture.TILT).random())
                            say(line(EveLines.tap))
                        }
                    },
            )
        }
    }
}

/** The live 3D view: Filament through SceneView on a transparent texture surface. */
@Composable
private fun EveLiveModel(
    mixer: EveClipMixer,
    onNode: (io.github.sceneview.node.ModelNode) -> Unit,
    nodeProvider: () -> io.github.sceneview.node.ModelNode?,
    expression: () -> EveExpression,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, EveAsset.MODEL) ?: return
    val rig = remember(modelInstance) { runCatching { EveRig(engine, modelInstance.asset) }.getOrNull() }
    val mainLight = rememberMainLightNode(engine) {
        intensity = 9_000f
        lightDirection = Direction(-0.45f, -0.75f, -0.48f)
    }
    val fillLight = rememberFillLightNode(engine) {
        intensity = 3_000f
        lightDirection = Direction(0.68f, -0.47f, 0.56f)
    }
    SceneView(
        modifier = Modifier.fillMaxSize(),
        surfaceType = SurfaceType.TextureSurface,
        isOpaque = false,
        engine = engine,
        modelLoader = modelLoader,
        mainLightNode = mainLight,
        fillLightNode = fillLight,
        cameraManipulator = null,
        onFrame = { frameTimeNanos ->
            nodeProvider()?.let { live ->
                runCatching {
                    // Rest pose first so the procedural layer never accumulates on bones a clip leaves alone.
                    rig?.restore()
                    mixer.apply(live.animator, frameTimeNanos)
                    rig?.express(expression(), frameTimeNanos)
                    live.animator.updateBoneMatrices()
                }
            }
        },
    ) {
        ModelNode(
            modelInstance = modelInstance,
            scaleToUnits = 2.05f,
            centerOrigin = Position(0f, -1.0f, 0f),
            autoAnimate = false,
            animationName = null,
            animationLoop = true,
            position = Position(0f, -0.28f, 0f),
            // Facing the player; the head turns towards the screen on its own (EveRig).
            rotation = Rotation(y = 0f),
            apply = { onNode(this) },
        )
    }
}

internal object EveDevice {
    /** True on Android emulators (goldfish/ranchu), whose software GPU stalls under Filament. */
    val isEmulator: Boolean by lazy {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        fingerprint.startsWith("generic") || fingerprint.contains("emulator") ||
            hardware.contains("goldfish") || hardware.contains("ranchu") ||
            Build.MODEL.orEmpty().contains("Android SDK built for") || Build.PRODUCT.orEmpty().startsWith("sdk")
    }
}

internal enum class EveMood { NEUTRAL, HAPPY, CURIOUS, SHY, SAD, SLEEPY }

/** Short head and ear moves layered over the feeling. */
internal enum class EveGesture(val millis: Long) {
    NONE(0L), NOD(1_300L), SHAKE(1_300L), TILT(1_800L), EAR_PLAY(1_600L), SNIFF(1_300L), LISTEN(2_200L), LOOK_UP(1_800L),
}

internal data class EveExpression(
    val mood: EveMood,
    val talking: Boolean,
    val gesture: EveGesture = EveGesture.NONE,
    val gestureKey: Int = 0,
)

/**
 * Eve's face is painted into her texture, so her feelings live in the skeleton: head turn, nod
 * and tilt, and her big ears (perked, flattened, drooping, twitching). Angles were checked on a
 * skinned render of the model: head x = nod down, y = turn to the viewer's right, z = tilt; ear
 * x+ = perk, x- = flatten back, z (left +, right -) = droop outwards. All on top of the clip.
 */
private class EveRig(engine: com.google.android.filament.Engine, asset: com.google.android.filament.gltfio.FilamentAsset) {
    private val tm = engine.transformManager
    private val head = bone(asset, "bone_10")
    private val earLeft = bone(asset, "bone_15")
    private val earRight = bone(asset, "bone_17")
    private val muzzle = bone(asset, "bone_11")
    private val controlled = listOfNotNull(head, earLeft, earRight, muzzle)
    private val rest = controlled.associateWith { tm.getTransform(it, FloatArray(16)) }
    private val m = FloatArray(16)
    // Smoothed pose: yaw, pitch, roll, perk, droop.
    private val value = FloatArray(5)
    private var lastFrame = 0L
    private var glanceYaw = 0f
    private var glancePitch = 0f
    private var nextGlanceAt = 0L
    private var twitchUntil = 0L
    private var twitchLeft = true
    private var nextTwitchAt = 0L
    private var seenGestureKey = -1
    private var gestureStartedAt = 0L
    private var tiltSide = 1f

    private fun bone(asset: com.google.android.filament.gltfio.FilamentAsset, name: String): Int? {
        val entity = asset.getFirstEntityByName(name)
        if (entity == 0) return null
        return tm.getInstance(entity).takeIf { it != 0 }
    }

    fun restore() {
        rest.forEach { (instance, transform) -> tm.setTransform(instance, transform) }
    }

    fun express(expression: EveExpression, now: Long) {
        val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1e9f).coerceIn(0f, .1f)
        lastFrame = now
        val t = now / 1e9f
        // Little glances around the screen and quick ear twitches keep her alive between feelings.
        if (now > nextGlanceAt) {
            // Mostly she looks right at the player; now and then a short glance aside.
            val looking = Random.nextInt(3) == 0
            glanceYaw = if (looking) Random.nextFloat() * 20f - 10f else 0f
            glancePitch = if (looking) Random.nextFloat() * 6f - 3f else 0f
            nextGlanceAt = now + (if (looking) 900L + Random.nextLong(900L) else 2_500L + Random.nextLong(3_000L)) * 1_000_000L
        }
        if (now > nextTwitchAt) {
            twitchLeft = Random.nextBoolean()
            twitchUntil = now + 170_000_000L
            nextTwitchAt = now + (3_500L + Random.nextLong(6_000L)) * 1_000_000L
        }
        // Targets per feeling: yaw, pitch, roll, perk, droop. Neutral looks towards the screen centre.
        val target = when (expression.mood) {
            EveMood.NEUTRAL -> floatArrayOf(glanceYaw, -4f + glancePitch, 0f, 4f, 0f)
            EveMood.HAPPY -> floatArrayOf(0f, -8f, -9f, 16f, 0f)
            EveMood.CURIOUS -> floatArrayOf(4f, -6f, 15f, 22f, 0f)
            EveMood.SHY -> floatArrayOf(-8f, 8f, 7f, -18f, 8f)
            EveMood.SAD -> floatArrayOf(0f, 14f, 0f, -6f, 30f)
            EveMood.SLEEPY -> floatArrayOf(0f, 10f, 0f, -10f, 18f)
        }
        val k = 1f - kotlin.math.exp(-dt * 7f)
        for (i in value.indices) value[i] += (target[i] - value[i]) * k
        val bob = if (expression.talking) 2.2f * kotlin.math.sin(t * 11f) else 0f
        val happyWiggle = if (expression.mood == EveMood.HAPPY) 5f * kotlin.math.sin(t * 9f) else 0f
        val twitch = if (now < twitchUntil) 16f else 0f

        // The current gesture, eased in and out over its length.
        if (expression.gestureKey != seenGestureKey) {
            seenGestureKey = expression.gestureKey
            gestureStartedAt = now
            tiltSide = if (Random.nextBoolean()) 1f else -1f
        }
        val g = expression.gesture
        val u = if (g == EveGesture.NONE) 1f else ((now - gestureStartedAt) / (g.millis * 1e6f)).coerceIn(0f, 1f)
        val env = if (u >= 1f) 0f else kotlin.math.sin(u * PI_F)
        var gPitch = 0f; var gYaw = 0f; var gRoll = 0f; var gEarL = 0f; var gEarR = 0f; var gSniff = 0f
        when (g) {
            EveGesture.NOD -> gPitch = 9f * kotlin.math.sin(u * 4f * PI_F) * env
            EveGesture.SHAKE -> gYaw = 13f * kotlin.math.sin(u * 4f * PI_F) * env
            EveGesture.TILT -> { gRoll = 18f * tiltSide * env; gEarL = 10f * env; gEarR = 10f * env }
            EveGesture.EAR_PLAY -> { gEarL = 22f * kotlin.math.sin(u * 6f * PI_F) * env; gEarR = -22f * kotlin.math.sin(u * 6f * PI_F) * env }
            EveGesture.SNIFF -> { gSniff = 5f * kotlin.math.abs(kotlin.math.sin(u * 10f * PI_F)) * env; gPitch = -6f * env }
            EveGesture.LISTEN -> { gRoll = 10f * tiltSide * env; gYaw = 8f * tiltSide * env; gEarL = (if (tiltSide > 0) 24f else -14f) * env; gEarR = (if (tiltSide > 0) -14f else 24f) * env }
            EveGesture.LOOK_UP -> { gPitch = -16f * env; gEarL = 8f * env; gEarR = 8f * env }
            EveGesture.NONE -> Unit
        }

        rotate(head, value[1] + bob + gPitch, value[0] + gYaw, value[2] + gRoll)
        rotate(earLeft, value[3] + happyWiggle + gEarL + if (twitchLeft) twitch else 0f, 0f, value[4])
        rotate(earRight, value[3] - happyWiggle + gEarR + if (!twitchLeft) twitch else 0f, 0f, -value[4])
        val talk = if (expression.talking) 4f * kotlin.math.abs(kotlin.math.sin(t * 13f)) else 0f
        if (talk + gSniff != 0f) rotate(muzzle, talk + gSniff, 0f, 0f)
    }

    private fun rotate(instance: Int?, x: Float, y: Float, z: Float) {
        if (instance == null) return
        tm.getTransform(instance, m)
        if (x != 0f) android.opengl.Matrix.rotateM(m, 0, x, 1f, 0f, 0f)
        if (y != 0f) android.opengl.Matrix.rotateM(m, 0, y, 0f, 1f, 0f)
        if (z != 0f) android.opengl.Matrix.rotateM(m, 0, z, 0f, 0f, 1f)
        tm.setTransform(instance, m)
    }
}

private const val PI_F = 3.1415927f

/** One hello per app session (the home screen can be re-entered many times). */
internal object EveSession {
    private var greeted = false
    fun claimHello(): Boolean {
        if (greeted) return false
        greeted = true
        return true
    }
}

/**
 * A small skeletal mixer on Filament's animator: applyAnimation(current), then
 * applyCrossFade(previous) with a smoothstep weight, then updateBoneMatrices(). Bone poses are
 * blended, so switching clips never pops.
 */
private class EveClipMixer(private val blendNanos: Long) {
    private data class Track(val index: Int, val startedAt: Long, val loop: Boolean)

    private var current: Track? = null
    private var previous: Track? = null
    private var blendStartedAt = 0L

    fun transition(animator: Animator, name: String, loop: Boolean, now: Long = System.nanoTime()) {
        val index = (0 until animator.animationCount).firstOrNull { animator.getAnimationName(it) == name } ?: return
        previous = current
        current = Track(index, now, loop)
        blendStartedAt = now
    }

    fun apply(animator: Animator, frameTimeNanos: Long) {
        val active = current ?: return
        animator.applyAnimation(active.index, time(animator, active, frameTimeNanos))
        previous?.let { old ->
            val linear = if (blendNanos <= 0L) 1f
            else ((frameTimeNanos - blendStartedAt).toDouble() / blendNanos).toFloat().coerceIn(0f, 1f)
            val alpha = linear * linear * (3f - 2f * linear)
            if (linear < 1f) animator.applyCrossFade(old.index, time(animator, old, frameTimeNanos), alpha)
            else previous = null
        }
    }

    private fun time(animator: Animator, track: Track, frameTimeNanos: Long): Float {
        val duration = animator.getAnimationDuration(track.index).coerceAtLeast(.0001f)
        val elapsed = ((frameTimeNanos - track.startedAt).coerceAtLeast(0L) / 1_000_000_000.0).toFloat()
        return if (track.loop) elapsed % duration else min(elapsed, duration)
    }
}
