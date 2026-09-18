package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Premium in-match tools. Both score preview and letter counts are server-authoritative and
 * entitlement-gated again on the RPC side, so UI state cannot unlock paid data by itself.
 */
@Composable
internal fun WordSiegePremiumPanel(
    game: WordSiegeGameDto,
    placements: Map<Int, Int>,
    canAct: Boolean,
) {
    val backend = remember { OnlineGameBackend() }
    var entitlements by remember(game.id) { mutableStateOf(VipEntitlementsDto()) }
    var preview by remember(game.id) { mutableStateOf<WordSiegePremiumPreviewDto?>(null) }
    var previewError by remember(game.id) { mutableStateOf(false) }
    var letterTable by remember(game.id) { mutableStateOf<List<WordSiegeLetterCountDto>>(emptyList()) }
    var showLetterTable by remember(game.id) { mutableStateOf(false) }

    LaunchedEffect(game.id, game.moveCount) {
        entitlements = runCatching { backend.getVipEntitlements() }.getOrDefault(VipEntitlementsDto())
        letterTable = if (entitlements.letterTableAccess) {
            runCatching { backend.getPremiumWordSiegeLetterTable(game.id) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    LaunchedEffect(game.id, game.moveCount, placements, canAct, entitlements.scoreCalculatorAccess) {
        preview = null
        previewError = false
        if (!canAct || placements.isEmpty() || !entitlements.scoreCalculatorAccess) return@LaunchedEffect
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
            // Invalid/unfinished placements are normal while the player is still composing a move.
            previewError = true
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = WordSiegeGameUi.SurfaceSoft,
            border = BorderStroke(1.dp, WordSiegeGameUi.Border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (entitlements.scoreCalculatorAccess) Icons.Rounded.Calculate else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (entitlements.scoreCalculatorAccess) WordSiegeGameUi.Blue else WordSiegeGameUi.Muted,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    when {
                        !entitlements.scoreCalculatorAccess -> sh("Puan Hesaplayıcı • Kilitli", "Score Calculator • Locked")
                        placements.isEmpty() -> sh("Puan Hesaplayıcı • Harf yerleştir", "Score Calculator • Place tiles")
                        preview != null -> sh(
                            "Kelime +${preview!!.wordScore} • Bölge +${preview!!.areaScore} • Toplam +${preview!!.totalScore}",
                            "Word +${preview!!.wordScore} • Territory +${preview!!.areaScore} • Total +${preview!!.totalScore}",
                        )
                        previewError -> sh("Hamleyi tamamla", "Complete the move")
                        else -> sh("Hesaplanıyor…", "Calculating…")
                    },
                    color = WordSiegeGameUi.Text,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }

        OutlinedButton(
            onClick = { if (entitlements.letterTableAccess) showLetterTable = true },
            enabled = entitlements.letterTableAccess,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, WordSiegeGameUi.Border),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                if (entitlements.letterTableAccess) Icons.Rounded.GridView else Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(sh("HARF", "LETTERS"), fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }

    if (showLetterTable) {
        AlertDialog(
            onDismissRequest = { showLetterTable = false },
            icon = { Icon(Icons.Rounded.GridView, null, tint = WordSiegeGameUi.Blue) },
            title = { Text(sh("Harf Tablosu", "Letter Table"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        sh("Tahtadaki harfler ve kendi elin çıkarıldıktan sonra oyunda görünmeyen harf adetleri.", "Unseen letter counts after subtracting the board and your own rack."),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 10.sp,
                    )
                    letterTable.chunked(6).forEach { group ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            group.forEach { item ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = WordSiegeGameUi.SurfaceSoft,
                                    border = BorderStroke(1.dp, WordSiegeGameUi.Border),
                                ) {
                                    Text(
                                        "${item.letter} ${item.remaining}",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
                                        color = WordSiegeGameUi.Text,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            repeat((6 - group.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLetterTable = false }) { Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black) }
            },
        )
    }
}
