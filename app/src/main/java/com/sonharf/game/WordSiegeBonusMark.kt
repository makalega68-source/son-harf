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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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

/** Text without the font's extra top/bottom padding, so lines sit on the true centre of the mark. */
private val BonusMarkText = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

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
    // Round marks (word surges, the seal) keep their text inside the circle's inscribed square.
    val round = code == "2K" || code == "3K" || seal
    Box(
        Modifier.fillMaxSize()
            .padding(if (overview) 5.dp else 3.5.dp)
            .clip(shape)
            // A soft embossed chip: gentle glow in the middle, a faint rim, no hard ring.
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(ink.copy(alpha = if (seal) .16f else .12f), ink.copy(alpha = if (strong) .07f else .04f)),
                ),
            )
            .border(1.dp, ink.copy(alpha = if (strong) .32f else .20f), shape)
            .padding(if (round) 5.dp else 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (overview) {
            Text(glyph, style = BonusMarkText, color = ink, fontSize = glyphSize, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)) {
                Text(glyph, style = BonusMarkText, color = ink, fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                words.forEach { word ->
                    Text(
                        word,
                        style = BonusMarkText,
                        color = ink,
                        fontSize = if (round) 7.5.sp else 8.sp,
                        lineHeight = if (round) 8.5.sp else 9.sp,
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

/**
 * Board cell surface. Empty cells read as shallow recesses in stone (a hair darker at the top-left
 * rim, no hard outline) so 225 of them rest the eye; tiles with a letter read as raised stones
 * with a soft top light and a darker lower edge.
 */
internal fun wordSiegeCellBrush(base: Color, raised: Boolean, walnut: Boolean): androidx.compose.ui.graphics.Brush =
    if (raised) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(
                androidx.compose.ui.graphics.lerp(base, Color.White, if (walnut) .10f else .16f),
                base,
                androidx.compose.ui.graphics.lerp(base, Color.Black, .12f),
            ),
        )
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.lerp(base, Color.Black, if (walnut) .06f else .035f),
                base,
                androidx.compose.ui.graphics.lerp(base, Color.White, if (walnut) .03f else .05f),
            ),
        )
    }
