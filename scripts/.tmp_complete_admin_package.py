from pathlib import Path

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')

def replace_once(path, old, new):
    text = read(path)
    n = text.count(old)
    if n != 1:
        raise SystemExit(f'{path}: expected 1 occurrence, got {n}: {old[:120]!r}')
    write(path, text.replace(old, new, 1))

# ---------------------------------------------------------------------------
# Build metadata: real CI branch/SHA when built in GitHub Actions. No token.
# ---------------------------------------------------------------------------
build = 'app/build.gradle.kts'
replace_once(
    build,
    '''        val admobAppId = adMobAppIdProvider.get()\n        val rewardedAdUnitId = rewardedAdUnitIdProvider.get()\n        val bannerAdUnitId = bannerAdUnitIdProvider.get()\n        buildConfigField("String", "SUPABASE_URL", "\\\"$supabaseUrl\\\"")''',
    '''        val admobAppId = adMobAppIdProvider.get()\n        val rewardedAdUnitId = rewardedAdUnitIdProvider.get()\n        val bannerAdUnitId = bannerAdUnitIdProvider.get()\n        val gitCommitSha = providers.environmentVariable("GITHUB_SHA").orElse("unknown").get()\n        val gitBranch = providers.environmentVariable("GITHUB_REF_NAME").orElse("unknown").get()\n        val ciBuild = providers.environmentVariable("GITHUB_ACTIONS").orElse("false").get()\n        buildConfigField("String", "GIT_COMMIT_SHA", "\\\"$gitCommitSha\\\"")\n        buildConfigField("String", "GIT_BRANCH", "\\\"$gitBranch\\\"")\n        buildConfigField("String", "CI_BUILD", "\\\"$ciBuild\\\"")\n        buildConfigField("String", "SUPABASE_URL", "\\\"$supabaseUrl\\\"")'''
)

# ---------------------------------------------------------------------------
# Admin data contracts + RPC client.
# ---------------------------------------------------------------------------
api = 'app/src/main/java/com/sonharf/game/data/AdminConsole.kt'
replace_once(
    api,
    '''@Serializable\ndata class AdminAnnouncementDto(\n    val message: String = "",\n    val enabled: Boolean = false,\n)''',
    '''@Serializable\ndata class AdminAnnouncementDto(\n    @SerialName("message_tr") val messageTr: String = "",\n    @SerialName("message_en") val messageEn: String = "",\n    val enabled: Boolean = false,\n    val maintenance: Boolean = false,\n    @SerialName("updated_at") val updatedAt: String? = null,\n)'''
)
replace_once(
    api,
    '''suspend fun OnlineGameBackend.adminSetGameControl(key: String, enabled: Boolean) {\n    SupabaseProvider.client.postgrest.rpc(\n        "admin_set_config",\n        buildJsonObject {\n            put("p_key", key)\n            put("p_value", enabled)\n        },\n    )\n}''',
    '''suspend fun OnlineGameBackend.adminSetGameControl(key: String, enabled: Boolean) {\n    SupabaseProvider.client.postgrest.rpc(\n        "admin_set_game_control_v1",\n        buildJsonObject {\n            put("p_key", key)\n            put("p_enabled", enabled)\n        },\n    )\n}'''
)
replace_once(
    api,
    '''suspend fun OnlineGameBackend.getAdminAnnouncement(): AdminAnnouncementDto =\n    SupabaseProvider.client.postgrest.rpc("admin_get_announcement_v1").decodeSingle()\n\nsuspend fun OnlineGameBackend.adminSetAnnouncement(message: String, enabled: Boolean) {\n    SupabaseProvider.client.postgrest.rpc(\n        "admin_set_announcement_v1",\n        buildJsonObject { put("p_message", message.take(500)); put("p_enabled", enabled) },\n    )\n}''',
    '''suspend fun OnlineGameBackend.getAdminAnnouncement(): AdminAnnouncementDto =\n    SupabaseProvider.client.postgrest.rpc("admin_get_announcement_v2").decodeSingle()\n\nsuspend fun OnlineGameBackend.adminSetAnnouncement(\n    messageTr: String,\n    messageEn: String,\n    enabled: Boolean,\n    maintenance: Boolean,\n) {\n    SupabaseProvider.client.postgrest.rpc(\n        "admin_set_announcement_v2",\n        buildJsonObject {\n            put("p_message_tr", messageTr.take(500))\n            put("p_message_en", messageEn.take(500))\n            put("p_enabled", enabled)\n            put("p_maintenance", maintenance)\n        },\n    )\n}'''
)
append = r'''

@Serializable
data class AdminShopItemDto(
    @SerialName("item_id") val itemId: String,
    val kind: String,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("diamond_price") val diamondPrice: Int = 0,
    @SerialName("vip_only") val vipOnly: Boolean = false,
    val active: Boolean = true,
    val rarity: String = "STANDARD",
    @SerialName("updated_hint") val updatedHint: String = "",
)

@Serializable
data class AdminTestInventoryDto(
    @SerialName("item_id") val itemId: String,
    val kind: String,
    @SerialName("name_tr") val nameTr: String,
    val quantity: Int = 1,
    @SerialName("is_equipped") val isEquipped: Boolean = false,
    @SerialName("acquired_at") val acquiredAt: String? = null,
)

suspend fun OnlineGameBackend.getAdminShopItems(): List<AdminShopItemDto> =
    SupabaseProvider.client.postgrest.rpc("admin_shop_items_v1").decodeList()

suspend fun OnlineGameBackend.adminSetShopItem(itemId: String, active: Boolean, diamondPrice: Int) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_shop_item_v1",
        buildJsonObject {
            put("p_item_id", itemId)
            put("p_active", active)
            put("p_diamond_price", diamondPrice)
        },
    )
}

suspend fun OnlineGameBackend.getAdminTestInventory(userId: String): List<AdminTestInventoryDto> =
    SupabaseProvider.client.postgrest.rpc(
        "admin_test_inventory_v1",
        buildJsonObject { put("p_user_id", userId) },
    ).decodeList()

suspend fun OnlineGameBackend.adminGrantTestItem(userId: String, itemId: String) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_grant_test_item_v1",
        buildJsonObject { put("p_user_id", userId); put("p_item_id", itemId) },
    )
}
'''
text = read(api)
if 'data class AdminShopItemDto' not in text:
    text += append
