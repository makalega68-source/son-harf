package com.sonharf.game

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
import androidx.compose.ui.layout.ContentScale
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

    ProfessionalUnifiedApp(onSignedOut = { authenticated = false })
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
                    Image(
                        painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                        contentDescription = "Kelime Kuşatması / Word Siege",
                        modifier = Modifier.fillMaxWidth(.72f).height(132.dp),
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilterChip(
                            selected = selected == "tr",
                            onClick = { selected = "tr" },
                            label = { Text("TÜRKÇE", fontWeight = FontWeight.Black) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = GameColors.PrimarySurface,
                                labelColor = GameColors.TextSecondary,
                                selectedContainerColor = GameColors.PrimaryBlue.copy(alpha = .16f),
                                selectedLabelColor = GameColors.PrimaryBlue,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected == "tr",
                                borderColor = GameColors.Border,
                                selectedBorderColor = GameColors.PrimaryBlue,
                            ),
                        )
                        FilterChip(
                            selected = selected == "en",
                            onClick = { selected = "en" },
                            label = { Text("ENGLISH", fontWeight = FontWeight.Black) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = GameColors.PrimarySurface,
                                labelColor = GameColors.TextSecondary,
                                selectedContainerColor = GameColors.PrimaryBlue.copy(alpha = .16f),
                                selectedLabelColor = GameColors.PrimaryBlue,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected == "en",
                                borderColor = GameColors.Border,
                                selectedBorderColor = GameColors.PrimaryBlue,
                            ),
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
