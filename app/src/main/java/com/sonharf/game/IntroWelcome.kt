package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.delay

/** When, before the clip ends, the Kelime Tahtı logo starts to come down (as Obi closes his eyes). */
internal const val INTRO_LOGO_LEAD_MS = 2_600L
internal const val INTRO_LOGO_DROP_MS = 900

/** The home screen shows the logo at 320×170 dp; the intro uses it 40% smaller. */
private val IntroLogoWidth = (320 * .6f).dp
private val IntroLogoHeight = (170 * .6f).dp

/**
 * First meeting with the game: the welcome clip plays full screen with sound. Near the end, as the
 * mascot closes its eyes, the Kelime Tahtı logo glides down from the top to above the mascot
 * (never touching it). When the clip ends, the language choice and the continue button rise in
 * under the mascot.
 */
@Composable
internal fun IntroWelcomeScreen(onContinue: (String) -> Unit) {
    val context = LocalContext.current
    var ended by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    val logoDrop = remember { Animatable(0f) }
    val player = remember { ChromaKeyVideoController() }

    // The clip has its own soundtrack: the game music waits until the intro is over.
    DisposableEffect(Unit) {
        SonHarfBackgroundMusic.pause()
        onDispose { SonHarfBackgroundMusic.start(context) }
    }
    // Pause with the app, carry on when it comes back.
    val lifecycle = LocalView.current.findViewTreeLifecycleOwner()?.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> if (!ended) player.resume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Watch the clock: drop the logo in time with the eyes closing, then show the choices.
    LaunchedEffect(Unit) {
        while (!ended) {
            val duration = player.durationMs
            if (player.completed) ended = true
            if (duration > 0 && player.positionMs >= duration - INTRO_LOGO_LEAD_MS && logoDrop.value == 0f && !logoDrop.isRunning) {
                break
            }
            delay(80L)
        }
        logoDrop.animateTo(1f, tween(INTRO_LOGO_DROP_MS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(player.completed) { if (player.completed) ended = true }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1430), Color(0xFF1B2F5E), Color(0xFF081226))))) {
        // Full screen: the clip covers the whole display (centre-cropped), with its own sound.
        ChromaKeyVideo(
            raw = R.raw.intro_welcome,
            muted = false,
            crop = true,
            controller = player,
            modifier = Modifier.fillMaxSize(),
        )

        // The logo comes down from above the screen and stops high above the mascot.
        val density = LocalDensity.current
        val travel = with(density) { 220.dp.toPx() }
        Image(
            painter = painterResource(R.drawable.kelime_tahti_brand_logo),
            contentDescription = "KELİME TAHTI",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 28.dp)
                .size(IntroLogoWidth, IntroLogoHeight)
                .graphicsLayer {
                    alpha = logoDrop.value
                    translationY = (logoDrop.value - 1f) * travel
                },
        )

        // After the clip: language and continue, under the mascot.
        AnimatedVisibility(
            visible = ended,
            enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6081226), Color(0xFF081226))))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 36.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Dilini seç / Choose your language",
                    color = Color.White.copy(alpha = .85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IntroLanguageCard("🇹🇷", "TÜRKÇE", "Türkçe oyna", selected == "tr", Modifier.weight(1f)) { selected = "tr" }
                    IntroLanguageCard("🇬🇧", "ENGLISH", "Play in English", selected == "en", Modifier.weight(1f)) { selected = "en" }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = selected != null,
                    onClick = { selected?.let(onContinue) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC93C),
                        contentColor = Color(0xFF3A2400),
                        disabledContainerColor = Color.White.copy(alpha = .16f),
                        disabledContentColor = Color.White.copy(alpha = .6f),
                    ),
                ) {
                    Text(
                        when (selected) {
                            "en" -> "CONTINUE  ➜"
                            "tr" -> "DEVAM ET  ➜"
                            else -> "Dil seç / Choose"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroLanguageCard(flag: String, title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0x33FFC93C) else Color.White.copy(alpha = .08f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFFFC93C) else Color.White.copy(alpha = .28f)),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(flag, fontSize = 28.sp)
            Text(title, color = if (selected) Color(0xFFFFD36B) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White.copy(alpha = .65f), fontSize = 11.sp)
        }
    }
}
