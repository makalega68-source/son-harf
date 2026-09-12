package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private val StoreBg: Color get() = SonHarfTheme.Background
private val StoreSurface: Color get() = SonHarfTheme.Surface
private val StoreAlt: Color get() = SonHarfTheme.SurfaceSecondary
private val StoreBlue: Color get() = SonHarfTheme.PrimaryBlue
private val StoreText: Color get() = SonHarfTheme.TextPrimary
private val StoreMuted: Color get() = SonHarfTheme.TextSecondary
private val StoreBorder: Color get() = SonHarfTheme.Border
private val StoreGreen: Color get() = SonHarfTheme.Success
private val StoreGold: Color get() = SonHarfTheme.Warning
private const val StoreTimeout = 12_000L
private data class StoreTab(val title: String, val kinds: Set<String>, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
internal fun MonsterStyleStoreScreen() {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) runCatching { OnlineGameBackend() }.getOrNull() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var catalog by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        loading = true
        val b = backend
        if (b == null) {
            notice = sh("Mağazaya bağlanılamadı. Bağlantını kontrol edip tekrar dene.", "The store is unavailable. Check your connection and try again.")
        } else {
            runCatching {
                withTimeout(StoreTimeout) {
                    val user = b.currentUserId() ?: error("unauthorized")
                    profile = b.getProfile(user)
                    catalog = b.getShopItems().filter { it.isRuntimeReadyStyle() }
                    owned = b.getInventory()
                    equipped = b.getEquippedCosmetics()
                    SonHarfCosmetics.applyAndPersist(context, equipped)
                }
            }.onFailure {
                if (it is CancellationException && it !is TimeoutCancellationException) throw it
                notice = sh("Mağaza verileri alınamadı. Lütfen tekrar dene.", "Store data could not be loaded. Please try again.")
            }
        }
        loading = false
    }

    fun buyAndEquip(item: ShopItemDto) {
        val b = backend ?: return
        if (busy != null || loading) return
        busy = item.id
        scope.launch {
            runCatching {
                withTimeout(StoreTimeout) {
                    if (item.id !in owned) b.purchaseShopItem(item.id)
                    b.equipShopItem(item.id)
                }
            }.onSuccess {
                notice = sh("${item.nameTr} anında uygulandı.", "${item.nameEn} was applied instantly.")
                reload()
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                reload()
                notice = when {
                    "insufficient_diamonds" in error.message.orEmpty() -> sh("Yeterli Son Coin'in yok.", "You do not have enough Son Coin.")
                    "vip_required" in error.message.orEmpty() -> sh("Bu görünüm PRO üyelerine özel.", "This style is exclusive to PRO members.")
                    item.id in owned -> sh("Ürün koleksiyonunda. Uygulamak için KULLAN düğmesine dokun.", "The item is in your collection. Tap EQUIP to apply it.")
                    else -> sh("İşlem doğrulanamadı. Bakiyeyi ve koleksiyonu yenileyip tekrar dene.", "The action could not be confirmed. Refresh your balance and collection before retrying.")
                }
            }
            busy = null
        }
    }

    LaunchedEffect(backend) { reload() }

    val tabs = listOf(
        StoreTab(sh("VİTRİN", "FEATURED"), setOf("game_theme", "profile_frame", "name_style", "keyboard_theme"), Icons.Rounded.AutoAwesome),
        StoreTab(sh("ÇERÇEVELER", "FRAMES"), setOf("profile_frame"), Icons.Rounded.AccountCircle),
        StoreTab(sh("TEMALAR", "THEMES"), setOf("game_theme", "keyboard_theme"), Icons.Rounded.Palette),
        StoreTab(sh("PROFİL", "PROFILE"), setOf("name_style"), Icons.Rounded.Title),
    )
    val visible = catalog.filter { it.kind in tabs[tab].kinds }
    val featured = catalog.firstOrNull { it.kind == "profile_frame" }
        ?: catalog.firstOrNull { it.kind == "game_theme" }
        ?: catalog.firstOrNull()

    Surface(Modifier.fillMaxSize(), color = StoreBg) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { StoreHeader(profile?.diamonds ?: 0, catalog.count { it.id in owned }, catalog.size) }
            item { StoreTabs(tabs, tab) { tab = it } }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = StoreBlue) }
            notice?.let { item { StoreNotice(it) } }
            item {
                TextButton(enabled = !loading && busy == null, onClick = { scope.launch { notice = null; reload() } }) {
                    Text(sh("BAKİYEYİ VE KOLEKSİYONU YENİLE", "REFRESH BALANCE & COLLECTION"))
                }
            }
            if (tab == 0 && featured != null) {
                item {
                    FeaturedCard(
                        featured,
                        featured.id in owned,
                        equipped.isEquipped(featured),
                        busy != null || loading || (featured.vipOnly && profile?.isVip != true && featured.id !in owned),
                        ::buyAndEquip,
                    )
                }
                item { StoreSectionHeader(sh("GERÇEK ÜRÜNLER", "REAL PRODUCTS"), sh("Önizleme ve oyundaki görünüm aynı kaynaktan gelir.", "Preview and in-game appearance use the same source.")) }
            } else {
                item { StoreSectionHeader(tabs[tab].title, sh("Kalıcı kozmetik koleksiyonu", "Permanent cosmetic collection")) }
            }
            if (visible.isEmpty() && !loading) item { EmptyCatalogCard() }
            items(visible.filter { tab != 0 || it.id != featured?.id }, key = { it.id }) { item ->
                ProductCard(
                    item,
                    item.id in owned,
                    equipped.isEquipped(item),
                    busy != null || loading || (item.vipOnly && profile?.isVip != true && item.id !in owned),
                    ::buyAndEquip,
                )
            }
            item { FairPlayCard() }
        }
    }
}

