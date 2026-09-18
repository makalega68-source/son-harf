from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, got {count}\nOLD={old[:180]!r}")
    p.write_text(text.replace(old, new, 1))


# --- Kelime Kuşatması: the existing lobby becomes the only entry screen. ---
siege = "app/src/main/java/com/sonharf/game/WordSiegeExperience.kt"
replace_once(
    siege,
    '''                notice = notice,
                onBack = onExit,
                onRefresh = { scope.launch { refreshGames(showProgress = true) } },''',
    '''                notice = notice,
                language = SonHarfUiState.language,
                onLanguageChange = { SonHarfUiState.language = it },
                onBack = onExit,
                onRefresh = { scope.launch { refreshGames(showProgress = true) } },''',
)
replace_once(
    siege,
    '''    notice: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,''',
    '''    notice: String?,
    language: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,''',
)
replace_once(
    siege,
    '''                        sh("Süre yok • 1v1 • En fazla 10 devam eden oyun", "No timer • 1v1 • Up to 10 ongoing games"),''',
    '''                        sh("Kelime kur • alan ele geçir", "Build words • claim territory"),''',
)
replace_once(
    siege,
    '''                        Text(sh("TAKTİK ALAN SAVAŞI", "TACTICAL TERRITORY BATTLE"), color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(sh("Kelime kur • bölge ele geçir • haritayı yönet", "Build words • capture territory • control the map"), color = MainUi.Muted, fontSize = 9.sp)''',
    '''                        Text(sh("ALAN SAVAŞI", "TERRITORY BATTLE"), color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(sh("Kelime kur. Bölgeyi al.", "Build a word. Take territory."), color = MainUi.Muted, fontSize = 9.sp)''',
)
anchor = '''        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    sh("OYUN SEÇ", "CHOOSE A GAME"),'''
insert = '''        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Language, null, tint = MainUi.Blue, modifier = Modifier.size(21.dp))
                    Text(sh("OYUN DİLİ", "GAME LANGUAGE"), color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    listOf("tr" to "TR", "en" to "EN").forEach { (code, label) ->
                        val selected = language == code
                        Surface(
                            onClick = { onLanguageChange(code) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MainUi.Blue else MainUi.SurfaceSoft,
                            border = BorderStroke(1.dp, if (selected) MainUi.Blue else MainUi.Border),
                        ) {
                            Text(
                                label,
                                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (selected) Color.White else MainUi.Text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    sh("OYUN SEÇ", "CHOOSE A GAME"),'''
replace_once(siege, anchor, insert)
replace_once(
    siege,
    '''                            Text(sh("İlk kuşatmanı kur", "Build your first siege"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                sh("Bonuslar sadece yeni harfte çalışır; rakibin karesini kelimene katarsan alan sana geçer.", "Bonuses work on new tiles; use a rival tile in your word to capture its territory."),
                                color = MainUi.Muted,
                                fontSize = 10.sp,
                            )''',
    '''                            Text(sh("İlk kuşatmanı başlat", "Start your first siege"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                sh("Kelime kur ve rakibin bölgesini ele geçir.", "Build words and capture rival territory."),
                                color = MainUi.Muted,
                                fontSize = 10.sp,
                            )''',
)

