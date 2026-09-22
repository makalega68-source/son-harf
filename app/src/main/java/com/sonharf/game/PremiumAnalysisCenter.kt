package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.VipMatchAnalysisDto
import com.sonharf.game.data.VipRecentCompletedMatchDto
import com.sonharf.game.data.getVipMatchAnalysis
import com.sonharf.game.data.getVipRecentCompletedMatches
import kotlinx.coroutines.launch
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
internal fun PremiumAnalysisCenterLauncher(modifier: Modifier = Modifier) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf<List<VipRecentCompletedMatchDto>>(emptyList()) }
    var selected by remember { mutableStateOf<VipRecentCompletedMatchDto?>(null) }
    var analysis by remember { mutableStateOf<VipMatchAnalysisDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun openCenter() {
        show = true
        selected = null
        analysis = null
        error = null
        val b = backend
        if (b == null) {
            error = "offline"
            return
        }
        scope.launch {
            loading = true
            runCatching { b.getVipRecentCompletedMatches() }
                .onSuccess { matches = it }
                .onFailure { error = it.message.orEmpty() }
            loading = false
        }
    }

    fun openAnalysis(match: VipRecentCompletedMatchDto) {
        selected = match
        analysis = null
        error = null
        val b = backend ?: return
        scope.launch {
            loading = true
            runCatching { b.getVipMatchAnalysis(match.matchId, match.mode) }
                .onSuccess { analysis = it }
                .onFailure { error = it.message.orEmpty() }
            loading = false
        }
    }

    Button(
        onClick = { openCenter() },
        modifier = modifier.height(50.dp),
        shape = GameShapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = GameColors.Lavender,
            contentColor = GameColors.TextPrimary,
        ),
    ) {
        Icon(Icons.Rounded.Analytics, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            sh("MAÇ ANALİZİ", "MATCH ANALYSIS"),
            fontWeight = FontWeight.Black,
        )
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = {
                Text(
                    if (selected == null) {
                        sh("Premium Maç Analizi", "Premium Match Analysis")
                    } else {
                        sh("Maç Sonu Analizi", "Post-match Analysis")
                    },
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            },
            text = {
                when {
                    loading -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = GameColors.Lavender)
                    }
                    error.orEmpty().contains("vip_required") -> PremiumLockedAnalysisCopy()
                    error != null -> Text(
                        sh(
                            "Analiz verileri yüklenemedi.",
                            "Analysis data could not be loaded.",
                        ),
                        color = GameColors.TextSecondary,
                    )
                    selected != null && analysis != null -> PremiumCenterAnalysisBody(analysis!!)
                    selected == null -> PremiumCompletedMatchList(
                        matches = matches,
                        onSelect = ::openAnalysis,
                    )
                    else -> Text(
                        sh("Analiz kaydı bulunamadı.", "Analysis record was not found."),
                        color = GameColors.TextSecondary,
                    )
                }
            },
            confirmButton = {
                if (selected != null) {
                    TextButton(
                        onClick = {
                            selected = null
                            analysis = null
                            error = null
                        },
                    ) {
                        Text(
                            sh("MAÇLAR", "MATCHES"),
                            color = GameColors.PrimaryBlue,
                            fontWeight = FontWeight.Black,
                        )
                    }
                } else {
                    TextButton(onClick = { show = false }) {
                        Text(
                            sh("KAPAT", "CLOSE"),
                            color = GameColors.PrimaryBlue,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            },
            dismissButton = if (selected != null) {
                {
                    TextButton(onClick = { show = false }) {
                        Text(
                            sh("KAPAT", "CLOSE"),
                            color = GameColors.TextSecondary,
                        )
                    }
                }
            } else {
                null
            },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
        )
    }
}

