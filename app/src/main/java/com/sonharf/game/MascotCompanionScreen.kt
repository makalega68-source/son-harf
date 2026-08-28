package com.sonharf.game

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

private enum class MascotCompanionTab { CHAT, CARE, MEMORY }

@Composable
internal fun MascotCompanionScreen(
    backend: OnlineGameBackend?,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenShop: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { MascotSelectionRuntime.load(context) }

    val mascotId = MascotSelectionRuntime.selectedId
    val catalog = MascotCatalog.item(mascotId)
    val character = LetharaLore.characterForMascot(mascotId)
    var progress by remember(mascotId) { mutableStateOf<MascotProgressDto?>(null) }
    var roomState by remember(mascotId) { mutableStateOf<MascotRoomStateDto?>(null) }
    var fruits by remember { mutableStateOf<List<MascotFruitDto>>(emptyList()) }
    var inventory by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var tab by remember { mutableStateOf(MascotCompanionTab.CHAT) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    suspend fun reload() {
        val b = backend ?: return
        val id = b.currentUserId()
        profile = id?.let { runCatching { b.getProfile(it) }.getOrNull() }
        progress = runCatching { b.getMascotProgress(mascotId) }.getOrNull()
        roomState = runCatching { b.getMascotRoomState(mascotId) }.getOrNull()
        fruits = runCatching { b.getMascotFruitCatalog() }.getOrDefault(emptyList())
        inventory = runCatching { b.getMascotFruitInventory() }.getOrDefault(emptyList()).associate { it.fruitId to it.quantity }
        progress?.let {
            MascotRuntime.syncProgress(it.totalXp, it.level)
            MascotRuntime.rename(it.petName)
        }
    }

    LaunchedEffect(mascotId) { reload() }

    LaunchedEffect(progress?.fullness, progress?.energy) {
        val p = progress ?: return@LaunchedEffect
        when {
            p.energy <= 30 -> MascotRuntime.react(MascotMotion.SIT)
            p.fullness <= 30 -> MascotRuntime.react(MascotMotion.LOOK_AT_PLAYER)
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(LetharaPalette.Night, LetharaPalette.Night2, Color(0xFF211344)))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = LetharaPalette.Text)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayName = progress?.petName ?: if (SonHarfUiState.isEnglish) catalog.nameEn else catalog.nameTr
                    Text(displayName.uppercase(), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(character.name + " • " + if (SonHarfUiState.isEnglish) character.titleEn else character.titleTr, color = character.color, fontSize = 9.sp)
                }
                IconButton(onClick = onOpenRoom) {
                    Icon(Icons.Rounded.Home, sh("Mühür Odası", "Seal Room"), tint = LetharaPalette.Gold)
                }
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Rounded.AutoStories, sh("Hikâye", "Story"), tint = LetharaPalette.Cyan)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = LetharaPalette.Panel,
                border = BorderStroke(1.dp, character.color.copy(alpha = .45f)),
            ) {
                Box(Modifier.fillMaxWidth().height(255.dp)) {
                    MascotLive3DStage(Modifier.fillMaxSize(), mascotId = mascotId)
                    progress?.let { p ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = LetharaPalette.PanelStrong,
                        ) {
                            Text(
                                sh("Seviye", "Level") + " " + p.level + "  •  ♥ " + (roomState?.friendshipLevel ?: 1) + "  •  ✦ " + p.memoryFragments + "/120",
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = LetharaPalette.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    if (MascotRuntime.message.isNotBlank()) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(10.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = .92f),
                        ) {
                            Text(
                                MascotRuntime.message,
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color(0xFF18213C),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                CompanionTabButton(sh("SOHBET", "CHAT"), tab == MascotCompanionTab.CHAT, Modifier.weight(1f)) { tab = MascotCompanionTab.CHAT }
                CompanionTabButton(sh("BAKIM", "CARE"), tab == MascotCompanionTab.CARE, Modifier.weight(1f)) { tab = MascotCompanionTab.CARE }
                CompanionTabButton(sh("HAFIZA", "MEMORY"), tab == MascotCompanionTab.MEMORY, Modifier.weight(1f)) { tab = MascotCompanionTab.MEMORY }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = LetharaPalette.PanelStrong,
            ) {
                when (tab) {
                    MascotCompanionTab.CHAT -> MascotChatPanel(
                        context = context,
                        character = character,
                        mascotId = mascotId,
                        companionName = progress?.petName ?: character.name,
                        playerName = profile?.displayName,
                        playerWins = profile?.wins,
                        playerLosses = profile?.losses,
                        progress = progress,
                    )
                    MascotCompanionTab.CARE -> MascotCarePanel(
                        progress = progress,
                        fruits = fruits,
                        inventory = inventory,
                        notice = notice,
                        loading = loading,
                        onRename = { value ->
                            val b = backend
                            if (b != null) {
                                scope.launch {
                                    loading = true
                                    runCatching { b.renameMascot(mascotId, value) }
                                        .onSuccess {
                                            progress = it
                                            MascotRuntime.rename(it.petName)
                                            notice = sh("İsim mühüre işlendi.", "The name was written into the seal.")
                                        }
                                        .onFailure { notice = sh("İsim değiştirilemedi.", "The name could not be changed.") }
                                    loading = false
                                }
                            }
                        },
                        onCare = { action ->
                            val b = backend
                            if (b != null) {
                                scope.launch {
                                    loading = true
                                    runCatching { b.careForMascotV2(mascotId, action) }
                                        .onSuccess { care ->
                                            val actionText = when (action) {
                                                "love" -> sh("Yoldaşının mührü sıcak bir ışıkla parladı.", "Your companion's seal glowed with warm light.")
                                                "play" -> sh("Kısa bir büyü oyunu yaptınız.", "You shared a short spell game.")
                                                else -> sh("Mührün tozu temizlendi; yoldaşın rahatladı.", "The seal dust cleared; your companion relaxed.")
                                            }
                                            notice = if (care.friendshipGained > 0) {
                                                actionText + "  +" + care.friendshipGained + " " + sh("Dostluk XP", "Friendship XP")
                                            } else {
                                                actionText
                                            }
                                            MascotRuntime.react(
                                                when (action) {
                                                    "play" -> MascotMotion.RUN
                                                    "love" -> MascotMotion.LOOK_AT_PLAYER
                                                    else -> MascotMotion.GREETING
                                                }
                                            )
                                            reload()
                                        }
                                        .onFailure { notice = sh("Bakım etkileşimi tamamlanamadı.", "Care interaction could not be completed.") }
                                    loading = false
                                }
                            }
                        },
                        onFeed = { fruit ->
                            val b = backend
                            if (b != null) {
                                scope.launch {
                                    loading = true
                                    runCatching { b.feedMascot(mascotId, fruit.id) }
                                        .onSuccess {
                                            notice = "+" + it.xpGained + " XP • " + sh("Hafıza kıvılcımı güçlendi.", "The memory spark grew stronger.")
                                            MascotRuntime.react(MascotMotion.GREETING)
                                            reload()
                                        }
                                        .onFailure { error ->
                                            val raw = error.message.orEmpty()
                                            notice = when {
                                                "normal_fruit_daily_limit" in raw -> sh("Bugünkü 3 normal meyve hakkı kullanıldı.", "Today's 3 normal fruit uses are complete.")
                                                "fruit_not_owned" in raw -> sh("Bu büyülü meyve çantanda yok.", "You do not own this magic fruit.")
                                                else -> sh("Maskot beslenemedi.", "The mascot could not be fed.")
                                            }
                                        }
                                    loading = false
                                }
                            }
                        },
                        onOpenShop = onOpenShop,
                    )
                    MascotCompanionTab.MEMORY -> MascotMemoryPanel(character, progress, roomState, onOpenHistory, onOpenRoom)
                }
            }
        }
    }
}

@Composable
private fun CompanionTabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) LetharaPalette.Gold else LetharaPalette.Panel),
        contentPadding = PaddingValues(vertical = 9.dp, horizontal = 4.dp),
    ) {
        Text(label, color = if (selected) Color(0xFF201A35) else LetharaPalette.Text, fontWeight = FontWeight.Black, fontSize = 10.sp)
    }
}

