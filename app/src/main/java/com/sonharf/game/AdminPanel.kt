package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

@Serializable
internal data class AdminDashboardDto(
    @SerialName("total_users") val totalUsers: Long = 0,
    @SerialName("active_now") val activeNow: Long = 0,
    @SerialName("active_today") val activeToday: Long = 0,
    @SerialName("active_7d") val active7d: Long = 0,
    @SerialName("vip_users") val vipUsers: Long = 0,
    @SerialName("matches_today") val matchesToday: Long = 0,
    @SerialName("active_rooms") val activeRooms: Long = 0,
    @SerialName("stale_rooms") val staleRooms: Long = 0,
    @SerialName("queue_waiting") val queueWaiting: Long = 0,
    @SerialName("verified_purchases") val verifiedPurchases: Long = 0,
    @SerialName("gross_revenue_minor") val grossRevenueMinor: Long = 0,
    @SerialName("revenue_currency") val revenueCurrency: String? = null,
)

@Serializable
internal data class AdminCapacityDto(
    val title: String = "",
    val status: String = "",
    @SerialName("used_value") val used: Long = 0,
    @SerialName("limit_value") val limit: Long = 0,
    @SerialName("percent_used") val percent: Int = 0,
    val unit: String = "",
)

@Serializable
internal data class AdminHealthDto(
    val title: String = "",
    val status: String = "",
    @SerialName("metric_value") val value: Long = 0,
    val detail: String? = null,
)

@Serializable
internal data class AdminErrorDto(
    val id: Long = 0,
    val severity: String = "",
    val source: String = "",
    @SerialName("event_type") val eventType: String = "",
    val details: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
internal data class AdminAnnouncementDto(
    @SerialName("message_tr") val messageTr: String? = null,
    @SerialName("message_en") val messageEn: String? = null,
    val enabled: Boolean = false,
    val maintenance: Boolean = false,
)

@Serializable
internal data class AdminPlayerDto(
    @SerialName("user_id") val userId: String,
    val email: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_vip") val isVip: Boolean = false,
    val rating: Int? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("blocked_until") val blockedUntil: String? = null,
    val diamonds: Int? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

/** The GitHub repository's size (public repository: read without a token). */
internal data class RepoUsage(val sizeKb: Long, val artifacts: Long?)

private object AdminApi {
    private val rest get() = SupabaseProvider.client.postgrest

    suspend fun dashboard() = rest.rpc("admin_dashboard_v1").decodeList<AdminDashboardDto>().firstOrNull()
    suspend fun capacity() = rest.rpc("admin_capacity_v1").decodeList<AdminCapacityDto>()
    suspend fun health() = rest.rpc("admin_health_v1").decodeList<AdminHealthDto>()
    suspend fun errors() = rest.rpc("admin_recent_errors_v1").decodeList<AdminErrorDto>()
    suspend fun announcement() = rest.rpc("admin_get_announcement_v2").decodeList<AdminAnnouncementDto>().firstOrNull()
    suspend fun setAnnouncement(tr: String, en: String, enabled: Boolean) {
        rest.rpc("admin_set_announcement_v2", buildJsonObject {
            put("p_message_tr", tr); put("p_message_en", en); put("p_enabled", enabled); put("p_maintenance", false)
        })
    }
    suspend fun search(query: String) = rest.rpc("admin_list_players_v1", buildJsonObject { put("p_query", query) }).decodeList<AdminPlayerDto>()
    suspend fun giftCoins(id: String, amount: Int) { rest.rpc("admin_gift_coins_v1", buildJsonObject { put("p_user_id", id); put("p_amount", amount) }) }
    suspend fun deletePlayer(id: String) { rest.rpc("admin_delete_player_v1", buildJsonObject { put("p_user_id", id) }) }
    suspend fun setVip(id: String, on: Boolean) { rest.rpc("admin_set_player_vip_v1", buildJsonObject { put("p_user_id", id); put("p_enabled", on) }) }
    suspend fun setBlocked(id: String, on: Boolean) { rest.rpc("admin_set_player_blocked_v1", buildJsonObject { put("p_user_id", id); put("p_blocked", on) }) }
    suspend fun setAdmin(id: String, on: Boolean) { rest.rpc("admin_set_admin_v1", buildJsonObject { put("p_user_id", id); put("p_enabled", on) }) }
    suspend fun setChampion(id: String, days: Int) { rest.rpc("admin_set_test_champion_v1", buildJsonObject { put("p_user_id", id); put("p_days", days) }) }
    suspend fun maintenance(): String = rest.rpc("admin_run_maintenance").data

    suspend fun repo(): RepoUsage? = withContext(Dispatchers.IO) {
        fun get(url: String): String? = runCatching {
            val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            c.connectTimeout = 8_000; c.readTimeout = 8_000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            try { if (c.responseCode == 200) c.inputStream.bufferedReader().readText() else null } finally { c.disconnect() }
        }.getOrNull()
        val repo = get("https://api.github.com/repos/makalega68-source/son-harf") ?: return@withContext null
        val size = Json.parseToJsonElement(repo).jsonObject["size"]?.jsonPrimitive?.longOrNull ?: return@withContext null
        val artifacts = get("https://api.github.com/repos/makalega68-source/son-harf/actions/artifacts?per_page=1")
            ?.let { Json.parseToJsonElement(it).jsonObject["total_count"]?.jsonPrimitive?.longOrNull }
        RepoUsage(size, artifacts)
    }
}

/** Admin-only entry in the lobby menu: shown when the signed-in player is on the admin list. */
internal fun isAdmin(userId: String?): Boolean = !userId.isNullOrBlank() && userId in AdminRoster.ids

/** The admin panel: one simple screen with five tabs. Every action is checked again on the server. */
@Composable
internal fun AdminPanelDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(LobbyBrand.Sky).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().background(LobbyBrand.Band).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(sh("YÖNETİM PANELİ", "ADMIN PANEL"), Modifier.weight(1f), color = LobbyBrand.Gold,
                    fontSize = 18.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, sh("Kapat", "Close"), tint = Color.White) }
            }
            var tab by remember { mutableIntStateOf(0) }
            val tabs = listOf(sh("Durum", "Status"), sh("Hatalar", "Errors"), sh("Duyuru", "Notice"), sh("Oyuncu", "Players"), sh("Bakım", "Upkeep"))
            TabRow(selectedTabIndex = tab, containerColor = LobbyBrand.NavBar, contentColor = LobbyBrand.Gold) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i },
                        text = { Text(title, fontSize = 11.sp, maxLines = 1, color = if (tab == i) LobbyBrand.Gold else Color.White) })
                }
            }
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> AdminStatusTab()
                    1 -> AdminErrorsTab()
                    2 -> AdminAnnouncementTab()
                    3 -> AdminPlayersTab()
                    else -> AdminMaintenanceTab()
                }
            }
        }
    }
}

