from pathlib import Path
import re

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')

def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected exactly one occurrence, got {count}: {old[:100]!r}')
    write(path, text.replace(old, new, 1))

def insert_before_once(path, marker, insertion):
    text = read(path)
    if text.count(marker) != 1:
        raise SystemExit(f'{path}: marker count != 1: {marker!r}')
    write(path, text.replace(marker, insertion + marker, 1))

def remove_balanced_call_line(path, marker):
    text = read(path)
    idx = text.find(marker)
    if idx < 0:
        raise SystemExit(f'{path}: missing marker {marker}')
    line_start = text.rfind('\n', 0, idx) + 1
    # Include directly preceding G4.6 comment lines, but nothing else.
    start = line_start
    prev_end = line_start - 1
    while prev_end > 0:
        prev_start = text.rfind('\n', 0, prev_end) + 1
        line = text[prev_start:prev_end].strip()
        if line.startswith('//') and ('G4.6' in line or 'sohbet' in line.lower() or 'chat' in line.lower()):
            start = prev_start
            prev_end = prev_start - 1
        else:
            break
    open_idx = text.find('(', idx)
    depth = 0
    end_idx = None
    in_str = False
    esc = False
    for i in range(open_idx, len(text)):
        c = text[i]
        if in_str:
            if esc:
                esc = False
            elif c == '\\':
                esc = True
            elif c == '"':
                in_str = False
            continue
        if c == '"':
            in_str = True
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                end_idx = i + 1
                break
    if end_idx is None:
        raise SystemExit(f'{path}: could not balance call')
    while end_idx < len(text) and text[end_idx] in ' \t':
        end_idx += 1
    if end_idx < len(text) and text[end_idx] == '\n':
        end_idx += 1
    write(path, text[:start] + text[end_idx:])

# ---------------------------------------------------------------------------
# Shared in-game help: one component, three game types, no navigation/state reset.
# ---------------------------------------------------------------------------
write('app/src/main/java/com/sonharf/game/GameHelpButton.kt', r'''package com.sonharf.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class GameHelpType { SIEGE, LAST_LETTER, LETTER_PATH }

@Composable
internal fun GameHelpButton(
    type: GameHelpType,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(
        onClick = { open = true },
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            Icons.Rounded.HelpOutline,
            contentDescription = sh("Nasıl oynanır?", "How to play?"),
            tint = SonHarfTheme.TextPrimary,
        )
    }
    if (!open) return

    val items = when (type) {
        GameHelpType.SIEGE -> listOf(
            sh("Amaç: kelimeler kurarak haritada alan hâkimiyeti sağla ve toplam skorda öne geç.", "Goal: build words, control territory, and lead the total score."),
            sh("Harflerini geçerli bir kelime oluşturacak şekilde haritaya yerleştir; hamle yeni bölgeler ele geçirebilir.", "Place letters to form a valid word; a move can capture new territory."),
            sh("Kelime Puanı kelimelerden kazanılır ve kalıcıdır. Bölge Puanı sahip olduğun küplerden gelir; her küp 2 puandır.", "Word Score comes from words and stays earned. Territory Score comes from owned cells; each cell is worth 2 points."),
            sh("Rakip senin alanını alırsa yalnız kaybettiğin Bölge Puanı düşer; daha önce kazandığın Kelime Puanı silinmez.", "If an opponent captures your area, only the lost Territory Score is deducted; previously earned Word Score remains."),
            sh("Harita kontrol yüzdesi alan hâkimiyetini gösterir. Birden çok kritik hücreyi zincirleme almak kuşatma baskısı yaratır.", "Map control shows territory dominance. Chained captures of critical cells create siege pressure."),
            sh("Kale, kritik bölge veya diğer özel noktalar görünüyorsa bunlar harita stratejisinin parçasıdır; herkese aynı kurallar uygulanır.", "If castles, critical zones, or other special points appear, they are shared strategic map elements with equal rules for both players."),
            sh("Maç sonunda Kelime Puanı + Bölge Puanı toplamı yüksek olan oyuncu kazanır.", "At match end, the player with the higher Word Score + Territory Score total wins."),
        )
        GameHelpType.LAST_LETTER -> listOf(
            sh("Rakibin yazdığı kelimenin son harfiyle başlayan yeni bir kelime üret.", "Create a new word that starts with the final letter of your opponent's word."),
            sh("Kelime sözlükte geçerli olmalı; maçta daha önce kullanılan kelime tekrar kullanılamaz.", "The word must be valid in the dictionary; a word already used in the match cannot be repeated."),
            sh("Hamleni süre dolmadan gönder. Tur süresi oyun ilerledikçe kısalabilir.", "Submit before the turn timer expires. The turn window may shrink as the match progresses."),
            sh("Geçerli hamleler skor kazandırır. Geçersiz hamle veya süre bitimi mevcut can/hamle kurallarına göre ceza verir.", "Valid moves score points. Invalid moves or timeouts apply the current life/turn penalty."),
            sh("Canı/hamle hakkı tükenen veya maçın bitiş koşulunda geride kalan oyuncu kaybeder; sonuç ekranda gösterilir.", "A player who exhausts the applicable lives/turn allowance, or trails at the match end condition, loses; the result is shown on screen."),
        )
        GameHelpType.LETTER_PATH -> listOf(
            sh("Amaç: bağlantılı harfleri kullanarak geçerli kelime rotaları oluşturmak ve hedefi tamamlamak.", "Goal: connect letters into valid word paths and complete the objective."),
            sh("Harfler izin verilen komşuluk/bağlantı yönleriyle birbirine bağlanır; kopuk rota geçerli hamle sayılmaz.", "Letters must follow the allowed adjacency/connection rules; a broken path is not a valid move."),
            sh("Geçerli kelimeler puan kazandırır; daha verimli ve uzun rotalar daha güçlü skor fırsatları yaratır.", "Valid words earn points; efficient and longer paths create stronger scoring opportunities."),
            sh("Rotayı ve ekrandaki hedefleri tamamladığında bölüm/oyun tamamlanır; sonuç mevcut skor kurallarına göre hesaplanır.", "Complete the route and on-screen objectives to finish; the result follows the current scoring rules."),
        )
    }

    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(sh("Nasıl Oynanır?", "How to Play"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items.forEach { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text("•", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(line, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { open = false }) {
                Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Bold)
            }
        },
    )
}
''')

