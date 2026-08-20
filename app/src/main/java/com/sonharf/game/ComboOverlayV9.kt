package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.random.Random

@Serializable
private data class ActionRoomV9(
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

private data class ComboV9(val title: String, val color: Color)
private fun comboV9(n: Int): ComboV9? = when (n) {
    3 -> ComboV9(sh("İSABET!", "NICE!"), SonHarfCyan)
    4 -> ComboV9(sh("AFERİN!", "WELL DONE!"), SonHarfGreen)
    5 -> ComboV9(sh("MÜKEMMEL!", "PERFECT!"), SonHarfGold)
    6 -> ComboV9(sh("KELİME KATİLİ!", "STREAK MASTER!"), SonHarfPink)
    7 -> ComboV9(sh("EFSANE!", "LEGENDARY!"), SonHarfPurple)
    8 -> ComboV9(sh("HARİKASIN!", "AMAZING!"), SonHarfCyan)
    9 -> ComboV9(sh("ŞOV ZAMANI!", "SHOWTIME!"), SonHarfGold)
    else -> if (n >= 10) ComboV9(sh("EFSANELER LİGİ!", "LEAGUE OF LEGENDS!"), SonHarfPink) else null
}

private data class ConfettiPiece(val x: Float, val delay: Float, val speed: Float, val size: Float, val angle: Float, val color: Color)

@Composable
fun ComboOverlayV9() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    var combo by remember { mutableStateOf<ComboV9?>(null) }
    var streak by remember { mutableIntStateOf(0) }
    var bonus by remember { mutableIntStateOf(0) }
    var actor by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val progress = remember { Animatable(1f) }
    val pieces = remember {
        val colors = listOf(SonHarfPink, SonHarfCyan, SonHarfGold, SonHarfGreen, SonHarfPurple, Color(0xFFFF6B35))
        List(64) { ConfettiPiece(Random.nextFloat(), Random.nextFloat() * .20f, .75f + Random.nextFloat() * .70f, 5f + Random.nextFloat() * 8f, Random.nextFloat() * 180f, colors[it % colors.size]) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            if (me != null) {
                val room = runCatching {
                    SupabaseProvider.client.from("game_rooms").select().decodeList<ActionRoomV9>()
                        .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("playing", "quiz", "final", "sudden_death") }
                        .maxByOrNull { it.actionSeq }
                }.getOrNull()
                if (room != null && room.actionSeq > 0) {
                    val key = room.id to room.actionSeq
                    val c = comboV9(room.lastActionStreak ?: 0)
                    if (c != null && key != shown) {
                        shown = key
                        combo = c
                        streak = room.lastActionStreak ?: 0
                        bonus = room.lastActionBonus ?: 0
                        actor = when {
                            room.lastActionIsBot -> room.botName ?: "BOT"
                            room.lastActionPlayerId == me -> sh("SEN", "YOU")
                            else -> sh("RAKİP", "OPPONENT")
                        }
                        SonHarfSoundFx.fireworks()
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(1050))
                        combo = null
                    }
                }
            }
            delay(280)
        }
    }

    val c = combo ?: return
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.value
            pieces.forEach { piece ->
                val local = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
                if (local > 0f && local < 1f) {
                    val x = size.width * piece.x + kotlin.math.sin(local * 9f + piece.x * 12f) * 22f
                    val y = size.height * (.08f + local * piece.speed * .82f)
                    rotate(piece.angle + local * 280f, pivot = Offset(x, y)) {
                        drawRect(piece.color.copy(alpha = (1f - local * .65f).coerceAtLeast(.2f)), topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(piece.size, piece.size * 1.8f))
                    }
                }
            }
        }
        Column(Modifier.padding(top = 86.dp, start = 20.dp, end = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(actor, color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(c.title, color = c.color, fontSize = 29.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text("$streak ${sh("DOĞRU SERİ", "WORD STREAK")}" + if (bonus > 0) "  •  +$bonus" else "", color = SonHarfText, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}
