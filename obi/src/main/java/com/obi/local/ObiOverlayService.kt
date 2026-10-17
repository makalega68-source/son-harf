package com.obi.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

/** A ScrollView that never grows past [maxPx], so the floating card always fits on screen. */
internal class CappedScroll(context: Context, private val maxPx: Int) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(maxPx, View.MeasureSpec.AT_MOST))
    }
}

internal sealed interface ChatGame
internal class GuessGame(val answer: String, var tries: Int = 0) : ChatGame
internal class RiddleGame(val riddle: ObiLife.Riddle, var misses: Int = 0) : ChatGame
internal class QuizGame(val seed: Long, var index: Int = 0, var score: Int = 0) : ChatGame

/** User-started, user-stoppable overlay. No accessibility or screen-reading permission is used. */
class ObiOverlayService : Service() {
    companion object {
        const val ACTION_START = "com.obi.local.START_OVERLAY"
        const val ACTION_STOP = "com.obi.local.STOP_OVERLAY"
        const val ACTION_SHOW = "com.obi.local.SHOW_OVERLAY"
        const val ACTION_OPEN_CHAT = "com.obi.local.OPEN_CHAT"
        const val ACTION_HIDE = "com.obi.local.HIDE_OVERLAY"
        const val ACTION_MIC_READY = "com.obi.local.MIC_READY"
        const val ACTION_LISTEN = "com.obi.local.LISTEN"
        const val ACTION_READY = "com.obi.local.OVERLAY_READY"
        const val ACTION_ERROR = "com.obi.local.OVERLAY_ERROR"
        private const val CHANNEL = "obi_companion"
        private const val NOTIFICATION_ID = 1201
        private const val PREFS = "obi_overlay"
        private const val DEFAULT_SIZE_DP = 92
        private const val FOCUS_SIZE_DP = 70
        private const val KELIME_TAHTI = "com.sonharf.game"

        fun isRunning(context: Context): Boolean =
            context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("running", false)

        // Palette shared by the card, bubbles and chips.
        private val INK = Color.rgb(16, 22, 38)
        private val CARD_TOP = Color.rgb(27, 37, 62)
        private val CARD_BOTTOM = Color.rgb(36, 30, 70)
        private val CHIP = Color.rgb(41, 56, 82)
        private val ACCENT = Color.rgb(108, 78, 196)
        private val MINT = Color.rgb(143, 236, 220)
        private val SOFT = Color.rgb(212, 229, 239)
        private val GOLD = Color.rgb(255, 224, 138)
    }

    private val windows by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val handler = Handler(Looper.getMainLooper())
    private val mind by lazy { ObiMind(this) }
    private val gemini by lazy { ObiGemini(this) }
    private val store by lazy { ObiStore(this) }
    private val weather by lazy { ObiWeather() }
    private val localPrefs by lazy { getSharedPreferences("obi_local", MODE_PRIVATE) }

    private var mascot: WordSiegeMascotView? = null
    private var mascotParams: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private var chatMascot: WordSiegeMascotView? = null
    private var bubble: View? = null
    private var timerLabel: TextView? = null
    private var hat = WordSiegeMascotHat.NONE

    private var input: EditText? = null
    private var askMoodNext = false

    // Hiding: by the user (drag to "Gizle" or the notification) and automatically in full screen.
    private var userHidden = false
    private var fullscreenHidden = false
    private var probe: View? = null
    private var hideTarget: TextView? = null
    private var overHideTarget = false
    private var requestId = 0
    private var game: ChatGame? = null

    private var lastTapAt = 0L
    private var lastInteractionAt = System.currentTimeMillis()
    private var actionKey = 0L
    private val moves = ArrayDeque<WordSiegeMascotAction>()
    private val touchReplies = ArrayDeque<ObiReply>()
    private val recentTouches = ArrayDeque<String>()
    private var touchPackLoading = false
    private var lastTouchFetchAt = 0L

    // Phone moments.
    private var batteryLow = false
    private var lastPowerCheer = 0L
    private var screenOnAt = SystemClock.elapsedRealtime()
    private var lastBreakNudge = 0L
    private var lastGreetAt = 0L

    // "Obi'yi yakala".
    private var catchUntil = 0L
    private var catchScore = 0

