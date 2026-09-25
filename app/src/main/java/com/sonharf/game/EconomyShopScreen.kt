package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    Column(Modifier.fillMaxSize().background(SonHarfBg)) {
        StoreTitleBar(balance = balance, onBack = onBack)
        HorizontalDivider(color = Hf.Gold.copy(alpha = .18f))
        // Categories follow the kinds that are actually on sale; PRO lives in the banner below.
        val categories = listOf(
            Triple(0, null as String?, sh("Öne Çıkan", "Featured")),
            Triple(2, "game_theme", sh("Tema", "Theme")),
            Triple(2, "keyboard_theme", sh("Klavye", "Keyboard")),
            Triple(2, "name_style", sh("İsim", "Name")),
            Triple(2, "victory_effect", sh("Efekt", "Effect")),
            Triple(4, null, sh("Maskotlar", "Mascots")),
            Triple(1, null, sh("Sezon", "Season")),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories.size) { position ->
                val (index, kind, label) = categories[position]
                val selected = tab == index && (index != 2 || kindFilter == kind)
                HfChip(label = label, selected = selected, onClick = {
                    tab = index
                    kindFilter = kind
                })
            }
        }
        Box(Modifier.weight(1f)) {
            if (tab == 1) SeasonCenterContent()
            // Mascot characters, each a permanent Google Play product.
            else if (tab == 4) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                MascotStoreSection()
            }
            else EconomyCatalogScreen(
                section = tab,
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
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(R.drawable.hf_ic_back), sh("Geri", "Back"), tint = Hf.Gold, modifier = Modifier.size(30.dp))
            }
        }
        Text(sh("Mağaza", "Store"), modifier = Modifier.weight(1f), color = Hf.Text, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 1)
        HfPill(modifier = Modifier.padding(end = 2.dp)) {
            HfCoin(20.dp)
            Spacer(Modifier.width(5.dp))
            Text(balance?.let { storeGrouped(it) } ?: "—", color = Hf.Text, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
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
            onBalance(profile?.diamonds)
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

    val featuredFirst = listOfNotNull(
        items.firstOrNull { it.kind == "game_theme" },
        items.firstOrNull { it.kind == "keyboard_theme" },
        items.firstOrNull { it.kind == "name_style" },
    )
    val filtered = when (section) {
        0 -> (featuredFirst + items).distinctBy { it.id }
        2 -> items.filter { kindFilter == null || it.kind == kindFilter }
        else -> emptyList()
    }
    val bundles = storefront?.bundles.orEmpty().filter { b -> b.items.isNotEmpty() && b.items.all { it.isRuntimeReadyStyle() } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (section == 0 || section == 2) {
            item { StoreProBanner(profile?.isVip == true) { onSection(3) } }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Gold, trackColor = Hf.Surface) }

        if (filtered.isEmpty() && !loading && section == 2) {
            item {
                HfCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        sh("Bu kategoride şu anda satışta ürün yok.", "Nothing in this category is on sale right now."),
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        color = Hf.TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        items(filtered.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    val mine = item.id in owned
                    val active = isEquipped(item)
                    VerifiedStoreProductCard(
                        item = item,
                        owned = mine,
                        equipped = active,
                        busy = busy != null || loading,
                        proActive = profile?.isVip == true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        val b = backend
                        if (b == null || busy != null) return@VerifiedStoreProductCard
                        scope.launch {
                            busy = item.id
                            val displayName = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
                            if (mine) {
                                onCollection()
                            } else {
                                runCatching { b.purchaseShopItem(item.id) }
                                    .onSuccess {
                                        notice = sh("$displayName satın alındı. Profil > Koleksiyonum'dan kullanabilirsin.", "$displayName purchased. Equip it from Profile > My Collection.")
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
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (section == 0) {
            item {
                HfSecondaryButton(
                    sh("Son Coin al", "Get Son Coin"),
                    onClick = { showCoins = true },
                    modifier = Modifier.fillMaxWidth(),
                    trailingChevron = true,
                )
            }
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
            if (storefront?.rewardedEnabled == true) item { TextButton(onClick = onRewards) { Text(sh("İsteğe bağlı reklam ödülleri", "Optional ad rewards"), color = Hf.Gold) } }
        }
        if (section == 3) {
            val proActive = profile?.isVip == true
            item { ProShopCard(proActive) { if (proActive) onPro() else showVip = true } }
            item { StoreProBenefits() }
            if (proActive) item {
                TextButton(onClick = onPro, modifier = Modifier.fillMaxWidth()) {
                    Text(sh("PRO ARAÇLARINI AÇ", "OPEN PRO TOOLS"), fontWeight = FontWeight.Black, color = Hf.Gold)
                }
            }
        }

        item {
            HfCard(modifier = Modifier.fillMaxWidth(), borderColor = Hf.Gold.copy(alpha = .4f)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = Hf.Gold, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(sh("ADİL OYUN SÖZÜ", "FAIR PLAY PROMISE"), color = Hf.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(sh("Mağaza ürünleri maç gücü, skor veya rating avantajı sağlamaz.", "Store products never provide match power, score, or rating advantages."), color = Hf.TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            HfCard(modifier = Modifier.fillMaxWidth(), color = Hf.Surface) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = Hf.Text, fontSize = 13.sp, textAlign = TextAlign.Center)
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
                .background(Brush.horizontalGradient(listOf(Hf.Ground, Hf.Surface, Color(0xFFE7D8BE))))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Hf.Gold, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Hf.Gold) {
                Text("PRO", Modifier.padding(horizontal = 10.dp, vertical = 3.dp), color = Hf.Ivory, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (active) sh("PRO üyeliğin aktif. Ayrıcalıklarını gör.", "Your PRO membership is active. See your perks.")
                else sh("Reklamsız oyun, özel içerikler ve daha fazlası!", "Ad-free play, exclusive content and more!"),
                modifier = Modifier.weight(1f),
                color = Hf.Text,
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
private fun VerifiedStoreProductCard(
    item: ShopItemDto,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    proActive: Boolean,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
    val lockedByPro = item.vipOnly && !proActive && !owned

    Surface(
        onClick = onAction,
        enabled = !busy && !equipped && !lockedByPro,
        modifier = modifier.heightIn(min = 198.dp),
        shape = Hf.CardShape,
        color = Hf.Surface,
        border = BorderStroke(if (equipped) 3.dp else 1.5.dp, if (equipped) Hf.Green else Hf.Gold),
        shadowElevation = 3.dp,
    ) {
        Box {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                StoreProductPreview(item, Modifier.fillMaxWidth().height(104.dp))
                Spacer(Modifier.height(10.dp))
                Text(name, color = Hf.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hf.Gold.copy(alpha = .45f)))
                Spacer(Modifier.height(10.dp))
                if (owned || equipped) {
                    Surface(shape = Hf.PillShape, color = Hf.Green) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null, tint = Hf.Ivory, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when {
                                    equipped -> sh("AKTİF", "ACTIVE")
                                    owned -> sh("SAHİPSİN", "OWNED")
                                    else -> ""
                                },
                                color = Hf.Ivory,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    Surface(shape = Hf.PillShape, color = Color(0xFFE6E1D4)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (lockedByPro) {
                                Icon(Icons.Rounded.WorkspacePremium, null, tint = Hf.GoldDeep, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PRO", color = Hf.Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            } else {
                                HfCoin(20.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(storeGrouped(item.diamondPrice), color = Hf.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (equipped) {
                Surface(Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp), shape = CircleShape, color = Hf.Green) {
                    Icon(Icons.Rounded.Check, null, tint = Hf.Ivory, modifier = Modifier.padding(4.dp))
                }
            }
            if (lockedByPro) {
                Box(Modifier.matchParentSize().background(Hf.Ground.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.hf_ic_lock), null, tint = Hf.Gold, modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

@Composable
private fun ProShopCard(active: Boolean, onClick: () -> Unit) {
    HfCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), borderColor = Hf.Gold) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(40.dp), tint = Hf.Gold)
                    Column {
                        Text("PRO", color = Hf.Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(sh("Reklamsız + profil + analiz", "Ad-free + profile + analysis"), color = Hf.TextMuted, fontSize = 12.sp)
                    }
                }
                Text(if (active) sh("AKTİF", "ACTIVE") else sh("KEŞFET ›", "EXPLORE ›"), color = if (active) Hf.GreenLight else Hf.Gold, fontWeight = FontWeight.Black)
            }
            Text(sh("Özel oda • PRO profil rozeti • gelişmiş istatistik • reklamsız deneyim • sosyal ayrıcalıklar", "Private rooms • PRO profile badge • advanced stats • ad-free experience • social benefits"), color = Hf.Text, fontSize = 13.sp)
            Text(sh("PRO, dereceli maçlarda skor, kelime ipucu veya rating avantajı vermez.", "PRO provides no score, word-hint, or rating advantage in ranked matches."), color = Hf.GreenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun storeKindLabel(kind: String?): String = when (kind) {
    null -> sh("Tümü", "All")
    "game_theme" -> sh("Oyun teması", "Game theme")
    "keyboard_theme" -> sh("Klavye", "Keyboard")
    "name_style" -> sh("İsim stili", "Name style")
    "profile_frame" -> sh("Çerçeve", "Frame")
    "victory_effect", "vfx" -> sh("Efekt", "Effect")
    else -> kind.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
