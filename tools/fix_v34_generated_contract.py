from pathlib import Path

path = Path('app/src/test/java/com/sonharf/game/WordSiegePracticeUxAndBotRegressionTest.kt')
text = path.read_text()
lines = text.splitlines()
replaced = 0
for index, line in enumerate(lines):
    if 'screen.contains' in line and 'SOHBET' in line:
        lines[index] = '        assertTrue(screen.contains("SOHBET"))'
        replaced += 1
if replaced != 1:
    raise SystemExit(f'Expected one SOHBET contract line, found {replaced}')
path.write_text('\n'.join(lines) + '\n')
print('V34 generated contract normalized.')