    private val events = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SHOW -> { showMascotIfAllowed(); fetchTouchPack() }
                ObiStore.ACTION_LOOK_CHANGED -> applyLook()
                ObiVoiceAlertService.ACTION_STATUS -> {
                    val text = intent.getStringExtra("text") ?: return
                    if (panel != null) say(text) else showBubble(text)
                }
                ObiStore.ACTION_ASK_MOOD -> {
                    askMoodNext = true
                    animate(WordSiegeMascotAction.WAVE)
                    showBubble("Bugün nasıldın? Bana dokun, anlat 💜")
                }
                ObiStore.ACTION_NUDGE -> {
                    animate(runCatching { WordSiegeMascotAction.valueOf(intent.getStringExtra("action") ?: "BOUNCE") }.getOrDefault(WordSiegeMascotAction.BOUNCE))
                    showBubble(intent.getStringExtra("text") ?: "Buradayım!")
                }
                else -> {
                    animate(WordSiegeMascotAction.CHEER)
                    val text = intent.getStringExtra("text") ?: "Yaşasın! ✨"
                    if (panel != null) say(text) else showBubble(text)
                    if (store.focusEnd() == 0L) endFocusView()
                }
            }
        }
    }

    private val phone = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = SystemClock.elapsedRealtime()
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                    val percent = if (level < 0) 100 else level * 100 / scale
                    if (plugged || percent > 20) batteryLow = false
                    else if (percent <= 15 && !batteryLow) {
                        batteryLow = true
                        animate(WordSiegeMascotAction.YAWN)
                        showBubble("Pilin %$percent kaldı… Ben de yoruldum 😴 Bizi şarja takar mısın?")
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> if (now - lastPowerCheer > 10 * 60_000L) {
                    lastPowerCheer = now
                    animate(WordSiegeMascotAction.CHEER)
                    showBubble(listOf("Şarj oluyorum ⚡ Teşekkürler!", "Mmm, enerji geliyor ⚡", "Yaşasın, pil doluyor! ⚡").random())
                }
                Intent.ACTION_SCREEN_OFF -> screenOnAt = 0L
                Intent.ACTION_SCREEN_ON -> screenOnAt = now
                Intent.ACTION_USER_PRESENT -> {
                    screenOnAt = now
                    greetOnUnlock(now)
                }
            }
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
            if (mascot != null && panel == null && catchUntil == 0L) {
                val calendar = java.util.Calendar.getInstance()
                val minuteOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
                val focusing = store.focusEnd() > 0L
                val action = when {
                    ObiLife.isNight(minuteOfDay, store.bedtime()) -> WordSiegeMascotAction.DOZE
                    focusing -> listOf(WordSiegeMascotAction.THINK, WordSiegeMascotAction.NOD).random()
                    System.currentTimeMillis() - lastInteractionAt > 30 * 60_000L -> WordSiegeMascotAction.YAWN
                    else -> listOf(WordSiegeMascotAction.LOOK_AROUND, WordSiegeMascotAction.SWAY, WordSiegeMascotAction.WAVE).random()
                }
                animate(action)
                maybeSuggestBreak(focusing)
            }
            handler.postDelayed(this, 30_000L)
        }
    }

    private val focusTicker = object : Runnable {
        override fun run() {
            val end = store.focusEnd()
            val label = timerLabel
            if (end == 0L || label == null) { endFocusView(); return }
            val left = ((end - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            label.text = String.format(java.util.Locale.ROOT, "⏱ %02d:%02d", left / 60, left % 60)
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val own = IntentFilter().apply {
            addAction(ObiStore.ACTION_CELEBRATE); addAction(ACTION_SHOW); addAction(ObiStore.ACTION_NUDGE)
            addAction(ObiStore.ACTION_ASK_MOOD); addAction(ObiStore.ACTION_LOOK_CHANGED); addAction(ObiVoiceAlertService.ACTION_STATUS)
        }
        val system = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED); addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(events, own, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(phone, system, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(events, own)
            registerReceiver(phone, system)
        }
        store.scheduleDaily()
        if (gemini.connected()) ObiGeminiVoice(this).prepareFixed()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SHOW && mascot != null) {
            setUserHidden(false)
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_MIC_READY) {
            if (mascot != null) {
                runCatching { startForegroundNotification() }
                if (intent.getBooleanExtra("granted", false)) {
                    if (wakeWanted()) listener.startWakeMode() else showBubble("Mikrofon hazır! Beni basılı tutup konuşabilirsin 🎤")
                } else showBubble("Mikrofon izni olmadan seni duyamam. İstersen yazarak devam edelim.")
                return START_NOT_STICKY
            }
        }
        if (intent?.action == ACTION_LISTEN && mascot != null) {
            startPushToTalk()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_HIDE && mascot != null) {
            setUserHidden(true)
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            notifyEntry(ACTION_ERROR)
            if (intent?.action == ACTION_OPEN_CHAT) runCatching {
                startActivity(Intent(this, ObiEntryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startForegroundNotification()
            if (mascot == null) {
                addMascot()
                handler.postDelayed(ambient, 8_000L)
                handler.postDelayed({ fetchTouchPack() }, 1_200L)
                handler.postDelayed({ greetToday() }, 2_000L)
                if (wakeWanted() && listener.hasPermission()) handler.postDelayed({ listener.startWakeMode() }, 3_000L)
                if (store.focusEnd() > 0L) startFocusView()
            }
            // Opening Obi from its icon, widget or notification always brings it back.
            setUserHidden(false)
            if (intent?.action == ACTION_OPEN_CHAT) handler.post { showPanel(openChat = true) }
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
        val toggle = PendingIntent.getService(
            this, 3, Intent(this, ObiOverlayService::class.java).setAction(if (userHidden) ACTION_SHOW else ACTION_HIDE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_obi_notification)
            .setContentTitle(if (userHidden) "Obi saklanıyor" else "Obi ekranında")
            .setContentText(if (userHidden) "Göstermek için ‘Obi'yi göster’e dokun. Alarmlar yine çalışır."
                else "Dokun: söz • Çift dokun: sohbet • Aşağıdaki ‘Gizle’ye sürükle: sakla")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, if (userHidden) "Obi'yi göster" else "Gizle", toggle).build())
            .addAction(Notification.Action.Builder(null, "🎤 Konuş", PendingIntent.getService(
                this, 4, Intent(this, ObiOverlayService::class.java).setAction(ACTION_LISTEN),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build())
            .addAction(Notification.Action.Builder(null, "Obi'yi kapat", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            val special = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            val mic = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            // The microphone type is only allowed while Obi was opened from the foreground; fall back without it.
            val withMic = listener.hasPermission()
            try {
                startForeground(NOTIFICATION_ID, notification, if (withMic) special or mic else special)
                micForeground = withMic
            } catch (_: RuntimeException) {
                startForeground(NOTIFICATION_ID, notification, special)
                micForeground = false
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
            micForeground = true
        }
    }

    // ---- Voice commands ---------------------------------------------------------------------------

    private var micForeground = false
    private var voicePending = false

    private val listener by lazy {
        ObiListener(this,
            onCommand = { command -> handler.post { handleVoice(command) } },
            onState = { state -> handler.post { onListenState(state) } })
    }

    private fun wakeWanted(): Boolean = localPrefs.getBoolean("wake_mode", false)

    private fun onListenState(state: String) {
        when (state) {
            "listening" -> { animate(WordSiegeMascotAction.PEEK); if (panel == null) showBubble("Dinliyorum… 🎤") else thinking("Dinliyorum… 🎤") }
            "idle" -> if (panel == null) hideBubble()
            else -> if (panel == null) showBubble(state) else say(state)
        }
    }

    /** Long-press on Obi, the notification's 🎤 or "Obi" in wake mode. */
    private fun startPushToTalk() {
        if (!listener.hasPermission()) { askMicPermission(); return }
        if (!listener.available()) { showBubble("Bu telefonda konuşma tanıma bulunamadı. Google uygulamasını güncellemek gerekebilir."); return }
        lastInteractionAt = System.currentTimeMillis()
        listener.listenOnce()
    }

    private fun askMicPermission() {
        showBubble("Seni duyabilmem için mikrofon izni gerekiyor 🎤")
        runCatching { startActivity(Intent(this, ObiMicPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun handleVoice(command: String) {
        if (command.isBlank()) {
            animate(WordSiegeMascotAction.WAVE)
            if (panel == null) showBubble("Efendim? 🎤") else say("Efendim? 🎤")
            playClip("listening")
            return
        }
        voicePending = true
        submit(command)
    }

    /** Plays one of Obi's ready-made Gemini lines through the overlay itself (no extra service). */
    private fun playClip(key: String) {
        if (!localPrefs.getBoolean("voice_alerts", true)) return
        val voice = ObiGeminiVoice(this)
        if (voice.has(key)) voice.play(key, ObiVoiceAlertService.pitch(this)) {}
    }

    private fun setWakeMode(on: Boolean): String {
        if (on && !listener.hasPermission()) { localPrefs.edit().putBoolean("wake_mode", true).apply(); askMicPermission(); return "Mikrofon iznini verince seni dinlemeye başlayacağım 🎤" }
        localPrefs.edit().putBoolean("wake_mode", on).apply()
        return if (on) {
            if (!micForeground) runCatching { startForegroundNotification() }
            listener.startWakeMode()
            "Tamam! ‘Obi’ diye başlayan cümleleri dinliyorum: ‘Obi, sabah 11'e alarm kur’. 🎤\nMikrofon açık kaldığı için telefonda mikrofon işareti görünür ve pil biraz daha çabuk biter. Kapatmak için ‘dinlemeyi kapat’."
        } else {
            listener.stopWakeMode()
            "Dinlemeyi kapattım. Konuşmak istersen beni basılı tut 🎤"
        }
    }

    // ---- The floating mascot -------------------------------------------------------------------

    private fun addMascot() {
        val size = dp(sizeDp())
        val bounds = resources.displayMetrics
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val savedTouches = prefs.getString("recent_touches", null)
        if (recentTouches.isEmpty() && savedTouches != null) runCatching {
            val items = JSONArray(savedTouches)
            for (index in 0 until items.length()) recentTouches.addLast(items.getString(index))
        }
        val params = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", dp(28)).coerceIn(0, (bounds.widthPixels - size).coerceAtLeast(0))
            y = prefs.getInt("y", bounds.heightPixels / 3).coerceIn(0, (bounds.heightPixels - size).coerceAtLeast(0))
        }
        val view = WordSiegeMascotView(this).apply {
            contentDescription = "Obi. Dokununca tepki verir, çift dokununca menüyü açar; sürükleyerek taşınır."
            isClickable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            updateGame(null, false, 0, 0, 0, null, emptyList(), true, WordSiegeMascotEmotion.HAPPY)
            dragStarted = { showHideTarget() }
            dragMoved = { delta ->
                val width = resources.displayMetrics.widthPixels
                val height = resources.displayMetrics.heightPixels
                params.x = (params.x + delta.x.roundToInt()).coerceIn(0, (width - params.width).coerceAtLeast(0))
                params.y = (params.y + delta.y.roundToInt()).coerceIn(0, (height - params.height).coerceAtLeast(0))
                runCatching { windows.updateViewLayout(this, params) }
                placeTimer()
                if (hideTarget == null) showHideTarget()
                updateHideTarget(params)
            }
            dragFinished = {
                val hide = overHideTarget && catchUntil == 0L
                removeHideTarget()
                overHideTarget = false
                if (hide) {
                    // Put Obi back where it was, so it returns there when shown again.
                    params.x = prefs.getInt("x", dp(28)); params.y = prefs.getInt("y", resources.displayMetrics.heightPixels / 3)
                    runCatching { windows.updateViewLayout(this, params) }
                    placeTimer()
                    setUserHidden(true)
                } else if (catchUntil == 0L) prefs.edit().putInt("x", params.x).putInt("y", params.y).apply()
            }
            setOnClickListener { onMascotTap(prefs) }
            setOnLongClickListener { if (catchUntil == 0L) startPushToTalk(); true }
        }
        windows.addView(view, params)
        mascot = view
        mascotParams = params
        userHidden = prefs.getBoolean("user_hidden", false)
        addFullscreenProbe()
        showMascotIfAllowed()
        applyLook()
        prefs.edit().putBoolean("running", true).apply()
    }

    /** Floating Obi size, changeable from the chat with "küçül" / "büyü". */
    private fun sizeDp(): Int = localPrefs.getInt("mascot_size_dp", DEFAULT_SIZE_DP).coerceIn(64, 140)

    private fun resizeMascot(stepDp: Int): Int {
        val next = (sizeDp() + stepDp).coerceIn(64, 140)
        localPrefs.edit().putInt("mascot_size_dp", next).apply()
        val params = mascotParams
        val view = mascot
        if (params != null && view != null && store.focusEnd() == 0L) {
            params.width = dp(next); params.height = dp(next)
            runCatching { windows.updateViewLayout(view, params) }
        }
        return next
    }

    private fun showMascotIfAllowed() {
        val view = mascot ?: return
        val visible = catchUntil > 0L || (!userHidden && !fullscreenHidden && panel == null)
        view.visibility = if (visible) View.VISIBLE else View.GONE
        timerLabel?.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) hideBubble()
    }

    private fun setUserHidden(hidden: Boolean) {
        if (userHidden == hidden) { showMascotIfAllowed(); return }
        userHidden = hidden
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("user_hidden", hidden).apply()
        showMascotIfAllowed()
        runCatching { startForegroundNotification() }
    }

    private fun autoHideEnabled(): Boolean = localPrefs.getBoolean("fullscreen_hide", true)

    private fun onFullscreen(full: Boolean) {
        val hide = full && autoHideEnabled()
        if (hide == fullscreenHidden) return
        fullscreenHidden = hide
        showMascotIfAllowed()
    }

    /** A 1-pixel, untouchable window that only listens for other apps going full screen (games, video). */
    private fun addFullscreenProbe() {
        val view = View(this)
        val params = WindowManager.LayoutParams(
            1, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSPARENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        if (Build.VERSION.SDK_INT >= 30) {
            view.setOnApplyWindowInsetsListener { _, insets ->
                onFullscreen(!insets.isVisible(android.view.WindowInsets.Type.statusBars()))
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            view.setOnSystemUiVisibilityChangeListener { flags -> onFullscreen(flags and View.SYSTEM_UI_FLAG_FULLSCREEN != 0) }
        }
        runCatching { windows.addView(view, params); probe = view }
    }

    private fun showHideTarget() {
        if (hideTarget != null || catchUntil > 0L) return
        val target = TextView(this).apply {
            text = "✕  Gizle"; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(22), dp(12), dp(22), dp(12))
            background = GradientDrawable().apply { setColor(Color.argb(200, 30, 36, 56)); cornerRadius = dp(26).toFloat(); setStroke(dp(2), Color.argb(160, 255, 255, 255)) }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(56) }
        runCatching { windows.addView(target, params); hideTarget = target }
        overHideTarget = false
    }

    private fun updateHideTarget(params: WindowManager.LayoutParams) {
        val target = hideTarget ?: return
        val metrics = resources.displayMetrics
        val centerX = params.x + params.width / 2
        val centerY = params.y + params.height / 2
        val over = centerY > metrics.heightPixels - dp(190) && kotlin.math.abs(centerX - metrics.widthPixels / 2) < dp(120)
        if (over == overHideTarget) return
        overHideTarget = over
        (target.background as? GradientDrawable)?.setColor(if (over) Color.argb(230, 214, 72, 96) else Color.argb(200, 30, 36, 56))
        target.scaleX = if (over) 1.15f else 1f; target.scaleY = target.scaleX
    }

    private fun removeHideTarget() {
        hideTarget?.let { runCatching { windows.removeView(it) } }
        hideTarget = null
    }

    private fun onMascotTap(prefs: android.content.SharedPreferences) {
        mascot?.reactToTap()
        if (catchUntil > 0L) {
            catchScore++
            animate(WordSiegeMascotAction.FLINCH)
            hopToRandomSpot()
            return
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastTapAt < 480L) {
            lastTapAt = 0L
            hideBubble()
            if (panel == null) showPanel(openChat = false) else hidePanel()
            return
        }
        lastTapAt = now
        lastInteractionAt = System.currentTimeMillis()
        val reply = if (touchReplies.isEmpty()) ObiReply(mind.touch(), nextMove()) else touchReplies.removeFirst()
        animate(reply.action)
        showBubble(reply.text)
        recentTouches.addLast(reply.text)
        while (recentTouches.size > 8) recentTouches.removeFirst()
        prefs.edit().putString("recent_touches", JSONArray(recentTouches.toList()).toString()).apply()
        if (touchReplies.size < 3) fetchTouchPack()
        reward(1)
    }

    private fun applyLook() {
        val skin = store.skin()
        hat = store.hat()
        mascot?.setSkin(skin)
        chatMascot?.setSkin(skin)
        mascot?.updateCompanion(actionKey, null, false, 1f, false, false, hat = hat)
        chatMascot?.updateCompanion(actionKey, null, false, 1f, false, true, hat = hat)
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
        mascot?.updateCompanion(actionKey, action, action == WordSiegeMascotAction.SPACE_FLIGHT, 1f, false, false, hat = hat)
        chatMascot?.updateCompanion(actionKey, action, action == WordSiegeMascotAction.SPACE_FLIGHT, 1f, action == WordSiegeMascotAction.NOD, true, hat = hat)
        if (action == WordSiegeMascotAction.SPACE_FLIGHT) handler.postDelayed({
            mascot?.updateCompanion(actionKey, null, false, 1f, false, false, hat = hat)
            chatMascot?.updateCompanion(actionKey, null, false, 1f, false, true, hat = hat)
        }, mascotActionMillis(action))
    }

    /** Bond points for everyday moments; a new skin or hat is announced with a sparkle. */
    private fun reward(points: Int) {
        val unlock = store.addBond(points) ?: return
        handler.postDelayed({
            animate(WordSiegeMascotAction.SPARKLE)
            showBubble("Yeni bir şey açıldı: ${unlock.name}! 🎁 Menüde ‘Dolap’tan giyebilirim.")
        }, 1_600L)
    }

    private fun showBubble(message: String) {
        hideBubble()
        if (panel != null || mascot?.visibility != View.VISIBLE) return
        val anchor = mascotParams ?: return
        val width = resources.displayMetrics.widthPixels
        val label = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.WHITE)
            maxWidth = dp(250)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(CARD_TOP, CARD_BOTTOM)).apply {
                cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.argb(120, 143, 236, 220))
            }
            elevation = dp(6).toFloat()
            setOnClickListener { hideBubble(); showPanel(openChat = false) }
        }
        val below = anchor.y < dp(90)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = anchor.x.coerceAtMost((width - dp(262)).coerceAtLeast(0))
            y = if (below) anchor.y + anchor.height + dp(4) else (anchor.y - dp(64)).coerceAtLeast(0)
        }
        runCatching { windows.addView(label, params); bubble = label }
        handler.postDelayed({ if (bubble === label) hideBubble() }, 5_500L)
    }

    private fun hideBubble() {
        bubble?.let { runCatching { windows.removeView(it) } }
        bubble = null
    }

    // ---- Moments of the day ---------------------------------------------------------------------

    private fun greetToday() {
        val today = LocalDate.now().toString()
        if (localPrefs.getString("greeted_day", "") == today) return
        val line = when {
            store.isBirthdayToday() -> "İyi ki doğdun! Bugün taç bende, kutlama sende 👑🎂"
            else -> ObiLife.celebration(LocalDate.now())?.message
        } ?: return
        localPrefs.edit().putString("greeted_day", today).apply()
        animate(WordSiegeMascotAction.DANCE)
        showBubble(line)
    }

    private fun greetOnUnlock(now: Long) {
        if (now - lastGreetAt < 20 * 60_000L || Random.nextFloat() > .5f || panel != null) return
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        if (ObiLife.isNight(hour * 60 + calendar.get(java.util.Calendar.MINUTE), store.bedtime())) return
        lastGreetAt = now
        animate(WordSiegeMascotAction.WAVE)
        showBubble(when (hour) {
            in 5..10 -> listOf("Günaydın! Seni gördüğüme sevindim ☀️", "Hoş geldin! Bugün harika olacak.").random()
            in 11..17 -> listOf("Tekrar merhaba! 👋", "Seni bekliyordum!", "Hey, buradayım!").random()
            else -> listOf("İyi akşamlar! Günün nasıl geçti?", "Akşam molası mı? 🌆").random()
        })
        greetToday()
    }

    private fun maybeSuggestBreak(focusing: Boolean) {
        if (focusing || !store.breakEnabled() || screenOnAt == 0L) return
        val now = SystemClock.elapsedRealtime()
        if (now - screenOnAt < 45 * 60_000L || now - lastBreakNudge < 45 * 60_000L) return
        lastBreakNudge = now
        animate(WordSiegeMascotAction.STRETCH)
        showBubble(listOf(
            "45 dakikadır ekrandayız. Gözlerini uzağa dikip 20 saniye dinlendirelim mi? 👀",
            "Mola vakti! Kalkıp biraz esneyelim 🧘",
            "Bir bardak su ve iki dakika yürüyüş? Ben de geliyorum! 💧",
        ).random())
    }

    // ---- Focus (pomodoro) -------------------------------------------------------------------------

    private fun startFocus(minutes: Int): String {
        store.startFocus(minutes)
        startFocusView()
        animate(WordSiegeMascotAction.THINK)
        return "Odak modu başladı: $minutes dakika ⏱ Küçülüp köşede bekleyeceğim. Bitince haber veririm. (Bitirmek için: ‘odağı bitir’)"
    }

    private fun startFocusView() {
        val params = mascotParams ?: return
        val view = mascot ?: return
        params.width = dp(FOCUS_SIZE_DP); params.height = dp(FOCUS_SIZE_DP)
        runCatching { windows.updateViewLayout(view, params) }
        if (timerLabel == null) {
            val label = TextView(this).apply {
                textSize = 13f; setTextColor(INK); typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(10), dp(3), dp(10), dp(3))
                background = GradientDrawable().apply { setColor(MINT); cornerRadius = dp(12).toFloat() }
            }
            val labelParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.START }
            runCatching { windows.addView(label, labelParams); timerLabel = label }
        }
        placeTimer()
        handler.removeCallbacks(focusTicker)
        handler.post(focusTicker)
    }

    private fun placeTimer() {
        val label = timerLabel ?: return
        val anchor = mascotParams ?: return
        val params = label.layoutParams as? WindowManager.LayoutParams ?: return
        params.x = anchor.x + dp(4)
        params.y = anchor.y + anchor.height
        runCatching { windows.updateViewLayout(label, params) }
    }

    private fun endFocusView() {
        handler.removeCallbacks(focusTicker)
        timerLabel?.let { runCatching { windows.removeView(it) } }
        timerLabel = null
        val params = mascotParams ?: return
        val view = mascot ?: return
        if (params.width != dp(sizeDp())) {
            params.width = dp(sizeDp()); params.height = dp(sizeDp())
            runCatching { windows.updateViewLayout(view, params) }
        }
    }

    // ---- "Obi'yi yakala" ----------------------------------------------------------------------------

    private fun startCatchGame() {
        hidePanel()
        catchScore = 0
        catchUntil = SystemClock.uptimeMillis() + 15_000L
        showMascotIfAllowed()
        showBubble("Beni yakala! 15 saniye, ne kadar çok dokunursan o kadar puan ⚡")
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (catchUntil == 0L) return
                if (SystemClock.uptimeMillis() >= catchUntil) { finishCatchGame(); return }
                hopToRandomSpot()
                handler.postDelayed(this, 850L)
            }
        }, 1_200L)
    }

    private fun hopToRandomSpot() {
        val params = mascotParams ?: return
        val view = mascot ?: return
        val metrics = resources.displayMetrics
        params.x = Random.nextInt(0, (metrics.widthPixels - params.width).coerceAtLeast(1))
        params.y = Random.nextInt(dp(60), (metrics.heightPixels - params.height - dp(60)).coerceAtLeast(dp(61)))
        runCatching { windows.updateViewLayout(view, params) }
        animate(WordSiegeMascotAction.HOP)
    }

    private fun finishCatchGame() {
        catchUntil = 0L
        showMascotIfAllowed()
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val best = maxOf(prefs.getInt("catch_best", 0), catchScore)
        prefs.edit().putInt("catch_best", best).apply()
        run {
            val params = mascotParams ?: return@run
            params.x = prefs.getInt("x", params.x); params.y = prefs.getInt("y", params.y)
            mascot?.let { runCatching { windows.updateViewLayout(it, params) } }
        }
        animate(if (catchScore >= best && catchScore > 0) WordSiegeMascotAction.CHEER else WordSiegeMascotAction.GIGGLE)
        showBubble("Süre bitti! $catchScore kez yakaladın. Rekorun: $best ${if (catchScore >= best && catchScore > 0) "🏆" else ""}")
        if (catchScore > 0) reward(3)
    }

    // ---- The chat: a calm full-screen sheet with Obi, a speech balloon and the keyboard ---------------

    private enum class Pending { NONE, CITY, LIST_ITEMS, MOOD, GAME_PICK }

    private var pending = Pending.NONE
    private var replyText: TextView? = null
    private var youText: TextView? = null
    private var replyScroll: ScrollView? = null
    private var lastUser = ""
    private var lastReply = ""

    private fun showPanel(@Suppress("UNUSED_PARAMETER") openChat: Boolean = true, askFirst: Pending = Pending.NONE) {
        val ask0 = askFirst
        if (!Settings.canDrawOverlays(this)) return
        hideBubble()
        val ask = if (askMoodNext && ask0 == Pending.NONE) Pending.MOOD.also { askMoodNext = false } else ask0
        if (panel != null) { if (ask != Pending.NONE) startAsk(ask); return }
        val metrics = resources.displayMetrics
        val root = android.widget.FrameLayout(this).apply {
            setBackgroundColor(Color.argb(185, 8, 12, 24))
            setOnClickListener { hidePanel() }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
            isClickable = true
        }
        root.addView(content, android.widget.FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        root.addView(label("×", 30f, SOFT).apply {
            gravity = Gravity.CENTER; contentDescription = "Kapat"; setOnClickListener { hidePanel() }
        }, android.widget.FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP or Gravity.END).apply { topMargin = dp(36); rightMargin = dp(8) })

        // Speech balloon above Obi's head: the user's last words small, Obi's answer large.
        val balloon = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            background = GradientDrawable().apply { setColor(Color.rgb(246, 243, 255)); cornerRadius = dp(22).toFloat() }
            elevation = dp(6).toFloat()
        }
        val you = label("", 12f, Color.rgb(110, 100, 150)).apply { visibility = View.GONE; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
        balloon.addView(you)
        val reply = TextView(this).apply {
            textSize = 17f; setTextColor(Color.rgb(28, 24, 52)); setLineSpacing(dp(2).toFloat(), 1f)
        }
        val scroll = CappedScroll(this, (metrics.heightPixels * .36f).toInt()).apply { addView(reply); isVerticalScrollBarEnabled = true }
        balloon.addView(scroll)
        content.addView(balloon, LinearLayout.LayoutParams(-1, -2))
        content.addView(label("▼", 22f, Color.rgb(246, 243, 255)).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = -dp(9) })
        replyText = reply; youText = you; replyScroll = scroll

        val avatar = WordSiegeMascotView(this).apply {
            setSkin(store.skin())
            updateGame(null, false, 0, 0, 0, null, emptyList(), true, WordSiegeMascotEmotion.HAPPY)
            contentDescription = "Obi"
            setOnClickListener { animate(nextMove()) }
            setOnLongClickListener { startPushToTalk(); true }
        }
        content.addView(avatar, LinearLayout.LayoutParams(dp(112), dp(112)))
        chatMascot = avatar

        val field = EditText(this).apply {
            hint = "Obi'ye yaz…"
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(170, 190, 204))
            textSize = 16f; setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 48, 68)); cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(84, 111, 130))
            }
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            // Wrap long text over up to five lines (growing upwards) while Enter still sends.
            setHorizontallyScrolling(false)
            maxLines = 5
            imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            // Ask the keyboard for Turkish instead of whatever language it used last.
            imeHintLocales = android.os.LocaleList(java.util.Locale("tr", "TR"))
        }
        input = field
        fun send() {
            val text = field.text.toString().trim()
            field.text.clear()
            submit(text)
        }
        field.setOnEditorActionListener { _, actionId, event ->
            val enter = event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || enter) { send(); true } else false
        }
        val sendButton = pill("➤", true).apply { setOnClickListener { send() } }
        val compose = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        compose.addView(field, LinearLayout.LayoutParams(0, -2, 1f))
        compose.addView(sendButton, LinearLayout.LayoutParams(dp(52), dp(50)).apply { leftMargin = dp(8) })
        content.addView(compose, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        // Keep the writing field above the keyboard.
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bottom = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(android.view.WindowInsets.Type.ime() or android.view.WindowInsets.Type.navigationBars()).bottom
            } else 0
            view.setPadding(0, 0, 0, bottom)
            insets
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 0, PixelFormat.TRANSLUCENT,
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                if (Build.VERSION.SDK_INT >= 30) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            windows.addView(root, params)
            panel = root
            if (listener.isWakeMode) listener.stopWakeMode()
        } catch (_: SecurityException) {
            return
        }
        mascot?.visibility = View.GONE
        applyLook()
        if (ask != Pending.NONE) startAsk(ask)
        else if (lastReply.isNotBlank()) showReply(lastReply, remember = false)
        else say(if (gemini.connected()) "Buradayım! Ne yapalım? 💜" else
            "Buradayım! Not, liste, alarm, hava durumu… hepsini bana yazarak söyleyebilirsin.\nSerbest sohbet için ‘Gemini bağla’ yaz.")
        animate(WordSiegeMascotAction.WAVE)
        field.requestFocus()
        handler.postDelayed({
            if (panel != null) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
        }, 200L)
    }

    private fun startAsk(ask: Pending) {
        pending = ask
        when (ask) {
            Pending.MOOD -> say("Bugün nasıldın? 1'den 5'e bir sayı ya da birkaç kelime yaz 💜")
            else -> Unit
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun pill(text: String, accent: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = 18f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            if (accent) { colors = intArrayOf(ACCENT, Color.rgb(84, 104, 214)); orientation = GradientDrawable.Orientation.LEFT_RIGHT }
            else setColor(CHIP)
            cornerRadius = dp(25).toFloat()
        }
        isClickable = true
    }

    /** Obi answers in the balloon and moves its mouth for a moment. */
    private fun say(text: String, action: WordSiegeMascotAction? = null) {
        showReply(text, remember = true, action = action)
        if (panel == null) showBubble(text)
        if (voicePending) {
            voicePending = false
            // A spoken request gets a spoken "done" unless Obi is asking something back.
            if (!text.contains("?") && !text.contains("anlayamadım") && !text.contains("yok")) playClip("done")
        }
    }

    private fun showReply(text: String, remember: Boolean, action: WordSiegeMascotAction? = null) {
        if (remember) lastReply = text
        replyText?.text = text
        youText?.let { you ->
            you.text = if (lastUser.isBlank()) "" else "Sen: $lastUser"
            you.visibility = if (lastUser.isBlank()) View.GONE else View.VISIBLE
        }
        replyScroll?.post { replyScroll?.scrollTo(0, 0) }
        action?.let(::animate)
        chatMascot?.updateCompanion(actionKey, null, false, 1f, true, true, hat = hat)
        handler.postDelayed({ chatMascot?.updateCompanion(actionKey, null, false, 1f, false, true, hat = hat) },
            (900L + text.length * 35L).coerceAtMost(4_000L))
    }

    private fun thinking(text: String = "…") {
        replyText?.text = text
        if (panel == null) showBubble(text)
        animate(WordSiegeMascotAction.THINK)
    }

    private fun openKelimeTahti() {
        hidePanel()
        val launch = packageManager.getLaunchIntentForPackage(KELIME_TAHTI)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$KELIME_TAHTI"))
        runCatching { startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$KELIME_TAHTI")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun openSettings(intent: Intent) {
        hidePanel()
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun openExactAlarmSettings() =
        openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))

    private fun openGeminiSetup() {
        hidePanel()
        mascot?.visibility = View.GONE
        runCatching { startActivity(Intent(this, ObiGeminiSetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { showMascotIfAllowed() }
    }

    private val helpText = "Bana yazman yeterli, örneğin:\n" +
        "• listeye süt ekmek ekle / listem\n• not al: faturayı öde / notlarım\n" +
        "• her gün 21'de ilacımı hatırlat / 10 dakika sonra alarm kur / alarmlarım\n" +
        "• Arnavutköy'de hava nasıl / şehrim İzmir\n• market 250 TL / harcamalarım\n" +
        "• geri sayım tatil 15 temmuz / annemin doğum günü 5 mart\n" +
        "• odaklan 25 dakika • oyun oynayalım • dolap / Pofi'yi giy / bere tak\n" +
        "• panodakini özetle / panodakini çevir • bugünün kelimeleri\n" +
        "• sesini dene / sesini değiştir / sesini incelt / sesini kalınlaştır / ses durumu\n• sesli uyarıyı kapat\n• beni dinle / dinlemeyi kapat • Obi'yi basılı tut: sesli komut\n• gizlen • küçül / büyü • alarm izni / bildirim izni\n• tam ekranda gizlenme / tam ekranda gizlen\n• su hatırlatıcısını aç • Gemini bağla"

    private fun clipboardText(): String = runCatching {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
    }.getOrNull().orEmpty()

    private fun clipboardTask(kind: String) {
        val text = clipboardText()
        if (text.isBlank()) { say("Panoda yazı göremedim. Metni buraya yapıştırıp ‘özetle:’ ya da ‘çevir:’ diye başlayarak da yazabilirsin."); return }
        lastUser = "Panodaki: “${text.take(60)}${if (text.length > 60) "…" else ""}”"
        if (kind == "translate" && !gemini.connected()) { translate("çevir: ${text.take(1500)}"); return }
        if (!gemini.connected()) { say("Bunun için önce Gemini'yi bağlamalısın. ‘Gemini bağla’ yaz."); return }
        val prompt = when (kind) {
            "summary" -> "Aşağıdaki metni Türkçe olarak en fazla 4 kısa maddede özetle:\n\n"
            "translate" -> "Aşağıdaki metin Türkçeyse İngilizceye, değilse Türkçeye çevir. Sadece çeviriyi yaz:\n\n"
            else -> "Aşağıdaki mesaja kibar, sıcak ve kısa bir Türkçe cevap taslağı yaz. Sadece cevabı yaz:\n\n"
        } + text.take(3000)
        askGemini(prompt, tools = false)
    }

    private fun askGemini(prompt: String, tools: Boolean = true) {
        requestId++
        val current = requestId
        thinking()
        gemini.ask(prompt, tools) { answer ->
            if (current != requestId) return@ask
            if (answer.intent != null && runIntent(answer.intent, answer.args ?: JSONObject(), answer.text)) return@ask
            say(answer.text, answer.action)
        }
    }

    private fun fetchWeather(candidates: List<String>, saveCity: Boolean) {
        requestId++
        val current = requestId
        thinking("Hava verisini getiriyorum… ☁")
        Thread {
            val result = weather.blocking(candidates)
            handler.post {
                if (current != requestId) return@post
                if (result == null) {
                    pending = Pending.CITY
                    say("‘${candidates.firstOrNull() ?: ""}’ için hava bilgisi bulamadım. Şehir ya da ilçe adını tekrar yazar mısın?", WordSiegeMascotAction.SHRUG)
                } else {
                    if (saveCity && store.city() == null) candidates.firstOrNull()?.let { store.handle("şehrim $it") }
                    say(result, WordSiegeMascotAction.POINT)
                }
            }
        }.start()
    }

    private fun showList() {
        val items = store.shopping()
        if (items.isEmpty()) { pending = Pending.LIST_ITEMS; say("Alışveriş listen boş. Ne ekleyeyim? (örneğin: süt ekmek kahve)", WordSiegeMascotAction.THINK) }
        else say(store.handle("listemi göster"), WordSiegeMascotAction.NOD)
    }

    private fun wardrobeText(): String {
        val bond = store.bond()
        val open = ObiLife.unlocked(bond)
        val next = ObiLife.nextUnlock(bond)
        val skins = open.mapNotNull { it.skin?.titleTr }.joinToString(", ")
        val hats = open.mapNotNull { it.hat }.joinToString(", ") { ObiLife.hatName(it) }.ifBlank { "henüz yok" }
        return "♥ Bağımız: $bond\n👕 Karakterler: $skins\n🎩 Şapkalar: $hats" +
            (next?.let { "\nSıradaki: ${it.name} (${it.points} ♥ gerekiyor)" } ?: "\nHer şey açıldı! 👑") +
            "\n\nGiymek için: ‘Pofi'yi giy’, ‘bere tak’, ‘şapkayı çıkar’."
    }

    /** Runs a phone task that Gemini recognised. Returns false when it could not be done locally. */
    private fun runIntent(intent: String, args: JSONObject, geminiText: String): Boolean {
        fun strings(name: String): List<String> = args.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty().filter { it.isNotBlank() }
        when (intent) {
            "list_add" -> strings("items").ifEmpty { listOf(args.optString("item")).filter { it.isNotBlank() } }
                .takeIf { it.isNotEmpty() }?.let { say(store.handle("listeye ${it.joinToString(", ")} ekle"), WordSiegeMascotAction.NOD) } ?: return false
            "list_show" -> showList()
            "list_remove" -> args.optString("item").takeIf { it.isNotBlank() }?.let { say(store.handle("listeden ${ObiLife.lower(it)} sil")) } ?: return false
            "list_clear" -> say(store.handle("listeyi temizle"))
            "note_add" -> args.optString("text").takeIf { it.isNotBlank() }?.let { say(store.handle("not al $it"), WordSiegeMascotAction.NOD) } ?: return false
            "notes_show" -> say(store.handle("notlarımı göster"))
            "reminder", "alarm" -> {
                val time = args.optString("datetime").takeIf { it.length >= 16 }?.let {
                    runCatching { java.time.LocalDateTime.parse(it.take(16)) }.getOrNull()
                }?.takeIf { it.isAfter(java.time.LocalDateTime.now()) }
                    ?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: return false
                val repeat = args.optString("repeat").takeIf { it == "daily" || it == "weekdays" || it.startsWith("weekly:") }
                val alarm = intent == "alarm"
                if (alarm) { say(store.setAlarm(time, repeat), WordSiegeMascotAction.CHEER); return true }
                val text = args.optString("text").ifBlank { "Hatırlatıcı" }
                store.schedule(time, text, if (alarm) "alarm" else "reminder", repeat)
                val day = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val whenText = repeat?.let { ObiLife.repeatLabel(it) } ?: when (day) {
                    LocalDate.now() -> "bugün"; LocalDate.now().plusDays(1) -> "yarın"
                    else -> "${day.dayOfMonth} ${ObiLife.title(ObiLife.months[day.monthValue - 1])}"
                }
                say("${if (alarm) "Alarmı" else "‘$text’ hatırlatıcısını"} $whenText ${store.formatClock(time)} için kurdum ✓" +
                    if (!store.canScheduleExact()) "\nTam saatinde çalması için ‘kesin alarm izni’ yaz." else "", WordSiegeMascotAction.CHEER)
            }
            "alarms_show" -> say(store.handle("alarmlarım"))
            "weather" -> {
                val city = args.optString("city").takeIf { it.isNotBlank() } ?: store.city()
                if (city == null) { pending = Pending.CITY; say("Hangi şehir ya da ilçe?") } else fetchWeather(listOf(ObiLife.lower(city)), saveCity = true)
            }
            "expense_add" -> {
                val amount = args.optDouble("amount", Double.NaN).takeIf { !it.isNaN() && it > 0 } ?: return false
                say(store.handle("harcama: ${args.optString("label").ifBlank { "Harcama" }} ${if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()} tl"))
            }
            "expenses_show" -> say(store.handle("harcamalarım"))
            "countdown_add" -> {
                val date = runCatching { LocalDate.parse(args.optString("date").take(10)) }.getOrNull() ?: return false
                say(store.handle("geri sayım ${args.optString("label").ifBlank { "etkinlik" }} ${date.dayOfMonth} ${ObiLife.months[date.monthValue - 1]} ${date.year}"))
            }
            "countdowns_show" -> say(store.handle("geri sayımlar"))
            "birthday_add" -> {
                val day = args.optInt("day"); val month = args.optInt("month")
                if (day !in 1..31 || month !in 1..12) return false
                say(store.handle("${ObiLife.lower(args.optString("person").ifBlank { "o kişinin" })} doğum günü $day ${ObiLife.months[month - 1]}"))
            }
            "focus" -> say(startFocus(args.optInt("minutes", 25).coerceIn(5, 120)))
            "game" -> startGameByName(args.optString("name"))
            "wardrobe" -> say(wardrobeText())
            "mood" -> args.optInt("score").takeIf { it in 1..5 }?.let { say(store.logMood(it)) } ?: return false
            "words_today" -> say(store.handle("bugünün kelimeleri"))
            else -> return false
        }
        return true
    }

    private fun submit(command: String) {
        if (command.isBlank()) return
        lastInteractionAt = System.currentTimeMillis()
        lastUser = command
        reward(1)
        val t = ObiLife.lower(command)

        // Follow-up answers to Obi's own question.
        val asked = pending
        pending = Pending.NONE
        when (asked) {
            Pending.CITY -> if (t.split(Regex("\\s+")).size <= 3 && !store.canHandle(command)) {
                fetchWeather(ObiParse.cityCandidates("$t hava").ifEmpty { listOf(t) }, saveCity = true); return
            }
            Pending.LIST_ITEMS -> if (!store.canHandle(command) && !t.contains("?")) {
                say(store.handle("listeye ${ObiLife.listItems(t).joinToString(", ")} ekle"), WordSiegeMascotAction.NOD); return
            }
            Pending.MOOD -> ObiLife.moodScore(command)?.let { say(store.logMood(it), if (it >= 4) WordSiegeMascotAction.DANCE else WordSiegeMascotAction.NUZZLE); return }
            Pending.GAME_PICK -> if (startGameByName(t)) return
            Pending.NONE -> Unit
        }
        game?.let { if (playGame(it, command)) return }

        when {
            t.contains("gemini") && (t.contains("bağla") || t.contains("bagla") || t.contains("anahtar")) -> { openGeminiSetup(); return }
            t.contains("dinlemeyi kapat") || t.contains("sesli komut kapat") || t.contains("sesli komutu kapat") || t.contains("beni dinleme") -> { say(setWakeMode(false)); return }
            t.contains("beni dinle") || t.contains("sesli komut") || t.contains("dinlemeyi aç") || t.contains("dinlemeyi ac") || t.contains("obi modu") -> { say(setWakeMode(true)); return }
            t == "yardım" || t == "yardim" || t.contains("ne yapabilirsin") || t.contains("neler yapabilirsin") || t == "komutlar" -> { say(helpText); return }
            t.contains("odaklan") || t.contains("pomodoro") -> {
                val minutes = Regex("(\\d{1,3})").find(t)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(5, 120) ?: 25
                say(startFocus(minutes)); return
            }
            t.contains("odağı bitir") || t.contains("odagi bitir") || t.contains("odak bitir") -> {
                store.stopFocus(); endFocusView(); say("Odak modunu bitirdim. Güzel çalıştın! 👏", WordSiegeMascotAction.CLAP); return
            }
            t.contains("oyun") || t == "oyna" -> {
                pending = Pending.GAME_PICK
                say("Hangisini oynayalım? 🎮\n• kelime tahmini\n• bilmece\n• kelime testi\n• yakala (Obi'yi yakala)", WordSiegeMascotAction.BOUNCE); return
            }
            startGameByName(t, onlyExact = true) -> return
            ObiLife.isWardrobe(t) || t.contains("bağımız") || t.contains("bagimiz") -> { say(wardrobeText()); return }
            t.contains("pano") -> {
                clipboardTask(when { t.contains("çevir") || t.contains("cevir") -> "translate"; t.contains("cevap") -> "reply"; else -> "summary" }); return
            }
            t.contains("kelime tahtı") && !t.contains("?") -> { openKelimeTahti(); return }
            t.contains("tam ekran") && (t.contains("gizlen") || t.contains("saklan")) -> {
                val on = !t.contains("gizlenme") && !t.contains("saklanma") && !t.contains("kapat")
                localPrefs.edit().putBoolean("fullscreen_hide", on).apply()
                if (!on) onFullscreen(false)
                say(if (on) "Tamam! Oyun ya da video tam ekran açılınca kendiliğimden saklanırım 🙈" else "Tamam, tam ekranda da görünür kalacağım."); return
            }
            t == "gizlen" || t == "saklan" || t.contains("obi gizlen") || t.contains("kendini gizle") -> {
                hidePanel(); setUserHidden(true); return
            }
            (t.contains("izin") || t.contains("izni")) && t.contains("alarm") -> { openExactAlarmSettings(); return }
            (t.contains("izin") || t.contains("izni")) && t.contains("bildirim") -> {
                openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)); return
            }
            t == "izinler" || t == "izin" || t.contains("izinleri aç") || t.contains("uygulama ayarları") -> {
                openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))); return
            }
            Regex("(?<!\\p{L})(?:küçül|küçült|kucul|kuçul)(?!\\p{L})").containsMatchIn(t) -> {
                val size = resizeMascot(-12)
                say(if (size <= 64) "Daha fazla küçülemem, minicik oldum! 🐣" else "Biraz küçüldüm. Böyle iyi mi? (‘biraz daha küçül’ ya da ‘büyü’)", WordSiegeMascotAction.SHY); return
            }
            Regex("(?<!\\p{L})(?:büyü|büyüt|buyu|buyut)(?!\\p{L})").containsMatchIn(t) -> {
                val size = resizeMascot(12)
                say(if (size >= 140) "Bundan büyük olamam! 💪" else "Biraz büyüdüm! Böyle iyi mi?", WordSiegeMascotAction.PROUD_POSE); return
            }
            t.contains("bildirim izni") -> { openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)); return }
            t.contains("sesli uyarı") || t.contains("sesli uyari") -> {
                val on = !t.contains("kapat")
                localPrefs.edit().putBoolean("voice_alerts", on).apply()
                say(if (on) "Sesli uyarıları açtım ♪" else "Sesli uyarıları kapattım. Sadece bildirim göndereceğim."); return
            }
            t.contains("sesini incelt") || t.contains("sesin ince") || t.contains("sesini kalınlaştır") || t.contains("sesini kalinlastir") || t.contains("sesin kalın") -> {
                val thinner = t.contains("ince")
                ObiVoiceAlertService.setPitch(this, ObiVoiceAlertService.pitch(this) + if (thinner) 0.15f else -0.15f)
                runCatching { startForegroundService(Intent(this, ObiVoiceAlertService::class.java).setAction(ObiVoiceAlertService.ACTION_PREVIEW)) }
                say(if (thinner) "Sesimi biraz incelttim. Nasıl? ♪" else "Sesimi biraz kalınlaştırdım. Nasıl? ♪", WordSiegeMascotAction.GIGGLE); return
            }
            t.contains("sesini dene") || t.contains("sesini duy") -> {
                runCatching { startForegroundService(Intent(this, ObiVoiceAlertService::class.java).setAction(ObiVoiceAlertService.ACTION_PREVIEW)) }
                say("Sesimi hazırlıyorum… ♪ (ilk seferde birkaç saniye sürebilir)", WordSiegeMascotAction.DANCE); return
            }
            t.contains("sesini değiştir") || t.contains("sesini degistir") || t.contains("başka ses") || t.contains("baska ses") -> {
                val name = ObiGeminiVoice(this).nextVoice()
                runCatching { startForegroundService(Intent(this, ObiVoiceAlertService::class.java).setAction(ObiVoiceAlertService.ACTION_PREVIEW)) }
                say("Yeni sesim: $name. Dinle bakalım! (Beğenmezsen yine ‘sesini değiştir’ yaz.)", WordSiegeMascotAction.GIGGLE); return
            }
            t.contains("ses durumu") || t.contains("sesin neden") -> { say(ObiGeminiVoice(this).lastStatus()); return }
            t.contains("telefon sesi") -> {
                val on = !t.contains("kapat")
                localPrefs.edit().putBoolean("phone_voice", on).apply()
                say(if (on) "Gemini sesi alınamazsa telefonun kendi sesiyle okuyacağım." else "Telefonun sesini kullanmayacağım; Gemini sesi yoksa sadece bildirim gelir."); return
            }
            t.startsWith("ruh halim") || t.startsWith("bugün ") && ObiLife.moodScore(t.removePrefix("bugün ")) != null && t.split(" ").size <= 3 -> {
                ObiLife.moodScore(t)?.let { say(store.logMood(it)); return }
            }
        }
        ObiLife.wear(t)?.let { wear ->
            val open = ObiLife.unlocked(store.bond())
            val skinOk = wear.skin == null || open.any { it.skin == wear.skin }
            val hatOk = wear.hat == null || wear.hat == WordSiegeMascotHat.NONE || open.any { it.hat == wear.hat }
            if (!skinOk || !hatOk) {
                val need = ObiLife.unlocks.firstOrNull { (wear.skin != null && it.skin == wear.skin) || (wear.hat != null && it.hat == wear.hat) }
                say("Bu henüz açılmadı 🔒 ${need?.points ?: ""} ♥ bağ puanında açılacak. Şu an: ${store.bond()} ♥"); return
            }
            store.wear(wear.skin, wear.hat)
            say("Nasıl oldum? ✨", WordSiegeMascotAction.PROUD_POSE); return
        }
        if (ObiLife.listCommand(t) == ObiLife.ListCommand.Show) { showList(); return }

        val wantsSchedule = (t.contains("hatırlat") || t.contains("hatirlat") || t.contains("alarm kur") || t.contains("uyandır") || t.contains("uyandir")) &&
            !t.contains("su hatırlat") && !t.contains("mola hatırlat") && !ObiLife.isAlarmList(t) && ObiLife.alarmToDelete(t) == null
        if (wantsSchedule && ObiParse.triggerTime(t) == null && gemini.connected()) {
            askGemini(command); return
        }
        if (store.canHandle(command)) {
            val reply = store.handle(command)
            say(reply, when {
                t.contains("alarm") || t.contains("hatırlat") -> WordSiegeMascotAction.CHEER
                else -> WordSiegeMascotAction.NOD
            })
            ObiWidget.refresh(this)
            return
        }
        if (weather.canHandle(command)) {
            val candidates = ObiParse.cityCandidates(command).ifEmpty { listOfNotNull(store.city()) }
            if (candidates.isEmpty()) { pending = Pending.CITY; say("Hangi şehir ya da ilçe? 🌤") } else fetchWeather(candidates, saveCity = false)
            return
        }
        if (gemini.connected()) { askGemini(command); return }
        if (!translate(command)) say(store.handle(command), nextMove())
    }

    // ---- Chat games ---------------------------------------------------------------------------------

    private fun startGameByName(raw: String, onlyExact: Boolean = false): Boolean {
        val t = ObiLife.lower(raw)
        val exact = t.split(Regex("\\s+")).size <= 3
        if (onlyExact && !exact) return false
        when {
            t.contains("tahmin") || t == "guess" -> startGuess()
            t.contains("bilmece") || t == "riddle" -> startRiddle()
            t.contains("test") || t.contains("quiz") -> startQuiz()
            t.contains("yakala") || t == "catch" -> startCatchGame()
            else -> return false
        }
        return true
    }

    private fun startGuess() {
        val answer = ObiLife.guessWords.random()
        game = GuessGame(answer)
        say("5 harfli bir kelime düşündüm. İlk harfi ‘${answer.first().uppercaseChar()}’. 6 hakkın var!\n🟩 doğru yer • 🟨 kelimede var • ⬜ yok\n(Vazgeçmek için ‘pes’)", WordSiegeMascotAction.THINK)
    }

    private fun startRiddle() {
        val riddle = ObiLife.riddles.random()
        game = RiddleGame(riddle)
        say("❓ ${riddle.question}\n(‘ipucu’ ya da ‘pes’ yazabilirsin)", WordSiegeMascotAction.THINK)
    }

    private fun startQuiz() {
        val quizGame = QuizGame(System.currentTimeMillis())
        game = quizGame
        askQuiz(quizGame)
    }

    private fun askQuiz(quizGame: QuizGame) {
        val quiz = ObiLife.quiz(LocalDate.now(), quizGame.index, quizGame.seed + quizGame.index)
        say("📘 Soru ${quizGame.index + 1}/3: “${quiz.word.en}” ne demek?\n" +
            quiz.options.mapIndexed { i, o -> "${'a' + i}) $o" }.joinToString("   "))
    }

    /** Returns true when the message was a move in the running game; other requests fall through. */
    private fun playGame(current: ChatGame, raw: String): Boolean {
        val t = ObiLife.lower(raw)
        val words = t.split(Regex("\\s+")).size
        if (t == "pes" || t == "çık" || t == "cik" || t == "bitir" || t == "oyunu bitir") {
            val reveal = when (current) {
                is GuessGame -> "Kelime ‘${current.answer}’ idi. Bir dahakine! 🙂"
                is RiddleGame -> "Cevap: ${current.riddle.answers.first()}. Güzel bilmeceydi, değil mi?"
                is QuizGame -> "Testi bitirdik. Puanın: ${current.score}/3"
            }
            game = null
            say(reveal, WordSiegeMascotAction.SHRUG)
            return true
        }
        when (current) {
            is GuessGame -> {
                if (words > 1) return false
                val guess = t.filter { it.isLetter() }
                if (guess.length != 5) { say("5 harfli bir kelime yaz 🙂"); return true }
                current.tries++
                val marks = ObiLife.guessMarks(current.answer, guess)
                when {
                    guess == current.answer -> { game = null; say("$marks\nBildin! ${current.tries}. denemede 🎉", WordSiegeMascotAction.DANCE); reward(3) }
                    current.tries >= 6 -> { game = null; say("$marks\nHakların bitti. Kelime ‘${current.answer}’ idi.", WordSiegeMascotAction.SHRUG) }
                    else -> say("$marks  ${guess.uppercase(java.util.Locale("tr", "TR"))}\nKalan hak: ${6 - current.tries}", WordSiegeMascotAction.NOD)
                }
            }
            is RiddleGame -> {
                if (words > 4) return false
                if (t == "ipucu") { say("💡 ${current.riddle.hint}"); return true }
                when {
                    ObiLife.riddleCorrect(current.riddle, t) -> { game = null; say("Doğru! 🎉 Cevap: ${current.riddle.answers.first()}", WordSiegeMascotAction.CHEER); reward(3) }
                    ++current.misses >= 3 -> { game = null; say("Olmadı 🙂 Cevap: ${current.riddle.answers.first()}", WordSiegeMascotAction.GIGGLE) }
                    else -> say("Hmm, değil. Tekrar dene ya da ‘ipucu’ yaz.", WordSiegeMascotAction.SHRUG)
                }
            }
            is QuizGame -> {
                val quiz = ObiLife.quiz(LocalDate.now(), current.index, current.seed + current.index)
                val letter = t.trim(')', '.', ' ').singleOrNull()?.let { it - 'a' }?.takeIf { it in quiz.options.indices }
                val answer = letter?.let { quiz.options[it] } ?: quiz.options.firstOrNull { ObiLife.lower(it) == t }
                if (answer == null && words > 2) return false
                val right = answer != null && ObiLife.lower(answer) == ObiLife.lower(quiz.word.tr)
                if (right) current.score++
                current.index++
                val feedback = (if (right) "Doğru ✓ " else "Olmadı. ") + "${quiz.word.en} = ${quiz.word.tr}\n“${quiz.word.example}”"
                animate(if (right) WordSiegeMascotAction.CLAP else WordSiegeMascotAction.SHRUG)
                if (current.index >= 3) {
                    game = null
                    say("$feedback\n\nTest bitti! Puanın: ${current.score}/3 ${if (current.score == 3) "🏆" else "✨"}")
                    if (current.score > 0) reward(3)
                } else {
                    say(feedback)
                    handler.postDelayed({ if (game === current) askQuiz(current) }, 2_200L)
                }
            }
        }
        return true
    }

    // ---- Offline translation -----------------------------------------------------------------------

    private fun translate(command: String): Boolean {
        val lower = ObiLife.lower(command)
        if (!lower.contains("çevir") && !lower.contains("cevir") && !lower.startsWith("translate ")) return false
        val explicitEnglish = lower.contains("ingilizceye") || lower.contains("english")
        val explicitTurkish = lower.contains("türkçeye") || lower.contains("turkceye")
        val sourceText = command.replace(Regex("(?i)ingilizceye|türkçeye|turkceye|english|translate|çevir|cevir|bunu|şunu|lütfen|please"), "")
            .trim(' ', ':', '-', '“', '”', '"')
        if (sourceText.isBlank()) {
            say("Çevireceğin metni de yaz: ‘Çevir: Hello, how are you?’")
            return true
        }
        thinking("Dili algılıyorum…")
        val placeholder = replyText
        if (explicitEnglish) startTranslation(sourceText, TranslateLanguage.TURKISH, TranslateLanguage.ENGLISH, placeholder)
        else if (explicitTurkish) startTranslation(sourceText, TranslateLanguage.ENGLISH, TranslateLanguage.TURKISH, placeholder)
        else {
            val identifier = LanguageIdentification.getClient()
            identifier.identifyLanguage(sourceText)
                .addOnSuccessListener { code ->
                    identifier.close()
                    val english = code == "en" || (code == "und" && sourceText.none { it in "çğıöşüÇĞİÖŞÜ" })
                    startTranslation(sourceText,
                        if (english) TranslateLanguage.ENGLISH else TranslateLanguage.TURKISH,
                        if (english) TranslateLanguage.TURKISH else TranslateLanguage.ENGLISH, placeholder)
                }
                .addOnFailureListener {
                    identifier.close()
                    startTranslation(sourceText, TranslateLanguage.TURKISH, TranslateLanguage.ENGLISH, placeholder)
                }
        }
        return true
    }

    private fun startTranslation(text: String, source: String, target: String, placeholder: TextView?) {
        val translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build())
        placeholder?.text = "Çeviri hazırlanıyor; ilk kullanımda dil modeli indirilebilir (mobil veri kullanabilir)…"
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated -> if (placeholder === replyText) say(translated, WordSiegeMascotAction.NOD); translator.close() }
                    .addOnFailureListener { if (placeholder === replyText) say("Çeviri başarısız oldu. Tekrar dene."); translator.close() }
            }
            .addOnFailureListener { if (placeholder === replyText) say("Dil modeli indirilemedi. İnternet bağlantını kontrol et."); translator.close() }
    }

    private fun hidePanel() {
        if (panel != null && wakeWanted() && listener.hasPermission()) handler.postDelayed({ if (panel == null) listener.startWakeMode() }, 600L)
        panel?.let { runCatching { windows.removeView(it) } }
        panel = null
        chatMascot = null
        replyText = null; youText = null; replyScroll = null; input = null
        showMascotIfAllowed()
        requestId++
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val view = mascot ?: return
        val params = mascotParams ?: return
        params.x = params.x.coerceIn(0, (resources.displayMetrics.widthPixels - params.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (resources.displayMetrics.heightPixels - params.height).coerceAtLeast(0))
        runCatching { windows.updateViewLayout(view, params) }
        placeTimer()
        hidePanel()
    }

    override fun onDestroy() {
        runCatching { listener.stop() }
        runCatching { unregisterReceiver(events) }
        runCatching { unregisterReceiver(phone) }
        handler.removeCallbacksAndMessages(null)
        hideBubble()
        hidePanel()
        timerLabel?.let { runCatching { windows.removeView(it) } }
        timerLabel = null
        removeHideTarget()
        probe?.let { runCatching { windows.removeView(it) } }
        probe = null
        mascot?.let { runCatching { windows.removeView(it) } }
        mascot = null
        mascotParams = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