write(api, text)

# ---------------------------------------------------------------------------
# Admin screen: bilingual announcement, real shop items, test inventory,
# grouped game flags + confirmations, and CI version metadata.
# ---------------------------------------------------------------------------
panel = 'app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt'
replace_once(
    panel,
    '''    var storeCatalog by remember { mutableStateOf<List<AdminStoreCatalogDto>>(emptyList()) }\n    var auditEntries by remember { mutableStateOf<List<AdminAuditEntryDto>>(emptyList()) }''',
    '''    var storeCatalog by remember { mutableStateOf<List<AdminStoreCatalogDto>>(emptyList()) }\n    var shopCatalog by remember { mutableStateOf<List<AdminShopItemDto>>(emptyList()) }\n    var auditEntries by remember { mutableStateOf<List<AdminAuditEntryDto>>(emptyList()) }'''
)
replace_once(
    panel,
    '''    var announcement by remember { mutableStateOf(AdminAnnouncementDto()) }\n    var announcementText by remember { mutableStateOf("") }\n    var announcementEnabled by remember { mutableStateOf(false) }''',
    '''    var announcement by remember { mutableStateOf(AdminAnnouncementDto()) }\n    var announcementTr by remember { mutableStateOf("") }\n    var announcementEn by remember { mutableStateOf("") }\n    var announcementEnabled by remember { mutableStateOf(false) }\n    var announcementMaintenance by remember { mutableStateOf(false) }\n    var selectedTestAccount by remember { mutableStateOf<AdminOwnerAccountDto?>(null) }\n    var testInventory by remember { mutableStateOf<List<AdminTestInventoryDto>>(emptyList()) }\n    var testItemId by remember { mutableStateOf("") }'''
)
replace_once(
    panel,
    '''            gameControls = backend.getAdminGameControls()\n            storeCatalog = backend.getAdminStoreCatalog()\n            auditEntries = backend.getAdminAuditV2()''',
    '''            gameControls = backend.getAdminGameControls()\n            storeCatalog = backend.getAdminStoreCatalog()\n            shopCatalog = backend.getAdminShopItems()\n            auditEntries = backend.getAdminAuditV2()'''
)
replace_once(
    panel,
    '''            announcement = backend.getAdminAnnouncement()\n            announcementText = announcement.message\n            announcementEnabled = announcement.enabled''',
    '''            announcement = backend.getAdminAnnouncement()\n            announcementTr = announcement.messageTr\n            announcementEn = announcement.messageEn\n            announcementEnabled = announcement.enabled\n            announcementMaintenance = announcement.maintenance'''
)

