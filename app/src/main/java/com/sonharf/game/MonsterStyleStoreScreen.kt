package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ShoppingBag
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
internal fun MonsterStyleStoreScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var themes by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equippedId by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            notice = sh("Tema mağazası için sunucu bağlantısı gerekiyor.", "The theme store requires a server connection.")
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
            themes = b.getShopItems()
                .filter { it.kind == "game_theme" && SonHarfThemeCatalog.known(it.id) }
                .sortedBy { it.sortOrder }
            owned = b.getInventory()
            val equipped = b.getEquippedCosmetics()
            equippedId = equipped?.gameThemeId
            SonHarfCosmetics.apply(equipped)
        }.onFailure {
            notice = sh("Tema mağazası yüklenemedi.", "Theme store could not be loaded.")
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val shell = SonHarfCosmetics.currentThemePalette
    Surface(Modifier.fillMaxSize(), color = shell.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("STYLE", color = shell.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(sh("Premium tema koleksiyonu", "Premium theme collection"), color = shell.muted, fontSize = 10.sp)
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = shell.surfaceRaised, border = BorderStroke(1.dp, shell.border)) {
                        Text("◈ ${profile?.diamonds ?: 0} SC", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = shell.accent, fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Surface(color = shell.surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, shell.border)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sh("MONSTER PREMIUM TEMALAR", "MONSTER PREMIUM THEMES"), color = shell.text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(
                            sh("Yerleşim aynı kalır; yalnız renk paleti değişir. Tüm paletler yüksek kontrast için tasarlanmıştır.", "Layout stays identical; only the color palette changes. Every palette is designed for high contrast."),
                            color = shell.muted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }

            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = shell.accent, trackColor = shell.surfaceSoft) }

            items(themes.size) { index ->
                val item = themes[index]
                val palette = SonHarfThemeCatalog.forId(item.id)
                val mine = item.id in owned
                val active = equippedId == item.id
                PremiumThemeStoreCard(
                    item = item,
                    palette = palette,
                    owned = mine,
                    active = active,
                    busy = busyId == item.id,
                    disabledByOtherOperation = busyId != null && busyId != item.id,
                    onAction = {
                        val b = backend ?: return@PremiumThemeStoreCard
                        scope.launch {
                            busyId = item.id
                            runCatching {
                                if (!mine) b.purchaseShopItem(item.id)
                                b.equipShopItem(item.id)
                            }.onSuccess {
                                notice = if (mine) {
                                    sh("${palette.nameTr} etkinleştirildi.", "${palette.nameEn} equipped.")
                                } else {
                                    sh("Satın alma tamamlandı ve tema etkinleştirildi.", "Purchase complete and theme equipped.")
                                }
                                reload()
                            }.onFailure { error ->
                                val raw = error.message.orEmpty()
                                notice = when {
                                    "insufficient_diamonds" in raw -> sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                    "vip_required" in raw -> sh("Bu tema VIP üyelerine özel.", "This theme is VIP only.")
                                    else -> sh("Tema işlemi tamamlanamadı.", "Theme action could not be completed.")
                                }
                            }
                            busyId = null
                        }
                    },
                )
            }

            if (!loading && themes.isEmpty()) {
                item {
                    Surface(color = shell.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, shell.border)) {
                        Text(sh("Tema ürünleri henüz sunucuda yayınlanmamış.", "Theme products are not published on the server yet."), Modifier.fillMaxWidth().padding(14.dp), color = shell.muted, textAlign = TextAlign.Center, fontSize = 10.sp)
                    }
                }
            }

            item {
                Surface(color = shell.surfaceRaised, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, shell.border)) {
                    Text(
                        sh("Temalar yalnız görünümü değiştirir. Puan, süre, rating, joker gücü veya maç avantajı vermez.", "Themes only change appearance. They give no score, timer, rating, joker power or match advantage."),
                        Modifier.fillMaxWidth().padding(12.dp),
                        color = shell.muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            notice?.let { message ->
                item {
                    Surface(color = shell.surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, shell.border)) {
                        Text(message, Modifier.fillMaxWidth().padding(11.dp), color = shell.text, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PremiumThemeStoreCard(
    item: ShopItemDto,
    palette: SonHarfThemePalette,
    owned: Boolean,
    active: Boolean,
    busy: Boolean,
    disabledByOtherOperation: Boolean,
    onAction: () -> Unit,
) {
    val shell = SonHarfCosmetics.currentThemePalette
    Card(
        colors = CardDefaults.cardColors(containerColor = shell.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) shell.accent else shell.border),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            MonsterThemePreview(palette)
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr, color = shell.text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr, color = shell.muted, fontSize = 10.sp)
                }
                if (active) Icon(Icons.Rounded.CheckCircle, null, tint = shell.green, modifier = Modifier.size(26.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        when {
                            active -> sh("AKTİF", "EQUIPPED")
                            owned -> sh("✓ SAHİPSİN", "✓ OWNED")
                            else -> "◈ ${item.diamondPrice} SC"
                        },
                        color = when { active -> shell.green; owned -> shell.green; else -> shell.accent },
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                    if (!owned && !active) Text(sh("Kalıcı tema", "Permanent theme"), color = shell.muted, fontSize = 8.sp)
                }
                Button(
                    enabled = !busy && !disabledByOtherOperation && !active,
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = shell.accent,
                        contentColor = shell.accentText,
                        disabledContainerColor = shell.surfaceSoft,
                        disabledContentColor = shell.muted,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            busy -> "…"
                            active -> sh("AKTİF", "EQUIPPED")
                            owned -> sh("KULLAN", "EQUIP")
                            else -> sh("SATIN AL", "BUY")
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MonsterThemePreview(palette: SonHarfThemePalette, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(150.dp),
        color = palette.background,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = palette.accent) {}
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(.55f).height(8.dp).background(palette.text, RoundedCornerShape(99.dp)))
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth(.35f).height(6.dp).background(palette.muted, RoundedCornerShape(99.dp)))
                }
                Surface(shape = CircleShape, color = palette.surfaceRaised, border = BorderStroke(1.dp, palette.border)) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Palette, null, tint = palette.accent, modifier = Modifier.size(14.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.weight(1f).height(56.dp), color = palette.surface, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, palette.border)) {}
                Surface(Modifier.weight(1f).height(56.dp), color = palette.surfaceRaised, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, palette.border)) {}
            }
            Box(Modifier.fillMaxWidth().height(24.dp).background(palette.accent, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Box(Modifier.width(64.dp).height(5.dp).background(palette.accentText.copy(alpha = .88f), RoundedCornerShape(99.dp)))
            }
        }
    }
}
