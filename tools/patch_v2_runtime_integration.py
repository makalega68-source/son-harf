from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def patch(path, fn):
    p = ROOT / path
    before = p.read_text()
    after = fn(before)
    if after == before:
        raise SystemExit(f"no change for {path}")
    p.write_text(after)

def rep(s, old, new, label):
    if old not in s:
        raise SystemExit(f"missing anchor: {label}")
    return s.replace(old, new, 1)

# Series uses its own entitlement-scoped friend picker instead of the general PRO friend-list surface.
patch('app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt', lambda s: rep(
    s,
    'friends = runCatching { backend.getFriends() }.getOrDefault(friends)',
    'friends = runCatching { backend.getSeriesFriends() }.getOrDefault(friends)',
    'series friend picker',
))

# Store: membership and theme runtime are authoritative from new entitlement/inventory sources.
def store(s):
    s = rep(s,
        'var profile by remember { mutableStateOf<ProfileDto?>(null) }\n    var products',
        'var profile by remember { mutableStateOf<ProfileDto?>(null) }\n    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }\n    var products',
        'store entitlements state')
    s = rep(s,
        'profile = b.getProfile(id)\n            products = b.getShopItems()',
        'profile = b.getProfile(id)\n            entitlements = runCatching { b.getVipEntitlements() }.getOrNull()\n            products = b.getShopItems()',
        'store entitlements load')
    s = rep(s, 'SonHarfCosmetics.apply(equipped)', 'SonHarfCosmetics.apply(equipped, owned)', 'store cosmetics ownership')
    s = rep(s, 'onMembershipChanged(profile?.isVip == true)', 'onMembershipChanged(entitlements?.isPro == true)', 'store membership callback')
    s = rep(s, 'PremiumStoreProHero(profile?.isVip == true)', 'PremiumStoreProHero(entitlements?.isPro == true)', 'store PRO hero')
    return s
patch('app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt', store)

# PRO page: legacy profiles.is_vip is never a source of new PRO authority.
def pro(s):
    s = rep(s,
        'entitlements = if (profile?.isVip == true) runCatching { backend.getVipEntitlements() }.getOrNull() else null',
        'entitlements = runCatching { backend.getVipEntitlements() }.getOrNull()',
        'PRO entitlement load')
    s = rep(s,
        'val active = profile?.isVip == true\n    val e = entitlements',
        'val e = entitlements\n    val active = e?.isPro == true',
        'PRO active authority')
    s = s.replace(
        '"Reklamsız deneyim, premium kozmetik ve gelişmiş analiz aktif.",\n                                "Ad-free experience, premium cosmetics and advanced analysis are active.",',
        '"Reklamsız kullanım ve PRO erişimleri aktif. Ücretli kozmetikler ayrıca satın alınır.",\n                                "Ad-free use and PRO access are active. Paid cosmetics remain separate purchases.",')
    s = s.replace('sh("PRO PLANLARINI GÖR", "VIEW PRO PLANS")', 'sh("PRO LIFETIME’I GÖR", "VIEW PRO LIFETIME")')
    return s
patch('app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt', pro)

