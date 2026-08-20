package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun EconomyShopScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        val b = backend ?: return
        val id = b.currentUserId() ?: return
        profile = runCatching { b.getProfile(id) }.getOrNull()
        items = runCatching { b.getShopItems() }.getOrDefault(emptyList())
        owned = runCatching { b.getInventory() }.getOrDefault(emptySet())
        equipped = runCatching { b.getEquippedCosmetics() }.getOrNull()
    }

    LaunchedEffect(Unit) {
        loading = true
        runCatching {
            if (backend?.currentUserId() == null) backend?.ensurePlayer(sh("Oyuncu", "Player"))
            reload()
        }.onFailure { notice = sh("Mağaza verisi alınamadı.", "Store data could not be loaded.") }
        loading = false
    }

    fun isEquipped(item: ShopItemDto): Boolean = when (item.kind) {
        "profile_frame" -> equipped?.profileFrameId == item.id
        "name_style" -> equipped?.nameStyleId == item.id
        "game_theme" -> equipped?.gameThemeId == item.id
        "keyboard_theme" -> equipped?.keyboardThemeId == item.id
        "victory_effect" -> equipped?.victoryEffectId == item.id
        "emoji_pack" -> equipped?.emojiPackId == item.id
        else -> false
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(sh("MAĞAZA", "SHOP"), fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kozmetik ve üyelik avantajları", "Cosmetics and membership benefits"), color = SonHarfMuted, fontSize = 11.sp)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = SonHarfCyan.copy(alpha = .12f), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f))) {
                    Text("💎 ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = SonHarfCyan, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfGold.copy(alpha = .10f)),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .45f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("♛  VIP", color = SonHarfGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(if (profile?.isVip == true) sh("AKTİF", "ACTIVE") else sh("PASİF", "INACTIVE"), color = if (profile?.isVip == true) SonHarfGreen else SonHarfMuted, fontWeight = FontWeight.Bold)
                    }
                    Text(sh("VIP: özel oda oluşturma, özel kozmetikler, gelişmiş istatistik erişimi, reklamsız deneyim ve her ay 400 elmas.", "VIP: private-room creation, exclusive cosmetics, advanced statistics access, ad-free experience and 400 diamonds every month."), color = SonHarfText, fontSize = 11.sp)
                    if (profile?.isVip == true) {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = "vip_claim"
                                    runCatching { backend?.claimVipMonthlyDiamonds() }
                                        .onSuccess { notice = sh("400 VIP elması hesabına eklendi.", "400 VIP diamonds were added to your account."); reload() }
                                        .onFailure { notice = if ("already_claimed" in it.message.orEmpty()) sh("Bu ayın VIP elmaslarını zaten aldın.", "You already claimed this month's VIP diamonds.") else sh("VIP elması alınamadı.", "VIP diamonds could not be claimed.") }
                                    busy = null
                                }
                            },
                            enabled = busy == null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF191000)),
                        ) { Text(if (busy == "vip_claim") "…" else sh("AYLIK 400 ELMASI AL", "CLAIM 400 MONTHLY DIAMONDS"), fontWeight = FontWeight.Black) }
                    } else {
                        Text(sh("VIP satın alma Google Play doğrulaması bağlandığında açılacak. Üyelik durumu sunucu tarafından doğrulanır; uygulama içinden sahte VIP açılamaz.", "VIP purchase will open when Google Play verification is connected. Membership status is server-verified and cannot be faked by the app."), color = SonHarfMuted, fontSize = 9.sp)
                    }
                }
            }
        }

        item {
            Text(sh("ELMASLA KOZMETİK", "COSMETICS WITH DIAMONDS"), color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(sh("Elmaslar puan, süre veya maç avantajı vermez. Yalnızca görünüm ve kişiselleştirme içindir.", "Diamonds never grant score, time or match advantages. They are only for appearance and customization."), color = SonHarfMuted, fontSize = 9.sp)
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

        items(items, key = { it.id }) { item ->
            val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
            val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
            val mine = item.id in owned
            val active = isEquipped(item)
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (active) SonHarfCyan.copy(alpha=.55f) else SonHarfMuted.copy(alpha=.12f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Black)
                            Text(description, color = SonHarfMuted, fontSize = 9.sp)
                        }
                        if (item.vipOnly) Text("VIP", color = SonHarfGold, fontWeight = FontWeight.Black)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mine) sh("SAHİPSİN", "OWNED") else "💎 ${item.diamondPrice}", color = if (mine) SonHarfGreen else SonHarfCyan, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = item.id
                                    if (mine) {
                                        runCatching { backend?.equipShopItem(item.id) }
                                            .onSuccess { notice = sh("Kozmetik etkinleştirildi.", "Cosmetic equipped."); reload() }
                                            .onFailure { notice = sh("Kozmetik etkinleştirilemedi.", "Cosmetic could not be equipped.") }
                                    } else {
                                        runCatching { backend?.purchaseShopItem(item.id) }
                                            .onSuccess { notice = sh("Satın alma tamamlandı.", "Purchase completed."); reload() }
                                            .onFailure {
                                                val raw = it.message.orEmpty()
                                                notice = when {
                                                    "insufficient_diamonds" in raw -> sh("Yeterli elmasın yok.", "You do not have enough diamonds.")
                                                    "vip_required" in raw -> sh("Bu ürün VIP üyelerine özel.", "This item is exclusive to VIP members.")
                                                    "already_owned" in raw -> sh("Bu ürüne zaten sahipsin.", "You already own this item.")
                                                    else -> sh("Satın alma tamamlanamadı.", "Purchase could not be completed.")
                                                }
                                            }
                                    }
                                    busy = null
                                }
                            },
                            enabled = busy == null && (!item.vipOnly || profile?.isVip == true),
                        ) {
                            Text(when { busy == item.id -> "…"; active -> sh("AKTİF", "EQUIPPED"); mine -> sh("KULLAN", "EQUIP"); else -> sh("SATIN AL", "BUY") })
                        }
                    }
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = SonHarfSurface2, shape = RoundedCornerShape(14.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
