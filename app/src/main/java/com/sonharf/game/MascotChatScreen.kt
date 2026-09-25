package com.sonharf.game

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class MascotVoiceController(
    context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit,
) : TextToSpeech.OnInitListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false

    init {
        engine = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        val tts = engine ?: return
        if (status != TextToSpeech.SUCCESS) return

        val locale = Locale("tr", "TR")
        val languageResult = tts.setLanguage(locale)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
        }

        val localVoice = tts.voices
            ?.filter { voice ->
                !voice.isNetworkConnectionRequired &&
                    (voice.locale.language == locale.language || voice.locale.language == Locale.ENGLISH.language)
            }
            ?.maxByOrNull { it.quality }
        if (localVoice != null) tts.voice = localVoice

        // A slightly brighter but slower delivery: soft/cute without becoming sharp or squeaky.
        tts.setPitch(1.08f)
        tts.setSpeechRate(.93f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { onSpeakingChanged(true) }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post { onSpeakingChanged(false) }
            }

            override fun onError(utteranceId: String?) {
                mainHandler.post { onSpeakingChanged(false) }
            }
        })
        ready = true
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mascot-${System.currentTimeMillis()}")
    }

    fun stop() {
        engine?.stop()
        onSpeakingChanged(false)
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}

@Composable
internal fun MascotChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val nanoEngine = remember { MascotNanoEngine() }
    val language = SonHarfUiState.language
    val isTurkish = language.lowercase(Locale.ROOT).startsWith("tr")
    val languageTag = if (isTurkish) "tr-TR" else "en-US"

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
    var voiceMode by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    var recognizedUtterance by remember { mutableStateOf<String?>(null) }
    var nanoAvailability by remember { mutableStateOf(MascotNanoAvailability.CHECKING) }
    var downloadBytes by remember { mutableStateOf<Long?>(null) }
    var mascotActionKey by remember { mutableLongStateOf(1L) }
    var mascotAction by remember { mutableStateOf<WordSiegeMascotAction?>(WordSiegeMascotAction.INTRO) }

    val voiceController = remember {
        MascotVoiceController(context) { isSpeaking -> speaking = isSpeaking }
    }
    DisposableEffect(Unit) {
        onDispose { voiceController.shutdown() }
    }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else {
            null
        }
    }
    DisposableEffect(recognizer, languageTag) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                speechError = null
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                listening = false
                speechError = if (isTurkish) {
                    "Sesini anlayamadım. Mikrofona tekrar dokunabilirsin."
                } else {
                    "I couldn't catch that. Tap the microphone and try again."
                }
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!text.isNullOrBlank()) recognizedUtterance = text
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
        }
    }

    fun startVoiceInput() {
        if (recognizer == null) {
            speechError = if (isTurkish) "Bu cihazda konuşma tanıma kullanılamıyor." else "Speech recognition isn't available on this device."
            return
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        speechError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer.startListening(intent)
        listening = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceInput()
        else speechError = if (isTurkish) "Sesli sohbet için mikrofon izni gerekiyor." else "Microphone permission is required for voice chat."
    }

    fun requestVoiceInput() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun submit(rawText: String, speakReply: Boolean) {
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
        speechError = null

        scope.launch {
            val prompt = MascotConversation.prompt(contextBeforeMessage, text, language)
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
            mascotAction = WordSiegeMascotAction.WIGGLE
            if (speakReply) voiceController.speak(reply)
        }
    }

    LaunchedEffect(Unit) {
        do {
            nanoAvailability = nanoEngine.checkAvailability()
            if (nanoAvailability == MascotNanoAvailability.DOWNLOADING) delay(1_500)
        } while (nanoAvailability == MascotNanoAvailability.DOWNLOADING)
    }

    LaunchedEffect(recognizedUtterance) {
        val spokenText = recognizedUtterance ?: return@LaunchedEffect
        recognizedUtterance = null
        input = spokenText
        submit(spokenText, speakReply = true)
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
            .background(GameColors.AppBackground)
            .imePadding(),
    ) {
        MascotChatHeader(onBack = {
            voiceController.stop()
            recognizer?.cancel()
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

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(236.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                GameColors.TacticalTurquoise.copy(alpha = .18f),
                                GameColors.PrimaryBlue.copy(alpha = .08f),
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
                        speaking -> WordSiegeMascotEmotion.SPEAKING
                        thinking -> WordSiegeMascotEmotion.FOCUS
                        else -> WordSiegeMascotEmotion.HAPPY
                    },
                    modifier = Modifier.size(220.dp),
                    speaking = speaking,
                    actionKey = mascotActionKey,
                    action = mascotAction,
                    onTap = {
                        mascotActionKey += 1L
                        mascotAction = WordSiegeMascotAction.WAVE
                    },
                )
            }
        }

        ModeSelector(
            voiceMode = voiceMode,
            isTurkish = isTurkish,
            onText = {
                voiceMode = false
                voiceController.stop()
                recognizer?.cancel()
                listening = false
            },
            onVoice = { voiceMode = true },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 10.dp),
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

        if (speechError != null) {
            Text(
                text = speechError.orEmpty(),
                modifier = Modifier.padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 2.dp),
                color = GameColors.RewardAmber,
                fontSize = 11.sp,
            )
        }

        ChatComposer(
            input = input,
            onInputChange = { input = it },
            voiceMode = voiceMode,
            listening = listening,
            enabled = !thinking,
            isTurkish = isTurkish,
            onMic = {
                if (listening) {
                    recognizer?.cancel()
                    listening = false
                } else {
                    requestVoiceInput()
                }
            },
            onSend = { submit(input, speakReply = voiceMode) },
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
            Icon(Icons.Rounded.ArrowBack, gameText("Geri", "Back"), tint = GameColors.TextPrimary)
        }
        Column(Modifier.weight(1f)) {
            Text(
                gameText("Maskotla Sohbet", "Mascot Chat"),
                color = GameColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                gameText("Sadece burada konuşur", "Only speaks here"),
                color = GameColors.TextSecondary,
                fontSize = 11.sp,
            )
        }
        Icon(Icons.Rounded.VolumeUp, null, tint = GameColors.Lavender, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
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
    val accent = if (ready) GameColors.PlayGreen else GameColors.TacticalTurquoise
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
            .padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 4.dp),
        shape = GameShapes.Medium,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(9.dp).background(accent, CircleShape))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = GameColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = GameColors.TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
            }
            if (state == MascotNanoAvailability.DOWNLOADABLE) {
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onDownload, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.CloudDownload, if (isTurkish) "İndir" else "Download", tint = GameColors.TacticalTurquoise)
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(
    voiceMode: Boolean,
    isTurkish: Boolean,
    onText: () -> Unit,
    onVoice: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        FilterChip(
            selected = !voiceMode,
            onClick = onText,
            label = { Text(if (isTurkish) "Yazılı" else "Text") },
            leadingIcon = { Icon(Icons.Rounded.Keyboard, null, modifier = Modifier.size(17.dp)) },
        )
        Spacer(Modifier.width(10.dp))
        FilterChip(
            selected = voiceMode,
            onClick = onVoice,
            label = { Text(if (isTurkish) "Sesli" else "Voice") },
            leadingIcon = { Icon(Icons.Rounded.Mic, null, modifier = Modifier.size(17.dp)) },
        )
    }
}

