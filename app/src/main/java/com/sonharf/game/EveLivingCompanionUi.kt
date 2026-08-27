package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LivingForestDeep = Color(0xFF073B32)
private val LivingForestMid = Color(0xFF176B52)
private val LivingGreen = Color(0xFF55BC75)
private val LivingMint = Color(0xFFEAF8EF)
private val LivingInk = Color(0xFF173B35)
private val LivingMuted = Color(0xFF6D8178)
private val LivingBlue = Color(0xFF24AEE4)
private val LivingGold = Color(0xFFFFC857)
private val LivingPink = Color(0xFFFF787D)

internal object EveLivingRoomRuntime {
    var open by mutableStateOf(false)
        private set

    fun show() { open = true }
    fun hide() { open = false }
}

internal object EveTravelRuntime {
    val areas = listOf("Ana Sayfa", "Oyna", "Arena", "Sosyal", "Profil")
    var areaIndex by mutableIntStateOf(0)
        private set

    val currentArea: String get() = areas[areaIndex]

    fun next() {
        areaIndex = (areaIndex + 1) % areas.size
    }

    fun moveTo(index: Int) {
        if (index in areas.indices) areaIndex = index
    }
}

private enum class LivingPanel { INFO, CHAT, STYLE, ROOM }

private data class StyleChoice(val id: String, val emoji: String, val tr: String, val en: String)
private data class RoomChoice(val id: String, val emoji: String, val tr: String, val en: String)

private val styleChoices = listOf(
    StyleChoice("default_white", "🤍", "Doğal Beyaz", "Natural White"),
    StyleChoice("leaf_charm", "🍃", "Yaprak Kolye", "Leaf Charm"),
    StyleChoice("forest_crown", "👑", "Orman Tacı", "Forest Crown"),
    StyleChoice("cozy_scarf", "🧣", "Sıcak Atkı", "Cozy Scarf"),
)

private val roomChoices = listOf(
    RoomChoice("enchanted_forest", "🌿", "Büyülü Orman", "Enchanted Forest"),
    RoomChoice("cozy_nest", "🛏️", "Sıcak Yuva", "Cozy Nest"),
    RoomChoice("starlight_grove", "✨", "Yıldız Korusu", "Starlight Grove"),
)

