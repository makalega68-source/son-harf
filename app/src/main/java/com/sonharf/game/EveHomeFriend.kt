package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
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
    /** Walk and run in place: the clips' forward root motion is pinned (EveRig.pinRoot), the screen moves her. */
    WALK("Walk", true),
    RUN("Run", true),
}

/** Her pace on screen, matched to each clip's stride (Run covers ~4.4x the Walk) so the paws do not slide. */
private const val EVE_WALK_DP_PER_SEC = 46f
private const val EVE_RUN_DP_PER_SEC = 200f

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
    val flee = listOf(
        "Yakalayamazsın! 😄" to "Can't catch me! 😄",
        "Hihi, kaçtım! 🍃" to "Hehe, got away! 🍃",
        "Ebelemece mi oynuyoruz? 🌿" to "Are we playing tag? 🌿",
        "Buradayım, buradaa! 😊" to "Over here, over heeere! 😊",
    )
    val moved = listOf(
        "Vay, uçtum! Burası da güzelmiş 🌿" to "Whee, I flew! Nice spot 🌿",
        "Yeni yerimi beğendim 😊" to "I like my new spot 😊",
        "Hop! Buraya mı koydun beni? 🍃" to "Hop! You put me here? 🍃",
    )
    val idle = listOf(
        "Şu yapraklar çok lezzetli 🌿" to "These leaves are delicious 🌿",
        "Bugün hangi kelimeyi bulacaksın acaba? 🤔" to "I wonder which word you'll find today 🤔",
        "Orman sessiz, tam oyun havası 🍃" to "The forest is quiet, perfect for a game 🍃",
    )
}

