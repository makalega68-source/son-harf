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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    // Rendered 3D object for the bonus (top-down, transparent). Falls back to the drawn mark.
    val icon = WordSiegeBonusIcons.bitmap(code)
    if (icon != null) {
        WordSiegeBonusIconMark(code, icon, label, overview, ink)
        return
    }
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

/** Light-board cell colours of each bonus, used to draw the tutorial's picture of the cell. */
private fun wordSiegeLegendSurface(code: String): Color = when (code) {
    "2H" -> Color(0xFFE0F3EF)
    "3H" -> Color(0xFFC3E7DF)
    "2K" -> Color(0xFFFFF0D3)
    "3K" -> Color(0xFFF6D596)
    WordSiegeBoardSpec.CenterBonus -> Color(0xFF24304B)
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
                        val bitmap = WordSiegeBonusIcons.bitmap(code)
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(bitmap, contentDescription = null, modifier = Modifier.size(icon))
                        } else {
                            Box(
                                Modifier.size(icon).clip(RoundedCornerShape(6.dp)).background(wordSiegeLegendSurface(code)),
                                contentAlignment = Alignment.Center,
                            ) {
                                WordSiegeBonusMark(
                                    code = code,
                                    label = WordSiegeBoardSpec.displayBonusLabel(code, turkish),
                                    overview = true,
                                    themeLabel = Color(0xFF3F4A5A),
                                    glyphSize = if (compact) 13.sp else 15.sp,
                                )
                            }
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

@Composable
private fun WordSiegeBonusIconMark(
    code: String,
    icon: androidx.compose.ui.graphics.ImageBitmap,
    label: String,
    overview: Boolean,
    ink: Color,
) {
    val star = code == WordSiegeBoardSpec.StarBonus
    // The +25 star cell glows softly so it is noticed at once.
    val glow = if (star) {
        val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "star cell glow")
        pulse.animateFloat(
            initialValue = .35f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween(900),
                androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "star cell glow alpha",
        )
    } else null
    Box(
        Modifier.fillMaxSize().then(
            if (glow != null) Modifier.drawBehind {
                drawRect(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(Color(0xFFFFE082).copy(alpha = .95f * glow.value), Color(0xFFFFC107).copy(alpha = .45f * glow.value), Color.Transparent),
                        center = center,
                        radius = size.minDimension * .75f,
                    ),
                )
            } else Modifier,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (overview) {
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(3.dp),
            )
        } else {
            val word = label.replace("\n", " ").trim()
            Column(
                Modifier.fillMaxSize().padding(horizontal = 1.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            ) {
                androidx.compose.foundation.Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(if (word.isEmpty()) 38.dp else 30.dp))
                if (word.isNotEmpty()) {
                    Text(
                        word,
                        style = BonusMarkText,
                        color = ink,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
