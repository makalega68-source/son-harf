from pathlib import Path

path = Path('app/src/main/java/com/sonharf/game/LetterLadderGame.kt')
text = path.read_text()
old = '''                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.harf_yolu_logo),
                            contentDescription = sh("Harf Yolu logosu", "Letter Path logo"),
                            modifier = Modifier.width(116.dp).height(54.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )'''
new = '''                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.harf_yolu_logo),
                            contentDescription = sh("Harf Yolu logosu", "Letter Path logo"),
                            modifier = Modifier.width(116.dp).height(54.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(LetterLadderUi.AccentStrong),
                        )'''
assert old in text, 'Harf Yolu logo block not found'
text = text.replace(old, new, 1)
assert 'ColorFilter.tint(LetterLadderUi.AccentStrong)' in text
path.write_text(text)
