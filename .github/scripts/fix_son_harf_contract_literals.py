from pathlib import Path

path = Path('app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt')
text = path.read_text(encoding='utf-8')
text = text.replace(
    'assertTrue(screen.contains("Raund ${room.roundNo} / 3"))',
    'assertTrue(screen.contains("Raund \\${room.roundNo} / 3"))',
)
text = text.replace(
    'assertTrue(screen.contains("Text(\\"$myRounds - $rivalRounds\\""))',
    'assertTrue(screen.contains("Text(\\"\\$myRounds - \\$rivalRounds\\""))',
)
path.write_text(text, encoding='utf-8')
print('Escaped Son Harf regression-test source literals')