/**
 * Eve: the home screen's 3D forest friend. She strolls around the bottom of the screen, breathes,
 * looks around, nibbles leaves, wags her tail, waves a paw, stretches and lies down for a nap when
 * left alone; a tap gets a little hop, a turn of the head and a speech bubble (never voice audio).
 * [modifier] is the area she may roam in (the lobby passes its whole size). Clip changes are skeletal cross-fades, not image swaps. If the
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
    // Where she stands (dp from the bottom-left corner) and which way her body faces (0 = the player).
    // She keeps her spot while the app is open (re-entering the lobby does not reset her).
    val posX = remember { Animatable(EveSession.x) }
    val posY = remember { Animatable(EveSession.y) }
    var bodyYaw by remember { mutableStateOf(0f) }
    var walking by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    // Roaming bounds in dp: furthest x and y her box may reach. A tap sets fleeNext for her life loop.
    val roam = remember { FloatArray(2) }
    val fleeNext = remember { BooleanArray(1) }
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
    // A stroll or a dash: turn towards the spot, move there at the clip's pace, then face the player.
    suspend fun walkTo(tx: Float, ty: Float, run: Boolean = false) {
        val dx = tx - posX.value
        val dy = ty - posY.value
        val distance = hypot(dx, dy)
        if (distance < 14f) return
        // Facing the player is 0 degrees; walking down the screen comes towards the player.
        bodyYaw = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
        walking = true
        play(if (run) EveClip.RUN else EveClip.WALK)
        try {
            delay(if (run) 160L else 340L)
            val speed = if (run) EVE_RUN_DP_PER_SEC else EVE_WALK_DP_PER_SEC
            val millis = (distance / speed * 1_000f).roundToInt()
            coroutineScope {
                launch { posX.animateTo(tx, tween(millis, easing = LinearEasing)) }
                posY.animateTo(ty, tween(millis, easing = LinearEasing))
            }
        } finally {
            walking = false
            bodyYaw = 0f
            EveSession.x = posX.value
            EveSession.y = posY.value
            play(EveClip.BREATHE)
        }
    }
    // A tap startles her: she runs (or trots) off to the side, then turns and peeks back at you.
    suspend fun flee() {
        val goRight = posX.value < roam[0] / 2f
        val distance = 120f + Random.nextFloat() * 130f
        val tx = (posX.value + if (goRight) distance else -distance).coerceIn(0f, roam[0])
        val ty = (posY.value + Random.nextFloat() * 80f - 40f).coerceIn(0f, roam[1])
        walkTo(tx, ty, run = Random.nextInt(3) != 0)
        delay(250L)
        feel(EveMood.HAPPY)
        doGesture(listOf(EveGesture.TILT, EveGesture.WAG, EveGesture.LOOK_UP).random())
        say(line(EveLines.flee))
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
        if (fleeNext[0]) {
            fleeNext[0] = false
            flee()
        }
        var lastAct = -1
        while (true) {
            delay(3_500L + Random.nextLong(4_000L))
            val idleFor = System.currentTimeMillis() - lastTouch
            if (resting || dragging) continue
            if (idleFor > 120_000L) {
                resting = true
                mood = EveMood.SLEEPY
                play(EveClip.LIE_DOWN)
                delay(1_300L)
                play(EveClip.REST)
                continue
            }
            // A varied little life: never the same act twice in a row.
            var act: Int
            // About a third of her acts are strolls, so she really wanders the screen.
            do { act = Random.nextInt(19) } while (act == lastAct || (!live3d && act >= 13))
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
                8 -> doGesture(EveGesture.SHAKE)
                9 -> doGesture(EveGesture.WAG)
                10 -> doGesture(EveGesture.PAW_WAVE)
                11 -> doGesture(EveGesture.STRETCH)
                12 -> doGesture(EveGesture.WIGGLE)
                else -> {
                    // A stroll to somewhere else on the screen, then a look around.
                    walkTo(Random.nextFloat() * roam[0], (posY.value + Random.nextFloat() * 120f - 60f).coerceIn(0f, roam[1]))
                    delay(400L)
                    doGesture(if (Random.nextBoolean()) EveGesture.SNIFF else EveGesture.LOOK_UP)
                }
            }
        }
    }

    LaunchedEffect(node, clipKey) {
        val live = node ?: return@LaunchedEffect
        runCatching { mixer.transition(live.animator, clip.clipName, clip.loop) }
    }

    // She roams the whole area; the bubble floats above her and follows, kept on screen.
    BoxWithConstraints(modifier) {
        // Room for her box, and for her bubble above her near the top.
        roam[0] = (maxWidth.value - EVE_BOX_WIDTH_DP).coerceAtLeast(0f)
        roam[1] = (maxHeight.value - 128f - 110f).coerceAtLeast(0f)
        val bubbleMax = 220f
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset {
                    val x = (posX.value + EVE_BOX_WIDTH_DP / 2f - bubbleMax / 2f)
                        .coerceIn(4f, (maxWidth.value - bubbleMax - 4f).coerceAtLeast(4f))
                    IntOffset(x.dp.roundToPx(), -(posY.value + 122f).dp.roundToPx())
                }
                .widthIn(max = bubbleMax.dp),
        ) {
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
        // Wide enough for her side view while she walks; the tap target stays on her body.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(posX.value.dp.roundToPx(), -posY.value.dp.roundToPx()) }
                .size(width = EVE_BOX_WIDTH_DP.dp, height = 128.dp)
                .graphicsLayer { translationY = -hop.value * 26.dp.toPx() },
        ) {
            if (live3d) {
                EveLiveModel(
                    mixer,
                    onNode = { node = it },
                    nodeProvider = { node },
                    bodyYaw = { bodyYaw },
                    expression = { EveExpression(mood, talking = speech != null, gesture = gesture, gestureKey = gestureKey, walking = walking) },
                )
            } else {
                Image(
                    painterResource(R.drawable.eve_still), sh("Eve", "Eve"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Touches land on this layer above the 3D view, which would otherwise keep them.
            // A tap makes her run off; dragging picks her up and puts her somewhere else.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(width = 112.dp, height = 128.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                lastTouch = System.currentTimeMillis()
                                dragging = true
                                resting = false
                                tapKey++
                                bodyYaw = 0f
                                play(EveClip.BREATHE)
                                feel(EveMood.CURIOUS)
                                scope.launch { hop.animateTo(.55f, tween(160)) }
                            },
                            onDragEnd = {
                                dragging = false
                                EveSession.x = posX.value
                                EveSession.y = posY.value
                                scope.launch { hop.animateTo(0f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMediumLow)) }
                                feel(EveMood.HAPPY)
                                doGesture(EveGesture.WIGGLE)
                                say(line(EveLines.moved))
                            },
                            onDragCancel = {
                                dragging = false
                                scope.launch { hop.animateTo(0f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMediumLow)) }
                            },
                        ) { change, amount ->
                            change.consume()
                            val nx = (posX.value + amount.x.toDp().value).coerceIn(0f, roam[0])
                            val ny = (posY.value - amount.y.toDp().value).coerceIn(0f, roam[1])
                            scope.launch {
                                posX.snapTo(nx)
                                posY.snapTo(ny)
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = sh("Eve ile oyna", "Play with Eve"),
                    ) {
                        lastTouch = System.currentTimeMillis()
                        scope.launch {
                            hop.snapTo(0f)
                            hop.animateTo(1f, tween(170))
                            hop.animateTo(0f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMediumLow))
                        }
                        if (resting) {
                            resting = false
                            tapKey++
                            play(EveClip.STAND_UP)
                            feel(EveMood.SHY)
                            say(line(EveLines.wake))
                            scope.launch {
                                delay(1_400L)
                                play(EveClip.BREATHE)
                            }
                        } else if (live3d) {
                            // Startled and playful: her life loop restarts with a dash away.
                            feel(EveMood.CURIOUS)
                            fleeNext[0] = true
                            tapKey++
                        } else {
                            tapKey++
                            feel(EveMood.HAPPY)
                            doGesture(listOf(EveGesture.NOD, EveGesture.TILT, EveGesture.WAG).random())
                            say(line(EveLines.tap))
                        }
                    },
            )
        }
    }
}

private const val EVE_BOX_WIDTH_DP = 150f

/** The live 3D view: Filament through SceneView on a transparent texture surface. */
@Composable
private fun EveLiveModel(
    mixer: EveClipMixer,
    onNode: (io.github.sceneview.node.ModelNode) -> Unit,
    nodeProvider: () -> io.github.sceneview.node.ModelNode?,
    bodyYaw: () -> Float,
    expression: () -> EveExpression,
) {
    // The body turns smoothly towards where she is heading (0 = facing the player).
    val turn = remember { FloatArray(2) }
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
                    val dt = if (turn[1] == 0f) 0f else ((frameTimeNanos / 1e9f) - turn[1]).coerceIn(0f, .1f)
                    turn[1] = frameTimeNanos / 1e9f
                    var delta = bodyYaw() - turn[0]
                    while (delta > 180f) delta -= 360f
                    while (delta < -180f) delta += 360f
                    turn[0] += delta * (1f - kotlin.math.exp(-dt * 8f))
                    live.rotation = Rotation(y = turn[0])
                    // Rest pose first so the procedural layer never accumulates on bones a clip leaves alone.
                    rig?.restore()
                    mixer.apply(live.animator, frameTimeNanos)
                    if (mixer.uses(live.animator, EveClip.WALK.clipName) || mixer.uses(live.animator, EveClip.RUN.clipName)) rig?.pinRoot()
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
/** Short moves layered over the feeling: head and ears, and the body, tail and paws. Unhurried. */
internal enum class EveGesture(val millis: Long) {
    NONE(0L), NOD(2_000L), SHAKE(2_000L), TILT(2_400L), EAR_PLAY(2_600L), SNIFF(1_800L), LISTEN(2_800L), LOOK_UP(2_200L),
    WAG(2_600L), PAW_WAVE(2_600L), STRETCH(3_000L), WIGGLE(2_200L),
}

internal data class EveExpression(
    val mood: EveMood,
    val talking: Boolean,
    val gesture: EveGesture = EveGesture.NONE,
    val gestureKey: Int = 0,
    val walking: Boolean = false,
)

/**
 * Eve's face is painted into her texture, so her feelings live in the skeleton: head turn, nod
 * and tilt, her big ears (perked, flattened, drooping, twitching), and her body: tail wag and
 * lift, a raised front paw, a stretch and a wiggle of the chest. Axes were checked on a skinned
 * render of the model: head x = nod down, y = turn to the viewer's right, z = tilt; ear x+ = perk,
 * x- = flatten back, z (left +, right -) = droop outwards; tail x+ = lower, z = wag; front leg
 * x- = lift forward; chest (bone_5) x+ = dip forward, z = sway. All layered on top of the clip.
 */
private class EveRig(engine: com.google.android.filament.Engine, asset: com.google.android.filament.gltfio.FilamentAsset) {
    private val tm = engine.transformManager
    private val root = bone(asset, "bone_0")
    private val head = bone(asset, "bone_10")
    private val earLeft = bone(asset, "bone_15")
    private val earRight = bone(asset, "bone_17")
    private val tail = listOfNotNull(bone(asset, "bone_1"), bone(asset, "bone_2"), bone(asset, "bone_3"))
    private val chest = bone(asset, "bone_5")
    private val pawLeft = bone(asset, "bone_22")
    private val pawRight = bone(asset, "bone_26")
    private val controlled = listOfNotNull(head, earLeft, earRight, chest, pawLeft, pawRight) + tail
    private val rest = controlled.associateWith { tm.getTransform(it, FloatArray(16)) }
    private val m = FloatArray(16)
    // Smoothed pose: yaw, pitch, roll, perk, droop, tail lift, tail wag amount.
    private val value = FloatArray(7)
    private var lastFrame = 0L
    private var glanceYaw = 0f
    private var glancePitch = 0f
    private var nextGlanceAt = 0L
    private var twitchAt = 0L
    private var twitchLeft = true
    private var nextTwitchAt = 0L
    private var seenGestureKey = -1
    private var gestureStartedAt = 0L
    private var side = 1f

    private fun bone(asset: com.google.android.filament.gltfio.FilamentAsset, name: String): Int? {
        val entity = asset.getFirstEntityByName(name)
        if (entity == 0) return null
        return tm.getInstance(entity).takeIf { it != 0 }
    }

    fun restore() {
        rest.forEach { (instance, transform) -> tm.setTransform(instance, transform) }
    }

    /** The Walk clip carries her forward; the screen does that instead, so the stride stays in place. */
    fun pinRoot() {
        val instance = root ?: return
        tm.getTransform(instance, m)
        m[13] = WALK_ROOT_Y
        tm.setTransform(instance, m)
    }

    fun express(expression: EveExpression, now: Long) {
        val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1e9f).coerceIn(0f, .1f)
        lastFrame = now
        val t = now / 1e9f
        // Slow glances and the odd ear twitch keep her alive between feelings.
        if (now > nextGlanceAt) {
            // Mostly she looks right at the player; now and then a calm glance aside.
            val looking = !expression.walking && Random.nextInt(3) == 0
            glanceYaw = if (looking) Random.nextFloat() * 20f - 10f else 0f
            glancePitch = if (looking) Random.nextFloat() * 6f - 3f else 0f
            nextGlanceAt = now + (if (looking) 1_400L + Random.nextLong(1_200L) else 3_000L + Random.nextLong(3_500L)) * 1_000_000L
        }
        if (now > nextTwitchAt) {
            twitchLeft = Random.nextBoolean()
            twitchAt = now
            nextTwitchAt = now + (4_500L + Random.nextLong(7_000L)) * 1_000_000L
        }
        // Targets per feeling: yaw, pitch, roll, perk, droop, tail lift (- = up), tail wag amount.
        val target = when (expression.mood) {
            // Ears move only a little: a real animal's ears shift a few degrees, not swing.
            EveMood.NEUTRAL -> floatArrayOf(glanceYaw, -4f + glancePitch, 0f, 1.5f, 0f, 0f, 5f)
            EveMood.HAPPY -> floatArrayOf(0f, -7f, -7f, 5f, 0f, -16f, 18f)
            EveMood.CURIOUS -> floatArrayOf(4f, -5f, 11f, 7f, 0f, -8f, 8f)
            EveMood.SHY -> floatArrayOf(-7f, 7f, 5f, -6f, 3f, 10f, 4f)
            EveMood.SAD -> floatArrayOf(0f, 12f, 0f, -3f, 10f, 20f, 0f)
            EveMood.SLEEPY -> floatArrayOf(0f, 9f, 0f, -4f, 7f, 10f, 2f)
        }
        if (expression.walking) { target[0] = 0f; target[1] = -2f; target[6] = maxOf(target[6], 9f) }
        // Unhurried easing: feelings settle over about half a second.
        val k = 1f - kotlin.math.exp(-dt * 4f)
        for (i in value.indices) value[i] += (target[i] - value[i]) * k
        // Talking shows as a slow, small nod of the head; her painted mouth itself stays still.
        val bob = if (expression.talking) 1.2f * kotlin.math.sin(t * 3.6f) else 0f
        // A small ear flick that rises and settles over ~0.4 s.
        val tw = ((now - twitchAt) / 400_000_000f).coerceIn(0f, 1f)
        val twitch = if (tw < 1f) 4f * kotlin.math.sin(tw * PI_F) else 0f

        // The current gesture, eased in and out over its length.
        if (expression.gestureKey != seenGestureKey) {
            seenGestureKey = expression.gestureKey
            gestureStartedAt = now
            side = if (Random.nextBoolean()) 1f else -1f
        }
        val g = expression.gesture
        val u = if (g == EveGesture.NONE) 1f else ((now - gestureStartedAt) / (g.millis * 1e6f)).coerceIn(0f, 1f)
        val env = if (u >= 1f) 0f else kotlin.math.sin(u * PI_F)
        var gPitch = 0f; var gYaw = 0f; var gRoll = 0f; var gEarL = 0f; var gEarR = 0f; var gSniff = 0f
        var gWag = 0f; var gLift = 0f; var gChestX = 0f; var gChestZ = 0f; var gPawL = 0f; var gPawR = 0f
        when (g) {
            EveGesture.NOD -> gPitch = 8f * kotlin.math.sin(u * 4f * PI_F) * env
            EveGesture.SHAKE -> gYaw = 12f * kotlin.math.sin(u * 4f * PI_F) * env
            EveGesture.TILT -> { gRoll = 18f * side * env; gEarL = 10f * env; gEarR = 10f * env }
            EveGesture.EAR_PLAY -> { gEarL = 14f * kotlin.math.sin(u * 2f * PI_F) * env; gEarR = -14f * kotlin.math.sin(u * 2f * PI_F) * env }
            EveGesture.SNIFF -> { gSniff = 4f * kotlin.math.abs(kotlin.math.sin(u * 6f * PI_F)) * env; gPitch = -6f * env }
            EveGesture.LISTEN -> { gRoll = 10f * side * env; gYaw = 8f * side * env; gEarL = (if (side > 0) 24f else -14f) * env; gEarR = (if (side > 0) -14f else 24f) * env }
            EveGesture.LOOK_UP -> { gPitch = -16f * env; gEarL = 8f * env; gEarR = 8f * env }
            EveGesture.WAG -> { gWag = 24f * kotlin.math.sin(u * 7f * PI_F) * env; gLift = -18f * env; gEarL = 8f * env; gEarR = 8f * env; gRoll = -6f * side * env }
            EveGesture.PAW_WAVE -> {
                // One front paw comes up and waves hello; the head tilts towards it.
                val wave = (-48f + 10f * kotlin.math.sin(u * 5f * PI_F)) * env
                if (side > 0) gPawR = wave else gPawL = wave
                gRoll = 9f * side * env; gChestZ = -4f * side * env; gEarL = 10f * env; gEarR = 10f * env
            }
            EveGesture.STRETCH -> {
                // A play bow: chest dips, front paws reach forward, tail goes up, she looks up at you.
                gChestX = 14f * env; gPawL = -16f * env; gPawR = -16f * env; gLift = -22f * env; gPitch = -10f * env
                gEarL = -10f * env; gEarR = -10f * env
            }
            EveGesture.WIGGLE -> {
                val w = kotlin.math.sin(u * 5f * PI_F) * env
                gChestZ = 7f * w; gWag = -16f * w; gEarL = 12f * w; gEarR = -12f * w
            }
            EveGesture.NONE -> Unit
        }

        rotate(head, value[1] + bob + gPitch, value[0] + gYaw, value[2] + gRoll)
        // Gesture ear moves are kept to a third: small, natural shifts.
        rotate(earLeft, value[3] + gEarL * EAR_GESTURE_SCALE + if (twitchLeft) twitch else 0f, 0f, value[4])
        rotate(earRight, value[3] + gEarR * EAR_GESTURE_SCALE + if (!twitchLeft) twitch else 0f, 0f, -value[4])
        // Sniffing is a few small dips of the head (the muzzle bone warps her painted face).
        if (gSniff != 0f) rotate(head, gSniff * .6f, 0f, 0f)
        if (gChestX != 0f || gChestZ != 0f) rotate(chest, gChestX, 0f, gChestZ)
        if (gPawL != 0f) rotate(pawLeft, gPawL, 0f, 0f)
        if (gPawR != 0f) rotate(pawRight, gPawR, 0f, 0f)
        // The tail sways with her mood (and her stride while walking); each segment trails the last.
        val pace = if (expression.walking) 6.1f else if (expression.mood == EveMood.HAPPY) 4.6f else 1.7f
        tail.forEachIndexed { index, segment ->
            val lag = index * .55f
            val wag = value[6] * kotlin.math.sin(t * pace - lag) * (1f - index * .25f) + gWag * (1f - index * .2f)
            rotate(segment, (value[5] + gLift) * (if (index == 0) 1f else .35f), 0f, wag)
        }
    }

    private fun rotate(instance: Int?, x: Float, y: Float, z: Float) {
        if (instance == null) return
        tm.getTransform(instance, m)
        if (x != 0f) android.opengl.Matrix.rotateM(m, 0, x, 1f, 0f, 0f)
        if (y != 0f) android.opengl.Matrix.rotateM(m, 0, y, 0f, 1f, 0f)
        if (z != 0f) android.opengl.Matrix.rotateM(m, 0, z, 0f, 0f, 1f)
        tm.setTransform(instance, m)
    }

    private companion object {
        /** The root's forward offset in her idle clips, where the in-place stride is pinned. */
        const val WALK_ROOT_Y = 18.34f
        const val EAR_GESTURE_SCALE = .35f
    }
}

private const val PI_F = 3.1415927f

/** One hello per app session (the home screen can be re-entered many times). */
internal object EveSession {
    private var greeted = false
    /** Where she stands in the lobby (dp from the bottom-left corner), kept while the app runs. */
    var x = 0f
    var y = 0f
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

    /** Whether [name] is playing or still fading out. */
    fun uses(animator: Animator, name: String): Boolean =
        listOfNotNull(current, previous).any { animator.getAnimationName(it.index) == name }

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
