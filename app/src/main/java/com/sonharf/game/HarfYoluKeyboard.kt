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

private object HarfYoluKeyboardUi {
    val Background = Color(0xFFEAF8FC)
    val Key = Color(0xFFFFFFFF)
    val KeyAlt = Color(0xFFDDF5FA)
    val Text = Color(0xFF123A4A)
    val Border = Color(0xFF9AD3E0)
    val SecondaryBorder = Color(0xFF69C5D5)
    val Action = Color(0xFF22BFC4)
    val ActionText = Color.White
}

/**
 * Harf Yolu'na özel kompakt klavye overload'u.
 * Ortak EmbeddedGameKeyboard dosyasını ve diğer oyunların klavye davranışını değiştirmez.
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
    val keyHeight = if (compact) 33.dp else 38.dp
    val rowGap = if (compact) 3.dp else 5.dp
    val keyGap = if (compact) 2.dp else 3.dp
    val secondInset = if (compact) 5.dp else 7.dp
    val thirdInset = if (compact) 13.dp else 17.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HarfYoluKeyboardUi.Background,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = BorderStroke(1.dp, HarfYoluKeyboardUi.Border),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = if (compact) 4.dp else 6.dp),
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
                horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
            ) {
                HarfYoluKeyButton(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    height = keyHeight,
                    alt = true,
                    onClick = {
                        actionSound()
                        onValueChange(value.dropLast(1))
                    },
                )
                HarfYoluKeyButton(
                    label = "TEMİZLE",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1.35f),
                    height = keyHeight,
                    alt = true,
                    onClick = {
                        actionSound()
                        onValueChange("")
                    },
                )
                HarfYoluKeyButton(
                    label = "GÖNDER  ➤",
                    enabled = submitEnabled && value.isNotBlank(),
                    modifier = Modifier.weight(2.15f),
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
            disabledContainerColor = if (alt) {
                HarfYoluKeyboardUi.KeyAlt.copy(alpha = .58f)
            } else {
                HarfYoluKeyboardUi.Key.copy(alpha = .62f)
            },
            disabledContentColor = HarfYoluKeyboardUi.Text.copy(alpha = .34f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            when {
                action -> HarfYoluKeyboardUi.Action.copy(alpha = .88f)
                alt -> HarfYoluKeyboardUi.SecondaryBorder.copy(alpha = .72f)
                else -> HarfYoluKeyboardUi.Border
            },
        ),
    ) {
        Text(
            label,
            fontSize = if (label.length > 4) 9.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
