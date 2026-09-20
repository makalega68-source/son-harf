package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared word-game keyboard for Son Harf / Harf Yolu and independent word inputs.
 * It intentionally contains only letters, backspace and the game action. System-keyboard
 * controls (?123, emoji, mic, clipboard, language, punctuation and suggestions) are excluded.
 * Purchased keyboard skins only affect presentation; geometry and available actions stay fixed.
 */
@Composable
internal fun EmbeddedWordKeyboard(
    value: String,
    language: String,
    enabled: Boolean,
    submitEnabled: Boolean = enabled,
    maxLength: Int = 20,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    submitLabel: String? = null,
    keySound: () -> Unit = { SonHarfSoundFx.typingClick() },
    actionSound: () -> Unit = { SonHarfSoundFx.tap() },
) {
    val palette = SonHarfCosmetics.keyboardPalette
    val rows = if (language.lowercase() == "en") {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P"),
            listOf("A","S","D","F","G","H","J","K","L"),
            listOf("Z","X","C","V","B","N","M"),
        )
    } else {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
            listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
            listOf("Z","X","C","V","B","N","M","Ö","Ç"),
        )
    }
    val actionText = submitLabel ?: if (maxLength == 5) sh("ONAYLA", "CONFIRM") else sh("GÖNDER", "SEND")

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, palette.border.copy(alpha = .78f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = when (index) {
                        1 -> 7.dp
                        2 -> 17.dp
                        else -> 0.dp
                    }),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    row.forEach { key ->
                        SharedKeyboardKeyButton(
                            label = key,
                            enabled = enabled && value.length < maxLength,
                            modifier = Modifier.weight(1f),
                            palette = palette,
                            onClick = {
                                keySound()
                                onValueChange((value + key).take(maxLength))
                            },
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 17.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SharedKeyboardKeyButton(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    palette = palette,
                    alt = true,
                    onClick = {
                        actionSound()
                        onValueChange(value.dropLast(1))
                    },
                )
                SharedKeyboardKeyButton(
                    label = "$actionText  →",
                    enabled = submitEnabled && value.isNotBlank(),
                    modifier = Modifier.weight(3.1f),
                    palette = palette,
                    action = true,
                    onClick = {
                        actionSound()
                        onSubmit()
                    },
                )
            }
        }
    }
}

@Composable
internal fun EmbeddedNumberKeyboard(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SonHarfCosmetics.keyboardPalette
    val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"))
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.background,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        SharedKeyboardKeyButton(
                            label = key,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            palette = palette,
                            onClick = {
                                SonHarfSoundFx.typingClick()
                                onValueChange((value + key).take(12))
                            },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SharedKeyboardKeyButton(
                    label = if (value.contains(",") || value.contains(".")) "−" else ",",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    palette = palette,
                    alt = true,
                    onClick = {
                        SonHarfSoundFx.tap()
                        if (!value.contains(",") && !value.contains(".")) onValueChange((value + ",").take(12))
                        else if (value.isBlank()) onValueChange("-")
                        else if (value.startsWith("-")) onValueChange(value.drop(1))
                        else onValueChange("-$value")
                    },
                )
                SharedKeyboardKeyButton(
                    label = "0",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    palette = palette,
                    onClick = {
                        SonHarfSoundFx.typingClick()
                        onValueChange((value + "0").take(12))
                    },
                )
                SharedKeyboardKeyButton(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    palette = palette,
                    alt = true,
                    onClick = {
                        SonHarfSoundFx.tap()
                        onValueChange(value.dropLast(1))
                    },
                )
                SharedKeyboardKeyButton(
                    label = "✓",
                    enabled = enabled && value.replace(',', '.').toDoubleOrNull() != null,
                    modifier = Modifier.weight(1f),
                    palette = palette,
                    action = true,
                    onClick = {
                        SonHarfSoundFx.tap()
                        onSubmit()
                    },
                )
            }
        }
    }
}

@Composable
private fun SharedKeyboardKeyButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    palette: WordKeyboardPalette,
    alt: Boolean = false,
    action: Boolean = false,
    onClick: () -> Unit,
) {
    val keyBackground = when {
        action -> palette.action
        alt -> palette.keyAlt
        else -> palette.key
    }
    val keyText = if (action) palette.actionText else palette.text
    val keyBorder = when {
        action -> palette.action.copy(alpha = .86f)
        alt -> palette.secondaryBorder.copy(alpha = .62f)
        else -> palette.border
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = keyBackground,
            contentColor = keyText,
            disabledContainerColor = palette.keyAlt.copy(alpha = .55f),
            disabledContentColor = palette.text.copy(alpha = .42f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, keyBorder),
    ) {
        Text(
            label,
            fontSize = if (label.length > 5) 10.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
