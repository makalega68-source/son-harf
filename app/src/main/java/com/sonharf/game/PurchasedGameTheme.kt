package com.sonharf.game

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Real purchased-asset theme layer.
 *
 * The atlas is generated only from the licensed NEW Casual Game UI #02 production assets and
 * selected textless OLD pack panels. English-labelled button PNGs are intentionally excluded;
 * all labels remain real Compose text so TR/EN localization stays authoritative.
 */
internal enum class PurchasedUiAsset(val x: Int, val y: Int, val w: Int, val h: Int) {
    PANEL_LARGE(2, 2, 215, 211),
    PANEL_MEDIUM(219, 2, 215, 182),
    PANEL_SMALL(2, 215, 215, 159),
    BANNER(219, 215, 215, 108),
    BUTTON_GREEN(436, 215, 48, 48),
    BUTTON_BLUE(2, 376, 48, 48),
    BUTTON_ORANGE(52, 376, 48, 48),
    BUTTON_PURPLE(102, 376, 48, 48),
    BUTTON_RED(152, 376, 48, 48),
    NAV_HOME(202, 376, 48, 48),
    NAV_SOCIAL(252, 376, 48, 48),
    NAV_SHOP(302, 376, 48, 48),
    NAV_PROFILE(352, 376, 48, 48),
    ICON_CHAT(402, 376, 48, 48),
    ICON_SETTINGS(452, 376, 48, 48),
    ICON_GIFT(2, 426, 48, 48),
    ICON_RANKING(52, 426, 48, 48),
    ICON_TROPHY(102, 426, 48, 48),
    ICON_CROWN(152, 426, 48, 48),
    ICON_GAMES(202, 426, 48, 48),
    ICON_SWORDS(252, 426, 48, 48),
    ICON_REPEAT(302, 426, 48, 48),
    ICON_CLOSE(352, 426, 48, 48),
    ICON_COIN(402, 426, 48, 48),
    ICON_CHECK(452, 426, 48, 37),
    AVATAR_FRAME(2, 476, 70, 70),
    AMOUNT_BAR(74, 476, 160, 36),
    PROGRESS_BASE(236, 476, 160, 14),
    PROGRESS_GREEN(2, 548, 160, 14),
    LEADERBOARD_ROW(164, 548, 160, 38),
    PODIUM_1(326, 548, 43, 80),
    PODIUM_2(371, 548, 44, 80),
    PODIUM_3(417, 548, 44, 80),
    REWARD_PANEL(2, 630, 63, 80),
    OLD_MISSION_ROW(67, 630, 180, 44),
    OLD_DAILY_REWARD(249, 630, 87, 110),
    OLD_LEADERBOARD_PANEL(338, 630, 160, 33),
    OLD_SETTINGS_PANEL(2, 742, 160, 145),
    SHOP_SHELVES(164, 742, 146, 260),
    SEASON_BANNER(312, 742, 150, 81),
    STAR_CENTER(2, 1004, 60, 58),
    STARS_BASE(64, 1004, 150, 73),
    TOGGLE_ON(216, 1004, 75, 29),
    TOGGLE_OFF(293, 1004, 75, 29),
    TOGGLE_HANDLE(370, 1004, 55, 38),
    LOGIN_FIELD(2, 1078, 160, 24),
    LOGIN_MAIL(164, 1078, 48, 35),
    LOGIN_KEY(214, 1078, 41, 48),
}

private object PurchasedAtlasCache {
    @Volatile private var uiAtlas: ImageBitmap? = null
    @Volatile private var ctaAtlas: ImageBitmap? = null

    fun ui(context: Context): ImageBitmap? {
        uiAtlas?.let { return it }
        return synchronized(this) {
            uiAtlas ?: runCatching {
                val ids = intArrayOf(
                    R.raw.purchased_ui_atlas_00,
                    R.raw.purchased_ui_atlas_01,
                    R.raw.purchased_ui_atlas_02,
                    R.raw.purchased_ui_atlas_03,
                )
                val encoded = buildString(46_000) {
                    ids.forEach { id ->
                        context.resources.openRawResource(id).bufferedReader().use { append(it.readText()) }
                    }
                }
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()?.also { uiAtlas = it }
        }
    }

    fun cta(context: Context): ImageBitmap? {
        ctaAtlas?.let { return it }
        return synchronized(this) {
            ctaAtlas ?: runCatching {
                val encoded = context.resources.openRawResource(R.raw.purchased_cta_atlas).bufferedReader().use { it.readText() }
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()?.also { ctaAtlas = it }
        }
    }
}

@Composable
private fun rememberPurchasedAtlas(): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    return remember(context) { PurchasedAtlasCache.ui(context) }
}

internal enum class PurchasedCtaAsset(val x: Int, val y: Int, val w: Int, val h: Int) {
    BLUE(9, 13, 238, 70),
    RED(265, 13, 238, 70),
    ORANGE(9, 109, 238, 70),
    PURPLE(265, 109, 238, 70),
    GREEN(9, 199, 238, 81),
}

@Composable
private fun rememberPurchasedCtaAtlas(): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    return remember(context) { PurchasedAtlasCache.cta(context) }
}

