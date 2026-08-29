package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalUriHandler
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

private enum class AdminSection(val title: String) {
    OVERVIEW("Ana Sayfa"),
    PLAYERS("Oyuncular & VIP"),
    GAMES("Oyunlar"),
    ANNOUNCEMENTS("Duyurular"),
    MAINTENANCE("Bakım"),
}

@Composable
fun AdminConsoleScreen(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<AdminDashboardDto?>(null) }
    var products by remember { mutableStateOf<List<AdminTopProductDto>>(emptyList()) }
    var storeItems by remember { mutableStateOf<List<AdminTopStoreItemDto>>(emptyList()) }
    var health by remember { mutableStateOf<List<AdminHealthDto>>(emptyList()) }
    var ownerAccounts by remember { mutableStateOf<List<AdminOwnerAccountDto>>(emptyList()) }
    var capacity by remember { mutableStateOf<List<AdminCapacityDto>>(emptyList()) }
    var gameControls by remember { mutableStateOf<List<AdminGameControlDto>>(emptyList()) }
    var selectedSection by remember { mutableStateOf(AdminSection.OVERVIEW) }
    var ownerEmailInput by remember { mutableStateOf("") }
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
    val uriHandler = LocalUriHandler.current

    suspend fun reload() {
        loading = true
        error = null
        runCatching {
            dashboard = backend.getAdminDashboard()
            products = backend.getAdminTopProducts()
            storeItems = backend.getAdminTopStoreItems()
            health = backend.getAdminHealth()
            ownerAccounts = backend.getAdminOwnerAccounts()
            capacity = backend.getAdminCapacity()
            gameControls = backend.getAdminGameControls()
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
                        Text("SON HARF YÖNETİM", color = AdminGold, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text("Sade operasyon merkezi", color = AdminMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { scope.launch { reload() } }, enabled = !busy) {
                        Icon(Icons.Rounded.Refresh, null, tint = AdminBlue)
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AdminSection.entries, key = { it.name }) { section ->
                        FilterChip(
                            selected = selectedSection == section,
                            onClick = { selectedSection = section; notice = null; error = null },
                            label = { Text(section.title, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }

            notice?.let { message ->
                item {
                    Surface(
                        color = AdminGreen.copy(alpha = .12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AdminGreen.copy(alpha=.45f)),
                    ) {
                        Text(message, Modifier.fillMaxWidth().padding(12.dp), color = AdminGreen, fontSize = 13.sp)
                    }
                }
            }
            error?.let { message ->
                item {
                    Surface(
                        color = AdminRed.copy(alpha = .12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AdminRed.copy(alpha=.45f)),
                    ) {
                        Text(message, Modifier.fillMaxWidth().padding(12.dp), color = AdminRed, fontSize = 13.sp)
                    }
                }
            }

            when (selectedSection) {
                AdminSection.OVERVIEW -> {
                    item { AdminSectionTitle("GENEL DURUM", Icons.Rounded.Home) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminStatCard("Kayıtlı Oyuncu", d.totalUsers.toString(), "Toplam profil", Modifier.weight(1f))
                            AdminStatCard("Şu An Aktif", d.activeNow.toString(), "Son 5 dk", Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminStatCard("Canlı Maç", d.activeRooms.toString(), "Aktif odalar", Modifier.weight(1f))
                            AdminStatCard("VIP Oyuncu", d.vipUsers.toString(), "Aktif VIP", Modifier.weight(1f))
                        }
                    }
                    item {
                        val warningCount = health.count { it.status != "ok" } +
                            capacity.count { it.status == "warning" || it.status == "critical" }
                        AdminWideCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (warningCount == 0) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                    null,
                                    tint = if (warningCount == 0) AdminGreen else AdminGold,
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (warningCount == 0) "Sistem sağlıklı" else "$warningCount konu kontrol edilmeli",
                                        color = AdminText,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                    )
                                    Text(
                                        if (warningCount == 0) "Kritik bir sorun görünmüyor." else "Bakım bölümünde ayrıntıyı ve çözüm bağlantısını görebilirsin.",
                                        color = AdminMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                    capacity.filter { it.metricKey == "supabase_database" || it.metricKey == "supabase_storage" }
                        .forEach { metric ->
                            item { AdminCapacityCompact(metric) }
                        }
                }

                AdminSection.PLAYERS -> {
                    item { AdminSectionTitle("OYUNCULAR & VIP", Icons.Rounded.People) }
                    item {
                        AdminWideCard {
                            Text("Özel hesaplar", color = AdminText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text(
                                "Bu hesaplar gerçek maç oynar. Rating veya lig puanı panelden değiştirilmez. Sadece VIP ve sınırsız oyun içi harcama hakları yönetilir.",
                                color = AdminMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    if (ownerAccounts.isEmpty()) {
                        item { AdminEmpty("Henüz özel hesap tanımlı değil.") }
                    } else {
                        items(ownerAccounts, key = { it.userId }) { account ->
                            AdminOwnerAccountCard(
                                account = account,
                                enabled = !busy,
                                onChange = { lifetimeVip, unlimitedDiamonds, unlimitedSonCoin, active ->
                                    scope.launch {
                                        busy = true
                                        runCatching {
                                            backend.adminSetOwnerAccount(
                                                email = account.email,
                                                lifetimeVip = lifetimeVip,
                                                unlimitedDiamonds = unlimitedDiamonds,
                                                unlimitedSonCoin = unlimitedSonCoin,
                                                active = active,
                                            )
                                        }.onSuccess {
                                            notice = "${account.displayName} özel hesap ayarları güncellendi."
                                        }.onFailure {
                                            error = it.message ?: "Özel hesap güncellenemedi."
                                        }
                                        reload()
                                        busy = false
                                    }
                                },
                            )
                        }
                    }
                    item {
                        AdminWideCard {
                            Text("Yeni özel hesap", color = AdminText, fontWeight = FontWeight.Bold)
                            Text(
                                "En fazla 5 aktif hesap. Eklenen hesabın oyunda önceden kayıtlı olması gerekir.",
                                color = AdminMuted,
                                fontSize = 10.sp,
                            )
                            OutlinedTextField(
                                value = ownerEmailInput,
                                onValueChange = { ownerEmailInput = it.trim().take(120) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Hesabın e-posta adresi") },
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    val targetEmail = ownerEmailInput.trim()
                                    if (targetEmail.isBlank()) {
                                        notice = "E-posta adresi gir."
                                    } else {
                                        scope.launch {
                                            busy = true
                                            runCatching {
                                                backend.adminSetOwnerAccount(
                                                    email = targetEmail,
                                                    lifetimeVip = true,
                                                    unlimitedDiamonds = true,
                                                    unlimitedSonCoin = true,
                                                    active = true,
                                                )
                                            }.onSuccess {
                                                notice = "$targetEmail özel hesap olarak eklendi."
                                                ownerEmailInput = ""
                                            }.onFailure {
                                                error = when {
                                                    (it.message ?: "").contains("owner_account_limit_reached") -> "En fazla 5 aktif özel hesap kullanılabilir."
                                                    (it.message ?: "").contains("user_not_found") -> "Bu e-posta ile kayıtlı oyuncu bulunamadı."
                                                    else -> it.message ?: "Özel hesap eklenemedi."
                                                }
                                            }
                                            reload()
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.PersonAdd, null)
                                Spacer(Modifier.width(7.dp))
                                Text("ÖZEL HESAP EKLE")
                            }
                        }
                    }
                }

                AdminSection.GAMES -> {
                    item { AdminSectionTitle("OYUNLAR", Icons.Rounded.SportsEsports) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminStatCard("Canlı Maç", d.activeRooms.toString(), "Şu an", Modifier.weight(1f))
                            AdminStatCard("Kuyrukta", d.queueWaiting.toString(), "Eşleşme bekliyor", Modifier.weight(1f))
                        }
                    }
                    if (gameControls.isEmpty()) {
                        item { AdminEmpty("Oyun kontrol bilgileri alınamadı.") }
                    } else {
                        items(gameControls, key = { it.configKey }) { control ->
                            AdminGameControlRow(
                                control = control,
                                enabled = !busy,
                                onChange = { value ->
                                    scope.launch {
                                        busy = true
                                        runCatching { backend.adminSetGameControl(control.configKey, value) }
                                            .onSuccess { notice = "${control.title} ayarı güncellendi." }
                                            .onFailure { error = it.message ?: "Ayar değiştirilemedi." }
                                        reload()
                                        busy = false
                                    }
                                },
                            )
                        }
                    }
                    item {
                        Text(
                            "Bakım modu gibi kritik ayarlar yalnız gerektiğinde kullanılmalı. Tüm değişiklikler audit log'a yazılır.",
                            color = AdminMuted,
                            fontSize = 10.sp,
                        )
                    }
                }

                AdminSection.ANNOUNCEMENTS -> {
                    item { AdminSectionTitle("DUYURULAR", Icons.Rounded.Campaign) }
                    item {
                        AdminWideCard {
                            Text(
                                "Oyuncuların göreceği duyuruyu buradan yönet.",
                                color = AdminMuted,
                                fontSize = 11.sp,
                            )
                            OutlinedTextField(
                                announcementText,
                                { announcementText = it.take(500) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Duyuru metni") },
                                minLines = 3,
                                maxLines = 6,
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Duyuruyu yayınla", color = AdminText, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = announcementEnabled,
                                    onCheckedChange = { announcementEnabled = it },
                                    enabled = !busy,
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        runCatching {
                                            backend.adminSetAnnouncement(announcementText.trim(), announcementEnabled)
                                        }.onSuccess {
                                            notice = "Duyuru güncellendi."
                                        }.onFailure {
                                            error = it.message ?: "Duyuru güncellenemedi."
                                        }
                                        reload()
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("DUYURUYU KAYDET")
                            }
                        }
                    }
                }

                AdminSection.MAINTENANCE -> {
                    item { AdminSectionTitle("SİSTEM SAĞLIĞI", Icons.Rounded.HealthAndSafety) }
                    if (health.isEmpty()) item { AdminEmpty("Sağlık bilgileri alınamadı.") }
                    else items(health, key = { it.metricKey }) { h -> AdminHealthRow(h) }

                    item { AdminSectionTitle("GÜVENLİ ONARIM", Icons.Rounded.BuildCircle) }
                    item {
                        AdminWideCard {
                            Text(
                                "Bu araçlar kullanıcı hesabı, XP veya oyun verilerini sıfırlamaz.",
                                color = AdminMuted,
                                fontSize = 11.sp,
                            )
                            AdminRepairGrid { action -> repairConfirm = action }
                        }
                    }

                    item { AdminSectionTitle("ALTYAPI & KAPASİTE", Icons.Rounded.Dns) }
                    if (capacity.isEmpty()) item { AdminEmpty("Altyapı bilgileri alınamadı.") }
                    else items(capacity, key = { it.metricKey }) { metric ->
                        AdminCapacityRow(
                            metric = metric,
                            onResolve = {
                                if (metric.resolveUrl.isNotBlank()) uriHandler.openUri(metric.resolveUrl)
                            },
                        )
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
private fun AdminGameControlRow(
    control: AdminGameControlDto,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val isMaintenance = control.configKey == "maintenance_mode"
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(control.title, color = AdminText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(control.detail, color = AdminMuted, fontSize = 10.sp)
            }
            Switch(
                checked = control.enabled,
                onCheckedChange = onChange,
                enabled = enabled,
                colors = if (isMaintenance) {
                    SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AdminRed)
                } else SwitchDefaults.colors()
            )
        }
        if (isMaintenance && control.enabled) {
            Text("BAKIM MODU AÇIK", color = AdminRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AdminCapacityCompact(metric: AdminCapacityDto) {
    val tone = when (metric.status) {
        "critical" -> AdminRed
        "warning" -> AdminGold
        else -> AdminGreen
    }
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(metric.title, color = AdminText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("%${metric.percentUsed}", color = tone, fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(
            progress = { (metric.percentUsed / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = tone,
            trackColor = Color.White.copy(alpha = .07f),
        )
        Text(
            "${formatBytes(metric.usedValue)} / ${formatBytes(metric.limitValue)}",
            color = AdminMuted,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun AdminOwnerAccountCard(
    account: AdminOwnerAccountDto,
    enabled: Boolean,
    onChange: (Boolean, Boolean, Boolean, Boolean) -> Unit,
) {
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(account.displayName, color = AdminText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(account.email, color = AdminMuted, fontSize = 10.sp)
            }
            Surface(
                color = if (account.active) AdminGreen.copy(alpha = .14f) else AdminRed.copy(alpha = .12f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    if (account.active) "AKTİF" else "PASİF",
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = if (account.active) AdminGreen else AdminRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            "Rating: ${account.rating} • Görünen bakiye: ${account.currentDiamonds} • Sınırsız haklarda harcama düşmez.",
            color = AdminMuted,
            fontSize = 10.sp,
        )
        AdminToggleRow(
            title = "Süresiz VIP",
            detail = if (account.lifetimeVip) "VIP hakkı kalıcı olarak korunur." else "Süresiz VIP kapalı.",
            checked = account.lifetimeVip,
            enabled = enabled,
        ) { onChange(it, account.unlimitedDiamonds, account.unlimitedSonCoin, account.active) }
        HorizontalDivider(color = Color.White.copy(alpha = .08f))
        AdminToggleRow(
            title = "Sınırsız Elmas",
            detail = "Mağaza alışverişlerinde elmas bakiyesi düşmez.",
            checked = account.unlimitedDiamonds,
            enabled = enabled,
        ) { onChange(account.lifetimeVip, it, account.unlimitedSonCoin, account.active) }
        HorizontalDivider(color = Color.White.copy(alpha = .08f))
        AdminToggleRow(
            title = "Sınırsız Son Coin",
            detail = "Son Coin kullanan özel satın alımlarda bakiye düşmez.",
            checked = account.unlimitedSonCoin,
            enabled = enabled,
        ) { onChange(account.lifetimeVip, account.unlimitedDiamonds, it, account.active) }
        HorizontalDivider(color = Color.White.copy(alpha = .08f))
        AdminToggleRow(
            title = "Özel hesabı etkin tut",
            detail = "Kapatılırsa sınırsız haklar devre dışı kalır; hesap ve verileri silinmez.",
            checked = account.active,
            enabled = enabled,
        ) { onChange(account.lifetimeVip, account.unlimitedDiamonds, account.unlimitedSonCoin, it) }
    }
}

@Composable
private fun AdminCapacityRow(metric: AdminCapacityDto, onResolve: () -> Unit) {
    val tone = when (metric.status) {
        "critical" -> AdminRed
        "warning" -> AdminGold
        "ok" -> AdminGreen
        else -> AdminBlue
    }
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (metric.status) {
                    "critical", "warning" -> Icons.Rounded.Warning
                    "ok" -> Icons.Rounded.CheckCircle
                    else -> Icons.Rounded.Info
                },
                null,
                tint = tone,
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(metric.title, color = AdminText, fontWeight = FontWeight.Bold)
                Text(metric.detail, color = AdminMuted, fontSize = 10.sp)
            }
            if (metric.unit == "bytes") {
                Text("%${metric.percentUsed}", color = tone, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
        if (metric.unit == "bytes") {
            LinearProgressIndicator(
                progress = { (metric.percentUsed / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = tone,
                trackColor = Color.White.copy(alpha = .07f),
            )
            Text(
                "${formatBytes(metric.usedValue)} / ${formatBytes(metric.limitValue)}",
                color = AdminMuted,
                fontSize = 10.sp,
            )
        }
        OutlinedButton(
            onClick = onResolve,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, tone.copy(alpha = .55f)),
        ) {
            Icon(Icons.Rounded.OpenInNew, null, tint = tone, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("PROBLEMİ ÇÖZ / İLGİLİ SAYFAYI AÇ", color = tone, fontSize = 11.sp)
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value <= 0) return "0 B"
    val mb = value / (1024.0 * 1024.0)
    return if (mb < 1024) String.format(Locale.US, "%.1f MB", mb)
    else String.format(Locale.US, "%.2f GB", mb / 1024.0)
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
