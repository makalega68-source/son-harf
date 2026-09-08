package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.PremierBoosterStatusDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.findPremierActiveRoom
import com.sonharf.game.data.getPremierBoosterStatus
import com.sonharf.game.data.usePremierHint
import com.sonharf.game.data.usePremierMultiplier
import com.sonharf.game.data.usePremierSwap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared only with the visible Premier arena so a server-approved letter swap is rendered immediately. */
internal object PremierBoosterUiState {
    var roomId by mutableStateOf<String?>(null)
    var requiredOverride by mutableStateOf<String?>(null)
    var multiplierArmed by mutableStateOf(false)
    var hintWhisper by mutableStateOf<String?>(null)

    fun clear() {
        roomId = null
        requiredOverride = null
        multiplierArmed = false
        hintWhisper = null
    }
}

@Composable
internal fun PremierBoosterOverlay() {
    if (!SupabaseProvider.configured || !SonHarfUiState.inMatch) {
        PremierBoosterUiState.clear()
        return
    }

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var roomId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(PremierBoosterStatusDto()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh(id: String) {
        runCatching { backend.getPremierBoosterStatus(id) }
            .onSuccess { next ->
                status = next
                PremierBoosterUiState.roomId = id
                PremierBoosterUiState.requiredOverride = next.requiredOverride
                PremierBoosterUiState.multiplierArmed = next.multiplierArmed
                if (!next.myTurn) PremierBoosterUiState.hintWhisper = null
            }
    }

    LaunchedEffect(SonHarfUiState.inMatch) {
        while (SonHarfUiState.inMatch) {
            val id = roomId ?: runCatching { backend.findPremierActiveRoom()?.id }.getOrNull()
            if (id != null) {
                roomId = id
                refresh(id)
            }
            delay(900)
        }
        PremierBoosterUiState.clear()
    }

    val id = roomId ?: return
    val enabled = status.myTurn && busy == null
    val tr = !SonHarfUiState.isEnglish

    Box(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, bottom = 214.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val whisper = PremierBoosterUiState.hintWhisper
            if (!whisper.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 7.dp),
                ) {
                    Text(
                        if (tr) "Fısıltı: $whisper" else "Whisper: $whisper",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color(0xFFE0F2FE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BoosterChip(
                    modifier = Modifier.weight(1f),
                    icon = "💡",
                    label = if (tr) "İPUCU" else "HINT",
                    count = status.hintCount,
                    active = false,
                    enabled = enabled && status.hintCount > 0,
                    onClick = {
                        busy = "hint"
                        scope.launch {
                            runCatching { backend.usePremierHint(id) }
                                .onSuccess { result ->
                                    PremierBoosterUiState.hintWhisper = "${result.whisper}… • ${result.wordLength} ${if (tr) "harf" else "letters"}"
                                    message = null
                                    refresh(id)
                                }
                                .onFailure { message = boosterError(tr, it.message.orEmpty()) }
                            busy = null
                        }
                    },
                )
                BoosterChip(
                    modifier = Modifier.weight(1f),
                    icon = "🔄",
                    label = if (tr) "DEĞİŞTİR" else "SWAP",
                    count = status.swapCount,
                    active = !status.requiredOverride.isNullOrBlank(),
                    enabled = enabled && status.swapCount > 0,
                    onClick = {
                        busy = "swap"
                        scope.launch {
                            runCatching { backend.usePremierSwap(id) }
                                .onSuccess { result ->
                                    PremierBoosterUiState.requiredOverride = result.requiredOverride
                                    PremierBoosterUiState.hintWhisper = null
                                    message = if (tr) "Yeni hedef: ${result.requiredOverride.uppercase()}" else "New target: ${result.requiredOverride.uppercase()}"
                                    refresh(id)
                                }
                                .onFailure { message = boosterError(tr, it.message.orEmpty()) }
                            busy = null
                        }
                    },
                )
                BoosterChip(
                    modifier = Modifier.weight(1f),
                    icon = "×2",
                    label = if (tr) "SKOR" else "SCORE",
                    count = status.multiplierCount,
                    active = status.multiplierArmed,
                    enabled = enabled && status.multiplierCount > 0 && !status.multiplierArmed,
                    onClick = {
                        busy = "multiplier"
                        scope.launch {
                            runCatching { backend.usePremierMultiplier(id) }
                                .onSuccess {
                                    PremierBoosterUiState.multiplierArmed = true
                                    message = if (tr) "2x skor bu hamle için hazır." else "2x score armed for this move."
                                    refresh(id)
                                }
                                .onFailure { message = boosterError(tr, it.message.orEmpty()) }
                            busy = null
                        }
                    },
                )
            }
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.width(1.dp))
                Text(
                    message.orEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    color = Color(0xFFCBD5E1),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BoosterChip(
    modifier: Modifier,
    icon: String,
    label: String,
    count: Int,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val border = if (active) Color(0xFFF59E0B) else Color(0xFF334155)
    val background = if (active) Color(0xFF3A2A09) else Color(0xFF0F172A)
    val content = if (enabled || active) Color(0xFFF8FAFC) else Color(0xFF64748B)
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = BorderStroke(if (active) 2.dp else 1.dp, border),
        shadowElevation = if (active) 7.dp else 2.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, color = content, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(label, color = content, fontSize = 7.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(if (active) "AKTİF" else "$count", color = if (active) Color(0xFFF59E0B) else content, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun boosterError(tr: Boolean, raw: String): String = when {
    "no_hint_tokens" in raw -> if (tr) "İpucu hakkın kalmadı." else "No Hint tokens left."
    "no_swap_tokens" in raw -> if (tr) "Harf Değiştirici hakkın kalmadı." else "No Letter Swap tokens left."
    "no_multiplier_tokens" in raw -> if (tr) "2x skor hakkın kalmadı." else "No 2x Score tokens left."
    "multiplier_already_armed" in raw -> if (tr) "2x skor zaten aktif." else "2x Score is already armed."
    "swap_not_needed_for_opening" in raw -> if (tr) "İlk kelimede harf değiştirmeye gerek yok." else "Letter Swap is not needed on the opening word."
    "not_your_turn" in raw -> if (tr) "Takviye yalnız kendi sıranda kullanılabilir." else "Boosters can only be used on your turn."
    "turn_expired" in raw -> if (tr) "Süre doldu." else "Turn expired."
    else -> if (tr) "Takviye şu an kullanılamadı." else "Booster is unavailable right now."
}
