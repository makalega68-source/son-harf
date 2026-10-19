package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object ChatColors {
    val AppBackground: Color get() = SonHarfTheme.Background
    val ElevatedBackground: Color get() = SonHarfTheme.SurfaceElevated
    val PrimarySurface: Color get() = SonHarfTheme.Surface
    val PrimaryBlue: Color get() = SonHarfTheme.SoftBlue
    val DeepBlue: Color get() = Color(0xFF1F4FB5)
    val TacticalTurquoise: Color get() = SonHarfTheme.Turquoise
    val PlayGreen: Color get() = SonHarfTheme.Success
    val Lavender: Color get() = SonHarfTheme.Lavender
    val RewardAmber: Color get() = SonHarfTheme.Warning
    val Danger: Color get() = SonHarfTheme.Error
    val TextPrimary: Color get() = SonHarfTheme.TextPrimary
    val TextSecondary: Color get() = SonHarfTheme.TextSecondary
    val TextTertiary: Color get() = SonHarfTheme.DisabledContent
    val Divider: Color get() = SonHarfTheme.Border
}

private object ChatShapes {
    val Medium = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(18.dp)
}

private val ChatScreenHorizontal = 16.dp

@Composable
internal fun MascotChatScreen(onBack: () -> Unit, mascotSkin: WordSiegeMascotSkin = WordSiegeMascotSkin.ORB) {
    SecureChatContent()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val nanoEngine = remember { MascotNanoEngine() }
    val language = SonHarfUiState.language
    val isTurkish = language.lowercase(Locale.ROOT).startsWith("tr")

    val messages = remember {
        mutableStateListOf(
            MascotChatMessage(
                id = 1L,
                text = if (isTurkish) {
                    "Selam! Burası sadece ikimizin sohbet köşesi. İstersen oyunu konuşalım, istersen biraz kafanı dağıtalım."
                } else {
                    "Hey! This is our little chat corner. We can talk about the game or just switch off for a while."
                },
                fromMascot = true,
            ),
        )
    }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var nanoAvailability by remember { mutableStateOf(MascotNanoAvailability.CHECKING) }
    var downloadBytes by remember { mutableStateOf<Long?>(null) }
    var mascotActionKey by remember { mutableLongStateOf(1L) }
    var mascotAction by remember { mutableStateOf<WordSiegeMascotAction?>(WordSiegeMascotAction.PEEK) }

    fun submit(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank() || thinking) return
        val contextBeforeMessage = messages.toList()
        val userMessage = MascotChatMessage(
            id = System.nanoTime(),
            text = text,
            fromMascot = false,
        )
        messages.add(userMessage)
        input = ""
        thinking = true

        scope.launch {
            val localResult = ObiLocalAssistant.handle(context, text, isTurkish)
            if (localResult.handled) {
                messages.add(
                    MascotChatMessage(
                        id = System.nanoTime(),
                        text = localResult.reply.orEmpty(),
                        fromMascot = true,
                    ),
                )
                thinking = false
                mascotActionKey += 1L
                mascotAction = WordSiegeMascotAction.NOD
                return@launch
            }
            val prompt = MascotConversation.prompt(contextBeforeMessage, text, language, mascotSkin)
            val nanoReply = if (nanoAvailability == MascotNanoAvailability.READY) {
                nanoEngine.generate(prompt)
            } else {
                null
            }
            val reply = nanoReply ?: MascotConversation.localFallback(text, language, messages.count { !it.fromMascot })
            messages.add(
                MascotChatMessage(
                    id = System.nanoTime(),
                    text = reply,
                    fromMascot = true,
                ),
            )
            thinking = false
            mascotActionKey += 1L
            mascotAction = WordSiegeMascotAction.NOD
        }
    }

    LaunchedEffect(Unit) {
        do {
            nanoAvailability = nanoEngine.checkAvailability()
            if (nanoAvailability == MascotNanoAvailability.DOWNLOADING) delay(1_500)
        } while (nanoAvailability == MascotNanoAvailability.DOWNLOADING)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, thinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex + if (thinking) 1 else 0)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ChatColors.AppBackground)
            .imePadding(),
    ) {
        MascotChatHeader(onBack = {
            onBack()
        })

        NanoStatusCard(
            state = nanoAvailability,
            bytes = downloadBytes,
            isTurkish = isTurkish,
            onDownload = {
                if (nanoAvailability != MascotNanoAvailability.DOWNLOADABLE) return@NanoStatusCard
                scope.launch {
                    nanoAvailability = MascotNanoAvailability.DOWNLOADING
                    val success = nanoEngine.download { state, bytes ->
                        nanoAvailability = state
                        downloadBytes = bytes
                    }
                    nanoAvailability = if (success) MascotNanoAvailability.READY else nanoEngine.checkAvailability()
                    downloadBytes = null
                }
            },
        )

        ObiLocalToolsCard(isTurkish)

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(306.8.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ChatColors.TacticalTurquoise.copy(alpha = .18f),
                                ChatColors.PrimaryBlue.copy(alpha = .08f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                WordSiegeMascot(
                    moveId = null,
                    lastMoveMine = false,
                    pendingCells = emptyList(),
                    playerTurn = true,
                    requestedEmotion = when {
                        thinking -> WordSiegeMascotEmotion.FOCUS
                        else -> WordSiegeMascotEmotion.HAPPY
                    },
                    modifier = Modifier.size(286.dp),
                    actionKey = mascotActionKey,
                    action = mascotAction,
                    skin = mascotSkin,
                    onTap = {
                        mascotActionKey += 1L
                        mascotAction = WordSiegeMascotAction.HOP
                    },
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ChatScreenHorizontal, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MascotMessageBubble(message)
            }
            if (thinking) {
                item(key = "thinking") {
                    MascotThinkingBubble(isTurkish)
                }
            }
        }

        ChatComposer(
            input = input,
            onInputChange = { input = it },
            enabled = !thinking,
            onSend = { submit(input) },
            isTurkish = isTurkish,
        )
    }
}

