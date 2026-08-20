package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

private data class ComboMoment(val streak: Int, val title: String, val subtitle: String, val symbol: String, val accent: Color)

private fun comboMoment(streak: Int): ComboMoment? = when (streak) {
    3 -> ComboMoment(3, "İSABET!", "GÜZEL BAŞLADIN!", "◎", SonHarfCyan)
    4 -> ComboMoment(4, "AFERİN!", "RİTMİN YERİNE OTURDU!", "ϟ", SonHarfGreen)
    5 -> ComboMoment(5, "MÜKEMMEL!", "DURDURULAMIYORSUN!", "★", SonHarfGold)
    6 -> ComboMoment(6, "SERİ KATİL!", "KELİMELER SENİNLE!", "◉", SonHarfPink)
    7 -> ComboMoment(7, "EFSANE!", "BÖYLESİNİ AZ GÖRÜRÜZ!", "♛", SonHarfPurple)
    8 -> ComboMoment(8, "HARİKASIN!", "ZİRVEYE YAKLAŞTIN!", "◆", SonHarfCyan)
    9 -> ComboMoment(9, "ŞOV ZAMANI!", "RAKİBİNİ SOLLADIN!", "↗", SonHarfGold)
    else -> if (streak >= 10 && streak % 5 == 0) ComboMoment(streak, "EFSANELER LİGİ!", "SEN BİR KELİME USTASISIN!", "🏆", SonHarfPink) else null
}

@Composable
fun OnlineGameScreenWithCombo() {
    Box(Modifier.fillMaxSize()) {
        OnlineGameScreenV6()
        if (SupabaseProvider.configured) ComboActionWatcher(Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun ComboActionWatcher(modifier: Modifier = Modifier) {
    val backend = remember { OnlineGameBackend() }
    var moment by remember { mutableStateOf<ComboMoment?>(null) }
    var visible by remember { mutableStateOf(false) }
    var lastShown by remember { mutableStateOf<Pair<String, Int>?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            if (me != null) {
                val active = runCatching {
                    SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
                        .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("playing", "final", "sudden_death") }
                        .maxByOrNull { it.validWordCount }
                }.getOrNull()
                if (active != null) {
                    val streak = if (active.hostId == me) active.hostStreak else active.guestStreak
                    val next = comboMoment(streak)
                    val key = active.id to streak
                    if (next != null && key != lastShown) {
                        lastShown = key
                        moment = next
                        visible = true
                        SonHarfSoundFx.softNotify()
                        delay(1450)
                        visible = false
                        delay(260)
                    }
                    if (streak < 3) lastShown = null
                } else {
                    visible = false
                    lastShown = null
                }
            }
            delay(450)
        }
    }

    AnimatedVisibility(
        visible = visible && moment != null,
        modifier = modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 92.dp),
        enter = fadeIn(tween(130)) + scaleIn(initialScale = .72f, animationSpec = tween(180)),
        exit = fadeOut(tween(220)) + scaleOut(targetScale = 1.08f, animationSpec = tween(220))
    ) {
        val m = moment ?: return@AnimatedVisibility
        Surface(
            color = Color(0xF20A1020),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(2.dp, m.accent.copy(alpha = .75f)),
            shadowElevation = 14.dp
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.horizontalGradient(listOf(m.accent.copy(alpha = .06f), m.accent.copy(alpha = .22f), m.accent.copy(alpha = .06f)))
                ).padding(horizontal = 18.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(m.symbol, color = m.accent, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(m.title, color = m.accent, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("${m.streak} DOĞRU SERİ!", color = SonHarfText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(m.subtitle, color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
