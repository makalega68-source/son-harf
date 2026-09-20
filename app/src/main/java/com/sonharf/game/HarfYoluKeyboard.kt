package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Harf Yolu compatibility overload.
 *
 * Harf Yolu and Son Harf now share the same game-only keyboard primitive so key geometry,
 * accessibility and Turkish layout cannot drift apart. The compact flag is retained only for
 * source compatibility; the shared keyboard already sizes itself for gameplay.
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
    EmbeddedWordKeyboard(
        value = value,
        language = language,
        enabled = enabled,
        submitEnabled = submitEnabled,
        maxLength = maxLength,
        onValueChange = onValueChange,
        onSubmit = onSubmit,
        modifier = Modifier,
        submitLabel = sh("ONAYLA", "CONFIRM"),
        keySound = keySound,
        actionSound = actionSound,
    )
}
