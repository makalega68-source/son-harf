package com.sonharf.game

import android.content.Context
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

internal enum class MascotMotion { IDLE, GREETING, THINKING, CRITICAL, VICTORY, DEFEAT }

internal data class MascotAnimationDef(
    val id: String,
    val motion: MascotMotion,
    val unlockLevel: Int,
)

internal object MascotAnimationRegistry {
    val core = listOf(
        MascotAnimationDef("idle", MascotMotion.IDLE, 1),
        MascotAnimationDef("greeting", MascotMotion.GREETING, 1),
        MascotAnimationDef("thinking", MascotMotion.THINKING, 1),
        MascotAnimationDef("critical", MascotMotion.CRITICAL, 1),
        MascotAnimationDef("victory", MascotMotion.VICTORY, 1),
        MascotAnimationDef("defeat", MascotMotion.DEFEAT, 1),
    )
    fun nextUnlockLevel(level: Int): Int = ((level.coerceAtLeast(1) / 10) + 1) * 10
}

internal object MascotRuntime {
    var motion by mutableStateOf(MascotMotion.GREETING)
    var message by mutableStateOf("Buradayım. Hadi başlayalım!")
    var playerLevel by mutableIntStateOf(1)
    var playerXp by mutableIntStateOf(0)

    fun syncProgress(xp: Int, level: Int) {
        playerXp = xp.coerceAtLeast(0)
        playerLevel = level.coerceAtLeast(1)
    }

    fun think(language: String = SonHarfUiState.language) {
        motion = MascotMotion.THINKING
        message = if (language == "en") "I'm thinking about the best next move." else "En iyi sonraki hamleyi düşünüyorum."
    }

    fun encourage(language: String = SonHarfUiState.language) {
        motion = MascotMotion.IDLE
        message = if (language == "en") "Focus on the last letter. You've got this." else "Son harfe odaklan. Bunu yapabilirsin."
    }
}

private object MascotMedia {
    const val IDLE = "https://d8j0ntlcm91z4.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/hf_20260823_110304_5d02ea3d-c6bf-42c4-a7bc-d153681c3c1e.mp4"
    const val GREETING = "https://d8j0ntlcm91z4.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/hf_20260823_121438_5b656a22-4f11-4d63-96e8-2f1fdb0c7cdb.mp4"
    const val THINKING = "https://d8j0ntlcm91z4.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/hf_20260823_112222_ed8b10cc-89d9-4ae2-89e6-cfb58a4206dd.mp4"
    const val CRITICAL = "https://d8j0ntlcm91z4.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/hf_20260823_112222_bf55bdfc-6d34-4db2-8cae-bdcfb8b2f418.mp4"
    const val VICTORY = "https://d8j0ntlcm91z4.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/hf_20260823_112222_ed6b58fe-16bb-4156-b2a7-b6fefe66308a.mp4"
    const val DEFEAT = "https://d8j0ntlcm91z4.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/hf_20260823_121438_cc6225c9-8c5e-4477-b7ff-47a9b74defbb.mp4"

    fun url(motion: MascotMotion) = when (motion) {
        MascotMotion.IDLE -> IDLE
        MascotMotion.GREETING -> GREETING
        MascotMotion.THINKING -> THINKING
        MascotMotion.CRITICAL -> CRITICAL
        MascotMotion.VICTORY -> VICTORY
        MascotMotion.DEFEAT -> DEFEAT
    }
}

private fun mascotName(context: Context): String =
    context.getSharedPreferences("son_harf_mascot", Context.MODE_PRIVATE).getString("name", "Dostum") ?: "Dostum"

private fun setMascotName(context: Context, value: String) {
    context.getSharedPreferences("son_harf_mascot", Context.MODE_PRIVATE).edit().putString("name", value.take(18)).apply()
}

@Composable
internal fun MascotReservedRail(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(mascotName(context)) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(name) }
    val motion = MascotRuntime.motion

    LaunchedEffect(Unit) {
        MascotRuntime.motion = MascotMotion.GREETING
        MascotRuntime.message = if (SonHarfUiState.language == "en") "I'm here. Let's play!" else "Buradayım. Hadi oynayalım!"
        delay(5200)
        MascotRuntime.motion = MascotMotion.IDLE
        MascotRuntime.message = if (SonHarfUiState.language == "en") "Level ${MascotRuntime.playerLevel} • Next motion at ${MascotAnimationRegistry.nextUnlockLevel(MascotRuntime.playerLevel)}" else "Seviye ${MascotRuntime.playerLevel} • Yeni hareket: ${MascotAnimationRegistry.nextUnlockLevel(MascotRuntime.playerLevel)}"
    }

    Surface(
        modifier = modifier,
        color = Color(0xFFF7FCFF),
        border = BorderStroke(1.dp, Color(0xFFB9E8F8)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = SonHarfBlue, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(2.dp))
                Text("AI", color = SonHarfBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp -> mp.isLooping = true; start() }
                        setVideoURI(Uri.parse(MascotMedia.url(motion)))
                    }
                },
                update = { view ->
                    val tag = motion.name
                    if (view.tag != tag) {
                        view.tag = tag
                        view.setVideoURI(Uri.parse(MascotMedia.url(motion)))
                        view.setOnPreparedListener { mp -> mp.isLooping = motion == MascotMotion.IDLE; view.start() }
                    }
                }
            )
            Row(
                Modifier.fillMaxWidth().clickable { renameValue = name; renameOpen = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(name, color = SonHarfText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Rounded.Edit, null, tint = SonHarfMuted, modifier = Modifier.size(10.dp))
            }
            Text("Lv ${MascotRuntime.playerLevel}", color = SonHarfMuted, fontSize = 8.sp)
            Spacer(Modifier.height(5.dp))
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD9F0F8)),
            ) {
                Text(
                    MascotRuntime.message,
                    modifier = Modifier.padding(5.dp),
                    color = SonHarfText,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(7.dp))
            TextButton(onClick = { MascotRuntime.think() }, contentPadding = PaddingValues(2.dp)) {
                Text(if (SonHarfUiState.language == "en") "THINK" else "DÜŞÜN", fontSize = 8.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("${MascotRuntime.playerXp} XP", color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(if (SonHarfUiState.language == "en") "Mascot name" else "Maskotunun adı") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it.take(18) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = renameValue.trim().ifBlank { if (SonHarfUiState.language == "en") "Buddy" else "Dostum" }
                    setMascotName(context, clean)
                    name = clean
                    renameOpen = false
                }) { Text(if (SonHarfUiState.language == "en") "Save" else "Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text(if (SonHarfUiState.language == "en") "Cancel" else "Vazgeç") }
            },
        )
    }
}
