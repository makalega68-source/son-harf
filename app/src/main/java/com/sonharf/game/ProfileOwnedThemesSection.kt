package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.EquippedCosmeticsDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ShopItemDto
import com.sonharf.game.data.equipDefaultCosmetic
import com.sonharf.game.data.equipDefaultGameTheme
import com.sonharf.game.data.equipShopItem
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getOwnedShopItems
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val ProfileThemeTimeoutMs = 10_000L
private const val BlackThemeId = "theme_black"
private const val LegacyDarkArenaThemeId = "theme_dark_arena"
private val DarkThemeIds = setOf(BlackThemeId, LegacyDarkArenaThemeId)

/** Slots the player can return to the free built-in look from the profile. */
private val ResettableKinds = listOf("profile_frame", "mascot_hat", "victory_effect", "keyboard_theme", "name_style")

private fun EquippedCosmeticsDto?.slotFor(kind: String): String? = when (kind) {
    "keyboard_theme" -> this?.keyboardThemeId
    "name_style" -> this?.nameStyleId
    "mascot_hat" -> this?.mascotHatId
    "profile_frame" -> this?.profileFrameId
    "victory_effect" -> this?.victoryEffectId
    else -> null
}

private fun defaultStyleTitle(kind: String) = when (kind) {
    "keyboard_theme" -> sh("Standart Klavye", "Standard Keyboard")
    "mascot_hat" -> sh("Şapkasız Obi", "Obi, no hat")
    "profile_frame" -> sh("Çerçevesiz", "No frame")
    "victory_effect" -> sh("Standart Zafer", "Standard Victory")
    else -> sh("Standart İsim Rengi", "Standard Name Color")
}

/**
 * Owned Style collection. Store rotation may stop new sales, but supported purchased visuals remain
 * available to their owner. Network failures never publish a partial result over the cached look.
 */
