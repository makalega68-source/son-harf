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
        play(EveClip.LOOK_AROUND)
        say(line(EveLines.hello))
    }

    // Her own little life: mostly breathing, sometimes looking around or grazing; a nap when alone.
    LaunchedEffect(ready, tapKey) {
        if (!ready) return@LaunchedEffect
        while (true) {
            delay(7_000L + Random.nextLong(7_000L))
            val idleFor = System.currentTimeMillis() - lastTouch
            if (resting) continue
            if (idleFor > 75_000L) {
                resting = true
                play(EveClip.LIE_DOWN)
                delay(1_300L)
                play(EveClip.REST)
                continue
            }
            when (Random.nextInt(10)) {
                0, 1, 2 -> {
                    play(EveClip.LOOK_AROUND)
                    delay(7_600L)
                    play(EveClip.BREATHE)
                }
                3, 4 -> {
                    play(EveClip.GRAZE_ONCE)
                    delay(3_000L)
                    play(EveClip.BREATHE)
                }
                5 -> {
                    play(EveClip.GRAZE)
                    if (Random.nextInt(3) == 0) say(line(EveLines.idle))
                    delay(5_000L)
                    play(EveClip.BREATHE)
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(node, clipKey) {
        val live = node ?: return@LaunchedEffect
        runCatching { mixer.transition(live.animator, clip.clipName, clip.loop) }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.widthIn(max = 200.dp), contentAlignment = Alignment.BottomCenter) {
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
                .size(width = 118.dp, height = 128.dp)
                .graphicsLayer { translationY = -hop.value * 26.dp.toPx() },
        ) {
            if (live3d) {
                EveLiveModel(mixer, onNode = { node = it }, nodeProvider = { node })
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
                            say(line(EveLines.wake))
                            scope.launch {
                                delay(1_400L)
                                play(EveClip.BREATHE)
                            }
                        } else {
                            play(EveClip.LOOK_AROUND)
                            say(line(EveLines.tap))
                            scope.launch {
                                delay(4_000L)
                                if (!resting) play(EveClip.BREATHE)
                            }
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
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, EveAsset.MODEL) ?: return
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
            nodeProvider()?.let { live -> runCatching { mixer.apply(live.animator, frameTimeNanos) } }
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
            // A slight three-quarter turn towards the middle of the screen.
            rotation = Rotation(y = 22f),
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
        animator.updateBoneMatrices()
    }

    private fun time(animator: Animator, track: Track, frameTimeNanos: Long): Float {
        val duration = animator.getAnimationDuration(track.index).coerceAtLeast(.0001f)
        val elapsed = ((frameTimeNanos - track.startedAt).coerceAtLeast(0L) / 1_000_000_000.0).toFloat()
        return if (track.loop) elapsed % duration else min(elapsed, duration)
    }
}
