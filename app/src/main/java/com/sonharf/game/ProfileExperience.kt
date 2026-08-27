package com.sonharf.game

import androidx.compose.runtime.Composable

@Composable
fun ProfileExperienceScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
) = CompleteProfileScreen(initialTab = initialTab, onBack = onBack)
