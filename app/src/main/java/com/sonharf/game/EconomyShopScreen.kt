package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
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
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 8)) }
    var kindFilter by remember { mutableStateOf<String?>(null) }
    var balance by remember { mutableStateOf<Int?>(null) }
    var rewards by remember { mutableStateOf(false) }
    if (rewards) {
        Column {
            TextButton(onClick = { rewards = false }) { Text(sh("Mağazaya dön", "Back to shop")) }
            RewardCenterScreen()
        }
        return
    }
    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        val b = OnlineGameBackend()
        balance = b.currentUserId()?.let { id -> runCatching { b.getProfile(id).diamonds }.getOrNull() }
    }
    Column(Modifier.fillMaxSize().background(LobbyPalette.Ground)) {
        StoreTitleBar(balance = balance, onBack = onBack)
        HorizontalDivider(color = LobbyPalette.Line.copy(alpha = .6f))
        // Keep every category visible; related products share one group instead of a hidden strip.
        val shown = if (tab in setOf(0, 2, 3, 4, 5, 6, 7, 8)) tab else 0
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCollection) {
                Icon(Icons.Rounded.Checkroom, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(sh("Koleksiyonum", "My collection"), color = LobbyPalette.Ink, fontSize = 12.sp)
            }
            TextButton(onClick = { tab = 8 }) {
                Icon(Icons.Rounded.Redeem, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(sh("Ödüller", "Rewards"), color = LobbyPalette.Ink, fontSize = 12.sp)
            }
        }
        val selectedGroup = when (shown) { 4 -> 5; 6 -> 2; else -> shown }
        val categories = listOf(0 to sh("Vitrin", "Featured"), 2 to sh("Stil", "Style"),
            5 to "Obi", 7 to sh("Çerçeve", "Frames"), 3 to "PRO")
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            categories.forEach { (index, label) ->
                Surface(onClick = { tab = index; kindFilter = null }, modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(12.dp), color = if (selectedGroup == index) LobbyPalette.Green else LobbyPalette.Paper,
                    border = BorderStroke(1.dp, if (selectedGroup == index) LobbyPalette.Green else LobbyPalette.Line)) {
                    Box(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (selectedGroup == index) Hf.OnAccent else LobbyPalette.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
        if (shown in setOf(2, 6, 4, 5)) {
            val choices = if (shown in setOf(2, 6)) listOf(2 to sh("Tahta & Tema", "Board & Theme"), 6 to sh("Klavye & İsim", "Keys & Name"))
                else listOf(5 to sh("Aksesuar & Zafer", "Hats & Victory"), 4 to sh("Maskotlar", "Mascots"))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { (index, label) -> FilterChip(selected = shown == index, onClick = { tab = index },
                    colors = FilterChipDefaults.filterChipColors(containerColor = LobbyPalette.Paper, labelColor = LobbyPalette.Muted,
                        selectedContainerColor = SonHarfTheme.PrimarySoft, selectedLabelColor = LobbyPalette.Ink),
                    label = { Text(label, fontSize = 12.sp) }) }
            }
        }
        Box(Modifier.weight(1f)) {
            // Mascot characters, each a permanent Google Play product.
            // Optional rewarded videos: coins, a day's keyboard or theme, Quick Duels, hints.
            if (shown == 8) RewardCenterScreen()
            else if (shown == 4) MascotStoreSection()
            // Profile frames: ornate crests via Google Play, simple rings for Son Coin.
            else if (shown == 7) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                ProfileFrameStoreSection(onBalance = { balance = it }, onPro = { tab = 3 })
            }
            else EconomyCatalogScreen(
                section = shown,
                kindFilter = kindFilter,
                onSection = { tab = it; if (it != 2) kindFilter = null },
                onRewards = { rewards = true },
                onMembershipChanged = onMembershipChanged,
                onCollection = onCollection,
                onPro = onPro,
                onBalance = { balance = it },
            )
        }
    }
}

