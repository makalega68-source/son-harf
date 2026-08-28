package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch
import java.util.Locale

private val AdminBg = Color(0xFF061321)
private val AdminPanel = Color(0xFF0D2237)
private val AdminPanel2 = Color(0xFF132D47)
private val AdminGold = Color(0xFFD9AD5E)
private val AdminText = Color(0xFFF7F3E9)
private val AdminMuted = Color(0xFFABB9C5)
private val AdminGreen = Color(0xFF66B58A)
private val AdminRed = Color(0xFFE07B78)
private val AdminBlue = Color(0xFF70B6D9)

private data class RepairAction(val key: String, val title: String, val detail: String)

@Composable
fun AdminConsoleScreen(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<AdminDashboardDto?>(null) }
    var products by remember { mutableStateOf<List<AdminTopProductDto>>(emptyList()) }
    var storeItems by remember { mutableStateOf<List<AdminTopStoreItemDto>>(emptyList()) }
    var health by remember { mutableStateOf<List<AdminHealthDto>>(emptyList()) }
    var monthlyRevenue by remember { mutableStateOf<List<AdminMonthlyRevenueDto>>(emptyList()) }
    var announcement by remember { mutableStateOf(AdminAnnouncementDto()) }
    var announcementText by remember { mutableStateOf("") }
    var announcementEnabled by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var repairConfirm by remember { mutableStateOf<RepairAction?>(null) }
    var priceProduct by remember { mutableStateOf<String?>(null) }
    var priceText by remember { mutableStateOf("") }

    suspend fun reload() {
        loading = true
        error = null
        runCatching {
            dashboard = backend.getAdminDashboard()
            products = backend.getAdminTopProducts()
            storeItems = backend.getAdminTopStoreItems()
            health = backend.getAdminHealth()
            monthlyRevenue = backend.getAdminMonthlyRevenue()
            announcement = backend.getAdminAnnouncement()
            announcementText = announcement.message
            announcementEnabled = announcement.enabled
        }.onFailure {
            dashboard = null
            error = "Bu panel yalnızca yetkili yönetici hesabında açılabilir."
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AdminBg, Color(0xFF081B2B))))) {
        if (loading && dashboard == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = AdminGold)
        } else if (dashboard == null) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.AdminPanelSettings, null, tint = AdminRed, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(14.dp))
                Text(error ?: "Yönetici erişimi yok.", color = AdminText, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onBack) { Text("GERİ DÖN") }
            }
            return@Box
        }

        val d = dashboard ?: return@Box
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = AdminText) }
                    Column(Modifier.weight(1f)) {
                        Text("YÖNETİCİ PANELİ", color = AdminGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("Canlı operasyon • yalnızca yönetici hesabı", color = AdminMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { scope.launch { reload() } }, enabled = !busy) {
                        Icon(Icons.Rounded.Refresh, null, tint = AdminBlue)
                    }
                }
            }

            notice?.let { message ->
                item {
                    Surface(color = AdminGreen.copy(alpha = .12f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AdminGreen.copy(alpha=.45f))) {
                        Text(message, Modifier.fillMaxWidth().padding(12.dp), color = AdminGreen, fontSize = 13.sp)
                    }
                }
            }

            item { AdminSectionTitle("GENEL DURUM", Icons.Rounded.QueryStats) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminStatCard("Kayıtlı Oyuncu", d.totalUsers.toString(), "Toplam profil", Modifier.weight(1f))
                    AdminStatCard("Şu An Aktif", d.activeNow.toString(), "Son 5 dk", Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminStatCard("Bugün Aktif", d.activeToday.toString(), "24 saat", Modifier.weight(1f))
                    AdminStatCard("7 Gün Aktif", d.active7d.toString(), "Son 7 gün", Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminStatCard("Gerçek VIP", d.vipUsers.toString(), "Test hesabı hariç", Modifier.weight(1f))
                    AdminStatCard("Canlı Maç", d.activeRooms.toString(), "Aktif odalar", Modifier.weight(1f))
                }
            }

            item { AdminSectionTitle("GELİR & SATIN ALMA", Icons.Rounded.Payments) }
            item {
                AdminWideCard {
                    Text("Kayıtlı Brüt Gelir", color = AdminMuted, fontSize = 12.sp)
                    Text(formatMoney(d.grossRevenueMinor, d.revenueCurrency), color = AdminGold, fontSize = 31.sp, fontWeight = FontWeight.Black)
                    Text("${d.verifiedPurchases} doğrulanmış satın alma", color = AdminText, fontSize = 13.sp)
                    if (d.unpricedPurchases > 0) {
                        Text("${d.unpricedPurchases} satın alımda ürün fiyatı tanımlı değil; gelir hesabına dahil edilmedi.", color = AdminRed, fontSize = 11.sp)
                    }
                    Text("Gösterilen tutar kayıtlı brüt satış değeridir; mağaza komisyonu, vergi ve iadeler düşülmemiştir.", color = AdminMuted, fontSize = 10.sp)
                }
            }
            if (products.isEmpty()) {
                item { AdminEmpty("Henüz doğrulanmış gerçek satın alma yok.") }
            } else {
                items(products, key = { it.productId }) { p ->
                    AdminProductRow(p) { priceProduct = p.productId; priceText = "" }
                }
            }
            item {
                OutlinedButton(
                    onClick = { priceProduct = "vip_monthly"; priceText = "" },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AdminGold.copy(alpha=.55f)),
                ) { Text("ÜRÜN FİYATI TANIMLA / GÜNCELLE", color = AdminGold) }
            }

            item { AdminSectionTitle("AYLIK GELİR", Icons.Rounded.CalendarMonth) }
            item {
                AdminWideCard {
                    val current = monthlyRevenue.firstOrNull()
                    Text("Bu ay kayıtlı brüt gelir", color = AdminMuted, fontSize = 12.sp)
                    Text(formatMoney(current?.revenueMinor ?: 0, current?.currency ?: "TRY"), color = AdminGold, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    monthlyRevenue.take(6).forEach { m -> AdminSimpleRow(m.month, formatMoney(m.revenueMinor, m.currency)) }
                }
            }

            item { AdminSectionTitle("DUYURU PANOSU", Icons.Rounded.Campaign) }
            item {
                AdminWideCard {
                    OutlinedTextField(announcementText, { announcementText = it.take(500) }, modifier = Modifier.fillMaxWidth(), label = { Text("Duyuru metni") }, minLines = 2, maxLines = 5)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Duyuruyu yayınla", color = AdminText, fontWeight = FontWeight.Bold)
                        Switch(checked = announcementEnabled, onCheckedChange = { announcementEnabled = it })
                    }
                    Button(onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backend.adminSetAnnouncement(announcementText.trim(), announcementEnabled) }
                                .onSuccess { notice = "Duyuru panosu güncellendi." }
                                .onFailure { error = it.message ?: "Duyuru güncellenemedi." }
                            reload(); busy = false
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("DUYURUYU KAYDET") }
                }
            }

            item { AdminSectionTitle("OYUN TERCİHİ", Icons.Rounded.SportsEsports) }
            item {
                AdminWideCard {
                    val total = d.sonHarfOpens + d.bilBakalimOpens
                    val sonPct = if (total == 0L) 0 else (d.sonHarfOpens * 100 / total).toInt()
                    val bilPct = if (total == 0L) 0 else (d.bilBakalimOpens * 100 / total).toInt()
                    AdminUsageLine("Son Harf", d.sonHarfOpens, sonPct)
                    AdminUsageLine("Bil Bakalım", d.bilBakalimOpens, bilPct)
                    Text("Yönetici testleri bu karşılaştırmaya dahil edilmez.", color = AdminMuted, fontSize = 10.sp)
                }
            }

            item { AdminSectionTitle("MAÇ İSTATİSTİKLERİ", Icons.Rounded.Leaderboard) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminStatCard("Toplam Maç", d.matchesTotal.toString(), "Tüm zamanlar", Modifier.weight(1f))
                    AdminStatCard("Bugünkü Maç", d.matchesToday.toString(), "Son 24 saat", Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminStatCard("Kuyrukta", d.queueWaiting.toString(), "Eşleşme bekliyor", Modifier.weight(1f))
                    AdminStatCard("Şüpheli Maç", d.staleRooms.toString(), "5 dk hareketsiz", Modifier.weight(1f))
                }
            }

            item { AdminSectionTitle("EN ÇOK ALINAN MAĞAZA ÜRÜNLERİ", Icons.Rounded.ShoppingBag) }
            if (storeItems.isEmpty()) item { AdminEmpty("Henüz mağaza edinimi yok.") }
            else items(storeItems, key = { it.itemId }) { item ->
                AdminSimpleRow(item.itemName, "${item.acquisitionCount} edinim")
            }

            item { AdminSectionTitle("SİSTEM SAĞLIĞI", Icons.Rounded.HealthAndSafety) }
            items(health, key = { it.metricKey }) { h -> AdminHealthRow(h) }

            item { AdminSectionTitle("ONARIM ARAÇLARI", Icons.Rounded.BuildCircle) }
            item {
                AdminRepairGrid { action -> repairConfirm = action }
            }

            item { AdminSectionTitle("BENİM TEST ARAÇLARIM", Icons.Rounded.Science) }
            item {
                AdminWideCard {
                    AdminToggleRow(
                        title = "VIP test durumum",
                        detail = "Gerçek abonelik oluşturmadan yalnızca bu yönetici hesabının VIP özelliğini açar/kapatır.",
                        checked = d.myIsVip,
                        enabled = !busy,
                    ) { enabled ->
                        scope.launch {
                            busy = true
                            runCatching { backend.adminSetMyVip(enabled) }
                                .onSuccess { notice = if (enabled) "Yönetici hesabı VIP test moduna alındı." else "Yönetici hesabının VIP test modu kapatıldı." }
                                .onFailure { error = it.message }
                            reload(); busy = false
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha=.08f))
                    AdminToggleRow(
                        title = "Satın alımlarım ücretsiz",
                        detail = "Elmas mağazasındaki ürünleri bakiyeden düşmeden test eder. Gerçek oyuncuların fiyatlarını etkilemez.",
                        checked = d.freeTestPurchases,
                        enabled = !busy,
                    ) { enabled ->
                        scope.launch {
                            busy = true
                            runCatching { backend.adminSetFreePurchases(enabled) }
                                .onSuccess { notice = if (enabled) "Ücretsiz test satın alımları açıldı." else "Ücretsiz test satın alımları kapatıldı." }
                            reload(); busy = false
                        }
                    }
                }
            }

            item { AdminSectionTitle("ÜCRETSİZ TEST PAKETLERİ", Icons.Rounded.CardGiftcard) }
            item {
                AdminWideCard {
                    Text("Bu paketler yalnızca yönetici hesabına test verisi verir; Google Play satın alımı ve gerçek gelir kaydı oluşturmaz.", color = AdminMuted, fontSize = 10.sp)
                    listOf(
                        "vip_monthly" to "VIP Aylık Test",
                        "vip_yearly" to "VIP Yıllık Test",
                        "coins_500" to "+500 Son Coin Test",
                        "coins_1500" to "+1500 Son Coin Test",
                        "coins_3500" to "+3500 Son Coin Test",
                        "coins_8000" to "+8000 Son Coin Test",
                        "season_pass_monthly" to "Sezon Bileti 30 Gün Test",
                        "starter_style_pack" to "Başlangıç Style Paketi Test",
                        "theme_neon" to "Neon Tema Test",
                    ).forEach { (productId, label) ->
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { backend.adminGrantTestProduct(productId) }
                                        .onSuccess { notice = "$label ücretsiz olarak uygulandı." }
                                        .onFailure { error = it.message ?: "Test ürünü uygulanamadı." }
                                    reload(); busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, AdminGold.copy(alpha=.4f)),
                        ) { Text(label, color = AdminText) }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    repairConfirm?.let { action ->
        AlertDialog(
            onDismissRequest = { if (!busy) repairConfirm = null },
            icon = { Icon(Icons.Rounded.BuildCircle, null, tint = AdminGold) },
            title = { Text(action.title) },
            text = { Text(action.detail) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        busy = true
                        runCatching { backend.adminRepair(action.key) }
                            .onSuccess { notice = "${action.title} tamamlandı." }
                            .onFailure { error = it.message ?: "Onarım başarısız." }
                        repairConfirm = null
                        reload(); busy = false
                    }
                }, enabled = !busy) { Text("ÇALIŞTIR") }
            },
            dismissButton = { TextButton(onClick = { repairConfirm = null }, enabled = !busy) { Text("VAZGEÇ") } },
        )
    }

    priceProduct?.let { initialProduct ->
        var productId by remember(initialProduct) { mutableStateOf(initialProduct) }
        AlertDialog(
            onDismissRequest = { if (!busy) priceProduct = null },
            title = { Text("Brüt ürün fiyatı") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = productId,
                        onValueChange = { productId = it.trim() },
                        label = { Text("Google Play ürün kimliği") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { value -> priceText = value.filter { it.isDigit() || it == ',' || it == '.' }.take(12) },
                        label = { Text("Brüt fiyat (TRY)") },
                        placeholder = { Text("Örn. 49,99") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    Text("Bu değer yalnızca yönetici gelir analizinde kullanılır; Play Store fiyatını değiştirmez.", fontSize = 11.sp)
                    Text("Bilinen ürünler: vip_monthly, vip_yearly, season_pass_monthly, coins_500, coins_1500, coins_3500, coins_8000, starter_style_pack, theme_neon", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val minor = parseMoneyMinor(priceText)
                    if (productId.isBlank() || minor == null) {
                        notice = "Geçerli ürün kimliği ve fiyat gir."
                        return@Button
                    }
                    scope.launch {
                        busy = true
                        runCatching { backend.adminSetProductPrice(productId, minor, "TRY") }
                            .onSuccess { notice = "$productId brüt fiyatı kaydedildi."; priceProduct = null }
                            .onFailure { error = it.message }
                        reload(); busy = false
                    }
                }, enabled = !busy) { Text("KAYDET") }
            },
            dismissButton = { TextButton(onClick = { priceProduct = null }, enabled = !busy) { Text("İPTAL") } },
        )
    }
}