old_games = r'''                    if (gameControls.isEmpty()) {
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
'''
new_games = r'''                    if (gameControls.isEmpty()) {
                        item { AdminEmpty("Oyun kontrol bilgileri alınamadı.") }
                    } else {
                        item { AdminSectionTitle("KELİME KUŞATMASI", Icons.Rounded.GridOn) }
                        items(gameControls.filter { it.configKey.startsWith("word_siege_") }, key = { it.configKey }) { control ->
                            AdminGameControlRow(control, !busy) { value ->
                                scope.launch {
                                    busy = true
                                    runCatching { backend.adminSetGameControl(control.configKey, value) }
                                        .onSuccess { notice = "${control.title} ayarı güncellendi." }
                                        .onFailure { error = it.message ?: "Ayar değiştirilemedi." }
                                    reload(); busy = false
                                }
                            }
                        }
                        item {
                            Text("Kelime Kuşatması bot fallback motoru henüz bulunmadığı için sahte bir bot anahtarı gösterilmez.", color = AdminMuted, fontSize = 10.sp)
                        }
                        item { AdminSectionTitle("SON HARF", Icons.Rounded.Spellcheck) }
                        items(gameControls.filter { it.configKey.startsWith("son_harf_") }, key = { it.configKey }) { control ->
                            AdminGameControlRow(control, !busy) { value ->
                                scope.launch {
                                    busy = true
                                    runCatching { backend.adminSetGameControl(control.configKey, value) }
                                        .onSuccess { notice = "${control.title} ayarı güncellendi." }
                                        .onFailure { error = it.message ?: "Ayar değiştirilemedi." }
                                    reload(); busy = false
                                }
                            }
                        }
                        item { AdminSectionTitle("SİSTEM", Icons.Rounded.Tune) }
                        items(gameControls.filterNot { it.configKey.startsWith("word_siege_") || it.configKey.startsWith("son_harf_") }, key = { it.configKey }) { control ->
                            AdminGameControlRow(control, !busy) { value ->
                                scope.launch {
                                    busy = true
                                    runCatching { backend.adminSetGameControl(control.configKey, value) }
                                        .onSuccess { notice = "${control.title} ayarı güncellendi." }
                                        .onFailure { error = it.message ?: "Ayar değiştirilemedi." }
                                    reload(); busy = false
                                }
                            }
                        }
                    }
'''
replace_once(panel, old_games, new_games)

old_ann = r'''                            OutlinedTextField(
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
                            }'''
new_ann = r'''                            OutlinedTextField(
                                announcementTr,
                                { announcementTr = it.take(500) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Türkçe duyuru") },
                                minLines = 3,
                                maxLines = 6,
                            )
                            OutlinedTextField(
                                announcementEn,
                                { announcementEn = it.take(500) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("English announcement") },
                                minLines = 3,
                                maxLines = 6,
                            )
                            AdminToggleRow("Yayında", "Duyuruyu oyunculara göster.", announcementEnabled, !busy) { announcementEnabled = it }
                            AdminToggleRow("Bakım duyurusu", "Bakım mesajı olarak işaretle; bakım modu ayrı güvenli anahtardan yönetilir.", announcementMaintenance, !busy) { announcementMaintenance = it }
                            Text("Son güncelleme: ${announcement.updatedAt ?: "Bilgi yok"}", color = AdminMuted, fontSize = 9.sp)
                            Button(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        runCatching {
                                            backend.adminSetAnnouncement(announcementTr.trim(), announcementEn.trim(), announcementEnabled, announcementMaintenance)
                                        }.onSuccess {
                                            notice = "Türkçe/İngilizce duyuru güncellendi."
                                        }.onFailure {
                                            error = it.message ?: "Duyuru güncellenemedi."
                                        }
                                        reload(); busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("DUYURUYU KAYDET") }'''
