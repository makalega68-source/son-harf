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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
fun EconomyShopScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
    onOpenProfileAppearance: (() -> Unit)? = null,
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    Column(Modifier.fillMaxSize().background(MainUi.Background)) {
        if (onBack != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back"), tint = MainUi.Text) }
                Column {
                    Text(sh("Style ve Ödüller", "Style & Rewards"), color = MainUi.Text, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(sh("Satın al • kazan • profilden uygula", "Buy • earn • apply from Profile"), color = MainUi.Muted, fontSize = 9.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("STYLE", fontSize = 10.sp) }, modifier = Modifier.weight(1f))
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(sh("ÖDÜLLER", "REWARDS"), fontSize = 10.sp) }, modifier = Modifier.weight(1f))
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0) EconomyCatalogScreen(onOpenProfileAppearance) else RewardCenterScreen()
        }
    }
}

@Composable
private fun EconomyCatalogScreen(onOpenProfileAppearance: (() -> Unit)?) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var category by remember { mutableIntStateOf(0) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            notice = sh("Mağaza sunucu bağlantısı olmadan kullanılamaz.", "The shop requires a server connection.")
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
            SonHarfCosmetics.apply(b.getEquippedCosmetics())
        }.onFailure { notice = sh("Mağaza verileri yüklenemedi.", "Shop data could not be loaded.") }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val supportedKinds = setOf("profile_frame", "name_style", "game_theme", "keyboard_theme", "victory_effect", "emoji_pack", "mascot")
    val filtered = items.filter { it.kind in supportedKinds && it.id != "theme_neon" }.filter { item ->
        when (category) {
            1 -> item.kind in setOf("profile_frame", "name_style")
            2 -> item.kind == "emoji_pack"
            3 -> item.kind in setOf("victory_effect", "game_theme", "keyboard_theme")
            else -> true
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("STYLE", color = MainUi.Blue, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(sh("Görünümünü kişiselleştir; maç gücü satın alma", "Personalize your appearance; never buy match power"), color = MainUi.Muted, fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = MainUi.Gold.copy(alpha = .14f), border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .30f))) {
                    Text("◈ ${profile?.diamonds ?: 0} SC", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MainUi.Gold, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            ScrollableTabRow(selectedTabIndex = category, edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}) {
                listOf(sh("TÜMÜ", "ALL"), sh("PROFİL", "PROFILE"), sh("İFADE", "EMOTES"), sh("DİĞER", "OTHER")).forEachIndexed { index, label ->
                    Tab(selected = category == index, onClick = { category = index }, text = {
                        Text(label, color = if (category == index) MainUi.Blue else MainUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    })
                }
            }
        }

        item {
            Surface(color = MainUi.BlueSoft, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Checkroom, null, tint = MainUi.Blue)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        sh("Mağaza satın alma ve keşif alanıdır. Sahip olduğun Style öğelerini Profil > Görünümümü düzenle bölümünden uygula.", "The shop is for discovery and purchases. Apply owned Style items from Profile > Edit appearance."),
                        color = MainUi.Text,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }

        items(filtered, key = { it.id }) { item ->
            val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
            val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
            val mine = item.id in owned
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MainUi.Surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StyleItemVisual(item)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (item.vipOnly) {
                                    Surface(color = MainUi.Gold.copy(alpha = .14f), shape = RoundedCornerShape(8.dp)) {
                                        Text("VIP", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = MainUi.Gold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Text(description, color = MainUi.Muted, fontSize = 9.sp, maxLines = 2)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (mine) sh("✓ SAHİPSİN", "✓ OWNED") else "◈ ${item.diamondPrice} SC",
                            color = if (mine) MainUi.Green else MainUi.Gold,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                        )
                        Button(
                            onClick = {
                                if (mine) {
                                    onOpenProfileAppearance?.invoke()
                                    if (onOpenProfileAppearance == null) notice = sh("Bu öğeyi Profil > Görünümümü düzenle bölümünden uygulayabilirsin.", "Apply this item from Profile > Edit appearance.")
                                    return@Button
                                }
                                val b = backend ?: return@Button
                                scope.launch {
                                    busy = item.id
                                    runCatching { b.purchaseShopItem(item.id) }
                                        .onSuccess {
                                            notice = sh("Satın alma tamamlandı. Ürün Profil görünüm envanterine eklendi.", "Purchase complete. The item was added to your Profile appearance inventory.")
                                            reload()
                                        }
                                        .onFailure {
                                            val raw = it.message.orEmpty()
                                            notice = when {
                                                "insufficient_diamonds" in raw -> sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                                "vip_required" in raw -> sh("Bu Style öğesi VIP üyelerine özel.", "This Style item is VIP only.")
                                                "already_owned" in raw -> sh("Bu ürüne zaten sahipsin.", "Already owned.")
                                                else -> sh("Satın alma tamamlanamadı.", "Purchase failed.")
                                            }
                                        }
                                    busy = null
                                }
                            },
                            enabled = busy == null && (mine || !item.vipOnly || profile?.isVip == true),
                            colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(when { busy == item.id -> "…"; mine -> sh("PROFİLDE UYGULA", "APPLY IN PROFILE"); else -> sh("SATIN AL", "BUY") }, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            if (mine) { Spacer(Modifier.width(3.dp)); Icon(Icons.Rounded.ChevronRight, null, Modifier.size(14.dp)) }
                        }
                    }
                }
            }
        }

        item { MainSectionTitle(sh("SEZON BİLETİ VE SON COIN", "SEASON PASS & SON COIN")) }
        item { SeasonPassPurchaseCard { scope.launch { reload() } } }
        item { GooglePlayProductsCard { scope.launch { reload() } } }

        item {
            Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(16.dp)) {
                Text(
                    sh("Son Coin yalnızca Style ve güç vermeyen kişiselleştirme için kullanılır. Satın alımlar puan, süre, rating, lig veya joker avantajı vermez.", "Son Coin is only for Style and non-power personalization. Purchases never grant score, time, rating, league or joker advantages."),
                    Modifier.fillMaxWidth().padding(12.dp), color = MainUi.Muted, fontSize = 9.sp,
                )
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(15.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = MainUi.Text, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StyleItemVisual(item: ShopItemDto) {
    val accent = when (item.kind) {
        "profile_frame" -> MainUi.Blue
        "name_style" -> MainUi.Purple
        "emoji_pack" -> MainUi.Gold
        "victory_effect" -> Color(0xFFF97316)
        "keyboard_theme" -> MainUi.Green
        "game_theme" -> MainUi.Purple
        "mascot" -> Color(0xFF22A6A1)
        else -> MainUi.Blue
    }
    Surface(
        modifier = Modifier.size(76.dp),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = .09f),
        border = BorderStroke(1.dp, accent.copy(alpha = .20f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (item.kind) {
                "profile_frame" -> {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(Modifier.size(52.dp, 62.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(3.dp, accent)) {}
                        Icon(Icons.Rounded.Person, null, tint = MainUi.Text.copy(alpha = .78f), modifier = Modifier.size(30.dp))
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MainUi.Gold, modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(18.dp))
                    }
                }
                "name_style" -> Text("Aa", color = accent, fontSize = 30.sp, fontWeight = FontWeight.Black)
                "emoji_pack" -> Text("😎✨", fontSize = 24.sp)
                "victory_effect" -> Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(40.dp))
                "keyboard_theme" -> {
                    Column(Modifier.width(52.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(4) { Surface(Modifier.weight(1f).height(10.dp), shape = RoundedCornerShape(3.dp), color = accent.copy(alpha = .75f)) {} } } }
                    }
                }
                "game_theme" -> Icon(Icons.Rounded.GridOn, null, tint = accent, modifier = Modifier.size(40.dp))
                "mascot" -> {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(Modifier.size(48.dp), shape = CircleShape, color = accent.copy(alpha = .23f)) {}
                        Icon(Icons.Rounded.Pets, null, tint = accent, modifier = Modifier.size(34.dp))
                    }
                }
                else -> Icon(Icons.Rounded.Diamond, null, tint = accent, modifier = Modifier.size(36.dp))
            }
        }
    }
}
