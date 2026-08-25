package com.sonharf.game

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val EveSky = Color(0xFFF4FBFF)
private val EvePanel = Color(0xFFFFFFFF)
private val EveInk = Color(0xFF173B57)
private val EveMuted = Color(0xFF6D879A)
private val EveBlue = Color(0xFF24AEE4)
private val EveLine = Color(0xFFB9E8F8)
private val EveSoft = Color(0xFFEAF8FF)

@Composable
internal fun EveMascotScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val history = remember {
        mutableStateListOf<EveChatTurn>().apply { addAll(loadEveHistory(context)) }
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }

    LaunchedEffect(Unit) {
        if (history.isEmpty()) {
            EveMascotRuntime.setBubble("Merhaba. Ben Eve. Bugün ne konuşalım? 🤍")
        } else {
            history.lastOrNull { it.role == "assistant" }?.text?.let(EveMascotRuntime::setBubble)
        }
        if (SupabaseProvider.configured) {
            val backend = OnlineGameBackend()
            profile = backend.currentUserId()?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
        }
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.lastIndex)
    }

    fun send() {
        val message = input.trim()
        if (message.isBlank() || sending) return
        val previous = history.toList()
        history += EveChatTurn("user", message)
        saveEveHistory(context, history)
        input = ""
        sending = true
        errorMessage = null
        EveMascotRuntime.thinking()

        scope.launch {
            runCatching {
                EveAiChatService.chat(
                    EveChatRequest(
                        message = message,
                        history = previous,
                        language = SonHarfUiState.language,
                        playerName = profile?.displayName,
                        gameContext = profile?.let {
                            "Son Harf oyuncusu. Galibiyet: ${it.wins}, mağlubiyet: ${it.losses}, VIP: ${it.isVip}."
                        },
                    ),
                )
            }.onSuccess { response ->
                history += EveChatTurn("assistant", response.reply)
                saveEveHistory(context, history)
                EveMascotRuntime.apply(response)
            }.onFailure { error ->
                EveMascotRuntime.calm()
                EveMascotRuntime.setBubble("Şu an bağlantımda küçük bir sorun var.")
                errorMessage = error.message ?: "Eve şu anda cevap veremiyor."
            }
            sending = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, EveSky, EveSoft)),
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) { Text("‹  ${sh("Geri", "Back")}") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("EVE", color = EveInk, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(sh("AI arkadaşın", "Your AI companion"), color = EveMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.size(64.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EvePanel),
            border = BorderStroke(1.dp, EveLine),
        ) {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                Eve3DStage(Modifier.fillMaxSize())
                val bubble = EveMascotRuntime.bubbleText
                if (bubble.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 18.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = .95f),
                        border = BorderStroke(1.dp, EveLine),
                        shadowElevation = 3.dp,
                    ) {
                        Text(
                            bubble,
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = EveInk,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(history) { turn ->
                EveChatBubble(turn)
            }
            if (sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = EveBlue)
                        Spacer(Modifier.size(8.dp))
                        Text(sh("Eve düşünüyor…", "Eve is thinking…"), color = EveMuted, fontSize = 12.sp)
                    }
                }
            }
            errorMessage?.let { message ->
                item {
                    Text(message, color = Color(0xFFB23A48), fontSize = 11.sp)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(1200) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(sh("Eve'ye bir şey sor…", "Ask Eve anything…")) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
                enabled = !sending,
            )
            Button(
                onClick = ::send,
                enabled = input.isNotBlank() && !sending,
                colors = ButtonDefaults.buttonColors(containerColor = EveBlue),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("➤", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
internal fun Eve3DStage(modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    val assetAvailable = remember {
        runCatching {
            context.assets.open(EveAssetPolicy.MODEL_ASSET).use { }
            true
        }.getOrDefault(false)
    }

    if (!assetAvailable) {
        Box(modifier.background(EveSoft), contentAlignment = Alignment.Center) {
            Text(
                sh("Eve'nin 3D modeli hazırlanıyor", "Eve's 3D model is being prepared"),
                color = EveMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, EveAssetPolicy.MODEL_ASSET)
    val cue = EveMascotRuntime.animation

    SceneView(
        modifier = modifier,
        surfaceType = SurfaceType.TextureSurface,
        isOpaque = false,
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = null,
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = if (compact) 1.05f else 1.55f,
                centerOrigin = Position(0f, -0.60f, 0f),
                autoAnimate = false,
                animationName = cue.clipName,
                animationLoop = cue.loop,
                position = Position(0f, if (compact) -0.10f else -0.18f, 0f),
                rotation = Rotation(y = 0f),
            )
        }
    }
}

@Composable
private fun EveChatBubble(turn: EveChatTurn) {
    val fromEve = turn.role == "assistant"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromEve) Arrangement.Start else Arrangement.End,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.84f),
            shape = RoundedCornerShape(17.dp),
            color = if (fromEve) Color.White else EveBlue,
            border = if (fromEve) BorderStroke(1.dp, EveLine) else null,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text(
                    if (fromEve) "Eve" else sh("Sen", "You"),
                    color = if (fromEve) EveBlue else Color.White.copy(alpha = .86f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    turn.text,
                    color = if (fromEve) EveInk else Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

private val EveHistoryJson = Json { ignoreUnknownKeys = true }
private const val EVE_HISTORY_PREF = "son_harf_eve_chat"
private const val EVE_HISTORY_KEY = "history_v1"

private fun loadEveHistory(context: Context): List<EveChatTurn> {
    val raw = context.getSharedPreferences(EVE_HISTORY_PREF, Context.MODE_PRIVATE)
        .getString(EVE_HISTORY_KEY, null) ?: return emptyList()
    return runCatching {
        EveHistoryJson.decodeFromString(ListSerializer(EveChatTurn.serializer()), raw)
    }.getOrDefault(emptyList()).takeLast(40)
}

private fun saveEveHistory(context: Context, history: List<EveChatTurn>) {
    val trimmed = history.takeLast(40)
    val raw = EveHistoryJson.encodeToString(ListSerializer(EveChatTurn.serializer()), trimmed)
    context.getSharedPreferences(EVE_HISTORY_PREF, Context.MODE_PRIVATE)
        .edit().putString(EVE_HISTORY_KEY, raw).apply()
}
