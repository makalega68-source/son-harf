package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
fun EconomyShopScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    Column(Modifier.fillMaxSize().background(SonHarfBg)) {
        if (onBack != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back"), tint = SonHarfText)
                }
                Column {
                    Text(sh("SON HARF MAĞAZASI", "SON HARF SHOP"), color = SonHarfText, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(sh("Gerçek ürünler • kalıcı koleksiyon • adil oyun", "Real products • permanent collection • fair play"), color = SonHarfMuted, fontSize = 9.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(sh("MAĞAZA", "SHOP"), fontSize = 10.sp) }, modifier = Modifier.weight(1f))
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(sh("ÖDÜLLER", "REWARDS"), fontSize = 10.sp) }, modifier = Modifier.weight(1f))
            FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text(sh("SEZON", "SEASON"), fontSize = 10.sp) }, modifier = Modifier.weight(1f))
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> EconomyCatalogScreen()
                1 -> RewardCenterScreen()
                else -> SeasonCenterContent()
            }
        }
    }
}

@Composable
private fun EconomyCatalogScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var category by remember { mutableIntStateOf(0) }
    var showVip by remember { mutableStateOf(false) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            notice = sh("Mağaza sunucu bağlantısı olmadan kullanılamaz.", "The shop requires a server connection.")
            return
        }
        val id = b.currentUserId()
        if (id == null) {
            notice = sh("Oyuncu oturumu hazırlanamadı.", "Player session is not ready.")
            return
        }
        runCatching {
            profile = b.getProfile(id)
            items = b.getShopItems().filter { it.isRuntimeReadyStyle() }
            owned = b.getInventory()
            equipped = b.getEquippedCosmetics()
            SonHarfCosmetics.apply(equipped)
        }.onFailure {
            notice = sh("Mağaza verileri yüklenemedi.", "Shop data could not be loaded.")
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        reload()
        loading = false
    }

    fun isEquipped(item: ShopItemDto): Boolean = equipped.isEquipped(item)

    val filtered = items.filter { item ->
        when (category) {
            1 -> item.kind == "profile_frame"
            2 -> item.kind in setOf("game_theme", "keyboard_theme")
            3 -> item.kind == "name_style"
            else -> true
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StoreCollectionHeader(
                balance = profile?.diamonds ?: 0,
                ownedCount = items.count { it.id in owned },
                total = items.size,
            )
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfTheme.Primary) }

        if (items.isNotEmpty()) {
            item { VerifiedProductsHero(items = items, owned = owned) }
        }

        item {
            ScrollableTabRow(selectedTabIndex = category, edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}) {
                listOf(
                    sh("VİTRİN", "FEATURED"),
                    sh("ÇERÇEVELER", "FRAMES"),
                    sh("TEMALAR", "THEMES"),
                    sh("PROFİL", "PROFILE"),
                ).forEachIndexed { index, label ->
                    Tab(
                        selected = category == index,
                        onClick = { category = index },
                        text = {
                            Text(
                                label,
                                color = if (category == index) SonHarfTheme.Primary else SonHarfMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                        },
                    )
                }
            }
        }

        if (filtered.isEmpty() && !loading) {
            item {
                Surface(
                    color = SonHarfSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SonHarfTheme.Border),
                ) {
                    Text(
                        sh("Bu kategoride doğrulanmış, oyunda gerçekten çalışan ürün henüz yok.", "There is no verified, fully working in-game product in this category yet."),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = SonHarfMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { item ->
            val mine = item.id in owned
            val active = isEquipped(item)
            VerifiedStoreProductCard(
                item = item,
                owned = mine,
                equipped = active,
                busy = busy != null,
                proActive = profile?.isVip == true,
            ) {
                val b = backend
                if (b == null || busy != null) return@VerifiedStoreProductCard
                scope.launch {
                    busy = item.id
                    val displayName = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
                    if (mine) {
                        runCatching { b.equipShopItem(item.id) }
                            .onSuccess {
                                notice = sh("$displayName kullanıma alındı.", "$displayName equipped.")
                                reload()
                            }
                            .onFailure { notice = sh("Ürün etkinleştirilemedi.", "The item could not be equipped.") }
                    } else {
                        runCatching { b.purchaseShopItem(item.id) }
                            .onSuccess {
                                runCatching { b.equipShopItem(item.id) }
                                notice = sh("Satın alma tamamlandı ve ürün uygulandı.", "Purchase complete and the item was equipped.")
                                reload()
                            }
                            .onFailure {
                                val raw = it.message.orEmpty()
                                notice = when {
                                    "insufficient_diamonds" in raw -> sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                    "vip_required" in raw -> sh("Bu ürün PRO üyelerine özel.", "This item is exclusive to PRO members.")
                                    "already_owned" in raw -> sh("Bu ürüne zaten sahipsin.", "You already own this item.")
                                    else -> sh("Satın alma tamamlanamadı.", "Purchase failed.")
                                }
                            }
                    }
                    busy = null
                }
            }
        }

        item { MainSectionTitle(sh("PRO VE SON COIN", "PRO & SON COIN")) }
        item { ProShopCard(profile?.isVip == true) { showVip = true } }
        item { GooglePlayProductsCard { scope.launch { reload() } } }
        item { SeasonPassPurchaseCard { scope.launch { reload() } } }

        item {
            Surface(
                color = SonHarfTheme.SurfaceSecondary,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SonHarfTheme.Border),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = SonHarfTheme.Success, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(sh("ADİL OYUN SÖZÜ", "FAIR PLAY PROMISE"), color = SonHarfText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(sh("Mağaza ürünleri maç gücü, skor veya rating avantajı sağlamaz.", "Store products never provide match power, score, or rating advantages."), color = SonHarfMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = SonHarfSurface2, shape = RoundedCornerShape(15.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfText, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }

    if (showVip) VipPurchaseDialog { showVip = false }
}

@Composable
private fun StoreCollectionHeader(balance: Int, ownedCount: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfTheme.Surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = SonHarfTheme.PrimarySoft) {
                Icon(Icons.Rounded.Storefront, null, Modifier.padding(11.dp).size(27.dp), tint = SonHarfTheme.Primary)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("MAĞAZA", "SHOP"), color = SonHarfText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(sh("Yalnızca gerçek ve oyunda çalışan ürünler", "Only real, working in-game products"), color = SonHarfMuted, fontSize = 10.sp)
                Text(sh("Koleksiyon $ownedCount/$total", "Collection $ownedCount/$total"), color = SonHarfTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(99.dp), color = SonHarfGold.copy(alpha = .14f), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .30f))) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Toll, null, Modifier.size(16.dp), tint = SonHarfGold)
                    Spacer(Modifier.width(4.dp))
                    Text("$balance SC", color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun VerifiedProductsHero(items: List<ShopItemDto>, owned: Set<String>) {
    val featured = items.filter { it.kind == "profile_frame" }.take(3)
    if (featured.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfTheme.PrimarySoft.copy(alpha = .62f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .25f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(sh("GERÇEK KOLEKSİYON", "REAL COLLECTION"), color = SonHarfTheme.Primary, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(sh("Aşağıdaki önizlemelerde gördüğün çerçeveler oyunda kullanılan aynı grafik dosyalarıdır.", "The frames below use the exact same artwork used in the game."), color = SonHarfMuted, fontSize = 9.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                featured.forEach { item ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        StoreProductPreview(item, Modifier.fillMaxWidth().height(82.dp))
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (item.id in owned) sh("SAHİPSİN", "OWNED") else if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr,
                            color = if (item.id in owned) SonHarfTheme.Success else SonHarfText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifiedStoreProductCard(
    item: ShopItemDto,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    proActive: Boolean,
    onAction: () -> Unit,
) {
    val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
    val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
    val lockedByPro = item.vipOnly && !proActive && !owned

    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfTheme.Surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) SonHarfTheme.Success else SonHarfTheme.Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreProductPreview(item, Modifier.size(92.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), color = SonHarfText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (equipped) Icon(Icons.Rounded.CheckCircle, null, tint = SonHarfTheme.Success, modifier = Modifier.size(18.dp))
                }
                Text(description, color = SonHarfMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.vipOnly) Text("PRO", color = SonHarfGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            equipped -> sh("AKTİF", "ACTIVE")
                            owned -> sh("SAHİPSİN", "OWNED")
                            else -> "${item.diamondPrice} SC"
                        },
                        color = if (owned) SonHarfTheme.Success else SonHarfGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onAction,
                        enabled = !busy && !equipped && !lockedByPro,
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary),
                    ) {
                        Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when {
                                equipped -> sh("AKTİF", "ACTIVE")
                                owned -> sh("KULLAN", "EQUIP")
                                lockedByPro -> "PRO"
                                else -> sh("SATIN AL", "BUY")
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProShopCard(active: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MainUi.BlueSoft),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .25f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = Color.White) {
                        Icon(Icons.Rounded.WorkspacePremium, null, Modifier.padding(10.dp).size(28.dp), tint = SonHarfGold)
                    }
                    Column {
                        Text("PRO", color = MainUi.Blue, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(sh("Reklamsız + kozmetik + analiz", "Ad-free + cosmetics + analysis"), color = SonHarfMuted, fontSize = 9.sp)
                    }
                }
                Text(if (active) sh("AKTİF", "ACTIVE") else sh("KEŞFET ›", "EXPLORE ›"), color = if (active) SonHarfTheme.Success else MainUi.Blue, fontWeight = FontWeight.Black)
            }
            Text(sh("Özel oda • özel kozmetik • gelişmiş istatistik • reklamsız deneyim • sosyal ayrıcalıklar", "Private rooms • exclusive cosmetics • advanced stats • ad-free experience • social benefits"), color = SonHarfText, fontSize = 10.sp)
            Text(sh("PRO, dereceli maçlarda skor, kelime ipucu veya rating avantajı vermez.", "PRO provides no score, word-hint, or rating advantage in ranked matches."), color = SonHarfTheme.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