@Composable
internal fun ProfileOwnedThemesSection(backend: OnlineGameBackend, category: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var collection by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reloadCollection() {
        loading = true
        notice = null
        try {
            withTimeout(ProfileThemeTimeoutMs) {
                coroutineScope {
                    val equippedRequest = async { backend.getEquippedCosmetics() }
                    val nextOwned = backend.getInventory()
                    val nextCollection = backend.getOwnedShopItems(nextOwned)
                    val nextEquipped = equippedRequest.await()

                    // Publish one complete snapshot only. A failed request must not erase cached UI.
                    owned = nextOwned
                    collection = nextCollection
                    equipped = nextEquipped
                    SonHarfCosmetics.applyAndPersist(context, nextEquipped)
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            notice = sh(
                "Koleksiyon yenilenemedi. Mevcut görünümün korundu; tekrar deneyebilirsin.",
                "Could not refresh your collection. Your current style is unchanged; you can retry.",
            )
        } finally {
            loading = false
        }
    }

    fun equipStyle(itemId: String?) {
        if (busy || loading) return
        if (itemId != null && itemId !in owned) return
        busy = true
        notice = null
        scope.launch {
            try {
                val nextEquipped = withTimeout(ProfileThemeTimeoutMs) {
                    if (itemId == null) backend.equipDefaultGameTheme()
                    else backend.equipShopItem(itemId)
                    backend.getEquippedCosmetics()
                }
                equipped = nextEquipped
                SonHarfCosmetics.applyAndPersist(context, nextEquipped)
                notice = sh("Görünüm uygulandı.", "Style applied.")
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                notice = sh(
                    "İşlem doğrulanamadı. Görünümünü yenileyip kontrol et.",
                    "Could not confirm the change. Refresh to check your equipped style.",
                )
            } finally {
                busy = false
            }
        }
    }

    /** Back to the free default look for one slot; ownership of bought items is untouched. */
    fun resetSlot(kind: String) {
        if (busy || loading) return
        busy = true
        notice = null
        scope.launch {
            try {
                val nextEquipped = withTimeout(ProfileThemeTimeoutMs) {
                    backend.equipDefaultCosmetic(kind)
                    backend.getEquippedCosmetics()
                }
                equipped = nextEquipped
                SonHarfCosmetics.applyAndPersist(context, nextEquipped)
                notice = sh("Varsayılan görünüme dönüldü.", "Back to the default look.")
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                notice = sh(
                    "İşlem doğrulanamadı. Görünümünü yenileyip kontrol et.",
                    "Could not confirm the change. Refresh to check your equipped style.",
                )
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(backend) { reloadCollection() }

    // Current Black Theme and the retired Night Arena id share the same supported dark-theme family.
    // Prefer the current sellable id while preserving historical ownership/equipment.
    val activeDarkThemeId = SonHarfCosmetics.gameThemeId?.takeIf { it in DarkThemeIds }
    val ownedDarkThemeId = when {
        BlackThemeId in owned -> BlackThemeId
        LegacyDarkArenaThemeId in owned -> LegacyDarkArenaThemeId
        activeDarkThemeId != null -> activeDarkThemeId
        else -> null
    }
    val darkActive = activeDarkThemeId != null
    val showBlackTheme = ownedDarkThemeId != null
    val walnutItem = collection.firstOrNull { it.id == WALNUT_IVORY_THEME_ID && it.id in owned && it.isSupportedOwnedStyle() }

    // Historical ownership stays safely on the server, but products with no live game
    // integration must not occupy the player's visible profile collection. Theme aliases are
    // represented by the single canonical theme card to avoid duplicate equipped states.
    val styles = collection.filter { it.id !in DarkThemeIds && it.id != WALNUT_IVORY_THEME_ID && it.isSupportedOwnedStyle() }
    // The Obi tab also holds the victory crown, since Obi wears it when you win.
    fun inCategory(kind: String) = kind == category || (category == "mascot_hat" && kind == "victory_effect")
    val shown = if (category == null) styles else styles.filter { inCategory(it.kind) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (category == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Palette, null, tint = Hf.Gold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(sh("Koleksiyonum", "My collection"), color = Hf.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (loading || busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Hf.Gold)
            }
        } else if (loading || busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Gold, trackColor = Hf.Surface)
        }

        if (category == null || category == "game_theme") {
            val themeTiles = buildList<@Composable (Modifier) -> Unit> {
                add { m ->
                    ProfileThemeCard(
                        title = sh("Ana Tema", "Main Theme"),
                        subtitle = sh("Varsayılan görünüm • Ücretsiz", "Default look • Free"),
                        active = SonHarfCosmetics.gameThemeId == null,
                        enabled = !busy && !loading,
                        blackVariant = false,
                        modifier = m,
                        onClick = { equipStyle(null) },
                    )
                }
                if (showBlackTheme) add { m ->
                    ProfileThemeCard(
                        title = "Black Theme",
                        subtitle = sh("Koleksiyonunda", "In your collection"),
                        active = darkActive,
                        enabled = !busy && !loading,
                        blackVariant = true,
                        modifier = m,
                        preview = {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.store_art_theme_black),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                            )
                        },
                        onClick = { equipStyle(ownedDarkThemeId ?: BlackThemeId) },
                    )
                }
                if (walnutItem != null) add { m ->
                    OwnedStyleCard(
                        item = walnutItem,
                        active = equipped.isEquipped(walnutItem),
                        enabled = !loading && !busy,
                        modifier = m,
                        onEquip = { equipStyle(walnutItem.id) },
                    )
                }
            }
            CollectionGrid(themeTiles)
        }

        notice?.let {
            Text(it, color = Hf.TextMuted, fontSize = 13.sp)
            TextButton(
                onClick = { scope.launch { reloadCollection() } },
                enabled = !busy && !loading,
            ) {
                Text(sh("YENİLE", "REFRESH"), color = Hf.Gold)
            }
        }

        if (category == null || category == "mascot_hat") OwnedMascotsPicker()

        if (category != "game_theme") {
            // Every slot the player can dress up also offers the free standard look to go back to.
            val defaultKinds = if (category == null) ResettableKinds.filter { kind -> styles.any { it.kind == kind } } else ResettableKinds.filter { kind -> inCategory(kind) && (kind == category || styles.any { it.kind == kind }) }
            if (!loading && notice == null && shown.isEmpty()) {
                HfCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        sh("Bu kategoride ürünün yok. Yenilerini mağazada keşfet.", "Nothing here yet. Discover new items in the store."),
                        Modifier.fillMaxWidth().padding(18.dp),
                        color = Hf.TextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            // The free standard look sits in the same grid as the owned items, first in line.
            val tiles = buildList<@Composable (Modifier) -> Unit> {
                defaultKinds.forEach { kind ->
                    add { m ->
                        ProfileThemeCard(
                            title = defaultStyleTitle(kind),
                            subtitle = sh("Varsayılan • Ücretsiz", "Default • Free"),
                            active = equipped != null && equipped.slotFor(kind) == null,
                            enabled = !busy && !loading,
                            blackVariant = false,
                            modifier = m,
                            preview = { DefaultSlotPreview(kind) },
                            onClick = { resetSlot(kind) },
                        )
                    }
                }
                shown.forEach { item ->
                    add { m ->
                        OwnedStyleCard(
                            item = item,
                            active = equipped.isEquipped(item),
                            enabled = !loading && !busy,
                            modifier = m,
                            onEquip = { equipStyle(item.id) },
                        )
                    }
                }
            }
            CollectionGrid(tiles)
        }
    }
}

@Composable
private fun ProfileThemeCard(
    title: String,
    subtitle: String,
    active: Boolean,
    enabled: Boolean,
    blackVariant: Boolean,
    modifier: Modifier = Modifier,
    preview: (@Composable BoxScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled && !active, onClick = onClick),
        shape = Hf.CardShape,
        color = Hf.Ground,
        border = BorderStroke(if (active) 2.dp else 1.5.dp, if (active) Hf.Green else Hf.Gold.copy(alpha = .75f)),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.fillMaxWidth().height(72.dp).background(
                    brush = if (blackVariant) {
                        Brush.linearGradient(listOf(Color(0xFF050608), Color(0xFF111318), Color(0xFF20242B)))
                    } else {
                        Brush.linearGradient(listOf(Hf.Ground, Hf.Surface, Hf.Gold.copy(alpha = .45f)))
                    },
                    shape = RoundedCornerShape(12.dp),
                ),
            ) {
                preview?.invoke(this)
                if (active) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = Hf.GreenLight,
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(22.dp),
                    )
                }
            }
            Text(title, color = Hf.Text, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(subtitle, color = Hf.TextMuted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            ProfileUseButton(active = active, enabled = enabled, onClick = onClick)
        }
    }
}

