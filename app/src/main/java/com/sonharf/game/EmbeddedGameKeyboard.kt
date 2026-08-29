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

private val KeyboardBg = Color(0xFF1F2027)
private val KeyboardKey = Color(0xFF3A3B44)
private val KeyboardKeyAlt = Color(0xFF55586B)
private val KeyboardText = Color(0xFFF4F6FB)
private val KeyboardAction = Color(0xFF1769E0)

@Composable
internal fun EmbeddedWordKeyboard(
    value: String,
    language: String,
    enabled: Boolean,
    maxLength: Int = 20,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = if (language.lowercase() == "en") {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P"),
            listOf("A","S","D","F","G","H","J","K","L"),
            listOf("Z","X","C","V","B","N","M"),
        )
    } else {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P"),
            listOf("Ğ","Ü","A","S","D","F","G","H","J","K"),
            listOf("L","Ş","İ","Z","X","C","V","B","N","M"),
            listOf("Ö","Ç","⌫","✓"),
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = KeyboardBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { key ->
                        when (key) {
                            "⌫" -> KeyboardKeyButton(
                                label = key,
                                enabled = enabled && value.isNotEmpty(),
                                modifier = Modifier.weight(1.6f),
                                alt = true,
                                onClick = {
                                    SonHarfSoundFx.tap()
                                    onValueChange(value.dropLast(1))
                                },
                            )
                            "✓" -> KeyboardKeyButton(
                                label = key,
                                enabled = enabled && value.isNotBlank(),
                                modifier = Modifier.weight(1.6f),
                                action = true,
                                onClick = {
                                    SonHarfSoundFx.tap()
                                    onSubmit()
                                },
                            )
                            else -> KeyboardKeyButton(
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
        modifier = modifier.height(34.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                action -> KeyboardAction
                alt -> KeyboardKeyAlt
                else -> KeyboardKey
            },
            contentColor = KeyboardText,
            disabledContainerColor = if (alt) KeyboardKeyAlt.copy(alpha = .45f) else KeyboardKey.copy(alpha = .45f),
            disabledContentColor = KeyboardText.copy(alpha = .48f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = .05f)),
    ) {
        Text(
            label,
            fontSize = if (label.length > 2) 8.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
