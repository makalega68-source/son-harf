package com.sonharf.game

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Word Siege board looks. Stone Keep is the free default board; the others are cosmetic products.
 * A skin is a tabletop slab drawn in code (so it stays sharp at every zoom): a bevelled rim with a
 * metal inlay line and corner studs, a recessed field, and the stone grain of the skin on empty
 * cells. Letters, bonus marks, territory colours and tap targets stay on the same grid.
 */
internal enum class WordSiegeBoardSkin(
    val id: String,
    @param:DrawableRes val plateRes: Int,
    @param:DrawableRes val artRes: Int,
    /** Rim of the slab: light edge, body, shaded edge. */
    val rim: List<Color>,
    /** Thin metal inlay line and corner studs on the rim. */
    val inlay: Color,
    /** Groove between the raised cells: a shade just below the plate tone, never a dark grid line. */
    val field: Color,
    val dark: Boolean,
    /** Table under the slab when zoomed out: centre and edge of a soft vignette. */
    val ground: List<Color>,
) {
    STONE_KEEP(
        "board_stone_keep", R.drawable.board_plate_stone_keep, R.drawable.store_art_board_stone_keep,
        listOf(Color(0xFFC9B48D), Color(0xFFA58C66), Color(0xFF76603F)), Color(0xFFD8B25E),
        Color(0xFFC4B593), dark = false, ground = listOf(Color(0xFF5B5044), Color(0xFF2F2821)),
    ),
    RIVER_VALLEY(
        "board_river_valley", R.drawable.board_plate_river_valley, R.drawable.store_art_board_river_valley,
        listOf(Color(0xFF6F9C82), Color(0xFF41705A), Color(0xFF244536)), Color(0xFFBFE3D2),
        Color(0xFFBBB8A9), dark = false, ground = listOf(Color(0xFF34473B), Color(0xFF18221C)),
    ),
    FROST_CITADEL(
        "board_frost_citadel", R.drawable.board_plate_frost_citadel, R.drawable.store_art_board_frost_citadel,
        listOf(Color(0xFF9FB2C9), Color(0xFF6D819C), Color(0xFF45566F)), Color(0xFFF1F6FB),
        Color(0xFFBDCADA), dark = false, ground = listOf(Color(0xFF45526A), Color(0xFF222A38)),
    ),
    OBSIDIAN(
        "board_obsidian", R.drawable.board_plate_obsidian, R.drawable.store_art_board_obsidian,
        listOf(Color(0xFF454952), Color(0xFF25282E), Color(0xFF101114)), Color(0xFFD4AF37),
        Color(0xFF1C2026), dark = true, ground = listOf(Color(0xFF1E1F23), Color(0xFF060607)),
    ),
    ;

    /** Rim band on each side as a share of the whole slab (left, top, right, bottom). */
    val inset: List<Float> get() = RimShare

    companion object {
        private val RimShare = listOf(.055f, .055f, .055f, .055f)
        val DEFAULT = STONE_KEEP
        /** Skins sold in the store (Stone Keep is free and needs no product). */
        val productIds: Set<String> = setOf(RIVER_VALLEY.id, FROST_CITADEL.id, OBSIDIAN.id)
        fun fromId(id: String?): WordSiegeBoardSkin? = entries.firstOrNull { it.id == id }
    }
}

internal object WordSiegeBoardSkins {
    private const val PREFS = "word_siege_board_skin"
    private const val KEY = "selected"

    /** A bought skin the player chose in Profile > Collection; null means the default Stone Keep. */
    var selectedId by mutableStateOf<String?>(null)
        private set

    fun restore(context: Context) {
        selectedId = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.takeIf { it in WordSiegeBoardSkin.productIds }
    }

    /** Only owned skins are offered for selection; null returns to Stone Keep. */
    fun select(context: Context, id: String?) {
        val next = id?.takeIf { it in WordSiegeBoardSkin.productIds }
        selectedId = next
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, next).apply()
    }

    /**
     * The board look in play: a chosen skin always wins; otherwise the Walnut & Ivory / Black app
     * themes keep their own boards, and everyone else gets the Stone Keep.
     */
    val active: WordSiegeBoardSkin?
        get() = WordSiegeBoardSkin.fromId(selectedId)
            ?: if (WordSiegeWalnutIvory.enabled) null else WordSiegeBoardSkin.DEFAULT
}

