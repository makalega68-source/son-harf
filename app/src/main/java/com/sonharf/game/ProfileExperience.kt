package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileExperienceScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        CompleteProfileScreen(initialTab = initialTab, onBack = onBack)
        PremiumAnalysisCenterLauncher(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}
