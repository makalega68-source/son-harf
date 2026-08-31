package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeMoveDto
import com.sonharf.game.data.WordSiegeMovePreviewDto

data class WordSiegeLiveStats(
    val totalTilesPlayed: Int,
    val myTilesPlayed: Int,
    val myWordCount: Int,
    val myArea: Int,
)

internal fun wordSiegeLiveStats(
    game: WordSiegeGameDto,
    moves: List<WordSiegeMoveDto>,
    me: String?,
): WordSiegeLiveStats {
    val mine = moves.filter { it.playerId == me }
    val owner = if (me == game.playerOneId) 1 else 2
    return WordSiegeLiveStats(
        totalTilesPlayed = moves.sumOf { it.placedTiles.size },
        myTilesPlayed = mine.sumOf { it.placedTiles.size },
        myWordCount = mine.sumOf { it.formedWords.size },
        myArea = if (owner == 1) game.playerOneArea else game.playerTwoArea,
    )
}

internal fun wordSiegeLetterDistribution(letters: Iterable<String>): String =
    letters
        .flatMap { it.toList() }
        .groupingBy { it }
        .eachCount()
        .toSortedMap(compareBy<Char> { it.toString() })
        .entries
        .joinToString("  ") { "${it.key}×${it.value}" }

internal fun wordSiegeUsedLetterDistribution(moves: List<WordSiegeMoveDto>): String =
    wordSiegeLetterDistribution(moves.flatMap { move -> move.placedTiles.map { it.letter } })

@Composable
internal fun WordSiegeMoveAnalysisBar(
    preview: WordSiegeMovePreviewDto?,
    placementsPresent: Boolean,
    vip: Boolean,
    game: WordSiegeGameDto,
    moves: List<WordSiegeMoveDto>,
    me: String?,
    tight: Boolean,
) {
    val valid = preview?.valid == true
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (tight) 10.dp else 12.dp),
        color = if (vip) SiegePurpleSoft else MainUi.BlueSoft,
        border = BorderStroke(1.dp, if (vip) SiegePurple.copy(alpha = .25f) else MainUi.Blue.copy(alpha = .22f)),
    ) {
        if (!vip) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = if (tight) 5.dp else 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(sh("HAMLE", "MOVE"), color = MainUi.Muted, fontSize = if (tight) 7.sp else 8.sp, fontWeight = FontWeight.Black)
                Text(
                    if (valid) "+${preview?.totalScore ?: 0}" else if (placementsPresent) "—" else "+0",
                    color = if (valid) MainUi.Blue else MainUi.Muted,
                    fontSize = if (tight) 11.sp else 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            return@Surface
        }

        val stats = wordSiegeLiveStats(game, moves, me)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = if (tight) 4.dp else 6.dp),
            verticalArrangement = Arrangement.spacedBy(if (tight) 2.dp else 3.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sh("HAMLE ANALİZİ • VIP", "MOVE ANALYSIS • VIP"), color = SiegePurple, fontSize = if (tight) 7.sp else 8.sp, fontWeight = FontWeight.Black)
                Text(
                    if (valid) "+${preview?.totalScore ?: 0}" else "—",
                    color = if (valid) MainUi.Text else MainUi.Muted,
                    fontSize = if (tight) 10.sp else 12.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AnalysisMetric(sh("Kelime", "Word"), if (valid) preview?.baseWordScore ?: 0 else null, tight)
                AnalysisMetric(sh("Alan", "Area"), if (valid) preview?.areaScore ?: 0 else null, tight)
                AnalysisMetric(sh("Bonus", "Bonus"), if (valid) preview?.bonusScore ?: 0 else null, tight)
                AnalysisMetric(sh("Toplam", "Total"), if (valid) preview?.totalScore ?: 0 else null, tight, bold = true)
            }
            if (!tight) {
                Text(
                    sh(
                        "Oynanan taş ${stats.totalTilesPlayed} • Sen ${stats.myTilesPlayed} • Kelime ${stats.myWordCount} • Alan ${stats.myArea} • Torba ${game.bag.length}",
                        "Tiles played ${stats.totalTilesPlayed} • You ${stats.myTilesPlayed} • Words ${stats.myWordCount} • Area ${stats.myArea} • Bag ${game.bag.length}",
                    ),
                    color = MainUi.Muted,
                    fontSize = 7.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AnalysisMetric(label: String, value: Int?, tight: Boolean, bold: Boolean = false) {
    Text(
        "$label ${value?.let { "+$it" } ?: "—"}",
        color = if (value == null) MainUi.Muted else MainUi.Text,
        fontSize = if (tight) 7.sp else 8.sp,
        fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
    )
}

@Composable
internal fun WordSiegeVipFinishedAnalysis(
    game: WordSiegeGameDto,
    moves: List<WordSiegeMoveDto>,
    me: String?,
) {
    val myMoves = moves.filter { it.playerId == me }
    val owner = if (me == game.playerOneId) 1 else 2
    val myWordScore = if (owner == 1) game.playerOneWordScore else game.playerTwoWordScore
    val myArea = if (owner == 1) game.playerOneArea else game.playerTwoArea
    val best = myMoves.maxByOrNull { it.wordScore }
    val valuable = myMoves.maxByOrNull { it.wordScore + it.placedTiles.size + it.capturedCells }
    val used = wordSiegeUsedLetterDistribution(moves).ifBlank { "—" }
    val remaining = wordSiegeLetterDistribution(game.bag.map { it.toString() }).ifBlank { "—" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SiegePurpleSoft,
        border = BorderStroke(1.dp, SiegePurple.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(sh("VIP MAÇ ANALİZİ", "VIP MATCH ANALYSIS"), color = SiegePurple, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(
                sh("Kelime $myWordScore • Alan $myArea • ${myMoves.sumOf { it.formedWords.size }} kelime", "Word $myWordScore • Area $myArea • ${myMoves.sumOf { it.formedWords.size }} words"),
                color = MainUi.Text,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
            )
            best?.let {
                Text(sh("En yüksek: ${it.primaryWord} +${it.wordScore}", "Highest: ${it.primaryWord} +${it.wordScore}"), color = MainUi.Text, fontSize = 8.sp)
            }
            valuable?.let {
                val total = it.wordScore + it.placedTiles.size + it.capturedCells
                Text(sh("En değerli hamle: ${it.primaryWord} +$total", "Most valuable move: ${it.primaryWord} +$total"), color = MainUi.Text, fontSize = 8.sp)
            }
            Text(sh("Kullanılan harfler: $used", "Used letters: $used"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 2)
            Text(sh("Kalan harfler: $remaining", "Remaining letters: $remaining"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 2)
            Text(
                sh("Kalan harf dağılımı yalnızca maç bittikten sonra gösterilir.", "Remaining-letter distribution is shown only after the match ends."),
                color = Color(0xFF6D5BB3),
                fontSize = 6.5.sp,
            )
        }
    }
}
