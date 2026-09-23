package com.sonharf.game

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.roundToInt
import kotlin.random.Random

private const val MASCOT_MASTER_URL =
    "https://d2ol7oe51mr4n9.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/ee86179e-77c9-4ef7-8495-9f65a72c3c0d.png"

class MascotLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF071224),
                    surface = Color(0xFF0D1A30),
                    primary = Color(0xFF42D7F5),
                    secondary = Color(0xFFB879FF),
                ),
            ) {
                MascotLabScreen()
            }
        }
    }
}

@Composable
private fun MascotLabScreen() {
    val brain = remember { RuleBasedMascotBrain() }
    val mascotBitmap = rememberNetworkImage(MASCOT_MASTER_URL)
    val letters = remember {
        listOf(
            "K", "E", "L", "İ", "M",
            "E", "K", "U", "Ş", "A",
            "T", "M", "A", "S", "I",
            "O", "Y", "U", "N", "A",
            "L", "A", "N", "I", "R",
        )
    }

    var selectedIndex by remember { mutableStateOf(7) }
    var autoTrack by remember { mutableStateOf(true) }
    var aiDemo by remember { mutableStateOf(true) }
    var speaking by remember { mutableStateOf(false) }
    var flyRight by remember { mutableStateOf(false) }
    var isFlying by remember { mutableStateOf(false) }
    var directive by remember {
        mutableStateOf(brain.react(MascotEvent.LetterFocused(2, 1, 'U')))
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(autoTrack) {
        while (autoTrack) {
            delay(1150)
            selectedIndex = (selectedIndex + 1) % letters.size
            val column = selectedIndex % 5
            val row = selectedIndex / 5
            directive = brain.react(
                MascotEvent.LetterFocused(column, row, letters[selectedIndex].first()),
            )
        }
    }

    LaunchedEffect(aiDemo) {
        if (!aiDemo) return@LaunchedEffect
        var step = 0
        while (true) {
            delay(4200)
            directive = when (step++ % 4) {
                0 -> brain.react(MascotEvent.WordChanged("KUŞATMA"))
                1 -> brain.react(MascotEvent.OpponentThreat)
                2 -> brain.react(MascotEvent.MoveAccepted("KUŞATMA", 24))
                else -> brain.react(MascotEvent.Listening)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF071224), Color(0xFF0C1830), Color(0xFF101936)),
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "KELİME KUŞATMASI • MASKOT LAB",
                fontSize = 20.sp,
                color = Color.White,
            )
            Text(
                text = "Gerçek zamanlı 2D • Video yok • Oyun alanı hedef takibi",
                fontSize = 13.sp,
                color = Color(0xFF9EB4D3),
            )

            MascotScene(
                bitmap = mascotBitmap,
                letters = letters,
                selectedIndex = selectedIndex,
                directive = directive,
                speaking = speaking,
                flightTargetRight = flyRight,
                isFlying = isFlying,
                onLetterSelected = { index ->
                    selectedIndex = index
                    val column = index % 5
                    val row = index / 5
                    directive = brain.react(
                        MascotEvent.LetterFocused(column, row, letters[index].first()),
                    )
                },
            )

            LabToggleRow(
                label = "Harfleri otomatik izle",
                active = autoTrack,
                onClick = { autoTrack = !autoTrack },
            )
            LabToggleRow(
                label = "AI davranış demosu",
                active = aiDemo,
                onClick = { aiDemo = !aiDemo },
            )
            LabToggleRow(
                label = "Konuşma / ağız hareketi",
                active = speaking,
                onClick = { speaking = !speaking },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabButton("Başarılı", Modifier.weight(1f)) {
                    directive = brain.react(MascotEvent.MoveAccepted("KUŞATMA", 24))
                }
                LabButton("Hatalı", Modifier.weight(1f)) {
                    directive = brain.react(MascotEvent.MoveRejected("KUSTMA"))
                }
                LabButton("Tehdit", Modifier.weight(1f)) {
                    directive = brain.react(MascotEvent.OpponentThreat)
                }
            }

            LabButton("Uçuş + minik parıltı izi", Modifier.fillMaxWidth()) {
                flyRight = !flyRight
                isFlying = true
                scope.launch {
                    delay(1550)
                    isFlying = false
                }
            }

            Text(
                text = "Durum: ${directive.emotion}  •  Hedef: ${letters[selectedIndex]}  •  " +
                    if (mascotBitmap == null) "maskot yükleniyor" else "master asset aktif",
                fontSize = 12.sp,
                color = Color(0xFF8EDFF0),
            )
            Text(
                text = "AI katmanı şu an güvenli laboratuvar davranış motorudur. Gerçek AI servisi aynı MascotBrain sözleşmesine sonradan bağlanır; oyun mantığına yetki verilmez.",
                fontSize = 12.sp,
                color = Color(0xFF8E9DB5),
            )
        }
    }
}

