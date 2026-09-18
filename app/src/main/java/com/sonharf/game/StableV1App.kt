package com.sonharf.game

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider

/** Premium startup shell: language -> auth -> approved Canva product shell. */
@Composable
fun StableV1App() {
    val context = LocalContext.current
    remember(context) {
        SonHarfCosmetics.restore(context)
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PremiumScreenBackground(Modifier.matchParentSize())
            CircularProgressIndicator(color = MainUi.Blue)
        }
        return
    }

    if (!authenticated) {
        CompactAuthGate { authenticated = true }
        return
    }

    PremiumCanvaAppV2(onSignedOut = { authenticated = false })
}

/** Keeps the existing authentication flow intact while preserving phone-scale ergonomics. */
@Composable
private fun CompactAuthGate(onAuthenticated: () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * 0.94f,
            fontScale = density.fontScale,
        ),
    ) {
        RequiredAuthGate(onAuthenticated)
    }
}

@Composable
private fun FirstRunLanguageScreen(onContinue: (String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        Box(Modifier.fillMaxSize()) {
            PremiumScreenBackground(Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PremiumAccentPill(text = "KELİME KUŞATMASI", color = MainUi.Purple)
                    Spacer(Modifier.height(18.dp))
                    SonHarfOfficialLogo(modifier = Modifier.fillMaxWidth(.86f).height(150.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Dilini seç / Choose your language",
                        color = MainUi.Text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Kelime oyunları, taktik alan savaşı ve sosyal rekabet tek yerde.",
                        color = MainUi.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth(), accent = MainUi.Blue) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = selected == "tr",
                                onClick = { selected = "tr" },
                                label = { Text("TÜRKÇE", fontWeight = FontWeight.Black) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MainUi.SurfaceSoft,
                                    labelColor = MainUi.Text,
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
                                onClick = { selected = "en" },
                                label = { Text("ENGLISH", fontWeight = FontWeight.Black) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MainUi.SurfaceSoft,
                                    labelColor = MainUi.Text,
                                    selectedContainerColor = MainUi.Purple.copy(alpha = .10f),
                                    selectedLabelColor = MainUi.Purple,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected == "en",
                                    borderColor = MainUi.Border,
                                    selectedBorderColor = MainUi.Purple,
                                ),
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        PremiumPrimaryButton(
                            text = if (selected == "en") "CONTINUE" else "DEVAM ET",
                            onClick = { selected?.let(onContinue) },
                            enabled = selected != null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
