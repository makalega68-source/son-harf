from pathlib import Path


def replace_exact(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new), encoding="utf-8")


replace_exact(
    "app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt",
    '''                                        repeat(16) {
                                            delay(750)
                                            val next = runCatching { backend.findPremierActiveRoom() }.getOrNull()
                                            if (next != null && next.id != active.id) {
                                                adoptRoom(next, cinematic = true)
                                                return@repeat
                                            }
                                        }''',
    '''                                        for (attempt in 0 until 16) {
                                            delay(750)
                                            val next = runCatching { backend.findPremierActiveRoom() }.getOrNull()
                                            if (next != null && next.id != active.id) {
                                                adoptRoom(next, cinematic = true)
                                                break
                                            }
                                        }''',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/WordSiegeExperience.kt",
    'Text(sh("KELİME TAHTI", "KELİME TAHTI"), color = MainUi.Text, fontSize = 23.sp, fontWeight = FontWeight.Black)',
    'Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 23.sp, fontWeight = FontWeight.Black)',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/WordSiegeExperience.kt",
    'contentDescription = sh("Kelime Tahtı logosu", "Kelime Tahtı logo")',
    'contentDescription = sh("Kelime Kuşatması logosu", "Word Siege logo")',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt",
    'Text(sh("KELİME TAHTI", "KELİME TAHTI"), color = WordSiegeGameUi.Text, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)',
    'Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = WordSiegeGameUi.Text, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt",
    'internal fun WordSiegeSeriesScreen(onExit: () -> Unit) {',
    'internal fun WordSiegeSeriesScreen(verifiedAccess: Boolean = false, onExit: () -> Unit) {',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt",
    'var entitlement by remember { mutableStateOf<VipEntitlementsDto?>(null) }',
    'var entitlement by remember { mutableStateOf<VipEntitlementsDto?>(if (verifiedAccess) VipEntitlementsDto(seriesGameAccess = true) else null) }',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt",
    '''    LaunchedEffect(Unit) {
        entitlement = runCatching { backend.getVipEntitlements() }.getOrDefault(VipEntitlementsDto())
        if (entitlement?.seriesGameAccess == true) refreshLobby(showProgress = true) else loading = false
    }''',
    '''    LaunchedEffect(Unit) {
        entitlement = runCatching { backend.getVipEntitlements() }.getOrElse { entitlement ?: VipEntitlementsDto() }
        if (entitlement?.seriesGameAccess == true) refreshLobby(showProgress = true) else loading = false
    }''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/WordSiegeEntryScreen.kt",
    'WordSiegeSeriesScreen { mode = null }',
    'WordSiegeSeriesScreen(verifiedAccess = true) { mode = null }',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt",
    'WordSiegeSeriesScreen(onExit = { showSeriesGame = false })',
    'WordSiegeSeriesScreen(verifiedAccess = true, onExit = { showSeriesGame = false })',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/MainSettingsVipScreen.kt",
    '"Son Harf ${BuildConfig.VERSION_NAME} • Android"',
    '"Kelime Kuşatması ${BuildConfig.VERSION_NAME} • Android"',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/MainSettingsVipScreen.kt",
    'sh("Bu cihazdaki Son Harf oturumu kapatılacak.", "Your Son Harf session on this device will end.")',
    'sh("Bu cihazdaki Kelime Kuşatması oturumu kapatılacak.", "Your Word Siege session on this device will end.")',
)

print("full regression large-file patch applied")
