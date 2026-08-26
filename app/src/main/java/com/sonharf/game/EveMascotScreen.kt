package com.sonharf.game

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val ForestDeep = Color(0xFF082F2A)
private val ForestMid = Color(0xFF176B52)
private val ForestGlow = Color(0xFF79D7A8)
private val EvePanel = Color(0xFFF8FFF9)
private val EveInk = Color(0xFF163D36)
private val EveMuted = Color(0xFF638078)
private val EveBlue = Color(0xFF24AEE4)
private val EveLine = Color(0xFFA7D8C2)
private val EveSoft = Color(0xFFE8F7EF)
private val EveGold = Color(0xFFFFC857)

private enum class EvePanelMode { CHAT, FEED, GIFT }

@Composable
internal fun EveMascotScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val history = remember { mutableStateListOf<EveChatTurn>().apply { addAll(loadEveHistory(context)) } }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var mode by remember { mutableStateOf(EvePanelMode.CHAT) }
    var nameDraft by remember { mutableStateOf(store.name) }
    var revision by remember { mutableIntStateOf(0) }
    revision

    LaunchedEffect(Unit) {
        if (history.lastOrNull()?.role == "user") {
            val recovery = sh(
                "Önceki mesajının cevabı tamamlanmamış. Tekrar gönderebilirsin. 🌿",
                "The previous reply did not finish. You can send it again. 🌿",
            )
            history += EveChatTurn("assistant", recovery)
            saveEveHistory(context, history)
        }
        EveMascotRuntime.setBubble(
            history.lastOrNull { it.role == "assistant" }?.text
                ?: "Ormanımıza hoş geldin. Bugün birlikte ne yapalım? ✨",
        )
        if (SupabaseProvider.configured) {
            val backend = OnlineGameBackend()
            profile = backend.currentUserId()?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
        }
    }
    LaunchedEffect(history.size) { if (history.isNotEmpty()) listState.animateScrollToItem(history.lastIndex) }

    fun saveName() {
        store.name = nameDraft
        nameDraft = store.name
        EveMascotRuntime.setBubble("Tamam! Bundan sonra adım ${store.name}. 🌿")
        revision++
    }

    fun send() {
        val message = input.trim()
        if (message.isBlank() || sending) return
        val previous = history.toList()
        history += EveChatTurn("user", message)
        saveEveHistory(context, history)
        input = ""; sending = true; errorMessage = null; EveMascotRuntime.thinking()
        scope.launch {
            runCatching {
                EveAiChatService.chat(EveChatRequest(
                    message = message,
                    history = previous,
                    language = SonHarfUiState.language,
                    playerName = profile?.displayName,
                    companionName = store.name,
                    gameContext = profile?.let {
                        "Son Harf oyuncusu. Galibiyet: ${it.wins}, mağlubiyet: ${it.losses}, VIP: ${it.isVip}. Maskot yakınlığı: ${store.affection}/100."
                    },
                ))
            }.onSuccess { response ->
                history += EveChatTurn("assistant", response.reply)
                saveEveHistory(context, history)
                EveMascotRuntime.apply(response)
            }.onFailure { error ->
                EveMascotRuntime.calm()
                val messageText = error.message ?: "Maskot şu anda cevap veremiyor."
                val visibleFailure = when {
                    messageText.contains("ücretsiz", ignoreCase = true) -> sh(
                        "Bugünkü ücretsiz sohbet hakkımız doldu. Yarın yeniden konuşabiliriz. 🤍",
                        "Today's free chat limit is full. We can talk again tomorrow. 🤍",
                    )
                    messageText.contains("oturum", ignoreCase = true) -> sh(
                        "Sohbet bağlantım için yeniden giriş yapman gerekiyor.",
                        "Please sign in again so I can reconnect to chat.",
                    )
                    else -> sh(
                        "Şu an cevabımı tamamlayamadım. Mesajını tekrar gönderebilir misin? 🌿",
                        "I couldn't finish my reply. Could you send your message again? 🌿",
                    )
                }
                history += EveChatTurn("assistant", visibleFailure)
                saveEveHistory(context, history)
                EveMascotRuntime.setBubble(visibleFailure)
                errorMessage = null
            }
            sending = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        FantasyForestBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("‹ ${sh("Geri", "Back")}", color = Color.White) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(store.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                    Text(sh("Büyülü orman arkadaşın", "Your enchanted forest companion"), color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
                }
                Surface(color = Color.White.copy(alpha = .16f), shape = RoundedCornerShape(16.dp)) {
                    Text("🍃 ${store.leaves}", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .12f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .25f)),
            ) {
                Box(Modifier.fillMaxWidth().height(275.dp)) {
                    Eve3DStage(Modifier.fillMaxSize())
                    if (EveMascotRuntime.bubbleText.isNotBlank()) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 18.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .95f), shadowElevation = 4.dp,
                        ) {
                            Text(EveMascotRuntime.bubbleText, Modifier.padding(12.dp), color = EveInk, fontSize = 13.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                        shape = RoundedCornerShape(14.dp), color = ForestDeep.copy(alpha = .78f),
                    ) {
                        Text("💚 ${store.affection}/100", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeButton("💬 ${sh("Sohbet", "Chat")}", mode == EvePanelMode.CHAT, Modifier.weight(1f)) { mode = EvePanelMode.CHAT }
                ModeButton("🍎 ${sh("Besle", "Feed")}", mode == EvePanelMode.FEED, Modifier.weight(1f)) { mode = EvePanelMode.FEED }
                ModeButton("🎁 ${sh("Hediye", "Gift")}", mode == EvePanelMode.GIFT, Modifier.weight(1f)) { mode = EvePanelMode.GIFT }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = EvePanel.copy(alpha = .97f),
            ) {
                when (mode) {
                    EvePanelMode.CHAT -> ChatPanel(history, listState, sending, errorMessage, input, { input = it.take(1000) }, ::send, nameDraft, { nameDraft = it.take(18) }, ::saveName)
                    EvePanelMode.FEED -> FeedPanel(store, revision) { revision++ }
                    EvePanelMode.GIFT -> GiftPanel(store, revision) { revision++ }
                }
            }
        }
    }
}

@Composable
private fun FantasyForestBackground(modifier: Modifier) {
    Canvas(modifier.background(Brush.verticalGradient(listOf(ForestDeep, ForestMid, Color(0xFF123D35))))) {
        val w = size.width; val h = size.height
        repeat(9) { i ->
            val x = w * (i + .3f) / 9f
            drawCircle(ForestGlow.copy(alpha = .12f + (i % 3) * .03f), w * (.11f + (i % 2) * .025f), Offset(x, h * (.18f + (i % 4) * .10f)))
        }
        repeat(22) { i ->
            val x = w * ((i * 37) % 100) / 100f
            val y = h * (.08f + ((i * 61) % 75) / 100f)
            drawCircle(if (i % 3 == 0) EveGold.copy(alpha = .65f) else Color.White.copy(alpha = .45f), if (i % 4 == 0) 3.2f else 1.8f, Offset(x, y))
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) EveGold else ForestDeep.copy(alpha = .78f))) {
        Text(label, color = if (selected) EveInk else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChatPanel(
    history: List<EveChatTurn>, listState: androidx.compose.foundation.lazy.LazyListState, sending: Boolean,
    error: String?, input: String, onInput: (String) -> Unit, onSend: () -> Unit,
    nameDraft: String, onName: (String) -> Unit, saveName: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = nameDraft, onValueChange = onName, label = { Text(sh("Maskot adı", "Companion name")) }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = saveName) { Text(sh("Kaydet", "Save")) }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp), contentPadding = PaddingValues(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(history) { EveChatBubble(it) }
            if (sending) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp); Spacer(Modifier.size(7.dp)); Text(sh("Düşünüyor…", "Thinking…"), color = EveMuted, fontSize = 11.sp) } }
            error?.let { item { Text(it, color = Color(0xFFB23A48), fontSize = 11.sp) } }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.weight(1f), placeholder = { Text(sh("Bir şey söyle…", "Say something…")) }, maxLines = 3, enabled = !sending, shape = RoundedCornerShape(18.dp))
            Button(onClick = onSend, enabled = input.isNotBlank() && !sending, colors = ButtonDefaults.buttonColors(containerColor = EveBlue)) { Text("➤") }
        }
    }
}

