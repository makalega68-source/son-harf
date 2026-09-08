package com.sonharf.game.mascot

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ZAFER UÇUŞU.
 * Maç bittikten SONRA gösterilir; kapatacak bir oyun alanı kalmadığı için
 * maskot burada tamamen serbesttir: ekranda süzülür, takla atar, konfeti saçar.
 */
@Composable
fun VictoryFlight(mascotKey: String = "magecat", modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "flight")

    val p by t.animateFloat(
        initialValue = 0f, targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "path"
    )
    val spin by t.animateFloat(
        initialValue = -12f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spin"
    )
    val pop by t.animateFloat(
        initialValue = 0.94f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pop"
    )

    // neşeli ifadeler arasında dönüş
    var moodIndex by remember { mutableIntStateOf(0) }
    val seq = remember { MascotBrain.celebrationSequence() }
    LaunchedEffect(Unit) {
        while (true) { delay(900); moodIndex = (moodIndex + 1) % seq.size }
    }

    val confetti = remember {
        List(26) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                listOf(Color(0xFFFFD36B), Color(0xFF79D2F2), Color(0xFFF4B3C4),
                    Color(0xFF7C6BE8), Color(0xFF9BE38A)).random()
            )
        }
    }
    val fall by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "fall"
    )

    Box(modifier = modifier.fillMaxSize()) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            confetti.forEach { (x, seed, c) ->
                val y = ((seed + fall) % 1f) * size.height
                val drift = sin((y / 60f) + seed * 6f) * 14f
                drawCircle(
                    color = c, radius = size.width * 0.011f,
                    center = Offset(x * size.width + drift, y)
                )
            }
        }

        BoxWithFlightLayout(p, spin, pop, seq[moodIndex], mascotKey)
    }
}

@Composable
private fun BoxWithFlightLayout(p: Float, spin: Float, pop: Float, mood: Mood, mascotKey: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        val flightModifier = Modifier.graphicsLayer {
            val w = size.width; val h = size.height
            // sekiz (lemniscate) yörüngesi — ekranda dolaşır
            translationX = (w * 0.30f) * sin(p) - w * 0.06f
            translationY = (h * 0.16f) * sin(2f * p) - h * 0.02f
            rotationZ = spin + cos(p) * 8f
            scaleX = pop; scaleY = pop
        }
        if (mascotKey == "magecat") {
            MageCatFigure(mood = mood, eyesVariant = MascotBrain.eyesVariant,
                size = 128.dp, modifier = flightModifier)
        } else {
            RiveMascotFigure(key = mascotKey, mood = mood, size = 128.dp, modifier = flightModifier)
        }
    }
}
