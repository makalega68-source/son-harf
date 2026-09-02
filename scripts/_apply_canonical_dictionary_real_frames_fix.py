from pathlib import Path

ROOT = Path('.')

def patch(path: str, old: str, new: str):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old in text:
        p.write_text(text.replace(old, new), encoding='utf-8')
    elif new not in text:
        raise SystemExit(f'contract drift in {path}: {old[:100]!r}')

# ---------------------------------------------------------------------------
# Word Siege practice: load/restore the exact canonical dictionary snapshot
# before allowing any move. No separate practice lexicon is permitted.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt'
patch(path,
'''import androidx.compose.ui.platform.LocalContext\n''',
'''import androidx.compose.ui.platform.LocalContext\n''') if (ROOT/path).read_text(encoding='utf-8').find('import androidx.compose.ui.platform.LocalContext') >= 0 else None

p = ROOT / path
text = p.read_text(encoding='utf-8')
if 'import androidx.compose.ui.platform.LocalContext' not in text:
    text = text.replace('import androidx.compose.ui.graphics.Color\n', 'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.platform.LocalContext\n')
if 'import com.sonharf.game.data.SharedDictionaryService' not in text:
    text = text.replace('import com.sonharf.game.data.ProfileDto\n', 'import com.sonharf.game.data.ProfileDto\nimport com.sonharf.game.data.SharedDictionaryService\n')

