package com.sonharf.game

import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CelebrationRoom(
    val id: String,
    val status: String,
    @SerialName("host_id") val hostId: String,
    @SerialName("guest_id") val guestId: String? = null,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("is_bot") val isBot: Boolean = false,
    @SerialName("bot_name") val botName: String? = null,
    @SerialName("host_score") val hostScore: Int = 0,
    @SerialName("guest_score") val guestScore: Int = 0,
    @SerialName("host_rounds") val hostRounds: Int = 0,
    @SerialName("guest_rounds") val guestRounds: Int = 0,
    @SerialName("action_seq") val actionSeq: Long = 0,
    @SerialName("last_action_streak") val lastActionStreak: Int? = null,
)

/**
 * Native Android equivalent of the requested WebAudio mini firework pop.
 * Generates a 450 ms white-noise burst with exponential decay and a falling
 * one-pole low-pass body. No external sound asset is used.
 */
private object ProceduralFireworkPop {
    private const val SAMPLE_RATE = 24000

    fun play() {
        Thread {
            val duration = 0.45
            val count = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(count)
            var low = 0.0
            val pitch = Random.nextDouble(0.90, 1.20)

            for (i in pcm.indices) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = exp(-t * 12.0)
                val noise = Random.nextDouble(-1.0, 1.0)
                val cutoffShape = (1.0 - t / duration).coerceIn(0.0, 1.0)
                val alpha = (0.045 + cutoffShape * 0.28 * pitch).coerceIn(0.03, 0.42)
                low += alpha * (noise - low)
                val transient = if (t < 0.025) noise * exp(-t * 80.0) * 0.22 else 0.0
                val sample = (low * 0.42 + transient) * envelope
                pcm[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(pcm)
        }.start()
    }

    private fun playPcm(pcm: ShortArray) {
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val bytes = pcm.size * 2
            val min = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(maxOf(bytes, min))
                .build()
            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(pcm.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(audioTrack: AudioTrack) = audioTrack.release()
                override fun onPeriodicNotification(audioTrack: AudioTrack) = Unit
            })
            track.play()
        }
    }
}

@Composable
fun WinnerFireworkOverlay() {
    if (!SupabaseProvider.configured) return

    val context = LocalContext.current
    val backend = remember { OnlineGameBackend() }
    var trackedActiveRoomId by remember { mutableStateOf<String?>(null) }
    var lastComboKey by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var celebratedRoomId by remember { mutableStateOf<String?>(null) }
    var celebrationRoom by remember { mutableStateOf<CelebrationRoom?>(null) }
    var winnerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var winnerIsBot by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            if (me != null) {
                val rooms = runCatching {
                    SupabaseProvider.client.from("game_rooms").select().decodeList<CelebrationRoom>()
                        .filter { it.hostId == me || it.guestId == me }
                }.getOrDefault(emptyList())

                val active = rooms
                    .filter { it.status in listOf("playing", "quiz", "final", "sudden_death", "paused") }
                    .maxByOrNull { it.actionSeq }

                if (active != null) {
                    trackedActiveRoomId = active.id
                    val streak = active.lastActionStreak ?: 0
                    val comboKey = active.id to active.actionSeq
                    if (streak >= 3 && active.actionSeq > 0 && comboKey != lastComboKey) {
                        lastComboKey = comboKey
                        if (SonHarfPreferences.soundEnabled(context)) ProceduralFireworkPop.play()
                    }
                }

                val latestFinished = rooms.filter { it.status == "finished" }.maxByOrNull { it.actionSeq }
                if (
                    latestFinished != null &&
                    latestFinished.id == trackedActiveRoomId &&
                    latestFinished.id != celebratedRoomId
                ) {
                    celebratedRoomId = latestFinished.id
                    val inferredWinner = latestFinished.winnerId ?: when {
                        latestFinished.hostRounds > latestFinished.guestRounds -> latestFinished.hostId
                        latestFinished.guestRounds > latestFinished.hostRounds -> latestFinished.guestId
                        latestFinished.hostScore >= latestFinished.guestScore -> latestFinished.hostId
                        else -> latestFinished.guestId
                    }
                    winnerIsBot = latestFinished.isBot && inferredWinner == null
                    winnerProfile = inferredWinner?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
                    if (latestFinished.isBot && winnerProfile == null && inferredWinner != latestFinished.hostId) {
                        winnerIsBot = true
                    }
                    celebrationRoom = latestFinished
                }
            }
            delay(350)
        }
    }

    celebrationRoom?.let { room ->
        WinnerCelebrationCard(
            room = room,
            winnerProfile = winnerProfile,
            winnerIsBot = winnerIsBot,
            soundEnabled = SonHarfPreferences.soundEnabled(context),
            onFinished = { celebrationRoom = null },
        )
    }
}