# ---------------------------------------------------------------------------
# FLAG_SECURE only while a true friend-to-friend DM is selected.
# ---------------------------------------------------------------------------
write('app/src/main/java/com/sonharf/game/PrivateChatSecurity.kt', r'''package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun PrivateChatSecureEffect(enabled: Boolean) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivityOrNull() }
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (enabled) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
''')

replace_once(
    'app/src/main/java/com/sonharf/game/SocialExperience.kt',
    '    val me = backend.currentUserId()\n\n    suspend fun reloadFriends() {',
    '    val me = backend.currentUserId()\n\n    // Only the actual one-to-one friend conversation is protected. The friends list,\n    // match chat and all normal game screens remain screenshot-enabled.\n    PrivateChatSecureEffect(enabled = selected != null)\n\n    suspend fun reloadFriends() {'
)

# ---------------------------------------------------------------------------
# Admin backend client additions.
# ---------------------------------------------------------------------------
admin_append = r'''

@Serializable
data class AdminAccessDto(
    val authorized: Boolean = false,
    @SerialName("admin_role") val adminRole: String = "",
)

@Serializable
data class AdminPlayerOpsDto(
    @SerialName("user_id") val userId: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_vip") val isVip: Boolean = false,
    val diamonds: Int = 0,
    val rating: Int = 0,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("blocked_until") val blockedUntil: String? = null,
    @SerialName("is_owner_account") val isOwnerAccount: Boolean = false,
)

@Serializable
data class AdminStoreCatalogDto(
    @SerialName("product_id") val productId: String,
    @SerialName("gross_price_minor") val grossPriceMinor: Long = 0,
    val currency: String = "TRY",
    val enabled: Boolean = true,
    @SerialName("badge_tr") val badgeTr: String? = null,
    @SerialName("badge_en") val badgeEn: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 100,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AdminAuditEntryDto(
    val id: Long,
    @SerialName("created_at") val createdAt: String,
    @SerialName("admin_email") val adminEmail: String = "",
    val action: String,
    @SerialName("target_type") val targetType: String = "",
    @SerialName("target_id") val targetId: String? = null,
    @SerialName("before_data") val beforeData: String? = null,
    @SerialName("after_data") val afterData: String? = null,
    val outcome: String = "success",
    @SerialName("error_text") val errorText: String? = null,
)

@Serializable
data class AdminSystemEventDto(
    val id: Long,
    val severity: String,
    val source: String,
    @SerialName("event_type") val eventType: String,
    val details: String = "{}",
    @SerialName("created_at") val createdAt: String,
)

suspend fun OnlineGameBackend.isCurrentUserAdmin(): Boolean =
    runCatching {
        SupabaseProvider.client.postgrest.rpc("admin_access_v1").decodeSingle<AdminAccessDto>().authorized
    }.getOrDefault(false)

suspend fun OnlineGameBackend.adminSearchPlayersV2(query: String): List<AdminPlayerOpsDto> =
    SupabaseProvider.client.postgrest.rpc(
        "admin_search_players_v2",
        buildJsonObject { put("p_query", query.trim()) },
    ).decodeList()

suspend fun OnlineGameBackend.adminAdjustPlayerDiamonds(userId: String, delta: Int) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_adjust_player_diamonds_v1",
        buildJsonObject { put("p_user_id", userId); put("p_delta", delta) },
    )
}

suspend fun OnlineGameBackend.adminSetPlayerBlocked(userId: String, blocked: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_player_blocked_v1",
        buildJsonObject { put("p_user_id", userId); put("p_blocked", blocked) },
    )
}

suspend fun OnlineGameBackend.getAdminStoreCatalog(): List<AdminStoreCatalogDto> =
    SupabaseProvider.client.postgrest.rpc("admin_store_catalog_v1").decodeList()

suspend fun OnlineGameBackend.adminSetStoreEnabled(productId: String, enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_store_enabled_v1",
        buildJsonObject { put("p_product_id", productId); put("p_enabled", enabled) },
    )
}

suspend fun OnlineGameBackend.getAdminAuditV2(): List<AdminAuditEntryDto> =
    SupabaseProvider.client.postgrest.rpc("admin_audit_v2").decodeList()

suspend fun OnlineGameBackend.getAdminRecentErrors(): List<AdminSystemEventDto> =
    SupabaseProvider.client.postgrest.rpc("admin_recent_errors_v1").decodeList()
'''
path_admin = 'app/src/main/java/com/sonharf/game/data/AdminConsole.kt'
text = read(path_admin)
if 'suspend fun OnlineGameBackend.isCurrentUserAdmin()' not in text:
    text += admin_append
