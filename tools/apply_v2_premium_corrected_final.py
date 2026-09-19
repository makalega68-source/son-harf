from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def read(path): return (ROOT / path).read_text()
def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)
def must_replace(text, old, new, label):
    if old not in text:
        raise SystemExit(f"missing replacement anchor: {label}")
    return text.replace(old, new)

# 1) Backend-authoritative premium DTO: direct ownership != PRO-derived effective access.
vip = '''package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VipEntitlementsDto(
    @SerialName("is_vip") val isVip: Boolean = false,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("series_game_direct_owned") val seriesGameDirectOwned: Boolean = false,
    @SerialName("letter_table_direct_owned") val letterTableDirectOwned: Boolean = false,
    @SerialName("score_calculator_direct_owned") val scoreCalculatorDirectOwned: Boolean = false,
    @SerialName("series_game_access") val seriesGameAccess: Boolean = false,
    @SerialName("letter_table_access") val letterTableAccess: Boolean = false,
    @SerialName("score_calculator_access") val scoreCalculatorAccess: Boolean = false,
    @SerialName("daily_jokers_claimed") val dailyJokersClaimed: Boolean = false,
    @SerialName("freezer_count") val freezerCount: Int = 0,
    @SerialName("swap_count") val swapCount: Int = 0,
    @SerialName("hint_count") val hintCount: Int = 0,
    @SerialName("multiplier_count") val multiplierCount: Int = 0,
    @SerialName("streak_shield_count") val streakShieldCount: Int = 0,
    @SerialName("xp_multiplier") val xpMultiplier: Int = 1,
    @SerialName("diamond_multiplier") val coinMultiplier: Int = 1,
    @SerialName("rewarded_ad_bypass") val rewardedAdBypass: Boolean = false,
    @SerialName("used_words_access") val usedWordsAccess: Boolean = false,
    @SerialName("direct_messages_access") val directMessagesAccess: Boolean = true,
    @SerialName("ranked_live_assist") val rankedLiveAssist: Boolean = false,
    @SerialName("post_match_analysis") val postMatchAnalysis: Boolean = false,
    @SerialName("saved_friend_list") val savedFriendList: Boolean = false,
    @SerialName("private_rooms") val privateRooms: Boolean = false,
    @SerialName("active_game_limit") val activeGameLimit: Int = 10,
)

@Serializable
data class VipDailyHelperClaimDto(
    val success: Boolean = false,
    @SerialName("already_claimed") val alreadyClaimed: Boolean = false,
    @SerialName("freezer_count") val freezerCount: Int = 0,
    @SerialName("swap_count") val swapCount: Int = 0,
    @SerialName("hint_count") val hintCount: Int = 0,
    @SerialName("multiplier_count") val multiplierCount: Int = 0,
    @SerialName("streak_shield_count") val streakShieldCount: Int = 0,
)

suspend fun OnlineGameBackend.getVipEntitlements(): VipEntitlementsDto =
    SupabaseProvider.client.postgrest.rpc("get_premium_entitlements_v2").decodeAs()

suspend fun OnlineGameBackend.claimVipDailyHelpers(): VipDailyHelperClaimDto =
    SupabaseProvider.client.postgrest.rpc("claim_vip_daily_jokers_v7").decodeAs()
'''
write('app/src/main/java/com/sonharf/game/data/VipEntitlements.kt', vip)

# 2) Real Canva artwork + honest state labels in Google Play product cards.
p = 'app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt'
s = read(p)
for old, new in {
    'R.drawable.premium_series_game':'R.drawable.premium_series_game_canva',
    'R.drawable.premium_letter_table':'R.drawable.premium_letter_table_canva',
    'R.drawable.premium_score_calculator':'R.drawable.premium_score_calculator_canva',
    'R.drawable.premium_pro':'R.drawable.premium_pro_canva',
}.items(): s = s.replace(old,new)

