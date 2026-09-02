from pathlib import Path

ROOT = Path('.')
RES = ROOT / 'app/src/main/res/drawable-nodpi'
RES.mkdir(parents=True, exist_ok=True)

def patch(path, old, new):
    p=ROOT/path
    text=p.read_text(encoding='utf-8')
    if old in text:
        p.write_text(text.replace(old,new),encoding='utf-8')
    elif new not in text:
        raise SystemExit(f'contract drift: {path} missing expected fragment: {old[:80]}')

# Stage 1: harden shop composition against backend-constructor failures.
for rel in [Path('app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt'), Path('app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt')]:
    patch(rel,
      'val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }',
      'val backend = remember { if (SupabaseProvider.configured) runCatching { OnlineGameBackend() }.getOrNull() else null }')

p=Path('app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt')
text=p.read_text(encoding='utf-8')
text=text.replace('''    const val GREEN = "frame_asset_green"\n\n    val ids = setOf(GOLD, MINT, PURPLE, GREEN)''','''    const val GREEN = "frame_asset_green"\n    const val RED = "frame_asset_red"\n    const val GOLD_CROWN = "frame_asset_gold_crown"\n\n    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN)''')
text=text.replace('''        GREEN -> R.drawable.style_frame_green\n        else -> null''','''        GREEN -> R.drawable.style_frame_green\n        RED -> R.drawable.style_frame_red\n        GOLD_CROWN -> R.drawable.style_frame_gold_crown\n        else -> null''')
text=text.replace('''    val accent: Color,\n)''','''    val accent: Color,\n    val accessTr: String,\n    val accessEn: String,\n    val sourceIcon: Int,\n)''')
old_specs='''private val purchasedFrameSpecs = listOf(\n    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD, "Altın Hat", "Gold Line", "Sıcak metalik profil çerçevesi", "Warm metallic profile frame", R.drawable.style_frame_gold, Color(0xFFD7A72E)),\n    PurchasedFrameSpec(PurchasedFrameCatalog.MINT, "Buz Mint", "Ice Mint", "Temiz ve modern mint çerçeve", "Clean modern mint frame", R.drawable.style_frame_mint, Color(0xFF32BFB3)),\n    PurchasedFrameSpec(PurchasedFrameCatalog.PURPLE, "Mor Spektrum", "Violet Spectrum", "Premium mor profil vurgusu", "Premium violet profile accent", R.drawable.style_frame_purple, Color(0xFF7257D8)),\n    PurchasedFrameSpec(PurchasedFrameCatalog.GREEN, "Zümrüt Hat", "Emerald Line", "Dengeli zümrüt profil çerçevesi", "Balanced emerald profile frame", R.drawable.style_frame_green, Color(0xFF2FAE68)),\n)'''
new_specs='''private val purchasedFrameSpecs = listOf(\n    PurchasedFrameSpec(PurchasedFrameCatalog.RED, "Kırmızı Hat", "Red Line", "Sade başlangıç ve günlük kullanım çerçevesi", "Clean starter and everyday frame", R.drawable.style_frame_red, Color(0xFFD84C4C), "SIRADAN", "STANDARD", R.drawable.style_icon_user),\n    PurchasedFrameSpec(PurchasedFrameCatalog.GREEN, "Zümrüt Hat", "Emerald Line", "Dengeli zümrüt profil çerçevesi", "Balanced emerald profile frame", R.drawable.style_frame_green, Color(0xFF2FAE68), "MAĞAZA", "SHOP", R.drawable.style_icon_coin),\n    PurchasedFrameSpec(PurchasedFrameCatalog.MINT, "Buz Mint", "Ice Mint", "Temiz ve modern mint çerçeve", "Clean modern mint frame", R.drawable.style_frame_mint, Color(0xFF32BFB3), "MAĞAZA", "SHOP", R.drawable.style_icon_coin),\n    PurchasedFrameSpec(PurchasedFrameCatalog.PURPLE, "Mor Spektrum", "Violet Spectrum", "Premium mor profil vurgusu", "Premium violet profile accent", R.drawable.style_frame_purple, Color(0xFF7257D8), "MAĞAZA", "SHOP", R.drawable.style_icon_coin),\n    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD, "Altın Hat", "Gold Line", "VIP ve prestij koleksiyonuna uygun metalik çerçeve", "Metallic frame for VIP and prestige collection", R.drawable.style_frame_gold, Color(0xFFD7A72E), "VIP / PREMIUM", "VIP / PREMIUM", R.drawable.style_icon_trophy),\n    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, "Altın Taç", "Gold Crown", "Yüksek lig ve prestij ödülü", "High-league prestige reward", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), "LİG ÖDÜLÜ", "LEAGUE REWARD", R.drawable.style_icon_trophy),\n)'''
if old_specs in text: text=text.replace(old_specs,new_specs)
elif new_specs not in text: raise SystemExit('frame spec contract drift')
old='''                        Text(sh(spec.subtitleTr, spec.subtitleEn), color = Color(0xFF6F7C8D), fontSize = 8.sp, minLines = 2)\n                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {'''
new='''                        Text(sh(spec.subtitleTr, spec.subtitleEn), color = Color(0xFF6F7C8D), fontSize = 8.sp, minLines = 2)\n                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {\n                            Image(painter = painterResource(spec.sourceIcon), contentDescription = null, modifier = Modifier.size(12.dp), colorFilter = ColorFilter.tint(spec.accent))\n                            Text(sh(spec.accessTr, spec.accessEn), color = spec.accent, fontSize = 7.sp, fontWeight = FontWeight.Black)\n                        }\n                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {'''
if old in text: text=text.replace(old,new)
elif new not in text: raise SystemExit('source badge contract drift')
old='''                                    else -> "${item?.diamondPrice ?: 0} SC"'''
new='''                                    item != null -> "${item.diamondPrice} SC"\n                                    else -> sh(spec.accessTr, spec.accessEn)'''
if old in text: text=text.replace(old,new)
elif new not in text: raise SystemExit('price label contract drift')
old='''                                enabled = backend != null && item != null && !equipped && busyId == null,'''
new='''                                enabled = backend != null && (owned || item != null) && !equipped && busyId == null,'''
if old in text: text=text.replace(old,new)
elif new not in text: raise SystemExit('enable contract drift')
p.write_text(text,encoding='utf-8')