private fun DrawScope.drawPurchasedCrop(image: ImageBitmap, asset: PurchasedUiAsset, alpha: Float = 1f) {
    drawImage(
        image = image,
        srcOffset = IntOffset(asset.x, asset.y),
        srcSize = IntSize(asset.w, asset.h),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
    )
}

/** Nine-slice renderer preserves the licensed frame corners when a panel grows with content. */
private fun DrawScope.drawPurchasedNineSlice(image: ImageBitmap, asset: PurchasedUiAsset, alpha: Float = 1f) {
    val dstW = size.width.roundToInt().coerceAtLeast(1)
    val dstH = size.height.roundToInt().coerceAtLeast(1)
    val srcCapX = (asset.w * .18f).roundToInt().coerceIn(10, asset.w / 3)
    val srcCapY = (asset.h * .18f).roundToInt().coerceIn(10, asset.h / 3)
    val dstCapX = srcCapX.coerceAtMost(dstW / 2)
    val dstCapY = srcCapY.coerceAtMost(dstH / 2)
    val srcMidW = (asset.w - srcCapX * 2).coerceAtLeast(1)
    val srcMidH = (asset.h - srcCapY * 2).coerceAtLeast(1)
    val dstMidW = (dstW - dstCapX * 2).coerceAtLeast(1)
    val dstMidH = (dstH - dstCapY * 2).coerceAtLeast(1)

    fun tile(sx: Int, sy: Int, sw: Int, sh: Int, dx: Int, dy: Int, dw: Int, dh: Int) {
        drawImage(
            image,
            IntOffset(asset.x + sx, asset.y + sy),
            IntSize(sw.coerceAtLeast(1), sh.coerceAtLeast(1)),
            IntOffset(dx, dy),
            IntSize(dw.coerceAtLeast(1), dh.coerceAtLeast(1)),
            alpha = alpha,
        )
    }

    tile(0, 0, srcCapX, srcCapY, 0, 0, dstCapX, dstCapY)
    tile(srcCapX, 0, srcMidW, srcCapY, dstCapX, 0, dstMidW, dstCapY)
    tile(asset.w - srcCapX, 0, srcCapX, srcCapY, dstW - dstCapX, 0, dstCapX, dstCapY)
    tile(0, srcCapY, srcCapX, srcMidH, 0, dstCapY, dstCapX, dstMidH)
    tile(srcCapX, srcCapY, srcMidW, srcMidH, dstCapX, dstCapY, dstMidW, dstMidH)
    tile(asset.w - srcCapX, srcCapY, srcCapX, srcMidH, dstW - dstCapX, dstCapY, dstCapX, dstMidH)
    tile(0, asset.h - srcCapY, srcCapX, srcCapY, 0, dstH - dstCapY, dstCapX, dstCapY)
    tile(srcCapX, asset.h - srcCapY, srcMidW, srcCapY, dstCapX, dstH - dstCapY, dstMidW, dstCapY)
    tile(asset.w - srcCapX, asset.h - srcCapY, srcCapX, srcCapY, dstW - dstCapX, dstH - dstCapY, dstCapX, dstCapY)
}

/** Three-slice renderer keeps the purchased CTA's rounded/bevelled ends undistorted on wide phones. */
private fun DrawScope.drawPurchasedHorizontalSlice(image: ImageBitmap, asset: PurchasedCtaAsset, alpha: Float = 1f) {
    val dstW = size.width.roundToInt().coerceAtLeast(1)
    val dstH = size.height.roundToInt().coerceAtLeast(1)
    val srcCap = (asset.h * .48f).roundToInt().coerceIn(12, asset.w / 3)
    val dstCap = (dstH * .48f).roundToInt().coerceAtMost(dstW / 2)
    val srcMidW = (asset.w - srcCap * 2).coerceAtLeast(1)
    val dstMidW = (dstW - dstCap * 2).coerceAtLeast(1)

    drawImage(image, IntOffset(asset.x, asset.y), IntSize(srcCap, asset.h), IntOffset.Zero, IntSize(dstCap, dstH), alpha = alpha)
    drawImage(image, IntOffset(asset.x + srcCap, asset.y), IntSize(srcMidW, asset.h), IntOffset(dstCap, 0), IntSize(dstMidW, dstH), alpha = alpha)
    drawImage(image, IntOffset(asset.x + asset.w - srcCap, asset.y), IntSize(srcCap, asset.h), IntOffset(dstW - dstCap, 0), IntSize(dstCap, dstH), alpha = alpha)
}