# --- Son Harf: simplify the real lobby; its existing TR/EN switch remains authoritative. ---
duel = "app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt"
replace_once(
    duel,
    '''                Text(pt(language, "PREMIER 1v1 DÜELLO", "PREMIER 1v1 DUEL"), color = PremierUi.Ocean, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)''',
    '''                Text(pt(language, "Son harften yeni kelime üret", "Build a new word from the last letter"), color = PremierUi.Ocean, fontSize = 10.sp, fontWeight = FontWeight.Bold)''',
)
replace_once(
    duel,
    '''                    pt(language, "Kelimeyi sürdür, rakibini geç.", "Keep the word chain alive. Outplay your rival."),''',
    '''                    pt(language, "Son harften yeni kelime üret.", "Build a new word from the last letter."),''',
)
replace_once(
    duel,
    '''                    pt(language, "Sunucu doğrulamalı ana sözlük • 3 round • kişi başı 10 kelime", "Server-verified master dictionary • 3 rounds • 10 words each"),''',
    '''                    pt(language, "3 round • kişi başı 10 kelime", "3 rounds • 10 words each"),''',
)
replace_once(
    duel,
    '''            PremierFeatureTile(Icons.Rounded.Verified, pt(language, "ANA SÖZLÜK", "MASTER DICTIONARY"), pt(language, "TR + EN", "TR + EN"), Modifier.weight(1f))
            PremierFeatureTile(Icons.Rounded.Bolt, pt(language, "HIZLI", "FAST"), pt(language, "15→13→11 sn", "15→13→11 sec"), Modifier.weight(1f))
            PremierFeatureTile(Icons.Rounded.Groups, pt(language, "CANLI", "LIVE"), "1v1", Modifier.weight(1f))''',
    '''            PremierFeatureTile(Icons.Rounded.Looks3, pt(language, "3 ROUND", "3 ROUNDS"), pt(language, "TOPLAM", "TOTAL"), Modifier.weight(1f))
            PremierFeatureTile(Icons.Rounded.Spellcheck, pt(language, "10 + 10", "10 + 10"), pt(language, "KELİME", "WORDS"), Modifier.weight(1f))
            PremierFeatureTile(Icons.Rounded.Bolt, "15→13→11", pt(language, "SANİYE", "SECONDS"), Modifier.weight(1f))''',
)
replace_once(
    duel,
    '''        Text(pt(language, "Rakip bulunamazsa seviye uyumlu bot devreye girer.", "If no rival is found, a level-appropriate bot takes over."), Modifier.fillMaxWidth(), color = PremierUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)''',
    '''        Text(pt(language, "Rakip yoksa uygun botla hemen başlarsın.", "If no rival is found, a suitable bot starts the match."), Modifier.fillMaxWidth(), color = PremierUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)''',
)

# --- Harf Yolu: no separate intro; add compact language selector to the actual game screen. ---
ladder = "app/src/main/java/com/sonharf/game/LetterLadderGame.kt"
replace_once(
    ladder,
    '''                            sh("5 hamle • Her kutu yalnızca 1 kez değişir", "5 moves • Each position changes only once"),''',
    '''                            sh("Bir harfi değiştir • hedefe ulaş", "Change one letter • reach the target"),''',
)
old = '''                    Surface(shape = RoundedCornerShape(99.dp), color = LetterLadderUi.Gold.copy(alpha = .16f)) {
                        Text(
                            "${usedPositions.size}/5",
                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = LetterLadderUi.Gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }'''
new = '''                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf("tr" to "TR", "en" to "EN").forEach { (code, label) ->
                            val selected = language == code
                            Surface(
                                onClick = { SonHarfUiState.language = code },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) LetterLadderUi.Accent else LetterLadderUi.SurfaceSoft,
                                border = BorderStroke(1.dp, if (selected) LetterLadderUi.Accent else LetterLadderUi.Border),
                            ) {
                                Text(
                                    label,
                                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    color = if (selected) Color.White else LetterLadderUi.Text,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = LetterLadderUi.Gold.copy(alpha = .16f)) {
                            Text(
                                "${usedPositions.size}/5",
                                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                color = LetterLadderUi.Gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }'''
replace_once(ladder, old, new)