internal fun ShopItemDto.isRuntimeReadyStyle(): Boolean = active && when (kind) {
    "game_theme" -> id == "theme_dark_arena"
    "profile_frame" -> id in PurchasedFrameCatalog.ids
    "name_style" -> id == "name_cyan"
    "keyboard_theme" -> id == "keyboard_neon"
    // Effects, emoji and action products remain hidden until a real runtime asset is connected.
    else -> false
}

internal fun EquippedCosmeticsDto?.isEquipped(item: ShopItemDto): Boolean = when (item.kind) {
    "game_theme" -> this?.gameThemeId == item.id
    "profile_frame" -> this?.profileFrameId == item.id
    "name_style" -> this?.nameStyleId == item.id
    "keyboard_theme" -> this?.keyboardThemeId == item.id
    "victory_effect" -> this?.victoryEffectId == item.id
    "emoji_pack" -> this?.emojiPackId == item.id
    else -> false
}

@Composable
private fun StoreHeader(balance: Int, collected: Int, total: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = StoreSurface), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, StoreBorder)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = StoreBlue.copy(.12f)) {
                Icon(Icons.Rounded.Storefront, null, Modifier.padding(11.dp).size(27.dp), tint = StoreBlue)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("STYLE MAĞAZASI", "STYLE STORE"), color = StoreText, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(sh("Gerçek ürün • gerçek önizleme • kalıcı koleksiyon", "Real product • real preview • permanent collection"), color = StoreMuted, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp))
                Text(sh("Koleksiyon $collected/$total", "Collection $collected/$total"), color = StoreBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(99.dp), color = StoreGold.copy(.14f), border = BorderStroke(1.dp, StoreGold.copy(.35f))) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Toll, null, Modifier.size(16.dp), tint = StoreGold)
                    Spacer(Modifier.width(4.dp))
                    Text("$balance SC", color = StoreText, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StoreTabs(tabs: List<StoreTab>, selected: Int, onSelected: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs) { tab ->
            val index = tabs.indexOf(tab)
            FilterChip(
                selected = selected == index,
                onClick = { onSelected(index) },
                label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.Black) },
                leadingIcon = { Icon(tab.icon, null, Modifier.size(15.dp)) },
            )
        }
    }
}

@Composable
private fun FeaturedCard(item: ShopItemDto, owned: Boolean, equipped: Boolean, busy: Boolean, onAction: (ShopItemDto) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StoreSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) StoreGreen else StoreBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            StoreProductPreview(item, Modifier.fillMaxWidth().height(150.dp), expanded = true)
            ProductInfo(item, owned, equipped, busy, onAction)
        }
    }
}

@Composable
private fun ProductCard(item: ShopItemDto, owned: Boolean, equipped: Boolean, busy: Boolean, onAction: (ShopItemDto) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StoreSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) StoreGreen else StoreBorder),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreProductPreview(item, Modifier.size(76.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { ProductInfo(item, owned, equipped, busy, onAction) }
        }
    }
}

@Composable
private fun ProductInfo(item: ShopItemDto, owned: Boolean, equipped: Boolean, busy: Boolean, onAction: (ShopItemDto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sh(item.nameTr, item.nameEn),
                Modifier.weight(1f),
                color = StoreText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (equipped) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = StoreGreen)
        }
        if (item.vipOnly) Text("PRO", color = StoreGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(sh(item.descriptionTr, item.descriptionEn), color = StoreMuted, fontSize = 11.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(99.dp), color = if (owned) StoreGreen.copy(.11f) else StoreGold.copy(.13f)) {
                Text(
                    if (equipped) sh("AKTİF", "ACTIVE") else if (owned) sh("SAHİPSİN", "OWNED") else "${item.diamondPrice} SC",
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = if (owned) StoreGreen else StoreText,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                enabled = !busy && !equipped,
                onClick = { onAction(item) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StoreBlue),
            ) {
                Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (equipped) sh("AKTİF", "ACTIVE") else if (owned) sh("KULLAN", "EQUIP") else sh("SATIN AL", "BUY"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun StoreSectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = StoreText, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = StoreMuted, fontSize = 11.sp)
    }
}

@Composable
private fun StoreNotice(message: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = StoreBlue.copy(.08f), border = BorderStroke(1.dp, StoreBlue.copy(.2f))) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, Modifier.size(16.dp), tint = StoreBlue)
            Spacer(Modifier.width(8.dp))
            Text(message, color = StoreText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EmptyCatalogCard() {
    Surface(shape = RoundedCornerShape(18.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) {
        Text(
            sh("Bu bölümde oyunda gerçekten çalışan ürün henüz yok. Hazır olmayan ürün gösterilmiyor.", "There is no fully working in-game product in this section yet. Unready products stay hidden."),
            Modifier.padding(18.dp),
            color = StoreMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun FairPlayCard() {
    Surface(shape = RoundedCornerShape(18.dp), color = StoreAlt, border = BorderStroke(1.dp, StoreBorder)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(24.dp), tint = StoreGreen)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(sh("ADİL OYUN SÖZÜ", "FAIR PLAY PROMISE"), color = StoreText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(sh("Mağazadaki tüm ürünler kozmetiktir; maç gücü veya puan avantajı sağlamaz.", "Every store item is cosmetic; it provides no match power or score advantage."), color = StoreMuted, fontSize = 11.sp)
            }
        }
    }
}
