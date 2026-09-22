package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
 * Transport failures stay distinct from a real locked entitlement so purchased features
 * never appear unowned during a temporary backend problem.
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
                entitlementError = true
            }
    }

    val access = entitlements

    LaunchedEffect(
        game.id,
        game.moveCount,
        placements,
        canAct,
        access?.scoreCalculatorAccess,
    ) {
        preview = null
        previewError = false
        if (!canAct || placements.isEmpty() || access?.scoreCalculatorAccess != true) {
            return@LaunchedEffect
        }
        val orientation = runCatching {
            WordSiegeFinalRules.detectOrientation(game.board, placements.keys)
        }.getOrNull() ?: return@LaunchedEffect
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
            shape = GameShapes.Medium,
            color = GameColors.SecondarySurface,
            border = BorderStroke(1.dp, GameColors.Border),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = GameColors.PrimaryBlue,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    if (access == null && !entitlementError) {
                        sh(
                            "Premium araçlar kontrol ediliyor…",
                            "Checking premium tools…",
                        )
                    } else {
                        sh(
                            "Premium erişimi doğrulanamadı. Satın alımların kilitlenmedi; yeniden dene.",
                            "Premium access could not be verified. Your purchases are not locked; retry.",
                        )
                    },
                    modifier = Modifier.weight(1f),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (entitlementError) {
                    TextButton(
                        onClick = { entitlementRetry++ },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            sh("YENİLE", "RETRY"),
                            color = GameColors.PrimaryBlue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
        if (access == null) return
    }

    val resolvedAccess = access ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = GameShapes.Medium,
            color = GameColors.SecondarySurface,
            border = BorderStroke(
                1.dp,
                if (resolvedAccess.scoreCalculatorAccess) {
                    GameColors.PrimaryBlue.copy(alpha = .30f)
                } else {
                    GameColors.Border
                },
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (resolvedAccess.scoreCalculatorAccess) {
                        Icons.Rounded.Calculate
                    } else {
                        Icons.Rounded.Lock
                    },
                    contentDescription = null,
                    tint = if (resolvedAccess.scoreCalculatorAccess) {
                        GameColors.PrimaryBlue
                    } else {
                        GameColors.TextTertiary
                    },
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        !resolvedAccess.scoreCalculatorAccess -> sh(
                            "Puan Hesaplayıcı • Kilitli",
                            "Score Calculator • Locked",
                        )
                        placements.isEmpty() -> sh(
                            "Puan Hesaplayıcı • Harf yerleştir",
                            "Score Calculator • Place tiles",
                        )
                        preview != null -> sh(
                            "Kelime +${preview!!.wordScore} • Bölge +${preview!!.areaScore} • Toplam +${preview!!.totalScore}",
                            "Word +${preview!!.wordScore} • Territory +${preview!!.areaScore} • Total +${preview!!.totalScore}",
                        )
                        previewError -> sh(
                            "Önizleme alınamadı • hamleyi kontrol et",
                            "Preview unavailable • check the move",
                        )
                        else -> sh("Hesaplanıyor…", "Calculating…")
                    },
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        OutlinedButton(
            onClick = {
                if (resolvedAccess.letterTableAccess) showLetterTable = true
            },
            enabled = resolvedAccess.letterTableAccess,
            shape = GameShapes.Medium,
            border = BorderStroke(
                1.dp,
                if (resolvedAccess.letterTableAccess) {
                    GameColors.TacticalTurquoise.copy(alpha = .45f)
                } else {
                    GameColors.Border
                },
            ),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
        ) {
            Icon(
                if (resolvedAccess.letterTableAccess) {
                    Icons.Rounded.GridView
                } else {
                    Icons.Rounded.Lock
                },
                contentDescription = null,
                tint = if (resolvedAccess.letterTableAccess) {
                    GameColors.TacticalTurquoise
                } else {
                    GameColors.TextTertiary
                },
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                sh("HARFLER", "LETTERS"),
                color = if (resolvedAccess.letterTableAccess) {
                    GameColors.TacticalTurquoise
                } else {
                    GameColors.TextTertiary
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
            )
        }
    }

    if (showLetterTable) {
        AlertDialog(
            onDismissRequest = { showLetterTable = false },
            icon = {
                Icon(
                    Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = GameColors.TacticalTurquoise,
                )
            },
            title = {
                Text(
                    sh("Harf Tablosu", "Letter Table"),
                    color = GameColors.TextPrimary,
                    fontWeight = FontWeight.Black,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        sh(
                            "Tahtadaki harfler ve kendi elin çıkarıldıktan sonra oyunda görünmeyen harf adetleri.",
                            "Unseen letter counts after subtracting the board and your own rack.",
                        ),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    when {
                        letterTableError -> Text(
                            sh(
                                "Harf tablosu şu anda alınamadı. Pencereyi kapatıp yeniden deneyebilirsin.",
                                "Letter counts are temporarily unavailable. Close and retry.",
                            ),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        letterTable.isEmpty() -> Text(
                            sh(
                                "Harf bilgisi yükleniyor…",
                                "Loading letter counts…",
                            ),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        else -> {
                            letterTable.chunked(6).forEach { group ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    group.forEach { item ->
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = GameShapes.Small,
                                            color = GameColors.ElevatedBackground,
                                            border = BorderStroke(1.dp, GameColors.Border),
                                        ) {
                                            Text(
                                                "${item.letter} ${item.remaining}",
                                                modifier = Modifier.padding(
                                                    horizontal = 5.dp,
                                                    vertical = 7.dp,
                                                ),
                                                color = GameColors.TextPrimary,
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                    repeat((6 - group.size).coerceAtLeast(0)) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLetterTable = false }) {
                    Text(
                        sh("KAPAT", "CLOSE"),
                        color = GameColors.PrimaryBlue,
                        fontWeight = FontWeight.Black,
                    )
                }
            },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
        )
    }
}
