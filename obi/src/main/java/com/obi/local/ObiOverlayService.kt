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
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.view.inputmethod.EditorInfo
import android.text.InputType
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
    private var bubble: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastTapAt = 0L
    private var lastInteractionAt = System.currentTimeMillis()
    private var actionKey = 0L
    private val mind by lazy { ObiMind(this) }
    private val gemini by lazy { ObiGemini(this) }
    private val moves = ArrayDeque<WordSiegeMascotAction>()
    private val celebration = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(celebration, IntentFilter("com.obi.local.CELEBRATE"), Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(celebration, IntentFilter("com.obi.local.CELEBRATE"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
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
            }
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
                    animate(nextMove())
                    showBubble(mind.touch())
                }
            }
        }
        windows.addView(view, params)
        mascot = view
        mascotParams = params
        prefs.edit().putBoolean("running", true).apply()
    }

    private fun animate(action: WordSiegeMascotAction) {
        mascot?.updateCompanion(++actionKey, action, action == WordSiegeMascotAction.SPACE_FLIGHT, 1f, false, false)
        if (action == WordSiegeMascotAction.SPACE_FLIGHT) handler.postDelayed({
            mascot?.updateCompanion(actionKey, null, false, 1f, false, false)
        }, mascotActionMillis(action))
    }

    private fun showBubble(message: String) {
        hideBubble()
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
        val width = resources.displayMetrics.widthPixels
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.rgb(22, 31, 51)); cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.rgb(92, 179, 168))
            }
            elevation = dp(18).toFloat()
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply {
            text = "✦  Obi"; setTextColor(Color.WHITE); textSize = 21f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val close = TextView(this).apply {
            text = "×"; textSize = 26f; setTextColor(Color.rgb(190, 205, 224))
            gravity = Gravity.CENTER
            setOnClickListener { hidePanel() }
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(42), dp(42)))
        val message = TextView(this).apply {
            text = mind.touch()
            setTextColor(Color.rgb(222, 242, 239)); textSize = 15f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply { setColor(Color.rgb(37, 54, 73)); cornerRadius = dp(18).toFloat() }
            maxLines = 7
        }
        val input = EditText(this).apply {
            hint = "Obi'ye bir şey yaz…"; maxLines = 3
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(170, 190, 204))
            textSize = 15f; setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 48, 68)); cornerRadius = dp(17).toFloat()
                setStroke(dp(1), Color.rgb(84, 111, 130))
            }
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        val send = pill("➤", true).apply {
            setOnClickListener {
                val command = input.text.toString().trim()
                if (command.isNotEmpty()) {
                    if (!translate(command, message)) {
                        if (store.canHandle(command) || !gemini.connected()) message.text = store.handle(command)
                        else {
                            message.text = "Bir düşüneyim… ✨"
                            gemini.ask(command) { answer ->
                                if (panel === card) message.text = answer
                                showBubble(answer.take(85))
                            }
                        }
                    }
                    input.text.clear()
                    animate(nextMove())
                }
            }
        }
        val compose = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        compose.addView(input, LinearLayout.LayoutParams(0, dp(50), 1f))
        compose.addView(send, LinearLayout.LayoutParams(dp(52), dp(50)).apply { leftMargin = dp(8) })
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun quick(label: String, command: String) {
            val item = pill(label, false)
            item.setOnClickListener { message.text = store.handle(command); animate(nextMove()) }
            shortcuts.addView(item, LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(5) })
        }
        quick("☀  Motive et", "motive et")
        quick("✎  Notlarım", "notlarımı göster")
        quick("♡  Sohbet", "merhaba")
        val settings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val settingsToggle = TextView(this).apply {
            text = "⚙  Ayarlar ve Gemini  ▾"; textSize = 13f
            setTextColor(Color.rgb(156, 212, 202)); setPadding(dp(4), dp(13), dp(4), 0)
            setOnClickListener { settings.visibility = if (settings.visibility == View.GONE) View.VISIBLE else View.GONE }
        }
        val connect = pill(if (gemini.connected()) "✓  Gemini bağlı · değiştir" else "✦  Gemini bağla", true)
        val setup = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val keyInput = EditText(this).apply {
            hint = "Gemini API anahtarını yapıştır"; setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(178, 188, 203)); textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        val keyHelp = TextView(this).apply {
            text = "İlk kez: Google AI Studio'dan ücretsiz katman anahtarı oluştur, buraya yapıştır. Sonrasında Obi kendisi bağlanır. Anahtar telefonda şifreli saklanır."
            textSize = 12f; setTextColor(Color.rgb(193, 209, 220)); setPadding(dp(5), dp(8), dp(5), dp(8))
        }
        val save = pill("Bağlantıyı kaydet", true).apply { setOnClickListener {
            if (gemini.saveKey(keyInput.text.toString())) {
                keyInput.text.clear(); setup.visibility = View.GONE
                connect.text = "✓  Gemini bağlı · değiştir"
                message.text = "Anahtar kaydedildi. Gemini'ye bir şey soralım 💜"
            } else message.text = "Anahtar kaydedilemedi. API anahtarını kontrol et."
        } }
        val studio = pill("API anahtarı al ↗", false).apply { setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://aistudio.google.com/api-keys"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } }
        connect.setOnClickListener { setup.visibility = if (setup.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        setup.addView(keyHelp)
        setup.addView(keyInput, LinearLayout.LayoutParams(-1, dp(48)))
        setup.addView(studio, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(6) })
        setup.addView(save, LinearLayout.LayoutParams(-1, dp(43)).apply { topMargin = dp(6) })
        settings.addView(pill("Bildirim izni", false).apply { setOnClickListener {
            hidePanel()
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(7) })
        card.addView(header)
        card.addView(message, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        card.addView(compose, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(14) })
        card.addView(shortcuts, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(10) })
        card.addView(connect, LinearLayout.LayoutParams(-1, dp(43)).apply { topMargin = dp(10) })
        card.addView(setup)
        card.addView(settingsToggle)
        card.addView(settings)
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