@Composable
private fun StoreTitleBar(balance: Int?, onBack: (() -> Unit)?) {
    // A row, not an overlay: a large balance can never cover the title.
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.hf_ic_back), sh("Geri", "Back"), tint = Hf.Gold, modifier = Modifier.size(30.dp))
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(sh("Mağaza", "Store"), color = LobbyPalette.Ink, fontSize = 27.sp,
                fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sh("Oyununa renk kat", "Make it yours"), color = LobbyPalette.Muted, fontSize = 12.sp)
        }
        HfPill(modifier = Modifier.padding(end = 6.dp), color = LobbyPalette.Paper, borderColor = LobbyPalette.Line) {
            HfCoin(20.dp)
            Spacer(Modifier.width(8.dp))
            Text(balance?.let { storeGrouped(it) } ?: "—", color = LobbyPalette.Ink, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

private fun storeGrouped(value: Int): String = String.format(java.util.Locale("tr", "TR"), "%,d", value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EconomyCatalogScreen(
    section: Int,
    kindFilter: String?,
    onSection: (Int) -> Unit,
    onRewards: () -> Unit,
    onMembershipChanged: (Boolean) -> Unit,
    onCollection: () -> Unit,
    onPro: () -> Unit,
    onBalance: (Int?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    var keyboardOffers by remember { mutableStateOf<Map<String, com.android.billingclient.api.ProductDetails>>(emptyMap()) }

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
            // The five reads are independent: fetch them together so the store fills at once.
            kotlinx.coroutines.coroutineScope {
                val profileTask = async { b.getProfile(id) }
                val itemsTask = async { b.getShopItems() }
                val ownedTask = async { b.getInventory() }
                val equippedTask = async { b.getEquippedCosmetics() }
                val storefrontTask = async { runCatching { b.getStorefront() }.getOrNull() }
                profile = profileTask.await()
                items = itemsTask.await().filter { it.isRuntimeReadyStyle() }
                owned = ownedTask.await()
                equipped = equippedTask.await()
                storefront = storefrontTask.await()
            }
            SonHarfCosmetics.apply(equipped)
            onMembershipChanged(profile?.isVip == true)
            onBalance(profile?.diamonds)
        }.onFailure {
            notice = sh("Mağaza verileri yüklenemedi.", "Shop data could not be loaded.")
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        reload()
        loading = false
    }

    // Premium keyboards are permanent Google Play products (Premium White stays on Son Coin).
    val billing = remember {
        com.sonharf.game.billing.BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != null && productId in com.sonharf.game.billing.ProductCatalog.keyboardProducts && productId !in owned) {
                    scope.launch {
                        busy = productId
                        runCatching { com.sonharf.game.billing.PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                notice = sh("Klavye koleksiyonuna eklendi.", "Keyboard added to your collection.")
                                SonHarfSoundFx.bonus()
                                reload()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() -> sh("Google Play sunucu doğrulaması henüz etkin değil.", "Google Play server verification is not enabled yet.")
                                    "product_disabled" in error.message.orEmpty() -> sh("Bu klavye henüz satışa açılmadı.", "This keyboard is not on sale yet.")
                                    else -> sh("Ödeme doğrulaması tamamlanamadı; yeniden deneyebilirsin.", "Purchase verification failed; you can retry.")
                                }
                            }
                        busy = null
                    }
                }
            },
            onMessage = { message -> notice = message; busy = null },
        )
    }
    DisposableEffect(billing) {
        billing.connect {
            billing.queryOneTimeProducts(com.sonharf.game.billing.ProductCatalog.keyboardProducts) { keyboardOffers = it }
            billing.restorePurchases(com.sonharf.game.billing.ProductCatalog.keyboardProducts.toSet())
        }
        onDispose { billing.close() }
    }

    fun isEquipped(item: ShopItemDto): Boolean = equipped.isEquipped(item)

    val featuredFirst = listOfNotNull(
        items.firstOrNull { it.kind == "game_theme" },
        items.firstOrNull { it.kind == "board_skin" },
        items.firstOrNull { it.kind == "keyboard_theme" },
        items.firstOrNull { it.kind == "name_style" },
    )
    val filtered = when (section) {
        0 -> (featuredFirst + items).distinctBy { it.id }.take(8)
        2 -> items.filter { it.kind == "game_theme" || it.kind == "board_skin" }.sortedByDescending { it.id == WALNUT_IVORY_THEME_ID }
        5 -> items.filter { it.kind == "mascot_hat" || it.kind == "victory_effect" }
        6 -> items.filter { it.kind == "keyboard_theme" || it.kind == "name_style" }
        else -> emptyList()
    }
    val bundles = storefront?.bundles.orEmpty().filter { b -> b.items.isNotEmpty() && b.items.all { it.isRuntimeReadyStyle() } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (section == 0) {
            item { StoreProBanner(profile?.isVip == true) { onSection(3) } }
            item {
                HfSecondaryButton(
                    sh("Son Coin al", "Get Son Coin"),
                    onClick = { showCoins = true },
                    modifier = Modifier.fillMaxWidth(),
                    trailingChevron = true,
                )
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Gold, trackColor = LobbyPalette.Paper) }

        if (filtered.isEmpty() && !loading && section in setOf(2, 5, 6)) {
            item {
                LobbyCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        sh("Bu kategoride şu anda satışta ürün yok.", "Nothing in this category is on sale right now."),
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        color = LobbyPalette.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        items(filtered.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val mine = item.id in owned
                    val active = isEquipped(item)
                    val playKeyboard = item.id in com.sonharf.game.billing.ProductCatalog.keyboardProducts
                    VerifiedStoreProductCard(
                        item = item,
                        owned = mine,
                        equipped = active,
                        busy = busy != null || loading || (playKeyboard && !mine && keyboardOffers[item.id]?.oneTimePurchaseOfferDetails == null),
                        proActive = profile?.isVip == true,
                        balance = profile?.diamonds,
                        wins = profile?.wins,
                        playPrice = if (playKeyboard) {
                            keyboardOffers[item.id]?.oneTimePurchaseOfferDetails?.formattedPrice ?: sh("ŞU AN SATIŞTA DEĞİL", "CURRENTLY UNAVAILABLE")
                        } else null,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onCollection = onCollection,
                    ) {
                        val b = backend
                        if (b == null || busy != null) return@VerifiedStoreProductCard
                        if (playKeyboard && !mine) {
                            val product = keyboardOffers[item.id]
                            val activity = context as? android.app.Activity
                            if (activity == null || product?.oneTimePurchaseOfferDetails == null) {
                                notice = sh("Bu klavye Google Play'de henüz satışta değil.", "This keyboard is not on Google Play yet.")
                            } else {
                                busy = item.id
                                val result = billing.launchProduct(activity, product)
                                if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                                    busy = null
                                    notice = sh("Google Play ödeme ekranı açılamadı (${result.responseCode}).", "Google Play billing could not open (${result.responseCode}).")
                                }
                            }
                            return@VerifiedStoreProductCard
                        }
                        scope.launch {
                            busy = item.id
                            val displayName = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
                            if (mine) {
                                // Owned items are managed (worn, changed, removed) only from the profile.
                                notice = sh("$displayName koleksiyonunda.", "$displayName is in your collection.")
                            } else {
                                runCatching { b.purchaseShopItem(item.id) }
                                    .onSuccess {
                                        // Buying only adds it to the collection; the player wears it from the profile.
                                        notice = sh("$displayName koleksiyonuna eklendi.", "$displayName added to your collection.")
                                        SonHarfSoundFx.bonus()
                                        reload()
                                    }
                                    .onFailure {
                                        val raw = it.message.orEmpty()
                                        notice = when {
                                            "insufficient_diamonds" in raw -> sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                            "vip_required" in raw -> sh("Bu ürün PRO üyelerine özel.", "This item is exclusive to PRO members.")
                                            "already_owned" in raw -> sh("Bu ürüne zaten sahipsin.", "You already own this item.")
                                            "play_only" in raw -> sh("Bu ürün Google Play ile satılır.", "This item is sold through Google Play.")
                                            "requirement_not_met" in raw -> item.requiredWins?.let { need ->
                                                sh("Bu ürün $need galibiyetle açılır. Maç kazandıkça yaklaşırsın.", "This item unlocks at $need wins. Every win brings you closer.")
                                            } ?: sh("Bu ürünün başarı şartı henüz tamamlanmadı.", "This item's achievement requirement is not met yet.")
                                            else -> sh("Satın alma tamamlanamadı.", "Purchase failed.")
                                        }
                                    }
                            }
                            busy = null
                        }
                    }
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
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
            bundles.filter { it.section == "starter" || it.section == "limited" }.forEach { bundle ->
                item { StoreBundleCard(bundle, owned, busy != null || loading) { selectedBundle = bundle } }
            }
            if (storefront?.rewardedEnabled == true) item { TextButton(onClick = onRewards) { Text(sh("İsteğe bağlı reklam ödülleri", "Optional ad rewards"), color = LobbyPalette.Gold) } }
        }
        if (section == 3) {
            val proActive = profile?.isVip == true
            item { ProShopCard(proActive) { if (proActive) onPro() else showVip = true } }
            item { StoreProBenefits() }
            if (proActive) item {
                TextButton(onClick = onPro, modifier = Modifier.fillMaxWidth()) {
                    Text(sh("PRO ARAÇLARINI AÇ", "OPEN PRO TOOLS"), fontWeight = FontWeight.Black, color = LobbyPalette.Gold)
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            LobbyCard(modifier = Modifier.fillMaxWidth(), color = LobbyPalette.Paper) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = LobbyPalette.Ink, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }

    if (showVip) VipPurchaseDialog(onVerified = { scope.launch { reload() } }) { showVip = false }
    if (showCoins) ModalBottomSheet(onDismissRequest = { showCoins = false }, containerColor = LobbyPalette.Paper) {
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

/** PRO banner from the 03 preview: crown, gold PRO plate, one line, chevron. */
@Composable
private fun StoreProBanner(active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = Hf.CardShape,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, Hf.Gold),
    ) {
        Row(
            Modifier
                .background(Brush.horizontalGradient(listOf(Color(0xFF244D35), Color(0xFF296B47), Color(0xFF367C54))))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Hf.Gold, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Hf.Gold) {
                Text("PRO", Modifier.padding(horizontal = 10.dp, vertical = 3.dp), color = Hf.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (active) sh("PRO aktif", "PRO active")
                else sh("Reklamsız · Profil · Analiz", "Ad-free · Profile · Analysis"),
                modifier = Modifier.weight(1f),
                color = Hf.OnAccent,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(Icons.Rounded.ChevronRight, null, tint = Hf.Gold, modifier = Modifier.size(28.dp))
        }
    }
}

/** 158×198 ivory product card: preview, name, champagne rule, price chip or owned pill. */
@Composable
internal fun VerifiedStoreProductCard(
    item: ShopItemDto,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    proActive: Boolean,
    balance: Int? = null,
    wins: Int? = null,
    playPrice: String? = null,
    modifier: Modifier = Modifier,
    onCollection: () -> Unit = {},
    onAction: () -> Unit,
) {
    var previewOpen by remember(item.id) { mutableStateOf(false) }
    val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
    val lockedByPro = item.vipOnly && !proActive && !owned
    val tier = StoreTier.from(item.economyTier, item.rarity)
    val goal = StoreGoalProgress.of(item.diamondPrice, balance, item.requiredWins, wins)
    val tierColor = when (tier) {
        StoreTier.STARTER, StoreTier.COMMON -> LobbyPalette.Muted
        StoreTier.RARE -> Color(0xFF3E9F4D)
        StoreTier.EPIC -> Color(0xFF8A4FC7)
        StoreTier.LEGENDARY, StoreTier.PRESTIGE -> Hf.GoldDeep
    }

    Surface(
        onClick = { if (owned || equipped) onCollection() else onAction() },
        enabled = !busy && !lockedByPro,
        modifier = modifier,
        shape = Hf.CardShape,
        color = LobbyPalette.Paper,
        // Legendary and prestige get a slightly stronger rim, nothing louder.
        border = BorderStroke(if (tier.rank >= StoreTier.LEGENDARY.rank) 2.5.dp else 1.5.dp, if (owned || equipped) Hf.Green else if (tier.rank >= StoreTier.RARE.rank) tierColor else Hf.Gold),
        shadowElevation = 3.dp,
    ) {
        Box {
            // Same order on every card: picture, name, one short line, then the price at the bottom,
            // all centred so neighbouring cards line up.
            Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                StoreProductPreview(item, Modifier.fillMaxWidth().height(102.dp))
                Spacer(Modifier.height(6.dp))
                // Kelimelik-style card: picture, name and price only; the "?" corner explains the item.
                Text(
                    name,
                    color = LobbyPalette.Ink,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f).heightIn(min = 6.dp))
                // A long goal reads as progress, not as a wall: coin share and any win requirement.
                if (!owned && !equipped && !lockedByPro && playPrice == null && balance != null) {
                    if (goal.winsNeeded != null) {
                        Text(
                            sh("${minOf(goal.winsHave, goal.winsNeeded)}/${goal.winsNeeded} galibiyet", "${minOf(goal.winsHave, goal.winsNeeded)}/${goal.winsNeeded} wins"),
                            color = if (goal.requirementMet) Hf.Green else LobbyPalette.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    if (goal.coinsMissing > 0) {
                        LinearProgressIndicator(
                            progress = { goal.coinFraction },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).height(4.dp),
                            color = tierColor.takeIf { tier.rank >= StoreTier.RARE.rank } ?: Hf.Gold,
                            trackColor = LobbyPalette.Line,
                        )
                        Text("%${goal.percent}", color = LobbyPalette.Muted, fontSize = 9.sp, maxLines = 1)
                    }
                }
                if (owned || equipped) {
                    Surface(shape = Hf.PillShape, color = Hf.Green) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                sh("SATIN ALINDI", "PURCHASED"),
                                color = Hf.OnAccent,
                                fontSize = 10.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                } else {
                    Surface(shape = Hf.PillShape, color = Hf.Gold.copy(alpha = .14f), border = BorderStroke(1.dp, Hf.Gold.copy(alpha = .7f))) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (lockedByPro) {
                                Icon(Icons.Rounded.WorkspacePremium, null, tint = Hf.GoldDeep, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PRO", color = LobbyPalette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            } else if (playPrice != null) {
                                // A Google Play product: its store price, not Son Coin.
                                Icon(Icons.Rounded.ShoppingCart, null, tint = Hf.GoldDeep, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(playPrice, color = LobbyPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            } else {
                                HfCoin(18.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(storeGrouped(item.diamondPrice), color = LobbyPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            }
                        }
                    }
                }
            }
            if (lockedByPro) {
                Box(Modifier.matchParentSize().background(LobbyPalette.Ground.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.hf_ic_lock), null, tint = Hf.Gold, modifier = Modifier.size(34.dp))
                }
            }
            IconButton(onClick = { previewOpen = true }, modifier = Modifier.align(Alignment.TopEnd).size(40.dp)) {
                Surface(shape = CircleShape, color = LobbyPalette.Paper, border = BorderStroke(1.dp, LobbyPalette.Line)) {
                    Icon(Icons.Rounded.QuestionMark, sh("Bu ürün ne işe yarar?", "What does this item do?"),
                        tint = LobbyPalette.Ink, modifier = Modifier.padding(4.dp).size(16.dp))
                }
            }
        }
    }
    if (previewOpen) AlertDialog(
        onDismissRequest = { previewOpen = false },
        containerColor = LobbyPalette.Paper,
        title = { Text(name, color = LobbyPalette.Ink, fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StoreProductPreview(item, Modifier.fillMaxWidth().height(230.dp), expanded = true)
            Text(tier.label, color = tierColor, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
            Text(storeItemEffect(item) ?: storeKindLabel(item.kind), color = LobbyPalette.Ink, fontSize = 15.sp, lineHeight = 21.sp)
            Text(if (owned || equipped) sh("Koleksiyonunda", "In your collection") else if (lockedByPro) sh("PRO'ya özel", "PRO exclusive") else playPrice ?: "${item.diamondPrice} Son Coin", color = LobbyPalette.Gold, fontWeight = FontWeight.Bold)
        } },
        confirmButton = { Button(enabled = !busy && !lockedByPro, onClick = {
            previewOpen = false
            if (owned || equipped) onCollection() else onAction()
        }) { Text(if (owned || equipped) sh("KOLEKSİYONUM", "MY COLLECTION") else sh("SATIN AL", "BUY")) } },
        dismissButton = { TextButton(onClick = { previewOpen = false }) { Text(sh("KAPAT", "CLOSE")) } },
    )
}

@Composable
private fun ProShopCard(active: Boolean, onClick: () -> Unit) {
    LobbyCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), borderColor = Hf.Gold) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(40.dp), tint = Hf.Gold)
                    Column {
                        Text("PRO", color = LobbyPalette.Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(sh("Reklamsız + profil + analiz", "Ad-free + profile + analysis"), color = LobbyPalette.Muted, fontSize = 12.sp)
                    }
                }
                Text(if (active) sh("AKTİF", "ACTIVE") else sh("KEŞFET ›", "EXPLORE ›"), color = if (active) Hf.GreenLight else Hf.Gold, fontWeight = FontWeight.Black)
            }
            Text(sh("Reklamsız • PRO profil • özel oda • maç özeti • konfor araçları", "Ad-free • PRO profile • private rooms • match recap • comfort tools"), color = LobbyPalette.Ink, fontSize = 13.sp)
        }
    }
}

private fun storeKindLabel(kind: String?): String = when (kind) {
    null -> sh("Tümü", "All")
    "game_theme" -> sh("Oyun teması", "Game theme")
    "keyboard_theme" -> sh("Klavye", "Keyboard")
    "name_style" -> sh("İsim stili", "Name style")
    "profile_frame" -> sh("Çerçeve", "Frame")
    "board_skin" -> sh("Tahta", "Board")
    "victory_effect", "vfx" -> sh("Efekt", "Effect")
    else -> kind.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