@Composable
private fun AdminSectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AdminGold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = AdminGold, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
private fun AdminStatCard(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = AdminPanel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha=.08f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = AdminMuted, fontSize = 10.sp)
            Text(value, color = AdminText, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(detail, color = AdminMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun AdminWideCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AdminPanel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha=.08f))) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
private fun AdminProductRow(p: AdminTopProductDto, onSetPrice: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AdminPanel), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.productName, color = AdminText, fontWeight = FontWeight.Bold)
                Text("${p.purchaseCount} satın alma • ${if (p.priceConfigured) formatMoney(p.revenueMinor,p.currency) else "fiyat tanımsız"}", color = if (p.priceConfigured) AdminMuted else AdminRed, fontSize = 11.sp)
            }
            TextButton(onClick = onSetPrice) { Text("FİYAT", color = AdminGold) }
        }
    }
}

@Composable
private fun AdminSimpleRow(title: String, value: String) {
    Surface(color = AdminPanel, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = AdminText, fontWeight = FontWeight.SemiBold)
            Text(value, color = AdminBlue)
        }
    }
}

@Composable
private fun AdminHealthRow(h: AdminHealthDto) {
    val ok = h.status == "ok"
    Surface(color = AdminPanel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, (if (ok) AdminGreen else AdminRed).copy(alpha=.32f))) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, null, tint = if (ok) AdminGreen else AdminRed)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(h.title, color = AdminText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(h.detail, color = AdminMuted, fontSize = 10.sp)
            }
            Text(h.metricValue.toString(), color = if (ok) AdminGreen else AdminRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AdminUsageLine(name: String, count: Long, pct: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, color = AdminText, fontWeight = FontWeight.Bold)
        Text("$count açılış • %$pct", color = AdminBlue)
    }
    LinearProgressIndicator(progress = { (pct / 100f).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(7.dp), color = AdminGold, trackColor = Color.White.copy(alpha=.07f))
}