@Composable
private fun MascotMessageBubble(message: MascotChatMessage) {
    val alignment = if (message.fromMascot) Alignment.CenterStart else Alignment.CenterEnd
    val background = if (message.fromMascot) GameColors.PrimarySurface else GameColors.DeepBlue
    val border = if (message.fromMascot) GameColors.Lavender.copy(alpha = .30f) else GameColors.PrimaryBlue.copy(alpha = .45f)
    Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = GameShapes.Large,
            color = background,
            border = BorderStroke(1.dp, border),
        ) {
            Text(
                message.text,
                Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MascotThinkingBubble(isTurkish: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = GameShapes.Large,
            color = GameColors.PrimarySurface,
            border = BorderStroke(1.dp, GameColors.TacticalTurquoise.copy(alpha = .25f)),
        ) {
            Row(
                Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = GameColors.TacticalTurquoise,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isTurkish) "Düşünüyorum…" else "Thinking…", color = GameColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ChatComposer(
    input: String,
    onInputChange: (String) -> Unit,
    voiceMode: Boolean,
    listening: Boolean,
    enabled: Boolean,
    isTurkish: Boolean,
    onMic: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
    ) {
        Column(Modifier.padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 10.dp)) {
            if (voiceMode) {
                Text(
                    if (listening) {
                        if (isTurkish) "Seni dinliyorum…" else "I'm listening…"
                    } else {
                        if (isTurkish) "Mikrofona dokun; cevabımı sesli vereyim." else "Tap the microphone and I'll answer out loud."
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    color = if (listening) GameColors.PlayGreen else GameColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (voiceMode) {
                    FilledIconButton(
                        onClick = onMic,
                        enabled = enabled,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (listening) GameColors.Danger else GameColors.TacticalTurquoise,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(if (listening) Icons.Rounded.StopCircle else Icons.Rounded.Mic, null)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    maxLines = 3,
                    placeholder = {
                        Text(
                            if (isTurkish) "Maskota bir şey söyle…" else "Say something to the mascot…",
                            color = GameColors.TextTertiary,
                        )
                    },
                    shape = GameShapes.Large,
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled && input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GameColors.PrimaryBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.Send, if (isTurkish) "Gönder" else "Send")
                }
            }
        }
    }
}