s = must_replace(s, 'owned = entitlements.seriesGameAccess,', 'directOwned = entitlements.seriesGameDirectOwned,\n                    proAccess = entitlements.isPro,', 'series ownership')
s = must_replace(s, 'owned = entitlements.letterTableAccess,', 'directOwned = entitlements.letterTableDirectOwned,\n                    proAccess = entitlements.isPro,', 'letter ownership')
s = must_replace(s, 'owned = entitlements.scoreCalculatorAccess,', 'directOwned = entitlements.scoreCalculatorDirectOwned,\n                    proAccess = entitlements.isPro,', 'score ownership')
s = must_replace(s, 'owned = entitlements.isPro,', 'directOwned = entitlements.isPro,\n                    proAccess = false,', 'pro ownership')
s = must_replace(s, '    owned: Boolean = false,\n    onOpen:', '    directOwned: Boolean = false,\n    proAccess: Boolean = false,\n    onOpen:', 'row signature')
s = must_replace(s, ') {\n    Surface(color = SonHarfTheme.SurfaceSecondary', ') {\n    val effectiveAccess = directOwned || proAccess\n    Surface(color = SonHarfTheme.SurfaceSecondary', 'effective access')
s = must_replace(s, '                    owned -> Text(sh("SATIN ALINDI", "OWNED"), color = SonHarfTheme.Success, fontSize = 8.sp, fontWeight = FontWeight.Black)\n                    product == null ->', '                    directOwned -> Text(sh("SATIN ALINDI", "OWNED"), color = SonHarfTheme.Success, fontSize = 8.sp, fontWeight = FontWeight.Black)\n                    proAccess -> Text(sh("PRO İLE AÇIK", "UNLOCKED WITH PRO"), color = SonHarfTheme.Primary, fontSize = 8.sp, fontWeight = FontWeight.Black)\n                    product == null ->', 'status labels')
s = s.replace('if (owned && onOpen != null)', 'if (effectiveAccess && onOpen != null)')
s = s.replace('} else if (owned) {', '} else if (effectiveAccess) {')
write(p,s)

# 3) Canonical V2 shell remains active, but PRO truth comes from server entitlement, not profiles.is_vip.
p = 'app/src/main/java/com/sonharf/game/PremiumCanvaAppV2.kt'
s = read(p)
s = must_replace(s, 'import com.sonharf.game.data.SharedDictionaryService', 'import com.sonharf.game.data.SharedDictionaryService\nimport com.sonharf.game.data.getVipEntitlements', 'V2 import')
s = must_replace(s, 'isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)', 'isPro = runCatching { backend.getVipEntitlements().isPro }.getOrDefault(false)', 'V2 pro authority')
# Add Series as an in-game destination without changing the V2 visual shell.
s = must_replace(s, 'LAST_LETTER, SIEGE, LETTER_PATH,', 'LAST_LETTER, SIEGE, LETTER_PATH, SERIES,', 'series destination enum')
s = s.replace('PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH)', 'PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH, PremiumV2Destination.SERIES)')
s = must_replace(s, '            PremiumV2Destination.LETTER_PATH -> {', '            PremiumV2Destination.LETTER_PATH,\n            PremiumV2Destination.SERIES -> {', 'series back handling')
s = must_replace(s, '                        onLetterPath = { openGame(PremiumV2Destination.LETTER_PATH, letterPathLanguage) },\n                    )', '                        onLetterPath = { openGame(PremiumV2Destination.LETTER_PATH, letterPathLanguage) },\n                        onSeries = { destination = PremiumV2Destination.SERIES },\n                    )', 'games center series callback')
s = must_replace(s, '                    PremiumV2Destination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }', '                    PremiumV2Destination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }\n                    PremiumV2Destination.SERIES -> WordSiegeSeriesScreen(onExit = { leaveGame(PremiumV2Destination.GAMES) })', 'series screen route')
# Extend only the game-center function signature and append a compact fourth card before the closing of its first LazyColumn scope.
needle = 'private fun PremiumV2GameCenter('
start = s.find(needle)
if start < 0: raise SystemExit('missing game center')
# Add callback at end of signature by finding the first ') {' following the function start.
sig_end = s.find(') {', start)
sig = s[start:sig_end]
if 'onSeries:' not in sig:
    last_nl = sig.rfind('\n')
    sig = sig[:last_nl] + '\n    onSeries: () -> Unit,' + sig[last_nl:]
    s = s[:start] + sig + s[sig_end:]