@Composable
private fun AdminToggleRow(title: String, detail: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AdminText, fontWeight = FontWeight.Bold)
            Text(detail, color = AdminMuted, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun AdminRepairGrid(onAction: (RepairAction) -> Unit) {
    val actions = listOf(
        RepairAction("maintenance", "Standart Bakım", "Mevcut güvenli oyun bakım prosedürünü çalıştırır."),
        RepairAction("stale_rooms", "Takılı Maçları Temizle", "5 dakikadan uzun süredir hareketsiz kalan aktif maçları güvenli şekilde iptal eder."),
        RepairAction("stale_queue", "Eşleşme Kuyruğunu Temizle", "Süresi geçmiş bekleyen eşleşme kayıtlarını kapatır."),
        RepairAction("stuck_quizzes", "Takılı Quizleri Kurtar", "Bitmiş Bil Bakalım sonuçlarını oyuna döndürür; çok eski çözümsüz bonus turlarını kapatır."),
        RepairAction("presence", "Oyuncu Durumlarını Düzelt", "Aktif maçı olmadığı halde oyunda görünen hesapların presence durumunu düzeltir."),
        RepairAction("all", "Tüm Güvenli Onarımlar", "Bakım, eski maç, kuyruk, quiz ve presence kontrollerini birlikte çalıştırır."),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { action ->
                    OutlinedButton(onClick = { onAction(action) }, modifier = Modifier.weight(1f).heightIn(min=58.dp), border = BorderStroke(1.dp, AdminBlue.copy(alpha=.45f)), contentPadding = PaddingValues(8.dp)) {
                        Text(action.title, color = AdminText, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminEmpty(text: String) {
    Surface(color = AdminPanel2.copy(alpha=.65f), shape = RoundedCornerShape(12.dp)) {
        Text(text, Modifier.fillMaxWidth().padding(13.dp), color = AdminMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

private fun formatMoney(minor: Long, currency: String): String {
    val amount = minor / 100.0
    return when (currency.uppercase()) {
        "TRY" -> String.format(Locale("tr","TR"), "₺%,.2f", amount)
        "USD" -> String.format(Locale.US, "\$%,.2f", amount)
        "EUR" -> String.format(Locale.GERMANY, "€%,.2f", amount)
        else -> String.format(Locale.US, "%,.2f %s", amount, currency.uppercase())
    }
}

private fun parseMoneyMinor(raw: String): Long? {
    val cleaned = raw.trim()
    val normalized = if (cleaned.contains(',')) cleaned.replace(".", "").replace(',', '.') else cleaned
    return normalized.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
}