replace_once(panel, old_ann, new_ann)

# Store: append the real cosmetic catalog after Play-product catalog.
marker = '''                    if (storeItems.isNotEmpty()) {\n                        item { AdminSectionTitle("EDİNİM ÖZETİ", Icons.Rounded.Inventory2) }'''
insert = '''                    item { AdminSectionTitle("OYUN İÇİ KOZMETİK KATALOG", Icons.Rounded.Palette) }\n                    if (shopCatalog.isEmpty()) item { AdminEmpty("Kozmetik mağaza kataloğu bilgisi alınamadı.") }\n                    else items(shopCatalog, key = { it.itemId }) { item ->\n                        AdminShopItemRow(\n                            item = item,\n                            enabled = !busy,\n                            onSave = { active, price ->\n                                scope.launch {\n                                    busy = true\n                                    runCatching { backend.adminSetShopItem(item.itemId, active, price) }\n                                        .onSuccess { notice = "${item.nameTr} mağaza bilgisi güncellendi." }\n                                        .onFailure { error = it.message ?: "Mağaza ürünü güncellenemedi." }\n                                    reload(); busy = false\n                                }\n                            },\n                        )\n                    }\n\n'''
replace_once(panel, marker, insert + marker)

old_tests_tail = '''                    if (ownerAccounts.size < 3) item { AdminEmpty("Şu anda backend'de ${ownerAccounts.size} gerçek test/özel hesap tanımlı. Eksik hesaplar için sahte UUID oluşturulmadı.") }\n'''
new_tests_tail = '''                    if (ownerAccounts.size < 3) item { AdminEmpty("Şu anda backend'de ${ownerAccounts.size} gerçek test/özel hesap tanımlı. Eksik hesaplar için sahte UUID oluşturulmadı.") }\n                    item {\n                        AdminWideCard {\n                            Text("Envanter / test kozmetiği", color = AdminText, fontWeight = FontWeight.Black)\n                            Text("Yalnız yukarıdaki gerçek ve aktif özel/test hesapları için çalışır. Rating ve lig puanına dokunmaz.", color = AdminMuted, fontSize = 10.sp)\n                            ownerAccounts.take(3).forEach { account ->\n                                OutlinedButton(onClick = {\n                                    selectedTestAccount = account\n                                    scope.launch {\n                                        testInventory = runCatching { backend.getAdminTestInventory(account.userId) }.getOrDefault(emptyList())\n                                    }\n                                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {\n                                    Text("${account.displayName} ENVANTERİ")\n                                }\n                            }\n                            if (selectedTestAccount != null) {\n                                Text("Seçili: ${selectedTestAccount?.displayName}", color = AdminBlue, fontWeight = FontWeight.Bold)\n                                if (testInventory.isEmpty()) Text("Envanter boş veya bilgi alınamadı.", color = AdminMuted, fontSize = 10.sp)\n                                testInventory.take(20).forEach { inv ->\n                                    Text("• ${inv.nameTr} • ${inv.kind} • x${inv.quantity}${if (inv.isEquipped) " • takılı" else ""}", color = AdminMuted, fontSize = 10.sp)\n                                }\n                                OutlinedTextField(testItemId, { testItemId = it.trim().take(80) }, modifier = Modifier.fillMaxWidth(), label = { Text("Aktif kozmetik item_id") }, singleLine = true)\n                                Button(onClick = {\n                                    val account = selectedTestAccount ?: return@Button\n                                    val itemId = testItemId.trim()\n                                    if (itemId.isBlank()) { notice = "Geçerli item_id gir."; return@Button }\n                                    scope.launch {\n                                        busy = true\n                                        runCatching { backend.adminGrantTestItem(account.userId, itemId) }\n                                            .onSuccess {\n                                                notice = "$itemId test kozmetiği ${account.displayName} hesabına verildi."\n                                                testInventory = backend.getAdminTestInventory(account.userId)\n                                                testItemId = ""\n                                            }\n                                            .onFailure { error = it.message ?: "Test ürünü verilemedi." }\n                                        busy = false\n                                    }\n                                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("TEST KOZMETİĞİ VER") }\n                            }\n                        }\n                    }\n'''
replace_once(panel, old_tests_tail, new_tests_tail)

