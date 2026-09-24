package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider

/** Unified Pro startup shell: language -> auth -> premium product. */
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
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(MainUi.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MainUi.Blue)
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
        PremiumUnifiedProApp(onSignedOut = { authenticated = false })
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

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        Box(Modifier.fillMaxSize()) {
            FirstRunLanguageBackdrop(Modifier.fillMaxSize())
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // The mascot flies in to welcome new players in both languages.
                Box(Modifier.fillMaxWidth().height(262.dp)) {
                    WordSiegeMascotCompanion(
                        anchors = listOf(Offset(.5f, .64f)),
                        mascotSize = 169.dp,
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
                Text(
                    text = "KELİME TAHTI",
                    color = MainUi.Text,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "WORD BOARD",
                    color = MainUi.Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Dilini seç / Choose your language",
                    color = MainUi.Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selected == "tr",
                        onClick = { choose("tr") },
                        label = { Text("TÜRKÇE", fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MainUi.BlueSoft,
                            selectedLabelColor = MainUi.Blue,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected == "tr",
                            borderColor = MainUi.Border,
                            selectedBorderColor = MainUi.Blue,
                        ),
                    )
                    FilterChip(
                        selected = selected == "en",
                        onClick = { choose("en") },
                        label = { Text("ENGLISH", fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MainUi.BlueSoft,
                            selectedLabelColor = MainUi.Blue,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected == "en",
                            borderColor = MainUi.Border,
                            selectedBorderColor = MainUi.Blue,
                        ),
                    )
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    enabled = selected != null,
                    onClick = { selected?.let(onContinue) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MainUiShape.Control,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainUi.Blue,
                        contentColor = MainUi.Surface,
                        disabledContainerColor = MainUi.Border,
                        disabledContentColor = MainUi.Muted,
                    ),
                ) {
                    Text(if (selected == "en") "CONTINUE" else "DEVAM ET", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
