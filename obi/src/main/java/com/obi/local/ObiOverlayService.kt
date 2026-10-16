package com.obi.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.view.inputmethod.EditorInfo
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.math.roundToInt

/** User-started, user-stoppable overlay. No accessibility or screen-reading permission is used. */
class ObiOverlayService : Service() {
    companion object {
        const val ACTION_START = "com.obi.local.START_OVERLAY"
        const val ACTION_STOP = "com.obi.local.STOP_OVERLAY"
        const val ACTION_SHOW = "com.obi.local.SHOW_OVERLAY"
        const val ACTION_READY = "com.obi.local.OVERLAY_READY"
        const val ACTION_ERROR = "com.obi.local.OVERLAY_ERROR"
        private const val CHANNEL = "obi_companion"
        private const val NOTIFICATION_ID = 1201
        private const val PREFS = "obi_overlay"
        private const val SIZE_DP = 112

        fun isRunning(context: Context): Boolean =
            context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("running", false)
    }

    private val windows by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var mascot: WordSiegeMascotView? = null
    private var mascotParams: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private var chatMascot: WordSiegeMascotView? = null
    private var bubble: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastTapAt = 0L
    private var lastInteractionAt = System.currentTimeMillis()
    private var actionKey = 0L
    private val mind by lazy { ObiMind(this) }
    private val gemini by lazy { ObiGemini(this) }
    private val moves = ArrayDeque<WordSiegeMascotAction>()
    private val touchReplies = ArrayDeque<ObiReply>()
    private val recentTouches = ArrayDeque<String>()
    private var touchPackLoading = false
    private var lastTouchFetchAt = 0L
    private val celebration = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_SHOW) {
                mascot?.visibility = View.VISIBLE
                fetchTouchPack()
                return
            }
            animate(WordSiegeMascotAction.CHEER)
            showBubble(intent.getStringExtra("text") ?: "Yaşasın! ✨")
        }
    }
    private fun nextMove(): WordSiegeMascotAction {
        if (moves.isEmpty()) moves.addAll(listOf(
            WordSiegeMascotAction.SPACE_FLIGHT, WordSiegeMascotAction.CHEER, WordSiegeMascotAction.NUZZLE,
            WordSiegeMascotAction.LOVE, WordSiegeMascotAction.BOUNCE, WordSiegeMascotAction.WINK,
            WordSiegeMascotAction.KISS, WordSiegeMascotAction.DANCE, WordSiegeMascotAction.CLAP,
            WordSiegeMascotAction.SHY, WordSiegeMascotAction.GIGGLE, WordSiegeMascotAction.HOP,
            WordSiegeMascotAction.PEEK, WordSiegeMascotAction.SWAY, WordSiegeMascotAction.CHASE_RUN
        ).shuffled())
        return moves.removeFirst()
    }
    private val ambient = object : Runnable {
        override fun run() {
            if (mascot != null && panel == null) {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val action = when {
                    hour >= 23 || hour < 7 -> WordSiegeMascotAction.DOZE
                    System.currentTimeMillis() - lastInteractionAt > 30 * 60_000L -> WordSiegeMascotAction.YAWN
                    else -> listOf(WordSiegeMascotAction.LOOK_AROUND, WordSiegeMascotAction.SWAY, WordSiegeMascotAction.WAVE).random()
                }
                animate(action)
            }
            handler.postDelayed(this, 30_000L)
        }
    }
    private val store by lazy { ObiStore(this) }
    private val weather by lazy { ObiWeather() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply { addAction("com.obi.local.CELEBRATE"); addAction(ACTION_SHOW) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(celebration, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(celebration, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SHOW) {
            mascot?.visibility = View.VISIBLE
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            notifyEntry(ACTION_ERROR)
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startForegroundNotification()
            if (mascot == null) {
                addMascot()
                handler.postDelayed(ambient, 8_000L)
                handler.postDelayed({ fetchTouchPack() }, 1_200L)
            } else mascot?.visibility = View.VISIBLE
            notifyEntry(ACTION_READY)
        } catch (_: RuntimeException) {
            notifyEntry(ACTION_ERROR)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun notifyEntry(action: String) {
        sendBroadcast(Intent(action).setPackage(packageName))
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Obi ekranda", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, ObiEntryActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, ObiOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle("Obi ekranında")
            .setContentText("Dokun: söz ve hareket • Çift dokun: yazış • Sürükle: taşı")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Obi'yi kapat", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun addMascot() {
        val size = dp(SIZE_DP)
        val bounds = resources.displayMetrics
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val savedTouches = prefs.getString("recent_touches", null)
        if (recentTouches.isEmpty() && savedTouches != null) runCatching {
            val items = org.json.JSONArray(savedTouches)
            for (index in 0 until items.length()) recentTouches.addLast(items.getString(index))
        }
        val params = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", dp(28))
                .coerceIn(0, (bounds.widthPixels - size).coerceAtLeast(0))
            y = prefs.getInt("y", bounds.heightPixels / 3)
                .coerceIn(0, (bounds.heightPixels - size).coerceAtLeast(0))
        }
        val view = WordSiegeMascotView(this).apply {
            contentDescription = "Obi. Dokununca tepki verir, çift dokununca yazışır; sürükleyerek taşınır."
            isClickable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            updateGame(null, false, 0, 0, 0, null, emptyList(), true, WordSiegeMascotEmotion.HAPPY)
            dragMoved = { delta ->
                val width = resources.displayMetrics.widthPixels
                val height = resources.displayMetrics.heightPixels
                params.x = (params.x + delta.x.roundToInt()).coerceIn(0, (width - size).coerceAtLeast(0))
                params.y = (params.y + delta.y.roundToInt()).coerceIn(0, (height - size).coerceAtLeast(0))
                windows.updateViewLayout(this, params)
            }
            dragFinished = {
                prefs.edit().putInt("x", params.x).putInt("y", params.y).apply()
            }
            setOnClickListener {
                reactToTap()
                val now = android.os.SystemClock.uptimeMillis()
                if (now - lastTapAt < 480L) {
                    lastTapAt = 0L
                    hideBubble()
                    if (panel == null) showPanel() else hidePanel()
                } else {
                    lastTapAt = now
                    lastInteractionAt = System.currentTimeMillis()
                    val reply = if (touchReplies.isEmpty()) ObiReply(mind.touch(), nextMove()) else touchReplies.removeFirst()
                    animate(reply.action)
                    showBubble(reply.text)
                    recentTouches.addLast(reply.text)
                    while (recentTouches.size > 8) recentTouches.removeFirst()
                    prefs.edit().putString("recent_touches", org.json.JSONArray(recentTouches.toList()).toString()).apply()
                    if (touchReplies.size < 3) fetchTouchPack()
                }
            }
        }
        windows.addView(view, params)
        mascot = view
        mascotParams = params
        prefs.edit().putBoolean("running", true).apply()
    }

    private fun fetchTouchPack() {
        val now = System.currentTimeMillis()
        if (touchPackLoading || !gemini.connected() || now - lastTouchFetchAt < 90_000L) return
        touchPackLoading = true
        lastTouchFetchAt = now
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        gemini.touchPack(hour, recentTouches.toList()) { generated ->
            touchPackLoading = false
            generated.forEach(touchReplies::addLast)
        }
    }

    private fun animate(action: WordSiegeMascotAction) {
        actionKey++
        mascot?.updateCompanion(actionKey, action, action == WordSiegeMascotAction.SPACE_FLIGHT, 1f, false, false)
        chatMascot?.updateCompanion(actionKey, action, action == WordSiegeMascotAction.SPACE_FLIGHT, 1f, action == WordSiegeMascotAction.NOD, true)
        if (action == WordSiegeMascotAction.SPACE_FLIGHT) handler.postDelayed({
            mascot?.updateCompanion(actionKey, null, false, 1f, false, false)
            chatMascot?.updateCompanion(actionKey, null, false, 1f, false, true)
        }, mascotActionMillis(action))
    }

    private fun showBubble(message: String) {
        hideBubble()
        if (panel != null) return
        val label = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 45, 73)); cornerRadius = dp(18).toFloat()
            }
            setOnClickListener { showPanel(); hideBubble() }
        }
        val anchor = mascotParams ?: return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = anchor.x.coerceAtMost((resources.displayMetrics.widthPixels - dp(230)).coerceAtLeast(0))
            y = (anchor.y - dp(52)).coerceAtLeast(0)
        }
        runCatching { windows.addView(label, params); bubble = label }
        handler.postDelayed({ if (bubble === label) hideBubble() }, 4_500L)
    }

    private fun hideBubble() {
        bubble?.let { runCatching { windows.removeView(it) } }
        bubble = null
    }

    private fun showPanel() {
        if (!Settings.canDrawOverlays(this)) return
        hideBubble()
        if (panel != null) return
        val width = resources.displayMetrics.widthPixels
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
            background = GradientDrawable().apply {
                setColor(Color.rgb(22, 31, 51)); cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.rgb(92, 179, 168))
            }
            elevation = dp(18).toFloat()
        }
        val menu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        fun header(label: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@ObiOverlayService).apply {
                text = label; textSize = 21f; setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, dp(44), 1f))
            addView(TextView(this@ObiOverlayService).apply {
                text = "×"; textSize = 28f; gravity = Gravity.CENTER
                setTextColor(Color.rgb(190, 205, 224))
                contentDescription = "Obi'yi kapat"
                setOnClickListener { hidePanel() }
            }, LinearLayout.LayoutParams(dp(42), dp(44)))
        }
        menu.addView(header("✦  Obi"))
        val intro = TextView(this).apply {
            text = if (gemini.connected()) "Obi ile yazış, not al veya hatırlatıcı kur."
                else "Gemini ile sohbet için:\n1. Google AI Studio'dan API anahtarı oluştur.\n2. Anahtarı kopyala.\n3. Obi'de “Gemini'yi bağla” düğmesine basıp yapıştır ve kaydet.\nBağlanana kadar not ve alarm yerel çalışır."
            textSize = 15f; setTextColor(Color.rgb(212, 229, 239))
            setPadding(dp(4), dp(12), dp(4), dp(16))
        }
        menu.addView(intro)
        if (!gemini.connected()) menu.addView(pill("✦  Gemini'yi bağla", true).apply {
            setOnClickListener {
                hidePanel()
                mascot?.visibility = View.GONE
                runCatching { startActivity(Intent(this@ObiOverlayService, ObiGeminiSetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { mascot?.visibility = View.VISIBLE }
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(10) })

        val message = TextView(this).apply {
            text = "Buradayım. Bana yaz 💜"
            setTextColor(Color.rgb(228, 245, 240)); textSize = 16f
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = GradientDrawable().apply { setColor(Color.rgb(37, 54, 73)); cornerRadius = dp(22).toFloat() }
            maxLines = 8
        }
        val avatar = WordSiegeMascotView(this).apply {
            updateGame(null, false, 0, 0, 0, null, emptyList(), true, WordSiegeMascotEmotion.HAPPY)
            contentDescription = "Obi, sohbet arkadaşın"
            setOnClickListener { animate(WordSiegeMascotAction.WAVE) }
        }
        val input = EditText(this).apply {
            hint = "Obi'ye yaz…"; maxLines = 2
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(170, 190, 204))
            textSize = 15f; setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 48, 68)); cornerRadius = dp(17).toFloat()
                setStroke(dp(1), Color.rgb(84, 111, 130))
            }
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        fun openChat() {
            menu.visibility = View.GONE
            chat.visibility = View.VISIBLE
            chatMascot = avatar
            mascot?.visibility = View.GONE
            animate(WordSiegeMascotAction.WAVE)
            input.requestFocus()
            handler.postDelayed({
                if (panel === card) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 180L)
        }
        var requestId = 0
        fun submit(command: String) {
            if (command.isBlank()) return
            requestId++
            val currentRequest = requestId
            lastInteractionAt = System.currentTimeMillis()
            if (store.canHandle(command)) {
                message.text = store.handle(command)
                animate(when {
                    command.contains("alarm", true) || command.contains("hatırlat", true) -> WordSiegeMascotAction.CHEER
                    command.contains("not", true) -> WordSiegeMascotAction.NOD
                    else -> WordSiegeMascotAction.WAVE
                })
            } else if (weather.canHandle(command)) {
                message.text = "Hava verisini getiriyorum… ☁"
                animate(WordSiegeMascotAction.LOOK_AROUND)
                weather.ask(command) { result ->
                    if (panel === card && currentRequest == requestId) {
                        message.text = result
                        animate(WordSiegeMascotAction.POINT)
                    }
                }
            } else if (gemini.connected()) {
                message.text = "Bir bakayım… ✨"
                animate(WordSiegeMascotAction.THINK)
                gemini.ask(command) { answer ->
                    if (panel === card && currentRequest == requestId) {
                        message.text = answer.text
                        animate(answer.action)
                    }
                }
            } else if (!translate(command, message)) {
                message.text = store.handle(command)
                animate(nextMove())
            }
        }
        val send = pill("➤", true).apply { setOnClickListener { submit(input.text.toString().trim()); input.text.clear() } }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { submit(input.text.toString().trim()); input.text.clear(); true } else false
        }
        val compose = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        compose.addView(input, LinearLayout.LayoutParams(0, dp(50), 1f))
        compose.addView(send, LinearLayout.LayoutParams(dp(52), dp(50)).apply { leftMargin = dp(8) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(pill("♡  Sohbet", true).apply { setOnClickListener { openChat() } },
            LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(6) })
        buttons.addView(pill("✎  Notlarım", false).apply {
            setOnClickListener { openChat(); submit("notlarımı göster") }
        }, LinearLayout.LayoutParams(0, dp(50), 1f))
        menu.addView(buttons)
        val voicePrefs = getSharedPreferences("obi_local", MODE_PRIVATE)
        menu.addView(pill(if (voicePrefs.getBoolean("voice_alerts", true)) "♪  Sesli uyarılar açık" else "♪  Sesli uyarılar kapalı", false).apply {
            setOnClickListener {
                val enabled = !voicePrefs.getBoolean("voice_alerts", true)
                voicePrefs.edit().putBoolean("voice_alerts", enabled).apply()
                text = if (enabled) "♪  Sesli uyarılar açık" else "♪  Sesli uyarılar kapalı"
            }
        }, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(9) })
        menu.addView(pill("Obi'nin sesini dene", false).apply {
            setOnClickListener {
                runCatching { startForegroundService(Intent(this@ObiOverlayService, ObiVoiceAlertService::class.java)
                    .setAction(ObiVoiceAlertService.ACTION_PREVIEW)) }
                    .onFailure { text = "Ses başlatılamadı" }
            }
        }, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(7) })
        if (!store.canScheduleExact()) menu.addView(pill("Kesin alarm izni", false).apply {
            setOnClickListener {
                hidePanel()
                runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(7) })
        menu.addView(pill("Bildirim izni", false).apply { setOnClickListener {
            hidePanel()
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } }, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(9) })
        chat.addView(header("Obi ile sohbet"))
        chat.addView(message, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        chat.addView(avatar, LinearLayout.LayoutParams(dp(170), dp(170)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        chat.addView(compose, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(4) })
        card.addView(menu)
        card.addView(chat)
        val params = WindowManager.LayoutParams(
            (width - dp(28)).coerceAtMost(dp(390)), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(56)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            windows.addView(card, params)
            panel = card
        } catch (_: SecurityException) {
            // User may revoke the overlay permission while Obi is open.
        }
    }

    private fun pill(label: String, accent: Boolean): TextView = TextView(this).apply {
        text = label; textSize = if (accent) 15f else 12f
        gravity = Gravity.CENTER; setTextColor(if (accent) Color.WHITE else Color.rgb(212, 231, 232))
        background = GradientDrawable().apply {
            setColor(if (accent) Color.rgb(96, 71, 160) else Color.rgb(43, 60, 80))
            cornerRadius = dp(15).toFloat()
        }
    }

    private fun translate(command: String, result: TextView): Boolean {
        val lower = command.lowercase(java.util.Locale("tr", "TR"))
        if (!lower.contains("çevir") && !lower.contains("cevir") && !lower.startsWith("translate ")) return false
        val explicitEnglish = lower.contains("ingilizceye") || lower.contains("english")
        val explicitTurkish = lower.contains("türkçeye") || lower.contains("turkceye")
        val sourceText = command.replace(Regex("(?i)ingilizceye|türkçeye|turkceye|english|translate|çevir|cevir|bunu|şunu|lütfen|please"), "")
            .trim(' ', ':', '-', '“', '”', '"')
        if (sourceText.isBlank()) {
            result.text = "Çevireceğin metni de yaz: ‘Çevir: Hello, how are you?’"
            return true
        }
        result.text = "Dili algılıyorum…"
        if (explicitEnglish) startTranslation(sourceText, TranslateLanguage.TURKISH, TranslateLanguage.ENGLISH, result)
        else if (explicitTurkish) startTranslation(sourceText, TranslateLanguage.ENGLISH, TranslateLanguage.TURKISH, result)
        else {
            val identifier = LanguageIdentification.getClient()
            identifier.identifyLanguage(sourceText)
                .addOnSuccessListener { code ->
                    identifier.close()
                    val english = code == "en" || (code == "und" && sourceText.none { it in "çğıöşüÇĞİÖŞÜ" })
                    startTranslation(sourceText,
                        if (english) TranslateLanguage.ENGLISH else TranslateLanguage.TURKISH,
                        if (english) TranslateLanguage.TURKISH else TranslateLanguage.ENGLISH, result)
                }
                .addOnFailureListener {
                    identifier.close()
                    startTranslation(sourceText, TranslateLanguage.TURKISH, TranslateLanguage.ENGLISH, result)
                }
        }
        return true
    }

    private fun startTranslation(text: String, source: String, target: String, result: TextView) {
        val translator = Translation.getClient(TranslatorOptions.Builder()
            .setSourceLanguage(source).setTargetLanguage(target).build())
        result.text = "Çeviri hazırlanıyor; ilk kullanımda dil modeli indirilebilir (mobil veri kullanabilir)…"
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated -> result.text = translated; translator.close() }
                    .addOnFailureListener { result.text = "Çeviri başarısız oldu. Tekrar dene."; translator.close() }
            }
            .addOnFailureListener { result.text = "Dil modeli indirilemedi. İnternet bağlantını kontrol et."; translator.close() }
    }

    private fun hidePanel() {
        panel?.let { runCatching { windows.removeView(it) } }
        panel = null
        chatMascot = null
        mascot?.visibility = View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val view = mascot ?: return
        val params = mascotParams ?: return
        params.x = params.x.coerceIn(0, (resources.displayMetrics.widthPixels - dp(SIZE_DP)).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (resources.displayMetrics.heightPixels - dp(SIZE_DP)).coerceAtLeast(0))
        runCatching { windows.updateViewLayout(view, params) }
        hidePanel()
    }

    override fun onDestroy() {
        unregisterReceiver(celebration)
        handler.removeCallbacksAndMessages(null)
        hideBubble()
        hidePanel()
        mascot?.let { runCatching { windows.removeView(it) } }
        mascot = null
        mascotParams = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
