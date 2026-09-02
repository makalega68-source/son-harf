package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider

/**
 * Stable product shell. First-install language selection is resolved before any
 * authentication lookup so a fresh install always has a deterministic route.
 */
@Composable
fun StableV1App() {
    val context = LocalContext.current
    var languageChosen by remember { mutableStateOf(FirstRunLanguagePreferences.isComplete(context)) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }

    if (!languageChosen) {
        FirstRunLanguageScreen { language ->
            FirstRunLanguagePreferences.complete(context, language)
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
        Box(
            Modifier.fillMaxSize().background(MainUi.Background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MainUi.Blue)
        }
        return
    }

    if (!authenticated) {
        RequiredAuthGate { authenticated = true }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MainUi.Background),
    ) {
        MonsterExperienceApp(onSignedOut = { authenticated = false })
    }
}

@Composable
private fun FirstRunLanguageScreen(onContinue: (String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SonHarfBrandLogo(size = 82.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "SON HARF",
                color = MainUi.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dilini seç / Choose your language",
                color = MainUi.Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))

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
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainUi.Blue,
                    contentColor = MainUi.Surface,
                    disabledContainerColor = MainUi.Border,
                    disabledContentColor = MainUi.Muted,
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
