package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.sonharf.game.data.getVipMatchAnalysis
import kotlinx.coroutines.launch
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Retrospective Premium analysis. This component deliberately refuses to render outside terminal
 * match state so Premium never becomes a live tactical-assist surface.
 */
@Composable
internal fun PremiumPostMatchAnalysisEntry(
    matchId: String,
    mode: String,
    terminal: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!terminal) return

    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var show by remember(matchId, mode) { mutableStateOf(false) }
    var loading by remember(matchId, mode) { mutableStateOf(false) }
    var analysis by remember(matchId, mode) { mutableStateOf<VipMatchAnalysisDto?>(null) }
    var error by remember(matchId, mode) { mutableStateOf<String?>(null) }

    fun load() {
        val b = backend
        show = true
        if (b == null) {
            error = "offline"
            return
        }
        scope.launch {
            loading = true
            error = null
            runCatching { b.getVipMatchAnalysis(matchId, mode) }
                .onSuccess { analysis = it }
                .onFailure { error = it.message.orEmpty() }
            loading = false
        }
    }

    OutlinedButton(
        onClick = { load() },
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .55f)),
    ) {
        Text(sh("PREMIUM MAÇ ANALİZİ", "PREMIUM MATCH ANALYSIS"), color = SonHarfPurple, fontWeight = FontWeight.Black)
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = {
                Text(sh("Maç Sonu Analizi", "Post-match Analysis"), color = SonHarfText, fontWeight = FontWeight.Black)
            },
            text = {
                when {
                    loading -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SonHarfPurple)
                    }
                    analysis != null -> PremiumAnalysisBody(analysis!!)
                    error.orEmpty().contains("vip_required") -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sh("Bu retrospektif analiz Premium özelliğidir.", "This retrospective analysis is a Premium feature."), color = SonHarfText, fontWeight = FontWeight.Bold)
                        Text(
                            sh(
                                "Adil rekabet korunur: Premium canlı maçta kelime, hamle, alan önizlemesi, süre veya rating avantajı vermez.",
                                "Fair play is preserved: Premium gives no live word, move, territory preview, time, or rating advantage.",
                            ),
                            color = SonHarfGreen,
                            fontSize = 11.sp,
                        )
                    }
                    error.orEmpty().contains("completed_match_not_available") -> Text(
                        sh("Bu maç için Premium analiz kaydı kullanılamıyor.", "Premium analysis is unavailable for this match."),
                        color = SonHarfMuted,
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sh("Analiz şu anda yüklenemedi.", "Analysis could not be loaded right now."), color = SonHarfMuted)
                        Button(
                            onClick = { load() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
                        ) { Text(sh("TEKRAR DENE", "RETRY"), fontWeight = FontWeight.Black) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { show = false }) { Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black) }
            },
            containerColor = SonHarfSurface,
        )
    }
}

@Composable
private fun PremiumAnalysisBody(analysis: VipMatchAnalysisDto) {
    val score = analysis.scoreBreakdown
    val wordScore = score["word_score"]?.jsonPrimitive?.intOrNull
    val areaScore = score["area_score"]?.jsonPrimitive?.intOrNull
    val totalScore = score["total_score"]?.jsonPrimitive?.intOrNull
    val finalScore = score["final_score"]?.jsonPrimitive?.intOrNull

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            sh("Yalnız tamamlanmış maç verileri • canlı taktik yardım yok", "Completed-match data only • no live tactical assistance"),
            color = SonHarfGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (analysis.mode == "siege" && wordScore != null && areaScore != null && totalScore != null) {
            Surface(shape = RoundedCornerShape(14.dp), color = SonHarfSurface2) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AnalysisMetric(sh("KELİME", "WORD"), wordScore.toString())
                    AnalysisMetric(sh("BÖLGE", "TERRITORY"), areaScore.toString())
                    AnalysisMetric(sh("TOPLAM", "TOTAL"), totalScore.toString())
                }
            }
        } else if (finalScore != null) {
            AnalysisMetric(sh("NİHAİ SKOR", "FINAL SCORE"), finalScore.toString())
        }
        if (!analysis.bestWord.isNullOrBlank()) AnalysisLine(sh("En iyi kelime", "Best word"), analysis.bestWord)
        if (!analysis.longestWord.isNullOrBlank()) AnalysisLine(sh("En uzun kelime", "Longest word"), analysis.longestWord)
        AnalysisLine(sh("Kelime sayısı", "Word count"), analysis.wordCount.toString())
        analysis.averageResponseMs?.let { AnalysisLine(sh("Ort. yanıt", "Avg. response"), "${it} ms") }
        analysis.highestMoveScore?.let { AnalysisLine(sh("En yüksek hamle", "Highest move"), it.toString()) }
        if (analysis.mode == "siege") {
            AnalysisLine(sh("Ele geçirilen alan", "Territory gained"), analysis.territoryGained.toString())
            AnalysisLine(sh("Kaybedilen alan", "Territory lost"), analysis.territoryLost.toString())
        }
    }
}

@Composable
private fun AnalysisMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SonHarfMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = SonHarfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AnalysisLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SonHarfMuted, fontSize = 11.sp)
        Spacer(Modifier.padding(horizontal = 5.dp))
        Text(value, color = SonHarfText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}