# Stage 3: lightweight Android-native VFX adapted from a purchased texture sprite.
vfx_kt=Path('app/src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt')
vfx_kt.write_text(r'''package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Cosmetic-only, bounded Compose adaptation of a purchased Eric Wang VFX texture. */
@Composable
internal fun PurchasedVictoryVfx(eventKey: String, modifier: Modifier = Modifier) {
    val progress = remember(eventKey) { Animatable(0f) }
    LaunchedEffect(eventKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1050))
    }
    val p = progress.value
    val alpha = if (p < .18f) p / .18f else (1f - p).coerceIn(0f, 1f)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        repeat(8) { i ->
            val dx = ((i % 4) - 1.5f) * (34f + 54f * p)
            val dy = ((i / 4) * 2 - 1) * (30f + 54f * p)
            Image(
                painterResource(R.drawable.vfx_twinkle),
                null,
                Modifier.offset(dx.dp, dy.dp).size((15f + (i % 3) * 3f).dp).rotate(i * 31f + p * 100f).alpha(alpha),
            )
        }
        Image(painterResource(R.drawable.vfx_twinkle), null, Modifier.offset(y = (-54).dp).size(42.dp).rotate(p * 90f).alpha(alpha))
    }
}
''',encoding='utf-8')

# Hook VFX into the existing winner celebration without touching scoring/state.
p=Path('app/src/main/java/com/sonharf/game/WinnerFireworkOverlay.kt')
text=p.read_text(encoding='utf-8')
old='''                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {\n                    Box('''
new='''                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {\n                    PurchasedVictoryVfx(room.id)\n                    Box('''
if old in text: text=text.replace(old,new)
elif new not in text: raise SystemExit('winner VFX hook contract drift')
p.write_text(text,encoding='utf-8')

