package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ForestScreenDeep = Color(0xFF073B32)
private val ForestScreenMid = Color(0xFF176B52)
private val ForestScreenMint = Color(0xFFEAF8EF)
private val ForestScreenInk = Color(0xFF173B35)
private val ForestScreenBlue = Color(0xFF24AEE4)
private val ForestScreenGreen = Color(0xFF55BC75)
private val ForestScreenGold = Color(0xFFFFC857)

private data class ForestChoice(val id: String, val icon: String, val label: String)

private val forestStyleChoices = listOf(
    ForestChoice("default_white", "🤍", "Doğal Beyaz"),
    ForestChoice("leaf_charm", "🍃", "Yaprak Kolye"),
    ForestChoice("forest_crown", "👑", "Orman Tacı"),
    ForestChoice("cozy_scarf", "🧣", "Sıcak Atkı"),
)

private val forestRoomChoices = listOf(
    ForestChoice("enchanted_forest", "🌿", "Büyülü Orman"),
    ForestChoice("cozy_nest", "🛏️", "Sıcak Yuva"),
    ForestChoice("starlight_grove", "✨", "Yıldız Korusu"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EveForestScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }

    var revision by remember { mutableIntStateOf(0) }
    var reactionNonce by remember { mutableIntStateOf(0) }
    var speechBubbleText by remember { mutableStateOf(store.greeting()) }
    var feedback by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var chatSending by remember { mutableStateOf(false) }
    var styleDialog by remember { mutableStateOf(false) }
    var roomDialog by remember { mutableStateOf(false) }
    revision

    val progress = store.progress()
    val xpProgress = progress.xp / progress.xpToNextLevel.coerceAtLeast(1).toFloat()

    fun levelSuffix(beforeLevel: Int): String {
        if (store.friendshipLevel <= beforeLevel) return ""
        val level = store.friendshipLevel
        val feature = store.featureUnlockedAtCurrentLevel()
        val reward = "🎉 Seviye $level! +${EveCompanionRules.levelRewardGold(level)} Altın, +${EveCompanionRules.levelRewardDiamonds(level)} Elmas"
        return if (feature != null) "$reward · 🔓 $feature" else reward
    }

    fun react(
        message: String,
        xp: Int = 0,
        beforeLevel: Int = store.friendshipLevel,
        animation: () -> Unit,
    ) {
        val suffix = levelSuffix(beforeLevel)
        speechBubbleText = if (suffix.isBlank()) message else "$message\n$suffix"
        EveMascotRuntime.setBubble(speechBubbleText)
        animation()
        feedback = if (xp > 0) "+$xp XP" else ""
        reactionNonce++
        revision++
    }

    fun sendChat() {
        val message = chatInput.trim()
        if (message.isBlank() || chatSending) return
        chatInput = ""
        chatSending = true
        focusManager.clearFocus()
        keyboardController?.hide()
        speechBubbleText = sh("Düşünüyorum…", "Thinking…")
        EveMascotRuntime.thinking()

        scope.launch {
            val before = store.friendshipLevel
            runCatching {
                EveAiChatService.chat(
                    EveChatRequest(
                        message = message,
                        history = emptyList(),
                        language = SonHarfUiState.language,
                        playerName = null,
                        companionName = store.name,
                        gameContext = "Son Harf EVE odası. Seviye ${store.friendshipLevel}, XP ${store.affection}/${store.xpToNextLevel}.",
                    ),
                )
            }.onSuccess { response ->
                EveMascotRuntime.apply(response)
                val xp = store.chatBond()
                val suffix = levelSuffix(before)
                speechBubbleText = if (suffix.isBlank()) response.reply else "${response.reply}\n$suffix"
                EveMascotRuntime.setBubble(speechBubbleText)
                feedback = "+$xp XP"
                reactionNonce++
                revision++
            }.onFailure {
                val fallback = sh(
                    "Şu an bağlantım yavaş ama buradayım. 🌿",
                    "My connection is slow, but I am here. 🌿",
                )
                speechBubbleText = fallback
                EveMascotRuntime.calm()
                EveMascotRuntime.setBubble(fallback)
            }
            chatSending = false
        }
    }

    BackHandler {
        focusManager.clearFocus()
        keyboardController?.hide()
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        EveMascotRuntime.calm()
        EveMascotRuntime.setBubble(speechBubbleText)
    }

    val roomGradient = when (store.selectedRoom) {
        "cozy_nest" -> listOf(Color(0xFF3C3024), Color(0xFF6D5738), Color(0xFF153F35))
        "starlight_grove" -> listOf(Color(0xFF152A45), Color(0xFF31557C), Color(0xFF0B443D))
        else -> listOf(ForestScreenDeep, ForestScreenMid, Color(0xFF0A4A3C))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(roomGradient)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            EveForestHeader(
                store = store,
                onBack = onNavigateBack,
                onStyle = { styleDialog = true },
                onRoom = { roomDialog = true },
            )

            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "💗 ${sh("Dostluk Seviyesi", "Friendship Level")} ${progress.level}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Text(
                        "${progress.xp}/${progress.xpToNextLevel} XP",
                        color = Color.White.copy(alpha = .9f),
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { xpProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = Color(0xFF7ED75E),
                    trackColor = Color.White.copy(alpha = .18f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = .09f)),
            ) {
                EveAnimatedStage(
                    modifier = Modifier.fillMaxSize(),
                    reactionNonce = reactionNonce,
                    onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        val before = store.friendshipLevel
                        val xp = store.pet()
                        if (xp > 0) {
                            react(
                                message = sh("Beni sevdiğin için çok mutluyum! 🤍", "I am so happy you petted me! 🤍"),
                                xp = xp,
                                beforeLevel = before,
                                animation = { EveMascotRuntime.petReaction() },
                            )
                        } else {
                            react(
                                message = sh("Biraz dinleneyim; bugünkü sevgi limitimizi doldurduk. 🤍", "Let me rest; today's petting limit is full. 🤍"),
                                animation = { EveMascotRuntime.play(EveAnimationCue.REST, returnToIdleAfterMs = 1_800) },
                            )
                        }
                    },
                )

                EveForestFurniture(store.selectedRoom)
                EveStyleOverlay(store.selectedStyle)

                if (speechBubbleText.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 44.dp, end = 44.dp),
                        color = Color(0xFFF8FFF9).copy(alpha = .96f),
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 8.dp,
                    ) {
                        Text(
                            text = speechBubbleText.take(260),
                            color = ForestScreenInk,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }

                if (feedback.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                        color = Color(0xFF2A9D61).copy(alpha = .95f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            "✨ $feedback",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    EveForestActionButton(
                        icon = "🍎",
                        label = "${sh("Besle", "Feed")} ${progress.dailyFeedCount}/${progress.maxDailyFeed}",
                        color = Color(0xFF66BB6A),
                        enabled = progress.dailyFeedCount < progress.maxDailyFeed,
                    ) {
                        val before = store.friendshipLevel
                        val xp = store.quickFeed()
                        if (xp > 0) {
                            react(
                                message = sh("Nefis! Enerjim yerine geldi. 🍎", "Delicious! My energy is back. 🍎"),
                                xp = xp,
                                beforeLevel = before,
                                animation = { EveMascotRuntime.feedReaction() },
                            )
                        } else {
                            val message = if (!store.canFeedToday()) {
                                sh("Bugün yeterince yedim. Yarın yine beslersin. 🥣", "I ate enough today. Feed me again tomorrow. 🥣")
                            } else {
                                sh("Mama için biraz daha yaprak toplamamız gerek. 🍃", "We need a few more leaves for food. 🍃")
                            }
                            react(message = message, animation = { EveMascotRuntime.play(EveAnimationCue.IDLE_LOOK_AROUND) })
                        }
                    }

                    EveForestActionButton(
                        icon = "🎁",
                        label = if (store.giftAvailable()) sh("Hediye Al", "Gift") else sh("Alındı", "Claimed"),
                        color = ForestScreenGold,
                        enabled = store.giftAvailable(),
                    ) {
                        val before = store.friendshipLevel
                        if (store.claimDailyGift()) {
                            react(
                                message = sh("Sana ormandan bir sürpriz getirdim! 🎁✨", "I brought you a forest surprise! 🎁✨"),
                                xp = 4,
                                beforeLevel = before,
                                animation = { EveMascotRuntime.giftReaction() },
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    color = ForestScreenDeep.copy(alpha = .78f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        "🤍 ${sh("Sev", "Pet")} ${progress.dailyPetCount}/${progress.maxDailyPet}",
                        modifier = Modifier
                            .clickable {
                                val before = store.friendshipLevel
                                val xp = store.pet()
                                if (xp > 0) {
                                    react(
                                        message = sh("Mırır! Çok güzel. 🤍", "Purr! That feels great. 🤍"),
                                        xp = xp,
                                        beforeLevel = before,
                                        animation = { EveMascotRuntime.petReaction() },
                                    )
                                } else {
                                    react(
                                        message = sh("Bugünkü sevgi limitimizi doldurduk. 🤍", "Today's petting limit is full. 🤍"),
                                        animation = { EveMascotRuntime.play(EveAnimationCue.REST, returnToIdleAfterMs = 1_800) },
                                    )
                                }
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    )
                }
            }

            EveForestChatBar(
                input = chatInput,
                onInput = { chatInput = it.take(600) },
                sending = chatSending,
                onSend = ::sendChat,
                focusRequester = focusRequester,
                bringIntoViewRequester = bringIntoViewRequester,
            )
        }
    }

    if (styleDialog) {
        EveChoiceDialog(
            title = "Style",
            choices = forestStyleChoices,
            selectedId = store.selectedStyle,
            onDismiss = { styleDialog = false },
        ) { id ->
            if (store.selectStyle(id)) {
                styleDialog = false
                revision++
                react(
                    message = sh("Bu Style bana çok yakıştı! ✨", "This Style looks great on me! ✨"),
                    animation = { EveMascotRuntime.happyReaction() },
                )
            }
        }
    }

    if (roomDialog) {
        EveChoiceDialog(
            title = sh("Oda Seç", "Choose Room"),
            choices = forestRoomChoices,
            selectedId = store.selectedRoom,
            onDismiss = { roomDialog = false },
        ) { id ->
            if (store.selectRoom(id)) {
                roomDialog = false
                revision++
                react(
                    message = sh("Yeni odamı çok sevdim! 🏡", "I love my new room! 🏡"),
                    animation = { EveMascotRuntime.happyReaction() },
                )
            }
        }
    }
}

@Composable
private fun EveForestHeader(
    store: EveCompanionStore,
    onBack: () -> Unit,
    onStyle: () -> Unit,
    onRoom: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(38.dp).clickable(onClick = onBack),
            shape = CircleShape,
            color = Color.White.copy(alpha = .16f),
        ) {
            Box(contentAlignment = Alignment.Center) { Text("‹", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(store.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(sh("Büyülü orman arkadaşın", "Your enchanted forest companion"), color = Color.White.copy(alpha = .75f), fontSize = 9.sp)
        }
        OutlinedButton(
            onClick = onStyle,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .42f)),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
        ) { Text("👕 Style", color = Color.White, fontSize = 9.sp) }
        Spacer(Modifier.width(5.dp))
        OutlinedButton(
            onClick = onRoom,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .42f)),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
        ) { Text("🛋️ ${sh("Oda", "Room")}", color = Color.White, fontSize = 9.sp) }
    }
}

