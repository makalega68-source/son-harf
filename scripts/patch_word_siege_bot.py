from pathlib import Path
p=Path('app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt')
t=p.read_text()
old='import java.util.Locale'
new='import java.util.Locale\nimport kotlin.random.Random'
if t.count(old)!=1: raise SystemExit('import mismatch')
t=t.replace(old,new,1)
old='''    fun bestBotMove(state: WordSiegePracticeState): WordSiegePracticeMove? {
        if (state.currentOwner != 2 || state.status != "playing") return null
        val rack = state.botRack
        var best: WordSiegePracticeMove? = null
        botWords.forEach { word ->
            listOf(true, false).forEach { horizontal ->
                (0..80).forEach startLoop@{ start ->
                    val placements = placementsForWord(state, rack, word, start, horizontal) ?: return@startLoop
                    val candidate = runCatching { validateMove(state, 2, placements, horizontal) }.getOrNull() ?: return@startLoop
                    if (best == null || candidate.wordScore + candidate.capturedCells * 2 > best!!.wordScore + best!!.capturedCells * 2) best = candidate
                }
            }
        }
        return best
    }'''
new='''    fun bestBotMove(
        state: WordSiegePracticeState,
        difficulty: TrainingBotDifficulty = TrainingBotDifficulty.MEDIUM,
        random: Random = Random.Default,
    ): WordSiegePracticeMove? {
        if (state.currentOwner != 2 || state.status != "playing") return null
        val rack = state.botRack
        val candidates = mutableListOf<Pair<WordSiegePracticeMove, Int>>()
        val seen = mutableSetOf<String>()
        botWords.forEach { word ->
            listOf(true, false).forEach { horizontal ->
                (0..80).forEach startLoop@{ start ->
                    val placements = placementsForWord(state, rack, word, start, horizontal) ?: return@startLoop
                    val key = "${horizontal}:${placements.entries.sortedBy { it.key }}"
                    if (!seen.add(key)) return@startLoop
                    val candidate = runCatching { validateMove(state, 2, placements, horizontal) }.getOrNull() ?: return@startLoop
                    candidates += candidate to botCandidateScore(state, candidate)
                }
            }
        }
        if (candidates.isEmpty()) return null
        val ranked = candidates.sortedByDescending { it.second }
        val window = when (difficulty) {
            TrainingBotDifficulty.EASY -> minOf(6, ranked.size)
            TrainingBotDifficulty.MEDIUM -> minOf(3, ranked.size)
            TrainingBotDifficulty.HARD -> minOf(2, ranked.size)
        }
        val pick = when (difficulty) {
            TrainingBotDifficulty.EASY -> random.nextInt(window)
            TrainingBotDifficulty.MEDIUM -> if (window == 1 || random.nextInt(100) < 65) 0 else random.nextInt(window)
            TrainingBotDifficulty.HARD -> if (window == 1 || random.nextInt(100) < 85) 0 else 1
        }
        return ranked[pick].first
    }

    internal fun botCandidateScore(state: WordSiegePracticeState, move: WordSiegePracticeMove): Int {
        var bonusValue = 0
        var centerValue = 0
        move.placements.forEach { (index, _) ->
            val cell = state.board[index]
            if (!cell.bonusUsed) bonusValue += when (cell.bonus) {
                "3K" -> 8; "2K" -> 5; "3H" -> 4; "2H" -> 2; else -> 0
            }
            val row = index / 9
            val col = index % 9
            centerValue += 4 - maxOf(kotlin.math.abs(row - 4), kotlin.math.abs(col - 4))
        }
        return move.wordScore * 4 + move.capturedCells * 11 + bonusValue + centerValue
    }'''
if t.count(old)!=1: raise SystemExit('bot target mismatch')
p.write_text(t.replace(old,new,1))
