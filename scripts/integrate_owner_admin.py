from pathlib import Path

path = Path('app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt')
text = path.read_text()

def replace_once(old: str, new: str, label: str):
    global text
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'Patch target not found: {label}')
    text = text.replace(old, new, 1)

replace_once(
    'import com.sonharf.game.data.SupabaseProvider\n',
    'import com.sonharf.game.data.SupabaseProvider\nimport com.sonharf.game.data.getAdminDashboard\n',
    'admin extension import',
)
replace_once(
    'HOME, PLAY, GAME, BIL_BAKALIM, PROFILE, SHOP, HUB, LEAGUE, PROFILE_FULL, SHOP_FULL',
    'HOME, PLAY, GAME, BIL_BAKALIM, ADMIN, PROFILE, SHOP, HUB, LEAGUE, PROFILE_FULL, SHOP_FULL',
    'admin enum',
)
replace_once(
    '    LaunchedEffect(lobbyRequest) {\n        if (authenticated && lobbyRequest > 0) {\n            gameKey += 1\n            screen = ClassicScreen.GAME\n        }\n    }\n',
    '    LaunchedEffect(lobbyRequest) {\n        if (authenticated && lobbyRequest > 0) {\n            gameKey += 1\n            screen = ClassicScreen.GAME\n        }\n    }\n    LaunchedEffect(screen, gameKey) {\n        if (authenticated && screen == ClassicScreen.GAME) {\n            runCatching { backend?.logEvent("son_harf_open") }\n        }\n    }\n',
    'son harf analytics',
)
replace_once(
    '                        onBilBakalim = { screen = ClassicScreen.BIL_BAKALIM },\n                        onHub = { screen = ClassicScreen.HUB },',
    '                        onBilBakalim = { screen = ClassicScreen.BIL_BAKALIM },\n                        onAdmin = { screen = ClassicScreen.ADMIN },\n                        onHub = { screen = ClassicScreen.HUB },',
    'home admin callback',
)
replace_once(
    '                    ClassicScreen.BIL_BAKALIM -> TrackedBilBakalimStandaloneScreen { screen = ClassicScreen.HOME }\n                    ClassicScreen.HUB -> MetaHubScreen()',
    '                    ClassicScreen.BIL_BAKALIM -> TrackedBilBakalimStandaloneScreen { screen = ClassicScreen.HOME }\n                    ClassicScreen.ADMIN -> AdminConsoleScreen { screen = ClassicScreen.HOME }\n                    ClassicScreen.HUB -> MetaHubScreen()',
    'admin route',
)
replace_once(
    '    onBilBakalim: () -> Unit,\n    onHub: () -> Unit,',
    '    onBilBakalim: () -> Unit,\n    onAdmin: () -> Unit,\n    onHub: () -> Unit,',
    'home signature',
)
replace_once(
    '    var dailyMessage by remember { mutableStateOf("") }\n',
    '    var dailyMessage by remember { mutableStateOf("") }\n    var isAdmin by remember { mutableStateOf(false) }\n',
    'admin state',
)
replace_once(
    '        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()\n',
    '        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()\n        isAdmin = if (backend == null) false else runCatching { backend.getAdminDashboard(); true }.getOrDefault(false)\n',
    'admin access detection',
)
replace_once(
    '        item { ClassicHeader(profile, growth, onProfile) }',
    '        item { ClassicHeader(profile, growth, onProfile, isAdmin, onAdmin) }',
    'header call',
)
replace_once(
    'private fun ClassicHeader(profile: ProfileDto?, growth: GrowthDashboardDto?, onProfile: () -> Unit) {',
    'private fun ClassicHeader(profile: ProfileDto?, growth: GrowthDashboardDto?, onProfile: () -> Unit, isAdmin: Boolean, onAdmin: () -> Unit) {',
    'header signature',
)
replace_once(
    '        Surface(shape = CircleShape, color = ClassicPanel, modifier = Modifier.size(38.dp)) {\n            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Notifications, null, tint = ClassicCream, modifier = Modifier.size(20.dp)) }\n        }',
    '        Surface(onClick = if (isAdmin) onAdmin else ({}), shape = CircleShape, color = if (isAdmin) ClassicGold.copy(alpha=.18f) else ClassicPanel, modifier = Modifier.size(38.dp), border = if (isAdmin) BorderStroke(1.dp, ClassicGold) else null) {\n            Box(contentAlignment = Alignment.Center) {\n                Icon(if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Notifications, null, tint = if (isAdmin) ClassicGold else ClassicCream, modifier = Modifier.size(20.dp))\n            }\n        }',
    'header admin entry',
)

path.write_text(text)
print('Owner admin console integrated into ClassicPremiumApp.kt')

admin_path = Path('app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt')
admin = admin_path.read_text()
section_marker = '            item { AdminSectionTitle("ÜCRETSİZ TEST PAKETLERİ", Icons.Rounded.CardGiftcard) }'
if section_marker not in admin:
    needle = '            item { Spacer(Modifier.height(24.dp)) }\n'
    if needle not in admin:
        raise SystemExit('Patch target not found: free test product section')
    section = '''            item { AdminSectionTitle("ÜCRETSİZ TEST PAKETLERİ", Icons.Rounded.CardGiftcard) }
            item {
                AdminWideCard {
                    Text("Bu paketler yalnızca yönetici hesabına test verisi verir; Google Play satın alımı ve gerçek gelir kaydı oluşturmaz.", color = AdminMuted, fontSize = 10.sp)
                    listOf(
                        "vip_monthly" to "VIP Aylık Test",
                        "vip_yearly" to "VIP Yıllık Test",
                        "coins_500" to "+500 Elmas Test",
                        "coins_1500" to "+1500 Elmas Test",
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

'''
    admin = admin.replace(needle, section + needle, 1)

old_money = '''private fun parseMoneyMinor(raw: String): Long? {
    val normalized = raw.trim().replace(".", "").replace(',', '.')
    return normalized.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
}
'''
new_money = '''private fun parseMoneyMinor(raw: String): Long? {
    val cleaned = raw.trim()
    val normalized = if (cleaned.contains(',')) cleaned.replace(".", "").replace(',', '.') else cleaned
    return normalized.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
}
'''
if new_money not in admin:
    if old_money not in admin:
        raise SystemExit('Patch target not found: money parser')
    admin = admin.replace(old_money, new_money, 1)

admin_path.write_text(admin)
print('Owner free test grants integrated into AdminConsoleScreen.kt')