/** Production UI for Eve's home, care loop, friendship, chat, Style and room selection. */
@Composable
internal fun EveLivingRoomScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf(LivingPanel.INFO) }
    var editName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(store.name) }
    var reactionNonce by remember { mutableIntStateOf(0) }
    var friendshipFeedback by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var chatReply by remember { mutableStateOf(EveMascotRuntime.bubbleText.ifBlank { sh("Bugün birlikte harika oynayacağız! ✨", "We are going to have a great time today! ✨") }) }
    var chatSending by remember { mutableStateOf(false) }
    revision

    fun react(message: String, friendship: Int = 0) {
        EveMascotRuntime.setBubble(message)
        chatReply = message
        reactionNonce++
        friendshipFeedback = if (friendship > 0) "+$friendship ${sh("Dostluk", "Friendship")}" else ""
        revision++
    }

    fun sendChat() {
        val message = chatInput.trim()
        if (message.isBlank() || chatSending) return
        chatInput = ""
        chatSending = true
        EveMascotRuntime.thinking()
        scope.launch {
            runCatching {
                EveAiChatService.chat(
                    EveChatRequest(
                        message = message,
                        history = emptyList(),
                        language = SonHarfUiState.language,
                        playerName = null,
                        companionName = store.name,
                        gameContext = "Son Harf maskot evi. Dostluk seviyesi ${store.friendshipLevel}, ilerleme ${store.affection}/100.",
                    ),
                )
            }.onSuccess { response ->
                EveMascotRuntime.apply(response)
                store.recordConversationMood(response.mood.toEveMood())
                store.chatBond()
                chatReply = response.reply
                friendshipFeedback = "+2 ${sh("Dostluk", "Friendship")}"
                reactionNonce++
                revision++
            }.onFailure {
                val fallback = sh("Şu an bağlantım biraz yavaş. Ben yine de yanındayım. 🌿", "My connection is a little slow, but I am still here with you. 🌿")
                EveMascotRuntime.calm()
                EveMascotRuntime.setBubble(fallback)
                chatReply = fallback
            }
            chatSending = false
        }
    }

    Box(Modifier.fillMaxSize().background(LivingForestDeep).imePadding()) {
        LivingRoomBackground(store.selectedRoom, Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {
            LivingHeader(
                store = store,
                onClose = onClose,
                onEditName = { nameDraft = store.name; editName = true },
                onStyle = { panel = LivingPanel.STYLE },
                onRoom = { panel = LivingPanel.ROOM },
            )

            LivingFriendshipBar(store)

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                color = Color.White.copy(alpha = .10f),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .24f)),
            ) {
                Box(Modifier.fillMaxWidth().height(316.dp)) {
                    LivingFurniture(store.selectedRoom)
                    EveLivingStage(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(.74f).height(282.dp),
                        reactionNonce = reactionNonce,
                        onTap = {
                            val points = store.pet()
                            EveMascotRuntime.petReaction()
                            react(sh("Bunu sevdim! Biraz daha sev beni. 💚", "I loved that! Pet me again. 💚"), points)
                        },
                    )
                    StyleAccessory(store.selectedStyle, Modifier.align(Alignment.Center))

                    if (chatReply.isNotBlank()) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).fillMaxWidth(.47f),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = .96f),
                            shadowElevation = 5.dp,
                        ) {
                            Text(
                                chatReply.take(125),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = LivingInk,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (friendshipFeedback.isNotBlank()) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                            color = Color(0xFF2A9D61).copy(alpha = .94f),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("💚 $friendshipFeedback", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            LivingVitals(store.vitals())

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                LivingActionButton("🤍", sh("Sev", "Pet"), LivingPink, Modifier.weight(1f)) {
                    val points = store.pet()
                    EveMascotRuntime.petReaction()
                    react(sh("Mırır! Çok güzel. 🤍", "Purr! That feels great. 🤍"), points)
                }
                LivingActionButton("🍎", sh("Besle", "Feed"), Color(0xFF72C94E), Modifier.weight(1f)) {
                    val points = store.quickFeed()
                    if (points > 0) {
                        EveMascotRuntime.feedReaction()
                        react(sh("Mmm! Tam kararında. 🍎", "Mmm! Just right. 🍎"), points)
                    } else {
                        react(sh("Mama için biraz daha yaprak toplamamız gerek. 🍃", "We need a few more leaves for food. 🍃"))
                    }
                }
                LivingActionButton("🎁", sh("Hediye", "Gift"), LivingGold, Modifier.weight(1f)) {
                    if (store.claimDailyGift()) {
                        EveMascotRuntime.giftReaction()
                        react(sh("Bunu senin için buldum! 🎁✨", "I found this for you! 🎁✨"), 4)
                    } else {
                        react(sh("Bugünkü hediyemizi aldık. Yarın yenisi var! 🎁", "We claimed today's gift. A new one comes tomorrow! 🎁"))
                    }
                }
                LivingActionButton("💬", sh("Sohbet", "Chat"), LivingBlue, Modifier.weight(1f)) {
                    panel = LivingPanel.CHAT
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color(0xFFF8FFF9).copy(alpha = .98f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, Color(0xFFBCE2CC)),
            ) {
                when (panel) {
                    LivingPanel.INFO -> LivingTravelInfo(store)
                    LivingPanel.CHAT -> LivingChatPanel(chatInput, { chatInput = it.take(600) }, chatReply, chatSending, ::sendChat)
                    LivingPanel.STYLE -> LivingStylePanel(store, revision) { id ->
                        if (store.selectStyle(id)) {
                            react(sh("Bu Style bana çok yakıştı! ✨", "This Style looks great on me! ✨"))
                            panel = LivingPanel.INFO
                        }
                    }
                    LivingPanel.ROOM -> LivingRoomPanel(store, revision) { id ->
                        if (store.selectRoom(id)) {
                            react(sh("Yeni odamı çok sevdim! 🏡", "I love my new room! 🏡"))
                            panel = LivingPanel.INFO
                        }
                    }
                }
            }
        }
    }

    if (editName) {
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(sh("Maskot adını değiştir", "Rename companion")) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(18) },
                    singleLine = true,
                    label = { Text(sh("Maskot adı", "Companion name")) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.name = nameDraft
                    nameDraft = store.name
                    react(sh("Tamam! Bundan sonra adım ${store.name}. 🌿", "Done! My name is ${store.name} now. 🌿"))
                    editName = false
                }) { Text(sh("Kaydet", "Save")) }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(sh("Vazgeç", "Cancel")) } },
        )
    }
}

