package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2, SonHarfBg))
        )
    ) {
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
                    Text(sh("Style • Görünüm • Efektler", "Style • Appearance • Effects"), color = SonHarfMuted, fontSize = 9.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(sh("STYLE", "STYLE"), fontSize = 10.sp) }, modifier = Modifier.weight(1f))
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(sh("ÖDÜLLER", "REWARDS"), fontSize = 10.sp) }, modifier = Modifier.weight(1f))
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> EconomyCatalogScreen()
                else -> RewardCenterScreen()
            }
        }
    }
}

@Composable
private fun EconomyCatalogScreen() {
    val context = LocalContext.current
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
            val nextProfile = b.getProfile(id)
            val nextItems = b.getShopItems()
            val nextOwned = b.getInventory()
            val nextEquipped = b.getEquippedCosmetics()
            profile = nextProfile
            items = nextItems
            owned = nextOwned
            equipped = nextEquipped
            SonHarfCosmetics.apply(nextEquipped)
        }.onFailure {
            notice = sh("Mağaza verileri yüklenemedi.", "Shop data could not be loaded.")
        }
    }

    LaunchedEffect(Unit) { loading = true; reload(); loading = false }

    fun isEquipped(item: ShopItemDto): Boolean = when (item.kind) {
        "profile_frame" -> equipped?.profileFrameId == item.id
        "name_style" -> equipped?.nameStyleId == item.id
        "game_theme" -> equipped?.gameThemeId == item.id
        "keyboard_theme" -> equipped?.keyboardThemeId == item.id
        "victory_effect" -> equipped?.victoryEffectId == item.id
        "emoji_pack" -> equipped?.emojiPackId == item.id
        else -> false
    }

    val supportedKinds = setOf("profile_frame", "name_style", "game_theme", "victory_effect", "emoji_pack")
    val filtered = items.filter { it.kind in supportedKinds }.filter { item ->
        when (category) {
            1 -> item.kind in setOf("profile_frame", "name_style")
            2 -> item.kind == "game_theme"
            3 -> item.kind in setOf("victory_effect", "emoji_pack")
            else -> true
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("STYLE", color = SonHarfBlue, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(sh("Profilini ve görünümünü kişiselleştir • güç satın alma", "Personalize your profile and appearance • never buy power"), color = SonHarfMuted, fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = SonHarfCyan.copy(alpha = .13f), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f))) {
                    Text("◈ ${profile?.diamonds ?: 0} SC", Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = SonHarfCyan, fontWeight = FontWeight.Black)
                }
            }
        }

        item { AnimatedVipShopCard(profile?.isVip == true) { showVip = true } }
        item { SeasonPassPurchaseCard { scope.launch { reload() } } }
        item { GooglePlayProductsCard { scope.launch { reload() } } }

        item {
            ScrollableTabRow(selectedTabIndex = category, edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}) {
                listOf(sh("TÜMÜ", "ALL"), sh("PROFİL", "PROFILE"), sh("TEMA", "THEME"), sh("EFEKT", "EFFECTS")).forEachIndexed { index, label ->
                    Tab(selected = category == index, onClick = { category = index }, text = { Text(label, color = if (category == index) SonHarfCyan else SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                }
            }
        }

        item {
            Text(sh("SON COIN İLE STYLE", "STYLE WITH SON COIN"), color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(sh("Son Coin maç avantajı vermez; yalnızca Style, görünüm ve kişiselleştirme içindir.", "Son Coin never gives match advantages; it is only for Style and personalization."), color = SonHarfMuted, fontSize = 9.sp)
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfCyan) }

        items(filtered, key = { it.id }) { item ->
            val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
            val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
            val mine = item.id in owned
            val active = isEquipped(item)
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .96f)),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(if (active) 1.6.dp else 1.dp, if (active) SonHarfCyan else SonHarfMuted.copy(alpha = .13f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CosmeticPreview(item)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text(description, color = SonHarfMuted, fontSize = 10.sp)
                        }
                        if (item.vipOnly) Surface(color = SonHarfGold.copy(alpha = .15f), shape = RoundedCornerShape(9.dp)) { Text("VIP", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SonHarfGold, fontWeight = FontWeight.Black) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mine) sh("✓ SAHİPSİN", "✓ OWNED") else "◈ ${item.diamondPrice} SC", color = if (mine) SonHarfGreen else SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        val buttonContainer = if (active) SonHarfGreen else SonHarfPurple
                        val anotherItemBusy = busy != null && busy != item.id
                        Button(
                            onClick = {
                                val b = backend
                                if (b == null) {
                                    notice = sh("Mağaza şu anda çevrimdışı.", "The shop is currently offline.")
                                    return@Button
                                }
                                scope.launch {
                                    busy = item.id
                                    if (mine) {
                                        runCatching { b.equipShopItem(item.id) }
                                            .onSuccess {
                                                notice = sh("${name} etkinleştirildi.", "$name equipped.")
                                                reload()
                                            }
                                            .onFailure { notice = sh("Style öğesi etkinleştirilemedi.", "Style item could not be equipped.") }
                                    } else {
                                        runCatching { b.purchaseShopItem(item.id) }
                                            .onSuccess { notice = sh("Satın alma tamamlandı. Şimdi kullanabilirsin.", "Purchase complete. You can equip it now."); reload() }
                                            .onFailure {
                                                val raw = it.message.orEmpty()
                                                notice = when {
                                                    "insufficient_diamonds" in raw -> sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                                    "vip_required" in raw -> sh("Bu Style öğesi VIP üyelerine özel.", "This Style item is VIP only.")
                                                    "already_owned" in raw -> sh("Bu ürüne zaten sahipsin.", "Already owned.")
                                                    else -> sh("Satın alma tamamlanamadı.", "Purchase failed.")
                                                }
                                            }
                                    }
                                    busy = null
                                }
                            },
                            enabled = busy == null && (!item.vipOnly || profile?.isVip == true),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonContainer,
                                disabledContainerColor = if (anotherItemBusy) buttonContainer else buttonContainer.copy(alpha = .55f),
                                disabledContentColor = if (anotherItemBusy) Color.White else Color.White.copy(alpha = .72f),
                            ),
                        ) {
                            Text(when { busy == item.id -> "…"; active -> sh("AKTİF", "EQUIPPED"); mine -> sh("KULLAN", "EQUIP"); else -> sh("SATIN AL", "BUY") }, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = SonHarfSurface2, shape = RoundedCornerShape(15.dp)) { Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfText, fontSize = 10.sp, textAlign = TextAlign.Center) }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
    if (showVip) VipPurchaseDialog { showVip = false }
}

