package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider

/** Professional startup shell: language -> auth -> product. */
@Composable
fun StableV1App() {
    val context = LocalContext.current
    remember(context) {
        SonHarfCosmetics.restore(context)
        WordSiegeMascotOwnership.restore(context)
        true
    }
    var languageChosen by remember { mutableStateOf(FirstRunLanguagePreferences.isComplete(context)) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }

    if (!languageChosen) {
        FirstRunLanguageScreen { language ->
            FirstRunLanguagePreferences.complete(context, language)
            SonHarfUiState.language = language
            languageChosen = true
        }
        return
    }

    LaunchedEffect(languageChosen) {
        if (!languageChosen) return@LaunchedEffect
        // The stored session loads asynchronously: wait for it (and silently re-login with the
        // remembered credentials if needed) so a signed-in player never sees the email screen again.
        authenticated = SupabaseProvider.configured && (hasVerifiedMembershipSession() || restoreMembershipSession(context))
        authChecked = true
    }

    if (!authChecked) {
        GameTheme {
            Box(
                Modifier.fillMaxSize().background(GameColors.AppBackground),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GameColors.PrimaryBlue)
            }
        }
        return
    }

    if (!authenticated) {
        CompactAuthGate { authenticated = true }
        return
    }

    // Players who skipped the language screen (e.g. an update over an older install) still get
    // the classic mascot's welcome once, on their first entry.
    var mascotWelcomePending by remember { mutableStateOf(!FirstRunLanguagePreferences.mascotWelcomeSeen(context)) }
    Box(Modifier.fillMaxSize()) {
        ProfessionalUnifiedApp(onSignedOut = { authenticated = false })
        if (mascotWelcomePending) {
            MascotWelcomeOverlay(onDone = {
                FirstRunLanguagePreferences.markMascotWelcomeSeen(context)
                mascotWelcomePending = false
            })
        }
    }
}

/** A one-time greeting from the classic mascot over the home screen; tap anywhere to continue. */
@Composable
private fun MascotWelcomeOverlay(onDone: () -> Unit) {
    val english = SonHarfUiState.language == "en"
    LaunchedEffect(Unit) {
        // It says hello, does its little show and then lets the player in by itself.
        kotlinx.coroutines.delay(10_500L)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC0B1530))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(320.dp)) {
                WordSiegeMascotCompanion(
                    anchors = listOf(Offset(.5f, .6f)),
                    mascotSize = 169.dp,
                    moveId = null,
                    lastMoveMine = false,
                    playerTurn = false,
                    modifier = Modifier.matchParentSize(),
                    greet = false,
                    greeting = if (english) "Hi! I'm Obi 👋\nWelcome to Word Board!" else "Merhaba! Ben Obi 👋\nKelime Tahtı'na hoş geldin!",
                    requireOwnership = false,
                    forcedSkin = WordSiegeMascotSkin.ORB,
                    grandEntrance = true,
                )
            }
            Text(
                text = if (english) "Tap to start" else "Başlamak için dokun",
                color = Color.White.copy(alpha = .78f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Keeps the existing authentication flow intact while making the oversized entry controls
 * slightly more compact on phones. The language selector is intentionally unaffected.
 */
@Composable
private fun CompactAuthGate(onAuthenticated: () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * 0.94f,
            fontScale = density.fontScale,
        )
    ) {
        RequiredAuthGate(onAuthenticated)
    }
}

