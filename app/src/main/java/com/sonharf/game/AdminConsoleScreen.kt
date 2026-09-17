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

private val AdminBg = Color(0xFFF5F8FC)
private val AdminPanel = Color(0xFFFFFFFF)
private val AdminPanel2 = Color(0xFFEEF5FF)
private val AdminGold = Color(0xFFF6C453)
private val AdminText = Color(0xFF142033)
private val AdminMuted = Color(0xFF6F7C8D)
private val AdminGreen = Color(0xFF35C878)
private val AdminRed = Color(0xFFFF5F57)
private val AdminBlue = Color(0xFF1677FF)

private data class RepairAction(val key: String, val title: String, val detail: String)

private enum class AdminSection(val title: String) {
    OVERVIEW("Ana Sayfa"),
    PLAYERS("Oyuncular & VIP"),
    GAMES("Oyunlar"),
    ANNOUNCEMENTS("Duyurular"),
    STORE("Mağaza"),
    TESTS("Test Hesapları"),
    SYSTEM("Sistem"),
    SECURITY("Güvenlik"),
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
    var storeCatalog by remember { mutableStateOf<List<AdminStoreCatalogDto>>(emptyList()) }
    var auditEntries by remember { mutableStateOf<List<AdminAuditEntryDto>>(emptyList()) }
    var recentErrors by remember { mutableStateOf<List<AdminSystemEventDto>>(emptyList()) }
    var selectedSection by remember { mutableStateOf(AdminSection.OVERVIEW) }
    var ownerEmailInput by remember { mutableStateOf("") }
    var playerSearchText by remember { mutableStateOf("") }
    var playerResults by remember { mutableStateOf<List<AdminPlayerOpsDto>>(emptyList()) }
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
            storeCatalog = backend.getAdminStoreCatalog()
            auditEntries = backend.getAdminAuditV2()
            recentErrors = backend.getAdminRecentErrors()
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

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AdminBg, Color(0xFFEEF5FF))))) {
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
                            Text("Oyuncu ara", color = AdminText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text(
                                "E-posta veya oyuncu adıyla ara. Buradan yalnız VIP durumunu yönet.",
                                color = AdminMuted,
                                fontSize = 10.sp,
                            )
                            OutlinedTextField(
                                value = playerSearchText,
                                onValueChange = { playerSearchText = it.take(120) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("E-posta / oyuncu adı") },
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    val q = playerSearchText.trim()
                                    if (q.length < 2) {
                                        notice = "Arama için en az 2 karakter gir."
                                    } else {
                                        scope.launch {
                                            busy = true
                                            runCatching { backend.adminSearchPlayersV2(q) }
                                                .onSuccess {
                                                    playerResults = it
                                                    notice = if (it.isEmpty()) "Oyuncu bulunamadı." else "${it.size} oyuncu bulundu."
                                                }
                                                .onFailure { error = it.message ?: "Oyuncu aranamadı." }
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.Search, null)
                                Spacer(Modifier.width(7.dp))
                                Text("OYUNCU ARA")
                            }
                        }
                    }

                    if (playerResults.isNotEmpty()) {
                        items(playerResults, key = { it.userId }) { player ->
                            AdminPlayerSearchRow(
                                player = player,
                                enabled = !busy,
                                onVipChange = { value ->
                                    scope.launch {
                                        busy = true
                                        runCatching { backend.adminSetPlayerVip(player.userId, value) }
                                            .onSuccess {
                                                notice = "${player.displayName} VIP durumu güncellendi."
                                                playerResults = backend.adminSearchPlayersV2(playerSearchText)
                                            }
                                            .onFailure {
                                                error = when {
                                                    (it.message ?: "").contains("owner_lifetime_vip_locked") ->
                                                        "Bu özel hesapta süresiz VIP korunuyor; kapatılamaz."
                                                    else -> it.message ?: "VIP durumu değiştirilemedi."
                                                }
                                            }
                                        reload()
                                        busy = false
                                    }
                                },
                                onDiamondDelta = { delta ->
                                    scope.launch {
                                        busy = true
                                        runCatching { backend.adminAdjustPlayerDiamonds(player.userId, delta) }
                                            .onSuccess { notice = "${player.displayName} elmas bakiyesi güncellendi." }
                                            .onFailure { error = it.message ?: "Elmas bakiyesi güncellenemedi." }
                                        playerResults = runCatching { backend.adminSearchPlayersV2(playerSearchText) }.getOrDefault(playerResults)
                                        reload(); busy = false
                                    }
                                },
                                onBlockedChange = { blocked ->
                                    scope.launch {
                                        busy = true
                                        runCatching { backend.adminSetPlayerBlocked(player.userId, blocked) }
                                            .onSuccess { notice = if (blocked) "${player.displayName} 24 saat geçici engellendi." else "${player.displayName} engeli kaldırıldı." }
                                            .onFailure { error = it.message ?: "Hesap durumu güncellenemedi." }
                                        playerResults = runCatching { backend.adminSearchPlayersV2(playerSearchText) }.getOrDefault(playerResults)
                                        reload(); busy = false
                                    }
                                },
                            )
                        }
                    }

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


                AdminSection.STORE -> {
                    item { AdminSectionTitle("MAĞAZA", Icons.Rounded.Storefront) }
                    item {
                        AdminWideCard {
                            Text("Ürün kataloğu", color = AdminText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text("Yalnız mevcut ürünlerin yayın durumu ve gelir analiz fiyatı yönetilir. Bu alan oyun gücü/rating satmaz.", color = AdminMuted, fontSize = 10.sp)
                        }
                    }
                    if (storeCatalog.isEmpty()) item { AdminEmpty("Mağaza kataloğu bilgisi alınamadı.") }
                    else items(storeCatalog, key = { it.productId }) { product ->
                        AdminStoreCatalogRow(
                            product = product,
                            enabled = !busy,
                            onSetPrice = { priceProduct = product.productId; priceText = if (product.grossPriceMinor > 0) (product.grossPriceMinor / 100.0).toString() else "" },
                            onEnabled = { enabled ->
                                scope.launch {
                                    busy = true
                                    runCatching { backend.adminSetStoreEnabled(product.productId, enabled) }
                                        .onSuccess { notice = "${product.productId} yayın durumu güncellendi." }
                                        .onFailure { error = it.message ?: "Ürün durumu güncellenemedi." }
                                    reload(); busy = false
                                }
                            },
                        )
                    }
                    if (storeItems.isNotEmpty()) {
                        item { AdminSectionTitle("EDİNİM ÖZETİ", Icons.Rounded.Inventory2) }
                        items(storeItems, key = { it.itemId }) { item ->
                            AdminSimpleRow(item.itemName, "${item.acquisitionCount} edinim")
                        }
                    }
                }

                AdminSection.TESTS -> {
                    item { AdminSectionTitle("TEST / ÖZEL HESAPLAR", Icons.Rounded.Science) }
                    item {
                        AdminWideCard {
                            Text("Gerçek test hesabı yoksa sahte kullanıcı oluşturulmaz.", color = AdminText, fontWeight = FontWeight.Bold)
                            Text("Rating ve lig puanı burada değiştirilemez. Yalnız backend'de tanımlı özel hesapların güvenli test hakları gösterilir.", color = AdminMuted, fontSize = 10.sp)
                        }
                    }
                    if (ownerAccounts.isEmpty()) item { AdminEmpty("Tanımlı test/özel hesap yok.") }
                    else items(ownerAccounts.take(3), key = { it.userId }) { account ->
                        AdminOwnerAccountCard(
                            account = account,
                            enabled = !busy,
                            onChange = { vip, diamonds, sonCoin, active ->
                                scope.launch {
                                    busy = true
                                    runCatching { backend.adminSetOwnerAccount(account.email, vip, diamonds, sonCoin, active) }
                                        .onSuccess { notice = "${account.displayName} test hesabı hakları güncellendi." }
                                        .onFailure { error = it.message ?: "Test hesabı güncellenemedi." }
                                    reload(); busy = false
                                }
                            },
                        )
                    }
                    if (ownerAccounts.size < 3) item { AdminEmpty("Şu anda backend'de ${ownerAccounts.size} gerçek test/özel hesap tanımlı. Eksik hesaplar için sahte UUID oluşturulmadı.") }
                }

                AdminSection.SYSTEM -> {
                    item { AdminSectionTitle("SUPABASE DURUMU", Icons.Rounded.Dns) }
                    if (capacity.isEmpty()) item { AdminEmpty("Bilgi alınamadı.") }
                    else items(capacity.filter { it.metricKey.startsWith("supabase_") }, key = { it.metricKey }) { metric ->
                        AdminCapacityRow(metric) { if (metric.resolveUrl.isNotBlank()) uriHandler.openUri(metric.resolveUrl) }
                    }
                    item { AdminSectionTitle("SÜRÜM / BUILD", Icons.Rounded.Info) }
                    item {
                        AdminWideCard {
                            AdminInlineValue("versionName", BuildConfig.VERSION_NAME)
                            AdminInlineValue("versionCode", BuildConfig.VERSION_CODE.toString())
                            AdminInlineValue("Build type", BuildConfig.BUILD_TYPE)
                            Text("Git commit SHA/tarihi bu APK'nin BuildConfig'ine güvenli biçimde enjekte edilmediği için tahmin gösterilmez.", color = AdminMuted, fontSize = 10.sp)
                        }
                    }
                    item { AdminSectionTitle("GITHUB", Icons.Rounded.Code) }
                    item {
                        AdminWideCard {
                            Text("GitHub token APK içine gömülmedi.", color = AdminText, fontWeight = FontWeight.Bold)
                            Text("Private repository branch/commit/Actions kota verisi için güvenli backend entegrasyonu gerekiyor. Bu bilgi mevcut backend üzerinden alınamadığında tahmin üretilmez.", color = AdminMuted, fontSize = 10.sp)
                            Text("Durum: Ek GitHub yetkisi / güvenli backend gerekiyor.", color = AdminBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item { AdminSectionTitle("SON HATALAR", Icons.Rounded.ErrorOutline) }
                    if (recentErrors.isEmpty()) item { AdminEmpty("Son error/critical kaydı yok veya bilgi alınamadı.") }
                    else items(recentErrors, key = { it.id }) { event -> AdminErrorRow(event) }
                }

                AdminSection.SECURITY -> {
                    item { AdminSectionTitle("ADMIN İŞLEM GEÇMİŞİ", Icons.Rounded.Security) }
                    item {
                        AdminWideCard {
                            Text("Kritik yönetici işlemleri server-side audit log'a yazılır.", color = AdminText, fontWeight = FontWeight.Bold)
                            Text("VIP, elmas, engelleme, mağaza, bakım ve güvenli ayar değişiklikleri burada izlenir.", color = AdminMuted, fontSize = 10.sp)
                        }
                    }
                    if (auditEntries.isEmpty()) item { AdminEmpty("Audit log bilgisi alınamadı.") }
                    else items(auditEntries, key = { it.id }) { entry -> AdminAuditRow(entry) }
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
private fun AdminPlayerSearchRow(
    player: AdminPlayerOpsDto,
    enabled: Boolean,
    onVipChange: (Boolean) -> Unit,
    onDiamondDelta: (Int) -> Unit,
    onBlockedChange: (Boolean) -> Unit,
) {
    var pendingVip by remember(player.userId) { mutableStateOf<Boolean?>(null) }
    var pendingDiamond by remember(player.userId) { mutableStateOf<Int?>(null) }
    var pendingBlock by remember(player.userId) { mutableStateOf<Boolean?>(null) }
    val blocked = !player.blockedUntil.isNullOrBlank()
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(player.displayName, color = AdminText, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(player.email, color = AdminMuted, fontSize = 10.sp)
                Text("User ID: ${player.userId}", color = AdminMuted, fontSize = 9.sp)
                Text("Rating: ${player.rating} (salt okunur) • Elmas: ${player.diamonds}", color = AdminMuted, fontSize = 10.sp)
                Text("Son görülme: ${player.lastSeenAt ?: "Bilgi yok"}", color = AdminMuted, fontSize = 9.sp)
                Text(if (blocked) "Hesap: GEÇİCİ ENGELLİ" else "Hesap: AKTİF", color = if (blocked) AdminRed else AdminGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (player.isOwnerAccount) {
                Surface(color = AdminGold.copy(alpha = .15f), shape = RoundedCornerShape(999.dp)) {
                    Text("ÖZEL", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = AdminGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        AdminToggleRow(
            title = "VIP",
            detail = if (player.isOwnerAccount) "Özel hesapta lifetime VIP koruması olabilir." else "Değişiklik audit log'a yazılır.",
            checked = player.isVip,
            enabled = enabled && !(player.isOwnerAccount && player.isVip),
        ) { pendingVip = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pendingDiamond = 100 }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("+100 ELMAS", fontSize = 10.sp) }
            OutlinedButton(onClick = { pendingDiamond = -100 }, enabled = enabled && player.diamonds > 0, modifier = Modifier.weight(1f)) { Text("-100 ELMAS", fontSize = 10.sp) }
        }
        OutlinedButton(onClick = { pendingBlock = !blocked }, enabled = enabled && !player.isOwnerAccount, modifier = Modifier.fillMaxWidth()) {
            Text(if (blocked) "GEÇİCİ ENGELİ KALDIR" else "24 SAAT GEÇİCİ ENGELLE", color = if (blocked) AdminGreen else AdminRed, fontSize = 10.sp)
        }
    }
    pendingVip?.let { value ->
        AlertDialog(
            onDismissRequest = { pendingVip = null },
            title = { Text("VIP durumunu değiştir?") },
            text = { Text("${player.displayName} için VIP ${if (value) "açılacak" else "kapatılacak"}. İşlem audit log'a yazılır.") },
            confirmButton = { Button(onClick = { pendingVip = null; onVipChange(value) }) { Text("ONAYLA") } },
            dismissButton = { TextButton(onClick = { pendingVip = null }) { Text("VAZGEÇ") } },
        )
    }
    pendingDiamond?.let { delta ->
        AlertDialog(
            onDismissRequest = { pendingDiamond = null },
            title = { Text("Elmas bakiyesini değiştir?") },
            text = { Text("${player.displayName}: ${if (delta > 0) "+" else ""}$delta elmas. Rating/lig etkilenmez; işlem audit log'a yazılır.") },
            confirmButton = { Button(onClick = { pendingDiamond = null; onDiamondDelta(delta) }) { Text("ONAYLA") } },
            dismissButton = { TextButton(onClick = { pendingDiamond = null }) { Text("VAZGEÇ") } },
        )
    }
    pendingBlock?.let { blockedValue ->
        AlertDialog(
            onDismissRequest = { pendingBlock = null },
            title = { Text(if (blockedValue) "Hesabı geçici engelle?" else "Engeli kaldır?") },
            text = { Text(if (blockedValue) "Hesap 24 saat giriş yapamayacak. Oyuncu XP/rating/envanteri silinmez." else "Geçici giriş engeli kaldırılacak.") },
            confirmButton = { Button(onClick = { pendingBlock = null; onBlockedChange(blockedValue) }) { Text("ONAYLA") } },
            dismissButton = { TextButton(onClick = { pendingBlock = null }) { Text("VAZGEÇ") } },
        )
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
            trackColor = MainUi.Border.copy(alpha = .55f),
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
        HorizontalDivider(color = MainUi.Border.copy(alpha = .72f))
        AdminToggleRow(
            title = "Sınırsız Elmas",
            detail = "Mağaza alışverişlerinde elmas bakiyesi düşmez.",
            checked = account.unlimitedDiamonds,
            enabled = enabled,
        ) { onChange(account.lifetimeVip, it, account.unlimitedSonCoin, account.active) }
        HorizontalDivider(color = MainUi.Border.copy(alpha = .72f))
        AdminToggleRow(
            title = "Sınırsız Son Coin",
            detail = "Son Coin kullanan özel satın alımlarda bakiye düşmez.",
            checked = account.unlimitedSonCoin,
            enabled = enabled,
        ) { onChange(account.lifetimeVip, account.unlimitedDiamonds, it, account.active) }
        HorizontalDivider(color = MainUi.Border.copy(alpha = .72f))
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
                trackColor = MainUi.Border.copy(alpha = .55f),
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
            Text(
                if (metric.metricKey == "github_rollback") "SON SAĞLAM SÜRÜME DÖN" else "PROBLEMİ ÇÖZ / İLGİLİ SAYFAYI AÇ",
                color = tone,
                fontSize = 11.sp,
            )
        }
    }
}


@Composable
private fun AdminStoreCatalogRow(
    product: AdminStoreCatalogDto,
    enabled: Boolean,
    onSetPrice: () -> Unit,
    onEnabled: (Boolean) -> Unit,
) {
    var pendingEnabled by remember(product.productId) { mutableStateOf<Boolean?>(null) }
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.productId, color = AdminText, fontWeight = FontWeight.Black)
                Text(if (product.grossPriceMinor > 0) formatMoney(product.grossPriceMinor, product.currency) else "Fiyat bilgisi yok", color = AdminMuted, fontSize = 10.sp)
                Text("Tür: mevcut katalog ürünü • Sıra: ${product.sortOrder}", color = AdminMuted, fontSize = 9.sp)
            }
            Switch(checked = product.enabled, onCheckedChange = { pendingEnabled = it }, enabled = enabled)
        }
        OutlinedButton(onClick = onSetPrice, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("FİYAT BİLGİSİNİ DÜZENLE") }
    }
    pendingEnabled?.let { value ->
        AlertDialog(
            onDismissRequest = { pendingEnabled = null },
            title = { Text("Mağaza yayın durumunu değiştir?") },
            text = { Text("${product.productId} ${if (value) "yayına alınacak" else "yayından kaldırılacak"}. İşlem audit log'a yazılır.") },
            confirmButton = { Button(onClick = { pendingEnabled = null; onEnabled(value) }) { Text("ONAYLA") } },
            dismissButton = { TextButton(onClick = { pendingEnabled = null }) { Text("VAZGEÇ") } },
        )
    }
}

@Composable
private fun AdminInlineValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AdminMuted, fontSize = 11.sp)
        Text(value, color = AdminText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminErrorRow(event: AdminSystemEventDto) {
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(event.severity.uppercase(), color = if (event.severity == "critical") AdminRed else AdminGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text(event.eventType, color = AdminText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Text("${event.source} • ${event.createdAt}", color = AdminMuted, fontSize = 9.sp)
        Text(event.details.take(500), color = AdminMuted, fontSize = 10.sp)
        Text("App/Device bilgisi event kaydında yoksa tahmin edilmez.", color = AdminMuted, fontSize = 9.sp)
    }
}

@Composable
private fun AdminAuditRow(entry: AdminAuditEntryDto) {
    AdminWideCard {
        Text(entry.action, color = AdminText, fontWeight = FontWeight.Black)
        Text("${entry.createdAt} • ${entry.adminEmail.ifBlank { "admin" }}", color = AdminMuted, fontSize = 9.sp)
        Text("Hedef: ${entry.targetType} ${entry.targetId ?: ""}", color = AdminMuted, fontSize = 10.sp)
        entry.beforeData?.takeIf { it.isNotBlank() }?.let { Text("Önce: ${it.take(220)}", color = AdminMuted, fontSize = 9.sp) }
        entry.afterData?.takeIf { it.isNotBlank() }?.let { Text("Sonra: ${it.take(220)}", color = AdminMuted, fontSize = 9.sp) }
        if (entry.outcome != "success") Text("${entry.outcome}: ${entry.errorText.orEmpty()}", color = AdminRed, fontSize = 9.sp)
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
