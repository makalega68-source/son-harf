package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    onCollection: () -> Unit = {},
    onPro: () -> Unit = {},
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var rewards by remember { mutableStateOf(false) }

    if (rewards) {
        Column(Modifier.fillMaxSize().background(GameColors.AppBackground)) {
            GameTopBar(
                title = gameText("Ödüller", "Rewards"),
                subtitle = gameText("İsteğe bağlı ödüller ve günlük kazanımlar", "Optional rewards and daily earnings"),
                onBack = { rewards = false },
            )
            Box(Modifier.weight(1f)) { RewardCenterScreen() }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(GameColors.AppBackground)) {
        GameTopBar(
            title = gameText("Mağaza", "Shop"),
            subtitle = gameText("Görünüm, konfor ve prestij", "Appearance, comfort and prestige"),
            onBack = onBack,
            trailing = {
                // Owned items live in the inventory, separate from the catalog.
                GameIconButton(
                    icon = Icons.Rounded.Inventory2,
                    description = gameText("Envanterim", "My inventory"),
                    onClick = onCollection,
                )
            },
        )
        SegmentedGameTabs(
            labels = listOf(
                gameText("Öne Çıkan", "Featured"),
                gameText("Kozmetik", "Cosmetics"),
                gameText("Koleksiyon", "Collections"),
                "PRO",
            ),
            selectedIndex = tab,
            onSelected = { tab = it },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.weight(1f)) {
            if (tab == 2) StoreCollectionsTab(
                catalog = { EconomyCatalogScreen(2, { tab = it }, { rewards = true }, onMembershipChanged, onCollection, onPro) },
                season = { SeasonCenterContent() },
            )
            else EconomyCatalogScreen(tab, { tab = it }, { rewards = true }, onMembershipChanged, onCollection, onPro)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EconomyCatalogScreen(
    section: Int,
    onSection: (Int) -> Unit,
    onRewards: () -> Unit,
    onMembershipChanged: (Boolean) -> Unit,
    onCollection: () -> Unit,
    onPro: () -> Unit,
) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    val twoColumnProducts = LocalConfiguration.current.screenWidthDp >= 380
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
    var selectedProduct by remember { mutableStateOf<ShopItemDto?>(null) }
    var kindFilter by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            notice = gameText("Mağaza sunucu bağlantısı olmadan kullanılamaz.", "The shop requires a server connection.")
            return
        }
        val id = b.currentUserId()
        if (id == null) {
            notice = gameText("Oyuncu oturumu hazırlanamadı.", "Player session is not ready.")
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
            notice = gameText("Mağaza verileri yüklenemedi.", "Shop data could not be loaded.")
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        reload()
        loading = false
    }

    fun isEquipped(item: ShopItemDto): Boolean = equipped.isEquipped(item)

    val filtered = when (section) {
        1 -> items.filter { kindFilter == null || it.kind == kindFilter }
        else -> emptyList()
    }

    // Purchase and equip are server-side RPCs; the client only reports their result.
    fun purchase(product: ShopItemDto) {
        val b = backend ?: return
        if (busy != null) return
        scope.launch {
            busy = product.id
            val displayName = if (SonHarfUiState.isEnglish) product.nameEn else product.nameTr
            runCatching { b.purchaseShopItem(product.id) }
                .onSuccess {
                    notice = gameText("$displayName satın alındı. Şimdi kullanabilirsin.", "$displayName purchased. You can equip it now.")
                    reload()
                }
                .onFailure {
                    val raw = it.message.orEmpty()
                    notice = when {
                        "insufficient_diamonds" in raw -> gameText("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                        "vip_required" in raw -> gameText("Bu ürün PRO üyelerine özel.", "This item is exclusive to PRO members.")
                        "already_owned" in raw -> gameText("Bu ürüne zaten sahipsin.", "You already own this item.")
                        else -> gameText("Satın alma tamamlanamadı.", "Purchase failed.")
                    }
                }
            busy = null
        }
    }

    fun equip(product: ShopItemDto) {
        val b = backend ?: return
        if (busy != null) return
        scope.launch {
            busy = product.id
            runCatching { b.equipShopItem(product.id) }
                .onSuccess {
                    notice = gameText("Görünüm kullanılıyor.", "Style equipped.")
                    reload()
                }
                .onFailure { notice = gameText("Görünüm uygulanamadı.", "Could not equip the style.") }
            busy = null
        }
    }
    val bundles = storefront?.bundles.orEmpty().filter { bundle ->
        bundle.items.isNotEmpty() && bundle.items.all { it.isRuntimeReadyStyle() }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
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

        if (loading) item {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = GameColors.PrimaryBlue,
                trackColor = GameColors.SecondarySurface,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GameIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = gameText("Mağazayı yenile", "Refresh shop"),
                    onClick = {
                        if (busy == null && !loading) scope.launch {
                            loading = true
                            notice = null
                            reload()
                            loading = false
                        }
                    },
                    tint = if (busy == null && !loading) GameColors.PrimaryBlue else GameColors.DisabledContent,
                )
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
                                notice = if (amount > 0) gameText("+$amount Son Coin eklendi.", "+$amount Son Coin added.")
                                else gameText("Bugünkü ödülünü aldın.", "You already claimed today's gift.")
                                reload()
                            }
                            .onFailure {
                                notice = gameText("Ödül alınamadı. Tekrar deneyebilirsin.", "Could not claim the gift. Please retry.")
                            }
                        busy = null
                    }
                }
            }
            item { ProShopCard(profile?.isVip == true) { onSection(3) } }
            item {
                StorePromoCard(
                    gameText("Sezon Bileti", "Season Pass"),
                    gameText("Sezonu ve ödül yolunu keşfet", "Explore the season and reward track"),
                    gameText("İncele", "Explore"),
                ) { onSection(2) }
            }
            bundles.firstOrNull { it.section == "starter" }?.let { bundle ->
                item { StoreBundleCard(bundle, owned, busy != null || loading) { selectedBundle = bundle } }
            }
            if (items.isNotEmpty()) {
                item { VerifiedProductsHero(items, owned) }
                item {
                    GameSecondaryButton(
                        text = gameText("Tüm kozmetikleri aç", "View all cosmetics"),
                        onClick = { onSection(1) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Palette,
                    )
                }
            }
            bundles.filter { it.section == "limited" }.forEach { bundle ->
                item { StoreBundleCard(bundle, owned, busy != null || loading) { selectedBundle = bundle } }
            }
            if (storefront?.rewardedEnabled == true) item {
                GameTertiaryButton(
                    text = gameText("İsteğe bağlı reklam ödülleri", "Optional ad rewards"),
                    onClick = onRewards,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.CardGiftcard,
                )
            }
        }

        if (section == 3) {
            val proActive = profile?.isVip == true
            item { ProShopCard(proActive) { if (proActive) onPro() else showVip = true } }
            item { StoreProBenefits() }
            if (proActive) item {
                GameSecondaryButton(
                    text = gameText("PRO araçlarını aç", "Open PRO tools"),
                    onClick = onPro,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.WorkspacePremium,
                )
            }
        }

        if (section == 1) {
            val kinds = items.map { it.kind }.distinct()
            if (kinds.size > 1) item {
                StoreKindFilter(kinds = kinds, selected = kindFilter, onSelect = { kindFilter = it })
            }
        }

        if (section == 2) {
            // Collections come from the catalog's bundle sections; nothing is hard-coded per season.
            val groups = bundles.groupBy { it.section }
            if (groups.isEmpty() && !loading) item {
                GameEmptyState(
                    icon = Icons.Rounded.CollectionsBookmark,
                    title = gameText("Aktif koleksiyon yok", "No active collections"),
                    body = gameText("Sezon ve etkinlik koleksiyonları yayınlandığında burada görünür.", "Season and event collections appear here when they go live."),
                )
            }
            groups.forEach { (section, group) ->
                item(key = "collection-$section") { StoreCollectionTitle(section) }
                group.forEach { bundle ->
                    item(key = "bundle-${bundle.id}") {
                        StoreBundleCard(bundle, owned, busy != null || loading) { selectedBundle = bundle }
                    }
                }
            }
        }

        if (filtered.isEmpty() && !loading && section == 1) {
            item {
                GameEmptyState(
                    icon = Icons.Rounded.Storefront,
                    title = gameText("Satışta görünüm yok", "No styles on sale"),
                    body = gameText("Katalog güncellendiğinde burada görünecek.", "New catalog items will appear here when available."),
                )
            }
        }

        items(
            items = filtered.chunked(if (twoColumnProducts) 2 else 1),
            key = { group -> group.joinToString(separator = "|") { it.id } },
        ) { group ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                group.forEach { product ->
                    val mine = product.id in owned
                    val active = isEquipped(product)
                    VerifiedStoreProductCard(
                        item = product,
                        owned = mine,
                        equipped = active,
                        busy = busy != null || loading,
                        proActive = profile?.isVip == true,
                        compact = twoColumnProducts,
                        modifier = Modifier.weight(1f),
                    ) {
                        val b = backend
                        if (b == null || busy != null) return@VerifiedStoreProductCard
                        selectedProduct = product
                    }
                }
                if (twoColumnProducts && group.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item {
            GameSurface(elevated = true, borderColor = GameColors.PlayGreen.copy(alpha = .35f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = GameColors.PlayGreen.copy(alpha = .14f)) {
                        Icon(
                            Icons.Rounded.VerifiedUser,
                            null,
                            tint = GameColors.PlayGreen,
                            modifier = Modifier.padding(9.dp).size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            gameText("ADİL OYUN SÖZÜ", "FAIR PLAY PROMISE"),
                            color = GameColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            gameText(
                                "Mağaza ürünleri maç gücü, skor veya rating avantajı sağlamaz.",
                                "Store products never provide match power, score, or rating advantages.",
                            ),
                            color = GameColors.TextSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(
                color = GameColors.SecondarySurface,
                shape = GameShapes.Medium,
                border = BorderStroke(1.dp, GameColors.Border),
            ) {
                Text(
                    notice!!,
                    Modifier.fillMaxWidth().padding(12.dp),
                    color = GameColors.TextPrimary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }

    if (showVip) VipPurchaseDialog(onVerified = { scope.launch { reload() } }) { showVip = false }

    selectedProduct?.let { product ->
        StoreProductDetailSheet(
            item = product,
            owned = product.id in owned,
            equipped = isEquipped(product),
            proActive = profile?.isVip == true,
            balance = profile?.diamonds ?: 0,
            busy = busy != null || loading,
            onBuy = { purchase(product) },
            onEquip = { equip(product) },
            onPro = {
                selectedProduct = null
                onSection(3)
            },
            onDismiss = { selectedProduct = null },
        )
    }

    if (showCoins) {
        ModalBottomSheet(
            onDismissRequest = { showCoins = false },
            containerColor = GameColors.PrimarySurface,
            contentColor = GameColors.TextPrimary,
        ) {
            GooglePlayProductsCard { scope.launch { reload() } }
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
        }
    }

    selectedBundle?.let { bundle ->
        AlertDialog(
            onDismissRequest = { if (busy == null) selectedBundle = null },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
            title = { Text(gameText(bundle.nameTr, bundle.nameEn)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bundle.items.forEach { product ->
                        Text(gameText(product.nameTr, product.nameEn) + if (product.id in owned) gameText(" · Sende var", " · Owned") else "")
                    }
                    Text("${bundle.diamondPrice} Son Coin", color = GameColors.RewardAmber, fontWeight = FontWeight.Bold)
                    Text(
                        gameText("Sende olan ürünler tekrar verilmez.", "Already owned items are not granted again."),
                        fontSize = 12.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = busy == null, onClick = {
                    val b = backend
                    if (b != null && busy == null) scope.launch {
                        busy = bundle.id
                        runCatching { b.purchaseStoreBundle(bundle.id) }
                            .onSuccess {
                                notice = gameText("Paket koleksiyonuna eklendi.", "Bundle added to your collection.")
                                reload()
                            }
                            .onFailure { error ->
                                notice = if ("insufficient_diamonds" in error.message.orEmpty()) {
                                    gameText("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                } else {
                                    gameText("Paket alınamadı. Teklifi yenileyip tekrar dene.", "Could not purchase. Refresh the offer and retry.")
                                }
                            }
                        selectedBundle = null
                        busy = null
                    }
                }) { Text(gameText("Satın al", "Buy"), color = GameColors.PrimaryBlue) }
            },
            dismissButton = {
                TextButton(enabled = busy == null, onClick = { selectedBundle = null }) {
                    Text(gameText("Vazgeç", "Cancel"), color = GameColors.TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun StoreCollectionHeader(balance: Int, ownedCount: Int, total: Int, onCoins: () -> Unit) {
    GameSurface(elevated = true, borderColor = GameColors.PrimaryBlue.copy(alpha = .28f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = GameShapes.Medium, color = GameColors.PrimaryBlue.copy(alpha = .14f)) {
                Icon(
                    Icons.Rounded.Storefront,
                    null,
                    modifier = Modifier.padding(11.dp).size(27.dp),
                    tint = GameColors.PrimaryBlue,
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(gameText("Koleksiyon", "Collection"), color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(gameText("Görünüm · konfor · prestij", "Appearance · comfort · prestige"), color = GameColors.TextSecondary, fontSize = 10.sp)
                Text(
                    gameText("Koleksiyon $ownedCount/$total", "Collection $ownedCount/$total"),
                    color = GameColors.PrimaryBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(
                onClick = onCoins,
                shape = GameShapes.Pill,
                color = GameColors.RewardAmber.copy(alpha = .14f),
                border = BorderStroke(1.dp, GameColors.RewardAmber.copy(alpha = .35f)),
            ) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Toll, null, Modifier.size(16.dp), tint = GameColors.RewardAmber)
                    Spacer(Modifier.width(4.dp))
                    Text("$balance SC", color = GameColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun VerifiedProductsHero(items: List<ShopItemDto>, owned: Set<String>) {
    val featured = listOfNotNull(
        items.firstOrNull { it.kind == "game_theme" },
        items.firstOrNull { it.kind == "keyboard_theme" },
        items.firstOrNull { it.kind == "name_style" },
    ).ifEmpty { items.take(3) }.take(3)
    if (featured.isEmpty()) return

    GameSurface(borderColor = GameColors.TacticalTurquoise.copy(alpha = .30f)) {
        Text(
            gameText("Öne çıkan görünümler", "Featured styles"),
            color = GameColors.TacticalTurquoise,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            gameText("Tahta, klavye ve profil görünümünü kişiselleştir.", "Personalize your board, keyboard, and profile style."),
            color = GameColors.TextSecondary,
            fontSize = 9.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            featured.forEach { item ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    StoreProductPreview(item, Modifier.fillMaxWidth().height(82.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (item.id in owned) gameText("SAHİPSİN", "OWNED")
                        else if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr,
                        color = if (item.id in owned) GameColors.PlayGreen else GameColors.TextPrimary,
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

@Composable
private fun VerifiedStoreProductCard(
    item: ShopItemDto,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    proActive: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
    val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
    val lockedByPro = item.vipOnly && !proActive && !owned

    // The whole card opens the product detail sheet, including PRO-only and equipped items.
    Surface(
        onClick = onAction,
        enabled = !busy,
        modifier = modifier,
        color = GameColors.PrimarySurface,
        shape = GameShapes.Large,
        border = BorderStroke(
            if (equipped) 2.dp else 1.dp,
            if (equipped) GameColors.PlayGreen else GameColors.Border,
        ),
    ) {
        if (compact) {
            Column(
                Modifier.fillMaxWidth().padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StoreProductPreview(item, Modifier.fillMaxWidth().height(92.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        Modifier.weight(1f),
                        color = GameColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (equipped) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.CheckCircle, null, tint = GameColors.PlayGreen, modifier = Modifier.size(17.dp))
                    }
                }
                Text(
                    description,
                    color = GameColors.TextSecondary,
                    fontSize = 9.sp,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.vipOnly) Text("PRO", color = GameColors.PrestigeGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(
                    when {
                        equipped -> gameText("AKTİF", "ACTIVE")
                        owned -> gameText("SAHİPSİN", "OWNED")
                        else -> "${item.diamondPrice} SC"
                    },
                    color = if (owned) GameColors.PlayGreen else GameColors.RewardAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Button(
                    onClick = onAction,
                    enabled = !busy && !equipped && !lockedByPro,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = GameShapes.Small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.PrimaryBlue,
                        contentColor = GameColors.TextPrimary,
                        disabledContainerColor = GameColors.Disabled,
                        disabledContentColor = GameColors.DisabledContent,
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                ) {
                    Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when {
                            equipped -> gameText("KULLANILIYOR", "EQUIPPED")
                            owned -> gameText("KULLAN", "EQUIP")
                            lockedByPro -> "PRO"
                            else -> gameText("SATIN AL", "BUY")
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StoreProductPreview(item, Modifier.size(76.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            Modifier.weight(1f),
                            color = GameColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (equipped) Icon(Icons.Rounded.CheckCircle, null, tint = GameColors.PlayGreen, modifier = Modifier.size(18.dp))
                    }
                    Text(description, color = GameColors.TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (item.vipOnly) Text("PRO", color = GameColors.PrestigeGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                equipped -> gameText("AKTİF", "ACTIVE")
                                owned -> gameText("SAHİPSİN", "OWNED")
                                else -> "${item.diamondPrice} SC"
                            },
                            color = if (owned) GameColors.PlayGreen else GameColors.RewardAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onAction,
                            enabled = !busy && !equipped && !lockedByPro,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = GameShapes.Small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GameColors.PrimaryBlue,
                                contentColor = GameColors.TextPrimary,
                                disabledContainerColor = GameColors.Disabled,
                                disabledContentColor = GameColors.DisabledContent,
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                        ) {
                            Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when {
                                    equipped -> gameText("KULLANILIYOR", "EQUIPPED")
                                    owned -> gameText("KULLAN", "EQUIP")
                                    lockedByPro -> "PRO"
                                    else -> gameText("SATIN AL", "BUY")
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProShopCard(active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = GameColors.PrestigeGold.copy(alpha = .10f),
        shape = GameShapes.Large,
        border = BorderStroke(1.dp, GameColors.PrestigeGold.copy(alpha = .38f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = GameColors.PrestigeGold.copy(alpha = .15f)) {
                        Icon(
                            Icons.Rounded.WorkspacePremium,
                            null,
                            Modifier.padding(10.dp).size(28.dp),
                            tint = GameColors.PrestigeGold,
                        )
                    }
                    Column {
                        Text("PRO", color = GameColors.PrestigeGold, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(gameText("Reklamsız + profil + analiz", "Ad-free + profile + analysis"), color = GameColors.TextSecondary, fontSize = 9.sp)
                    }
                }
                Text(
                    if (active) gameText("AKTİF", "ACTIVE") else gameText("KEŞFET ›", "EXPLORE ›"),
                    color = if (active) GameColors.PlayGreen else GameColors.PrestigeGold,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                gameText(
                    "Özel oda • PRO profil rozeti • gelişmiş istatistik • reklamsız deneyim • sosyal ayrıcalıklar",
                    "Private rooms • PRO profile badge • advanced stats • ad-free experience • social benefits",
                ),
                color = GameColors.TextPrimary,
                fontSize = 10.sp,
            )
            Text(
                gameText(
                    "PRO, dereceli maçlarda skor, kelime ipucu veya rating avantajı vermez.",
                    "PRO provides no score, word-hint, or rating advantage in ranked matches.",
                ),
                color = GameColors.PlayGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** KOLEKSİYON: catalog collections (bundle sections) and the season store. */
@Composable
private fun StoreCollectionsTab(catalog: @Composable () -> Unit, season: @Composable () -> Unit) {
    var sub by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        SegmentedGameTabs(
            labels = listOf(gameText("Koleksiyonlar", "Collections"), gameText("Sezon", "Season")),
            selectedIndex = sub,
            onSelected = { sub = it },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(Modifier.weight(1f)) { if (sub == 1) season() else catalog() }
    }
}

@Composable
private fun StoreCollectionTitle(section: String) {
    val title = when (section) {
        "starter" -> gameText("Başlangıç koleksiyonu", "Starter collection")
        "limited" -> gameText("Sınırlı süreli koleksiyon", "Limited-time collection")
        "season" -> gameText("Sezon koleksiyonu", "Season collection")
        "tournament" -> gameText("Turnuva koleksiyonu", "Tournament collection")
        "league" -> gameText("Lig koleksiyonu", "League collection")
        else -> section.replace('_', ' ').replaceFirstChar { it.titlecase() }
    }
    Text(title, color = GameColors.Lavender, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
}

@Composable
private fun StoreKindFilter(kinds: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(gameText("Tümü", "All")) },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = GameColors.PrimarySurface,
                labelColor = GameColors.TextSecondary,
                selectedContainerColor = GameColors.Lavender.copy(alpha = .22f),
                selectedLabelColor = GameColors.TextPrimary,
            ),
        )
        kinds.forEach { kind ->
            FilterChip(
                selected = selected == kind,
                onClick = { onSelect(kind) },
                label = { Text(storeKindLabel(kind), maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GameColors.PrimarySurface,
                    labelColor = GameColors.TextSecondary,
                    selectedContainerColor = GameColors.Lavender.copy(alpha = .22f),
                    selectedLabelColor = GameColors.TextPrimary,
                ),
            )
        }
    }
}