write(path_admin, text)

# ---------------------------------------------------------------------------
# Home: remove redundant envelope and add backend-authorized Admin icon.
# ---------------------------------------------------------------------------
home = 'app/src/main/java/com/sonharf/game/PremiumHomeV3.kt'
remove_balanced_call_line(home, 'com.sonharf.game.ui.premium.UnreadChatIcon(')
replace_once(
    home,
    '''internal fun PremiumHomeCommandDeck(\n    profile: ProfileDto?,\n    onProfile: () -> Unit,\n    onSiege: () -> Unit,\n    onSocial: () -> Unit,\n) {''',
    '''internal fun PremiumHomeCommandDeck(\n    profile: ProfileDto?,\n    onProfile: () -> Unit,\n    onSiege: () -> Unit,\n    onSocial: () -> Unit,\n    isAdmin: Boolean,\n    onAdmin: () -> Unit,\n) {'''
)
insert_before_once(
    home,
    '            IconButton(onClick = onSocial, modifier = Modifier.size(48.dp)) {',
    '''            if (isAdmin) {\n                IconButton(onClick = onAdmin, modifier = Modifier.size(48.dp)) {\n                    Icon(\n                        Icons.Rounded.AdminPanelSettings,\n                        sh("Yönetici paneli", "Admin panel"),\n                        tint = SonHarfTheme.Primary,\n                    )\n                }\n            }\n'''
)

# ---------------------------------------------------------------------------
# Main router: fail-closed admin visibility + three in-game help overlays.
# ---------------------------------------------------------------------------
app = 'app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt'
replace_once(app, 'import com.sonharf.game.data.SupabaseProvider\n', 'import com.sonharf.game.data.SupabaseProvider\nimport com.sonharf.game.data.isCurrentUserAdmin\n')
replace_once(
    app,
    '    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP\n}',
    '    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP, ADMIN\n}'
)
replace_once(
    app,
    '    var isPro by remember { mutableStateOf(false) }\n',
    '    var isPro by remember { mutableStateOf(false) }\n    var isAdmin by remember { mutableStateOf(false) }\n'
)
replace_once(
    app,
    '            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)\n',
    '            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)\n            isAdmin = backend.isCurrentUserAdmin()\n'
)
replace_once(
    app,
    '            PremiumDestination.SOCIAL, PremiumDestination.SHOP -> PremiumDestination.HOME\n',
    '            PremiumDestination.SOCIAL, PremiumDestination.SHOP, PremiumDestination.ADMIN -> PremiumDestination.HOME\n'
)
replace_once(
    app,
    '''                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },\n                    )''',
    '''                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },\n                        isAdmin = isAdmin,\n                        onAdmin = { destination = PremiumDestination.ADMIN },\n                    )'''
)
replace_once(
    app,
    '''                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()\n                    PremiumDestination.SIEGE -> WordSiegeExperienceScreen {\n                        leaveGame()\n                    }\n                    PremiumDestination.LETTER_PATH -> LetterLadderGameScreen {\n                        leaveGame()\n                    }''',
    '''                    PremiumDestination.LAST_LETTER -> Box(Modifier.fillMaxSize()) {\n                        OnlineGameScreenV6()\n                        GameHelpButton(GameHelpType.LAST_LETTER, Modifier.align(Alignment.TopEnd).padding(8.dp))\n                    }\n                    PremiumDestination.SIEGE -> Box(Modifier.fillMaxSize()) {\n                        WordSiegeExperienceScreen { leaveGame() }\n                        GameHelpButton(GameHelpType.SIEGE, Modifier.align(Alignment.TopEnd).padding(8.dp))\n                    }\n                    PremiumDestination.LETTER_PATH -> Box(Modifier.fillMaxSize()) {\n                        LetterLadderGameScreen { leaveGame() }\n                        GameHelpButton(GameHelpType.LETTER_PATH, Modifier.align(Alignment.TopEnd).padding(8.dp))\n                    }'''
)
replace_once(
    app,
    '''                    PremiumDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) {\n                        destination = PremiumDestination.PROFILE\n                    }\n                }''',
    '''                    PremiumDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) {\n                        destination = PremiumDestination.PROFILE\n                    }\n                    PremiumDestination.ADMIN -> AdminConsoleScreen { destination = PremiumDestination.HOME }\n                }'''
)
replace_once(
    app,
    '''    onSocial: () -> Unit,\n    onLastLetter: () -> Unit,\n    onLetterPath: () -> Unit,\n) {''',
    '''    onSocial: () -> Unit,\n    onLastLetter: () -> Unit,\n    onLetterPath: () -> Unit,\n    isAdmin: Boolean,\n    onAdmin: () -> Unit,\n) {'''
)
replace_once(
    app,
    '                PremiumHomeCommandDeck(profile, onProfile, onPrimary, onSocial)\n',
    '                PremiumHomeCommandDeck(profile, onProfile, onPrimary, onSocial, isAdmin, onAdmin)\n'
)