old = '''    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }\n    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }'''
new = '''    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }\n    val context = LocalContext.current.applicationContext\n    var dictionaryReady by remember { mutableStateOf(SharedDictionaryService.hasSnapshot(state.language)) }\n    var dictionaryLoading by remember { mutableStateOf(false) }\n    var dictionaryRetryKey by remember { mutableIntStateOf(0) }\n    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('practice state contract drift')

anchor = '''    LaunchedEffect(me, backend) {\n        val b = backend ?: return@LaunchedEffect\n        if (me != null) playerProfile = runCatching { b.getProfile(me) }.getOrNull()\n    }\n'''
insert = '''    LaunchedEffect(me, backend) {\n        val b = backend ?: return@LaunchedEffect\n        if (me != null) playerProfile = runCatching { b.getProfile(me) }.getOrNull()\n    }\n\n    LaunchedEffect(state.language, dictionaryRetryKey) {\n        dictionaryLoading = true\n        dictionaryReady = SharedDictionaryService.restorePersisted(context, state.language)\n        if (!dictionaryReady) {\n            runCatching { SharedDictionaryService.preloadCanonical(context, state.language) }\n                .onSuccess {\n                    dictionaryReady = true\n                    notice = sh(\"Ana sözlük hazır. Çevrimdışı alıştırmada da aynı sözlük kullanılacak.\", \"Main dictionary ready. The same dictionary will be used for offline practice.\")\n                }\n                .onFailure {\n                    dictionaryReady = false\n                    notice = sh(\"Ana sözlük yüklenemedi. Yenile düğmesine basıp tekrar dene.\", \"Main dictionary could not be loaded. Tap refresh to retry.\")\n                }\n        }\n        dictionaryLoading = false\n    }\n'''
if anchor in text: text = text.replace(anchor, insert)
elif insert not in text: raise SystemExit('dictionary launched effect anchor drift')

old = '''    fun applyPlayerMove() {\n        runCatching { WordSiegePracticeEngine.applyMove(state, 1, placements) }'''
new = '''    fun applyPlayerMove() {\n        if (!dictionaryReady) {\n            notice = sh(\"Ana sözlük henüz hazır değil. Yenile düğmesine basıp tekrar dene.\", \"Main dictionary is not ready yet. Tap refresh and try again.\")\n            return\n        }\n        runCatching { WordSiegePracticeEngine.applyMove(state, 1, placements) }'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('apply move guard drift')

old = '''    LaunchedEffect(state.currentOwner, state.moveCount, state.status, displayedPlayerScore, displayedBotScore, displayedOwner) {\n        if (state.status != \"playing\" || state.currentOwner != 2) return@LaunchedEffect'''
new = '''    LaunchedEffect(state.currentOwner, state.moveCount, state.status, displayedPlayerScore, displayedBotScore, displayedOwner, dictionaryReady) {\n        if (!dictionaryReady || state.status != \"playing\" || state.currentOwner != 2) return@LaunchedEffect'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('bot dictionary guard drift')

old = '''                        Text(sh(\"BOT İLE ALIŞTIRMA • ÇEVRİMDIŞI\", \"BOT PRACTICE • OFFLINE\"), color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)'''
new = '''                        Text(\n                            when {\n                                dictionaryLoading -> sh(\"BOT İLE ALIŞTIRMA • ANA SÖZLÜK HAZIRLANIYOR\", \"BOT PRACTICE • LOADING MAIN DICTIONARY\")\n                                dictionaryReady -> sh(\"BOT İLE ALIŞTIRMA • ANA SÖZLÜK\", \"BOT PRACTICE • MAIN DICTIONARY\")\n                                else -> sh(\"BOT İLE ALIŞTIRMA • ANA SÖZLÜK GEREKLİ\", \"BOT PRACTICE • MAIN DICTIONARY REQUIRED\")\n                            },\n                            color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1,\n                        )'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('practice subtitle drift')

old = '''                        onClick = {\n                            if (state.moveCount > 0 || placements.isNotEmpty()) showRestart = true else startAgain()\n                        },'''
new = '''                        onClick = {\n                            if (!dictionaryReady) dictionaryRetryKey += 1\n                            else if (state.moveCount > 0 || placements.isNotEmpty()) showRestart = true else startAgain()\n                        },'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('refresh retry drift')

text = text.replace('enabled = state.status == "playing" && state.currentOwner == 1 && !botThinking,', 'enabled = dictionaryReady && state.status == "playing" && state.currentOwner == 1 && !botThinking,')
text = text.replace('if (state.status != "playing" || state.currentOwner != 1 || botThinking) return@WordSiegeBoard', 'if (!dictionaryReady || state.status != "playing" || state.currentOwner != 1 || botThinking) return@WordSiegeBoard')
text = text.replace('enabled = state.currentOwner == 1 && !botThinking,', 'enabled = dictionaryReady && state.currentOwner == 1 && !botThinking,')
text = text.replace('enabled = state.currentOwner == 1 && !botThinking && state.bag.isNotEmpty(),', 'enabled = dictionaryReady && state.currentOwner == 1 && !botThinking && state.bag.isNotEmpty(),')
text = text.replace('enabled = state.currentOwner == 1 && placements.isNotEmpty() && !botThinking,', 'enabled = dictionaryReady && state.currentOwner == 1 && placements.isNotEmpty() && !botThinking,')
p.write_text(text, encoding='utf-8')

# ---------------------------------------------------------------------------
# Style: backend catalog/inventory is authoritative. The six staged frame IDs
# are not the entire catalog. Legacy purchased IDs get deterministic native
# rendering; malformed staged raster files are never offered for new purchase.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt'
p = ROOT / path
text = p.read_text(encoding='utf-8')

text = text.replace('''        GOLD_CROWN -> Color(0xFFE0A51C)\n        else -> Color(0xFF8A97A8)''', '''        GOLD_CROWN -> Color(0xFFE0A51C)\n        \"frame_neon\", \"frame_modern_neon\" -> Color(0xFF1677FF)\n        \"frame_starter\", \"frame_ice\", \"frame_crystal\" -> Color(0xFF32BFB3)\n        \"frame_gold\", \"frame_royal_gold\" -> Color(0xFFD7A72E)\n        \"frame_black_gold\" -> Color(0xFF5E5140)\n        \"frame_purple_prestige\" -> Color(0xFF7257D8)\n        else -> Color(0xFF8A97A8)''')
text = text.replace('@DrawableRes val drawable: Int,', '@DrawableRes val drawable: Int?,')

old = '''@Composable\nprivate fun rememberStyleBitmap(@DrawableRes drawable: Int): ImageBitmap? {\n    val resources = LocalContext.current.resources\n    return remember(resources, drawable) {\n        runCatching {\n            resources.openRawResource(drawable).use { stream ->\n                BitmapFactory.decodeStream(stream)?.asImageBitmap()\n            }\n        }.getOrNull()\n    }\n}'''
new = '''@Composable\nprivate fun rememberStyleBitmap(@DrawableRes drawable: Int?): ImageBitmap? {\n    val resources = LocalContext.current.resources\n    return remember(resources, drawable) {\n        if (drawable == null) null else runCatching {\n            resources.openRawResource(drawable).use { stream ->\n                BitmapFactory.decodeStream(stream)?.asImageBitmap()\n            }\n        }.getOrNull()\n    }\n}'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('nullable bitmap loader drift')

old = '''@Composable\nprivate fun SafeFrameArtwork(\n    @DrawableRes drawable: Int,\n    frameId: String,\n    modifier: Modifier,\n): Boolean {\n    val bitmap = rememberStyleBitmap(drawable)'''
new = '''@Composable\nprivate fun SafeFrameArtwork(\n    @DrawableRes drawable: Int?,\n    frameId: String,\n    modifier: Modifier,\n): Boolean {\n    val bitmap = rememberStyleBitmap(drawable)'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('safe frame signature drift')

old = '''@Composable\ninternal fun PurchasedProfileFrameOverlay(frameId: String?, modifier: Modifier = Modifier) {\n    val drawable = PurchasedFrameCatalog.drawable(frameId) ?: return\n    SafeFrameArtwork(\n        drawable = drawable,\n        frameId = frameId.orEmpty(),\n        modifier = modifier,\n    )\n}\n\n@Composable\ninternal fun PurchasedProfileFramesStoreRow'''
new = '''@Composable\ninternal fun PurchasedProfileFrameOverlay(frameId: String?, modifier: Modifier = Modifier) {\n    if (frameId.isNullOrBlank()) return\n    SafeFrameArtwork(\n        drawable = PurchasedFrameCatalog.drawable(frameId),\n        frameId = frameId,\n        modifier = modifier,\n    )\n}\n\nprivate val verifiedStagedFrameIds = setOf(PurchasedFrameCatalog.GREEN, PurchasedFrameCatalog.MINT)\n\nprivate fun legacyFrameSpec(item: ShopItemDto): PurchasedFrameSpec = PurchasedFrameSpec(\n    id = item.id,\n    titleTr = item.nameTr,\n    titleEn = item.nameEn,\n    subtitleTr = item.descriptionTr.ifBlank { \"Satın alınmış profil çerçevesi\" },\n    subtitleEn = item.descriptionEn.ifBlank { \"Purchased profile frame\" },\n    drawable = null,\n    accent = PurchasedFrameCatalog.accent(item.id),\n    accessTr = if (item.vipOnly) \"VIP / PREMIUM\" else \"MAĞAZA\",\n    accessEn = if (item.vipOnly) \"VIP / PREMIUM\" else \"SHOP\",\n    sourceIcon = if (item.vipOnly) R.drawable.style_icon_trophy else R.drawable.style_icon_coin,\n)\n\n@Composable\ninternal fun PurchasedProfileFramesStoreRow'''
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('legacy frame helper drift')

old = 'val shop = b.getShopItems().filter { it.kind == "profile_frame" && it.id in PurchasedFrameCatalog.ids }'
new = 'val shop = b.getShopItems().filter { it.kind == "profile_frame" }'
if old in text: text = text.replace(old, new)
elif new not in text: raise SystemExit('backend frame filter drift')

anchor = '''    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {'''
insert = '''    val displaySpecs = remember(shopItems, inventory, equippedId) {\n        val legacy = shopItems.values\n            .filter { it.id !in PurchasedFrameCatalog.ids }\n            .sortedBy { it.sortOrder }\n            .map(::legacyFrameSpec)\n        val staged = purchasedFrameSpecs.filter { spec ->\n            spec.id in verifiedStagedFrameIds || spec.id in inventory || equippedId == spec.id\n        }\n        (legacy + staged).distinctBy { it.id }\n    }\n\n    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {'''
if anchor in text: text = text.replace(anchor, insert, 1)
elif insert not in text: raise SystemExit('display specs insertion drift')

text = text.replace('items(purchasedFrameSpecs, key = { it.id }) { spec ->', 'items(displaySpecs, key = { it.id }) { spec ->')
text = text.replace('val assetReady = frameBitmap != null', 'val assetReady = spec.drawable == null || frameBitmap != null')
text = text.replace('''                                } else {\n                                    Box(\n                                        Modifier.size(82.dp)''', '''                                } else {\n                                    Box(\n                                        Modifier.size(82.dp)''')
text = text.replace('sh("Görsel doğrulanamadı", "Artwork unavailable")', 'sh("Orijinal görsel onarılıyor", "Original artwork is being repaired")')
text = text.replace('''                                !assetReady -> Text(sh("KAPALI", "LOCKED"), color = Color(0xFF8A97A8), fontSize = 7.sp, fontWeight = FontWeight.Black)''', '''                                !assetReady && !owned -> Text(sh("KAPALI", "LOCKED"), color = Color(0xFF8A97A8), fontSize = 7.sp, fontWeight = FontWeight.Black)''')
text = text.replace('enabled = backend != null && (owned || item != null) && busyId == null,', 'enabled = backend != null && (owned || (item != null && assetReady)) && busyId == null,')
p.write_text(text, encoding='utf-8')

# Update implementation-sensitive asset test to the repaired catalog contract.
path = 'app/src/test/java/com/sonharf/game/AssetIntegrationContractTest.kt'
p = ROOT / path
text = p.read_text(encoding='utf-8')
text = text.replace('assertTrue(frames.contains("Görsel doğrulanamadı") || frames.contains("Artwork unavailable"))', 'assertTrue(frames.contains("Orijinal görsel onarılıyor") || frames.contains("Original artwork is being repaired"))')
text = text.replace('assertTrue(frames.contains("!assetReady ->"))', 'assertTrue(frames.contains("!assetReady && !owned ->"))')
text = text.replace('assertTrue(frames.contains("!assetReady -> Text"))', 'assertTrue(frames.contains("!assetReady && !owned -> Text"))')
p.write_text(text, encoding='utf-8')

print('canonical dictionary + real frame catalog patch applied')
