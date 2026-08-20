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
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private data class LiveCombo(
    val n: Int,
    val title: String,
    val sub: String,
    val mark: String,
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
    3 -> LiveCombo(n, sh("İSABET!", "NICE!"), sh("GÜZEL BAŞLADIN!", "GREAT START!"), "◎", SonHarfCyan)
    4 -> LiveCombo(n, sh("AFERİN!", "WELL DONE!"), sh("RİTMİN YERİNE OTURDU!", "YOU FOUND YOUR RHYTHM!"), "ϟ", SonHarfGreen)
    5 -> LiveCombo(n, sh("MÜKEMMEL!", "PERFECT!"), sh("DURDURULAMIYORSUN!", "YOU'RE UNSTOPPABLE!"), "★", SonHarfGold)
    6 -> LiveCombo(n, sh("SERİ KATİL!", "STREAK MASTER!"), sh("KELİMELER SENİNLE!", "WORDS ARE ON YOUR SIDE!"), "◉", SonHarfPink)
    7 -> LiveCombo(n, sh("EFSANE!", "LEGENDARY!"), sh("BÖYLESİNİ AZ GÖRÜRÜZ!", "WHAT A PERFORMANCE!"), "♛", SonHarfPurple)
    8 -> LiveCombo(n, sh("HARİKASIN!", "AMAZING!"), sh("ZİRVEYE YAKLAŞTIN!", "YOU'RE NEAR THE TOP!"), "◆", SonHarfCyan)
    9 -> LiveCombo(n, sh("ŞOV ZAMANI!", "SHOWTIME!"), sh("RAKİBİNİ SOLLADIN!", "YOU'VE PULLED AHEAD!"), "↗", SonHarfGold)
    else -> if (n >= 10) LiveCombo(n, sh("EFSANELER LİGİ!", "LEAGUE OF LEGENDS!"), sh("SEN BİR KELİME USTASISIN!", "YOU ARE A WORD MASTER!"), "🏆", SonHarfPink) else null
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

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            if (me != null) {
                val room = runCatching {
                    SupabaseProvider.client.from("game_rooms")
                        .select()
                        .decodeList<ActionRoomDto>()
                        .filter {
                            (it.hostId == me || it.guestId == me) &&
                                it.status in listOf("playing", "quiz", "final", "sudden_death")
                        }
                        .maxByOrNull { it.actionSeq }
                }.getOrNull()

                if (room != null && room.actionSeq > 0) {
                    val eventKey = room.id to room.actionSeq
                    val streak = room.lastActionStreak ?: 0
                    val action = liveCombo(streak)
                    if (action != null && eventKey != shownEvent) {
                        shownEvent = eventKey
                        combo = action
                        bonus = room.lastActionBonus ?: 0
                        actorLabel = when {
                            room.lastActionIsBot -> room.botName ?: sh("BOT", "BOT")
                            room.lastActionPlayerId == me -> sh("SEN", "YOU")
                            else -> sh("RAKİP", "OPPONENT")
                        }
                        show = true
                        SonHarfSoundFx.softNotify()
                        delay(1800)
                        show = false
                        delay(220)
                    }
                } else {
                    show = false
                    shownEvent = null
                }
            }
            delay(350)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = show && combo != null,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 92.dp),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = .70f, animationSpec = tween(180)),
            exit = fadeOut(tween(220)) + scaleOut(targetScale = 1.08f, animationSpec = tween(220)),
        ) {
            val c = combo ?: return@AnimatedVisibility
            Surface(
                color = SonHarfSurface.copy(alpha = .96f),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(2.dp, c.color.copy(alpha = .82f)),
                shadowElevation = 18.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(c.color.copy(alpha = .05f), c.color.copy(alpha = .26f), c.color.copy(alpha = .05f)),
                            ),
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(actorLabel, color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(c.mark, color = c.color, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(c.title, color = c.color, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("${c.n} ${sh("DOĞRU SERİ!", "WORD STREAK!")}", color = SonHarfText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(c.sub, color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (bonus > 0) {
                        Spacer(Modifier.height(5.dp))
                        Text("+$bonus ${sh("BONUS PUAN", "BONUS POINTS")}", color = SonHarfGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
