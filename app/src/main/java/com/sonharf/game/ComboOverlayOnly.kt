package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sin
import kotlin.random.Random

private data class LiveCombo(
    val n: Int,
    val title: String,
    val sub: String,
    val color: Color,
)

private data class ConfettiBit(
    val x: Float,
    val delay: Float,
    val drift: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
)

@Serializable
private data class ActionRoomDto(
    val id: String,
    @SerialName("host_id") val hostId: String,
    @SerialName("guest_id") val guestId: String? = null,
    val status: String,
    @SerialName("is_bot") val isBot: Boolean = false,
    @SerialName("bot_name") val botName: String? = null,
    @SerialName("action_seq") val actionSeq: Long = 0,
    @SerialName("last_action_streak") val lastActionStreak: Int? = null,
    @SerialName("last_action_bonus") val lastActionBonus: Int? = null,
    @SerialName("last_action_player_id") val lastActionPlayerId: String? = null,
    @SerialName("last_action_is_bot") val lastActionIsBot: Boolean = false,
)

private fun liveCombo(n: Int): LiveCombo? = when (n) {
    3 -> LiveCombo(n, sh("İSABET!", "NICE!"), sh("GÜZEL BAŞLADIN!", "GREAT START!"), SonHarfCyan)
    4 -> LiveCombo(n, sh("AFERİN!", "WELL DONE!"), sh("RİTMİN YERİNE OTURDU!", "YOU FOUND YOUR RHYTHM!"), SonHarfGreen)
    5 -> LiveCombo(n, sh("MÜKEMMEL!", "PERFECT!"), sh("DURDURULAMIYORSUN!", "YOU'RE UNSTOPPABLE!"), SonHarfGold)
    6 -> LiveCombo(n, sh("KELİME KATİLİ!", "STREAK MASTER!"), sh("KELİMELER SENİNLE!", "WORDS ARE ON YOUR SIDE!"), SonHarfPink)
    7 -> LiveCombo(n, sh("EFSANE!", "LEGENDARY!"), sh("BÖYLESİNİ AZ GÖRÜRÜZ!", "WHAT A PERFORMANCE!"), SonHarfPurple)
    8 -> LiveCombo(n, sh("HARİKASIN!", "AMAZING!"), sh("ZİRVEYE YAKLAŞTIN!", "YOU'RE NEAR THE TOP!"), SonHarfCyan)
    9 -> LiveCombo(n, sh("ŞOV ZAMANI!", "SHOWTIME!"), sh("RAKİBİNİ SOLLADIN!", "YOU'VE PULLED AHEAD!"), SonHarfGold)
    else -> if (n >= 10) LiveCombo(n, sh("EFSANELER LİGİ!", "LEAGUE OF LEGENDS!"), sh("SEN BİR KELİME USTASISIN!", "YOU ARE A WORD MASTER!"), SonHarfPink) else null
}

@Composable
fun OnlineGameScreenComboOverlayOnly() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }
    var combo by remember { mutableStateOf<LiveCombo?>(null) }
    var show by remember { mutableStateOf(false) }
    var actorLabel by remember { mutableStateOf("") }
    var bonus by remember { mutableIntStateOf(0) }
    var shownEvent by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val fall = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            if (me != null) {
                val room = runCatching {
                    SupabaseProvider.client.from("game_rooms").select().decodeList<ActionRoomDto>()
                        .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("playing", "quiz", "final", "sudden_death") }
                        .maxByOrNull { it.actionSeq }
                }.getOrNull()

                if (room != null && room.actionSeq > 0) {
                    val eventKey = room.id to room.actionSeq
                    val action = liveCombo(room.lastActionStreak ?: 0)
                    if (action != null && eventKey != shownEvent) {
                        shownEvent = eventKey
                        combo = action
                        bonus = room.lastActionBonus ?: 0
                        actorLabel = when {
                            room.lastActionIsBot -> room.botName ?: "BOT"
                            room.lastActionPlayerId == me -> sh("SEN", "YOU")
                            else -> sh("RAKİP", "OPPONENT")
                        }
                        show = true
                        SonHarfSoundFx.softNotify()
                        fall.snapTo(0f)
                        fall.animateTo(1f, animationSpec = tween(1050, easing = LinearEasing))
                        show = false
                        delay(120)
                    }
                } else {
                    show = false
                    shownEvent = null
                }
            }
            delay(300)
        }
    }

    val particles = remember(shownEvent) {
        val random = Random(shownEvent?.hashCode() ?: 1)
        val palette = listOf(SonHarfPink, SonHarfCyan, SonHarfGold, SonHarfGreen, SonHarfPurple, Color(0xFFFF7043))
        List(72) {
            ConfettiBit(
                x = random.nextFloat(),
                delay = random.nextFloat() * .42f,
                drift = (random.nextFloat() - .5f) * 70f,
                speed = .72f + random.nextFloat() * .50f,
                size = 5f + random.nextFloat() * 8f,
                color = palette[random.nextInt(palette.size)],
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (show) {
            Canvas(Modifier.fillMaxSize()) {
                val p = fall.value
                particles.forEachIndexed { index, bit ->
                    val local = ((p * bit.speed) - bit.delay).coerceIn(0f, 1f)
                    if (local > 0f) {
                        val wave = sin((local * 9f) + index) * bit.drift
                        val x = bit.x * size.width + wave
                        val y = -25f + local * (size.height * .78f)
                        drawRect(bit.color.copy(alpha = (1f - local * .25f).coerceAtLeast(.45f)), topLeft = Offset(x, y), size = Size(bit.size, bit.size * 1.8f))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = show && combo != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 18.dp, vertical = 108.dp),
            enter = fadeIn(tween(80)) + scaleIn(initialScale = .72f, animationSpec = tween(130)),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 1.06f, animationSpec = tween(140)),
        ) {
            val c = combo ?: return@AnimatedVisibility
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(actorLabel, color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(c.title, color = c.color, fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("${c.n} ${sh("DOĞRU SERİ", "WORD STREAK")}${if (bonus > 0) "  •  +$bonus" else ""}", color = SonHarfText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(c.sub, color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}
