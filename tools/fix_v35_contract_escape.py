from pathlib import Path

p = Path('app/src/test/java/com/sonharf/game/WordSiegeReadabilityV35ContractTest.kt')
text = p.read_text(encoding='utf-8')
text = text.replace('bag.contains("Torbada $total harf")', 'bag.contains("Torbada \\$total harf")')
text = text.replace('bag.contains("×$count")', 'bag.contains("×\\$count")')
p.write_text(text, encoding='utf-8')
print('V35 contract interpolation escapes fixed.')
