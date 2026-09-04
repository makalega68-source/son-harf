package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private enum class ProductionStoreCategory(val tr: String, val en: String, val icon: ImageVector) {
    VIP("VIP", "VIP", Icons.Rounded.WorkspacePremium),
    STYLE("STYLE", "STYLE", Icons.Rounded.Palette),
    SON_COIN("SON COIN", "SON COIN", Icons.Rounded.Toll),
    SEASON("SEZON PASS", "SEASON PASS", Icons.Rounded.ConfirmationNumber),
    PACKS("PAKETLER", "PACKS", Icons.Rounded.Inventory2),
    KASA("KASA", "PIGGY BANK", Icons.Rounded.Savings),
    EVENT("ETKİNLİK", "EVENT", Icons.Rounded.EventAvailable),
}

@Composable
internal fun MonsterStyleStoreScreen() {
    val backend = remember { if (SupabaseProvider.configured) runCatching { OnlineGameBackend() }.getOrNull() else null }
    var category by remember { mutableStateOf(ProductionStoreCategory.STYLE) }
    var balance by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun reloadBalance() {
        val id = backend?.currentUserId() ?: return
        runCatching { backend.getProfile(id) }.onSuccess { balance = it.diamonds }
    }

    LaunchedEffect(backend) {
        reloadBalance()
        runCatching { backend?.trackStoreEvent("store_view") }
    }

    Column(
        Modifier.fillMaxSize().background(SonHarfTheme.Background).statusBarsPadding(),
    ) {
        StoreProductionHeader(balance)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductionStoreCategory.entries.forEach { item ->
                FilterChip(
                    selected = category == item,
                    onClick = {
                        category = item
                        scope.launch { runCatching { backend?.trackStoreEvent("product_view", item.name.lowercase()) } }
                    },
                    leadingIcon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(17.dp)) },
                    label = { Text(if (SonHarfUiState.isEnglish) item.en else item.tr, fontSize = 10.sp, fontWeight = FontWeight.Black) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (category) {
                ProductionStoreCategory.VIP -> VipStoreCategoryScreen(onChanged = { scope.launch { reloadBalance() } })
                ProductionStoreCategory.STYLE -> StyleCatalogCategoryScreen(backend = backend, eventOnly = false, onChanged = { scope.launch { reloadBalance() } })
                ProductionStoreCategory.SON_COIN -> StoreSingleCardScroll { GooglePlayCoinProductsCard { scope.launch { reloadBalance() } } }
                ProductionStoreCategory.SEASON -> StoreSingleCardScroll { SeasonPassPurchaseCard { scope.launch { reloadBalance() } } }
                ProductionStoreCategory.PACKS -> StoreSingleCardScroll { GooglePlayPackProductsCard { scope.launch { reloadBalance() } } }
                ProductionStoreCategory.KASA -> RewardCenterScreen(showKasaOnly = true)
                ProductionStoreCategory.EVENT -> StyleCatalogCategoryScreen(backend = backend, eventOnly = true, onChanged = { scope.launch { reloadBalance() } })
            }
        }
    }
}