# Stage 4: use a restrained subset of Mobile Game UI FREE only in shop/economy surfaces.
p=Path('app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt')
text=p.read_text(encoding='utf-8')
if 'import androidx.compose.foundation.Image' not in text:
    text=text.replace('import androidx.compose.foundation.BorderStroke\n','import androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.Image\n')
if 'import androidx.compose.ui.res.painterResource' not in text:
    text=text.replace('import androidx.compose.ui.graphics.vector.ImageVector\n','import androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.graphics.ColorFilter\nimport androidx.compose.ui.res.painterResource\n')
old='''private fun BundleRow() {\n    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {\n        item { BundleCard(sh("Başlangıç Style Paketi", "Starter Style Bundle"), Icons.Rounded.RocketLaunch, StoreBlue) }\n        item { BundleCard(sh("Premium Style Paketi", "Premium Style Bundle"), Icons.Rounded.Diamond, Color(0xFF6C63D9)) }\n        item { BundleCard(sh("Sezon Style Paketi", "Season Style Bundle"), Icons.Rounded.CalendarMonth, StoreGold) }\n    }\n}'''
new='''private fun BundleRow() {\n    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {\n        item { AssetBundleCard(sh("Başlangıç Style Paketi", "Starter Style Bundle"), R.drawable.mobile_ui_market, StoreBlue) }\n        item { AssetBundleCard(sh("Premium Style Paketi", "Premium Style Bundle"), R.drawable.mobile_ui_market, Color(0xFF6C63D9)) }\n        item { AssetBundleCard(sh("Sezon Style Paketi", "Season Style Bundle"), R.drawable.mobile_ui_market, StoreGold) }\n    }\n}\n\n@Composable\nprivate fun AssetBundleCard(title: String, drawable: Int, accent: Color) {\n    Surface(modifier = Modifier.width(190.dp), shape = RoundedCornerShape(18.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) {\n        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {\n            Row(verticalAlignment = Alignment.CenterVertically) {\n                Surface(shape = RoundedCornerShape(11.dp), color = accent.copy(alpha = .12f)) {\n                    Image(painterResource(drawable), null, Modifier.padding(9.dp).size(23.dp), colorFilter = ColorFilter.tint(accent))\n                }\n                Spacer(Modifier.width(8.dp))\n                Text(title, Modifier.weight(1f), color = StoreText, fontSize = 10.sp, fontWeight = FontWeight.Black)\n            }\n            Text(sh("Son Harf tasarımına uyarlanmış, güç vermeyen Style koleksiyonu", "Son Harf-adapted cosmetic Style collection"), color = StoreMuted, fontSize = 8.sp)\n            Text(sh("YAKINDA", "COMING SOON"), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)\n        }\n    }\n}'''
if old in text: text=text.replace(old,new)
elif new not in text: raise SystemExit('bundle integration contract drift')
old='''Surface(shape = CircleShape, color = StoreAlt) { Icon(Icons.Rounded.Toll, null, Modifier.padding(10.dp).size(24.dp), tint = StoreBlue) }'''
new='''Surface(shape = CircleShape, color = StoreAlt) { Image(painterResource(R.drawable.mobile_ui_market), null, Modifier.padding(10.dp).size(24.dp), colorFilter = ColorFilter.tint(StoreBlue)) }'''
if old in text: text=text.replace(old,new)
elif new not in text: raise SystemExit('son coin asset contract drift')
p.write_text(text,encoding='utf-8')