# ---------------------------------------------------------------------------
# Admin console extensions. Existing panel remains; only new sections/actions are added.
# ---------------------------------------------------------------------------
panel = 'app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt'
replace_once(
    panel,
    '''    GAMES("Oyunlar"),\n    ANNOUNCEMENTS("Duyurular"),\n    MAINTENANCE("Bakım"),\n}''',
    '''    GAMES("Oyunlar"),\n    ANNOUNCEMENTS("Duyurular"),\n    STORE("Mağaza"),\n    TESTS("Test Hesapları"),\n    SYSTEM("Sistem"),\n    SECURITY("Güvenlik"),\n    MAINTENANCE("Bakım"),\n}'''
)
replace_once(
    panel,
    '    var gameControls by remember { mutableStateOf<List<AdminGameControlDto>>(emptyList()) }\n',
    '''    var gameControls by remember { mutableStateOf<List<AdminGameControlDto>>(emptyList()) }\n    var storeCatalog by remember { mutableStateOf<List<AdminStoreCatalogDto>>(emptyList()) }\n    var auditEntries by remember { mutableStateOf<List<AdminAuditEntryDto>>(emptyList()) }\n    var recentErrors by remember { mutableStateOf<List<AdminSystemEventDto>>(emptyList()) }\n'''
)
replace_once(
    panel,
    '    var playerResults by remember { mutableStateOf<List<AdminPlayerSearchDto>>(emptyList()) }\n',
    '    var playerResults by remember { mutableStateOf<List<AdminPlayerOpsDto>>(emptyList()) }\n'
)
replace_once(
    panel,
    '''            gameControls = backend.getAdminGameControls()\n            monthlyRevenue = backend.getAdminMonthlyRevenue()''',
    '''            gameControls = backend.getAdminGameControls()\n            storeCatalog = backend.getAdminStoreCatalog()\n            auditEntries = backend.getAdminAuditV2()\n            recentErrors = backend.getAdminRecentErrors()\n            monthlyRevenue = backend.getAdminMonthlyRevenue()'''
)
text = read(panel).replace('backend.adminSearchPlayers(q)', 'backend.adminSearchPlayersV2(q)').replace('backend.adminSearchPlayers(playerSearchText)', 'backend.adminSearchPlayersV2(playerSearchText)')
write(panel, text)
replace_once(
    panel,
    '''                                onVipChange = { value ->\n                                    scope.launch {''',
    '''                                onVipChange = { value ->\n                                    scope.launch {'''
)
# Inject new callbacks after the existing AdminPlayerSearchRow onVipChange lambda by replacing its closing call block.
old_player_call_tail = '''                                        reload()\n                                        busy = false\n                                    }\n                                },\n                            )'''
new_player_call_tail = '''                                        reload()\n                                        busy = false\n                                    }\n                                },\n                                onDiamondDelta = { delta ->\n                                    scope.launch {\n                                        busy = true\n                                        runCatching { backend.adminAdjustPlayerDiamonds(player.userId, delta) }\n                                            .onSuccess { notice = "${player.displayName} elmas bakiyesi güncellendi." }\n                                            .onFailure { error = it.message ?: "Elmas bakiyesi güncellenemedi." }\n                                        playerResults = runCatching { backend.adminSearchPlayersV2(playerSearchText) }.getOrDefault(playerResults)\n                                        reload(); busy = false\n                                    }\n                                },\n                                onBlockedChange = { blocked ->\n                                    scope.launch {\n                                        busy = true\n                                        runCatching { backend.adminSetPlayerBlocked(player.userId, blocked) }\n                                            .onSuccess { notice = if (blocked) "${player.displayName} 24 saat geçici engellendi." else "${player.displayName} engeli kaldırıldı." }\n                                            .onFailure { error = it.message ?: "Hesap durumu güncellenemedi." }\n                                        playerResults = runCatching { backend.adminSearchPlayersV2(playerSearchText) }.getOrDefault(playerResults)\n                                        reload(); busy = false\n                                    }\n                                },\n                            )'''
# There can be other similar tails; target first after AdminPlayerSearchRow.
txt = read(panel)
call_start = txt.find('                            AdminPlayerSearchRow(')
if call_start < 0:
    raise SystemExit('AdminPlayerSearchRow call not found')
