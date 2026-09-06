package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private val StoreBg = SonHarfTheme.Background
private val StoreSurface = SonHarfTheme.Surface
private val StoreAlt = SonHarfTheme.SurfaceSecondary
private val StoreBlue = SonHarfTheme.PrimaryBlue
private val StoreText = SonHarfTheme.TextPrimary
private val StoreMuted = SonHarfTheme.TextSecondary
private val StoreBorder = SonHarfTheme.Border
private val StoreGreen = SonHarfTheme.Success
private val StoreGold = SonHarfTheme.Warning
private const val StoreTimeout = 12_000L
private data class StoreTab(val title: String, val kinds: Set<String>, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
internal fun MonsterStyleStoreScreen() {
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
        if (b == null) notice = sh("Mağazaya bağlanılamadı. Bağlantını kontrol edip tekrar dene.", "The store is unavailable. Check your connection and try again.")
        else runCatching {
            withTimeout(StoreTimeout) {
                val user = b.currentUserId() ?: error("unauthorized")
                profile = b.getProfile(user)
                catalog = b.getShopItems().filter { it.isRuntimeReadyStyle() } // theme_monster_blue is the active sale theme.
                owned = b.getInventory()
                equipped = b.getEquippedCosmetics().also { SonHarfCosmetics.apply(it) }
            }
        }.onFailure { notice = sh("Mağaza verileri alınamadı. Lütfen tekrar dene.", "Store data could not be loaded. Please try again.") }
        loading = false
    }
    fun buyAndEquip(item: ShopItemDto) {
        val b = backend ?: return
        scope.launch {
            busy = item.id
            runCatching {
                withTimeout(StoreTimeout) { if (item.id !in owned) b.purchaseShopItem(item.id); b.equipShopItem(item.id) }
            }.onSuccess {
                notice = sh("${item.nameTr} anında uygulandı.", "${item.nameEn} was applied instantly."); reload()
            }.onFailure { error ->
                notice = when {
                    "insufficient_diamonds" in error.message.orEmpty() -> sh("Yeterli Son Coin'in yok.", "You do not have enough Son Coin.")
                    "vip_required" in error.message.orEmpty() -> sh("Bu görünüm VIP üyelerine özel.", "This style is exclusive to VIP members.")
                    else -> sh("İşlem tamamlanamadı. Son Coin'in harcanmadıysa tekrar deneyebilirsin.", "The action could not be completed. You can safely try again if your Son Coin was not charged.")
                }
            }
            busy = null
        }
    }
    LaunchedEffect(backend) { reload() }
    val tabs = listOf(
        StoreTab(sh("VİTRİN", "FEATURED"), setOf("game_theme", "profile_frame"), Icons.Rounded.AutoAwesome),
        StoreTab(sh("TEMALAR", "THEMES"), setOf("game_theme", "keyboard_theme"), Icons.Rounded.Palette),
        StoreTab(sh("PROFİL", "PROFILE"), setOf("profile_frame", "name_style"), Icons.Rounded.AccountCircle),
        StoreTab(sh("MAÇ", "MATCH"), setOf("victory_effect", "emoji_pack"), Icons.Rounded.EmojiEvents),
    )
    val visible = catalog.filter { it.kind in tabs[tab].kinds }
    Surface(Modifier.fillMaxSize(), color = StoreBg) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { StoreHeader(profile?.diamonds ?: 0, owned.size, catalog.size) }
            item { StoreTabs(tabs, tab) { tab = it } }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape), color = StoreBlue) }
            notice?.let { item { StoreNotice(it) } }
            if (tab == 0) {
                catalog.firstOrNull { it.id == "theme_monster_blue" }?.let { item { FeaturedCard(it, it.id in owned, equipped.isEquipped(it), busy == it.id, ::buyAndEquip) } }
                item { StoreSectionHeader(sh("HEMEN UYGULANABİLİR", "READY TO APPLY"), sh("Satın al, tek dokunuşla görünümünü değiştir.", "Buy and change your look in one tap.")) }
            } else item { StoreSectionHeader(tabs[tab].title, sh("Kalıcı kozmetik koleksiyonu", "Permanent cosmetic collection")) }
            if (visible.isEmpty() && !loading) item { EmptyCatalogCard() }
            items(visible.filter { tab != 0 || it.id != "theme_monster_blue" }, key = { it.id }) { item -> ProductCard(item, item.id in owned, equipped.isEquipped(item), busy == item.id, ::buyAndEquip) }
            item { PurchasedProfileFramesStoreRow(backend = backend) }
            item { FairPlayCard() }
        }
    }
}
private fun ShopItemDto.isRuntimeReadyStyle(): Boolean = when (kind) {
    "game_theme" -> id == "theme_monster_blue"
    "profile_frame" -> id in PurchasedFrameCatalog.ids
    "name_style" -> id == "name_cyan"; "keyboard_theme" -> id == "keyboard_neon"
    "victory_effect" -> id == "victory_crown"; "emoji_pack" -> id == "emoji_vip"; else -> false
}
private fun EquippedCosmeticsDto?.isEquipped(item: ShopItemDto): Boolean = when (item.kind) {
    "game_theme" -> this?.gameThemeId == item.id; "profile_frame" -> this?.profileFrameId == item.id
    "name_style" -> this?.nameStyleId == item.id; "keyboard_theme" -> this?.keyboardThemeId == item.id
    "victory_effect" -> this?.victoryEffectId == item.id; "emoji_pack" -> this?.emojiPackId == item.id; else -> false
}
@Composable private fun StoreHeader(balance: Int, collected: Int, total: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = StoreSurface), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, StoreBorder)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = StoreBlue.copy(.12f)) { Icon(Icons.Rounded.Storefront, null, Modifier.padding(11.dp).size(27.dp), tint = StoreBlue) }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(sh("STYLE MAĞAZASI", "STYLE STORE"), color = StoreText, fontSize = 19.sp, fontWeight = FontWeight.Black); Text(sh("Al • uygula • hemen oyuna dön", "Buy • equip • get back in game"), color = StoreMuted, fontSize = 10.sp); Spacer(Modifier.height(5.dp)); Text(sh("Koleksiyon $collected/$total", "Collection $collected/$total"), color = StoreBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            Surface(shape = RoundedCornerShape(99.dp), color = StoreGold.copy(.14f), border = BorderStroke(1.dp, StoreGold.copy(.35f))) { Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Toll, null, Modifier.size(16.dp), tint = StoreGold); Spacer(Modifier.width(4.dp)); Text("$balance SC", color = StoreText, fontWeight = FontWeight.Black, fontSize = 12.sp) } }
        }
    }
}
@Composable private fun StoreTabs(tabs: List<StoreTab>, selected: Int, onSelected: (Int) -> Unit) { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(tabs) { tab -> val index = tabs.indexOf(tab); FilterChip(selected = selected == index, onClick = { onSelected(index) }, label = { Text(tab.title, fontSize = 9.sp, fontWeight = FontWeight.Black) }, leadingIcon = { Icon(tab.icon, null, Modifier.size(15.dp)) }) } } }
@Composable private fun FeaturedCard(item: ShopItemDto, owned: Boolean, equipped: Boolean, busy: Boolean, onAction: (ShopItemDto) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = StoreSurface), shape = RoundedCornerShape(24.dp), border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) StoreGreen else StoreBorder)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) { Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(17.dp))) { Image(painterResource(R.drawable.store_featured_arena), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop); Surface(Modifier.padding(9.dp), color = StoreText.copy(.85f), shape = RoundedCornerShape(8.dp)) { Text(sh("ÖNE ÇIKAN", "FEATURED"), Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black) } }; ProductInfo(item, owned, equipped, busy, onAction) } }
}
@Composable private fun ProductCard(item: ShopItemDto, owned: Boolean, equipped: Boolean, busy: Boolean, onAction: (ShopItemDto) -> Unit) {
    val accent = accentFor(item)
    Card(colors = CardDefaults.cardColors(containerColor = StoreSurface), shape = RoundedCornerShape(20.dp), border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) StoreGreen else StoreBorder)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(16.dp), color = accent.copy(.12f), modifier = Modifier.size(72.dp)) { Box(contentAlignment = Alignment.Center) { Icon(iconFor(item), null, Modifier.size(34.dp), tint = accent); if (item.kind == "profile_frame") Surface(shape = CircleShape, color = Color.White.copy(.82f), border = BorderStroke(3.dp, accent), modifier = Modifier.size(23.dp)) {} } }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { ProductInfo(item, owned, equipped, busy, onAction) } } }
}
@Composable private fun ProductInfo(item: ShopItemDto, owned: Boolean, equipped: Boolean, busy: Boolean, onAction: (ShopItemDto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(item.nameTr, Modifier.weight(1f), color = StoreText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis); if (equipped) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = StoreGreen) }
        Text(item.descriptionTr, color = StoreMuted, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(99.dp), color = if (owned) StoreGreen.copy(.11f) else StoreGold.copy(.13f)) { Text(if (equipped) sh("AKTİF", "ACTIVE") else if (owned) sh("SAHİPSİN", "OWNED") else "${item.diamondPrice} SC", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (owned) StoreGreen else StoreText, fontWeight = FontWeight.Black, fontSize = 9.sp) }; Spacer(Modifier.weight(1f)); Button(enabled = !busy && !equipped, onClick = { onAction(item) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), shape = RoundedCornerShape(11.dp), colors = ButtonDefaults.buttonColors(containerColor = StoreBlue)) { Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(if (busy) "…" else if (owned) sh("KULLAN", "EQUIP") else sh("AL & KULLAN", "BUY & EQUIP"), fontSize = 9.sp, fontWeight = FontWeight.Black) } }
    }
}
private fun iconFor(item: ShopItemDto) = when (item.kind) { "game_theme" -> Icons.Rounded.Palette; "profile_frame" -> Icons.Rounded.AccountCircle; "name_style" -> Icons.Rounded.Title; "keyboard_theme" -> Icons.Rounded.Keyboard; "victory_effect" -> Icons.Rounded.EmojiEvents; else -> Icons.Rounded.EmojiEmotions }
private fun accentFor(item: ShopItemDto) = when (item.id) { "theme_aurora" -> Color(0xFF795CE8); "theme_monster_blue" -> StoreBlue; "victory_crown" -> StoreGold; "keyboard_neon", "name_cyan" -> Color(0xFF13A9B7); else -> Color(0xFF765CD7) }
// PROFILE STYLE • MATCH STYLE • PRESTIGE • BUNDLES: permanent cosmetic collections, no match power.
@Composable private fun StoreSectionHeader(title: String, subtitle: String) { Column { Text(title, color = StoreText, fontSize = 13.sp, fontWeight = FontWeight.Black); Text(subtitle, color = StoreMuted, fontSize = 9.sp) } }
@Composable private fun StoreNotice(message: String) { Surface(shape = RoundedCornerShape(14.dp), color = StoreBlue.copy(.08f), border = BorderStroke(1.dp, StoreBlue.copy(.2f))) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Info, null, Modifier.size(16.dp), tint = StoreBlue); Spacer(Modifier.width(8.dp)); Text(message, color = StoreText, fontSize = 10.sp) } } }
@Composable private fun EmptyCatalogCard() { Surface(shape = RoundedCornerShape(18.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) { Text(sh("Bu bölümdeki ürünler yakında hazır olacak.", "Products for this section will be ready soon."), Modifier.padding(18.dp), color = StoreMuted, fontSize = 11.sp) } }
@Composable private fun FairPlayCard() { Surface(shape = RoundedCornerShape(18.dp), color = StoreAlt, border = BorderStroke(1.dp, StoreBorder)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(24.dp), tint = StoreGreen); Spacer(Modifier.width(10.dp)); Column { Text(sh("ADİL OYUN SÖZÜ", "FAIR PLAY PROMISE"), color = StoreText, fontSize = 10.sp, fontWeight = FontWeight.Black); Text(sh("Mağazadaki tüm ürünler kozmetiktir; maç gücü veya puan avantajı sağlamaz.", "Every store item is cosmetic; it provides no match power or score advantage."), color = StoreMuted, fontSize = 9.sp) } } } }