@Composable
private fun PremiumCompletedMatchList(
    matches: List<VipRecentCompletedMatchDto>,
    onSelect: (VipRecentCompletedMatchDto) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(
            shape = GameShapes.Small,
            color = GameColors.PlayGreen.copy(alpha = .08f),
            border = BorderStroke(1.dp, GameColors.PlayGreen.copy(alpha = .26f)),
        ) {
            Text(
                sh(
                    "Yalnız tamamlanmış maçlar. Premium canlı maç sırasında hiçbir taktik bilgi avantajı vermez.",
                    "Completed matches only. Premium never gives a live tactical-information advantage.",
                ),
                modifier = Modifier.fillMaxWidth().padding(9.dp),
                color = GameColors.PlayGreen,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        if (matches.isEmpty()) {
            GameEmptyState(
                icon = Icons.Rounded.Analytics,
                title = sh("Analiz edilecek maç yok", "No match to analyze"),
                body = sh(
                    "Tamamlanan maçların burada listelenecek.",
                    "Your completed matches will appear here.",
                ),
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(matches, key = { "${it.mode}:${it.matchId}" }) { match ->
                    Surface(
                        onClick = { onSelect(match) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = GameShapes.Medium,
                        color = GameColors.SecondarySurface,
                        border = BorderStroke(1.dp, GameColors.Border),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    modeLabel(match.mode),
                                    color = GameColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    sh("Tamamlanmış maç", "Completed match"),
                                    color = GameColors.TextTertiary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = GameColors.Lavender,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumLockedAnalysisCopy() {
    GameSurface(
        borderColor = GameColors.Lavender.copy(alpha = .34f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = GameColors.Lavender,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                sh(
                    "Maç-sonu derin analiz Premium özelliğidir.",
                    "Deep post-match analysis is a Premium feature.",
                ),
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            sh(
                "Adil rekabet: Premium kelime önerisi, alan önizlemesi, ek süre, rating veya canlı hamle avantajı vermez.",
                "Fair play: Premium gives no word suggestions, territory preview, extra time, rating, or live-move advantage.",
            ),
            color = GameColors.PlayGreen,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun PremiumCenterAnalysisBody(analysis: VipMatchAnalysisDto) {
    val score = analysis.scoreBreakdown
    val wordScore = score["word_score"]?.jsonPrimitive?.intOrNull
    val areaScore = score["area_score"]?.jsonPrimitive?.intOrNull
    val totalScore = score["total_score"]?.jsonPrimitive?.intOrNull
    val finalScore = score["final_score"]?.jsonPrimitive?.intOrNull

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            sh("Yalnız tamamlanmış maç verileri", "Completed-match data only"),
            color = GameColors.PlayGreen,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )

        if (
            analysis.mode == "siege" &&
            wordScore != null &&
            areaScore != null &&
            totalScore != null
        ) {
            Surface(
                shape = GameShapes.Medium,
                color = GameColors.SecondarySurface,
                border = BorderStroke(1.dp, GameColors.Border),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CenterMetric(sh("KELİME", "WORD"), wordScore.toString())
                    CenterMetric(sh("BÖLGE", "TERRITORY"), areaScore.toString())
                    CenterMetric(sh("TOPLAM", "TOTAL"), totalScore.toString())
                }
            }
        } else if (finalScore != null) {
            CenterLine(sh("Nihai skor", "Final score"), finalScore.toString())
        }

        if (!analysis.bestWord.isNullOrBlank()) {
            CenterLine(sh("En iyi kelime", "Best word"), analysis.bestWord)
        }
        if (!analysis.longestWord.isNullOrBlank()) {
            CenterLine(sh("En uzun kelime", "Longest word"), analysis.longestWord)
        }
        CenterLine(sh("Kelime sayısı", "Word count"), analysis.wordCount.toString())
        analysis.averageResponseMs?.let {
            CenterLine(sh("Ort. yanıt", "Avg. response"), "$it ms")
        }
        analysis.highestMoveScore?.let {
            CenterLine(sh("En yüksek hamle", "Highest move"), it.toString())
        }
        if (analysis.mode == "siege") {
            CenterLine(
                sh("Ele geçirilen alan", "Territory gained"),
                analysis.territoryGained.toString(),
            )
            CenterLine(
                sh("Kaybedilen alan", "Territory lost"),
                analysis.territoryLost.toString(),
            )
        }
    }
}

@Composable
private fun CenterMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = GameColors.TextTertiary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun CenterLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = GameColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    "siege" -> sh("Kelime Kuşatması", "Word Siege")
    "arena" -> sh("Kelime Düellosu", "Word Duel")
    else -> sh("Son Harf", "Last Letter")
}
