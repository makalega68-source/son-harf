package com.sonharf.game

import androidx.compose.foundation.BorderStroke
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
        kotlinx.coroutines.delay(9_000L)
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

    GameTheme {
        Surface(Modifier.fillMaxSize(), color = GameColors.AppBackground) {
            Box(Modifier.fillMaxSize()) {
                FirstRunLanguageBackdrop(Modifier.fillMaxSize())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // First-run welcome is one of the mascot's few moments; it is not a persistent companion.
                    Box(Modifier.fillMaxWidth().height(196.dp)) {
                        WordSiegeMascotCompanion(
                            anchors = listOf(Offset(.5f, .62f)),
                            mascotSize = 128.dp,
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
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                        contentDescription = "Kelime Kuşatması / Word Siege",
                        modifier = Modifier.fillMaxWidth(.6f).height(92.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        text = "KELİME KUŞATMASI / WORD SIEGE",
                        color = GameColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Dilini seç / Choose your language",
                        color = GameColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FirstRunLanguageCard(
                            flag = "🇹🇷",
                            title = "TÜRKÇE",
                            subtitle = "Türkçe oyna",
                            selected = selected == "tr",
                            onClick = { choose("tr") },
                            modifier = Modifier.weight(1f),
                        )
                        FirstRunLanguageCard(
                            flag = "🇬🇧",
                            title = "ENGLISH",
                            subtitle = "Play in English",
                            selected = selected == "en",
                            onClick = { choose("en") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        enabled = selected != null,
                        onClick = { selected?.let(onContinue) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = GameShapes.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.PrimaryBlue,
                            contentColor = GameColors.TextPrimary,
                            disabledContainerColor = GameColors.Disabled,
                            disabledContentColor = GameColors.DisabledContent,
                        ),
                    ) {
                        Text(
                            if (selected == "en") "CONTINUE" else "DEVAM ET",
                            fontWeight = FontWeight.Black,
                        )
                    }
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
    Surface(
        onClick = onClick,
        modifier = modifier.height(118.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) GameColors.PrimaryBlue.copy(alpha = .16f) else GameColors.PrimarySurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) GameColors.PrimaryBlue else GameColors.Border),
        shadowElevation = if (selected) 8.dp else 2.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(flag, fontSize = 34.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, color = if (selected) GameColors.PrimaryBlue else GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = GameColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (selected) Text("✓", color = GameColors.PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}
