from pathlib import Path

home = Path('app/src/main/java/com/sonharf/game/PremiumMasterHome.kt')
text = home.read_text(encoding='utf-8')
old = '''                Text("👑", fontSize = 33.sp)\n                Text("SON", color = MasterGold, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)\n                Text("HARF", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, lineHeight = 42.sp)'''
new = '''                SonHarfBrandLogo(\n                    modifier = Modifier.padding(top = 2.dp, bottom = 3.dp),\n                    size = 146.dp,\n                )'''
if old not in text:
    raise SystemExit('Expected SON/HARF header block not found; refusing unsafe patch')
text = text.replace(old, new, 1)
home.write_text(text, encoding='utf-8')

# Guard: approved logo must remain the only source for the main Son Harf card.
updated = home.read_text(encoding='utf-8')
assert 'SonHarfBrandLogo(' in updated
assert 'Text("SON", color = MasterGold' not in updated
assert 'Text("HARF", color = Color.White' not in updated