@Composable
private fun MascotChatPanel(
    context: Context,
    character: WizardLoreCharacter,
    mascotId: String,
    companionName: String,
    playerName: String?,
    playerWins: Int?,
    playerLosses: Int?,
    progress: MascotProgressDto?,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val json = remember { Json { ignoreUnknownKeys = true } }
    val prefs = remember(mascotId) { context.getSharedPreferences("lethara_chat_" + mascotId, Context.MODE_PRIVATE) }
    val serializer = remember { ListSerializer(MascotChatTurn.serializer()) }
    var voiceEnabled by remember(mascotId) { mutableStateOf(prefs.getBoolean("voice_enabled", false)) }
    var ttsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context, SonHarfUiState.language) {
        WordMeaningRuntime.init(context)
        val engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        engine.language = if (SonHarfUiState.isEnglish) Locale.ENGLISH else Locale("tr", "TR")
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            if (tts === engine) tts = null
        }
    }
    val history = remember(mascotId) {
        mutableStateListOf<MascotChatTurn>().apply {
            val raw = prefs.getString("history", null)
            if (!raw.isNullOrBlank()) {
                addAll(runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList()))
            }
        }
    }
    val memoryNotes = remember(mascotId) {
        mutableStateListOf<String>().apply {
            addAll(
                prefs.getString("memory_notes", "")
                    .orEmpty()
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(12)
                    .toList()
            )
        }
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    val daySeed = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) }
    val dailyQuest = remember(character.key, daySeed, playerName) {
        val seed = kotlin.math.abs(character.key.hashCode() + daySeed)
        when (seed % 3) {
            0 -> sh("Bugün 1 düello kazan; bir hafıza kıvılcımını uyandır.", "Win 1 duel today and awaken a memory spark.")
            1 -> sh("Bugün 3 düelloyu tamamla; Söz Dokusu’nun ritmini koru.", "Complete 3 duels today and keep the rhythm of the Word Weave.")
            else -> sh("Bugün 2 düello kazan; mühür yankısını güçlendir.", "Win 2 duels today and strengthen the seal echo.")
        }
    }

    fun persist() {
        prefs.edit()
            .putString("history", json.encodeToString(serializer, history.takeLast(30)))
            .putString("memory_notes", memoryNotes.takeLast(12).joinToString("\n"))
            .putBoolean("voice_enabled", voiceEnabled)
            .apply()
    }

    fun requestedDefinitionWord(message: String): String? {
        val clean = message.trim().replace(Regex("[?!.,;:]"), "")
        val tr = Regex("^(.{1,32}?)\\s+(ne demek|ne anlama gelir|anlamı nedir|anlamı)$", RegexOption.IGNORE_CASE).find(clean)
            ?.groupValues?.getOrNull(1)?.trim()?.split(Regex("\\s+"))?.lastOrNull()
        if (!tr.isNullOrBlank()) return tr
        val en = Regex("^what does\\s+([A-Za-z'-]{1,32})\\s+mean$", RegexOption.IGNORE_CASE).find(clean)
            ?.groupValues?.getOrNull(1)?.trim()
        return en?.takeIf { it.isNotBlank() }
    }

    fun speak(text: String) {
        if (!voiceEnabled || !ttsReady || text.isBlank()) return
        tts?.speak(text.take(420), TextToSpeech.QUEUE_FLUSH, null, "lethara_mascot_reply")
    }

    fun send() {
        val message = input.trim()
        if (message.isBlank() || sending) return
        val previous = history.toList()
        history += MascotChatTurn("user", message)
        persist()
        input = ""
        sending = true
        MascotRuntime.react(MascotMotion.THINKING)
        scope.launch {
            val requestedWord = requestedDefinitionWord(message)
            val response = if (requestedWord != null) {
                val meaning = runCatching { WordMeaningRuntime.meaning(requestedWord, SonHarfUiState.language) }
                    .getOrElse { sh("Bu kelimenin kısa anlamını şu an bulamadım.", "I could not recover a short meaning for that word just now.") }
                MascotChatResponse(
                    reply = if (SonHarfUiState.isEnglish) {
                        "The Word Weave remembers “" + requestedWord + "”: " + meaning
                    } else {
                        "Söz Dokusu “" + requestedWord + "” için şunu hatırlıyor: " + meaning
                    },
                    mood = "curious",
                    animation = "idle_breathe",
                    memoryNote = "",
                    usedFallback = true,
                )
            } else MascotAiChatService.chat(
                MascotChatRequest(
                    message = message,
                    history = previous,
                    language = SonHarfUiState.language,
                    playerName = playerName,
                    companionName = companionName,
                    gameContext = progress?.let {
                        val record = if (playerWins != null && playerLosses != null) {
                            " Player record: " + playerWins + " wins, " + playerLosses + " losses."
                        } else ""
                        val memories = if (memoryNotes.isEmpty()) "" else " Stable remembered notes: " + memoryNotes.joinToString(" | ")
                        "Mascot level " + it.level + "; XP " + it.totalXp + "; memory fragments " + it.memoryFragments + "/120; fullness " + it.fullness + "; happiness " + it.happiness + "." + record + memories
                    },
                    mascotId = mascotId,
                    mascotTitle = if (SonHarfUiState.isEnglish) character.titleEn else character.titleTr,
                    mascotPersonality = if (SonHarfUiState.isEnglish) character.archetypeEn + "; " + character.temperamentEn else character.archetypeTr + "; " + character.temperamentTr,
                    loreContext = if (SonHarfUiState.isEnglish) LetharaLore.introEn else LetharaLore.introTr,
                )
            )
            history += MascotChatTurn("assistant", response.reply)
            response.memoryNote?.trim()?.takeIf { it.length in 3..180 }?.let { note ->
                if (note !in memoryNotes) {
                    memoryNotes += note
                    while (memoryNotes.size > 12) memoryNotes.removeAt(0)
                }
            }
            persist()
            speak(response.reply)
            MascotRuntime.react(
                when (response.mood) {
                    "celebrating", "happy" -> MascotMotion.VICTORY
                    "thinking", "curious" -> MascotMotion.THINKING
                    "tired" -> MascotMotion.SIT
                    "encouraging", "supportive" -> MascotMotion.LOOK_AT_PLAYER
                    else -> MascotMotion.IDLE
                }
            )
            sending = false
        }
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                sh(
                    "Bu yoldaş insan gibi konuşmaz; Lethara'nın diliyle kısa ve karakterli yanıtlar verir.",
                    "This companion does not speak like a human; replies stay short and in Lethara's character.",
                ),
                modifier = Modifier.weight(1f),
                color = LetharaPalette.Muted,
                fontSize = 9.sp,
            )
            IconButton(
                onClick = {
                    voiceEnabled = !voiceEnabled
                    persist()
                    if (!voiceEnabled) tts?.stop()
                },
                enabled = ttsReady,
            ) {
                Icon(
                    if (voiceEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    sh("Maskot sesi", "Mascot voice"),
                    tint = if (voiceEnabled) character.color else LetharaPalette.Muted,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            shape = RoundedCornerShape(14.dp),
            color = LetharaPalette.Gold.copy(alpha = .09f),
            border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha = .25f)),
        ) {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                Text(sh("BUGÜNÜN MÜHÜR GÖREVİ", "TODAY'S SEAL QUEST"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text(dailyQuest, color = LetharaPalette.Text, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (history.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = character.color.copy(alpha = .12f)) {
                        Text(
                            "“" + LetharaLore.randomWhisper(character, SonHarfUiState.language, progress?.totalXp ?: 0) + "”",
                            Modifier.padding(12.dp),
                            color = LetharaPalette.Text,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            items(history) { turn ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (turn.role == "user") Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(.84f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (turn.role == "user") LetharaPalette.Cyan.copy(alpha = .18f) else character.color.copy(alpha = .13f),
                    ) {
                        Text(turn.text, Modifier.padding(11.dp), color = LetharaPalette.Text, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
            if (sending) {
                item {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth(),
                        color = character.color,
                        trackColor = Color.White.copy(alpha = .08f),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().imePadding().padding(8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(700) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(sh("Yoldaşına bir şey söyle…", "Say something to your companion…")) },
                maxLines = 3,
                enabled = !sending,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LetharaPalette.Text,
                    unfocusedTextColor = LetharaPalette.Text,
                    focusedBorderColor = character.color,
                    unfocusedBorderColor = Color.White.copy(alpha = .20f),
                    cursorColor = character.color,
                ),
            )
            FilledIconButton(
                onClick = ::send,
                enabled = input.isNotBlank() && !sending,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = character.color),
            ) {
                Icon(Icons.Rounded.Send, sh("Gönder", "Send"), tint = Color(0xFF11152D))
            }
        }
    }
}

@Composable
private fun MascotCarePanel(
    progress: MascotProgressDto?,
    fruits: List<MascotFruitDto>,
    inventory: Map<String, Int>,
    notice: String?,
    loading: Boolean,
    onRename: (String) -> Unit,
    onCare: (String) -> Unit,
    onFeed: (MascotFruitDto) -> Unit,
    onOpenShop: () -> Unit,
) {
    var name by remember(progress?.petName) { mutableStateOf(progress?.petName.orEmpty()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(sh("MÜHÜR BAKIMI", "SEAL CARE"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(
                sh(
                    "Bakım ve meyveler yalnızca yoldaşlık, görünüm ve hikâye ilerlemesini etkiler; maç gücü vermez.",
                    "Care and fruit only affect companionship, cosmetics and story progression; never match power.",
                ),
                color = LetharaPalette.Muted,
                fontSize = 9.sp,
            )
        }

        progress?.let { p ->
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = LetharaPalette.Panel,
                    border = BorderStroke(1.dp, LetharaPalette.Cyan.copy(alpha = .35f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sh("Seviye", "Level") + " " + p.level, color = LetharaPalette.Text, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Text((p.totalXp % 100).toString() + "/100 XP", color = LetharaPalette.Cyan, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (p.totalXp % 100) / 100f },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = LetharaPalette.Cyan,
                            trackColor = Color.White.copy(alpha = .09f),
                        )
                        Text(
                            "💚 " + p.happiness + "   🍽 " + p.fullness + "   ⚡ " + p.energy + "   ✦ " + p.memoryFragments + "/120",
                            color = LetharaPalette.Muted,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(18) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(sh("Maskot adı", "Mascot name")) },
                    )
                    Button(onClick = { if (name.trim().length >= 2) onRename(name.trim()) }) {
                        Text(sh("Kaydet", "Save"))
                    }
                }
            }
        }

        item {
            Text(sh("YOLDAŞ ETKİLEŞİMLERİ", "COMPANION INTERACTIONS"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = { onCare("love") },
                    modifier = Modifier.weight(1f),
                    enabled = !loading,
                    border = BorderStroke(1.dp, Color(0xFFFF8BCB).copy(alpha=.65f)),
                ) {
                    Text("❤ " + sh("SEV", "LOVE"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = { onCare("play") },
                    modifier = Modifier.weight(1f),
                    enabled = !loading,
                    border = BorderStroke(1.dp, LetharaPalette.Cyan.copy(alpha=.65f)),
                ) {
                    Text("✦ " + sh("OYNA", "PLAY"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = { onCare("groom") },
                    modifier = Modifier.weight(1f),
                    enabled = !loading,
                    border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha=.65f)),
                ) {
                    Text("✨ " + sh("BAKIM", "GROOM"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            Text(sh("LETHARA MEYVELERİ", "LETHARA FRUIT"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        items(fruits, key = { it.id }) { fruit ->
            val count = inventory[fruit.id] ?: 0
            val normal = !fruit.isMagic
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LetharaPalette.Panel,
                border = BorderStroke(1.dp, if (fruit.isMagic) LetharaPalette.Gold.copy(alpha = .35f) else LetharaPalette.Green.copy(alpha = .35f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (normal) "🍎" else if (fruit.xpReward == 10) "🌙" else if (fruit.xpReward == 20) "⭐" else "🔮", fontSize = 30.sp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (SonHarfUiState.isEnglish) fruit.nameEn else fruit.nameTr, color = LetharaPalette.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        val detail = if (normal) {
                            "+" + fruit.xpReward + " XP • " + (progress?.normalFruitUsedToday ?: 0) + "/3"
                        } else {
                            "+" + fruit.xpReward + " XP • " + sh("Çanta", "Bag") + ": " + count
                        }
                        Text(detail, color = if (fruit.isMagic) LetharaPalette.Gold else LetharaPalette.Green, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onFeed(fruit) },
                        enabled = !loading && (normal || count > 0),
                        colors = ButtonDefaults.buttonColors(containerColor = if (fruit.isMagic) LetharaPalette.Violet else LetharaPalette.Green),
                    ) {
                        Text(sh("YEDİR", "FEED"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenShop,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, LetharaPalette.Gold),
            ) {
                Icon(Icons.Rounded.ShoppingBag, null)
                Spacer(Modifier.width(6.dp))
                Text(sh("BÜYÜLÜ MEYVE MAĞAZASI", "MAGIC FRUIT SHOP"), fontWeight = FontWeight.Black)
            }
        }

        notice?.let {
            item {
                Text(
                    it,
                    color = LetharaPalette.Cyan,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MascotMemoryPanel(
    character: WizardLoreCharacter,
    progress: MascotProgressDto?,
    roomState: MascotRoomStateDto?,
    onOpenHistory: () -> Unit,
    onOpenRoom: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Text(
                character.name + " — " + if (SonHarfUiState.isEnglish) character.titleEn else character.titleTr,
                color = LetharaPalette.Gold,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            Text(
                if (SonHarfUiState.isEnglish) character.nameMeaningEn else character.nameMeaningTr,
                color = LetharaPalette.Text,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
        item {
            Text(if (SonHarfUiState.isEnglish) character.archetypeEn else character.archetypeTr, color = character.color, fontWeight = FontWeight.Bold)
            Text(if (SonHarfUiState.isEnglish) character.temperamentEn else character.temperamentTr, color = LetharaPalette.Muted, fontSize = 10.sp)
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFF8BCB).copy(alpha = .08f),
                border = BorderStroke(1.dp, Color(0xFFFF8BCB).copy(alpha = .30f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♥ " + sh("Dostluk Seviyesi", "Friendship Level") + " " + (roomState?.friendshipLevel ?: 1), color = Color(0xFFFFA6D6), fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        Text(((roomState?.friendshipXp ?: 0) % 40).toString() + "/40", color = LetharaPalette.Muted, fontSize = 10.sp)
                    }
                    LinearProgressIndicator(
                        progress = { (((roomState?.friendshipXp ?: 0) % 40) / 40f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = Color(0xFFFF8BCB),
                        trackColor = Color.White.copy(alpha = .08f),
                    )
                    Text(sh("Dostluk, yeni oda mühürlerini ve hikâye bölümlerini açar.", "Friendship unlocks new room seals and story chapters."), color = LetharaPalette.Muted, fontSize = 9.sp)
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = character.color.copy(alpha = .10f),
                border = BorderStroke(1.dp, character.color.copy(alpha = .35f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text(sh("HAFIZA YANKISI", "MEMORY ECHO"), color = character.color, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "“" + LetharaLore.randomWhisper(character, SonHarfUiState.language, progress?.totalXp ?: 0) + "”",
                        color = LetharaPalette.Text,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenRoom,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, LetharaPalette.Cyan),
                ) {
                    Text(sh("MÜHÜR ODASI", "SEAL ROOM"), fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
                Button(
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = LetharaPalette.Gold, contentColor = Color(0xFF201A35)),
                ) {
                    Text(sh("HİKÂYE", "STORY"), fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
            }
        }
    }
}
