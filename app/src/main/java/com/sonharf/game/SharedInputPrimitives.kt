package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared custom word keyboard for Son Harf and Harf Yolu.
 * Only letters, backspace and the game action are rendered; no system-keyboard extras exist.
 * Visual chrome comes from the real purchased Casual Game UI #02 assets.
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

    PurchasedPanel(
        modifier = modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
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
                        PurchasedKeyboardKey(
                            label = key,
                            enabled = enabled && value.length < maxLength,
                            modifier = Modifier.weight(1f),
                            height = keyHeight,
                            asset = PurchasedUiAsset.BUTTON_BLUE,
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PurchasedKeyboardKey(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    height = keyHeight + 4.dp,
                    asset = PurchasedUiAsset.BUTTON_PURPLE,
                    onClick = {
                        actionSound()
                        onValueChange(value.dropLast(1))
                    },
                )
                PurchasedButton(
                    text = actionText,
                    enabled = submitEnabled && value.isNotBlank(),
                    onClick = {
                        actionSound()
                        onSubmit()
                    },
                    modifier = Modifier.weight(3.1f).height(keyHeight + 8.dp),
                    style = PurchasedButtonStyle.PRIMARY,
                    leadingAsset = PurchasedUiAsset.ICON_CHECK,
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
    val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"))
    PurchasedPanel(
        modifier = modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        PurchasedKeyboardKey(
                            label = key,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                            asset = PurchasedUiAsset.BUTTON_BLUE,
                            onClick = {
                                SonHarfSoundFx.typingClick()
                                onValueChange((value + key).take(12))
                            },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PurchasedKeyboardKey(
                    label = if (value.contains(",") || value.contains(".")) "−" else ",",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                    asset = PurchasedUiAsset.BUTTON_ORANGE,
                    onClick = {
                        SonHarfSoundFx.tap()
                        if (!value.contains(",") && !value.contains(".")) onValueChange((value + ",").take(12))
                        else if (value.isBlank()) onValueChange("-")
                        else if (value.startsWith("-")) onValueChange(value.drop(1))
                        else onValueChange("-$value")
                    },
                )
                PurchasedKeyboardKey(
                    label = "0",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                    asset = PurchasedUiAsset.BUTTON_BLUE,
                    onClick = {
                        SonHarfSoundFx.typingClick()
                        onValueChange((value + "0").take(12))
                    },
                )
                PurchasedKeyboardKey(
                    label = "⌫",
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                    asset = PurchasedUiAsset.BUTTON_PURPLE,
                    onClick = {
                        SonHarfSoundFx.tap()
                        onValueChange(value.dropLast(1))
                    },
                )
                PurchasedKeyboardKey(
                    label = "✓",
                    enabled = enabled && value.replace(',', '.').toDoubleOrNull() != null,
                    modifier = Modifier.weight(1f),
                    height = 42.dp,
                    asset = PurchasedUiAsset.BUTTON_GREEN,
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
private fun PurchasedKeyboardKey(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    height: Dp,
    asset: PurchasedUiAsset,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else .42f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PurchasedAsset(asset, Modifier.matchParentSize())
        Text(
            text = label,
            color = Color.White,
            fontSize = if (label.length > 4) 10.sp else 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}