/** Stone texture for empty cells, provided once per board so cells do not decode it 225 times. */
internal val LocalWordSiegePlate = staticCompositionLocalOf<ImageBitmap?> { null }

/**
 * Size of the board plus its frame, in the board's own pixels. The grid keeps its origin at (0,0);
 * the frame extends [left]/[top] before it and to [width]/[height] in total.
 */
internal data class WordSiegeSkinExtent(val width: Float, val height: Float, val left: Float, val top: Float)

internal fun WordSiegeBoardSkin?.extent(boardPx: Float): WordSiegeSkinExtent {
    if (this == null) return WordSiegeSkinExtent(boardPx, boardPx, 0f, 0f)
    val width = boardPx / (1f - inset[0] - inset[2])
    val height = boardPx / (1f - inset[1] - inset[3])
    return WordSiegeSkinExtent(width, height, width * inset[0], height * inset[1])
}

/** Zoomed all the way out, the whole framed board fits the screen. */
internal fun wordSiegeSkinnedFitScale(viewportWidthPx: Float, viewportHeightPx: Float, boardPx: Float, skin: WordSiegeBoardSkin?): Float {
    val e = skin.extent(boardPx)
    return wordSiegeFitScale(viewportWidthPx, viewportHeightPx, e.width, e.height)
}

/** Pan limits for the framed board: the frame may be scrolled into view, never past it. */
internal fun clampWordSiegeSkinnedPan(
    candidate: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    boardPx: Float,
    scale: Float,
    skin: WordSiegeBoardSkin?,
): Offset {
    if (skin == null) return clampWordSiegeBoardPan(candidate, viewportWidthPx, viewportHeightPx, boardPx, scale)
    val e = skin.extent(boardPx)
    val shift = Offset(e.left * scale, e.top * scale)
    return clampWordSiegeBoardPan(candidate - shift, viewportWidthPx, viewportHeightPx, e.width, scale, e.height) + shift
}

/**
 * Board area with a skin: the viewport is the table under the slab; the slab itself is drawn by the
 * board layer ([wordSiegeBoardFrame]) so it zooms and pans together with the cells.
 */
@Composable
internal fun WordSiegeSkinnedBoard(
    skin: WordSiegeBoardSkin?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(viewportModifier: Modifier) -> Unit,
) {
    if (skin == null) {
        Box(modifier) { content(Modifier.fillMaxSize()) }
        return
    }
    val plate = ImageBitmap.imageResource(skin.plateRes)
    Box(modifier.background(Brush.radialGradient(skin.ground))) {
        CompositionLocalProvider(LocalWordSiegePlate provides plate) {
            content(Modifier.fillMaxSize())
        }
    }
}

/**
 * Drawn on the transformed board layer, behind the 15x15 grid (whose origin stays at 0,0): a soft
 * shadow on the table, the bevelled slab with its stone grain, a metal inlay line with studs at the
 * corners and side centres, then the recessed field under the cells.
 */
