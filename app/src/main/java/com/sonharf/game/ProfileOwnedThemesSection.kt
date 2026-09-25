package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

    // Historical ownership stays safely on the server, but products with no live game
    // integration must not occupy the player's visible profile collection. Theme aliases are
    // represented by the single canonical theme card to avoid duplicate equipped states.
    val styles = collection.filter { it.id !in DarkThemeIds && it.isSupportedOwnedStyle() }
    val shown = if (category == null) styles else styles.filter { it.kind == category }

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
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileThemeCard(
                    title = sh("Ana Tema", "Main Theme"),
                    subtitle = sh("Varsayılan görünüm • Ücretsiz", "Default look • Free"),
                    active = !darkActive,
                    enabled = !busy && !loading,
                    blackVariant = false,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { equipStyle(null) },
                )
                if (showBlackTheme) {
                    ProfileThemeCard(
                        title = "Black Theme",
                        subtitle = sh("Koleksiyonunda", "In your collection"),
                        active = darkActive,
                        enabled = !busy && !loading,
                        blackVariant = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { equipStyle(ownedDarkThemeId ?: BlackThemeId) },
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
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

        if (category != "game_theme") {
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
            shown.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { item ->
                        OwnedStyleCard(
                            item = item,
                            active = equipped.isEquipped(item),
                            enabled = !loading && !busy,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onEquip = { equipStyle(item.id) },
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
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
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled && !active, onClick = onClick),
        shape = Hf.CardShape,
        color = Hf.Ground,
        border = BorderStroke(if (active) 2.dp else 1.5.dp, if (active) Hf.Green else Hf.Gold.copy(alpha = .75f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.fillMaxWidth().height(84.dp).background(
                    brush = if (blackVariant) {
                        Brush.linearGradient(listOf(Color(0xFF050608), Color(0xFF111318), Color(0xFF20242B)))
                    } else {
                        Brush.linearGradient(listOf(Hf.Ground, Hf.Surface, Hf.Gold.copy(alpha = .45f)))
                    },
                    shape = RoundedCornerShape(12.dp),
                ),
            ) {
                if (active) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = Hf.GreenLight,
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(22.dp),
                    )
                }
            }
            Text(title, color = Hf.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Hf.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            ProfileUseButton(active = active, enabled = enabled, onClick = onClick)
        }
    }
}

/** Kullan / Kullanılıyor pill from the 06 preview. */
@Composable
private fun ProfileUseButton(active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled && !active,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        shape = Hf.PillShape,
        color = if (active) Hf.Green else Hf.Ivory,
        border = if (active) BorderStroke(1.5.dp, Hf.GreenLight) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (active) sh("Kullanılıyor", "Equipped") else sh("Kullan", "Equip"),
                color = if (active) Hf.Ivory else Hf.Ink,
                fontSize = 15.sp,
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
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                    if (item.kind == "profile_frame" && supported) {
                        Icon(Icons.Rounded.Person, null, Modifier.size(40.dp), tint = Hf.TextMuted)
                        PurchasedProfileFrameOverlay(frameId = item.id, modifier = Modifier.size(104.dp))
                    } else {
                        StoreProductPreview(item, Modifier.fillMaxSize())
                    }
                }
                Text(sh(item.nameTr, item.nameEn), color = Hf.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (!item.active) {
                    Text(sh("Arşiv ürünü • Koleksiyonunda", "Retired item • In your collection"), color = Hf.TextMuted, fontSize = 11.sp)
                }
                if (!supported) {
                    Text(
                        sh(
                            "Bu sürümde kullanılamıyor. Sahipliğin korunuyor.",
                            "Unavailable in this version. You still own this item.",
                        ),
                        color = Hf.TextMuted,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.weight(1f, fill = false))
                Button(
                    onClick = onEquip,
                    enabled = enabled && supported && !active,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    shape = Hf.PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Hf.Ivory,
                        contentColor = Hf.Ink,
                        disabledContainerColor = if (active) Hf.Green else Hf.Disabled,
                        disabledContentColor = Hf.Ivory,
                    ),
                ) {
                    Text(if (active) sh("Kullanılıyor", "Equipped") else sh("Kullan", "Equip"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (active) {
                Surface(Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp), shape = androidx.compose.foundation.shape.CircleShape, color = Hf.Green, border = BorderStroke(1.5.dp, Hf.GreenLight)) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Hf.Ivory, modifier = Modifier.padding(3.dp))
                }
            }
        }
    }
}