@Composable
private fun EveAnimatedStage(
    modifier: Modifier,
    reactionNonce: Int,
    onTap: () -> Unit,
) {
    val idle = rememberInfiniteTransition(label = "eve_forest_idle")
    val y by idle.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1_850), RepeatMode.Reverse),
        label = "eve_forest_idle_y",
    )
    val sway by idle.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2_500), RepeatMode.Reverse),
        label = "eve_forest_idle_sway",
    )
    var reacting by remember { mutableStateOf(false) }
    LaunchedEffect(reactionNonce) {
        if (reactionNonce == 0) return@LaunchedEffect
        reacting = true
        delay(420)
        reacting = false
    }
    val reactionScale by animateFloatAsState(
        targetValue = if (reacting) 1.10f else 1f,
        animationSpec = spring(dampingRatio = .48f, stiffness = 390f),
        label = "eve_forest_reaction_scale",
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationY = y
            rotationZ = sway
            scaleX = reactionScale
            scaleY = reactionScale
        },
        contentAlignment = Alignment.Center,
    ) {
        Eve3DStage(Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EveForestChatBar(
    input: String,
    onInput: (String) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
) {
    val scope = rememberCoroutineScope()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color(0xFF14241B).copy(alpha = .97f),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = input,
                onValueChange = onInput,
                enabled = !sending,
                singleLine = true,
                placeholder = { Text(sh("EVE ile konuş…", "Talk to EVE…"), color = Color(0xFF81C784).copy(alpha = .7f), fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch { bringIntoViewRequester.bringIntoView() }
                        }
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = onSend,
                enabled = input.isNotBlank() && !sending,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ForestScreenBlue),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(if (sending) "…" else "➤", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun EveForestActionButton(
    icon: String,
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(50.dp).clickable(enabled = enabled, onClick = onClick),
            shape = CircleShape,
            color = if (enabled) color else Color.Gray.copy(alpha = .35f),
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 22.sp) }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (enabled) Color.White else Color.White.copy(alpha = .45f),
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun EveForestFurniture(roomId: String) {
    Box(Modifier.fillMaxSize()) {
        Text(if (roomId == "starlight_grove") "🌙" else "✨", fontSize = 25.sp, modifier = Modifier.align(Alignment.TopStart).padding(18.dp))
        Text(if (roomId == "cozy_nest") "🛏️" else "🧺", fontSize = 38.sp, modifier = Modifier.align(Alignment.BottomStart).padding(18.dp))
        Text(if (roomId == "starlight_grove") "🔮" else "🪴", fontSize = 34.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp))
    }
}

@Composable
private fun EveStyleOverlay(styleId: String) {
    val accessory = when (styleId) {
        "leaf_charm" -> "🍃"
        "forest_crown" -> "👑"
        "cozy_scarf" -> "🧣"
        else -> ""
    }
    if (accessory.isNotBlank()) {
        Text(
            accessory,
            fontSize = if (styleId == "forest_crown") 30.sp else 26.sp,
            modifier = Modifier.padding(top = if (styleId == "forest_crown") 116.dp else 230.dp),
        )
    }
}

@Composable
private fun EveChoiceDialog(
    title: String,
    choices: List<ForestChoice>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                choices.forEach { choice ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(choice.id) },
                        shape = RoundedCornerShape(13.dp),
                        color = if (selectedId == choice.id) ForestScreenMint else Color.White,
                        border = BorderStroke(1.dp, if (selectedId == choice.id) ForestScreenGreen else Color(0xFFD8E8DE)),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(choice.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(9.dp))
                            Text(choice.label, Modifier.weight(1f), color = ForestScreenInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(if (selectedId == choice.id) "✓" else "›", color = ForestScreenGreen)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("Kapat", "Close")) } },
    )
}