replace_once(
    panel,
    '''                            AdminInlineValue("Build type", BuildConfig.BUILD_TYPE)\n                            Text("Git commit SHA/tarihi bu APK'nin BuildConfig'ine güvenli biçimde enjekte edilmediği için tahmin gösterilmez.", color = AdminMuted, fontSize = 10.sp)''',
    '''                            AdminInlineValue("Build type", BuildConfig.BUILD_TYPE)\n                            AdminInlineValue("Git branch", BuildConfig.GIT_BRANCH)\n                            AdminInlineValue("Git SHA", BuildConfig.GIT_COMMIT_SHA.take(12))\n                            AdminInlineValue("CI build", if (BuildConfig.CI_BUILD == "true") "GitHub Actions" else "Yerel / bilinmiyor")\n                            Text("Commit tarihi/mesajı bu APK'de güvenilir metadata olarak bulunmuyorsa tahmin gösterilmez.", color = AdminMuted, fontSize = 10.sp)'''
)
replace_once(
    panel,
    '''                            Text("GitHub token APK içine gömülmedi.", color = AdminText, fontWeight = FontWeight.Bold)\n                            Text("Private repository branch/commit/Actions kota verisi için güvenli backend entegrasyonu gerekiyor. Bu bilgi mevcut backend üzerinden alınamadığında tahmin üretilmez.", color = AdminMuted, fontSize = 10.sp)\n                            Text("Durum: Ek GitHub yetkisi / güvenli backend gerekiyor.", color = AdminBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)''',
    '''                            Text("GitHub token APK içine gömülmedi.", color = AdminText, fontWeight = FontWeight.Bold)\n                            AdminInlineValue("Aktif build branch", BuildConfig.GIT_BRANCH)\n                            AdminInlineValue("Build commit", BuildConfig.GIT_COMMIT_SHA.take(12))\n                            Text("Repository boyutu, LFS, Actions artifact/cache ve Packages kotaları bu istemciye güvenli ve yetkili bir GitHub backend entegrasyonu olmadan gösterilmez.", color = AdminMuted, fontSize = 10.sp)\n                            Text("Eksik GitHub kota bilgileri için tahmin üretilmiyor.", color = AdminBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)'''
)

# Confirmation for all remote game-control changes.
txt = read(panel)
start = txt.find('@Composable\nprivate fun AdminGameControlRow(')
end = txt.find('\n@Composable\nprivate fun AdminCapacityCompact(', start)
if start < 0 or end < 0:
    raise SystemExit('AdminGameControlRow bounds not found')
new_game_row = r'''@Composable
private fun AdminGameControlRow(
    control: AdminGameControlDto,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    var pending by remember(control.configKey) { mutableStateOf<Boolean?>(null) }
    val isCritical = control.configKey == "maintenance_mode" || control.configKey.endsWith("_enabled")
    AdminWideCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(control.title, color = AdminText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(control.detail, color = AdminMuted, fontSize = 10.sp)
            }
            Switch(
                checked = control.enabled,
                onCheckedChange = { pending = it },
                enabled = enabled,
                colors = if (control.configKey == "maintenance_mode") {
                    SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AdminRed)
                } else SwitchDefaults.colors()
            )
        }
        if (control.configKey == "maintenance_mode" && control.enabled) {
            Text("BAKIM MODU AÇIK", color = AdminRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
    pending?.let { value ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Ayar değişikliğini onayla") },
            text = { Text("${control.title}: ${if (value) "AÇIK" else "KAPALI"}. ${if (isCritical) "Bu kritik operasyon değişikliği server-side audit log'a yazılır." else "İşlem audit log'a yazılır."}") },
            confirmButton = { Button(onClick = { pending = null; onChange(value) }) { Text("ONAYLA") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("VAZGEÇ") } },
        )
    }
}
'''
txt = txt[:start] + new_game_row + txt[end:]
write(panel, txt)

