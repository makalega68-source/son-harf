from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def save(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str, label: str) -> None:
    text = load(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match in {path}, found {count}")
    save(path, text.replace(old, new, 1))


# 1) PRO purchase: lifetime-only presentation; remove the explicitly prohibited Fair Play block.
replace_once(
    "app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt",
    'Text("Kelime Tahtı PRO", color = ProText, fontSize = 20.sp, fontWeight = FontWeight.Black)',
    'Text("Kelime Kuşatması PRO", color = ProText, fontSize = 20.sp, fontWeight = FontWeight.Black)',
    "PRO title",
)
replace_once(
    "app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt",
    '''                Surface(\n                    shape = RoundedCornerShape(14.dp),\n                    color = ProGreen.copy(alpha = .10f),\n                    border = BorderStroke(1.dp, ProGreen.copy(alpha = .35f)),\n                ) {\n                    Text(\n                        sh(\n                            "ADİL REKABET: PRO, dereceli maçlarda skor, hedef harf veya kelime avantajı vermez.",\n                            "FAIR PLAY: PRO gives no score, target-letter, or word advantage in ranked matches.",\n                        ),\n                        Modifier.fillMaxWidth().padding(11.dp),\n                        color = ProGreen,\n                        fontSize = 9.sp,\n                        fontWeight = FontWeight.Bold,\n                        textAlign = TextAlign.Center,\n                    )\n                }\n\n''',
    "",
    "remove fair play block",
)

# 2) Permanent product detail dialog: Series Game is a first-class Play product too.
path = "app/src/main/java/com/sonharf/game/PremiumProductPurchaseDialog.kt"
text = load(path)
text = text.replace(
    "import androidx.compose.material.icons.rounded.GridView\n",
    "import androidx.compose.material.icons.rounded.GridView\nimport androidx.compose.material.icons.rounded.Timer\n",
    1,
)
text = text.replace(
    "    require(productId == ProductCatalog.LETTER_TABLE || productId == ProductCatalog.SCORE_CALCULATOR)\n",
    "    require(productId in setOf(ProductCatalog.SERIES_GAME, ProductCatalog.LETTER_TABLE, ProductCatalog.SCORE_CALCULATOR))\n",
    1,
)
old = '''    val isLetterTable = productId == ProductCatalog.LETTER_TABLE\n    val title = if (isLetterTable) sh("Harf Tablosu", "Letter Table") else sh("Puan Hesaplayıcı", "Score Calculator")\n    val description = if (isLetterTable) {\n        sh(\n            "Kelime Kuşatması sırasında kalan harf adetlerini server doğrulamasıyla gösterir. Tek ödeme ile kalıcı erişim.",\n            "Shows remaining letter counts during Word Siege with server validation. One payment for permanent access.",\n        )\n    } else {\n        sh(\n            "Hamleni göndermeden önce kelime ve bölge puanı ön izlemesini server doğrulamasıyla gösterir. Tek ödeme ile kalıcı erişim.",\n            "Shows a server-validated word and territory score preview before submitting your move. One payment for permanent access.",\n        )\n    }\n    val price = product?.oneTimePurchaseOfferDetails?.formattedPrice\n        ?: if (isLetterTable) ProductCatalog.LETTER_TABLE_FALLBACK_PRICE_TRY else ProductCatalog.SCORE_CALCULATOR_FALLBACK_PRICE_TRY\n'''
new = '''    val isSeriesGame = productId == ProductCatalog.SERIES_GAME\n    val isLetterTable = productId == ProductCatalog.LETTER_TABLE\n    val title = when (productId) {\n        ProductCatalog.SERIES_GAME -> sh("Seri Oyun", "Series Game")\n        ProductCatalog.LETTER_TABLE -> sh("Harf Tablosu", "Letter Table")\n        else -> sh("Puan Hesaplayıcı", "Score Calculator")\n    }\n    val description = when (productId) {\n        ProductCatalog.SERIES_GAME -> sh(\n            "3, 5 veya 10 dakikalık server zamanlı ayrı eşleşme havuzunu kalıcı olarak açar. Tek ödeme ile kalıcı erişim.",\n            "Permanently unlocks the separate server-timed 3, 5 or 10 minute matchmaking pool. One payment for permanent access.",\n        )\n        ProductCatalog.LETTER_TABLE -> sh(\n            "Kelime Kuşatması sırasında kalan harf adetlerini server doğrulamasıyla gösterir. Tek ödeme ile kalıcı erişim.",\n            "Shows remaining letter counts during Word Siege with server validation. One payment for permanent access.",\n        )\n        else -> sh(\n            "Hamleni göndermeden önce kelime ve bölge puanı ön izlemesini server doğrulamasıyla gösterir. Tek ödeme ile kalıcı erişim.",\n            "Shows a server-validated word and territory score preview before submitting your move. One payment for permanent access.",\n        )\n    }\n    val price = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: when (productId) {\n        ProductCatalog.SERIES_GAME -> ProductCatalog.SERIES_GAME_FALLBACK_PRICE_TRY\n        ProductCatalog.LETTER_TABLE -> ProductCatalog.LETTER_TABLE_FALLBACK_PRICE_TRY\n        else -> ProductCatalog.SCORE_CALCULATOR_FALLBACK_PRICE_TRY\n    }\n'''
if old not in text:
    raise RuntimeError("PremiumProductPurchaseDialog product copy anchor missing")
text = text.replace(old, new, 1)
text = text.replace(
    "                if (isLetterTable) Icons.Rounded.GridView else Icons.Rounded.Calculate,\n",
    "                when { isSeriesGame -> Icons.Rounded.Timer; isLetterTable -> Icons.Rounded.GridView; else -> Icons.Rounded.Calculate },\n",
    1,
)
save(path, text)

# 3) Series locked state must route directly to the Series Game purchase, then re-read server entitlement.
path = "app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt"
text = load(path)
if "import com.sonharf.game.billing.ProductCatalog" not in text:
    text = text.replace("import com.sonharf.game.data.*\n", "import com.sonharf.game.billing.ProductCatalog\nimport com.sonharf.game.data.*\n", 1)
text = text.replace(
    "    var clockTick by remember { mutableLongStateOf(0L) }\n",
    "    var clockTick by remember { mutableLongStateOf(0L) }\n    var showSeriesPurchase by remember { mutableStateOf(false) }\n",
    1,
)
text = text.replace(
    "            entitlement?.seriesGameAccess != true -> SeriesLocked(onExit)\n",
    "            entitlement?.seriesGameAccess != true -> SeriesLocked(onExit = onExit, onPurchase = { showSeriesPurchase = true })\n",
    1,
)
anchor = "\n    if (inviteFriend) {\n"
insert = '''\n    if (showSeriesPurchase) {\n        PremiumProductPurchaseDialog(\n            productId = ProductCatalog.SERIES_GAME,\n            onVerified = {\n                showSeriesPurchase = false\n                scope.launch {\n                    entitlement = runCatching { backend.getVipEntitlements() }.getOrDefault(entitlement ?: VipEntitlementsDto())\n                    if (entitlement?.seriesGameAccess == true) refreshLobby(showProgress = true)\n                }\n            },\n            onDismiss = { showSeriesPurchase = false },\n        )\n    }\n'''
if insert.strip() not in text:
    if anchor not in text:
        raise RuntimeError("Series purchase insertion anchor missing")
    text = text.replace(anchor, insert + anchor, 1)
text = text.replace(
    "private fun SeriesLocked(onExit: () -> Unit) {",
    "private fun SeriesLocked(onExit: () -> Unit, onPurchase: () -> Unit) {",
    1,
)
text = text.replace(
    '        Button(onClick = onExit) { Text(sh("MAĞAZAYA DÖN", "BACK TO STORE")) }\n',
    '''        Button(onClick = onPurchase) {\n            Icon(Icons.Rounded.ShoppingCart, null)\n            Spacer(Modifier.width(7.dp))\n            Text(sh("SERİ OYUN'U AÇ", "UNLOCK SERIES GAME"), fontWeight = FontWeight.Black)\n        }\n        TextButton(onClick = onExit) { Text(sh("GERİ DÖN", "GO BACK")) }\n''',
    1,
)
save(path, text)

# 4) Featured store must expose premium products; Coin sheet must remain Coin-only.
path = "app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt"
text = load(path)
hero = '''                    item {\n                        PremiumStoreProHero(entitlements?.isPro == true) { tab = 3 }\n                    }\n'''
featured = hero + '''                    item {\n                        GooglePlayProductsCard(\n                            onPurchased = { scope.launch { reload() } },\n                            showPremiumProducts = true,\n                            showCoinPacks = false,\n                        )\n                    }\n'''
if "showPremiumProducts = true,\n                            showCoinPacks = false" not in text:
    if hero not in text:
        raise RuntimeError("PremiumStore featured hero anchor missing")
    text = text.replace(hero, featured, 1)
text = text.replace(
    "            GooglePlayProductsCard { scope.launch { reload() } }\n",
    '''            GooglePlayProductsCard(\n                onPurchased = { scope.launch { reload() } },\n                showPremiumProducts = false,\n                showCoinPacks = true,\n            )\n''',
    1,
)
save(path, text)

# 5) PRO page shows the exact, non-pay-to-win bundle instead of old analysis/private-room copy.
path = "app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt"
text = load(path)
text = text.replace(
    'Text(sh("Premium üyelik", "Premium membership"), color = SonHarfTheme.Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)',
    'Text(sh("Tek ödeme • kalıcı PRO", "One payment • lifetime PRO"), color = SonHarfTheme.Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)',
    1,
)
old_cards = '''        item {\n            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {\n                ProAccessCard(\n                    icon = Icons.Rounded.Block,\n                    label = sh("REKLAMSIZ", "AD-FREE"),\n                    enabled = active && e?.rewardedAdBypass == true,\n                    accent = SonHarfTheme.Primary,\n                    modifier = Modifier.weight(1f),\n                )\n                ProAccessCard(\n                    icon = Icons.Rounded.BarChart,\n                    label = sh("ANALİZ", "ANALYSIS"),\n                    enabled = active && e?.postMatchAnalysis == true,\n                    accent = SonHarfTheme.Turquoise,\n                    modifier = Modifier.weight(1f),\n                )\n                ProAccessCard(\n                    icon = Icons.Rounded.MeetingRoom,\n                    label = sh("ÖZEL ODA", "PRIVATE ROOM"),\n                    enabled = active && e?.privateRooms == true,\n                    accent = SonHarfTheme.Purple,\n                    modifier = Modifier.weight(1f),\n                )\n            }\n        }\n'''
new_cards = '''        item {\n            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {\n                ProAccessCard(\n                    icon = Icons.Rounded.Block,\n                    label = sh("REKLAMSIZ", "AD-FREE"),\n                    enabled = active,\n                    accent = SonHarfTheme.Primary,\n                    modifier = Modifier.weight(1f),\n                )\n                ProAccessCard(\n                    icon = Icons.Rounded.Bolt,\n                    label = sh("SERİ OYUN", "SERIES GAME"),\n                    enabled = active && e?.seriesGameAccess == true,\n                    accent = SonHarfTheme.Turquoise,\n                    modifier = Modifier.weight(1f),\n                )\n                ProAccessCard(\n                    icon = Icons.Rounded.ViewAgenda,\n                    label = sh("50 OYUN", "50 GAMES"),\n                    enabled = active && (e?.activeGameLimit ?: 10) >= 50,\n                    accent = SonHarfTheme.Purple,\n                    modifier = Modifier.weight(1f),\n                )\n            }\n        }\n'''
if old_cards not in text:
    raise RuntimeError("UnifiedProVipScreen access cards anchor missing")
text = text.replace(old_cards, new_cards, 1)
old_benefits = '''                    ProBenefitRow(Icons.Rounded.Palette, sh("PRO Style ve profil ayrıcalıkları", "PRO Style and profile benefits"), SonHarfTheme.Purple)\n                    ProBenefitRow(Icons.Rounded.AutoGraph, sh("Gelişmiş istatistik ve maç analizi", "Advanced stats and match analysis"), SonHarfTheme.Turquoise)\n                    ProBenefitRow(Icons.Rounded.Groups, sh("Sosyal ve arkadaş ayrıcalıkları", "Social and friend benefits"), SonHarfTheme.Primary)\n                    ProBenefitRow(Icons.Rounded.DoorFront, sh("Özel oda erişimi", "Private room access"), SonHarfTheme.ActionOrange)\n'''
new_benefits = '''                    ProBenefitRow(Icons.Rounded.Block, sh("Reklamsız kullanım", "Ad-free use"), SonHarfTheme.Primary)\n                    ProBenefitRow(Icons.Rounded.Calculate, sh("Puan Hesaplayıcı", "Score Calculator"), SonHarfTheme.Turquoise)\n                    ProBenefitRow(Icons.Rounded.GridView, sh("Harf Tablosu", "Letter Table"), SonHarfTheme.Turquoise)\n                    ProBenefitRow(Icons.Rounded.Bolt, sh("Seri Oyun", "Series Game"), SonHarfTheme.ActionOrange)\n                    ProBenefitRow(Icons.Rounded.Groups, sh("Arkadaş Listesi", "Friends List"), SonHarfTheme.Primary)\n                    ProBenefitRow(Icons.Rounded.History, sh("Son Harf tam kelime geçmişi", "Full Son Harf word history"), SonHarfTheme.Purple)\n                    ProBenefitRow(Icons.Rounded.ViewAgenda, sh("Aynı anda 50 aktif oyun", "Up to 50 active games"), SonHarfTheme.ActionOrange)\n                    ProBenefitRow(Icons.Rounded.AccountCircle, sh("PRO profil çerçevesi", "PRO profile frame"), SonHarfTheme.Purple)\n                    ProBenefitRow(Icons.Rounded.WorkspacePremium, sh("PRO rozeti ve prestij", "PRO badge and prestige"), SonHarfTheme.Purple)\n                    ProBenefitRow(Icons.Rounded.Toll, sh("İlk başarılı PRO aktivasyonunda bir kez 100 Son Coin", "100 Son Coins once on the first successful PRO activation"), SonHarfTheme.ActionOrange)\n'''
if old_benefits not in text:
    raise RuntimeError("UnifiedProVipScreen benefits anchor missing")
text = text.replace(old_benefits, new_benefits, 1)
text = text.replace(
    'Text(sh("PRO ÜYELİĞİN AKTİF", "YOUR PRO MEMBERSHIP IS ACTIVE"), color = SonHarfTheme.Turquoise, fontWeight = FontWeight.Black, fontSize = 11.sp)',
    'Text(sh("PRO ERİŞİMİN AKTİF", "YOUR PRO ACCESS IS ACTIVE"), color = SonHarfTheme.Turquoise, fontWeight = FontWeight.Black, fontSize = 11.sp)',
    1,
)
text = text.replace(
    'notice = sh("PRO üyeliğin doğrulandı.", "Your PRO membership was verified.")',
    'notice = sh("PRO erişimin doğrulandı.", "Your PRO access was verified.")',
    1,
)
save(path, text)

# 6) High-risk final acceptance contracts.
test_path = ROOT / "app/src/test/java/com/sonharf/game/V2PremiumFinalAcceptanceContractTest.kt"
test_path.write_text(r'''package com.sonharf.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class V2PremiumFinalAcceptanceContractTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir"))
        repeat(7) {
            if (File(d, "app/src/main").exists()) return d
            d = d.parentFile ?: return@repeat
        }
        error("repo root not found")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test fun proPurchaseIsLifetimeOnlyAndHasNoProhibitedFairPlayOrRestoreUi() {
        val s = source("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt")
        assertTrue("ProductCatalog.PRO_LIFETIME" in s)
        assertTrue("oneTimePurchaseOfferDetails" in s)
        assertFalse("ADİL REKABET" in s)
        assertFalse("FAIR PLAY" in s)
        assertFalse("Restore Purchases" in s)
        assertFalse("Manage subscription" in s)
        assertFalse("Aboneliği yönet" in s)
    }

    @Test fun seriesLockedStateRoutesToExactPermanentSeriesProduct() {
        val dialog = source("app/src/main/java/com/sonharf/game/PremiumProductPurchaseDialog.kt")
        assertTrue("ProductCatalog.SERIES_GAME" in dialog)
        assertTrue("SERIES_GAME_FALLBACK_PRICE_TRY" in dialog)
        val series = source("app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt")
        assertTrue("productId = ProductCatalog.SERIES_GAME" in series)
        assertTrue("backend.getVipEntitlements()" in series)
        assertTrue("SERİ OYUN'U AÇ" in series)
    }

    @Test fun featuredStoreShowsPremiumProductsAndCoinSheetDoesNotDuplicateThem() {
        val s = source("app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt")
        assertTrue("showPremiumProducts = true" in s)
        assertTrue("showCoinPacks = false" in s)
        assertTrue("showPremiumProducts = false" in s)
        assertTrue("showCoinPacks = true" in s)
        assertFalse("Maskot" in s)
        assertFalse("Mascot" in s)
    }

    @Test fun proPageListsExactBundleWithoutLegacyAnalysisOrPrivateRoomSalesCopy() {
        val s = source("app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt")
        listOf(
            "Reklamsız kullanım",
            "Puan Hesaplayıcı",
            "Harf Tablosu",
            "Seri Oyun",
            "Arkadaş Listesi",
            "Son Harf tam kelime geçmişi",
            "50 aktif oyun",
            "PRO profil çerçevesi",
            "PRO rozeti ve prestij",
            "100 Son Coin",
        ).forEach { assertTrue(it in s, "Missing PRO benefit: $it") }
        assertFalse("Gelişmiş istatistik ve maç analizi" in s)
        assertFalse("Özel oda erişimi" in s)
    }

    @Test fun proAdBannerIsCollapsedFromAuthoritativeProState() {
        val banner = source("app/src/main/java/com/sonharf/game/NonGameBannerAd.kt")
        assertTrue("isPremium" in banner)
        assertTrue("if (isPremium)" in banner)
        val shell = source("app/src/main/java/com/sonharf/game/PremiumCanvaAppV2.kt")
        assertTrue("backend.getVipEntitlements().isPro" in shell)
        assertTrue("isPremium = isPro" in shell)
    }

    @Test fun seriesBackendSourceIsSeparateAndServerTimed() {
        val sql = source("supabase/migrations/20260919043000_word_siege_series_game_v1.sql")
        listOf("180", "300", "600", "game_mode = 'series'", "turn_deadline", "series_three_timeouts", "TIMEOUT_AUTO_PASS", "word_siege_series_reset_actor_v1").forEach {
            assertTrue(it in sql, "Missing Series backend contract marker: $it")
        }
        assertTrue("coalesce(g.game_mode, 'classic') = 'classic'" in sql || "game_mode is null or game_mode = 'classic'" in sql)
    }
}
''', encoding="utf-8")

print("final V2 premium acceptance patch applied")