@Composable
private fun FeedPanel(store: EveCompanionStore, revision: Int, changed: () -> Unit) {
    revision
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text(sh("Orman Mutfağı", "Forest Pantry"), color = EveInk, fontWeight = FontWeight.Black, fontSize = 18.sp); Text(sh("Yapraklarla yiyecek al, sonra arkadaşını besle.", "Buy treats with leaves, then feed your companion."), color = EveMuted, fontSize = 11.sp) }
        items(EveCompanionRules.foods) { food ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, EveLine)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(food.emoji, fontSize = 28.sp)
                    Column(Modifier.weight(1f)) { Text(food.titleTr, color = EveInk, fontWeight = FontWeight.Bold); Text("🍃 ${food.price}  •  Çantada: ${store.inventory(food.id)}  •  +${food.affection} 💚", color = EveMuted, fontSize = 10.sp) }
                    TextButton(onClick = { if (store.buy(food)) { EveMascotRuntime.setBubble("Bu çok güzel kokuyor! 🌿"); changed() } }) { Text(sh("Satın al", "Buy")) }
                    Button(onClick = { if (store.feed(food)) { EveMascotRuntime.play(EveAnimationCue.IDLE_GRAZE, "Mmm! Teşekkür ederim. 💚"); changed() } }, enabled = store.inventory(food.id) > 0) { Text(sh("Besle", "Feed")) }
                }
            }
        }
    }
}