@Composable
private fun MascotChatHeader(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = ChatColors.TextPrimary)
        }
        Column(Modifier.weight(1f)) {
            Text(
                sh("Obi Yerel", "Obi Local"),
                color = ChatColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                sh("Notlar ve hatırlatıcılar telefonunda kalır", "Notes and reminders stay on your phone"),
                color = ChatColors.TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ObiLocalToolsCard(isTurkish: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ChatScreenHorizontal, vertical = 3.dp),
        shape = ChatShapes.Medium,
        color = ChatColors.PrimarySurface,
        border = BorderStroke(1.dp, ChatColors.PlayGreen.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(
                if (isTurkish) "İnternetsiz asistan" else "Offline assistant",
                color = ChatColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (isTurkish) "\"Not al ...\" · \"20 dakika zamanlayıcı başlat\" · \"Yarın saat 09.00'da ... diye hatırlat\"" else "\"Take a note ...\" · \"Start a 20 minute timer\" · \"Remind me tomorrow at 9 ...\"",
                color = ChatColors.TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun NanoStatusCard(
    state: MascotNanoAvailability,
    bytes: Long?,
    isTurkish: Boolean,
    onDownload: () -> Unit,
) {
    val ready = state == MascotNanoAvailability.READY
    val accent = if (ready) ChatColors.PlayGreen else ChatColors.TacticalTurquoise
    val title = when (state) {
        MascotNanoAvailability.CHECKING -> if (isTurkish) "Cihaz AI kontrol ediliyor" else "Checking on-device AI"
        MascotNanoAvailability.READY -> "Gemini Nano • ${if (isTurkish) "cihazda hazır" else "ready on device"}"
        MascotNanoAvailability.DOWNLOADABLE -> if (isTurkish) "Gemini Nano indirilmeye hazır" else "Gemini Nano is ready to download"
        MascotNanoAvailability.DOWNLOADING -> if (isTurkish) "Gemini Nano indiriliyor" else "Downloading Gemini Nano"
        MascotNanoAvailability.UNAVAILABLE -> if (isTurkish) "Bu cihazda yerel sohbet modu" else "Local fallback chat on this device"
    }
    val subtitle = when (state) {
        MascotNanoAvailability.READY -> if (isTurkish) "Sohbet üretimi desteklenen cihazlarda telefonun içinde çalışır." else "On supported devices, replies are generated on the phone."
        MascotNanoAvailability.DOWNLOADABLE -> if (isTurkish) "Bir kez indir; sonra maskot cihaz içi modeli kullanabilsin." else "Download once so the mascot can use the on-device model."
        MascotNanoAvailability.DOWNLOADING -> bytes?.let {
            val mb = it / (1024L * 1024L)
            if (isTurkish) "$mb MB indirildi" else "$mb MB downloaded"
        } ?: if (isTurkish) "Model hazırlanıyor…" else "Preparing model…"
        MascotNanoAvailability.UNAVAILABLE -> if (isTurkish) "Sohbet yine çalışır; bulut AI anahtarı kullanılmaz." else "Chat still works without a cloud AI key."
        else -> if (isTurkish) "Kısa bir kontrol yapılıyor." else "Running a quick check."
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ChatScreenHorizontal, vertical = 4.dp),
        shape = ChatShapes.Medium,
        color = ChatColors.PrimarySurface,
        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(9.dp).background(accent, CircleShape))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ChatColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = ChatColors.TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
            }
            if (state == MascotNanoAvailability.DOWNLOADABLE) {
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onDownload, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.CloudDownload, if (isTurkish) "İndir" else "Download", tint = ChatColors.TacticalTurquoise)
                }
            }
        }
    }
}

@Composable
private fun MascotMessageBubble(message: MascotChatMessage) {
    val alignment = if (message.fromMascot) Alignment.CenterStart else Alignment.CenterEnd
    val background = if (message.fromMascot) ChatColors.PrimarySurface else ChatColors.DeepBlue
    val border = if (message.fromMascot) ChatColors.Lavender.copy(alpha = .30f) else ChatColors.PrimaryBlue.copy(alpha = .45f)
    Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = ChatShapes.Large,
            color = background,
            border = BorderStroke(1.dp, border),
        ) {
            Text(
                message.text,
                Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                color = if (message.fromMascot) ChatColors.TextPrimary else Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MascotThinkingBubble(isTurkish: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = ChatShapes.Large,
            color = ChatColors.PrimarySurface,
            border = BorderStroke(1.dp, ChatColors.TacticalTurquoise.copy(alpha = .25f)),
        ) {
            Row(
                Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = ChatColors.TacticalTurquoise,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isTurkish) "Düşünüyorum…" else "Thinking…", color = ChatColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ChatComposer(
    input: String,
    onInputChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
    isTurkish: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ChatColors.ElevatedBackground,
        border = BorderStroke(1.dp, ChatColors.Divider),
    ) {
        Column(Modifier.padding(horizontal = ChatScreenHorizontal, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    maxLines = 3,
                    placeholder = {
                        Text(
                            if (isTurkish) "Maskota bir şey söyle…" else "Say something to the mascot…",
                            color = ChatColors.TextTertiary,
                        )
                    },
                    shape = ChatShapes.Large,
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled && input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ChatColors.PrimaryBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.Send, if (isTurkish) "Gönder" else "Send")
                }
            }
        }
    }
}