@Composable
private fun WinnerCelebrationCard(
    room: CelebrationRoom,
    winnerProfile: ProfileDto?,
    winnerIsBot: Boolean,
    soundEnabled: Boolean,
    onFinished: () -> Unit,
) {
    val burstProgress = remember(room.id) { Animatable(0f) }
    var burstIndex by remember(room.id) { mutableIntStateOf(0) }
    var avatarBitmap by remember(room.id, winnerProfile?.avatarUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }

    val canShowPhoto = winnerProfile?.avatarUrl?.isNotBlank() == true &&
        (winnerProfile.avatarVisibility == "public" || winnerProfile.id == runCatching { OnlineGameBackend().currentUserId() }.getOrNull())

    LaunchedEffect(winnerProfile?.avatarUrl, canShowPhoto) {
        avatarBitmap = if (canShowPhoto) {
            withContext(Dispatchers.IO) {
                runCatching { URL(winnerProfile?.avatarUrl).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
            }
        } else null
    }

    LaunchedEffect(room.id) {
        delay(180)
        repeat(3) { index ->
            burstIndex = index
            burstProgress.snapTo(0f)
            if (soundEnabled) ProceduralFireworkPop.play()
            burstProgress.animateTo(1f, tween(620))
            delay(180)
        }
        delay(850)
        onFinished()
    }

    val winnerName = when {
        winnerProfile != null -> winnerProfile.displayName
        winnerIsBot -> room.botName ?: "KelimeBot"
        else -> sh("Şampiyon", "Champion")
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = 0.98f)),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(2.dp, SonHarfGold.copy(alpha = 0.70f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🏆  1.", color = SonHarfGold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(sh("MAÇIN KAZANANI", "MATCH WINNER"), color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                    PurchasedVictoryVfx(room.id)
                    Box(
                        Modifier.size(126.dp).clip(CircleShape).background(SonHarfPurple.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap!!.asImageBitmap(),
                                contentDescription = winnerName,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                if (winnerIsBot) "🤖" else winnerName.take(1).uppercase(),
                                color = SonHarfGold,
                                fontSize = if (winnerIsBot) 58.sp else 54.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }

                    Canvas(Modifier.fillMaxSize()) {
                        val p = burstProgress.value
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.minDimension * 0.22f
                        val travel = size.minDimension * 0.30f * p
                        val colors = listOf(SonHarfGold, SonHarfPink, SonHarfCyan, SonHarfGreen, SonHarfPurple)
                        repeat(30) { i ->
                            val angle = (2.0 * PI * i / 30.0) + burstIndex * 0.37
                            val radius = baseRadius + travel * (0.72f + (i % 5) * 0.06f)
                            val end = Offset(
                                center.x + cos(angle).toFloat() * radius,
                                center.y + sin(angle).toFloat() * radius,
                            )
                            val startRadius = (radius - 18f - 18f * p).coerceAtLeast(baseRadius)
                            val start = Offset(
                                center.x + cos(angle).toFloat() * startRadius,
                                center.y + sin(angle).toFloat() * startRadius,
                            )
                            val alpha = (1f - p).coerceIn(0f, 1f)
                            val color = colors[i % colors.size].copy(alpha = alpha)
                            drawLine(color, start, end, strokeWidth = 4f * (1f - p * 0.45f))
                            drawCircle(color, radius = 5f * (1f - p * 0.45f), center = end)
                        }
                    }
                }

                Text(winnerName, color = SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(sh("ŞAMPİYON!", "CHAMPION!"), color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
