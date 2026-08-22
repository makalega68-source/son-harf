from pathlib import Path

path = Path('app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt')
text = path.read_text()
needle = '            ComboOverlayV9()\n'
replacement = '            ComboOverlayV9()\n            BilBakalimBonusOverlay()\n'
if replacement not in text:
    if needle not in text:
        raise SystemExit('ComboOverlayV9 mount point not found')
    text = text.replace(needle, replacement, 1)
path.write_text(text)
print('Bil Bakalim bonus overlay mounted')