tail_idx = txt.find(old_player_call_tail, call_start)
if tail_idx < 0:
    raise SystemExit('AdminPlayerSearchRow call tail not found')
txt = txt[:tail_idx] + new_player_call_tail + txt[tail_idx+len(old_player_call_tail):]
write(panel, txt)

# Add new when branches immediately before MAINTENANCE.
branches = r'''
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

'''
insert_before_once(panel, '                AdminSection.MAINTENANCE -> {', branches)

# Replace player row helper with confirmation-aware operational controls.
txt = read(panel)
start = txt.find('@Composable\nprivate fun AdminPlayerSearchRow(')
end = txt.find('\n@Composable\nprivate fun AdminGameControlRow(', start)
if start < 0 or end < 0:
    raise SystemExit('player helper bounds not found')
new_player_helper = r'''@Composable
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
'''
txt = txt[:start] + new_player_helper + txt[end:]
write(panel, txt)

# Add helper composables before formatBytes.
helpers = r'''
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

'''
insert_before_once(panel, 'private fun formatBytes(value: Long): String {', helpers)

# ---------------------------------------------------------------------------
# Son Harf arena: remove visible surrender and quick-chat actions only.
# Core word input/submit and safe Android back handling remain untouched.
# Use source-level replacements around click modifiers so the action surfaces are not rendered.
# ---------------------------------------------------------------------------
premier = 'app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt'
txt = read(premier)
# Replace the header action callbacks at the callsite with inert callbacks and hide them in the header body via source surgery below.
# Locate PremierArenaHeader function body and remove balanced composable blocks containing its two clickable action markers.
def remove_enclosing_surface(text, marker):
    idx = text.find(marker)
    if idx < 0:
        return text, False
    # Find nearest preceding Surface( or Box( within 800 chars.
    candidates = [(text.rfind('Surface(', max(0, idx-1200), idx), 'Surface('), (text.rfind('Box(', max(0, idx-1200), idx), 'Box(')]
    candidates = [c for c in candidates if c[0] >= 0]
    if not candidates:
        raise SystemExit(f'no enclosing composable for {marker}')
    start, token = max(candidates, key=lambda x: x[0])
    brace = text.find('{', start, idx+400)
    if brace < 0:
        raise SystemExit(f'no opening brace for {marker}')
    depth = 0
    in_str = False
    esc = False
    end = None
    for i in range(brace, len(text)):
        c = text[i]
        if in_str:
            if esc: esc = False
            elif c == '\\': esc = True
            elif c == '"': in_str = False
            continue
        if c == '"': in_str = True
        elif c == '{': depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise SystemExit(f'unbalanced composable for {marker}')
    ls = text.rfind('\n', 0, start) + 1
    while end < len(text) and text[end] in ' \t': end += 1
    if end < len(text) and text[end] == '\n': end += 1
    return text[:ls] + text[end:], True

for marker in ['Modifier.clickable(onClick = onForfeit)', 'Modifier.clickable(onClick = onQuickChat)']:
    txt, removed = remove_enclosing_surface(txt, marker)
    if not removed:
        raise SystemExit(f'{premier}: expected visible action marker {marker}')
write(premier, txt)

