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
) {
    STONE_KEEP(
        "board_stone_keep", R.drawable.board_frame_stone_keep, R.drawable.board_plate_stone_keep, R.drawable.store_art_board_stone_keep,
        listOf(.085f, .085f, .09f, .088f), Color(0xFFD6C8AA), dark = false,
    ),
    RIVER_VALLEY(
        "board_river_valley", R.drawable.board_frame_river_valley, R.drawable.board_plate_river_valley, R.drawable.store_art_board_river_valley,
        listOf(.09f, .05f, .09f, .06f), Color(0xFFCFCCBE), dark = false,
    ),
    FROST_CITADEL(
        "board_frost_citadel", R.drawable.board_frame_frost_citadel, R.drawable.board_plate_frost_citadel, R.drawable.store_art_board_frost_citadel,
        listOf(.045f, .05f, .045f, .06f), Color(0xFFD5E0EC), dark = false,
    ),
    OBSIDIAN(
        "board_obsidian", R.drawable.board_frame_obsidian, R.drawable.board_plate_obsidian, R.drawable.store_art_board_obsidian,
        listOf(.05f, .055f, .05f, .055f), Color(0xFF2A2F37), dark = true,
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

/**
 * Framed board: the skin's frame fills the whole area and the playing viewport sits inside its
 * border band. Without a skin the content is drawn unchanged.
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
    BoxWithConstraints(modifier) {
        Image(
            painter = painterResource(skin.frameRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        val viewport = Modifier
            .fillMaxSize()
            .padding(
                start = maxWidth * skin.inset[0],
                top = maxHeight * skin.inset[1],
                end = maxWidth * skin.inset[2],
                bottom = maxHeight * skin.inset[3],
            )
            .background(skin.field)
        CompositionLocalProvider(LocalWordSiegePlate provides plate) {
            content(viewport)
        }
    }
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