@Composable
private fun LivingHeader(
    store: EveCompanionStore,
    onClose: () -> Unit,
    onEditName: () -> Unit,
    onStyle: () -> Unit,
    onRoom: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onClose, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("‹ ${sh("Geri", "Back")}", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(store.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            TextButton(onClick = onEditName, contentPadding = PaddingValues(4.dp)) { Text("✏️", fontSize = 15.sp) }
        }
        Surface(color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(14.dp)) {
            Text("🍃 ${store.leaves}", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
        OutlinedButton(onClick = onStyle, border = BorderStroke(1.dp, Color.White.copy(alpha = .4f)), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
            Text("👕 Style", color = Color.White, fontSize = 10.sp)
        }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = onRoom, border = BorderStroke(1.dp, Color.White.copy(alpha = .4f)), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
            Text("🛋️ ${sh("Oda", "Room")}", color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LivingFriendshipBar(store: EveCompanionStore) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("💗 ${sh("Dostluk Seviyesi", "Friendship Level")} ${store.friendshipLevel}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${store.affection}/100", color = Color.White.copy(alpha = .88f), fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { store.affection / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = Color(0xFF7ED75E),
            trackColor = Color.White.copy(alpha = .18f),
        )
    }
}

@Composable
private fun LivingRoomBackground(roomId: String, modifier: Modifier) {
    val colors = when (roomId) {
        "cozy_nest" -> listOf(Color(0xFF3C3024), Color(0xFF6D5738), Color(0xFF153F35))
        "starlight_grove" -> listOf(Color(0xFF152A45), Color(0xFF31557C), Color(0xFF0B443D))
        else -> listOf(LivingForestDeep, LivingForestMid, Color(0xFF0A4A3C))
    }
    Canvas(modifier.background(Brush.verticalGradient(colors))) {
        val w = size.width
        val h = size.height
        repeat(10) { i ->
            val x = w * ((i * 17 + 7) % 100) / 100f
            val y = h * (.10f + ((i * 31) % 64) / 100f)
            drawCircle(Color(0xFF90D9A2).copy(alpha = .10f), w * (.09f + (i % 2) * .025f), Offset(x, y))
        }
        repeat(26) { i ->
            val x = w * ((i * 43 + 11) % 100) / 100f
            val y = h * (.06f + ((i * 29) % 78) / 100f)
            drawCircle(if (i % 3 == 0) LivingGold.copy(alpha = .72f) else Color.White.copy(alpha = .45f), if (i % 4 == 0) 3.1f else 1.6f, Offset(x, y))
        }
    }
}

@Composable
private fun LivingFurniture(roomId: String) {
    Box(Modifier.fillMaxSize()) {
        Text(if (roomId == "starlight_grove") "🔮" else "🪴", fontSize = 34.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp))
        Text(if (roomId == "cozy_nest") "🛏️" else "🧺", fontSize = 40.sp, modifier = Modifier.align(Alignment.BottomStart).padding(17.dp))
        Text("🧶", fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-92).dp, y = (-3).dp))
        Text("🥣", fontSize = 30.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(x = 92.dp, y = (-3).dp))
        Text(if (roomId == "starlight_grove") "🌙" else "✨", fontSize = 23.sp, modifier = Modifier.align(Alignment.TopStart).padding(18.dp))
    }
}

@Composable
private fun EveLivingStage(modifier: Modifier, reactionNonce: Int, onTap: () -> Unit) {
    val context = LocalContext.current
    val assetAvailable = remember {
        runCatching { context.assets.open(EveAssetPolicy.MODEL_ASSET).use { }; true }.getOrDefault(false)
    }
    val idle = rememberInfiniteTransition(label = "eve_idle")
    val y by idle.animateFloat(
        initialValue = -2f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "eve_breathe_y",
    )
    val sway by idle.animateFloat(
        initialValue = -1.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "eve_sway",
    )
    var reacting by remember { mutableStateOf(false) }
    LaunchedEffect(reactionNonce) {
        if (reactionNonce == 0) return@LaunchedEffect
        reacting = true
        delay(340)
        reacting = false
    }
    val reactionScale by animateFloatAsState(
        targetValue = if (reacting) 1.09f else 1f,
        animationSpec = spring(dampingRatio = .48f, stiffness = 410f),
        label = "eve_reaction",
    )

    Box(
        modifier.graphicsLayer {
            translationY = y
            rotationZ = sway
            scaleX = reactionScale
            scaleY = reactionScale
        },
        contentAlignment = Alignment.Center,
    ) {
        if (assetAvailable) {
            Eve3DStage(Modifier.fillMaxSize())
        } else {
            EveMark(Modifier.fillMaxSize(.72f))
        }
        Box(Modifier.fillMaxSize().clickable(onClick = onTap))
    }
}

@Composable
private fun StyleAccessory(styleId: String, modifier: Modifier = Modifier) {
    val accessory = when (styleId) {
        "leaf_charm" -> "🍃"
        "forest_crown" -> "👑"
        "cozy_scarf" -> "🧣"
        else -> ""
    }
    if (accessory.isNotBlank()) {
        Text(
            accessory,
            fontSize = if (styleId == "forest_crown") 31.sp else 27.sp,
            modifier = modifier.offset(y = if (styleId == "forest_crown") (-104).dp else 58.dp),
        )
    }
}

@Composable
private fun LivingVitals(vitals: EveVitalSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        color = Color.White.copy(alpha = .95f),
        shape = RoundedCornerShape(23.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            VitalCell("🙂", sh("Mutlu", "Happy"), vitals.happiness)
            VitalCell("🥣", sh("Tok", "Full"), vitals.fullness)
            VitalCell("⚡", sh("Enerjik", "Energetic"), vitals.energy)
        }
    }
}

