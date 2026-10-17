package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Display art only: stored bonus codes and score multipliers stay unchanged. */
internal object WordSiegeBonusStyle {
    val SealGold = Color(0xFFB18B47)
    val Graphite = Color(0xFF45484C)

    fun glyph(code: String?): String = when (code) {
        "2H" -> "HB²"
        "3H" -> "HB³"
        "2K" -> "KB²"
        "3K" -> "KB³"
        else -> ""
    }
}

private val BonusMarkText = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = .25.sp,
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
    if (glyph.isNotEmpty()) {
        // Text is the entire multiplier mark: no object, badge or secondary caption.
        Box(
            Modifier.fillMaxSize().then(
                if (WordSiegeWalnutIvory.enabled || WordSiegeBoardSkins.active?.dark == true)
                    Modifier.background(Color(0xFFF3EEE3)) else Modifier,
            ), contentAlignment = Alignment.Center,
        ) {
            Text(
                glyph, style = BonusMarkText, color = WordSiegeBonusStyle.Graphite,
                fontSize = glyphSize, maxLines = 1, softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val star = code == WordSiegeBoardSpec.StarBonus
    if (!star && code != WordSiegeBoardSpec.CenterBonus) {
        Text(label, style = BonusMarkText, color = themeLabel, fontSize = glyphSize)
        return
    }
    Column(
        Modifier.fillMaxSize().then(
            if (WordSiegeWalnutIvory.enabled || WordSiegeBoardSkins.active?.dark == true)
                Modifier.background(Color(0xFFF3E8CD)) else Modifier,
        ).padding(if (overview) 2.dp else 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WordSiegeGoldEmblem(star, Modifier.weight(1f).fillMaxSize())
        if (star && !overview) {
            Text("+25", style = BonusMarkText, color = WordSiegeBonusStyle.Graphite,
                fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1)
        }
    }
}

/** Vector metalwork stays crisp at both zoom levels and requires no idle animation. */
@Composable
private fun WordSiegeGoldEmblem(star: Boolean, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val radius = size.minDimension * .43f
        val gold = WordSiegeBonusStyle.SealGold
        drawCircle(
            androidx.compose.ui.graphics.Brush.radialGradient(
                listOf(Color(0xFFFFF9E8), Color(0xFFE9D5A7)), center = center, radius = radius,
            ), radius = radius, center = center,
        )
        drawCircle(gold.copy(alpha = .65f), radius, center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = .8.dp.toPx()))
        drawCircle(gold.copy(alpha = .25f), radius * .83f, center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = .5.dp.toPx()))
        val path = androidx.compose.ui.graphics.Path()
        if (star) {
            repeat(10) { index ->
                val angle = -Math.PI / 2 + index * Math.PI / 5
                val r = radius * if (index % 2 == 0) .69f else .31f
                val x = center.x + kotlin.math.cos(angle).toFloat() * r
                val y = center.y + kotlin.math.sin(angle).toFloat() * r
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        } else {
            // The starting cell carries a small throne crown in a champagne seal.
            val x = center.x
            val y = center.y
            val r = radius * .66f
            path.moveTo(x - r, y - r * .38f)
            path.lineTo(x - r * .48f, y + r * .05f)
            path.lineTo(x, y - r * .78f)
            path.lineTo(x + r * .48f, y + r * .05f)
            path.lineTo(x + r, y - r * .38f)
            path.lineTo(x + r * .73f, y + r * .58f)
            path.lineTo(x - r * .73f, y + r * .58f)
        }
        path.close()
        drawPath(path, androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(Color(0xFFF1D89C), Color(0xFFB18B47), Color(0xFF826136)),
            start = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            end = androidx.compose.ui.geometry.Offset(center.x + radius, center.y + radius),
        ))
        drawPath(path, Color(0xFF826136).copy(alpha = .65f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = .65.dp.toPx()))
        if (!star) {
            drawLine(gold, center + androidx.compose.ui.geometry.Offset(-radius * .48f, radius * .55f),
                center + androidx.compose.ui.geometry.Offset(radius * .48f, radius * .55f), 1.dp.toPx())
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

/** Light-board cell colours of each bonus, used to draw the tutorial's picture of the cell. */
private fun wordSiegeLegendSurface(code: String): Color = when (code) {
    "2H" -> Color(0xFFE0F3EF)
    "3H" -> Color(0xFFC3E7DF)
    "2K" -> Color(0xFFFFF0D3)
    "3K" -> Color(0xFFF6D596)
    WordSiegeBoardSpec.CenterBonus -> Color(0xFFF3E8CD)
    else -> Color(0xFFFBEBB5)
}

/** One short line per bonus: what it does, in plain words. */
internal fun wordSiegeBonusShortEffect(code: String, turkish: Boolean): String = when (code) {
    "2H" -> if (turkish) "Üstüne koyduğun harfin puanı 2 kat." else "The letter placed here scores double."
    "3H" -> if (turkish) "Üstüne koyduğun harfin puanı 3 kat." else "The letter placed here scores triple."
    "2K" -> if (turkish) "Buradan geçen kelimenin puanı 2 kat." else "A word through here scores double."
    "3K" -> if (turkish) "Buradan geçen kelimenin puanı 3 kat." else "A word through here scores triple."
    WordSiegeBoardSpec.CenterBonus -> if (turkish) "İlk kelime buradan geçer; puanı 4 kat." else "The first word passes here; it scores 4x."
    WordSiegeBoardSpec.StarBonus -> if (turkish) "Buraya taş koyana +25 puan." else "+25 points to whoever places a tile here."
    else -> ""
}

/**
 * Picture guide to the bonuses, as they look on the board, each with one short line. Shown in the
 * tutorial and the how-to-play card.
 */
@Composable
internal fun WordSiegeBonusLegend(compact: Boolean, modifier: Modifier = Modifier) {
    val turkish = !SonHarfUiState.isEnglish
    val codes = listOf("2H", "3H", "2K", "3K", WordSiegeBoardSpec.CenterBonus, WordSiegeBoardSpec.StarBonus)
    val icon = if (compact) 30.dp else 36.dp
    // Picture = meaning, one bonus per line, e.g. [gem] = 2x letter points.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)) {
        codes.chunked(2).forEach { pair ->
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { code ->
                    androidx.compose.foundation.layout.Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier.size(icon).clip(RoundedCornerShape(6.dp)).background(wordSiegeLegendSurface(code)),
                            contentAlignment = Alignment.Center,
                        ) {
                            WordSiegeBonusMark(
                                code = code,
                                label = WordSiegeBoardSpec.displayBonusLabel(code, turkish),
                                overview = true,
                                themeLabel = WordSiegeBonusStyle.Graphite,
                                glyphSize = if (compact) 12.sp else 14.sp,
                            )
                        }
                        Text("=", color = Color(0xFF3F554A), fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Black)
                        Text(
                            wordSiegeBonusEquation(code, turkish),
                            color = Color(0xFF17372C),
                            fontSize = if (compact) 10.sp else 11.sp,
                            lineHeight = if (compact) 12.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** The legend's text next to each bonus picture: "picture = Harf Bonus ×2". */
internal fun wordSiegeBonusEquation(code: String, turkish: Boolean): String = when (code) {
    WordSiegeBoardSpec.CenterBonus -> if (turkish) "İlk kelime, Kelime Bonus ×4" else "First word, Word Bonus ×4"
    WordSiegeBoardSpec.StarBonus -> if (turkish) "+25 puan" else "+25 points"
    else -> WordSiegeBoardSpec.bonusLongName(code, turkish)
}


/** 3D bonus objects (rendered top-down, transparent PNG), decoded once for the whole app. */
internal object WordSiegeBonusIcons {
    private val cache = mutableMapOf<Int, androidx.compose.ui.graphics.ImageBitmap>()
    private var resources: android.content.res.Resources? = null

    fun res(code: String?): Int? = when (code) {
        "2H" -> R.drawable.bonus_letter_boost
        "3H" -> R.drawable.bonus_letter_boost_plus
        "2K" -> R.drawable.bonus_word_surge
        "3K" -> R.drawable.bonus_word_surge_plus
        WordSiegeBoardSpec.CenterBonus -> R.drawable.bonus_starting_seal
        WordSiegeBoardSpec.StarBonus -> R.drawable.bonus_premium_star
        else -> null
    }

    fun init(context: android.content.Context) { resources = context.applicationContext.resources }

    fun bitmap(code: String?): androidx.compose.ui.graphics.ImageBitmap? {
        val id = res(code) ?: return null
        cache[id]?.let { return it }
        val r = resources ?: return null
        return runCatching { android.graphics.BitmapFactory.decodeResource(r, id) }
            .getOrNull()?.asImageBitmap()
            ?.also { cache[id] = it }
    }
}

