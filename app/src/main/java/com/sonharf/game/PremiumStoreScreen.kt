package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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

private const val PremiumStoreBlackThemeId = "theme_black"
private val PremiumStoreKinds = setOf(
    "game_theme",
    "profile_frame",
    "keyboard_theme",
    "name_style",
    "victory_effect",
    "emoji_pack",
)

private data class PremiumStoreCategory(
    val titleTr: String,
    val titleEn: String,
    val asset: PurchasedUiAsset,
    val kinds: Set<String>,
)

private val premiumStoreCategories = listOf(
    PremiumStoreCategory("Temalar", "Themes", PurchasedUiAsset.ICON_GAMES, setOf("game_theme")),
    PremiumStoreCategory("Profil Çerçeveleri", "Profile Frames", PurchasedUiAsset.NAV_PROFILE, setOf("profile_frame")),
    PremiumStoreCategory("Tuş Stilleri", "Keyboard Styles", PurchasedUiAsset.ICON_SWORDS, setOf("keyboard_theme")),
    PremiumStoreCategory("İsim & Prestij", "Name & Prestige", PurchasedUiAsset.ICON_CROWN, setOf("name_style", "victory_effect", "emoji_pack")),
)

@Composable
internal fun PremiumStoreScreen(
    initialTab: Int = 0,
    onBack: () -> Unit,
    onMembershipChanged: (Boolean) -> Unit,
    onCollection: () -> Unit,
) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var products by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var storefront by remember { mutableStateOf<StorefrontDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showCoins by remember { mutableStateOf(false) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            loading = false
            notice = sh("Mağaza için sunucu bağlantısı gerekli.", "A server connection is required for the shop.")
            return
        }
        loading = true
        runCatching {
            val id = requireNotNull(b.currentUserId())
            profile = b.getProfile(id)
            entitlements = runCatching { b.getVipEntitlements() }.getOrNull()
            products = b.getShopItems()
                .filterNot { it.id in ProfileFrameV2Catalog.paidIds }
                .filter { it.kind in PremiumStoreKinds }
                .filterNot { it.id in setOf("theme_dark_arena", "theme_monster_blue", "theme_aurora", "theme_neon", "theme_midnight") }
                .sortedWith(compareBy<ShopItemDto> { premiumStoreKindOrder(it.kind) }.thenBy { it.sortOrder }.thenBy { it.id })
            owned = b.getInventory()
            equipped = b.getEquippedCosmetics()
            storefront = runCatching { b.getStorefront() }.getOrNull()
            SonHarfCosmetics.apply(equipped, owned)
            onMembershipChanged(entitlements?.isPro == true)
        }.onFailure {
            notice = sh("Mağaza verileri yenilenemedi.", "Shop data could not be refreshed.")
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PurchasedIconButton(
                asset = PurchasedUiAsset.ICON_CLOSE,
                onClick = onBack,
                contentDescription = sh("Geri", "Back"),
            )
            Spacer(Modifier.width(8.dp))
            PurchasedSectionHeader(
                title = sh("MAĞAZA", "SHOP"),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PurchasedCurrencyBar(
                amount = "${profile?.diamonds ?: 0}",
                onClick = { showCoins = true },
            )
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            val tabs = listOf(
                Triple(sh("ÖNE ÇIKAN", "FEATURED"), PurchasedUiAsset.ICON_GIFT, 0),
                Triple(sh("SEZON", "SEASON"), PurchasedUiAsset.ICON_TROPHY, 1),
                Triple(sh("GÖRÜNÜMLER", "STYLES"), PurchasedUiAsset.NAV_PROFILE, 2),
                Triple("PRO", PurchasedUiAsset.ICON_CROWN, 3),
            )
            tabs.forEach { (label, asset, index) ->
                PremiumStoreAssetTab(
                    text = label,
                    asset = asset,
                    selected = tab == index,
                    onClick = { tab = index },
                )
            }
        }

        when (tab) {
            1 -> SeasonCenterContent()
            3 -> {
                val b = backend
                if (b != null) UnifiedProVipScreen(b) { tab = 0 }
                else PremiumStoreOfflineCard(Modifier.padding(16.dp))
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading) {
                    item {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().height(5.dp),
                            color = Color(0xFF58B957),
                            trackColor = Color(0xFFDEC59B),
                        )
                    }
                }

                if (tab == 0 || tab == 2) {
                    item {
                        PurchasedPanel(
                            modifier = Modifier.fillMaxWidth(),
                            asset = PurchasedUiAsset.PANEL_MEDIUM,
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            ProfileFramesV2StoreRow(
                                backend = backend,
                                onChanged = { scope.launch { reload() } },
                            )
                        }
                    }
                }

                if (tab == 0) {
                    item {
                        PurchasedPanel(
                            modifier = Modifier.fillMaxWidth(),
                            asset = PurchasedUiAsset.PANEL_MEDIUM,
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            StoreDailyRewardCard(storefront, busyId != null || loading) {
                                val b = backend ?: return@StoreDailyRewardCard
                                if (busyId != null || storefront?.dailyClaimed == true) return@StoreDailyRewardCard
                                scope.launch {
                                    busyId = "daily"
                                    runCatching { b.claimDailyCheckin() }
                                        .onSuccess { amount ->
                                            notice = if (amount > 0) sh("+$amount Son Coin hesabına eklendi.", "+$amount Son Coin added to your account.")
                                            else sh("Bugünkü hediyeni aldın.", "Today's gift was claimed.")
                                            reload()
                                        }
                                        .onFailure { notice = sh("Günlük hediye alınamadı.", "Daily gift could not be claimed.") }
                                    busyId = null
                                }
                            }
                        }
                    }
                    item { PremiumStoreProHero(entitlements?.isPro == true) { tab = 3 } }
                    item {
                        PurchasedSectionHeader(sh("GOOGLE PLAY", "GOOGLE PLAY"))
                        Spacer(Modifier.height(7.dp))
                        GooglePlayProductsCard(
                            onPurchased = { scope.launch { reload() } },
                            showPremiumProducts = true,
                            showCoinPacks = false,
                        )
                    }
                    products.firstOrNull { it.id == PremiumStoreBlackThemeId }?.let { black ->
                        item {
                            PremiumStoreFeaturedTheme(
                                item = black,
                                owned = black.id in owned,
                                busy = busyId != null || loading,
                                onAction = {
                                    if (black.id in owned) onCollection()
                                    else premiumStorePurchase(
                                        backend = backend,
                                        item = black,
                                        scope = scope,
                                        setBusy = { busyId = it },
                                        setNotice = { notice = it },
                                        reload = { reload() },
                                    )
                                },
                            )
                        }
                    }
                    item {
                        PurchasedSectionHeader(
                            title = sh("SEÇİLİ GÖRÜNÜMLER", "SELECTED STYLES"),
                            action = sh("TÜMÜ", "ALL"),
                            onAction = { tab = 2 },
                        )
                    }
                    val highlights = products.filter { it.id != PremiumStoreBlackThemeId }.take(4)
                    highlights.chunked(2).forEach { rowItems ->
                        item {
                            PremiumStoreProductRow(
                                items = rowItems,
                                owned = owned,
                                equipped = equipped,
                                busyId = busyId,
                                loading = loading,
                                onOwned = onCollection,
                                onBuy = { product ->
                                    premiumStorePurchase(backend, product, scope, { busyId = it }, { notice = it }, { reload() })
                                },
                            )
                        }
                    }
                } else {
                    premiumStoreCategories.forEach { category ->
                        val categoryProducts = products.filter { it.kind in category.kinds }
                        if (categoryProducts.isNotEmpty()) {
                            item {
                                PremiumStoreCategoryHeader(
                                    title = sh(category.titleTr, category.titleEn),
                                    count = categoryProducts.size,
                                    asset = category.asset,
                                )
                            }
                            categoryProducts.chunked(2).forEach { rowItems ->
                                item {
                                    PremiumStoreProductRow(
                                        items = rowItems,
                                        owned = owned,
                                        equipped = equipped,
                                        busyId = busyId,
                                        loading = loading,
                                        onOwned = onCollection,
                                        onBuy = { product ->
                                            premiumStorePurchase(backend, product, scope, { busyId = it }, { notice = it }, { reload() })
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (!loading && products.isEmpty()) item { PremiumStoreOfflineCard() }
                }

                notice?.let { message ->
                    item {
                        PurchasedPanel(
                            modifier = Modifier.fillMaxWidth(),
                            asset = PurchasedUiAsset.PANEL_SMALL,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
                        ) {
                            Text(message, Modifier.fillMaxWidth(), color = Color(0xFF4A2D20), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
        }
    }

    if (showCoins) {
        ModalBottomSheet(
            onDismissRequest = { showCoins = false },
            containerColor = Color.Transparent,
            dragHandle = null,
        ) {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                asset = PurchasedUiAsset.PANEL_LARGE,
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    PurchasedSectionHeader("SON COIN")
                    Spacer(Modifier.height(8.dp))
                    GooglePlayProductsCard(
                        onPurchased = { scope.launch { reload() } },
                        showPremiumProducts = false,
                        showCoinPacks = true,
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
        }
    }
}

@Composable
private fun PremiumStoreAssetTab(
    text: String,
    asset: PurchasedUiAsset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PurchasedPanel(
        modifier = Modifier.widthIn(min = 102.dp).clickable(onClick = onClick),
        asset = if (selected) PurchasedUiAsset.PANEL_SMALL else PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PurchasedAsset(asset, Modifier.size(28.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = if (selected) Color(0xFF6B3CA6) else Color(0xFF654A3D), fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

private fun premiumStoreKindOrder(kind: String): Int = when (kind) {
    "game_theme" -> 0
    "profile_frame" -> 1
    "keyboard_theme" -> 2
    "name_style" -> 3
    "victory_effect" -> 4
    "emoji_pack" -> 5
    else -> 9
}

private fun ShopItemDto.premiumStoreName(): String = when (id) {
    PremiumStoreBlackThemeId -> sh("Siyah Tema", "Black Theme")
    else -> if (SonHarfUiState.isEnglish) nameEn else nameTr
}

private fun ShopItemDto.premiumStoreDescription(): String = when (id) {
    PremiumStoreBlackThemeId -> sh(
        "Siyah ve grafit yüzeyler; mavi, turkuaz ve eflatun premium vurgular.",
        "Black and graphite surfaces with blue, turquoise and purple premium accents.",
    )
    else -> if (SonHarfUiState.isEnglish) descriptionEn else descriptionTr
}

private fun EquippedCosmeticsDto?.premiumStoreEquipped(item: ShopItemDto): Boolean = when (item.kind) {
    "game_theme" -> this?.gameThemeId == item.id
    "profile_frame" -> this?.profileFrameId == item.id
    "keyboard_theme" -> this?.keyboardThemeId == item.id
    "name_style" -> this?.nameStyleId == item.id
    "victory_effect" -> this?.victoryEffectId == item.id
    "emoji_pack" -> this?.emojiPackId == item.id
    else -> false
}

private fun premiumStorePurchase(
    backend: OnlineGameBackend?,
    item: ShopItemDto,
    scope: kotlinx.coroutines.CoroutineScope,
    setBusy: (String?) -> Unit,
    setNotice: (String?) -> Unit,
    reload: suspend () -> Unit,
) {
    val b = backend ?: return
    scope.launch {
        setBusy(item.id)
        runCatching { b.purchaseShopItem(item.id) }
            .onSuccess {
                setNotice(sh("${item.premiumStoreName()} satın alındı. Profil > Koleksiyonum'dan kullanabilirsin.", "${item.premiumStoreName()} purchased. Equip it from Profile > My Collection."))
                reload()
            }
            .onFailure { error ->
                val raw = error.message.orEmpty()
                setNotice(
                    when {
                        "insufficient_diamonds" in raw -> sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                        "vip_required" in raw -> sh("Bu ürün PRO üyelerine özel.", "This item is exclusive to PRO members.")
                        "already_owned" in raw -> sh("Bu ürün zaten koleksiyonunda.", "This item is already in your collection.")
                        else -> sh("Satın alma tamamlanamadı.", "Purchase could not be completed.")
                    }
                )
            }
        setBusy(null)
    }
}

@Composable
private fun PremiumStoreProHero(active: Boolean, onClick: () -> Unit) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(62.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("PRO", color = Color(0xFF6B3CA6), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    if (active) sh("Üyeliğin aktif • ayrıcalıklarını gör", "Membership active • view your benefits")
                    else sh("Reklamsız kullanım • premium stil • analiz", "Ad-free • premium style • analysis"),
                    color = Color(0xFF654A3D),
                    fontSize = 9.sp,
                )
            }
            PurchasedAsset(PurchasedUiAsset.ICON_GIFT, Modifier.size(43.dp))
        }
    }
}

@Composable
private fun PremiumStoreFeaturedTheme(item: ShopItemDto, owned: Boolean, busy: Boolean, onAction: () -> Unit) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 17.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.SHOP_SHELVES, Modifier.size(width = 92.dp, height = 132.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("ÖNE ÇIKAN TEMA", "FEATURED THEME"), color = Color(0xFF6B3CA6), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(item.premiumStoreName(), color = Color(0xFF4A2D20), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(item.premiumStoreDescription(), color = Color(0xFF765746), fontSize = 9.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(7.dp))
                    StoreProductPreview(item, Modifier.fillMaxWidth().height(72.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.ICON_COIN, Modifier.size(30.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    if (owned) sh("KOLEKSİYONUNDA", "IN COLLECTION") else "${item.diamondPrice} Son Coin",
                    color = Color(0xFF654A3D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            PurchasedButton(
                text = if (owned) sh("KOLEKSİYONA GİT", "OPEN COLLECTION") else sh("SATIN AL", "BUY"),
                onClick = onAction,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                style = if (owned) PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.PRIMARY,
                leadingAsset = if (owned) PurchasedUiAsset.NAV_PROFILE else PurchasedUiAsset.NAV_SHOP,
            )
        }
    }
}

@Composable
private fun PremiumStoreCategoryHeader(title: String, count: Int, asset: PurchasedUiAsset) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PurchasedAsset(asset, Modifier.size(38.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, Modifier.weight(1f), color = Color(0xFF4A2D20), fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(count.toString(), color = Color(0xFF6B3CA6), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremiumStoreProductRow(
    items: List<ShopItemDto>,
    owned: Set<String>,
    equipped: EquippedCosmeticsDto?,
    busyId: String?,
    loading: Boolean,
    onOwned: () -> Unit,
    onBuy: (ShopItemDto) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            PremiumStoreProductTile(
                item = item,
                owned = item.id in owned,
                equipped = equipped.premiumStoreEquipped(item),
                busy = loading || busyId != null,
                modifier = Modifier.weight(1f),
                onAction = { if (item.id in owned) onOwned() else onBuy(item) },
            )
        }
        if (items.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PremiumStoreProductTile(
    item: ShopItemDto,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    modifier: Modifier,
    onAction: () -> Unit,
) {
    PurchasedPanel(
        modifier = modifier,
        asset = if (equipped) PurchasedUiAsset.PANEL_LARGE else PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StoreProductPreview(item, Modifier.fillMaxWidth().height(88.dp))
            Text(item.premiumStoreName(), color = Color(0xFF4A2D20), fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.premiumStoreDescription(), color = Color(0xFF765746), fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!owned) PurchasedAsset(PurchasedUiAsset.ICON_COIN, Modifier.size(24.dp))
                Text(
                    when {
                        equipped -> sh("AKTİF", "ACTIVE")
                        owned -> sh("SAHİPSİN", "OWNED")
                        else -> "${item.diamondPrice} SC"
                    },
                    color = if (equipped) Color(0xFF58A957) else Color(0xFF654A3D),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            if (!equipped) {
                PurchasedButton(
                    text = if (owned) sh("AÇ", "OPEN") else sh("AL", "BUY"),
                    onClick = onAction,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    style = if (owned) PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.PRIMARY,
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(31.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumStoreOfflineCard(modifier: Modifier = Modifier) {
    PurchasedPanel(
        modifier = modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(18.dp),
    ) {
        Text(
            sh("Mağaza verisi şu anda kullanılamıyor.", "Shop data is currently unavailable."),
            Modifier.fillMaxWidth(),
            color = Color(0xFF765746),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
