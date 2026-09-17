package com.sonharf.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.sonharf.game.data.WordSiegeCellDto
import com.sonharf.game.data.WordSiegeMoveDto
import kotlinx.coroutines.delay

internal const val WORD_SIEGE_MOVE_FEEDBACK_TOTAL_MS = 1_350L
private const val WORD_SIEGE_WORD_STEP_MS = 300L
private const val WORD_SIEGE_TERRITORY_STEP_MS = 300L
private const val WORD_SIEGE_TOTAL_STEP_MS = 280L
private const val WORD_SIEGE_SIEGE_STEP_MS = 320L
private const val WORD_SIEGE_FADE_STEP_MS = 150L

internal fun wordSiegeTerritoryGainPoints(move: WordSiegeMoveDto): Int =
    WordSiegeFinalRules.cubeTransfer(move.capturedCells)

internal fun wordSiegeMoveGainTotal(move: WordSiegeMoveDto): Int =
    move.wordScore + wordSiegeTerritoryGainPoints(move)

/**
 * Keeps the previous authoritative board long enough to reveal ownership changes cell-by-cell.
 * This is presentation-only: the server board remains authoritative and is never mutated here.
 */
@Composable
internal fun rememberWordSiegePresentedBoard(
    gameId: String,
    authoritativeBoard: List<WordSiegeCellDto>,
): List<WordSiegeCellDto> {
    var presentedBoard by remember(gameId) { mutableStateOf(authoritativeBoard) }
    var initialized by remember(gameId) { mutableStateOf(false) }

    LaunchedEffect(gameId, authoritativeBoard) {
        if (!initialized || presentedBoard.size != authoritativeBoard.size) {
            presentedBoard = authoritativeBoard
            initialized = true
            return@LaunchedEffect
        }

        val before = presentedBoard
        val ownershipChanges = authoritativeBoard.indices.filter { index ->
            val oldCell = before.getOrNull(index)
            val newCell = authoritativeBoard[index]
            oldCell != null && oldCell.owner != newCell.owner
        }

        if (ownershipChanges.isEmpty()) {
            presentedBoard = authoritativeBoard
            return@LaunchedEffect
        }

        val staged = authoritativeBoard.toMutableList()
        ownershipChanges.forEach { index ->
            staged[index] = before[index]
        }
        presentedBoard = staged.toList()

        val perCellDelay = (420L / ownershipChanges.size.coerceAtLeast(1)).coerceIn(45L, 90L)
        ownershipChanges.sorted().forEach { index ->
            delay(perCellDelay)
            staged[index] = authoritativeBoard[index]
            presentedBoard = staged.toList()
        }
        presentedBoard = authoritativeBoard
    }

    return presentedBoard
}

@Composable
internal fun WordSiegeMoveResolutionOverlay(
    move: WordSiegeMoveDto,
    isMine: Boolean,
    onComplete: () -> Unit,
) {
    val territoryPoints = remember(move.id, move.capturedCells) { wordSiegeTerritoryGainPoints(move) }
    val totalPoints = remember(move.id, move.wordScore, move.capturedCells) { wordSiegeMoveGainTotal(move) }
    var phase by remember(move.id) { mutableIntStateOf(0) }

    LaunchedEffect(move.id) {
        phase = 1
        delay(WORD_SIEGE_WORD_STEP_MS)
        phase = 2
        delay(WORD_SIEGE_TERRITORY_STEP_MS)
        phase = 3
        delay(WORD_SIEGE_TOTAL_STEP_MS)
        phase = 4
        delay(WORD_SIEGE_SIEGE_STEP_MS)
        phase = 5
        delay(WORD_SIEGE_FADE_STEP_MS)
        onComplete()
    }

    val visibleAlpha by animateFloatAsState(
        targetValue = if (phase in 1..4) 1f else 0f,
        animationSpec = tween(150),
        label = "siege-resolution-alpha",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else .94f,
        animationSpec = tween(180),
        label = "siege-resolution-scale",
    )
    val accent = if (isMine) Color(0xFF3F7C53) else WordSiegeGameUi.Red

    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .14f * visibleAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = visibleAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                    },
                color = WordSiegeGameUi.Surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, accent.copy(alpha = .48f)),
                shadowElevation = 12.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        if (isMine) sh("HAMLE TAMAMLANDI", "MOVE COMPLETE") else sh("RAKİP HAMLESİ", "RIVAL MOVE"),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        move.primaryWord.uppercase(),
                        color = WordSiegeGameUi.Text,
                        fontSize = 25.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        WordSiegeResolutionStat(
                            label = sh("KELİME", "WORD"),
                            value = "+${move.wordScore}",
                            active = phase >= 1,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        )
                        WordSiegeResolutionStat(
                            label = sh("BÖLGE", "TERRITORY"),
                            value = "+$territoryPoints",
                            active = phase >= 2,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        )
                        WordSiegeResolutionStat(
                            label = sh("TOPLAM", "TOTAL"),
                            value = "+$totalPoints",
                            active = phase >= 3,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    val siegeAlpha by animateFloatAsState(
                        targetValue = if (phase >= 4) 1f else 0f,
                        animationSpec = tween(150),
                        label = "siege-resolution-callout",
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = siegeAlpha }
                            .clip(RoundedCornerShape(14.dp)),
                        color = accent.copy(alpha = .09f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = .25f)),
                    ) {
                        Text(
                            if (move.capturedCells > 0) {
                                sh(
                                    "KUŞATMA +$territoryPoints  •  ${move.capturedCells} KÜP",
                                    "SIEGE +$territoryPoints  •  ${move.capturedCells} CUBES",
                                )
                            } else {
                                sh("KELİME HATTI KURULDU", "WORD LINE ESTABLISHED")
                            },
                            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = accent,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordSiegeResolutionStat(
    label: String,
    value: String,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else .18f,
        animationSpec = tween(150),
        label = "siege-resolution-stat-$label",
    )
    Surface(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        color = accent.copy(alpha = .07f),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .2f)),
    ) {
        Column(
            Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = WordSiegeGameUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}