/** Bought mascots are chosen here (the store only sells them): the pick follows the player everywhere. */
@Composable
private fun OwnedMascotsPicker() {
    val owned = WordSiegeMascotOwnership.owned.sortedBy { it.ordinal }
    if (owned.isEmpty()) return
    val context = LocalContext.current
    val bond = remember { WordSiegeMascotBond(context) }
    var chosen by remember { mutableStateOf(bond.skinChoice?.takeIf { it in owned } ?: owned.first()) }
    val scope = rememberCoroutineScope()
    Text(sh("MASKOTLARIM", "MY MASCOTS"), color = Hf.Gold, fontSize = 13.sp, fontWeight = FontWeight.Black)
    CollectionGrid(owned.map { skin ->
        @Composable { m: Modifier ->
            val active = chosen == skin
            Surface(
                modifier = m,
                shape = Hf.CardShape,
                color = Hf.Ground,
                border = BorderStroke(if (active) 2.dp else 1.5.dp, if (active) Hf.Green else Hf.Gold.copy(alpha = .75f)),
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                        WordSiegeMascot(
                            moveId = null,
                            lastMoveMine = false,
                            pendingCells = emptyList(),
                            playerTurn = false,
                            modifier = Modifier.size(70.dp),
                            skin = skin,
                        )
                    }
                    Text(sh(skin.titleTr, skin.titleEn), color = Hf.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    ProfileUseButton(active = active, enabled = true) {
                        bond.skinChoice = skin
                        chosen = skin
                        scope.launch { PlayerMascots.publish(skin) }
                    }
                }
            }
        }
    })
}

