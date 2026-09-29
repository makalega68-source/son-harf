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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Word Siege board looks. Stone Keep is the free default board; the others are cosmetic products.
 * A skin draws the frame around the board and the stone texture of empty cells only: letters, bonus
 * marks, territory colours and tap targets are drawn by the board as before, on the same grid.
 */
internal enum class WordSiegeBoardSkin(
    val id: String,
    @param:DrawableRes val frameRes: Int,
    @param:DrawableRes val plateRes: Int,
    @param:DrawableRes val artRes: Int,
    /** Frame band on each side as a share of the frame image (left, top, right, bottom). */
    val inset: List<Float>,
    /** Field colour between cells: the plate tone itself, so no dark grid lines tire the eye. */
    val field: Color,
    val dark: Boolean,
    /** Ground around the frame when the board is zoomed out: the frame image's own outer tone. */
    val ground: Color,
) {
    STONE_KEEP(
        "board_stone_keep", R.drawable.board_frame_stone_keep, R.drawable.board_plate_stone_keep, R.drawable.store_art_board_stone_keep,
        listOf(.085f, .085f, .09f, .088f), Color(0xFFD6C8AA), dark = false, ground = Color(0xFF8A7C67),
    ),
    RIVER_VALLEY(
        "board_river_valley", R.drawable.board_frame_river_valley, R.drawable.board_plate_river_valley, R.drawable.store_art_board_river_valley,
        listOf(.09f, .05f, .09f, .06f), Color(0xFFCFCCBE), dark = false, ground = Color(0xFF55621B),
    ),
    FROST_CITADEL(
        "board_frost_citadel", R.drawable.board_frame_frost_citadel, R.drawable.board_plate_frost_citadel, R.drawable.store_art_board_frost_citadel,
        listOf(.045f, .05f, .045f, .06f), Color(0xFFD5E0EC), dark = false, ground = Color(0xFF99A6B9),
    ),
    OBSIDIAN(
        "board_obsidian", R.drawable.board_frame_obsidian, R.drawable.board_plate_obsidian, R.drawable.store_art_board_obsidian,
        listOf(.05f, .055f, .05f, .055f), Color(0xFF2A2F37), dark = true, ground = Color(0xFF171919),
    ),
    ;

    companion object {
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

/** The skin's frame image, provided once per board and drawn around the 15x15 grid. */
internal val LocalWordSiegeFrame = staticCompositionLocalOf<ImageBitmap?> { null }

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
 * Board area with a skin: the viewport is filled with the ground tone; the frame itself is drawn by
 * the board layer ([wordSiegeBoardFrame]) so it zooms and pans together with the cells.
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
    val frame = ImageBitmap.imageResource(skin.frameRes)
    Box(modifier.background(skin.ground)) {
        CompositionLocalProvider(LocalWordSiegePlate provides plate, LocalWordSiegeFrame provides frame) {
            content(Modifier.fillMaxSize())
        }
    }
}

/**
 * Drawn on the transformed board layer: the frame around the grid, then the plate-tone field under
 * the cells so the frame picture's own tiles never show through.
 */
internal fun Modifier.wordSiegeBoardFrame(skin: WordSiegeBoardSkin?, frame: ImageBitmap?): Modifier =
    if (skin == null || frame == null) this else drawBehind {
        val e = skin.extent(size.width)
        drawImage(
            image = frame,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(frame.width, frame.height),
            dstOffset = IntOffset(-e.left.toInt(), -e.top.toInt()),
            dstSize = IntSize(e.width.toInt(), e.height.toInt()),
        )
        drawRect(skin.field, size = size)
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
 * Shading laid over the stone texture of an empty cell: a faint recess, or the bonus family's tint
 * so bonus cells still read at a glance on every skin.
 */
internal fun wordSiegeSkinCellOverlay(bonusSurface: Color?): androidx.compose.ui.graphics.Brush =
    if (bonusSurface != null) {
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(bonusSurface.copy(alpha = .66f), bonusSurface.copy(alpha = .52f)))
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Black.copy(alpha = .07f), Color.Transparent, Color.White.copy(alpha = .06f)))
    }
