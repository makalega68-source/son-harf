from pathlib import Path

path = Path('app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt')
text = path.read_text()

needle = '            ComboOverlayV9()\n'
replacement = '            ComboOverlayV9()\n            BilBakalimBonusOverlay()\n'
if replacement not in text:
    if needle not in text:
        raise SystemExit('ComboOverlayV9 mount point not found')
    text = text.replace(needle, replacement, 1)

route_old = 'ClassicScreen.BIL_BAKALIM -> BilBakalimStandaloneScreen { screen = ClassicScreen.HOME }'
route_new = 'ClassicScreen.BIL_BAKALIM -> TrackedBilBakalimStandaloneScreen { screen = ClassicScreen.HOME }'
if route_new not in text:
    if route_old not in text:
        raise SystemExit('Bil Bakalim route not found')
    text = text.replace(route_old, route_new, 1)

path.write_text(text)
print('Bil Bakalim bonus overlay mounted and standalone tracking enabled')