/** Three even columns; a short last row keeps its cards the same width as the rest. */
@Composable
private fun CollectionGrid(tiles: List<@Composable (Modifier) -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** What the free standard look of a slot is: a plain ring, bare Obi, a plain key, and so on. */
@Composable
private fun BoxScope.DefaultSlotPreview(kind: String) {
    when (kind) {
        "profile_frame" -> Box(
            Modifier.align(Alignment.Center).size(58.dp).border(2.dp, Color(0xFFBDBDBD), CircleShape).background(Hf.Surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, null, Modifier.size(34.dp), tint = Hf.TextMuted)
        }
        "mascot_hat" -> ObiHatPreview(hatId = "", modifier = Modifier.align(Alignment.Center).padding(6.dp))
        else -> Text(
            when (kind) {
                "victory_effect" -> "🏆"
                "keyboard_theme" -> "⌨️"
                else -> "Aa"
            },
            Modifier.align(Alignment.Center),
            color = Hf.Text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Kullan / Kullanılıyor pill from the 06 preview. */
@Composable
private fun ProfileUseButton(active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled && !active,
        modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp),
        shape = Hf.PillShape,
        color = if (active) Hf.Green else Hf.Ivory,
        border = if (active) BorderStroke(1.5.dp, Hf.GreenLight) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (active) sh("✓ Takılı", "✓ On") else sh("Kullan", "Equip"),
                color = if (active) Hf.OnAccent else Hf.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun OwnedStyleCard(
    item: ShopItemDto,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onEquip: () -> Unit,
) {
    val supported = item.isSupportedOwnedStyle()
    Surface(
        modifier = modifier,
        shape = Hf.CardShape,
        color = Hf.Ground,
        border = BorderStroke(if (active) 2.dp else 1.5.dp, if (active) Hf.Green else Hf.Gold.copy(alpha = .75f)),
    ) {
        Box {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    if (item.kind == "profile_frame" && supported) {
                        Icon(Icons.Rounded.Person, null, Modifier.size(30.dp), tint = Hf.TextMuted)
                        ProfileFrameArt(frameId = item.id, size = 52.dp)
                    } else {
                        StoreProductPreview(item, Modifier.fillMaxSize())
                    }
                }
                Text(sh(item.nameTr, item.nameEn), color = Hf.Text, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (!item.active) {
                    Text(sh("Arşiv ürünü", "Retired item"), color = Hf.TextMuted, fontSize = 10.sp)
                }
                if (!supported) {
                    Text(
                        sh(
                            "Bu sürümde kullanılamıyor. Sahipliğin korunuyor.",
                            "Unavailable in this version. You still own this item.",
                        ),
                        color = Hf.TextMuted,
                        fontSize = 10.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onEquip,
                    enabled = enabled && supported && !active,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = Hf.PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Hf.Ivory,
                        contentColor = Hf.Text,
                        disabledContainerColor = if (active) Hf.Green else Hf.Disabled,
                        disabledContentColor = Hf.Text,
                    ),
                ) {
                    Text(if (active) sh("✓ Takılı", "✓ On") else sh("Kullan", "Equip"), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            if (active) {
                Surface(Modifier.align(Alignment.TopEnd).padding(5.dp).size(22.dp), shape = androidx.compose.foundation.shape.CircleShape, color = Hf.Green, border = BorderStroke(1.5.dp, Hf.GreenLight)) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Hf.OnAccent, modifier = Modifier.padding(3.dp))
                }
            }
        }
    }
}
