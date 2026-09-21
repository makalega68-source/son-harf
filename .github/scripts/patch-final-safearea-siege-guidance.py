from pathlib import Path

# 1) Keep the custom purchased bottom navigation above Android's navigation bar.
home = Path('app/src/main/java/com/sonharf/game/PremiumAdultApp.kt')
s = home.read_text()
old = '        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),\n        asset = PurchasedUiAsset.PANEL_LARGE,'
new = '        modifier = Modifier\n            .fillMaxWidth()\n            .navigationBarsPadding()\n            .heightIn(min = 88.dp),\n        asset = PurchasedUiAsset.PANEL_LARGE,'
if old not in s:
    raise SystemExit('AdultBottomBar purchased panel anchor not found')
s = s.replace(old, new, 1)
home.write_text(s)

# 2) Replace the remaining legacy Material guidance/dialog shells in Siege practice.
guidance = Path('app/src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt')
g = guidance.read_text()
if 'import androidx.compose.ui.window.Dialog' not in g:
    g = g.replace('import androidx.compose.ui.unit.sp\n', 'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.window.Dialog\n')

zone_start = g.index('@Composable\ninternal fun WordSiegePracticeZoneInfoDialog(')
tutorial_start = g.index('@Composable\ninternal fun WordSiegePracticeTutorialCard(')
status_start = g.index('@Composable\ninternal fun WordSiegePracticeStatusBar(')

zone = '''@Composable
internal fun WordSiegePracticeZoneInfoDialog(
    code: String,
    onDismiss: () -> Unit,
) {
    val turkish = !SonHarfUiState.isEnglish
    Dialog(onDismissRequest = onDismiss) {
        PurchasedPanel(
            modifier = Modifier.fillMaxWidth(),
            asset = PurchasedUiAsset.PANEL_MEDIUM,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PurchasedSectionHeader(
                    title = WordSiegeBoardSpec.bonusLongName(code, turkish),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    wordSiegePracticeZoneExplanation(code, turkish),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF654A3D),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                PurchasedButton(
                    text = if (turkish) "ANLADIM" else "GOT IT",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.PRIMARY,
                )
            }
        }
    }
}

'''

tutorial = '''@Composable
internal fun WordSiegePracticeTutorialCard(
    step: Int,
    compact: Boolean,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    val turkish = !SonHarfUiState.isEnglish
    val title = when (step) {
        0 -> if (turkish) "KELİME KUŞATMASI NASIL OYNANIR?" else "HOW TO PLAY WORD SIEGE"
        1 -> if (turkish) "1/4 • BİR HARF SEÇ" else "1/4 • PICK A TILE"
        2 -> if (turkish) "2/4 • TAHTAYA YERLEŞTİR" else "2/4 • PLACE IT"
        3 -> if (turkish) "3/4 • KELİMEYİ TAMAMLA" else "3/4 • COMPLETE THE WORD"
        else -> if (turkish) "4/4 • SIRA RAKİBİNDE" else "4/4 • YOUR RIVAL PLAYS"
    }
    val body = when (step) {
        0 -> if (turkish) {
            "Harf seç, tahtaya yerleştir ve kelimeni onayla. Sonraki kelimeler tahtadaki harflere yatay veya dikey bağlanır. En yüksek toplam puan kazanır."
        } else {
            "Pick tiles, place your word and confirm. Connect later words horizontally or vertically to letters on the board. Highest total score wins."
        }
        1 -> if (turkish) {
            "Alttaki raftan bir harfe dokun. Seçilen harf belirginleşecek."
        } else {
            "Tap a tile in the rack below. The selected tile will highlight."
        }
        2 -> if (turkish) {
            "Şimdi boş bir hücreye dokun. İlk kelimen merkezden geçmeli."
        } else {
            "Now tap an empty cell. Your first word must cross the center."
        }
        3 -> if (turkish) {
            "Harfleri aynı satır veya sütunda kelime olacak şekilde tamamla; sonra HAMLEYİ ONAYLA."
        } else {
            "Complete a word in one row or column, then tap CONFIRM MOVE."
        }
        else -> if (turkish) {
            "Harf bonusu yalnız harfi, kelime bonusu tüm kelimeyi çarpar. +25 ödülü hamlene eklenir. Skordaki bölge puanı mevcut maç kuralıdır: sahip olduğun hücre başına 2 puan; kelime puanı kalıcıdır."
        } else {
            "Letter bonuses multiply one letter; word bonuses multiply the word. +25 adds to the move. Existing matches also award 2 points per owned cell; word points are permanent."
        }
    }

    PurchasedPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = if (compact) 10.dp else 12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PurchasedSectionHeader(
                title = title,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                body,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF654A3D),
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PurchasedButton(
                    text = if (turkish) "ATLA" else "SKIP",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    style = PurchasedButtonStyle.SECONDARY,
                )
                when (step) {
                    0 -> PurchasedButton(
                        text = if (turkish) "BAŞLA" else "START",
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        style = PurchasedButtonStyle.PRIMARY,
                    )
                    4 -> PurchasedButton(
                        text = if (turkish) "ANLADIM" else "GOT IT",
                        onClick = onFinish,
                        modifier = Modifier.weight(1f),
                        style = PurchasedButtonStyle.PRIMARY,
                    )
                    else -> PurchasedPanel(
                        modifier = Modifier.weight(1f),
                        asset = PurchasedUiAsset.PANEL_SMALL,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (turkish) "Ekrandaki adımı yap" else "Complete the step on screen",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF765746),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

'''

status = '''@Composable
internal fun WordSiegePracticeStatusBar(
    message: String,
    compact: Boolean,
) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF654A3D),
            fontSize = if (compact) 8.sp else 9.sp,
            lineHeight = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
'''

g = g[:zone_start] + zone + tutorial + status
if 'KELİME TAHTI NASIL OYNANIR?' in g or 'HOW TO PLAY WORD THRONE' in g:
    raise SystemExit('Legacy Siege tutorial branding still present')
if 'AlertDialog(' in g or 'TextButton(' in g or '\n                    0 -> Button(' in g:
    raise SystemExit('Legacy Material guidance control still present')
guidance.write_text(g)

# Regression contract: visual fixes only, no game/backend mutations.
test = Path('app/src/test/java/com/sonharf/game/PurchasedFinalSafeAreaSiegeGuidanceContractTest.kt')
test.write_text('''package com.sonharf.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PurchasedFinalSafeAreaSiegeGuidanceContractTest {
    @Test
    fun purchasedBottomNavigationRespectsAndroidNavigationInset() {
        val source = File("src/main/java/com/sonharf/game/PremiumAdultApp.kt").readText()
        assertTrue(source.contains(".navigationBarsPadding()"))
        assertTrue(source.contains("PurchasedNavItem("))
    }

    @Test
    fun siegePracticeGuidanceUsesPurchasedShellAndCurrentBrand() {
        val source = File("src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt").readText()
        assertTrue(source.contains("KELİME KUŞATMASI NASIL OYNANIR?"))
        assertTrue(source.contains("HOW TO PLAY WORD SIEGE"))
        assertTrue(source.contains("PurchasedPanel("))
        assertTrue(source.contains("PurchasedButton("))
        assertTrue(source.contains("Dialog(onDismissRequest = onDismiss)"))
        assertFalse(source.contains("KELİME TAHTI NASIL OYNANIR?"))
        assertFalse(source.contains("HOW TO PLAY WORD THRONE"))
        assertFalse(source.contains("AlertDialog("))
    }
}
''')

print('Patched final Home safe-area and Siege practice guidance surfaces.')
