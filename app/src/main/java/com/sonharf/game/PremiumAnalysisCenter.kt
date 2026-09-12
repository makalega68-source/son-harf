package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
    ) {
        Text(sh("MAÇ ANALİZİ", "MATCH ANALYSIS"), fontWeight = FontWeight.Black)
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = {
                Text(
                    if (selected == null) sh("Premium Maç Analizi", "Premium Match Analysis") else sh("Maç Sonu Analizi", "Post-match Analysis"),
                    color = SonHarfText,
                    fontWeight = FontWeight.Black,
                )
            },
            text = {
                when {
                    loading -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SonHarfPurple)
                    }
                    error.orEmpty().contains("vip_required") -> PremiumLockedAnalysisCopy()
                    error != null -> Text(sh("Analiz verileri yüklenemedi.", "Analysis data could not be loaded."), color = SonHarfMuted)
                    selected != null && analysis != null -> PremiumCenterAnalysisBody(analysis!!)
                    selected == null -> PremiumCompletedMatchList(matches = matches, onSelect = ::openAnalysis)
                    else -> Text(sh("Analiz kaydı bulunamadı.", "Analysis record was not found."), color = SonHarfMuted)
                }
            },
            confirmButton = {
                if (selected != null) {
                    TextButton(onClick = { selected = null; analysis = null; error = null }) {
                        Text(sh("MAÇLAR", "MATCHES"), fontWeight = FontWeight.Black)
                    }
                } else {
                    TextButton(onClick = { show = false }) { Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black) }
                }
            },
            dismissButton = if (selected != null) {
                { TextButton(onClick = { show = false }) { Text(sh("KAPAT", "CLOSE")) } }
            } else null,
            containerColor = SonHarfSurface,
        )
    }
}

@Composable
private fun PremiumCompletedMatchList(
    matches: List<VipRecentCompletedMatchDto>,
    onSelect: (VipRecentCompletedMatchDto) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            sh(
                "Yalnız tamamlanmış maçlar. Premium canlı maç sırasında hiçbir taktik bilgi avantajı vermez.",
                "Completed matches only. Premium never gives a live tactical-information advantage.",
            ),
            color = SonHarfGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        if (matches.isEmpty()) {
            Text(sh("Analiz edilebilir tamamlanmış maç yok.", "No completed match is available for analysis."), color = SonHarfMuted)
        } else {
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(matches, key = { "${it.mode}:${it.matchId}" }) { match ->
                    Surface(
                        onClick = { onSelect(match) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SonHarfSurface2,
                        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .15f)),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(modeLabel(match.mode), color = SonHarfText, fontWeight = FontWeight.Black)
                                Text(sh("Tamamlanmış maç", "Completed match"), color = SonHarfMuted, fontSize = 9.sp)
                            }
                            Text("›", color = SonHarfPurple, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumLockedAnalysisCopy() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(sh("Maç-sonu derin analiz Premium özelliğidir.", "Deep post-match analysis is a Premium feature."), color = SonHarfText, fontWeight = FontWeight.Bold)
        Text(
            sh(
                "Adil rekabet: Premium kelime önerisi, alan önizlemesi, ek süre, rating veya canlı hamle avantajı vermez.",
                "Fair play: Premium gives no word suggestions, territory preview, extra time, rating, or live-move advantage.",
            ),
            color = SonHarfGreen,
            fontSize = 10.sp,
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
        Text(sh("Yalnız tamamlanmış maç verileri", "Completed-match data only"), color = SonHarfGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        if (analysis.mode == "siege" && wordScore != null && areaScore != null && totalScore != null) {
            Surface(shape = RoundedCornerShape(14.dp), color = SonHarfSurface2) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CenterMetric(sh("KELİME", "WORD"), wordScore.toString())
                    CenterMetric(sh("BÖLGE", "TERRITORY"), areaScore.toString())
                    CenterMetric(sh("TOPLAM", "TOTAL"), totalScore.toString())
                }
            }
        } else if (finalScore != null) {
            CenterLine(sh("Nihai skor", "Final score"), finalScore.toString())
        }
        if (!analysis.bestWord.isNullOrBlank()) CenterLine(sh("En iyi kelime", "Best word"), analysis.bestWord)
        if (!analysis.longestWord.isNullOrBlank()) CenterLine(sh("En uzun kelime", "Longest word"), analysis.longestWord)
        CenterLine(sh("Kelime sayısı", "Word count"), analysis.wordCount.toString())
        analysis.averageResponseMs?.let { CenterLine(sh("Ort. yanıt", "Avg. response"), "${it} ms") }
        analysis.highestMoveScore?.let { CenterLine(sh("En yüksek hamle", "Highest move"), it.toString()) }
        if (analysis.mode == "siege") {
            CenterLine(sh("Ele geçirilen alan", "Territory gained"), analysis.territoryGained.toString())
            CenterLine(sh("Kaybedilen alan", "Territory lost"), analysis.territoryLost.toString())
        }
    }
}

@Composable
private fun CenterMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SonHarfMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = SonHarfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CenterLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SonHarfMuted, fontSize = 11.sp)
        Text(value, color = SonHarfText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    "siege" -> sh("Kelime Kuşatması", "Word Siege")
    "arena" -> sh("Kelime Düellosu", "Word Duel")
    else -> sh("Son Harf", "Last Letter")
}
