package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared Android-like word keyboard for Son Harf and Harf Yolu.
 *
 * Deliberately contains only gameplay keys: letters, backspace and submit. There is no emoji,
 * microphone, symbols page, language switcher, suggestion row, punctuation, clipboard or settings.
 */
@Composable
internal fun AndroidWordKeyboard(
    value: String,
    language: String,
    enabled: Boolean,
    submitEnabled: Boolean,
    maxLength: Int,
    submitLabelTr: String,
    submitLabelEn: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    actionColor: Color = PurchasedCasualUi2.Blue,
    keySound: () -> Unit = {},
    actionSound: () -> Unit = {},
) {
    val isEnglish = language.equals("en", ignoreCase = true)
    val rows = if (isEnglish) {
        listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M"),
        )
    } else {
        listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "Ğ", "Ü"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ş", "İ"),
            listOf("Z", "X", "C", "V", "B", "N", "M", "Ö", "Ç"),
        )
    }

    val keyHeight = if (compact) 34.dp else 42.dp
    val rowGap = if (compact) 4.dp else 5.dp
    val keyGap = if (compact) 2.dp else 3.dp
    val secondInset = if (compact) 6.dp else 8.dp
    val thirdInset = if (compact) 15.dp else 19.dp
    val shell = Color(0xFFE7ECF2)
    val key = Color(0xFFFFFFFF)
    val keyBorder = Color(0xFFD0D8E2)
    val keyText = Color(0xFF17243B)
    val disabledKey = Color(0xFFF1F4F7)
    val disabledText = Color(0xFF9AA6B5)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = shell,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = BorderStroke(1.dp, keyBorder),
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(shell)
                .padding(horizontal = 5.dp, vertical = if (compact) 5.dp else 7.dp),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier.fillMaxWidth().padding(
                        horizontal = when (index) {
                            1 -> secondInset
                            2 -> thirdInset
                            else -> 0.dp
                        },
                    ),
                    horizontalArrangement = Arrangement.spacedBy(keyGap),
                ) {
                    row.forEach { label ->
                        AndroidWordKey(
                            label = label,
                            enabled = enabled && value.length < maxLength,
                            modifier = Modifier.weight(1f),
                            height = keyHeight,
                            containerColor = key,
                            contentColor = keyText,
                            borderColor = keyBorder,
                            disabledContainerColor = disabledKey,
                            disabledContentColor = disabledText,
                            onClick = {
                                keySound()
                                onValueChange((value + label).take(maxLength))
                            },
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = thirdInset),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
            ) {
                AndroidWordKey(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1.1f),
                    height = keyHeight,
                    containerColor = Color(0xFFDCE3EB),
                    contentColor = keyText,
                    borderColor = Color(0xFFC2CBD7),
                    disabledContainerColor = disabledKey,
                    disabledContentColor = disabledText,
                    onClick = {
                        actionSound()
                        onValueChange(value.dropLast(1))
                    },
                )
                AndroidWordKey(
                    label = if (isEnglish) submitLabelEn else submitLabelTr,
                    enabled = enabled && submitEnabled,
                    modifier = Modifier.weight(2.9f),
                    height = keyHeight,
                    containerColor = actionColor,
                    contentColor = Color.White,
                    borderColor = actionColor.copy(alpha = .82f),
                    disabledContainerColor = actionColor.copy(alpha = .18f),
                    disabledContentColor = actionColor.copy(alpha = .52f),
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
private fun AndroidWordKey(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    height: Dp,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    disabledContainerColor: Color,
    disabledContentColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 5) 10.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