# New shop row helper before AdminInlineValue.
marker = '@Composable\nprivate fun AdminInlineValue(label: String, value: String) {'
helper = r'''@Composable
private fun AdminShopItemRow(
    item: AdminShopItemDto,
    enabled: Boolean,
    onSave: (Boolean, Int) -> Unit,
) {
    var active by remember(item.itemId, item.active) { mutableStateOf(item.active) }
    var priceText by remember(item.itemId, item.diamondPrice) { mutableStateOf(item.diamondPrice.toString()) }
    var confirm by remember(item.itemId) { mutableStateOf(false) }
    AdminWideCard {
        Text(item.nameTr, color = AdminText, fontWeight = FontWeight.Black)
        Text("${item.nameEn} • ${item.kind} • ${item.rarity}${if (item.vipOnly) " • VIP" else ""}", color = AdminMuted, fontSize = 9.sp)
        AdminToggleRow("Yayında", "Kozmetik ürün mağazada görünür.", active, enabled) { active = it }
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it.filter(Char::isDigit).take(7) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Elmas fiyatı") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Button(onClick = { confirm = true }, enabled = enabled && priceText.toIntOrNull() != null, modifier = Modifier.fillMaxWidth()) { Text("DEĞİŞİKLİĞİ KAYDET") }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Mağaza değişikliğini onayla") },
            text = { Text("${item.nameTr}: ${priceText.toIntOrNull() ?: item.diamondPrice} elmas • ${if (active) "yayında" else "pasif"}. Rating/lig ve oyun gücü etkilenmez.") },
            confirmButton = { Button(onClick = { confirm = false; onSave(active, priceText.toIntOrNull() ?: item.diamondPrice) }) { Text("ONAYLA") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("VAZGEÇ") } },
        )
    }
}

'''
text = read(panel)
if marker not in text:
    raise SystemExit('AdminInlineValue marker missing')
write(panel, text.replace(marker, helper + marker, 1))

# ---------------------------------------------------------------------------
# Regression tests for the completion pass.
# ---------------------------------------------------------------------------
write('app/src/test/java/com/sonharf/game/AdminOperationsCompletionRegressionTest.kt', r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminOperationsCompletionRegressionTest {
    @Test
    fun gameControlsAreAllowListedAndServerEnforced() {
        val api = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()
        val sql = projectFile("supabase/migrations/20260917140000_admin_operations_completion.sql").readText()
        assertTrue(api.contains("admin_set_game_control_v1"))
        assertTrue(sql.contains("son_harf_bot_fallback_enabled"))
        assertTrue(sql.contains("word_siege_enabled"))
        assertTrue(sql.contains("son_harf_enabled"))
        assertTrue(sql.contains("unsupported_game_control"))
        assertTrue(sql.contains("select public.find_or_create_word_siege_game_v2"))
    }

    @Test
    fun announcementsAreBilingualAndMaintenanceAware() {
        val panel = projectFile("app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt").readText()
        val api = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()
        assertTrue(panel.contains("Türkçe duyuru"))
        assertTrue(panel.contains("English announcement"))
        assertTrue(panel.contains("Bakım duyurusu"))
        assertTrue(api.contains("admin_get_announcement_v2"))
        assertTrue(api.contains("admin_set_announcement_v2"))
    }

    @Test
    fun storeAndTestToolsRemainCosmeticAndRatingSafe() {
        val sql = projectFile("supabase/migrations/20260917140000_admin_operations_completion.sql").readText().lowercase()
        assertTrue(sql.contains("admin_set_shop_item_v1"))
        assertTrue(sql.contains("admin_grant_test_item_v1"))
        assertTrue(sql.contains("non_cosmetic_item_blocked"))
        assertTrue(sql.contains("test_account_required"))
        assertFalse(sql.contains("update public.profiles set rating"))
    }

    @Test
    fun buildMetadataContainsNoGithubToken() {
        val gradle = projectFile("app/build.gradle.kts").readText()
        val panel = projectFile("app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt").readText()
        assertTrue(gradle.contains("GIT_COMMIT_SHA"))
        assertTrue(gradle.contains("GIT_BRANCH"))
        assertTrue(panel.contains("BuildConfig.GIT_COMMIT_SHA"))
        assertFalse((gradle + panel).lowercase().contains("github_token"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
''')

print('admin operations completion patch applied')