# Canonical V2 shell remains the runtime; add Series as the fourth game without importing a legacy shell.
def v2(s):
    s = rep(s,
        'import com.sonharf.game.data.SharedDictionaryService',
        'import com.sonharf.game.data.SharedDictionaryService\nimport com.sonharf.game.data.getEquippedCosmetics\nimport com.sonharf.game.data.getInventory\nimport com.sonharf.game.data.getVipEntitlements',
        'V2 premium imports')
    s = rep(s,
        'LAST_LETTER, SIEGE, LETTER_PATH,',
        'LAST_LETTER, SIEGE, LETTER_PATH, SERIES,',
        'V2 Series destination')
    s = rep(s,
        'var letterPathLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }',
        'var letterPathLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }\n    var seriesLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }\n    var seriesAccess by remember { mutableStateOf(false) }',
        'V2 Series state')
    s = rep(s,
        'backend.currentUserId()?.let { id ->\n            runCatching { backend.getEquippedCosmetics() }.getOrNull()?.let(SonHarfCosmetics::apply)\n            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)\n        }',
        'backend.currentUserId()?.let {\n            val owned = runCatching { backend.getInventory() }.getOrDefault(emptySet())\n            val equipped = runCatching { backend.getEquippedCosmetics() }.getOrNull()\n            SonHarfCosmetics.apply(equipped, owned)\n            val premium = runCatching { backend.getVipEntitlements() }.getOrNull()\n            isPro = premium?.isPro == true\n            seriesAccess = premium?.seriesGameAccess == true\n        }',
        'V2 startup authority')
    s = rep(s,
        'PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH)',
        'PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH, PremiumV2Destination.SERIES)',
        'V2 presence game set')
    s = rep(s,
        'PremiumV2Destination.SIEGE,\n            PremiumV2Destination.LETTER_PATH -> {',
        'PremiumV2Destination.SIEGE,\n            PremiumV2Destination.LETTER_PATH,\n            PremiumV2Destination.SERIES -> {',
        'V2 back handling')
    s = rep(s,
        'PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH)',
        'PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH, PremiumV2Destination.SERIES)',
        'V2 in-game set')
    s = rep(s,
        'letterPathLanguage = letterPathLanguage,\n                        onSiegeLanguage',
        'letterPathLanguage = letterPathLanguage,\n                        seriesLanguage = seriesLanguage,\n                        seriesUnlocked = seriesAccess,\n                        onSiegeLanguage',
        'V2 GameCenter Series params')
    s = rep(s,
        'onLetterPathLanguage = { letterPathLanguage = it },\n                        onSiege',
        'onLetterPathLanguage = { letterPathLanguage = it },\n                        onSeriesLanguage = { seriesLanguage = it },\n                        onSiege',
        'V2 Series language callback')
    s = rep(s,
        'onLetterPath = { openGame(PremiumV2Destination.LETTER_PATH, letterPathLanguage) },\n                    )',
        'onLetterPath = { openGame(PremiumV2Destination.LETTER_PATH, letterPathLanguage) },\n                        onSeries = { if (seriesAccess) openGame(PremiumV2Destination.SERIES, seriesLanguage) else openStore(0) },\n                    )',
        'V2 Series open callback')
    s = rep(s,
        'PremiumV2Destination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }',
        'PremiumV2Destination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }\n                    PremiumV2Destination.SERIES -> WordSiegeSeriesScreen { leaveGame(PremiumV2Destination.GAMES) }',
        'V2 Series route')
    s = rep(s,
        'letterPathLanguage: String,\n    onSiegeLanguage:',
        'letterPathLanguage: String,\n    seriesLanguage: String,\n    seriesUnlocked: Boolean,\n    onSiegeLanguage:',
        'GameCenter Series signature state')
    s = rep(s,
        'onLetterPathLanguage: (String) -> Unit,\n    onSiege:',
        'onLetterPathLanguage: (String) -> Unit,\n    onSeriesLanguage: (String) -> Unit,\n    onSiege:',
        'GameCenter Series language signature')
    s = rep(s,
        'onLetterPath: () -> Unit,\n) {',
        'onLetterPath: () -> Unit,\n    onSeries: () -> Unit,\n) {',
        'GameCenter Series callback signature')

    # Scope the card search to PremiumV2GameCenter. The same Harf Yolu title also exists
    # in the home secondary-modes section; searching globally inserted the Series item
    # outside LazyColumn and caused the Kotlin compile failure in run 35440498780.
    game_center = s.find('private fun PremiumV2GameCenter(')
    if game_center < 0: raise SystemExit('missing PremiumV2GameCenter')
    marker = 'title = sh("HARF YOLU", "LETTER PATH")'
    m = s.find(marker, game_center)
    if m < 0: raise SystemExit('missing GameCenter Harf Yolu card')
    close = s.find('\n        }\n    }\n}', m)
    if close < 0: raise SystemExit('cannot locate GameCenter close')
    insert = '''\n        item {\n            PremiumV2GameCard(\n                icon = if (seriesUnlocked) Icons.Rounded.Timer else Icons.Rounded.Lock,\n                title = sh("SERİ OYUN", "SERIES GAME"),\n                subtitle = if (seriesUnlocked) sh("3 / 5 / 10 dakikalık premium hızlı mod", "Premium fast mode with 3 / 5 / 10 minute turns") else sh("Premium mod • satın al veya PRO ile aç", "Premium mode • buy it or unlock with PRO"),\n                language = seriesLanguage,\n                onLanguageChange = onSeriesLanguage,\n                accent = SonHarfTheme.ActionOrange,\n                onClick = onSeries,\n            )\n        }'''
    s = s[:close] + insert + s[close:]
    return s
patch('app/src/main/java/com/sonharf/game/PremiumCanvaAppV2.kt', v2)

print('V2 runtime integration patch applied')