# Find the next composable after game center to scope insertion.
start = s.find(needle)
next_comp = s.find('\n@Composable', start + len(needle))
segment = s[start: next_comp if next_comp > 0 else len(s)]
if 'SERİ OYUN' not in segment:
    # Insert immediately before the final closing brace of this function.
    insert_at = (next_comp if next_comp > 0 else len(s))
    pos = s.rfind('\n}', start, insert_at)
    series_ui = '''\n    Surface(\n        onClick = onSeries,\n        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),\n        shape = MainUiShape.Card,\n        color = SonHarfTheme.Surface,\n        border = BorderStroke(1.dp, SonHarfTheme.ActionOrange.copy(alpha = .24f)),\n        shadowElevation = 3.dp,\n    ) {\n        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {\n            Image(painter = painterResource(R.drawable.premium_series_game_canva), contentDescription = null, modifier = Modifier.size(58.dp), contentScale = ContentScale.Fit)\n            Spacer(Modifier.width(12.dp))\n            Column(Modifier.weight(1f)) {\n                Text(sh("SERİ OYUN", "SERIES GAME"), color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)\n                Text(sh("3 / 5 / 10 dk • premium hızlı mod", "3 / 5 / 10 min • premium fast mode"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp)\n            }\n            Icon(Icons.Rounded.LockClock, null, tint = SonHarfTheme.ActionOrange, modifier = Modifier.size(20.dp))\n        }\n    }\n'''
    s = s[:pos] + series_ui + s[pos:]
write(p,s)

# 4) Source-level contracts. These are intentionally text contracts for regression-prone integration rules.
test = r'''package com.sonharf.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class V2PremiumCorrectedFinalContractTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir"))
        repeat(6) {
            if (File(d, "app/src/main").exists()) return d
            d = d.parentFile ?: return@repeat
        }
        error("repo root not found")
    }
    private fun text(path: String) = File(root(), path).readText()

    @Test fun canonicalV2ShellIsRuntimeAuthority() {
        val stable = text("app/src/main/java/com/sonharf/game/StableV1App.kt")
        assertTrue("PremiumCanvaAppV2" in stable)
        assertFalse("MonsterLime" in stable)
        val v2 = text("app/src/main/java/com/sonharf/game/PremiumCanvaAppV2.kt")
        assertTrue("lightColorScheme" in v2)
        assertTrue("PremiumWeeklyBestPolished" in v2)
        assertTrue("PremiumV2BottomBar" in v2)
        assertTrue("SERİ OYUN" in v2)
    }

    @Test fun canvaArtworkIsRuntimeAsset() {
        val card = text("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt")
        listOf("premium_series_game_canva","premium_letter_table_canva","premium_score_calculator_canva","premium_pro_canva").forEach { assertTrue(it in card) }
        listOf("R.drawable.premium_series_game,","R.drawable.premium_letter_table,","R.drawable.premium_score_calculator,","R.drawable.premium_pro,").forEach { assertFalse(it in card) }
        listOf("series_game","letter_table","score_calculator","pro_lifetime").forEach { assertTrue(it in text("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt")) }
    }

    @Test fun ownershipLabelsAreDistinct() {
        val e = text("app/src/main/java/com/sonharf/game/data/VipEntitlements.kt")
        assertTrue("seriesGameDirectOwned" in e && "letterTableDirectOwned" in e && "scoreCalculatorDirectOwned" in e)
        val card = text("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt")
        assertTrue("SATIN ALINDI" in card)
        assertTrue("PRO İLE AÇIK" in card)
        assertTrue("oneTimePurchaseOfferDetails != null" in card)
    }

    @Test fun blackThemeIsNotBundledIntoPro() {
        val catalog = text("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt")
        assertFalse("theme_black" in catalog)
        val store = text("app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt")
        assertTrue("theme_black" in store)
        assertFalse("theme_dark_arena" in text("app/src/main/java/com/sonharf/game/data/VipEntitlements.kt"))
    }
}
'''
write('app/src/test/java/com/sonharf/game/V2PremiumCorrectedFinalContractTest.kt', test)

print('V2 premium corrected patch applied')