internal fun Modifier.wordSiegeBoardFrame(skin: WordSiegeBoardSkin?, plate: ImageBitmap?): Modifier =
    if (skin == null) this else drawBehind {
        val e = skin.extent(size.width)
        val band = e.left
        val origin = Offset(-e.left, -e.top)
        val slab = Size(e.width, e.height)
        val radius = CornerRadius(band * .6f)

        // Shadow on the table: a few widening, fading layers below the slab.
        for (step in 1..5) {
            val grow = band * .07f * step
            drawRoundRect(
                Color.Black.copy(alpha = .09f),
                topLeft = origin + Offset(-grow, -grow + band * .12f * step),
                size = Size(slab.width + grow * 2, slab.height + grow * 2),
                cornerRadius = CornerRadius(radius.x + grow),
            )
        }
        // Slab body, lit from the top left.
        drawRoundRect(
            Brush.linearGradient(skin.rim, start = origin, end = origin + Offset(slab.width, slab.height)),
            topLeft = origin, size = slab, cornerRadius = radius,
        )
        if (plate != null) {
            drawRoundRect(
                ShaderBrush(ImageShader(plate, TileMode.Mirror, TileMode.Mirror)),
                topLeft = origin, size = slab, cornerRadius = radius,
                alpha = if (skin.dark) .35f else .45f, blendMode = BlendMode.Multiply,
            )
        }
        // Outer chamfer: bright top-left edge, dark bottom-right edge.
        val edge = band * .09f
        drawRoundRect(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = .42f), Color.Transparent, Color.Black.copy(alpha = .38f)),
                start = origin, end = origin + Offset(slab.width, slab.height),
            ),
            topLeft = origin + Offset(edge / 2, edge / 2), size = Size(slab.width - edge, slab.height - edge),
            cornerRadius = radius, style = Stroke(edge),
        )
        // Metal inlay line with studs.
        val inlayAt = band * .36f
        val inlayOrigin = origin + Offset(inlayAt, inlayAt)
        val inlaySize = Size(slab.width - inlayAt * 2, slab.height - inlayAt * 2)
        drawRoundRect(
            skin.inlay.copy(alpha = .9f), topLeft = inlayOrigin, size = inlaySize,
            cornerRadius = CornerRadius(band * .25f), style = Stroke(maxOf(1.5f, band * .05f)),
        )
        val studs = listOf(
            inlayOrigin, inlayOrigin + Offset(inlaySize.width, 0f),
            inlayOrigin + Offset(0f, inlaySize.height), inlayOrigin + Offset(inlaySize.width, inlaySize.height),
            inlayOrigin + Offset(inlaySize.width / 2, 0f), inlayOrigin + Offset(inlaySize.width / 2, inlaySize.height),
            inlayOrigin + Offset(0f, inlaySize.height / 2), inlayOrigin + Offset(inlaySize.width, inlaySize.height / 2),
        )
        studs.forEachIndexed { i, c ->
            val r = band * (if (i < 4) .16f else .11f)
            drawCircle(Color.Black.copy(alpha = .35f), r, c + Offset(r * .15f, r * .3f))
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = .9f), skin.inlay, skin.inlay.copy(red = skin.inlay.red * .55f, green = skin.inlay.green * .55f, blue = skin.inlay.blue * .55f)), center = c - Offset(r * .35f, r * .35f), radius = r * 1.3f),
                r, c,
            )
        }
        // Recessed field: a dark lip on the inner edge, the plate tone, and shade falling from the rim.
        val lip = band * .12f
        drawRoundRect(
            Brush.linearGradient(
                listOf(Color.Black.copy(alpha = .45f), Color.Black.copy(alpha = .2f), Color.White.copy(alpha = .3f)),
                start = Offset(-lip, -lip), end = Offset(size.width + lip, size.height + lip),
            ),
            topLeft = Offset(-lip, -lip), size = Size(size.width + lip * 2, size.height + lip * 2),
            cornerRadius = CornerRadius(lip),
        )
        drawRect(skin.field, size = size)
        val fall = band * .35f
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .22f), Color.Transparent), 0f, fall), size = Size(size.width, fall))
        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .14f), Color.Transparent), 0f, fall), size = Size(fall, size.height))
    }

/** Draws the skin's stone texture across an empty cell (after the cell is clipped to its shape). */
internal fun Modifier.wordSiegePlateTexture(plate: ImageBitmap?): Modifier =
    if (plate == null) this else drawBehind {
        drawImage(
            image = plate,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(plate.width, plate.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }

/**
 * Shading laid over the stone texture of an empty cell: a convex light-to-shade fall so the plate reads
 * as a raised stone, or the bonus family's tint so bonus cells still read at a glance on every skin.
 */
internal fun wordSiegeSkinCellOverlay(bonusSurface: Color?): Brush =
    if (bonusSurface != null) {
        Brush.linearGradient(listOf(bonusSurface.copy(alpha = .40f), bonusSurface.copy(alpha = .28f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = .16f), Color.Transparent, Color.Black.copy(alpha = .12f)))
    }

/**
 * Bevel of a raised stone cell, drawn inside its rounded clip: a bright top-left edge, a dark
 * bottom-right edge and a soft top sheen, lit from the same corner as the slab.
 */
internal fun Modifier.wordSiegeCellBevel(dark: Boolean): Modifier = drawBehind {
    val w = size.minDimension * .085f
    val corner = size.minDimension * .14f
    drawRoundRect(
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (dark) .26f else .55f),
                Color.White.copy(alpha = if (dark) .06f else .12f),
                Color.Black.copy(alpha = if (dark) .45f else .30f),
            ),
            start = Offset.Zero, end = Offset(size.width, size.height),
        ),
        topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(corner), style = Stroke(w),
    )
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) .07f else .14f), Color.Transparent), w, size.height * .45f),
        topLeft = Offset(w, w), size = Size(size.width - w * 2, size.height * .42f),
        cornerRadius = CornerRadius(corner * .7f),
    )
}
