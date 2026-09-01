package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    var previewItem by remember { mutableStateOf<ShopItemDto?>(null) }
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
                        sh("Her Style ürününün gerçek görünümü aşağıdaki önizlemede gösterilir. Sahip olduklarını Profil > Görünümümü düzenle bölümünden uygula.", "Each Style item shows its real visual preview below. Apply owned items from Profile > Edit appearance."),
                        color = MainUi.Text, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
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
                modifier = Modifier.fillMaxWidth(), color = MainUi.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    StyleItemVisual(
                        item = item, profile = profile,
                        modifier = Modifier.fillMaxWidth().height(154.dp).clickable { previewItem = item }, large = true,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                if (item.vipOnly) {
                                    Surface(color = MainUi.Gold.copy(alpha = .14f), shape = RoundedCornerShape(8.dp)) {
                                        Text("VIP", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = MainUi.Gold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Text(styleKindLabel(item.kind), color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(description, color = MainUi.Muted, fontSize = 9.5.sp, maxLines = 2)
                        }
                        TextButton(onClick = { previewItem = item }) { Text(sh("BÜYÜK ÖNİZLE", "LARGE PREVIEW"), fontSize = 8.sp, fontWeight = FontWeight.Black) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mine) sh("✓ SAHİPSİN", "✓ OWNED") else "◈ ${item.diamondPrice} SC", color = if (mine) MainUi.Green else MainUi.Gold, fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                            shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
    previewItem?.let { item ->
        val previewName = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = { Text(previewName, color = MainUi.Text, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StyleItemVisual(item, profile, Modifier.fillMaxWidth().height(270.dp), large = true)
                    Text(styleKindLabel(item.kind), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr, color = MainUi.Muted, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { previewItem = null }) { Text(sh("KAPAT", "CLOSE")) } },
            containerColor = MainUi.Surface,
        )
    }
}

private fun styleKindLabel(kind: String): String = when (kind) {
    "profile_frame" -> sh("PROFİL ÇERÇEVESİ", "PROFILE FRAME")
    "keyboard_theme" -> sh("KLAVYE", "KEYBOARD")
    "game_theme" -> sh("TEMA", "THEME")
    "victory_effect" -> sh("ZAFER EFEKTİ", "VICTORY EFFECT")
    "emoji_pack" -> "EMOJI"
    "mascot" -> "MASCOT"
    "name_style" -> sh("İSİM STİLİ", "NAME STYLE")
    else -> "STYLE"
}

@Composable
private fun StyleItemVisual(item: ShopItemDto, profile: ProfileDto?, modifier: Modifier = Modifier, large: Boolean = false) {
    val frameAccent = SonHarfCosmetics.frameAccent(item.id)
    val baseAccent = when (item.kind) {
        "profile_frame" -> frameAccent
        "name_style" -> when { "gold" in item.id -> MainUi.Gold; "black" in item.id -> MainUi.Text; "cyan" in item.id -> SonHarfCyan; else -> MainUi.Purple }
        "emoji_pack" -> MainUi.Gold
        "victory_effect" -> Color(0xFFF97316)
        "keyboard_theme" -> SonHarfCosmetics.keyboardPaletteFor(item.id).accent
        "game_theme" -> SonHarfCosmetics.gamePaletteFor(item.id).accent
        "mascot" -> Color(0xFF22A6A1)
        else -> MainUi.Blue
    }
    Surface(
        modifier = modifier.then(if (modifier == Modifier) Modifier.size(82.dp) else Modifier), shape = RoundedCornerShape(if (large) 24.dp else 20.dp), color = baseAccent.copy(alpha = .08f), border = BorderStroke(1.dp, baseAccent.copy(alpha = .22f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (item.kind) {
                "profile_frame" -> FrameItemPreview(item.id, item.vipOnly, profile, large)
                "name_style" -> {
                    val label = when { "black" in item.id -> "Ümit"; "gold" in item.id -> "★ Ümit"; "cyan" in item.id -> "Ümit ✦"; else -> "Ümit" }
                    Text(label, color = baseAccent, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                "emoji_pack" -> Text(if ("vip" in item.id) "👑✨" else "😎✨", fontSize = 24.sp)
                "victory_effect" -> {
                    val asset = premiumStyleAsset(item.id)
                    if (asset != null) Image(painterResource(asset), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Rounded.AutoAwesome, null, tint = baseAccent, modifier = Modifier.size(if (large) 62.dp else 40.dp))
                }
                "keyboard_theme" -> KeyboardItemPreview(item.id, large)
                "game_theme" -> GameThemeItemPreview(item.id, large)
                "mascot" -> {
                    val asset = premiumStyleAsset(item.id)
                    if (asset != null) Image(painterResource(asset), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else Icon(Icons.Rounded.Pets, null, tint = baseAccent, modifier = Modifier.size(if (large) 62.dp else 34.dp))
                }
                else -> Icon(Icons.Rounded.Diamond, null, tint = baseAccent, modifier = Modifier.size(36.dp))
            }
        }
    }
}

internal fun premiumStyleAsset(id: String): Int? = when {
    "frame_black_gold" in id -> R.drawable.premium_frame_black_gold_v3
    "frame_royal_gold" in id -> R.drawable.premium_frame_royal_gold_v3
    "frame_modern_neon" in id || "frame_neon" in id -> R.drawable.premium_frame_neon_v3
    "frame_crystal" in id -> R.drawable.premium_frame_crystal_higgsfield
    "frame_purple_prestige" in id -> R.drawable.premium_frame_purple_prestige_higgsfield
    "keyboard_midnight" in id -> R.drawable.premium_keyboard_midnight_higgsfield
    "keyboard_black_gold" in id -> R.drawable.premium_keyboard_black_gold_higgsfield
    "keyboard_crystal" in id -> R.drawable.premium_keyboard_crystal_higgsfield
    "theme_midnight" in id -> R.drawable.premium_theme_midnight_preview_higgsfield
    "victory_crown" in id -> R.drawable.premium_victory_crown_preview_higgsfield
    "mascot_white" in id -> R.drawable.premium_mascot_white_preview_higgsfield
    else -> null
}

@Composable
private fun FrameItemPreview(id: String, vipOnly: Boolean, profile: ProfileDto?, large: Boolean) {
    val previewWidth = if (large) 104.dp else 58.dp
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ProfilePhotoAvatarRectWithGender(
            avatarPath = profile?.avatarPath, gender = profile?.gender,
            name = profile?.displayName ?: sh("Oyuncu", "Player"),
            width = previewWidth, height = previewWidth * (74f / 56f),
            accent = SonHarfCosmetics.frameAccent(id), visible = true, frameIdOverride = id,
        )
        if (vipOnly) {
            Surface(Modifier.align(Alignment.BottomEnd).padding(6.dp), shape = RoundedCornerShape(99.dp), color = MainUi.Gold.copy(alpha = .16f)) {
                Text("VIP", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = MainUi.Gold, fontSize = 7.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun KeyboardItemPreview(id: String, large: Boolean = false) {
    val asset = premiumStyleAsset(id)
    if (asset != null) {
        Image(painterResource(asset), null, if (large) Modifier.fillMaxSize().padding(10.dp) else Modifier.size(68.dp, 54.dp), contentScale = ContentScale.Fit)
        return
    }
    val palette = SonHarfCosmetics.keyboardPaletteFor(id)
    Surface(Modifier.size(64.dp, 52.dp), shape = RoundedCornerShape(10.dp), color = palette.background, border = BorderStroke(1.dp, palette.accent.copy(alpha = .65f))) {
        Column(Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(4) { col ->
                        Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(3.dp), color = if (row == 2 && col == 3) palette.action else palette.key) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun GameThemeItemPreview(id: String, large: Boolean = false) {
    val asset = premiumStyleAsset(id)
    if (asset != null) {
        Image(painterResource(asset), null, if (large) Modifier.fillMaxSize().padding(10.dp) else Modifier.size(64.dp, 56.dp), contentScale = ContentScale.Fit)
        return
    }
    val palette = SonHarfCosmetics.gamePaletteFor(id)
    Surface(Modifier.size(64.dp, 52.dp), shape = RoundedCornerShape(10.dp), color = palette.background, border = BorderStroke(1.dp, palette.accent.copy(alpha = .62f))) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(Modifier.fillMaxWidth().height(11.dp), shape = RoundedCornerShape(4.dp), color = palette.surface) {}
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(4.dp), color = palette.accent.copy(alpha = .85f)) {}
                Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(4.dp), color = palette.secondary.copy(alpha = .85f)) {}
            }
        }
    }
}
