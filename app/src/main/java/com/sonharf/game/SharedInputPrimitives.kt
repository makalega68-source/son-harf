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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared custom word keyboard for Son Harf-compatible word entry and Harf Yolu.
 * It intentionally contains only letters, backspace and the game action. System-keyboard extras
 * and a separate clear key are deliberately excluded.
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
    keyHeight: Dp = 38.dp,
    rowGap: Dp = 5.dp,
    keyGap: Dp = 3.dp,
    secondInset: Dp = 7.dp,
    thirdInset: Dp = 17.dp,
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
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = when (index) {
                        1 -> secondInset
                        2 -> thirdInset
                        else -> 0.dp
                    }),
                    horizontalArrangement = Arrangement.spacedBy(keyGap),
                ) {
                    row.forEach { key ->
                        SharedKeyboardKeyButton(
                            label = key,
                            enabled = enabled && value.length < maxLength,
                            modifier = Modifier.weight(1f),
                            palette = palette,
                            height = keyHeight,
                            onClick = {
                                keySound()
                                onValueChange((value + key).take(maxLength))
                            },
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = thirdInset),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SharedKeyboardKeyButton(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    palette = palette,
                    height = keyHeight,
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
                    height = keyHeight,
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
                            height = 38.dp,
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
                    height = 38.dp,
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
                    height = 38.dp,
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
                    height = 38.dp,
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
                    height = 38.dp,
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
    height: Dp,
    alt: Boolean = false,
    action: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                action -> palette.action
                alt -> palette.keyAlt
                else -> palette.key
            },
            contentColor = if (action) palette.actionText else palette.text,
            disabledContainerColor = if (alt) palette.keyAlt.copy(alpha = .55f) else palette.key.copy(alpha = .55f),
            disabledContentColor = palette.text.copy(alpha = .42f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            when {
                action -> palette.action.copy(alpha = .82f)
                alt -> palette.secondaryBorder.copy(alpha = .55f)
                else -> palette.border
            },
        ),
    ) {
        Text(label, fontSize = if (label.length > 4) 10.sp else 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
