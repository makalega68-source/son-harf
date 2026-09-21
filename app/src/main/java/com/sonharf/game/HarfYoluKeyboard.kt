package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Harf Yolu compatibility overload. Rendering is delegated to the shared game keyboard so Son Harf
 * compatible word entry and Harf Yolu no longer maintain separate key layouts.
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
        keyHeight = if (compact) 33.dp else 38.dp,
        rowGap = if (compact) 3.dp else 5.dp,
        keyGap = if (compact) 2.dp else 3.dp,
        secondInset = if (compact) 5.dp else 7.dp,
        thirdInset = if (compact) 13.dp else 17.dp,
        keySound = keySound,
        actionSound = actionSound,
    )
}