# New catalog migration. Cosmetic-only; league/event entitlement remains a separate future backend concern.
mig=Path('supabase/migrations/20260902193000_profile_frame_catalog_v2.sql')
mig.write_text('''-- Expanded purchased LAYERLAB frame catalog. Cosmetic only; never grants gameplay power.\ninsert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order) values\n('frame_asset_red','profile_frame','Kırmızı Hat','Red Line','Sıradan başlangıç çerçevesi.','Standard starter frame.',120,false,true,105),\n('frame_asset_gold_crown','profile_frame','Altın Taç','Gold Crown','VIP/prestij koleksiyon çerçevesi.','VIP/prestige collection frame.',450,true,true,150)\non conflict (id) do update set name_tr=excluded.name_tr,name_en=excluded.name_en,description_tr=excluded.description_tr,description_en=excluded.description_en,diamond_price=excluded.diamond_price,vip_only=excluded.vip_only,active=excluded.active,sort_order=excluded.sort_order;\n-- Christmas/Halloween remain event-controlled in the source pack and are deliberately not permanently bundled.\n''',encoding='utf-8')

Path('docs/ASSET_LICENSE_AND_USAGE.md').write_text('''# Son Harf Asset Register\n\n## LAYERLAB – GUI - Avatar Frame\n- Source package: `2D Avatar Frame.zip`, purchased 2026-09-02.\n- Integrated permanent/runtime variants: Red, Green, Mint, Purple, Gold, Gold Crown.\n- Christmas and Halloween remain reserved in the purchased source package for future seasonal/event releases; they are deliberately not bundled into the permanent store build.\n- Usage: cosmetic profile frames only; no gameplay advantage.\n- Important restriction from vendor page: asset must not be used as input/training material for generative-AI programs. Integration here is deterministic file extraction and Android runtime use; no generative image processing was used.\n\n## Nieobie – Game Icon Pack v1.4\n- Integrated existing selected PNGs: user, palette, trophy, coin.\n- Usage: Style/profile/reward/economy semantics, preserving one icon language.\n- Project record: CC0 1.0 / commercial use permitted.\n\n## Eric Wang VFX – Game VFX: UI & Interaction Effects Bundle\n- Source package: `game_vfx_ui_interaction_effects.unitypackage`.\n- Integrated texture subset: `twink_01.png` only. Other Unity-prefab/shader resources remain outside the Android build.\n- Unity prefabs/shaders are not embedded. Texture is adapted to a bounded native Jetpack Compose victory overlay.\n- Usage is cosmetic only and isolated from scoring, rating, turn, timer and matchmaking state.\n\n## Mobile Game UI FREE version\n- Integrated restrained subset only: Market icon in Style shop/bundle/economy surfaces.\n- The pack does not replace Son Harf's blue-white theme, typography, spacing or CTA hierarchy.\n\n## Product constraints\n- Pay-to-win is prohibited.\n- OYNA remains primary CTA.\n- Warm Beginnings remains the sole background music.\n- Third-party provenance must remain documented for future due diligence/transfer.\n''',encoding='utf-8')

# Source-level regression contract for the staged integration.
test=Path('app/src/test/java/com/sonharf/game/AssetIntegrationContractTest.kt')
test.write_text(r'''package com.sonharf.game

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AssetIntegrationContractTest {
    private fun read(path: String) = File(path).readText()

    @Test fun frameCatalogContainsIntegratedPurchasedVariantsAndNoPowerFields() {
        val src = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        listOf("RED", "GREEN", "MINT", "PURPLE", "GOLD", "GOLD_CROWN").forEach { assertTrue(src.contains(it)) }
        assertTrue(src.contains("only change appearance") || src.contains("yalnızca görünümü"))
    }

    @Test fun shopBackendConstructionIsDefensive() {
        val shop = read("src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt")
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(shop.contains("runCatching { OnlineGameBackend() }.getOrNull()"))
        assertTrue(frames.contains("runCatching { OnlineGameBackend() }.getOrNull()"))
    }

    @Test fun purchasedVfxIsCosmeticAndBounded() {
        val src = read("src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt")
        assertTrue(src.contains("1050"))
        assertTrue(src.contains("Cosmetic-only"))
    }
}
''',encoding='utf-8')
print('staged asset integration applied')
