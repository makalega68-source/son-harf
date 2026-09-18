package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val icon: ImageVector,
    val accent: Color,
    val kinds: Set<String>,
)

private val premiumStoreCategories = listOf(
    PremiumStoreCategory("Temalar", "Themes", Icons.Rounded.Palette, Color(0xFF7C3AED), setOf("game_theme")),
    PremiumStoreCategory("Profil Çerçeveleri", "Profile Frames", Icons.Rounded.AccountCircle, Color(0xFF2563EB), setOf("profile_frame")),
    PremiumStoreCategory("Tuş Stilleri", "Keyboard Styles", Icons.Rounded.Keyboard, Color(0xFF12B8A6), setOf("keyboard_theme")),
    PremiumStoreCategory("İsim & Prestij", "Name & Prestige", Icons.Rounded.AutoAwesome, Color(0xFFF97316), setOf("name_style", "victory_effect", "emoji_pack")),
)

@OptIn(ExperimentalMaterial3Api::class)
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
            products = b.getShopItems()
                .filter { it.kind in PremiumStoreKinds }
                .filterNot { it.id in setOf("theme_dark_arena", "theme_monster_blue", "theme_aurora", "theme_neon", "theme_midnight") }
                .sortedWith(compareBy<ShopItemDto> { premiumStoreKindOrder(it.kind) }.thenBy { it.sortOrder }.thenBy { it.id })
            owned = b.getInventory()
            equipped = b.getEquippedCosmetics()
            storefront = runCatching { b.getStorefront() }.getOrNull()
            SonHarfCosmetics.apply(equipped)
            onMembershipChanged(profile?.isVip == true)
        }.onFailure {
            notice = sh("Mağaza verileri yenilenemedi.", "Shop data could not be refreshed.")
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().background(Color.Transparent)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = SonHarfTheme.Surface,
                border = BorderStroke(1.dp, SonHarfTheme.Border),
                shadowElevation = 2.dp,
            ) {
                Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = SonHarfTheme.TextPrimary, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Mağaza", "Shop"), color = SonHarfTheme.TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(sh("Tarzını seç, koleksiyonunu oluştur", "Choose your style and build your collection"), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
            }
            Surface(
                onClick = { showCoins = true },
                shape = RoundedCornerShape(99.dp),
                color = SonHarfTheme.ActionOrange.copy(alpha = .11f),
                border = BorderStroke(1.dp, SonHarfTheme.ActionOrange.copy(alpha = .20f)),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Toll, null, tint = SonHarfTheme.ActionOrange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("${profile?.diamonds ?: 0}", color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 12.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { positions ->
                if (tab < positions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[tab]),
                        color = when (tab) {
                            0 -> SonHarfTheme.Primary
                            1 -> SonHarfTheme.Turquoise
                            2 -> SonHarfTheme.ActionOrange
                            else -> SonHarfTheme.Purple
                        },
                    )
                }
            },
        ) {
            listOf(sh("Öne Çıkan", "Featured"), sh("Sezon", "Season"), sh("Görünümler", "Styles"), "PRO")
                .forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = {
                            Text(
                                label,
                                color = if (tab == index) SonHarfTheme.TextPrimary else SonHarfTheme.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (tab == index) FontWeight.Black else FontWeight.Medium,
                            )
                        },
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
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                if (loading) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfTheme.Turquoise, trackColor = SonHarfTheme.SurfaceSecondary) }
                }

                if (tab == 0) {
                    item {
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
                    item {
                        PremiumStoreProHero(profile?.isVip == true) { tab = 3 }
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
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(sh("Seçili Görünümler", "Selected Styles"), Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            TextButton(onClick = { tab = 2 }) { Text(sh("TÜMÜ", "ALL"), color = SonHarfTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                        }
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
                                    icon = category.icon,
                                    accent = category.accent,
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
                    if (!loading && products.isEmpty()) {
                        item { PremiumStoreOfflineCard() }
                    }
                }

                notice?.let { message ->
                    item {
                        Surface(shape = RoundedCornerShape(15.dp), color = SonHarfTheme.PrimarySoft) {
                            Text(message, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfTheme.TextPrimary, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    if (showCoins) {
        ModalBottomSheet(onDismissRequest = { showCoins = false }, containerColor = SonHarfTheme.Surface) {
            GooglePlayProductsCard { scope.launch { reload() } }
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
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
    PremiumStoreBlackThemeId -> "Black Theme"
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 4.dp,
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(SonHarfTheme.Primary, SonHarfTheme.Purple, SonHarfTheme.Turquoise)),
                RoundedCornerShape(22.dp),
            ).padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f)) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.padding(11.dp).size(24.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("PRO", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (active) sh("Üyeliğin aktif · Ayrıcalıklarını gör", "Membership active · View your benefits")
                        else sh("Reklamsız kullanım · premium stil · analiz", "Ad-free · premium style · analysis"),
                        color = Color.White.copy(alpha = .84f),
                        fontSize = 9.sp,
                    )
                }
                Text(if (active) sh("AÇ", "OPEN") else sh("KEŞFET", "EXPLORE"), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(3.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PremiumStoreFeaturedTheme(item: ShopItemDto, owned: Boolean, busy: Boolean, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0B0E14),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = .45f)),
        shadowElevation = 5.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(sh("ÖNE ÇIKAN TEMA", "FEATURED THEME"), color = Color(0xFF1FD1C2), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    Text("Black Theme", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(item.premiumStoreDescription(), color = Color.White.copy(alpha = .68f), fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(12.dp))
                StoreProductPreview(item, Modifier.size(width = 118.dp, height = 88.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (owned) sh("KOLEKSİYONUNDA", "IN COLLECTION") else "${item.diamondPrice} Son Coin",
                    color = if (owned) Color(0xFF1FD1C2) else Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onAction,
                    enabled = !busy,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (owned) Color(0xFF7C3AED) else Color(0xFF2563EB), contentColor = Color.White),
                ) {
                    Text(if (owned) sh("KOLEKSİYONA GİT", "OPEN COLLECTION") else sh("SATIN AL", "BUY"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PremiumStoreCategoryHeader(title: String, count: Int, icon: ImageVector, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(11.dp), color = accent.copy(alpha = .10f)) {
            Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.width(9.dp))
        Text(title, Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .09f)) {
            Text(count.toString(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = SonHarfTheme.Surface,
        border = BorderStroke(if (equipped) 1.5.dp else 1.dp, if (equipped) SonHarfTheme.Turquoise else SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            StoreProductPreview(item, Modifier.fillMaxWidth().height(92.dp))
            Text(item.premiumStoreName(), color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.premiumStoreDescription(), color = SonHarfTheme.TextSecondary, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        equipped -> sh("AKTİF", "ACTIVE")
                        owned -> sh("SAHİPSİN", "OWNED")
                        else -> "${item.diamondPrice} SC"
                    },
                    color = when {
                        equipped -> SonHarfTheme.Turquoise
                        owned -> SonHarfTheme.Success
                        else -> SonHarfTheme.ActionOrange
                    },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = onAction,
                    enabled = !busy && !equipped,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (owned) SonHarfTheme.Purple else SonHarfTheme.Primary,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumStoreOfflineCard(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Text(
            sh("Mağaza verisi şu anda kullanılamıyor.", "Shop data is currently unavailable."),
            Modifier.fillMaxWidth().padding(18.dp),
            color = SonHarfTheme.TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}