@Composable
private fun StoreProductionHeader(balance: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(sh("SON HARF MAĞAZASI", "SON HARF STORE"), color = SonHarfTheme.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(sh("Style • koleksiyon • üyelik • sezon", "Style • collection • membership • season"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp)
        }
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = SonHarfTheme.SurfaceSecondary,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Toll, contentDescription = null, tint = SonHarfTheme.PrimaryBlue, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("$balance SC", color = SonHarfTheme.PrimaryBlue, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StoreSingleCardScroll(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VipStoreCategoryScreen(onChanged: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    StoreSingleCardScroll {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FF)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, SonHarfTheme.PrimaryBlue.copy(alpha = .28f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = SonHarfGold, modifier = Modifier.size(42.dp))
                Text("SON HARF VIP", color = SonHarfTheme.PrimaryBlue, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text(sh("Daha kolay. Daha detaylı. Daha kişisel.", "Easier. More detailed. More personal."), color = SonHarfTheme.TextSecondary)
                Text(
                    sh(
                        "Arkadaş Listesi • Otomatik Puan Hesabı • Maç Kelimeleri • Gelişmiş İstatistik • Reklamsızlık • VIP Style",
                        "Friend List • Automatic Score Breakdown • Match Words • Advanced Statistics • Ad-free • VIP Style",
                    ),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Surface(color = SonHarfTheme.Success.copy(alpha = .10f), shape = RoundedCornerShape(14.dp)) {
                    Text(
                        sh(
                            "VIP ranked maçta süre, puan, hamle, rating, eşleşme veya canlı karar avantajı vermez.",
                            "VIP gives no time, score, move, rating, matchmaking or live-decision advantage in ranked play.",
                        ),
                        Modifier.padding(12.dp),
                        color = SonHarfTheme.Success,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.PrimaryBlue),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(sh("VIP PLANLARINI GÖR", "VIEW VIP PLANS"), fontWeight = FontWeight.Black) }
            }
        }
    }
    if (showDialog) VipPurchaseDialog(onVerified = { onChanged(); showDialog = false }, onDismiss = { showDialog = false })
}

@Composable
private fun StyleCatalogCategoryScreen(
    backend: OnlineGameBackend?,
    eventOnly: Boolean,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        val b = backend
        if (b == null) {
            notice = sh("Mağaza sunucu bağlantısı olmadan satın alma yapmaz.", "The store does not make purchases without a server connection.")
            loading = false
            return
        }
        val id = b.currentUserId()
        if (id == null) {
            notice = sh("Oyuncu oturumu hazırlanamadı.", "Player session is not ready.")
            loading = false
            return
        }
        runCatching {
            profile = b.getProfile(id)
            items = b.getShopItems()
            owned = b.getInventory()
            equipped = b.getEquippedCosmetics()
            SonHarfCosmetics.apply(equipped)
        }.onFailure { notice = sh("Style kataloğu yüklenemedi.", "Style catalog could not be loaded.") }
        loading = false
    }

    LaunchedEffect(backend, eventOnly) { reload() }

    val supportedKinds = setOf(
        "profile_frame", "avatar_background", "nameplate", "badge", "title", "name_style",
        "game_theme", "keyboard_theme", "vs_intro", "word_effect", "victory_effect", "emote", "emoji_pack",
    )
    val visible = items.filter { it.kind in supportedKinds }.filter {
        if (eventOnly) it.rarity in setOf("EVENT", "SEASON") else it.rarity !in setOf("EVENT")
    }

    fun isEquipped(item: ShopItemDto): Boolean = when (item.kind) {
        "profile_frame" -> equipped?.profileFrameId == item.id
        "avatar_background" -> equipped?.avatarBackgroundId == item.id
        "nameplate" -> equipped?.nameplateId == item.id
        "badge" -> equipped?.badgeId == item.id
        "title" -> equipped?.titleStyleId == item.id
        "name_style" -> equipped?.nameStyleId == item.id
        "game_theme" -> equipped?.gameThemeId == item.id
        "keyboard_theme" -> equipped?.keyboardThemeId == item.id
        "vs_intro" -> equipped?.vsIntroId == item.id
        "word_effect" -> equipped?.wordEffectId == item.id
        "victory_effect" -> equipped?.victoryEffectId == item.id
        "emote" -> equipped?.emoteId == item.id
        "emoji_pack" -> equipped?.emojiPackId == item.id
        else -> false
    }

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(if (eventOnly) sh("ETKİNLİK STYLE", "EVENT STYLE") else "STYLE", color = SonHarfTheme.PrimaryBlue, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(
                if (eventOnly) sh("Yalnız aktif sezon ve etkinlik koleksiyonları gösterilir.", "Only active season and event collections are shown.")
                else sh("Görünüm, prestij ve koleksiyon. Hiçbir Style ürünü oyun gücü vermez.", "Appearance, prestige and collection. No Style item grants gameplay power."),
                color = SonHarfTheme.TextSecondary,
                fontSize = 10.sp,
            )
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!loading && visible.isEmpty()) item {
            StoreEmptyState(
                if (eventOnly) sh("Şu anda aktif etkinlik koleksiyonu yok.", "There is no active event collection right now.")
                else sh("Aktif Style ürünü bulunamadı.", "No active Style items were found."),
            )
        }
        items(visible.size, key = { visible[it].id }) { index ->
            val item = visible[index]
            val mine = item.id in owned
            val active = isEquipped(item)
            StyleProductCard(
                item = item,
                owned = mine,
                active = active,
                vipActive = profile?.isVip == true,
                busy = busy == item.id,
                onPrimary = {
                    val b = backend ?: return@StyleProductCard
                    scope.launch {
                        busy = item.id
                        runCatching {
                            if (!mine) b.purchaseShopItem(item.id) else if (!active) b.equipShopItem(item.id)
                        }.onSuccess {
                            runCatching { b.trackStoreEvent(if (mine) "equip" else "purchase_success", item.id) }
                            reload(); onChanged()
                        }.onFailure { error ->
                            notice = when {
                                "insufficient_diamonds" in error.message.orEmpty() -> sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                "vip_required" in error.message.orEmpty() -> sh("Bu Style VIP üyelerine özel.", "This Style is VIP-only.")
                                else -> sh("Style işlemi tamamlanamadı.", "Style action could not be completed.")
                            }
                        }
                        busy = null
                    }
                },
                onTrial = if (!mine && item.trialMode in setOf("match", "minutes") && (item.trialValue ?: 0) > 0) {
                    {
                        val b = backend ?: return@StyleProductCard
                        scope.launch {
                            busy = item.id
                            runCatching { b.startStyleTrial(item.id); b.equipRewardTrial() }
                                .onSuccess {
                                    runCatching { b.trackStoreEvent("preview_start", item.id) }
                                    notice = if (item.trialMode == "match") sh("1 maçlık deneme etkin.", "1-match trial is active.") else sh("30 dakikalık deneme etkin.", "30-minute trial is active.")
                                    reload()
                                }
                                .onFailure { error ->
                                    notice = if ("trial_daily_limit_reached" in error.message.orEmpty()) sh("Bu ürünün bugünkü denemesi kullanıldı.", "Today's trial for this item has been used.") else sh("Deneme başlatılamadı.", "Trial could not be started.")
                                }
                            busy = null
                        }
                    }
                } else null,
            )
        }
        notice?.let { message -> item { StoreNoticeCard(message) } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StyleProductCard(
    item: ShopItemDto,
    owned: Boolean,
    active: Boolean,
    vipActive: Boolean,
    busy: Boolean,
    onPrimary: () -> Unit,
    onTrial: (() -> Unit)?,
) {
    val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
    val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
    val rarityLabel = item.rarity.uppercase()
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfTheme.Surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) SonHarfTheme.PrimaryBlue else SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StyleLivePreview(item)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = SonHarfTheme.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(description, color = SonHarfTheme.TextSecondary, fontSize = 9.sp, maxLines = 2)
                }
                Surface(color = SonHarfTheme.SurfaceSecondary, shape = RoundedCornerShape(9.dp)) {
                    Text(rarityLabel, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (item.rarity == "VIP") SonHarfGold else SonHarfTheme.PrimaryBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when { active -> sh("AKTİF", "ACTIVE"); owned -> sh("SAHİPSİN", "OWNED"); else -> "${item.diamondPrice} SC" },
                        color = if (active || owned) SonHarfTheme.Success else SonHarfTheme.PrimaryBlue,
                        fontWeight = FontWeight.Black,
                    )
                    if (item.vipOnly) Text("VIP", color = SonHarfGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onPrimary,
                    enabled = !busy && !active && (!item.vipOnly || vipActive),
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        when { busy -> sh("İŞLENİYOR", "PROCESSING"); active -> sh("AKTİF", "ACTIVE"); owned -> sh("KULLAN", "EQUIP"); else -> sh("SATIN AL", "BUY") },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (onTrial != null) {
                OutlinedButton(onClick = onTrial, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (item.trialMode == "match") sh("1 MAÇ DENE", "TRY 1 MATCH") else sh("30 DAKİKA DENE", "TRY 30 MINUTES"),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleLivePreview(item: ShopItemDto) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(122.dp),
        color = SonHarfTheme.SurfaceSecondary,
        shape = RoundedCornerShape(18.dp),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            when (item.kind) {
                "profile_frame" -> Box(
                    Modifier.size(78.dp).background(SonHarfTheme.PrimaryBlue.copy(alpha = .12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = CircleShape, color = SonHarfTheme.Surface, border = BorderStroke(5.dp, if (item.vipOnly) SonHarfGold else SonHarfTheme.PrimaryBlue)) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, contentDescription = null, tint = SonHarfTheme.TextPrimary) }
                    }
                }
                "game_theme" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(5) { col ->
                                Box(Modifier.weight(1f).height(24.dp).background(if ((row + col) % 3 == 0) SonHarfTheme.PrimaryBlue.copy(alpha = .22f) else SonHarfTheme.Surface, RoundedCornerShape(6.dp)))
                            }
                        }
                    }
                }
                "keyboard_theme" -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(6) { Surface(shape = RoundedCornerShape(6.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) { Box(Modifier.size(30.dp, 22.dp)) } }
                        }
                    }
                }
                "name_style", "nameplate", "title" -> Text(sh("OYUNCU", "PLAYER"), color = SonHarfTheme.PrimaryBlue, fontSize = 25.sp, fontWeight = FontWeight.Black)
                "victory_effect" -> Icon(Icons.Rounded.Celebration, contentDescription = null, tint = SonHarfGold, modifier = Modifier.size(58.dp))
                "vs_intro" -> Text("VS", color = SonHarfTheme.PrimaryBlue, fontSize = 44.sp, fontWeight = FontWeight.Black)
                "word_effect" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sh("KELİME", "WORD"), fontWeight = FontWeight.Black); Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SonHarfTheme.PrimaryBlue)
                }
                "badge" -> Icon(Icons.Rounded.Verified, contentDescription = null, tint = SonHarfGold, modifier = Modifier.size(56.dp))
                "avatar_background" -> Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = SonHarfTheme.PrimaryBlue, modifier = Modifier.size(56.dp))
                "emote", "emoji_pack" -> Icon(Icons.Rounded.Forum, contentDescription = null, tint = SonHarfTheme.PrimaryBlue, modifier = Modifier.size(54.dp))
                else -> Icon(Icons.Rounded.Palette, contentDescription = null, tint = SonHarfTheme.PrimaryBlue, modifier = Modifier.size(54.dp))
            }
        }
    }
}

@Composable
private fun StoreEmptyState(message: String) {
    Surface(color = SonHarfTheme.Surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(32.dp))
            Text(message, color = SonHarfTheme.TextSecondary, textAlign = TextAlign.Center, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StoreNoticeCard(message: String) {
    Surface(color = SonHarfTheme.SurfaceSecondary, shape = RoundedCornerShape(14.dp)) {
        Text(message, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfTheme.TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