# --- Regression contracts for the one-screen architecture. ---
Path("app/src/test/java/com/sonharf/game/GameEntryArenaRegressionTest.kt").write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEntryArenaRegressionTest {
    @Test
    fun everyGameRoutesDirectlyToItsOnlyPlayableEntrySurface() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val duel = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(shell.contains("SIEGE_ENTRY"))
        assertFalse(shell.contains("LAST_LETTER_ENTRY"))
        assertFalse(shell.contains("LETTER_PATH_ENTRY"))
        assertFalse(shell.contains("PremiumSiegeEntryScreen"))
        assertFalse(shell.contains("PremiumLastLetterEntryScreen"))
        assertFalse(shell.contains("PremiumLetterPathEntryScreen"))
        assertTrue(shell.contains("onPrimary = { openGame(PremiumDestination.SIEGE) }"))
        assertTrue(shell.contains("onLastLetter = { openGame(PremiumDestination.LAST_LETTER) }"))
        assertTrue(shell.contains("onLetterPath = { openGame(PremiumDestination.LETTER_PATH) }"))

        assertTrue(siege.contains("onLanguageChange = { SonHarfUiState.language = it }"))
        assertTrue(siege.contains("Kelime kur • alan ele geçir"))
        assertTrue(duel.contains("PremierLanguageSwitch(language, onLanguage)"))
        assertTrue(duel.contains("Son harften yeni kelime üret"))
        assertTrue(ladder.contains("SonHarfUiState.language = code"))
        assertTrue(ladder.contains("Bir harfi değiştir • hedefe ulaş"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
''')

Path("app/src/test/java/com/sonharf/game/GameModeBrandingAndVisibleActionContractTest.kt").write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeBrandingAndVisibleActionContractTest {
    @Test
    fun homeRoutesEveryModeDirectlyAndKeepsLogoFreeIdentity() {
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()

        assertTrue(home.contains("Text(\"KELİME KUŞATMASI\""))
        assertTrue(home.contains("Button(onClick = onSiege"))
        assertTrue(home.contains("Icons.Rounded.GridView"))
        assertTrue(home.contains("Icons.Rounded.Bolt"))
        assertTrue(home.contains("Icons.Rounded.Route"))
        assertTrue(shell.contains("openGame(PremiumDestination.SIEGE)"))
        assertTrue(shell.contains("openGame(PremiumDestination.LAST_LETTER)"))
        assertTrue(shell.contains("openGame(PremiumDestination.LETTER_PATH)"))
        assertFalse(shell.contains("_ENTRY"))
        assertFalse(shell.contains("PremiumGameCenter("))
        assertFalse(home.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertFalse(home.contains("R.drawable.son_harf_app_icon_master"))
        assertFalse(home.contains("R.drawable.harf_yolu_logo"))
    }

    @Test
    fun letterPathKeepsGameplaySuccessVfxAndUsesItsActualGameScreenAsEntry() {
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        assertTrue(ladder.contains("successVfxNonce += 1"))
        assertTrue(ladder.contains("PurchasedVictoryVfx("))
        assertTrue(ladder.contains("eventKey = \"letter:${'$'}{puzzle?.id}:${'$'}successVfxNonce\""))
        assertTrue(ladder.contains("SonHarfUiState.language = code"))
        assertFalse(ladder.contains("rememberInfiniteTransition"))
        assertFalse(ladder.contains("R.drawable.harf_yolu_logo"))
    }

    @Test
    fun tactilePressFeedbackCannotCollapseBackToInvisibleOnePercentMotion() {
        val motion = projectFile("app/src/main/java/com/sonharf/game/SonHarfMicroMotion.kt").readText()
        assertTrue(motion.contains("pressedScale.coerceAtMost(0.955f)"))
        assertTrue(motion.contains("waitForUpOrCancellation()"))
        assertFalse(motion.contains("infiniteRepeatable"))
    }

    @Test
    fun purchasedSuccessVfxHasReadableRingAndStillDoesNotCaptureInput() {
        val vfx = projectFile("app/src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt").readText()
        assertTrue(vfx.contains("PurchasedWordSuccessGreen"))
        assertTrue(vfx.contains("30f + 58f * p"))
        assertTrue(vfx.contains("drawCircle("))
        assertTrue(vfx.contains("R.drawable.vfx_twinkle"))
        assertFalse(vfx.contains("pointerInput"))
        assertFalse(vfx.contains("clickable"))
        assertFalse(vfx.contains("infiniteRepeatable"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
''')

Path("app/src/test/java/com/sonharf/game/LogoFreeGameSurfacesRegressionTest.kt").write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoFreeGameSurfacesRegressionTest {
    @Test
    fun activeGameSurfacesUseTypographyAndFunctionalIconsInsteadOfLogoArtwork() {
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val lastLetter = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        listOf(home, siege, ladder, lastLetter).forEach { source ->
            assertFalse(source.contains("R.drawable.kelime_kusatma_logo_hd"))
            assertFalse(source.contains("R.drawable.harf_yolu_logo"))
            assertFalse(source.contains("R.drawable.son_harf_app_icon_master"))
        }
        assertTrue(home.contains("Icons.Rounded.GridView"))
        assertTrue(home.contains("Icons.Rounded.Bolt"))
        assertTrue(home.contains("Icons.Rounded.Route"))
        assertTrue(siege.contains("KELİME KUŞATMASI"))
        assertTrue(ladder.contains("Icons.Rounded.Route"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
''')

# The shared intro file is obsolete in the one-screen architecture.
entry = Path("app/src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt")
if entry.exists():
    entry.unlink()
