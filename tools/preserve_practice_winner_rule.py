from pathlib import Path

p = Path('app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt')
t = p.read_text()
new = '''        val winner = forcedWinner ?: when {
            totalScore(state, 1) > totalScore(state, 2) -> 1
            totalScore(state, 2) > totalScore(state, 1) -> 2
            state.playerArea > state.botArea -> 1
            state.botArea > state.playerArea -> 2
            else -> null
        }'''
old = '''        val winner = forcedWinner ?: when {
            state.playerArea > state.botArea -> 1
            state.botArea > state.playerArea -> 2
            else -> null
        }'''
if new not in t:
    raise SystemExit('expected temporary winner change not found')
p.write_text(t.replace(new, old, 1))
