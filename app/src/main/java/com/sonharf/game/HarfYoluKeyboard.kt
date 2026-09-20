package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Harf Yolu wrapper around the shared Android-like word keyboard.
 * The game deliberately exposes no non-gameplay keys: only letters, backspace and confirmation.
 */
@Composable
internal fun EmbeddedWordKeyboard(
    value: String,
    language: String,
    enabled: Boolean,
    submitEnabled: Boolean,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    compact: Boolean,
    keySound: () -> Unit,
    actionSound: () -> Unit,
) {
    AndroidWordKeyboard(
        value = value,
        language = language,
        enabled = enabled,
        submitEnabled = submitEnabled,
        maxLength = maxLength,
        submitLabelTr = "ONAYLA",
        submitLabelEn = "CONFIRM",
        onValueChange = onValueChange,
        onSubmit = onSubmit,
        compact = compact,
        actionColor = Color(0xFF278DC3),
        keySound = keySound,
        actionSound = actionSound,
    )
}
