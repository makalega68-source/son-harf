from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, got {count}")
    p.write_text(text.replace(old, new, 1))


home = "app/src/main/java/com/sonharf/game/PremiumHomeV3.kt"
replace_once(
    home,
    '''                    Image(
                        painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                        contentDescription = sh("Kelime Kuşatması", "Kelime Kuşatması"),
                        modifier = Modifier.size(width = 158.dp, height = 104.dp),
                        contentScale = ContentScale.Fit,
                    )''',
    '''                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = .12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .18f)),
                    ) {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = SonHarfTheme.PremiumGoldLight,
                            modifier = Modifier.padding(18.dp).size(42.dp),
                        )
                    }''',
)
replace_once(home, '            logo = R.drawable.son_harf_app_icon_master,', '            icon = Icons.Rounded.Bolt,')
replace_once(home, '            logo = R.drawable.harf_yolu_logo,', '            icon = Icons.Rounded.Route,')
replace_once(home, '    logo: Int,\n    title: String,', '    icon: androidx.compose.ui.graphics.vector.ImageVector,\n    title: String,')
replace_once(
    home,
    '''                Surface(shape = CircleShape, color = Color.White.copy(alpha = .18f)) {
                    Image(
                        painter = painterResource(logo),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(48.dp),
                        contentScale = ContentScale.Fit,
                    )
                }''',
    '''                Surface(shape = CircleShape, color = Color.White.copy(alpha = .18f)) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(12.dp).size(40.dp),
                    )
                }''',
)

ladder = "app/src/main/java/com/sonharf/game/LetterLadderGame.kt"
replace_once(
    ladder,
    'import androidx.compose.material.icons.rounded.Refresh\n',
    'import androidx.compose.material.icons.rounded.Refresh\nimport androidx.compose.material.icons.rounded.Route\n',
)
replace_once(
    ladder,
    '''                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.harf_yolu_logo),
                            contentDescription = sh("Harf Yolu logosu", "Letter Path logo"),
                            modifier = Modifier.width(116.dp).height(54.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )''',
    '''                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LetterLadderUi.SurfaceSoft,
                            border = BorderStroke(1.dp, LetterLadderUi.Accent.copy(alpha = .20f)),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Route, null, tint = LetterLadderUi.Accent, modifier = Modifier.size(23.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    sh("HARF YOLU", "LETTER PATH"),
                                    color = LetterLadderUi.Text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = .5.sp,
                                )
                            }
                        }''',
)
replace_once(
    ladder,
    '''            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.harf_yolu_logo),
                contentDescription = sh("Harf Yolu logosu", "Letter Path logo"),
                modifier = Modifier.size(106.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )''',
    '''            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LetterLadderUi.SurfaceSoft,
                border = BorderStroke(1.dp, LetterLadderUi.Accent.copy(alpha = .22f)),
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.Route, null, tint = LetterLadderUi.Accent, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(5.dp))
                    Text("A → B", color = LetterLadderUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }''',
)

siege = "app/src/main/java/com/sonharf/game/WordSiegeExperience.kt"
replace_once(
    siege,
    'Text(sh("KELİME TAHTI", "KELİME TAHTI"), color = MainUi.Text, fontSize = 23.sp, fontWeight = FontWeight.Black)',
    'Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 23.sp, fontWeight = FontWeight.Black)',
)

sources = {
    "entry": Path("app/src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").read_text(),
    "home": Path(home).read_text(),
    "siege": Path(siege).read_text(),
    "ladder": Path(ladder).read_text(),
    "last": Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").read_text(),
}
forbidden = [
    "R.drawable.kelime_kusatma_logo_hd",
    "R.drawable.harf_yolu_logo",
    "R.drawable.son_harf_app_icon_master",
]
for name, source in sources.items():
    for token in forbidden:
        if token in source:
            raise SystemExit(f"{name}: forbidden rendered game-logo reference remains: {token}")

Path("app/src/test/java/com/sonharf/game/LogoFreeGameSurfacesRegressionTest.kt").write_text(
    '''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoFreeGameSurfacesRegressionTest {
    @Test
    fun activeGameSurfacesUseTypographyAndFunctionalIconsInsteadOfLogoArtwork() {
        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val siege = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val ladder = File("src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val lastLetter = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val visibleSources = listOf(entries, home, siege, ladder, lastLetter)
        visibleSources.forEach { source ->
            assertFalse(source.contains("R.drawable.kelime_kusatma_logo_hd"))
            assertFalse(source.contains("R.drawable.harf_yolu_logo"))
            assertFalse(source.contains("R.drawable.son_harf_app_icon_master"))
        }
        assertTrue(entries.contains("PremiumSiegeEntryScreen"))
        assertTrue(entries.contains("PremiumLastLetterEntryScreen"))
        assertTrue(entries.contains("PremiumLetterPathEntryScreen"))
        assertTrue(siege.contains("KELİME KUŞATMASI"))
        assertTrue(ladder.contains("Icons.Rounded.Route"))
    }
}
'''
)

duel = sources["last"]
for token in [
    "PremierHistoryDrawer(words, language)",
    "OYNANAN KELİMELER",
    "PLAYED WORDS",
    "words.takeLast(20).reversed()",
]:
    if token not in duel:
        raise SystemExit(f"Son Harf played-word visibility contract missing: {token}")
if "PremierHistoryDrawer(words, language, isPro)" in duel:
    raise SystemExit("Son Harf history is still gated behind Pro")
