package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.MatchHistoryDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.VipMatchAnalysisDto
import com.sonharf.game.data.getVipMatchAnalysis
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
internal fun VipMatchAnalysisDialog(
    backend: OnlineGameBackend,
    match: MatchHistoryDto,
    onDismiss: () -> Unit,
) {
    var analysis by remember(match.matchId) { mutableStateOf<VipMatchAnalysisDto?>(null) }
    var loading by remember(match.matchId) { mutableStateOf(true) }
    var error by remember(match.matchId) { mutableStateOf<String?>(null) }

    LaunchedEffect(match.matchId, match.mode) {
        loading = true
        error = null
        runCatching { backend.getVipMatchAnalysis(match.matchId, match.mode) }
            .onSuccess { analysis = it }
            .onFailure { failure ->
                error = when {
                    "vip_required" in failure.message.orEmpty() -> sh(
                        "Maç sonrası gelişmiş analiz Son Harf VIP ile açılır.",
                        "Advanced post-match analysis unlocks with Son Harf VIP.",
                    )
                    "completed_match_not_available" in failure.message.orEmpty() -> sh(
                        "Bu maç için doğrulanmış analiz verisi henüz hazır değil.",
                        "Verified analysis data is not available for this match yet.",
                    )
                    else -> sh("Analiz yüklenemedi.", "Analysis could not be loaded.")
                }
            }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Analytics, contentDescription = null, tint = MainUi.Blue)
                Column {
                    Text(sh("MAÇ SONRASI ANALİZ", "POST-MATCH ANALYSIS"), fontWeight = FontWeight.Black)
                    Text(match.displayName, color = MainUi.Muted, fontSize = 10.sp)
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    sh(
                        "Yalnız tamamlanmış maç verileri kullanılır. Ranked maç sırasında karar desteği veya güç avantajı verilmez.",
                        "Only completed-match data is used. No live ranked decision support or gameplay power is provided.",
                    ),
                    color = MainUi.Green,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )

                when {
                    loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue)
                    error != null -> Text(error.orEmpty(), color = MainUi.Red, fontWeight = FontWeight.Bold)
                    analysis != null -> VipAnalysisContent(analysis!!)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black) }
        },
    )
}

@Composable
private fun VipAnalysisContent(analysis: VipMatchAnalysisDto) {
    val finalScore = analysis.scoreBreakdown["final_score"]?.jsonPrimitive?.intOrNull
    val opponentScore = analysis.scoreBreakdown["opponent_final_score"]?.jsonPrimitive?.intOrNull

    if (finalScore != null && opponentScore != null) {
        VipAnalysisMetric(
            icon = Icons.Rounded.Analytics,
            title = sh("Skor özeti", "Score summary"),
            value = "$finalScore - $opponentScore",
        )
    }

    analysis.bestWord?.takeIf { it.isNotBlank() }?.let {
        VipAnalysisMetric(Icons.Rounded.AutoAwesome, sh("En değerli kelime", "Best word"), it)
    }
    analysis.longestWord?.takeIf { it.isNotBlank() }?.let {
        VipAnalysisMetric(Icons.Rounded.TextFields, sh("En uzun kelime", "Longest word"), it)
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VipAnalysisSmallMetric(analysis.wordCount.toString(), sh("Kelime", "Words"), Modifier.weight(1f))
        VipAnalysisSmallMetric(
            analysis.avgWordLength?.let { String.format("%.1f", it) } ?: "—",
            sh("Ort. uzunluk", "Avg length"),
            Modifier.weight(1f),
        )
        VipAnalysisSmallMetric(analysis.highestMoveScore?.toString() ?: "—", sh("En iyi hamle", "Best move"), Modifier.weight(1f))
    }

    if (analysis.fastestResponseMs != null || analysis.avgResponseMs != null || analysis.slowestResponseMs != null) {
        VipAnalysisMetric(
            icon = Icons.Rounded.Schedule,
            title = sh("Yanıt süresi", "Response time"),
            value = buildString {
                analysis.fastestResponseMs?.let { append("${sh("en hızlı", "fastest")} ${formatMs(it)}") }
                analysis.avgResponseMs?.let {
                    if (isNotEmpty()) append(" • ")
                    append("${sh("ortalama", "average")} ${formatMs(it)}")
                }
                analysis.slowestResponseMs?.let {
                    if (isNotEmpty()) append(" • ")
                    append("${sh("en yavaş", "slowest")} ${formatMs(it)}")
                }
            },
        )
    }

    if (analysis.mode == "siege") {
        VipAnalysisMetric(
            icon = Icons.Rounded.Dashboard,
            title = sh("Alan hareketi", "Territory movement"),
            value = sh(
                "+${analysis.territoryGained} kazanılan • -${analysis.territoryLost} rakibe kaybedilen küp",
                "+${analysis.territoryGained} gained • -${analysis.territoryLost} cubes lost to opponent",
            ),
        )
    }

    if (analysis.criticalTimeResponses > 0) {
        Text(
            sh(
                "Kritik zaman diliminde ${analysis.criticalTimeResponses} geçerli yanıt.",
                "${analysis.criticalTimeResponses} valid responses in the critical time window.",
            ),
            color = MainUi.Muted,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun VipAnalysisMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MainUi.Surface,
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MainUi.Blue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MainUi.Muted, fontSize = 8.5.sp)
                Text(value, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VipAnalysisSmallMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = MainUi.BlueSoft) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(label, color = MainUi.Muted, fontSize = 7.5.sp)
        }
    }
}

private fun formatMs(value: Int): String = if (value >= 1000) {
    String.format("%.1fs", value / 1000.0)
} else {
    "${value}ms"
}