@Composable
internal fun PurchasedAsset(asset: PurchasedUiAsset, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val atlas = rememberPurchasedAtlas()
    Canvas(modifier = modifier) { atlas?.let { drawPurchasedCrop(it, asset, alpha) } }
}

@Composable
internal fun PurchasedPanel(
    modifier: Modifier = Modifier,
    asset: PurchasedUiAsset = PurchasedUiAsset.PANEL_MEDIUM,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val atlas = rememberPurchasedAtlas()
    Box(modifier = modifier) {
        Canvas(Modifier.matchParentSize()) { atlas?.let { drawPurchasedNineSlice(it, asset) } }
        Box(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

internal enum class PurchasedButtonStyle { PRIMARY, SECONDARY, PURPLE, WARNING, DANGER }

private fun PurchasedButtonStyle.asset(): PurchasedCtaAsset = when (this) {
    PurchasedButtonStyle.PRIMARY -> PurchasedCtaAsset.GREEN
    PurchasedButtonStyle.SECONDARY -> PurchasedCtaAsset.BLUE
    PurchasedButtonStyle.PURPLE -> PurchasedCtaAsset.PURPLE
    PurchasedButtonStyle.WARNING -> PurchasedCtaAsset.ORANGE
    PurchasedButtonStyle.DANGER -> PurchasedCtaAsset.RED
}

@Composable
internal fun PurchasedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PurchasedButtonStyle = PurchasedButtonStyle.PRIMARY,
    enabled: Boolean = true,
    leadingAsset: PurchasedUiAsset? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else .42f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val atlas = rememberPurchasedCtaAtlas()
        Canvas(Modifier.matchParentSize()) { atlas?.let { drawPurchasedHorizontalSlice(it, style.asset()) } }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingAsset != null) {
                PurchasedAsset(leadingAsset, Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .35.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun PurchasedIconButton(
    asset: PurchasedUiAsset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentDescription: String = "",
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(if (selected) 58.dp else 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) PurchasedAsset(PurchasedUiAsset.BUTTON_PURPLE, Modifier.matchParentSize())
        PurchasedAsset(asset, Modifier.size(if (selected) 52.dp else 48.dp))
    }
}

@Composable
internal fun PurchasedSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        PurchasedAsset(PurchasedUiAsset.BANNER, Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = .55.sp, textAlign = TextAlign.Center)
            if (action != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                PurchasedButton(text = action, onClick = onAction, modifier = Modifier.widthIn(min = 74.dp), style = PurchasedButtonStyle.SECONDARY)
            }
        }
    }
}

@Composable
internal fun PurchasedCurrencyBar(amount: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(42.dp)
            .widthIn(min = 112.dp)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        PurchasedAsset(PurchasedUiAsset.AMOUNT_BAR, Modifier.matchParentSize())
        Row(verticalAlignment = Alignment.CenterVertically) {
            PurchasedAsset(PurchasedUiAsset.ICON_COIN, Modifier.size(28.dp))
            Spacer(Modifier.width(5.dp))
            Text(amount, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun PurchasedAvatarFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()
        PurchasedAsset(PurchasedUiAsset.AVATAR_FRAME, Modifier.matchParentSize())
    }
}

@Composable
internal fun PurchasedProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(18.dp).fillMaxWidth()) {
        PurchasedAsset(PurchasedUiAsset.PROGRESS_BASE, Modifier.matchParentSize())
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))) {
            PurchasedAsset(PurchasedUiAsset.PROGRESS_GREEN, Modifier.matchParentSize())
        }
    }
}

@Composable
internal fun PurchasedNavItem(label: String, icon: PurchasedUiAsset, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PurchasedIconButton(icon, onClick, selected = selected)
        Text(text = label, color = if (selected) Color(0xFF6D36B6) else Color(0xFF654A3D), fontSize = 9.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun PurchasedGameBackdrop(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFFF4E3BD)))
}
