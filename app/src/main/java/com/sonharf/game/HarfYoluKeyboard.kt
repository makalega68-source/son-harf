package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/** Harf Yolu keyboard aligned with the professional app palette. */
private object HarfYoluKeyboardUi {
    val Background = GameColors.ElevatedBackground
    val Key = GameColors.PrimarySurface
    val KeyAlt = GameColors.SecondarySurface
    val Text = GameColors.TextPrimary
    val Border = GameColors.Border
    val AltBorder = GameColors.Lavender.copy(alpha = .58f)
    val Action = GameColors.PlayGreen
    val ActionPressed = GameColors.PlayGreenDeep
    val ActionText = Color.White
    val Disabled = GameColors.Disabled
    val DisabledText = GameColors.DisabledContent
}

/**
 * Harf Yolu-specific compact keyboard overload.
 * It intentionally exposes only letters, Backspace and Send; system-keyboard extras and a
 * separate clear key are not part of this game input surface.
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
    val isEnglish = language.equals("en", ignoreCase = true)
    val rows = if (isEnglish) {
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
    val keyHeight = if (compact) 34.dp else 40.dp
    val rowGap = if (compact) 4.dp else 5.dp
    val keyGap = if (compact) 2.dp else 3.dp
    val secondInset = if (compact) 5.dp else 7.dp
    val thirdInset = if (compact) 13.dp else 17.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HarfYoluKeyboardUi.Background,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = BorderStroke(1.dp, HarfYoluKeyboardUi.Border),
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = if (compact) 5.dp else 7.dp),
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
                        HarfYoluKeyButton(
                            label = key,
                            enabled = enabled && value.length < maxLength,
                            modifier = Modifier.weight(1f),
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
                horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
            ) {
                HarfYoluKeyButton(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1.15f),
                    height = keyHeight,
                    alt = true,
                    onClick = {
                        actionSound()
                        onValueChange(value.dropLast(1))
                    },
                )
                HarfYoluKeyButton(
                    label = if (isEnglish) "SEND" else "GÖNDER",
                    enabled = submitEnabled && value.isNotBlank(),
                    modifier = Modifier.weight(2.85f),
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
private fun HarfYoluKeyButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
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
                action -> HarfYoluKeyboardUi.Action
                alt -> HarfYoluKeyboardUi.KeyAlt
                else -> HarfYoluKeyboardUi.Key
            },
            contentColor = if (action) HarfYoluKeyboardUi.ActionText else HarfYoluKeyboardUi.Text,
            disabledContainerColor = HarfYoluKeyboardUi.Disabled,
            disabledContentColor = HarfYoluKeyboardUi.DisabledText,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            when {
                action -> HarfYoluKeyboardUi.ActionPressed
                alt -> HarfYoluKeyboardUi.AltBorder
                else -> HarfYoluKeyboardUi.Border
            },
        ),
    ) {
        Text(
            label,
            fontSize = if (label.length > 4) 10.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
