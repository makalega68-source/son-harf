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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyShopScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
    onMembershipChanged: (Boolean) -> Unit = {},
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    var rewards by remember { mutableStateOf(false) }
    if (rewards) {
        Column {
            TextButton(onClick = { rewards = false }) { Text(sh("Mağazaya dön", "Back to shop")) }
            RewardCenterScreen()
        }
        return
    }
    Column(Modifier.fillMaxSize().background(SonHarfBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = SonHarfText)
            }
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(sh("Mağaza", "Shop"), color = SonHarfText, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
                Text(sh("Kelime Kuşatması · Tarzını seç", "Word Siege · Make it yours"), color = SonHarfMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp, containerColor = Color.Transparent, divider = {}) {
            listOf(sh("Öne Çıkan", "Featured"), sh("Sezon", "Season"), sh("Görünümler", "Styles"), sh("Maskotlar", "Mascots"), "PRO")
                .forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = {
                        Text(label, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium)
                    })
                }
        }
        Box(Modifier.weight(1f)) {
            if (tab == 1) SeasonCenterContent()
            else EconomyCatalogScreen(tab, { tab = it }, { rewards = true }, onMembershipChanged)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EconomyCatalogScreen(section: Int, onSection: (Int) -> Unit, onRewards: () -> Unit, onMembershipChanged: (Boolean) -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var storefront by remember { mutableStateOf<StorefrontDto?>(null) }
    var showCoins by remember { mutableStateOf(false) }
    var selectedBundle by remember { mutableStateOf<StoreBundleDto?>(null) }
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
            onMembershipChanged(profile?.isVip == true)
            storefront = b.getStorefront()
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

    val filtered = when (section) {
        2 -> items
        3 -> items.filter { it.kind == "mascot" }
        4 -> items.filter { it.vipOnly }
        else -> emptyList()
    }
    val bundles = storefront?.bundles.orEmpty().filter { b -> b.items.isNotEmpty() && b.items.all { it.isRuntimeReadyStyle() } }


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
                onCoins = { showCoins = true },
            )
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfTheme.Primary) }

        item {
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(enabled = busy == null && !loading, onClick = {
                    scope.launch { loading = true; notice = null; reload(); loading = false }
                }) { Text(sh("Yenile", "Refresh"), fontSize = 12.sp) }
            }
        }
        if (section == 0) {
            item {
                StoreDailyRewardCard(storefront, busy != null || loading) {
                    val b = backend
                    if (b != null && busy == null && storefront?.dailyClaimed == false) scope.launch {
                        busy = "daily"
                        runCatching { b.claimDailyCheckin() }
                            .onSuccess { amount ->
                                notice = if (amount > 0) sh("+$amount Son Coin eklendi.", "+$amount Son Coin added.")
                                    else sh("Bugünkü ödülünü aldın.", "You already claimed today's gift.")
                                reload()
                            }.onFailure { notice = sh("Ödül alınamadı. Tekrar deneyebilirsin.", "Could not claim the gift. Please retry.") }
                        busy = null
                    }
                }
            }
            item { ProShopCard(profile?.isVip == true) { onSection(4) } }
            item { StorePromoCard(sh("Sezon Bileti", "Season Pass"), sh("Sezonu ve ödül yolunu keşfet", "Explore the season and reward track"), sh("İncele", "Explore")) { onSection(1) } }
            bundles.firstOrNull { it.section == "starter" }?.let { bundle ->
                item { StoreBundleCard(bundle, owned, busy != null || loading) { selectedBundle = bundle } }
            }
            if (items.isNotEmpty()) {
                item { VerifiedProductsHero(items, owned) }
                item { TextButton(onClick = { onSection(2) }) { Text(sh("Tüm görünümler", "All styles")) } }
            }
            bundles.filter { it.section == "limited" }.forEach { bundle ->
                item { StoreBundleCard(bundle, owned, busy != null || loading) { selectedBundle = bundle } }
            }
            if (storefront?.rewardedEnabled == true) item { TextButton(onClick = onRewards) { Text(sh("İsteğe bağlı reklam ödülleri", "Optional ad rewards")) } }
        }
        if (section == 4) {
            item { ProShopCard(profile?.isVip == true) { showVip = true } }
            item { StoreProBenefits() }
        }
        if (filtered.isEmpty() && !loading && section in setOf(2, 3)) {
            item {
                Surface(
                    color = SonHarfSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SonHarfTheme.Border),
                ) {
                    Text(
                        if (section == 3) sh("Maskot koleksiyonu hazırlanıyor.", "The mascot collection is on its way.") else sh("Şu anda satışta görünüm yok.", "No styles are on sale right now."),
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
                busy = busy != null || loading,
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
                                val applied = runCatching { b.equipShopItem(item.id) }.isSuccess
                                notice = if (applied) sh("Satın alındı ve uygulandı.", "Purchased and equipped.")
                                    else sh("Satın alındı. Koleksiyonundan kullanabilirsin.", "Purchased. You can equip it from your collection.")
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

    if (showVip) VipPurchaseDialog(onVerified = { scope.launch { reload() } }) { showVip = false }
    if (showCoins) ModalBottomSheet(onDismissRequest = { showCoins = false }, containerColor = SonHarfSurface) {
        GooglePlayProductsCard { scope.launch { reload() } }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
    selectedBundle?.let { bundle ->
        AlertDialog(
            onDismissRequest = { if (busy == null) selectedBundle = null },
            title = { Text(sh(bundle.nameTr, bundle.nameEn)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bundle.items.forEach { p -> Text(sh(p.nameTr, p.nameEn) + if (p.id in owned) sh(" · Sende var", " · Owned") else "") }
                Text("${bundle.diamondPrice} Son Coin", fontWeight = FontWeight.Bold)
                Text(sh("Sende olan ürünler tekrar verilmez.", "Already owned items are not granted again."), fontSize = 12.sp)
            } },
            confirmButton = { TextButton(enabled = busy == null, onClick = {
                val b = backend
                if (b != null && busy == null) scope.launch {
                    busy = bundle.id
                    runCatching { b.purchaseStoreBundle(bundle.id) }
                        .onSuccess { notice = sh("Paket koleksiyonuna eklendi.", "Bundle added to your collection."); reload() }
                        .onFailure { error ->
                            notice = if ("insufficient_diamonds" in error.message.orEmpty()) sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                else sh("Paket alınamadı. Teklifi yenileyip tekrar dene.", "Could not purchase. Refresh the offer and retry.")
                        }
                    selectedBundle = null
                    busy = null
                }
            }) { Text(sh("Satın al", "Buy")) } },
            dismissButton = { TextButton(enabled = busy == null, onClick = { selectedBundle = null }) { Text(sh("Vazgeç", "Cancel")) } },
        )
    }
}

@Composable
private fun StoreCollectionHeader(balance: Int, ownedCount: Int, total: Int, onCoins: () -> Unit) {
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
                Text(sh("Koleksiyon", "Collection"), color = SonHarfText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(sh("Görünüm · konfor · prestij", "Appearance · comfort · prestige"), color = SonHarfMuted, fontSize = 10.sp)
                Text(sh("Koleksiyon $ownedCount/$total", "Collection $ownedCount/$total"), color = SonHarfTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Surface(onClick = onCoins, shape = RoundedCornerShape(99.dp), color = SonHarfGold.copy(alpha = .14f), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .30f))) {
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
            Text(sh("Yeni görünümler", "New styles"), color = SonHarfTheme.Primary, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(sh("Profiline küçük, güçlü bir dokunuş.", "A distinctive touch for your profile."), color = SonHarfMuted, fontSize = 9.sp)
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
            StoreProductPreview(item, Modifier.size(76.dp))
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