@Composable
private fun GiftPanel(store: EveCompanionStore, revision: Int, changed: () -> Unit) {
    revision
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🎁", fontSize = 54.sp)
            Text(sh("Günün Orman Hediyesi", "Daily Forest Gift"), color = EveInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(sh("Maskotun her gün senin için yaprak ve sürpriz bir yiyecek toplar.", "Your companion gathers leaves and a surprise treat for you every day."), color = EveMuted, textAlign = TextAlign.Center)
            Button(onClick = { if (store.claimDailyGift()) { EveMascotRuntime.play(EveAnimationCue.RUN, "Bunu senin için buldum! 🎁✨"); changed() } }, enabled = store.giftAvailable(), colors = ButtonDefaults.buttonColors(containerColor = EveGold, contentColor = EveInk)) {
                Text(if (store.giftAvailable()) sh("Hediyeyi al", "Claim gift") else sh("Bugünkü hediye alındı ✓", "Today's gift claimed ✓"), fontWeight = FontWeight.Bold)
            }
            Text("+${EveCompanionRules.DAILY_GIFT_LEAVES} 🍃  + 1 ${sh("sürpriz yiyecek", "surprise treat")}", color = EveMuted, fontSize = 11.sp)
        }
    }
}

/**
 * Real 3D EVE only. A missing asset or a model that never materializes must never be hidden by
 * a 2D substitute; debug builds surface the failure and release builds fail fast.
 */
@Composable
internal fun Eve3DStage(modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    val assetAvailable = remember {
        runCatching { context.assets.open(EveAssetPolicy.MODEL_ASSET).use { } }.isSuccess
    }

    if (!assetAvailable) {
        val message = "FATAL: ${EveAssetPolicy.MODEL_ASSET} is missing from the APK"
        if (!BuildConfig.DEBUG) error(message)
        Surface(
            modifier = modifier.padding(12.dp),
            color = Color(0xFF7F1D1D),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "3D EVE ASSET HATASI\n${EveAssetPolicy.MODEL_ASSET}\n2D fallback devre dışı.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                )
            }
        }
        return
    }

    // Eve's rig is too large for the minimum GLES vertex-uniform budget on some renderers.
    // Keep Vulkan for the 3D model, but use SurfaceView instead of TextureView below so IME/window
    // relayouts do not recycle a TextureView NativeWindow underneath Filament's Vulkan swap chain.
    val engine = rememberEngine(
        engineCreator = { eglContext ->
            runCatching {
                com.google.android.filament.Engine.create(
                    com.google.android.filament.Engine.Backend.VULKAN,
                )
            }.getOrElse {
                com.google.android.filament.Engine.create(eglContext)
            }
        },
    )
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, EveAssetPolicy.MODEL_ASSET)
    val cue = EveMascotRuntime.animation
    var loadTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(modelInstance) {
        if (modelInstance == null) {
            delay(8_000)
            loadTimedOut = true
        } else {
            loadTimedOut = false
        }
    }

    if (loadTimedOut && modelInstance == null) {
        val message = "FATAL: Eve GLB exists but SceneView could not create a model instance"
        if (!BuildConfig.DEBUG) error(message)
        Surface(
            modifier = modifier.padding(12.dp),
            color = Color(0xFF7F1D1D),
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "3D EVE YÜKLEME HATASI\nGLB APK içinde fakat ModelNode oluşturulamadı.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                )
            }
        }
        return
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (modelInstance == null) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        } else {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                surfaceType = SurfaceType.Surface,
                isOpaque = false,
                engine = engine,
                modelLoader = modelLoader,
                cameraManipulator = null,
            ) {
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = if (compact) 1.0f else 0.90f,
                    centerOrigin = Position(0f, -0.60f, 0f),
                    autoAnimate = false,
                    animationName = cue.clipName,
                    animationLoop = cue.loop,
                    position = Position(0f, if (compact) -.08f else -.10f, 0f),
                    rotation = Rotation(y = 0f),
                )
            }
        }
    }
}

@Composable
private fun EveChatBubble(turn: EveChatTurn) {
    val fromEve = turn.role == "assistant"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromEve) Arrangement.Start else Arrangement.End) {
        Surface(modifier = Modifier.fillMaxWidth(.84f), shape = RoundedCornerShape(17.dp), color = if (fromEve) Color.White else EveBlue, border = if (fromEve) BorderStroke(1.dp, EveLine) else null) {
            Text(turn.text, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = if (fromEve) EveInk else Color.White, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

private val EveHistoryJson = Json { ignoreUnknownKeys = true }
private const val EVE_HISTORY_PREF = "son_harf_eve_chat"
private const val EVE_HISTORY_KEY = "history_v1"
private fun loadEveHistory(context: Context): List<EveChatTurn> {
    val raw = context.getSharedPreferences(EVE_HISTORY_PREF, Context.MODE_PRIVATE).getString(EVE_HISTORY_KEY, null) ?: return emptyList()
    return runCatching { EveHistoryJson.decodeFromString(ListSerializer(EveChatTurn.serializer()), raw) }.getOrDefault(emptyList()).takeLast(40)
}
private fun saveEveHistory(context: Context, history: List<EveChatTurn>) {
    val raw = EveHistoryJson.encodeToString(ListSerializer(EveChatTurn.serializer()), history.takeLast(40))
    context.getSharedPreferences(EVE_HISTORY_PREF, Context.MODE_PRIVATE).edit().putString(EVE_HISTORY_KEY, raw).apply()
}
