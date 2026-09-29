package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The board's own bonus language, shared by the online and practice boards:
 *  - Harf Gücü / Harf Gücü+ (letter boosts): teal cut-corner chip with a ◆ mark.
 *  - Kelime Akımı / Kelime Akımı+ (word surges): amber pill with a ≈ mark.
 *  - Başlangıç Mührü (centre): navy disc with a gold ring and a ✦ mark.
 *  - Sürpriz Ödül: a plain "+25".
 * Only the look changes; the persisted cell codes (2H, 3H, 2K, 3K, 4K, 3Y) and scoring stay the same.
 */
internal object WordSiegeBonusStyle {
    val SealGold = Color(0xFFE6BE55)
    private val LetterBoostInk = Color(0xFF16776C)
    private val WordSurgeInk = Color(0xFFA25A06)

    fun glyph(code: String?): String = when (code) {
        "2H" -> "◆"
        "3H" -> "◆+"
        "2K" -> "≈"
        "3K" -> "≈+"
        WordSiegeBoardSpec.CenterBonus -> "✦"
        else -> ""
    }

    fun shape(code: String?): Shape = when (code) {
        "2H", "3H" -> CutCornerShape(6.dp)
        "2K", "3K" -> RoundedCornerShape(50)
        WordSiegeBoardSpec.CenterBonus -> CircleShape
        else -> RoundedCornerShape(4.dp)
    }

    /** Ink for the mark. On the wooden themes the palette's label colour keeps the contrast. */
    fun ink(code: String?, themeLabel: Color): Color = when {
        code == WordSiegeBoardSpec.CenterBonus -> SealGold
        WordSiegeWalnutIvory.enabled -> themeLabel
        code == "2H" || code == "3H" -> LetterBoostInk
        code == "2K" || code == "3K" -> WordSurgeInk
        else -> themeLabel
    }
}

@Composable
internal fun WordSiegeBonusMark(
    code: String,
    label: String,
    overview: Boolean,
    themeLabel: Color,
    glyphSize: TextUnit,
) {
    val glyph = WordSiegeBonusStyle.glyph(code)
    val ink = WordSiegeBonusStyle.ink(code, themeLabel)
    if (glyph.isEmpty()) {
        Text(label, color = ink, fontSize = glyphSize, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        return
    }
    val seal = code == WordSiegeBoardSpec.CenterBonus
    val strong = code == "3H" || code == "3K" || seal
    val shape = WordSiegeBonusStyle.shape(code)
    // The seal keeps its word short inside the cell ("Mührü" / "Seal"); the full name is in the info card.
    val words = label.split("\n").let { if (seal) it.takeLast(1) else it }
    Box(
        Modifier.fillMaxSize()
            .padding(if (overview) 5.dp else 3.5.dp)
            .clip(shape)
            .background(ink.copy(alpha = if (seal) .10f else .08f))
            .border(if (strong) 1.6.dp else 1.dp, ink.copy(alpha = if (strong) .85f else .55f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (overview) {
            Text(glyph, color = ink, fontSize = glyphSize, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(glyph, color = ink, fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Black)
                words.forEach { word ->
                    Text(
                        word,
                        color = ink,
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