@Composable
private fun FirstRunLanguageScreen(onContinue: (String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    var mascotAnnouncement by remember { mutableStateOf<Pair<Int, String>?>(null) }
    fun choose(language: String) {
        selected = language
        val text = if (language == "en") "Great choice! Let's play ✨" else "Harika seçim! Hadi oynayalım ✨"
        mascotAnnouncement = (mascotAnnouncement?.first ?: 0) + 1 to text
    }

    // Staged entrance: the mascot drops in first, then the brand, the cards and the button.
    val brand = remember { Animatable(0f) }
    val cards = remember { Animatable(0f) }
    val cta = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(700L)
        brand.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        cards.animateTo(1f, spring(dampingRatio = .62f, stiffness = 220f))
        cta.animateTo(1f, tween(420))
    }
    val glow = rememberInfiniteTransition(label = "first-run-glow")
    val halo by glow.animateFloat(
        initialValue = .75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "halo",
    )

    GameTheme {
        Surface(Modifier.fillMaxSize(), color = GameColors.AppBackground) {
            Box(Modifier.fillMaxSize()) {
                FirstRunLanguageBackdrop(Modifier.fillMaxSize())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // The welcome mascot makes a grand entrance with its own intro animation.
                    Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(210.dp)
                                .graphicsLayer { scaleX = halo; scaleY = halo }
                                .background(
                                    Brush.radialGradient(
                                        listOf(GameColors.PrimaryBlue.copy(alpha = .30f), GameColors.Lavender.copy(alpha = .12f), Color.Transparent),
                                    ),
                                    CircleShape,
                                ),
                        )
                        WordSiegeMascotCompanion(
                            anchors = listOf(Offset(.5f, .58f)),
                            mascotSize = 150.dp,
                            moveId = null,
                            lastMoveMine = false,
                            playerTurn = false,
                            modifier = Modifier.matchParentSize(),
                            greet = false,
                            greeting = "Merhaba! Ben Obi 👋\nHi! I'm Obi, welcome!",
                            announcement = mascotAnnouncement,
                            // Everyone meets the classic mascot here; elsewhere it must be purchased.
                            requireOwnership = false,
                            forcedSkin = WordSiegeMascotSkin.ORB,
                            grandEntrance = true,
                        )
                    }
                    Column(
                        Modifier.graphicsLayer {
                            alpha = brand.value
                            translationY = (1f - brand.value) * 40f
                            scaleX = .92f + .08f * brand.value
                            scaleY = .92f + .08f * brand.value
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                            contentDescription = "Kelime Kuşatması / Word Siege",
                            modifier = Modifier.fillMaxWidth(.58f).height(86.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            text = "KELİME KUŞATMASI / WORD SIEGE",
                            color = GameColors.TextPrimary,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = .5.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = GameShapes.Pill, color = GameColors.PrimaryBlue.copy(alpha = .12f)) {
                            Text(
                                text = "Dilini seç / Choose your language",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = GameColors.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FirstRunLanguageCard(
                            flag = "🇹🇷",
                            title = "TÜRKÇE",
                            subtitle = "Türkçe oyna",
                            selected = selected == "tr",
                            onClick = { choose("tr") },
                            modifier = Modifier.weight(1f).graphicsLayer {
                                alpha = cards.value.coerceIn(0f, 1f)
                                translationX = (1f - cards.value) * -160f
                            },
                        )
                        FirstRunLanguageCard(
                            flag = "🇬🇧",
                            title = "ENGLISH",
                            subtitle = "Play in English",
                            selected = selected == "en",
                            onClick = { choose("en") },
                            modifier = Modifier.weight(1f).graphicsLayer {
                                alpha = cards.value.coerceIn(0f, 1f)
                                translationX = (1f - cards.value) * 160f
                            },
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    FirstRunContinueButton(
                        enabled = selected != null,
                        label = when (selected) {
                            "en" -> "CONTINUE  ➜"
                            "tr" -> "DEVAM ET  ➜"
                            else -> "Dil seç / Choose"
                        },
                        onClick = { selected?.let(onContinue) },
                        modifier = Modifier.graphicsLayer { alpha = cta.value },
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstRunLanguageCard(
    flag: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1f, spring(dampingRatio = .5f, stiffness = 400f), label = "card-scale")
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(132.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = shape,
        color = GameColors.PrimarySurface,
        border = if (selected) {
            BorderStroke(2.dp, Brush.linearGradient(listOf(GameColors.PrimaryBlue, GameColors.Lavender)))
        } else {
            BorderStroke(1.dp, GameColors.Border)
        },
        shadowElevation = if (selected) 12.dp else 3.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (selected) {
                        Brush.verticalGradient(listOf(GameColors.PrimaryBlue.copy(alpha = .20f), GameColors.Lavender.copy(alpha = .10f)))
                    } else {
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = .06f), Color.Transparent))
                    },
                ),
        ) {
            Column(
                Modifier.fillMaxSize().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(flag, fontSize = 40.sp)
                Spacer(Modifier.height(6.dp))
                Text(title, color = if (selected) GameColors.PrimaryBlue else GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(subtitle, color = GameColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (selected) {
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    GameBadgeIcon(Icons.Rounded.Check, GameColors.PlayGreen, size = 24.dp)
                }
            }
        }
    }
}

/** Gradient call to action with a slow light sweep once a language is chosen. */
@Composable
private fun FirstRunContinueButton(enabled: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = "cta-sweep")
    val shine by sweep.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing)),
        label = "shine",
    )
    val shape = GameShapes.Medium
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (enabled) 10.dp else 0.dp, shape, spotColor = GameColors.PrimaryBlue)
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(GameColors.PrimaryBlue, GameColors.TacticalTurquoise, GameColors.Lavender))
                else Brush.horizontalGradient(listOf(GameColors.Disabled, GameColors.Disabled)),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            0f to Color.Transparent,
                            .5f to Color.White.copy(alpha = .35f),
                            1f to Color.Transparent,
                            start = Offset(shine * 600f, 0f),
                            end = Offset(shine * 600f + 220f, 160f),
                        ),
                    ),
            )
        }
        Text(label, color = if (enabled) Color.White else GameColors.DisabledContent, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.sp)
    }
}