@Composable
private fun MascotScene(
    bitmap: ImageBitmap?,
    letters: List<String>,
    selectedIndex: Int,
    directive: MascotDirective,
    speaking: Boolean,
    flightTargetRight: Boolean,
    isFlying: Boolean,
    onLetterSelected: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(570.dp)
            .background(Color(0xC90A1428), RoundedCornerShape(22.dp))
            .border(1.dp, Color(0x3342D7F5), RoundedCornerShape(22.dp))
            .padding(12.dp),
    ) {
        val mascotSize = 164.dp
        val horizontalTravelPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            (maxWidth - mascotSize - 10.dp).coerceAtLeast(0.dp).toPx()
        }
        val targetFlight = if (flightTargetRight) 1f else 0f
        val flightX by animateFloatAsState(
            targetValue = targetFlight,
            animationSpec = tween(1400),
            label = "mascotFlight",
        )

        SparkleTrail(
            xPx = flightX * horizontalTravelPx,
            active = isFlying,
            modifier = Modifier.fillMaxSize(),
        )

        RealtimeMascot(
            bitmap = bitmap,
            emotion = directive.emotion,
            gazeX = directive.gazeX,
            gazeY = directive.gazeY,
            speaking = speaking,
            modifier = Modifier
                .size(mascotSize)
                .offset { IntOffset((flightX * horizontalTravelPx).roundToInt(), 0) },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "Maskot bu tahtadaki seçili harfi izliyor",
                color = Color(0xFFAAC4DE),
                fontSize = 12.sp,
            )
            for (row in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (column in 0 until 5) {
                        val index = row * 5 + column
                        val selected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(
                                    if (selected) Color(0xFF173F60) else Color(0xFF111F38),
                                    RoundedCornerShape(10.dp),
                                )
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color(0xFFFFC45C) else Color(0x334D6B8D),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onLetterSelected(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = letters[index],
                                color = Color.White,
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10253B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Text(
                    text = "AKTİF KELİME  •  KUŞATMA",
                    color = Color(0xFF6DE7F5),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun RealtimeMascot(
    bitmap: ImageBitmap?,
    emotion: MascotEmotion,
    gazeX: Float,
    gazeY: Float,
    speaking: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "mascotIdle")
    val hoverPx by infinite.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "hover",
    )
    val breath by infinite.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "lightBreath",
    )
    val speechWave by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(135), RepeatMode.Reverse),
        label = "speechWave",
    )
    var blinkClosed by remember { mutableStateOf(false) }
    val blink by animateFloatAsState(
        targetValue = if (blinkClosed) 1f else 0f,
        animationSpec = tween(75),
        label = "blink",
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2600, 4300))
            blinkClosed = true
            delay(95)
            blinkClosed = false
        }
    }

    val emotionTilt = when (emotion) {
        MascotEmotion.SURPRISED -> -1.2f
        MascotEmotion.THINKING -> 1.1f
        MascotEmotion.CONCERNED -> 0.8f
        MascotEmotion.PROUD -> -0.6f
        else -> 0f
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationY = hoverPx
            rotationZ = gazeX.coerceIn(-1f, 1f) * 2.2f + emotionTilt
        },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF54E8FF).copy(alpha = breath * 0.34f),
                        Color(0xFFB85CFF).copy(alpha = breath * 0.18f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.56f,
                ),
                radius = size.minDimension * 0.56f,
            )

            if (bitmap == null) {
                drawCircle(Color(0xFF10183C), radius = size.minDimension * 0.38f)
                return@Canvas
            }

            val w = size.width
            val h = size.height
            val scaleX = w / 1254f
            val scaleY = h / 1254f
            drawImage(
                image = bitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
            )

            val eyeDx = gazeX.coerceIn(-1f, 1f) * 3.0f
            val eyeDy = ((gazeY.coerceIn(0f, 1f) - 0.55f) * 6.0f)

            fun shiftedEye(x0: Int, y0: Int, x1: Int, y1: Int) {
                val left = x0 * scaleX
                val top = y0 * scaleY
                val right = x1 * scaleX
                val bottom = y1 * scaleY
                clipRect(left, top, right, bottom) {
                    drawImage(
                        image = bitmap,
                        srcOffset = IntOffset(x0, y0),
                        srcSize = IntSize(x1 - x0, y1 - y0),
                        dstOffset = IntOffset(
                            (left + eyeDx).roundToInt(),
                            (top + eyeDy).roundToInt(),
                        ),
                        dstSize = IntSize(
                            (right - left).roundToInt(),
                            (bottom - top).roundToInt(),
                        ),
                    )
                }
            }

            shiftedEye(385, 515, 635, 765)
            shiftedEye(770, 515, 1020, 765)

            if (blink > 0.02f) {
                val face = Color(0xFF12143B).copy(alpha = 0.97f)
                val leftTop = 535f * scaleY
                val eyeHeight = 210f * scaleY * blink
                drawRoundRect(
                    color = face,
                    topLeft = Offset(390f * scaleX, leftTop),
                    size = Size(240f * scaleX, eyeHeight),
                    cornerRadius = CornerRadius(55f * scaleX, 55f * scaleY),
                )
                drawRoundRect(
                    color = face,
                    topLeft = Offset(780f * scaleX, leftTop),
                    size = Size(230f * scaleX, eyeHeight),
                    cornerRadius = CornerRadius(55f * scaleX, 55f * scaleY),
                )
            }

            if (speaking) {
                val amp = speechWave.coerceIn(0f, 1f)
                drawOval(
                    color = Color(0xFF12143B),
                    topLeft = Offset(630f * scaleX, 730f * scaleY),
                    size = Size(165f * scaleX, 145f * scaleY),
                )
                drawOval(
                    color = Color(0xFF8B175C),
                    topLeft = Offset(655f * scaleX, (760f - 12f * amp) * scaleY),
                    size = Size(115f * scaleX, (62f + 42f * amp) * scaleY),
                )
                drawOval(
                    color = Color(0xFFFF78CB),
                    topLeft = Offset(672f * scaleX, (797f + 7f * amp) * scaleY),
                    size = Size(82f * scaleX, (25f + 18f * amp) * scaleY),
                )
            }
        }
    }
}

@Composable
private fun SparkleTrail(
    xPx: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "sparkles")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850)),
        label = "sparklePhase",
    )
    Canvas(modifier) {
        if (!active) return@Canvas
        val baseX = 82f + xPx
        val baseY = 118f
        repeat(6) { index ->
            val local = (phase + index * 0.17f) % 1f
            val alpha = (1f - local) * 0.7f
            val x = baseX - 18f - local * 70f
            val y = baseY + ((index % 3) - 1) * 12f + local * 8f
            drawCircle(
                color = if (index % 2 == 0) Color(0xFF72EDFF) else Color(0xFFE878FF),
                radius = 2.2f + (1f - local) * 1.4f,
                center = Offset(x, y),
                alpha = alpha,
            )
        }
    }
}

@Composable
private fun LabToggleRow(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101D34), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
        Text(
            text = if (active) "AÇIK" else "KAPALI",
            color = if (active) Color(0xFF66E6CF) else Color(0xFF77879E),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun LabButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF17405D),
            contentColor = Color.White,
        ),
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
private fun rememberNetworkImage(url: String): ImageBitmap? {
    var image by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        image = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return image
}