@Composable
private fun VitalCell(icon: String, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(5.dp))
        Column {
            Text(label, color = LivingInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("$value/100", color = LivingMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun LivingActionButton(icon: String, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 17.sp)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun LivingTravelInfo(store: EveCompanionStore) {
    Row(Modifier.fillMaxSize().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = LivingMint, border = BorderStroke(2.dp, LivingGreen), modifier = Modifier.size(58.dp)) {
            EveMark(Modifier.padding(6.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("${store.name} ${sh("seninle geziyor!", "travels with you!")} 🐾", color = LivingInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(sh("Menüler arasında yanında kalır: Ana Sayfa • Oyna • Arena • Sosyal • Profil", "Stays with you across menus: Home • Play • Arena • Social • Profile"), color = LivingMuted, fontSize = 9.sp, lineHeight = 12.sp)
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(progress = { store.affection / 100f }, modifier = Modifier.fillMaxWidth().height(5.dp), color = LivingGreen, trackColor = LivingMint)
        }
    }
}

@Composable
private fun LivingChatPanel(
    input: String,
    onInput: (String) -> Unit,
    reply: String,
    sending: Boolean,
    onSend: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(sh("Eve ile Sohbet", "Chat with Eve"), color = LivingInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(15.dp), color = LivingMint) {
            Text(if (sending) sh("Düşünüyor…", "Thinking…") else reply.ifBlank { sh("Buradayım. 🌿", "I am here. 🌿") }, Modifier.padding(10.dp), color = LivingInk, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text(sh("Bir şey söyle…", "Say something…"), fontSize = 10.sp) })
            Button(onClick = onSend, enabled = input.isNotBlank() && !sending, colors = ButtonDefaults.buttonColors(containerColor = LivingBlue), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 12.dp)) {
                Text("➤")
            }
        }
    }
}

@Composable
private fun LivingStylePanel(store: EveCompanionStore, revision: Int, onSelect: (String) -> Unit) {
    revision
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Style", color = LivingInk, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        items(styleChoices) { choice ->
            ChoiceRow(choice.emoji, sh(choice.tr, choice.en), store.selectedStyle == choice.id) { onSelect(choice.id) }
        }
    }
}

@Composable
private fun LivingRoomPanel(store: EveCompanionStore, revision: Int, onSelect: (String) -> Unit) {
    revision
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text(sh("Oda Seç", "Choose Room"), color = LivingInk, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        items(roomChoices) { choice ->
            ChoiceRow(choice.emoji, sh(choice.tr, choice.en), store.selectedRoom == choice.id) { onSelect(choice.id) }
        }
    }
}

@Composable
private fun ChoiceRow(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) Color(0xFFE5F7EA) else Color.White,
        border = BorderStroke(1.dp, if (selected) LivingGreen else Color(0xFFD8E8DE)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(9.dp))
            Text(label, Modifier.weight(1f), color = LivingInk, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp)
            Text(if (selected) "✓" else "›", color = if (selected) LivingGreen else LivingMuted, fontWeight = FontWeight.Black)
        }
    }
}