@Composable
private fun AnimatedVipShopCard(active: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shopVip")
    val pulse by transition.animateFloat(.96f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "shopVipScale")
    val glow by transition.animateFloat(.12f, .34f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "shopVipGlow")
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = SonHarfGold.copy(alpha = .08f + glow / 5f)), shape = RoundedCornerShape(25.dp), border = BorderStroke(1.5.dp, SonHarfGold.copy(alpha = .58f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(48.dp).scale(pulse).background(SonHarfGold.copy(alpha = glow), CircleShape), contentAlignment = Alignment.Center) { Text("♛", color = SonHarfGold, fontSize = 30.sp, fontWeight = FontWeight.Black) }
                    Column { Text("PREMIUM", color = SonHarfGold, fontSize = 24.sp, fontWeight = FontWeight.Black); Text(sh("Reklamsız + özel Style", "Ad-free + exclusive Style"), color = SonHarfMuted, fontSize = 9.sp) }
                }
                Text(if (active) sh("AKTİF", "ACTIVE") else sh("KEŞFET ›", "EXPLORE ›"), color = if (active) SonHarfGreen else SonHarfGold, fontWeight = FontWeight.Black)
            }
            Text(sh("Özel oda • özel Style • gelişmiş istatistik • reklamsız deneyim • aylık 400 Son Coin", "Private rooms • exclusive Style • advanced stats • no ads • 400 Son Coin monthly"), color = SonHarfText, fontSize = 10.sp)
            Text(sh("Premium maç gücü, süre veya lig avantajı vermez.", "Premium never gives match power, time or league advantages."), color = SonHarfGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CosmeticPreview(item: ShopItemDto) {
    val transition = rememberInfiniteTransition(label = "preview_${item.id}")
    val pulse by transition.animateFloat(.94f, 1.04f, infiniteRepeatable(tween(1150), RepeatMode.Reverse), label = "previewPulse_${item.id}")
    val previewHeight = 108.dp
    Surface(modifier = Modifier.fillMaxWidth().height(previewHeight), color = SonHarfSurface2.copy(alpha = .72f), shape = RoundedCornerShape(18.dp)) {
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
            when (item.kind) {
                "profile_frame" -> {
                    val accent = if (item.id == "frame_gold") SonHarfGold else SonHarfCyan
                    Box(Modifier.size(76.dp).scale(pulse).background(accent.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = SonHarfSurface, border = BorderStroke(5.dp, accent)) { Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) { Text("A", color = SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black) } }
                    }
                }
                "name_style" -> Text("Oyuncu-10DD", color = SonHarfCyan, fontSize = 25.sp, fontWeight = FontWeight.Black)
                "game_theme" -> Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(SonHarfPurple.copy(alpha = .35f), SonHarfCyan.copy(alpha = .28f), SonHarfGold.copy(alpha = .20f))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text("AURORA ARENA", fontWeight = FontWeight.Black, color = SonHarfText) }
                "keyboard_theme" -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("S","O","N","H","A","R","F").forEach { k -> Surface(color = LetharaPalette.PanelStrong, shape = RoundedCornerShape(7.dp), border = BorderStroke(1.5.dp, LetharaPalette.Cyan)) { Text(k, Modifier.padding(horizontal = 8.dp, vertical = 10.dp), color = LetharaPalette.Cyan, fontWeight = FontWeight.Black) } } }
                "victory_effect" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Text("✦", color = SonHarfCyan, fontSize = 30.sp); Text("♛", color = SonHarfGold, fontSize = 50.sp, fontWeight = FontWeight.Black, modifier = Modifier.scale(pulse)); Text("✦", color = SonHarfPink, fontSize = 30.sp) }
                "emoji_pack" -> Text("👑  ⚡  😎  🔥  ◈", fontSize = 30.sp)
                else -> Text("◇", fontSize = 44.sp, color = SonHarfCyan)
            }
        }
    }
}
