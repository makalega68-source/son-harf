package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KeyboardBg = Color(0xFF070A18)
private val KeyboardKey = Color(0xFF121833)
private val KeyboardKeyAlt = Color(0xFF1C2347)
private val KeyboardText = Color(0xFFF7F8FF)
private val KeyboardAction = Color(0xFFFFB31A)
private val KeyboardBlue = Color(0xFF2188FF)
private val KeyboardViolet = Color(0xFF8A5CFF)

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
) {
    // Game-owned keyboard: the Android system IME never needs to open during a match.
    // Turkish layout mirrors the familiar Turkish-Q ordering and remains fixed on screen.
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = KeyboardBg,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = BorderStroke(1.dp, KeyboardViolet.copy(alpha = .30f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = when {
                            index == 1 -> 7.dp
                            index == 2 -> 17.dp
                            else -> 0.dp
                        }),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    row.forEach { key ->
                        KeyboardKeyButton(
                            label = key,
                            enabled = enabled && value.length < maxLength,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SonHarfSoundFx.typingClick()
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
                KeyboardKeyButton(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    alt = true,
                    onClick = {
                        SonHarfSoundFx.tap()
                        onValueChange(value.dropLast(1))
                    },
                )
                KeyboardKeyButton(
                    label = "TEMİZLE",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1.35f),
                    alt = true,
                    onClick = {
                        SonHarfSoundFx.tap()
                        onValueChange("")
                    },
                )
                KeyboardKeyButton(
                    label = "GÖNDER  ➤",
                    enabled = submitEnabled && value.isNotBlank(),
                    modifier = Modifier.weight(2.15f),
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
internal fun EmbeddedNumberKeyboard(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1","2","3"),
        listOf("4","5","6"),
        listOf("7","8","9"),
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = KeyboardBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        KeyboardKeyButton(
                            label = key,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SonHarfSoundFx.typingClick()
                                onValueChange((value + key).take(12))
                            },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyboardKeyButton(
                    label = if (value.contains(",") || value.contains(".")) "−" else ",",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    alt = true,
                    onClick = {
                        SonHarfSoundFx.tap()
                        if (!value.contains(",") && !value.contains(".")) onValueChange((value + ",").take(12))
                        else if (value.isBlank()) onValueChange("-")
                        else if (value.startsWith("-")) onValueChange(value.drop(1))
                        else onValueChange("-$value")
                    },
                )
                KeyboardKeyButton(
                    label = "0",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        SonHarfSoundFx.typingClick()
                        onValueChange((value + "0").take(12))
                    },
                )
                KeyboardKeyButton(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    alt = true,
                    onClick = {
                        SonHarfSoundFx.tap()
                        onValueChange(value.dropLast(1))
                    },
                )
                KeyboardKeyButton(
                    label = "✓",
                    enabled = enabled && value.replace(',', '.').toDoubleOrNull() != null,
                    modifier = Modifier.weight(1f),
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
private fun KeyboardKeyButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    alt: Boolean = false,
    action: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(38.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                action -> KeyboardAction
                alt -> KeyboardKeyAlt
                else -> KeyboardKey
            },
            contentColor = if (action) Color(0xFF241300) else KeyboardText,
            disabledContainerColor = if (alt) KeyboardKeyAlt.copy(alpha = .55f) else KeyboardKey.copy(alpha = .55f),
            disabledContentColor = KeyboardText.copy(alpha = .42f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            when {
                action -> KeyboardAction.copy(alpha = .82f)
                alt -> KeyboardBlue.copy(alpha = .32f)
                else -> KeyboardViolet.copy(alpha = .24f)
            },
        ),
    ) {
        Text(
            label,
            fontSize = if (label.length > 4) 10.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