# ---------------------------------------------------------------------------
# Supabase migration: real UUID bootstrap, fail-closed ops, audit, store/errors.
# ---------------------------------------------------------------------------
write('supabase/migrations/20260917130000_admin_help_privacy_package.sql', r'''begin;

-- Admin bootstrap uses the real Supabase Auth identities. It never invents UUIDs.
insert into public.admin_users(user_id, role, free_test_purchases)
select u.id, 'admin', true
from auth.users u
where lower(trim(u.email)) in ('makalega58@gmail.com','makalega68@gmail.com')
on conflict (user_id) do update set role = excluded.role;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
  select exists(
    select 1 from public.admin_users a
    where a.user_id = auth.uid() and a.role = 'admin'
  );
$$;
revoke all on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated;

create or replace function public.admin_search_players_v2(p_query text)
returns table(
  user_id uuid,
  email text,
  display_name text,
  is_vip boolean,
  diamonds integer,
  rating integer,
  last_seen_at timestamptz,
  created_at timestamptz,
  blocked_until timestamptz,
  is_owner_account boolean
)
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_q text := lower(trim(coalesce(p_query,'')));
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if length(v_q) < 2 then raise exception 'query_too_short'; end if;
  return query
  select p.id, coalesce(u.email,'')::text, p.display_name, p.is_vip, p.diamonds, p.rating,
         p.last_seen_at, p.created_at, u.banned_until,
         exists(select 1 from public.owner_game_accounts o where o.user_id=p.id and o.active)
  from public.profiles p
  join auth.users u on u.id=p.id
  where lower(coalesce(u.email,'')) like '%'||v_q||'%'
     or lower(coalesce(p.display_name,'')) like '%'||v_q||'%'
  order by p.last_seen_at desc nulls last
  limit 50;
end;
$$;
revoke all on function public.admin_search_players_v2(text) from public, anon;
grant execute on function public.admin_search_players_v2(text) to authenticated;

create or replace function public.admin_adjust_player_diamonds_v1(p_user_id uuid, p_delta integer)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_before integer; v_after integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_delta is null or p_delta = 0 or abs(p_delta) > 1000000 then raise exception 'invalid_delta'; end if;
  select diamonds into v_before from public.profiles where id=p_user_id for update;
  if v_before is null then raise exception 'user_not_found'; end if;
  v_after := greatest(0, least(10000000, v_before + p_delta));
  update public.profiles set diamonds=v_after, updated_at=now() where id=p_user_id;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'adjust_player_diamonds','profile',p_user_id::text,
         jsonb_build_object('diamonds',v_before),jsonb_build_object('diamonds',v_after,'delta',p_delta));
end;
$$;
revoke all on function public.admin_adjust_player_diamonds_v1(uuid,integer) from public, anon;
grant execute on function public.admin_adjust_player_diamonds_v1(uuid,integer) to authenticated;

create or replace function public.admin_set_player_blocked_v1(p_user_id uuid, p_blocked boolean)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_before timestamptz; v_after timestamptz;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if exists(select 1 from public.admin_users where user_id=p_user_id and role='admin') then
    raise exception 'cannot_block_admin';
  end if;
  select banned_until into v_before from auth.users where id=p_user_id for update;
  if not found then raise exception 'user_not_found'; end if;
  v_after := case when coalesce(p_blocked,false) then now()+interval '24 hours' else null end;
  update auth.users set banned_until=v_after where id=p_user_id;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_player_blocked','auth_user',p_user_id::text,
         jsonb_build_object('banned_until',v_before),jsonb_build_object('banned_until',v_after,'temporary_24h',coalesce(p_blocked,false)));
end;
$$;
revoke all on function public.admin_set_player_blocked_v1(uuid,boolean) from public, anon;
grant execute on function public.admin_set_player_blocked_v1(uuid,boolean) to authenticated;

create or replace function public.admin_store_catalog_v1()
returns table(
  product_id text,
  gross_price_minor bigint,
  currency text,
  enabled boolean,
  badge_tr text,
  badge_en text,
  sort_order integer,
  updated_at timestamptz
)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select coalesce(p.product_id,c.product_id), coalesce(p.gross_price_minor,0), coalesce(p.currency,'TRY'),
         coalesce(c.enabled,true), c.badge_tr, c.badge_en, coalesce(c.sort_order,100),
         greatest(coalesce(p.updated_at,'epoch'::timestamptz),coalesce(c.updated_at,'epoch'::timestamptz))
  from public.admin_product_catalog p
  full join public.store_catalog_config c on c.product_id=p.product_id
  order by coalesce(c.sort_order,100), coalesce(p.product_id,c.product_id);
end;
$$;
revoke all on function public.admin_store_catalog_v1() from public, anon;
grant execute on function public.admin_store_catalog_v1() to authenticated;

create or replace function public.admin_set_store_enabled_v1(p_product_id text, p_enabled boolean)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_id text := trim(coalesce(p_product_id,'')); v_before boolean;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_id='' or not exists(
      select 1 from public.admin_product_catalog where product_id=v_id
      union all select 1 from public.store_catalog_config where product_id=v_id
  ) then raise exception 'unknown_product'; end if;
  select enabled into v_before from public.store_catalog_config where product_id=v_id;
  insert into public.store_catalog_config(product_id,enabled,updated_at)
  values(v_id,coalesce(p_enabled,false),now())
  on conflict(product_id) do update set enabled=excluded.enabled,updated_at=now();
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_store_enabled','product',v_id,
         jsonb_build_object('enabled',v_before),jsonb_build_object('enabled',coalesce(p_enabled,false)));
end;
$$;
revoke all on function public.admin_set_store_enabled_v1(text,boolean) from public, anon;
grant execute on function public.admin_set_store_enabled_v1(text,boolean) to authenticated;

create or replace function public.admin_audit_v2()
returns table(
  id bigint,
  created_at timestamptz,
  admin_email text,
  action text,
  target_type text,
  target_id text,
  before_data text,
  after_data text,
  outcome text,
  error_text text
)
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select a.id,a.created_at,coalesce(u.email,'')::text,a.action,a.target_type,a.target_id,
         a.before_data::text,a.after_data::text,coalesce(a.outcome,'success'),a.error_text
  from public.admin_audit_log a
  left join auth.users u on u.id=a.admin_id
  order by a.created_at desc
  limit 100;
end;
$$;
revoke all on function public.admin_audit_v2() from public, anon;
grant execute on function public.admin_audit_v2() to authenticated;

create or replace function public.admin_recent_errors_v1()
returns table(id bigint,severity text,source text,event_type text,details text,created_at timestamptz)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select e.id,e.severity,e.source,e.event_type,e.details::text,e.created_at
  from public.system_events e
  where e.severity in ('error','critical')
  order by e.created_at desc
  limit 100;
end;
$$;
revoke all on function public.admin_recent_errors_v1() from public, anon;
grant execute on function public.admin_recent_errors_v1() to authenticated;

-- User-requested capacity bands: Normal 0-69, Warning 70-84, High 85-94, Critical 95+.
create or replace function public.admin_capacity_v1()
returns table(metric_key text,title text,status text,used_value bigint,limit_value bigint,percent_used integer,unit text,detail text,resolve_url text)
language plpgsql
security definer
set search_path = pg_catalog, public, storage, pg_temp
as $$
declare c public.admin_platform_config%rowtype; v_db bigint; v_storage bigint; v_db_pct integer; v_storage_pct integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select * into c from public.admin_platform_config where singleton=true;
  v_db := pg_database_size(current_database());
  select coalesce(sum((o.metadata->>'size')::bigint),0) into v_storage from storage.objects o where o.metadata ? 'size';
  if c.database_limit_bytes > 0 then v_db_pct := least(999,round(v_db*100.0/c.database_limit_bytes)::int); else v_db_pct := 0; end if;
  if c.storage_limit_bytes > 0 then v_storage_pct := least(999,round(v_storage*100.0/c.storage_limit_bytes)::int); else v_storage_pct := 0; end if;
  return query values
    ('supabase_database','Supabase Veritabanı',case when v_db_pct>=95 then 'critical' when v_db_pct>=85 then 'high' when v_db_pct>=70 then 'warning' else 'ok' end,
     v_db,c.database_limit_bytes,v_db_pct,'bytes','Gerçek PostgreSQL kullanılan alan; toplam limit admin platform plan yapılandırmasından gelir.','https://supabase.com/dashboard/project/bzdtftzdjtjoqhtcqtxb/observability/database'),
    ('supabase_storage','Supabase Storage',case when v_storage_pct>=95 then 'critical' when v_storage_pct>=85 then 'high' when v_storage_pct>=70 then 'warning' else 'ok' end,
     v_storage,c.storage_limit_bytes,v_storage_pct,'bytes','Gerçek storage nesne boyutu; toplam limit admin platform plan yapılandırmasından gelir.','https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'),
    ('supabase_usage','Supabase Auth / Edge / Trafik','info',0,0,0,'link','MAU, Edge Function, egress ve bağlantı kotaları SQL API üzerinden güvenilir biçimde alınamıyor.','https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'),
    ('github_actions','GitHub / Actions','info',0,0,0,'link','Private repository ayrıntıları için APK içine token gömülmez; güvenli backend yetkisi olmadan kota tahmini gösterilmez.','https://github.com/makalega68-source/son-harf/actions');
end;
$$;
revoke all on function public.admin_capacity_v1() from public, anon;
grant execute on function public.admin_capacity_v1() to authenticated;

commit;
''')