@Composable
private fun AdminCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().premiumPanel(RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun AdminHeading(text: String) =
    Text(text, color = LobbyBrand.Gold, fontSize = 13.sp, fontWeight = FontWeight.Black)

@Composable
private fun AdminLine(label: String, value: String, warn: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
        Text(value, color = if (warn) Color(0xFFFF8A80) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminNotice(text: String?) {
    if (!text.isNullOrBlank()) Text(text, color = Color(0xFFFFE7A3), fontSize = 12.sp)
}

private fun bytesText(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(java.util.Locale.ROOT, "%.2f GB", bytes / 1073741824.0)
    bytes >= 1L shl 20 -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1048576.0)
    else -> "${bytes / 1024} KB"
}

private fun shortTime(iso: String?): String =
    iso?.takeIf { it.length >= 16 }?.let { it.substring(0, 10) + " " + it.substring(11, 16) } ?: "—"

@Composable
private fun AdminUsageBar(title: String, used: Long, limit: Long, percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AdminLine(title, if (limit > 0) "${bytesText(used)} / ${bytesText(limit)} · %$percent" else bytesText(used), warn = percent >= 85)
        if (limit > 0) LinearProgressIndicator(progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (percent >= 85) Color(0xFFFF6E6E) else if (percent >= 70) LobbyBrand.Gold else Color(0xFF59D49A),
            trackColor = Color.White.copy(alpha = .15f))
    }
}

@Composable
private fun AdminStatusTab() {
    var reload by remember { mutableIntStateOf(0) }
    var dash by remember { mutableStateOf<AdminDashboardDto?>(null) }
    var capacity by remember { mutableStateOf<List<AdminCapacityDto>>(emptyList()) }
    var health by remember { mutableStateOf<List<AdminHealthDto>>(emptyList()) }
    var repo by remember { mutableStateOf<RepoUsage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(reload) {
        error = null
        gameRequestResult { dash = AdminApi.dashboard(); capacity = AdminApi.capacity(); health = AdminApi.health() }
            .onFailure { error = sh("Veriler alınamadı: ", "Could not load: ") + it.message.orEmpty().take(120) }
        repo = AdminApi.repo()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { reload++ }) {
            Icon(Icons.Rounded.Refresh, null, tint = LobbyBrand.Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text(sh("Yenile", "Refresh"), color = Color.White)
        }
        AdminNotice(error)
        AdminCard {
            AdminHeading(sh("OYUNCULAR VE MAÇLAR", "PLAYERS AND MATCHES"))
            val d = dash
            if (d == null) Text("…", color = Color.White) else {
                AdminLine(sh("Toplam oyuncu", "Total players"), "${d.totalUsers}")
                AdminLine(sh("Şu an çevrimiçi", "Online now"), "${d.activeNow}")
                AdminLine(sh("Bugün aktif", "Active today"), "${d.activeToday}")
                AdminLine(sh("7 günde aktif", "Active in 7 days"), "${d.active7d}")
                AdminLine("PRO", "${d.vipUsers}")
                AdminLine(sh("Bugünkü maç", "Matches today"), "${d.matchesToday}")
                AdminLine(sh("Süren maç", "Live rooms"), "${d.activeRooms}")
                AdminLine(sh("Takılı maç", "Stuck rooms"), "${d.staleRooms}", warn = d.staleRooms > 0)
                AdminLine(sh("Eşleşme kuyruğu", "Match queue"), "${d.queueWaiting}")
                AdminLine(sh("Doğrulanmış satın alma", "Verified purchases"), "${d.verifiedPurchases}")
                AdminLine(sh("Brüt gelir", "Gross revenue"),
                    String.format(java.util.Locale.ROOT, "%.2f %s", d.grossRevenueMinor / 100.0, d.revenueCurrency ?: "TRY"))
            }
        }
        AdminCard {
            AdminHeading(sh("DEPOLAMA", "STORAGE"))
            capacity.forEach { AdminUsageBar(it.title, it.used, it.limit, it.percent) }
            val r = repo
            if (r == null) AdminLine("GitHub", sh("okunamadı", "unavailable"))
            else {
                // GitHub recommends repositories stay under 1 GB and warns hard at 5 GB.
                val bytes = r.sizeKb * 1024
                AdminUsageBar(sh("GitHub deposu (önerilen 1 GB)", "GitHub repo (1 GB advised)"), bytes, 1L shl 30, (bytes * 100 / (1L shl 30)).toInt())
                r.artifacts?.let { AdminLine(sh("CI çıktıları (APK vb.)", "CI artifacts (APKs etc.)"), "$it") }
            }
        }
        AdminCard {
            AdminHeading(sh("SİSTEM SAĞLIĞI", "SYSTEM HEALTH"))
            health.forEach { h ->
                Column {
                    AdminLine(h.title, "${h.value} · ${h.status}", warn = h.status != "ok")
                    h.detail?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White.copy(alpha = .6f), fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun AdminErrorsTab() {
    var reload by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<List<AdminErrorDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(reload) {
        error = null
        gameRequestResult { rows = AdminApi.errors() }.onFailure { error = it.message.orEmpty().take(160) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { reload++ }) { Text(sh("Yenile", "Refresh"), color = Color.White) }
        AdminNotice(error)
        if (rows.isEmpty() && error == null) Text(sh("Kayıtlı hata yok.", "No errors logged."), color = Color.White)
        rows.forEach { e ->
            AdminCard {
                Row(Modifier.fillMaxWidth().clickable { open = if (open == e.id) null else e.id }) {
                    Text("${e.source} · ${e.eventType}", Modifier.weight(1f), color = if (e.severity == "critical") Color(0xFFFF8A80) else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(shortTime(e.createdAt), color = Color.White.copy(alpha = .7f), fontSize = 11.sp)
                }
                val details = e.details.orEmpty()
                if (open == e.id) SelectionContainer { Text(details, color = Color.White.copy(alpha = .85f), fontSize = 11.sp) }
                else Text(details.take(140), color = Color.White.copy(alpha = .7f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AdminAnnouncementTab() {
    var tr by remember { mutableStateOf("") }
    var en by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        gameRequestResult { AdminApi.announcement() }.onSuccess { a ->
            tr = a?.messageTr.orEmpty(); enabled = a?.enabled == true
            // A copy of the Turkish is not an English text.
            en = a?.messageEn.orEmpty().takeUnless { it.trim().equals(tr.trim(), ignoreCase = true) }.orEmpty()
        }.onFailure { notice = it.message.orEmpty().take(160) }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminCard {
            AdminHeading(sh("ÜST BANTTA DUYURU", "TOP-BAND NOTICE"))
            OutlinedTextField(tr, { tr = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Türkçe") }, minLines = 2)
            OutlinedTextField(en, { en = it.take(500) }, Modifier.fillMaxWidth(),
                label = { Text(sh("English (boş bırak: otomatik çevrilir)", "English (leave empty: auto-translated)")) }, minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sh("Yayında", "Live"), Modifier.weight(1f), color = Color.White)
                Switch(enabled, { enabled = it })
            }
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    val autoTranslate = en.isBlank()
                    gameRequestResult { AdminApi.setAnnouncement(tr.trim(), en.trim(), enabled) }
                        .onSuccess {
                            notice = if (autoTranslate) sh("Kaydedildi. İngilizcesi sunucuda çevriliyor…", "Saved. The English is being translated on the server…")
                                else sh("Kaydedildi. Oyuncular bir dakika içinde görür.", "Saved. Players see it within a minute.")
                            if (autoTranslate) {
                                // The server translation lands within seconds; show it here.
                                for (attempt in 0 until 6) {
                                    kotlinx.coroutines.delay(2_500)
                                    load()
                                    if (en.isNotBlank()) {
                                        notice = sh("Kaydedildi ve İngilizceye çevrildi. Oyuncular bir dakika içinde görür.", "Saved and translated. Players see it within a minute.")
                                        break
                                    }
                                }
                            }
                        }
                        .onFailure { notice = it.message.orEmpty().take(160) }
                    busy = false
                }
            }) { Text(sh("Kaydet", "Save")) }
            AdminNotice(notice)
        }
    }
}

@Composable
private fun AdminPlayersTab() {
    var query by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<AdminPlayerDto>>(emptyList()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<Pair<String, suspend () -> Unit>?>(null) }
    val scope = rememberCoroutineScope()
    var gift by remember { mutableStateOf<AdminPlayerDto?>(null) }
    fun search() = scope.launch {
        gameRequestResult { rows = AdminApi.search(query.trim()) }
            .onSuccess { notice = if (rows.isEmpty()) sh("Sonuç yok.", "No results.") else null }
            .onFailure { notice = it.message.orEmpty().take(160) }
    }
    fun act(label: String, action: suspend () -> Unit) { confirm = label to action }
    fun friendly(raw: String): String = when {
        "remove_admin_first" in raw -> sh("Yönetici silinemez; önce yöneticiliğini al.", "Admins cannot be deleted; remove admin first.")
        "player_owns_club" in raw -> sh("Bu oyuncunun bir kulübü var; önce kulüp kapatılmalı.", "This player owns a club; close it first.")
        "cannot_delete_self" in raw -> sh("Kendi hesabını buradan silemezsin.", "You cannot delete your own account here.")
        "admin_delete_player_v1" in raw || "PGRST202" in raw -> sh("Silme yetkisi sunucuda henüz açılmadı.", "Deletion is not enabled on the server yet.")
        "amount_out_of_range" in raw -> sh("Miktar 1 ile 100.000 arasında olmalı.", "Amount must be 1–100,000.")
        else -> raw.take(160)
    }
    // The list is there on opening: newest activity first, search narrows it.
    LaunchedEffect(Unit) { search() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it.take(60) }, Modifier.weight(1f), singleLine = true,
                label = { Text(sh("İsim veya e-posta", "Name or e-mail")) })
            Button(onClick = { search() }) { Text(sh("Ara", "Find")) }
        }
        AdminNotice(notice)
        rows.forEach { p ->
            val admin = p.isAdmin || p.userId in AdminRoster.ids
            val blocked = p.blockedUntil != null
            AdminCard {
                Text((p.displayName ?: "—") + if (admin) "  · " + sh("Yönetici", "Admin") else "", color = Color.White, fontWeight = FontWeight.Black)
                Text(p.email, color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                Text(sh("Son görülme: ", "Last seen: ") + shortTime(p.lastSeenAt) + (if (p.isVip) " · PRO" else "") +
                    (p.diamonds?.let { " · $it coin" } ?: "") +
                    (if (blocked) " · " + sh("Engelli", "Blocked") else ""), color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { act(if (p.isVip) sh("PRO kapatılsın mı?", "Turn PRO off?") else sh("PRO açılsın mı?", "Turn PRO on?")) { AdminApi.setVip(p.userId, !p.isVip) } }, Modifier.weight(1f)) {
                        Text(if (p.isVip) sh("PRO kapat", "PRO off") else sh("PRO aç", "PRO on"), fontSize = 11.sp, color = Color.White)
                    }
                    OutlinedButton(onClick = { act(if (blocked) sh("Engel kaldırılsın mı?", "Unblock?") else sh("Oyuncu engellensin mi?", "Block player?")) { AdminApi.setBlocked(p.userId, !blocked) } }, Modifier.weight(1f)) {
                        Text(if (blocked) sh("Engeli kaldır", "Unblock") else sh("Engelle", "Block"), fontSize = 11.sp, color = Color.White)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { act(if (admin) sh("Yöneticilik alınsın mı?", "Remove admin?") else sh("Yönetici yapılsın mı?", "Make admin?")) { AdminApi.setAdmin(p.userId, !admin) } }, Modifier.weight(1f)) {
                        Text(if (admin) sh("Yöneticiliği al", "Remove admin") else sh("Yönetici yap", "Make admin"), fontSize = 11.sp, color = Color.White)
                    }
                    OutlinedButton(onClick = { act(sh("1 haftalık test hükümdarı yapılsın mı?", "Make test throne owner for a week?")) { AdminApi.setChampion(p.userId, 7) } }, Modifier.weight(1f)) {
                        Text(sh("Test hükümdarı", "Test ruler"), fontSize = 11.sp, color = Color.White)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { gift = p }, Modifier.weight(1f)) {
                        Text(sh("Coin hediye et", "Gift coins"), fontSize = 11.sp, color = LobbyBrand.Gold)
                    }
                    OutlinedButton(enabled = !admin, onClick = {
                        act(sh("${p.displayName ?: p.email} KALICI olarak silinsin mi? Hesap, oyunlar ve satın alınanlar geri gelmez.",
                            "Delete ${p.displayName ?: p.email} PERMANENTLY? Account, games and items cannot be restored.")) { AdminApi.deletePlayer(p.userId) }
                    }, Modifier.weight(1f)) {
                        Text(sh("Oyuncuyu sil", "Delete player"), fontSize = 11.sp, color = if (admin) Color.White.copy(alpha = .4f) else Color(0xFFFF8A80))
                    }
                }
            }
        }
    }
    gift?.let { target ->
        var amount by remember(target.userId) { mutableStateOf("1000") }
        AlertDialog(onDismissRequest = { gift = null },
            title = { Text(sh("${target.displayName ?: target.email} için coin", "Coins for ${target.displayName ?: target.email}")) },
            text = {
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(6) }, singleLine = true,
                    label = { Text(sh("Miktar (1–100.000)", "Amount (1–100,000)")) })
            },
            confirmButton = { TextButton(onClick = {
                val value = amount.toIntOrNull() ?: 0
                gift = null
                scope.launch {
                    gameRequestResult { AdminApi.giftCoins(target.userId, value) }
                        .onSuccess { notice = sh("$value coin verildi.", "$value coins gifted."); search() }
                        .onFailure { notice = friendly(it.message.orEmpty()) }
                }
            }) { Text(sh("Ver", "Gift")) } },
            dismissButton = { TextButton(onClick = { gift = null }) { Text(sh("Vazgeç", "Cancel")) } })
    }
    confirm?.let { (label, action) ->
        AlertDialog(onDismissRequest = { confirm = null },
            title = { Text(label) },
            confirmButton = { TextButton(onClick = {
                confirm = null
                scope.launch {
                    gameRequestResult { action() }
                        .onSuccess { notice = sh("Yapıldı.", "Done."); AdminRoster.forceRefresh(); search() }
                        .onFailure { notice = friendly(it.message.orEmpty()) }
                }
            }) { Text(sh("Evet", "Yes")) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(sh("Vazgeç", "Cancel")) } })
    }
}

@Composable
private fun AdminMaintenanceTab() {
    var result by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminCard {
            AdminHeading(sh("TEMİZLİK", "CLEAN-UP"))
            Text(sh("Takılı eşleşmeleri, süresi geçmiş davetleri ve eski bekleyen odaları temizler.",
                "Clears stuck matchmaking, expired invites and old waiting rooms."), color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    gameRequestResult { result = AdminApi.maintenance() }.onFailure { result = it.message.orEmpty().take(160) }
                    busy = false
                }
            }) { Text(sh("Temizliği çalıştır", "Run clean-up")) }
            result?.let { SelectionContainer { Text(it, color = Color.White, fontSize = 11.sp) } }
        }
    }
}
