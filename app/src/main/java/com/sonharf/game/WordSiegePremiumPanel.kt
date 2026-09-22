package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeLetterCountDto
import com.sonharf.game.data.WordSiegePlacement
import com.sonharf.game.data.WordSiegePremiumPreviewDto
import com.sonharf.game.data.getPremiumWordSiegeLetterTable
import com.sonharf.game.data.getVipEntitlements
import com.sonharf.game.data.previewPremiumWordSiegeMove

/**
 * Premium in-match tools. Score preview and letter counts are server-authoritative and
 * entitlement-gated again on the RPC side, so UI state cannot unlock paid data by itself.
 * Transport failures are kept distinct from a real "locked" entitlement to avoid making
 * purchased features look unowned during a temporary backend problem.
 */
@Composable
internal fun WordSiegePremiumPanel(
    game: WordSiegeGameDto,
    placements: Map<Int, Int>,
    canAct: Boolean,
) {
    val backend = remember { OnlineGameBackend() }
    var entitlements by remember(game.id) { mutableStateOf<VipEntitlementsDto?>(null) }
    var entitlementError by remember(game.id) { mutableStateOf(false) }
    var entitlementRetry by remember(game.id) { mutableIntStateOf(0) }
    var preview by remember(game.id) { mutableStateOf<WordSiegePremiumPreviewDto?>(null) }
    var previewError by remember(game.id) { mutableStateOf(false) }
    var letterTable by remember(game.id) { mutableStateOf<List<WordSiegeLetterCountDto>>(emptyList()) }
    var letterTableError by remember(game.id) { mutableStateOf(false) }
    var showLetterTable by remember(game.id) { mutableStateOf(false) }

    LaunchedEffect(game.id, game.moveCount, entitlementRetry) {
        entitlementError = false
        runCatching { backend.getVipEntitlements() }
            .onSuccess { next ->
                entitlements = next
                if (next.letterTableAccess) {
                    letterTableError = false
                    runCatching { backend.getPremiumWordSiegeLetterTable(game.id) }
                        .onSuccess { letterTable = it }
                        .onFailure {
                            letterTable = emptyList()
                            letterTableError = true
                        }
                } else {
                    letterTable = emptyList()
                    letterTableError = false
                }
            }
            .onFailure {
                // Preserve the last known entitlement instead of downgrading a paid user to "locked".
                entitlementError = true
            }
    }

    val access = entitlements

    LaunchedEffect(game.id, game.moveCount, placements, canAct, access?.scoreCalculatorAccess) {
        preview = null
        previewError = false
        if (!canAct || placements.isEmpty() || access?.scoreCalculatorAccess != true) return@LaunchedEffect
        val orientation = runCatching { WordSiegeFinalRules.detectOrientation(game.board, placements.keys) }.getOrNull()
            ?: return@LaunchedEffect
        val request = placements.entries
            .sortedBy { it.key }
            .map { WordSiegePlacement(index = it.key, rackIndex = it.value) }
        runCatching {
            backend.previewPremiumWordSiegeMove(
                gameId = game.id,
                placements = request,
                horizontal = orientation == WordSiegeOrientation.HORIZONTAL,
            )
        }.onSuccess {
            preview = it
        }.onFailure {
            previewError = true
        }
    }

    if (access == null || entitlementError) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = WordSiegeGameUi.PremiumSurface,
            border = BorderStroke(1.dp, WordSiegeGameUi.PremiumBorder.copy(alpha = .55f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = WordSiegeGameUi.Gold)
                Text(
                    if (access == null && !entitlementError) {
                        sh("Premium araçlar kontrol ediliyor…", "Checking premium tools…")
                    } else {
                        sh("Premium erişimi doğrulanamadı. Satın alımların kilitlenmedi; yeniden dene.", "Premium access could not be verified. Your purchases are not locked; retry.")
                    },
                    modifier = Modifier.weight(1f),
                    color = WordSiegeGameUi.Text,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (entitlementError) {
                    TextButton(
                        onClick = { entitlementRetry++ },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(sh("YENİLE", "RETRY"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        if (access == null) return
    }

    val resolvedAccess = access ?: return
    val hasAnyPremiumTool = resolvedAccess.scoreCalculatorAccess || resolvedAccess.letterTableAccess

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (hasAnyPremiumTool) WordSiegeGameUi.PremiumSurface else WordSiegeGameUi.Surface,
        border = BorderStroke(
            1.dp,
            if (hasAnyPremiumTool) WordSiegeGameUi.PremiumBorder.copy(alpha = .60f) else WordSiegeGameUi.Border,
        ),
        shadowElevation = if (hasAnyPremiumTool) 2.dp else 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (hasAnyPremiumTool) WordSiegeGameUi.Gold else WordSiegeGameUi.Navy,
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hasAnyPremiumTool) Icons.Rounded.Verified else Icons.Rounded.Lock,
                            null,
                            modifier = Modifier.padding(end = 4.dp),
                            tint = Color.White,
                        )
                        Text("PRO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    sh("KUŞATMA ARAÇLARI", "SIEGE TOOLS"),
                    color = WordSiegeGameUi.Text,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .4.sp,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = WordSiegeGameUi.Surface,
                    border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .85f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (resolvedAccess.scoreCalculatorAccess) Icons.Rounded.Calculate else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (resolvedAccess.scoreCalculatorAccess) WordSiegeGameUi.Gold else WordSiegeGameUi.Muted,
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                sh("PUAN HESAPLAMA", "SCORE PREVIEW"),
                                color = WordSiegeGameUi.Text,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                when {
                                    !resolvedAccess.scoreCalculatorAccess -> sh("PRO ile açılır", "Unlocks with PRO")
                                    placements.isEmpty() -> sh("Harf yerleştir", "Place tiles")
                                    preview != null -> sh(
                                        "Kelime +${preview!!.wordScore} • Bölge +${preview!!.areaScore} • Toplam +${preview!!.totalScore}",
                                        "Word +${preview!!.wordScore} • Territory +${preview!!.areaScore} • Total +${preview!!.totalScore}",
                                    )
                                    previewError -> sh("Önizleme alınamadı", "Preview unavailable")
                                    else -> sh("Hesaplanıyor…", "Calculating…")
                                },
                                color = WordSiegeGameUi.Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                lineHeight = 12.sp,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { if (resolvedAccess.letterTableAccess) showLetterTable = true },
                    enabled = resolvedAccess.letterTableAccess,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (resolvedAccess.letterTableAccess) WordSiegeGameUi.PremiumBorder else WordSiegeGameUi.Border,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(
                        if (resolvedAccess.letterTableAccess) Icons.Rounded.GridView else Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(sh("HARFLER", "LETTERS"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (showLetterTable) {
        AlertDialog(
            onDismissRequest = { showLetterTable = false },
            icon = { Icon(Icons.Rounded.GridView, null, tint = WordSiegeGameUi.Gold) },
            title = { Text(sh("Kalan Harfler", "Remaining Letters"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        sh(
                            "Tahtadaki harfler ve kendi elin çıkarıldıktan sonra oyunda görünmeyen harf adetleri.",
                            "Unseen letter counts after subtracting the board and your own rack.",
                        ),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 11.sp,
                    )
                    if (letterTableError) {
                        Text(
                            sh("Harf tablosu şu anda alınamadı. Pencereyi kapatıp yeniden deneyebilirsin.", "Letter counts are temporarily unavailable. Close and retry."),
                            color = WordSiegeGameUi.Text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else if (letterTable.isEmpty()) {
                        Text(sh("Harf bilgisi yükleniyor…", "Loading letter counts…"), color = WordSiegeGameUi.Muted, fontSize = 11.sp)
                    } else {
                        letterTable.chunked(6).forEach { group ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                group.forEach { item ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(9.dp),
                                        color = WordSiegeGameUi.SurfaceSoft,
                                        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
                                    ) {
                                        Text(
                                            "${item.letter} ${item.remaining}",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 7.dp),
                                            color = WordSiegeGameUi.Text,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                                repeat((6 - group.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLetterTable = false }) {
                    Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black)
                }
            },
        )
    }
}