# ---------------------------------------------------------------------------
# Security/regression tests.
# ---------------------------------------------------------------------------
write('app/src/test/java/com/sonharf/game/AdminHelpPrivacyRegressionTest.kt', r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminHelpPrivacyRegressionTest {
    @Test
    fun adminEntryIsServerAuthorizedAndHomeButtonIsNotEmailHardcoded() {
        val router = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val api = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()
        assertTrue(router.contains("backend.isCurrentUserAdmin()"))
        assertTrue(api.contains("admin_access_v1"))
        assertTrue(home.contains("if (isAdmin)"))
        assertFalse(home.contains("UnreadChatIcon("))
        assertFalse((router + home).contains("makalega58@gmail.com"))
        assertFalse((router + home).contains("makalega68@gmail.com"))
    }

    @Test
    fun privateChatSecureFlagHasScopedLifecycle() {
        val secure = projectFile("app/src/main/java/com/sonharf/game/PrivateChatSecurity.kt").readText()
        val social = projectFile("app/src/main/java/com/sonharf/game/SocialExperience.kt").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MainActivity.kt").readText()
        assertTrue(secure.contains("DisposableEffect"))
        assertTrue(secure.contains("FLAG_SECURE"))
        assertTrue(secure.contains("clearFlags"))
        assertTrue(secure.contains("activity?.window"))
        assertTrue(social.contains("PrivateChatSecureEffect(enabled = selected != null)"))
        assertFalse(main.contains("FLAG_SECURE"))
    }

    @Test
    fun allThreeGamesUseOneReusableHelpDialogWithoutNavigation() {
        val help = projectFile("app/src/main/java/com/sonharf/game/GameHelpButton.kt").readText()
        val router = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        assertTrue(help.contains("enum class GameHelpType"))
        assertTrue(help.contains("SIEGE"))
        assertTrue(help.contains("LAST_LETTER"))
        assertTrue(help.contains("LETTER_PATH"))
        assertTrue(help.contains("Nasıl Oynanır?"))
        assertTrue(help.contains("How to Play"))
        assertTrue(router.contains("GameHelpButton(GameHelpType.SIEGE"))
        assertTrue(router.contains("GameHelpButton(GameHelpType.LAST_LETTER"))
        assertTrue(router.contains("GameHelpButton(GameHelpType.LETTER_PATH"))
    }

    @Test
    fun lastLetterSecondaryArenaActionsAreGoneButCoreScreenRemains() {
        val duel = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val wrapper = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        assertFalse(duel.contains("Modifier.clickable(onClick = onForfeit)"))
        assertFalse(duel.contains("Modifier.clickable(onClick = onQuickChat)"))
        assertTrue(wrapper.contains("PremierWordDuelScreen()"))
    }

    @Test
    fun migrationBootstrapsRealAdminsAndAllMutationsAreFailClosed() {
        val sql = projectFile("supabase/migrations/20260917130000_admin_help_privacy_package.sql").readText().lowercase()
        assertTrue(sql.contains("makalega58@gmail.com"))
        assertTrue(sql.contains("makalega68@gmail.com"))
        assertTrue(sql.contains("if not public.is_admin()"))
        assertTrue(sql.contains("admin_adjust_player_diamonds_v1"))
        assertTrue(sql.contains("admin_set_player_blocked_v1"))
        assertTrue(sql.contains("admin_audit_log"))
        assertTrue(sql.contains("revoke all"))
        assertFalse(sql.contains("service_role"))
        assertFalse(sql.contains("github_token"))
        assertFalse(sql.contains("admin_secret"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
''')

# Extend existing security regression to scan the new client and migration.
test_path = 'app/src/test/java/com/sonharf/game/AdminPanelSecurityRegressionTest.kt'
t = read(test_path)
replace_marker = '        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()\n'
if replace_marker in t:
    t = t.replace(replace_marker, replace_marker + '        val adminApi = projectFile("app/src/main/java/com/sonharf/game/data/AdminConsole.kt").readText()\n        val migration = projectFile("supabase/migrations/20260917130000_admin_help_privacy_package.sql").readText()\n', 1)
    t = t.replace('        val combined = (panel + "\\n" + backend).lowercase()\n', '        val combined = (panel + "\\n" + backend + "\\n" + adminApi + "\\n" + migration).lowercase()\n', 1)
    t = t.replace('        assertTrue(backend.contains("postgrest.rpc"))\n', '        assertTrue(backend.contains("postgrest.rpc"))\n        assertTrue(adminApi.contains("admin_access_v1"))\n        assertTrue(migration.contains("public.is_admin()"))\n        assertFalse(combined.contains("github_token"))\n', 1)
write(test_path, t)

# Final static assertions before the CI build.
for forbidden in ['service_role', 'service-role', 'SUPABASE_SERVICE', 'admin_secret', 'GITHUB_TOKEN']:
    combined = '\n'.join(read(p) for p in [
        'app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt',
        'app/src/main/java/com/sonharf/game/data/AdminConsole.kt',
        'app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt',
        'app/src/main/java/com/sonharf/game/PremiumHomeV3.kt',
    ])
    if forbidden.lower() in combined.lower():
        raise SystemExit(f'forbidden secret marker in Android source: {forbidden}')

print('admin/help/private-chat package patches applied successfully')
