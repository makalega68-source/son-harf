from pathlib import Path

p = Path('app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt')
s = p.read_text()

old_header = '''        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = SonHarfTheme.PrimarySoft) {
                Icon(Icons.Rounded.GridView, null, tint = SonHarfTheme.Primary, modifier = Modifier.padding(8.dp).size(19.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("KOLEKSİYON", "COLLECTION"), color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(sh("Yalnızca sahip olduğun ürünleri burada yönet.", "Manage only the items you own here."), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
            }
            if (loading || busyId != null) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SonHarfTheme.Primary)
        }'''
new_header = '''        PurchasedPanel(
            modifier = Modifier.fillMaxWidth(),
            asset = PurchasedUiAsset.PANEL_MEDIUM,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.ICON_GAMES, Modifier.size(38.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("KOLEKSİYON", "COLLECTION"), color = Color(0xFF4A2D20), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(sh("Yalnızca sahip olduğun ürünleri burada yönet.", "Manage only the items you own here."), color = Color(0xFF765746), fontSize = 10.sp)
                }
                if (loading || busyId != null) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SonHarfTheme.Primary)
            }
        }'''
if old_header not in s: raise SystemExit('collection header missing')
s = s.replace(old_header, new_header, 1)

old_empty = '''            Surface(shape = MainUiShape.Control, color = SonHarfTheme.SurfaceSecondary) {
                Text(
                    sh("Mağazadan satın aldığın diğer ürünler burada kategorileri altında görünür.", "Other items you purchase from the shop will appear here under their categories."),
                    Modifier.fillMaxWidth().padding(14.dp),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 11.sp,
                )
            }'''
new_empty = '''            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.NAV_SHOP, Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        sh("Mağazadan satın aldığın diğer ürünler burada kategorileri altında görünür.", "Other items you purchase from the shop will appear here under their categories."),
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF765746),
                        fontSize = 10.sp,
                    )
                }
            }'''
if old_empty not in s: raise SystemExit('collection empty panel missing')
s = s.replace(old_empty, new_empty, 1)

old_notice = '''            Surface(shape = MainUiShape.Control, color = SonHarfTheme.PrimarySoft) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontSize = 11.sp)
                    TextButton(onClick = { scope.launch { reloadCollection() } }, enabled = busyId == null) {
                        Text(sh("YENİLE", "REFRESH"), color = SonHarfTheme.Primary, fontWeight = FontWeight.Black)
                    }
                }
            }'''
new_notice = '''            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.REWARD_PANEL,
                contentPadding = PaddingValues(11.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(28.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(message, Modifier.weight(1f), color = Color(0xFF4A2D20), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    PurchasedButton(
                        text = sh("YENİLE", "REFRESH"),
                        onClick = { scope.launch { reloadCollection() } },
                        enabled = busyId == null,
                        modifier = Modifier.width(108.dp),
                        style = PurchasedButtonStyle.SECONDARY,
                        leadingAsset = PurchasedUiAsset.ICON_REPEAT,
                    )
                }
            }'''
if old_notice not in s: raise SystemExit('collection notice missing')
s = s.replace(old_notice, new_notice, 1)

start = s.index('@Composable\nprivate fun ActiveStyleSummary(')
end = s.index('@Composable\nprivate fun DefaultPremiumThemeTile(', start)
new_helpers = r'''@Composable
private fun ActiveStyleSummary(theme: String, frame: String, keyboard: String) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(13.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(30.dp))
                Spacer(Modifier.width(7.dp))
                Text(sh("AKTİF GÖRÜNÜM", "ACTIVE LOOK"), color = Color(0xFF4A2D20), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SummaryChip(Icons.Rounded.Palette, theme, SonHarfTheme.Purple, Modifier.weight(1f))
                SummaryChip(Icons.Rounded.AccountCircle, frame, SonHarfTheme.Primary, Modifier.weight(1f))
                SummaryChip(Icons.Rounded.Keyboard, keyboard, SonHarfTheme.Turquoise, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryChip(icon: ImageVector, text: String, accent: Color, modifier: Modifier) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 76.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, color = Color(0xFF4A2D20), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CollectionCategoryBlock(title: String, subtitle: String, icon: ImageVector, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.ICON_GAMES, Modifier.size(38.dp))
                Spacer(Modifier.width(7.dp))
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(title, color = Color(0xFF4A2D20), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = Color(0xFF765746), fontSize = 9.sp)
                }
            }
            content()
        }
    }
}

'''
s = s[:start] + new_helpers + s[end:]

old_shell = '''@Composable
private fun CollectionTileShell(active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = SonHarfTheme.Surface,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) SonHarfTheme.Turquoise else SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}'''
new_shell = '''@Composable
private fun CollectionTileShell(active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    PurchasedPanel(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        asset = if (active) PurchasedUiAsset.REWARD_PANEL else PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(9.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}'''
if old_shell not in s: raise SystemExit('collection tile shell missing')
s = s.replace(old_shell, new_shell, 1)

old_check = '''@Composable
private fun BoxScope.ActiveCheck(accent: Color) {
    Surface(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), shape = RoundedCornerShape(99.dp), color = accent) {
        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp).size(13.dp))
    }
}'''
new_check = '''@Composable
private fun BoxScope.ActiveCheck(accent: Color) {
    PurchasedAsset(
        PurchasedUiAsset.ICON_CHECK,
        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(28.dp),
    )
}'''
if old_check not in s: raise SystemExit('active check missing')
s = s.replace(old_check, new_check, 1)

for token in [
    'backend.getInventory()',
    'backend.getOwnedShopItems(nextOwned)',
    'backend.getEquippedCosmetics()',
    'backend.equipDefaultGameTheme()',
    'backend.equipShopItem(itemId)',
    'SonHarfCosmetics.applyAndPersist(context, next, owned)',
    'PurchasedUiAsset.PANEL_LARGE',
    'PurchasedUiAsset.REWARD_PANEL',
    'PurchasedButton(',
]:
    if token not in s:
        raise SystemExit('settings collection contract missing: ' + token)

p.write_text(s)

Path('app/src/test/java/com/sonharf/game/PurchasedSettingsCollectionContractTest.kt').write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedSettingsCollectionContractTest {
    @Test fun ownedCollectionUsesPurchasedPanelsWithoutChangingOwnershipFlow() {
        val source = File("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()
        assertTrue(source.contains("PurchasedUiAsset.PANEL_MEDIUM"))
        assertTrue(source.contains("PurchasedUiAsset.PANEL_LARGE"))
        assertTrue(source.contains("PurchasedUiAsset.REWARD_PANEL"))
        assertTrue(source.contains("PurchasedButton("))
        assertTrue(source.contains("backend.getInventory()"))
        assertTrue(source.contains("backend.getOwnedShopItems(nextOwned)"))
        assertTrue(source.contains("backend.getEquippedCosmetics()"))
        assertTrue(source.contains("backend.equipDefaultGameTheme()"))
        assertTrue(source.contains("backend.equipShopItem(itemId)"))
        assertTrue(source.contains("SonHarfCosmetics.applyAndPersist(context, next, owned)"))
        val summary = source.substringAfter("private fun ActiveStyleSummary(").substringBefore("private fun DefaultPremiumThemeTile(")
        assertFalse(summary.contains("Surface("))
        val tile = source.substringAfter("private fun CollectionTileShell(").substringBefore("private fun BoxScope.ActiveCheck")
        assertTrue(tile.contains("PurchasedPanel("))
        assertFalse(tile.contains("Surface("))
    }
}
''')
print('Patched purchased settings collection shell.')
