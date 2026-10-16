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
        const val ACTION_READY = "com.obi.local.OVERLAY_READY"
        const val ACTION_ERROR = "com.obi.local.OVERLAY_ERROR"
        private const val CHANNEL = "obi_companion"
        private const val NOTIFICATION_ID = 1201
        private const val PREFS = "obi_overlay"
        private const val SIZE_DP = 112
        private const val FOCUS_SIZE_DP = 78
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

    // Panel parts that outlive one build of the menu.
    private var menuBox: LinearLayout? = null
    private var chatBox: LinearLayout? = null
    private var chatLog: LinearLayout? = null
    private var chatScroll: ScrollView? = null
    private var actionRow: LinearLayout? = null
    private var input: EditText? = null
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
                ACTION_SHOW -> { mascot?.visibility = View.VISIBLE; fetchTouchPack() }
                ObiStore.ACTION_LOOK_CHANGED -> applyLook()
                ObiStore.ACTION_ASK_MOOD -> {
                    animate(WordSiegeMascotAction.WAVE)
                    showBubble("Bugün nasıldın? Bana iki kez dokun, bir yüz seç 💜")
                }
                ObiStore.ACTION_NUDGE -> {
                    animate(runCatching { WordSiegeMascotAction.valueOf(intent.getStringExtra("action") ?: "BOUNCE") }.getOrDefault(WordSiegeMascotAction.BOUNCE))
                    showBubble(intent.getStringExtra("text") ?: "Buradayım!")
                }
                else -> {
                    animate(WordSiegeMascotAction.CHEER)
                    showBubble(intent.getStringExtra("text") ?: "Yaşasın! ✨")
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
            addAction(ObiStore.ACTION_ASK_MOOD); addAction(ObiStore.ACTION_LOOK_CHANGED)
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SHOW && mascot != null) {
            mascot?.visibility = View.VISIBLE
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
                if (store.focusEnd() > 0L) startFocusView()
            } else mascot?.visibility = View.VISIBLE
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
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_obi_notification)
            .setContentTitle("Obi ekranında")
            .setContentText("Dokun: söz ve hareket • Çift dokun: menü • Sürükle: taşı")
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

    // ---- The floating mascot -------------------------------------------------------------------

    private fun addMascot() {
        val size = dp(SIZE_DP)
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
            dragMoved = { delta ->
                val width = resources.displayMetrics.widthPixels
                val height = resources.displayMetrics.heightPixels
                params.x = (params.x + delta.x.roundToInt()).coerceIn(0, (width - params.width).coerceAtLeast(0))
                params.y = (params.y + delta.y.roundToInt()).coerceIn(0, (height - params.height).coerceAtLeast(0))
                runCatching { windows.updateViewLayout(this, params) }
                placeTimer()
            }
            dragFinished = {
                if (catchUntil == 0L) prefs.edit().putInt("x", params.x).putInt("y", params.y).apply()
            }
            setOnClickListener { onMascotTap(prefs) }
        }
        windows.addView(view, params)
        mascot = view
        mascotParams = params
        applyLook()
        prefs.edit().putBoolean("running", true).apply()
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
        if (panel != null) return
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
        if (params.width != dp(SIZE_DP)) {
            params.width = dp(SIZE_DP); params.height = dp(SIZE_DP)
            runCatching { windows.updateViewLayout(view, params) }
        }
    }

    // ---- "Obi'yi yakala" ----------------------------------------------------------------------------

    private fun startCatchGame() {
        hidePanel()
        catchScore = 0
        catchUntil = SystemClock.uptimeMillis() + 15_000L
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

    // ---- The panel --------------------------------------------------------------------------------

    private fun showPanel(openChat: Boolean) {
        if (!Settings.canDrawOverlays(this)) return
        hideBubble()
        if (panel != null) { if (openChat) openChatView() ; return }
        val metrics = resources.displayMetrics
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(CARD_TOP, CARD_BOTTOM)).apply {
                cornerRadius = dp(26).toFloat(); setStroke(dp(1), Color.argb(150, 92, 179, 168))
            }
            elevation = dp(18).toFloat()
        }
        val menu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        menuBox = menu; chatBox = chat
        card.addView(menu); card.addView(chat)
        buildChat(chat, (metrics.heightPixels * .34f).toInt())
        showHome()
        val params = WindowManager.LayoutParams(
            (metrics.widthPixels - dp(24)).coerceAtMost(dp(400)), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(48)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            windows.addView(card, params)
            panel = card
            if (openChat) openChatView()
        } catch (_: SecurityException) {
            // User may revoke the overlay permission while Obi is open.
        }
    }

    private fun header(title: String, back: (() -> Unit)?): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        if (back != null) addView(label("‹", 28f, SOFT).apply {
            gravity = Gravity.CENTER; contentDescription = "Geri"; setOnClickListener { back() }
        }, LinearLayout.LayoutParams(dp(36), dp(44)))
        addView(label(title, 20f, Color.WHITE, bold = true), LinearLayout.LayoutParams(0, dp(44), 1f))
        addView(label("×", 28f, SOFT).apply {
            gravity = Gravity.CENTER; contentDescription = "Kapat"; setOnClickListener { hidePanel() }
        }, LinearLayout.LayoutParams(dp(42), dp(44)))
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun pill(text: String, accent: Boolean, small: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        textSize = if (small) 12f else 14f
        gravity = Gravity.CENTER
        setTextColor(if (accent) Color.WHITE else SOFT)
        setPadding(dp(10), 0, dp(10), 0)
        background = GradientDrawable().apply {
            if (accent) { colors = intArrayOf(ACCENT, Color.rgb(84, 104, 214)); orientation = GradientDrawable.Orientation.LEFT_RIGHT }
            else setColor(CHIP)
            cornerRadius = dp(15).toFloat()
        }
        isClickable = true
    }

    /** Rows of two equal chips. */
    private fun grid(items: List<Pair<String, () -> Unit>>): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        items.chunked(2).forEach { row ->
            addView(LinearLayout(this@ObiOverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                row.forEachIndexed { i, (text, click) ->
                    addView(pill(text, false).apply { setOnClickListener { click() } },
                        LinearLayout.LayoutParams(0, dp(44), 1f).apply { if (i == 0 && row.size > 1) rightMargin = dp(6) })
                }
                if (row.size == 1) addView(View(this@ObiOverlayService), LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(6) })
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }
    }

    private fun section(title: String): TextView = label(title, 12f, MINT, bold = true).apply {
        setPadding(dp(4), dp(14), 0, dp(2))
    }

    private fun showHome() {
        val menu = menuBox ?: return
        menu.removeAllViews()
        menu.addView(header("✦  Obi", null))
        val bond = store.bond()
        val next = ObiLife.nextUnlock(bond)
        menu.addView(label("♥ Bağımız: $bond" + (next?.let { "  •  Sıradaki: ${it.name} (${it.points})" } ?: "  •  Her şey açıldı! 👑"), 12f, GOLD).apply {
            setPadding(dp(4), 0, 0, dp(6))
        })
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = CappedScroll(this, (resources.displayMetrics.heightPixels * .62f).toInt()).apply {
            isVerticalScrollBarEnabled = false; addView(content)
        }
        menu.addView(scroll)

        if (!gemini.connected()) {
            content.addView(label("Gemini ile sohbet için Google AI Studio'dan ücretsiz bir API anahtarı al ve bağla. Bağlanana kadar not, alarm ve liste yerel çalışır.", 13f, SOFT).apply {
                setPadding(dp(4), dp(4), dp(4), dp(8))
            })
            content.addView(pill("✦  Gemini'yi bağla", true).apply {
                setOnClickListener {
                    hidePanel()
                    mascot?.visibility = View.GONE
                    runCatching { startActivity(Intent(this@ObiOverlayService, ObiGeminiSetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { mascot?.visibility = View.VISIBLE }
                }
            }, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(4) })
        }
        val main = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        main.addView(pill("♡  Sohbet", true).apply { setOnClickListener { openChatView() } },
            LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(6) })
        main.addView(pill("✎  Notlarım", false).apply { setOnClickListener { openChatView(); submit("notlarımı göster") } },
            LinearLayout.LayoutParams(0, dp(50), 1f))
        content.addView(main, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })

        // Mood of the day.
        content.addView(section(if (store.moodLoggedToday()) "BUGÜN NASILDIN? (kaydedildi)" else "BUGÜN NASILDIN?"))
        val today = store.moods()[LocalDate.now()]
        val moods = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        ObiLife.moodFaces.forEachIndexed { i, face ->
            moods.addView(TextView(this).apply {
                text = face; textSize = 24f; gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(if (today == i + 1) ACCENT else CHIP); cornerRadius = dp(14).toFloat()
                }
                setOnClickListener {
                    val reply = store.logMood(i + 1)
                    animate(if (i >= 3) WordSiegeMascotAction.DANCE else if (i <= 1) WordSiegeMascotAction.NUZZLE else WordSiegeMascotAction.NOD)
                    openChatView(); addMessage(false, reply)
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { if (i > 0) leftMargin = dp(5) })
        }
        content.addView(moods)

        content.addView(section("GÜNLÜK"))
        val focusing = store.focusEnd() > 0L
        content.addView(grid(listOf(
            "⏰ Alarmlarım" to { openChatView(); submit("alarmlarım") },
            "🛒 Listem" to { openChatView(); submit("listemi göster") },
            (if (focusing) "⏹ Odağı bitir" else "⏱ Odaklan 25 dk") to {
                openChatView(); submit(if (focusing) "odağı bitir" else "odaklan 25 dakika")
            },
            "💸 Harcamalar" to { openChatView(); submit("harcamalarım") },
            "☁ Hava" to { openChatView(); submit("hava durumu") },
            "🎂 Doğum günleri" to { openChatView(); submit("doğum günleri") },
            "⏳ Geri sayımlar" to { openChatView(); submit("geri sayımlar") },
            "📘 Günün kelimeleri" to { openChatView(); submit("bugünün kelimeleri") },
        )))

        content.addView(section("EĞLENCE"))
        content.addView(grid(listOf(
            "🎮 Oyunlar" to { showGames() },
            "👕 Dolap" to { showWardrobe() },
            "📊 Haftam" to { openChatView(); submit("haftam nasıl") },
            "🎯 Kelime Tahtı" to { openKelimeTahti() },
        )))

        content.addView(section("AYARLAR"))
        val voiceOn = localPrefs.getBoolean("voice_alerts", true)
        content.addView(grid(listOf(
            (if (voiceOn) "♪ Sesli uyarı: açık" else "♪ Sesli uyarı: kapalı") to {
                localPrefs.edit().putBoolean("voice_alerts", !voiceOn).apply(); showHome()
            },
            "♪ Sesi dene" to {
                runCatching { startForegroundService(Intent(this, ObiVoiceAlertService::class.java).setAction(ObiVoiceAlertService.ACTION_PREVIEW)) }
                Unit
            },
            (if (store.waterEnabled()) "💧 Su: açık" else "💧 Su: kapalı") to {
                store.handle(if (store.waterEnabled()) "su hatırlatıcısını kapat" else "su hatırlatıcısını aç"); showHome()
            },
            (if (store.breakEnabled()) "🧘 Mola: açık" else "🧘 Mola: kapalı") to {
                store.handle(if (store.breakEnabled()) "mola hatırlatıcısını kapat" else "mola hatırlatıcısını aç"); showHome()
            },
            "🔔 Bildirim izni" to {
                hidePanel()
                runCatching { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                Unit
            },
        ) + (if (!store.canScheduleExact()) listOf("⏰ Kesin alarm izni" to { openExactAlarmSettings() }) else emptyList())))
        content.addView(label("İpucu: ‘her gün 21'de ilacımı hatırlat’, ‘listeye süt ekle’, ‘market 250 TL’, ‘geri sayım tatil 15 temmuz’, ‘şehrim İzmir’, ‘yatma saatim 23’.", 11f, Color.rgb(160, 178, 200)).apply {
            setPadding(dp(4), dp(12), dp(4), 0)
        })
    }

    private fun showGames() {
        val menu = menuBox ?: return
        menu.removeAllViews()
        menu.addView(header("🎮  Oyunlar", ::showHome))
        val best = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("catch_best", 0)
        val list = listOf(
            Triple("🔤  Kelime tahmini", "5 harfli kelimeyi 6 denemede bul") { openChatView(); startGuess() },
            Triple("❓  Bilmece", "Obi sorar, sen bulursun") { openChatView(); startRiddle() },
            Triple("📘  Kelime testi", "Bugünün 3 İngilizce kelimesi") { openChatView(); startQuiz() },
            Triple("⚡  Obi'yi yakala", "15 saniyede kaç kez? Rekor: $best") { startCatchGame() },
        )
        list.forEach { (title, hint, click) ->
            menu.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = GradientDrawable().apply { setColor(CHIP); cornerRadius = dp(16).toFloat() }
                addView(label(title, 15f, Color.WHITE, bold = true))
                addView(label(hint, 12f, SOFT))
                setOnClickListener { click() }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
    }

    private fun showWardrobe() {
        val menu = menuBox ?: return
        menu.removeAllViews()
        menu.addView(header("👕  Obi'nin dolabı", ::showHome))
        val bond = store.bond()
        menu.addView(label("Benimle konuştukça, oyun oynadıkça ve ruh hâlini yazdıkça bağımız güçlenir ve yeni kıyafetler açılır. ♥ $bond", 12f, SOFT).apply {
            setPadding(dp(4), 0, dp(4), dp(6))
        })
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        menu.addView(CappedScroll(this, (resources.displayMetrics.heightPixels * .6f).toInt()).apply { addView(content) })
        val skin = store.skin()
        val chosenHat = store.chosenHat()
        content.addView(section("KARAKTER"))
        content.addView(grid(ObiLife.unlocks.filter { it.skin != null }.map { item ->
            val open = item.points <= bond
            val mark = if (item.skin == skin) "✓ " else if (!open) "🔒 " else ""
            "$mark${item.name}${if (!open) " (${item.points})" else ""}" to {
                if (open) { store.wear(skin = item.skin); animate(WordSiegeMascotAction.SPARKLE); showWardrobe() }
            }
        }))
        content.addView(section("ŞAPKA"))
        val hats = listOf(ObiLife.Unlock(0, hat = WordSiegeMascotHat.NONE)) + ObiLife.unlocks.filter { it.hat != null }
        content.addView(grid(hats.map { item ->
            val open = item.points <= bond
            val mark = if (item.hat == chosenHat) "✓ " else if (!open) "🔒 " else ""
            "$mark${item.name}${if (!open) " (${item.points})" else ""}" to {
                if (open) { store.wear(hat = item.hat); animate(WordSiegeMascotAction.PROUD_POSE); showWardrobe() }
            }
        }))
        content.addView(label("Bayramlarda ve doğum gününde Obi, sen şapka seçmediysen özel şapkasını kendisi takar.", 11f, Color.rgb(160, 178, 200)).apply {
            setPadding(dp(4), dp(10), dp(4), 0)
        })
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

    private fun openExactAlarmSettings() {
        hidePanel()
        runCatching {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // ---- Chat ---------------------------------------------------------------------------------------

    private fun buildChat(chat: LinearLayout, logHeight: Int) {
        chat.addView(header("Obi ile sohbet") { chatBox?.visibility = View.GONE; menuBox?.visibility = View.VISIBLE; showHome() })
        val avatar = WordSiegeMascotView(this).apply {
            setSkin(store.skin())
            updateGame(null, false, 0, 0, 0, null, emptyList(), true, WordSiegeMascotEmotion.HAPPY)
            contentDescription = "Obi, sohbet arkadaşın"
            setOnClickListener { animate(nextMove()) }
        }
        chat.addView(avatar, LinearLayout.LayoutParams(dp(104), dp(104)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        chatMascot = avatar
        val log = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(4)) }
        val scroll = CappedScroll(this, logHeight).apply { addView(log); isVerticalScrollBarEnabled = false }
        chat.addView(scroll, LinearLayout.LayoutParams(-1, -2))
        chatLog = log; chatScroll = scroll
        loadLog()
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chat.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; addView(actions)
        }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(6) })
        actionRow = actions
        resetActions()
        val field = EditText(this).apply {
            hint = "Obi'ye yaz…"; maxLines = 3
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(170, 190, 204))
            textSize = 15f; setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 48, 68)); cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(84, 111, 130))
            }
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        input = field
        val send = pill("➤", true).apply { setOnClickListener { submit(field.text.toString().trim()); field.text.clear() } }
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { submit(field.text.toString().trim()); field.text.clear(); true } else false
        }
        val compose = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        compose.addView(field, LinearLayout.LayoutParams(0, -2, 1f))
        compose.addView(send, LinearLayout.LayoutParams(dp(52), dp(48)).apply { leftMargin = dp(8) })
        chat.addView(compose, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
    }

    private fun openChatView() {
        menuBox?.visibility = View.GONE
        chatBox?.visibility = View.VISIBLE
        mascot?.visibility = View.GONE
        animate(WordSiegeMascotAction.WAVE)
        scrollToEnd()
        val field = input ?: return
        field.requestFocus()
        handler.postDelayed({
            if (panel != null) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
        }, 180L)
    }

    private fun loadLog() {
        val log = chatLog ?: return
        val saved = runCatching { JSONArray(localPrefs.getString("chat_log", "[]") ?: "[]") }.getOrDefault(JSONArray())
        if (saved.length() == 0) { addBubble(log, false, "Buradayım. Bana yaz 💜\nNe yapabileceğimi merak edersen ‘ne yapabilirsin’ yaz."); return }
        for (i in 0 until saved.length()) saved.optJSONObject(i)?.let { addBubble(log, it.optBoolean("me"), it.optString("text")) }
    }

    private fun persist(me: Boolean, text: String) {
        val saved = runCatching { JSONArray(localPrefs.getString("chat_log", "[]") ?: "[]") }.getOrDefault(JSONArray())
        saved.put(JSONObject().put("me", me).put("text", text.take(1200)))
        val trimmed = JSONArray()
        for (i in maxOf(0, saved.length() - 30) until saved.length()) trimmed.put(saved.get(i))
        localPrefs.edit().putString("chat_log", trimmed.toString()).apply()
    }

    private fun addBubble(log: LinearLayout, me: Boolean, text: String): TextView {
        val bubbleView = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(if (me) Color.WHITE else Color.rgb(228, 245, 240))
            setTextIsSelectable(true)
            setPadding(dp(13), dp(9), dp(13), dp(9))
            background = GradientDrawable().apply {
                setColor(if (me) ACCENT else Color.rgb(37, 54, 73))
                cornerRadii = if (me) floatArrayOf(dp(18f), dp(18f), dp(4f), dp(4f), dp(18f), dp(18f), dp(18f), dp(18f))
                    else floatArrayOf(dp(4f), dp(4f), dp(18f), dp(18f), dp(18f), dp(18f), dp(18f), dp(18f))
            }
        }
        log.addView(bubbleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = if (me) Gravity.END else Gravity.START
            topMargin = dp(6)
            if (me) leftMargin = dp(40) else rightMargin = dp(40)
        })
        return bubbleView
    }

    /** Adds a chat line; returns the bubble so an answer can replace a "thinking" placeholder. */
    private fun addMessage(me: Boolean, text: String, save: Boolean = true): TextView? {
        val log = chatLog ?: return null
        val view = addBubble(log, me, text)
        if (save) persist(me, text)
        scrollToEnd()
        return view
    }

    private fun finish(placeholder: TextView?, text: String) {
        placeholder?.text = text
        persist(false, text)
        scrollToEnd()
    }

    private fun scrollToEnd() {
        val scroll = chatScroll ?: return
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun resetActions(extra: List<Pair<String, () -> Unit>> = emptyList()) {
        val row = actionRow ?: return
        row.removeAllViews()
        val items = extra + listOf(
            "📋 Özetle" to { clipboardTask("summary") },
            "📋 Çevir" to { clipboardTask("translate") },
            "📋 Kibar cevap" to { clipboardTask("reply") },
            "🧹 Sohbeti temizle" to { clearChat() },
        )
        items.forEachIndexed { i, (text, click) ->
            row.addView(pill(text, i < extra.size, small = true).apply { setOnClickListener { click() } },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply { if (i > 0) leftMargin = dp(6) })
        }
    }

    private fun clearChat() {
        localPrefs.edit().remove("chat_log").apply()
        gemini.forgetConversation()
        game = null
        chatLog?.removeAllViews()
        addMessage(false, "Temiz bir sayfa açtık ✨", save = false)
        resetActions()
    }

    private fun clipboardTask(kind: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = runCatching { clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()?.trim() }
            .getOrNull().orEmpty()
        if (text.isBlank()) {
            addMessage(false, "Panoda metin göremedim. Metni kopyalayıp yazı kutusuna yapıştırarak da gönderebilirsin.", save = false)
            return
        }
        addMessage(true, when (kind) { "summary" -> "📋 Panodakini özetle"; "translate" -> "📋 Panodakini çevir"; else -> "📋 Kibar bir cevap yaz" })
        if (kind == "translate" && !gemini.connected()) { translate("çevir: ${text.take(1500)}"); return }
        if (!gemini.connected()) { addMessage(false, "Bunun için Gemini'yi bağlamalısın. Menüde ‘Gemini'yi bağla’ düğmesi var."); return }
        val prompt = when (kind) {
            "summary" -> "Aşağıdaki metni Türkçe olarak en fazla 4 kısa maddede özetle:\n\n"
            "translate" -> "Aşağıdaki metin Türkçeyse İngilizceye, değilse Türkçeye çevir. Sadece çeviriyi yaz:\n\n"
            else -> "Aşağıdaki mesaja kibar, sıcak ve kısa bir Türkçe cevap taslağı yaz. Sadece cevabı yaz:\n\n"
        } + text.take(3000)
        askGemini(prompt)
    }

    private fun askGemini(prompt: String) {
        requestId++
        val current = requestId
        val placeholder = addMessage(false, "Bir bakayım… ✨", save = false)
        animate(WordSiegeMascotAction.THINK)
        gemini.ask(prompt) { answer ->
            if (current == requestId) {
                finish(placeholder, answer.text)
                animate(answer.action)
            }
        }
    }

    private fun submit(command: String) {
        if (command.isBlank()) return
        lastInteractionAt = System.currentTimeMillis()
        addMessage(true, command)
        reward(1)
        val t = ObiLife.lower(command)
        game?.let { if (playGame(it, command)) return }

        if (t.contains("odaklan") || t.contains("pomodoro")) {
            val minutes = Regex("(\\d{1,3})").find(t)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(5, 120) ?: 25
            addMessage(false, startFocus(minutes)); return
        }
        if (t.contains("odağı bitir") || t.contains("odagi bitir") || t.contains("odak bitir")) {
            store.stopFocus(); endFocusView()
            addMessage(false, "Odak modunu bitirdim. Güzel çalıştın! 👏"); animate(WordSiegeMascotAction.CLAP); return
        }
        if (t == "oyun" || t.contains("oyun oynayalım") || t.contains("oyun oynayalim")) { showGamesFromChat(); return }

        val wantsSchedule = (t.contains("hatırlat") || t.contains("hatirlat") || t.contains("alarm kur") || t.contains("uyandır") || t.contains("uyandir")) &&
            !t.contains("su hatırlat") && !t.contains("mola hatırlat") && !ObiLife.isAlarmList(t) && ObiLife.alarmToDelete(t) == null
        if (wantsSchedule && ObiParse.triggerTime(t) == null && gemini.connected()) {
            val placeholder = addMessage(false, "Saati çözüyorum… ⏰", save = false)
            animate(WordSiegeMascotAction.THINK)
            val repeat = ObiLife.repeatRule(t)
            gemini.extractSchedule(command) { time, text ->
                if (time == null) { finish(placeholder, store.handle(command)); return@extractSchedule }
                val alarm = t.contains("alarm") || t.contains("uyandır") || t.contains("uyandir")
                store.schedule(time, if (alarm) "Obi alarmı: Uyanma zamanı!" else (text ?: ObiParse.reminderNote(command)), if (alarm) "alarm" else "reminder", repeat)
                finish(placeholder, "${if (alarm) "Alarmı" else "‘${text ?: "Hatırlatıcı"}’ hatırlatıcısını"} ${store.formatClock(time)} için kurdum ✓ (${java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDate().let { "${it.dayOfMonth} ${ObiLife.title(ObiLife.months[it.monthValue - 1])}" }})")
                animate(WordSiegeMascotAction.CHEER)
                exactPrompt()
            }
            return
        }
        if (store.canHandle(command)) {
            addMessage(false, store.handle(command))
            animate(when {
                t.contains("alarm") || t.contains("hatırlat") -> WordSiegeMascotAction.CHEER
                t.contains("liste") -> WordSiegeMascotAction.NOD
                t.contains("tl") || t.contains("lira") -> WordSiegeMascotAction.THINK
                else -> WordSiegeMascotAction.NOD
            })
            exactPrompt()
            ObiWidget.refresh(this)
            return
        }
        if (weather.canHandle(command)) {
            requestId++
            val current = requestId
            val placeholder = addMessage(false, "Hava verisini getiriyorum… ☁", save = false)
            animate(WordSiegeMascotAction.LOOK_AROUND)
            weather.ask(command, store.city()) { result ->
                if (current == requestId) { finish(placeholder, result); animate(WordSiegeMascotAction.POINT) }
            }
            return
        }
        if (gemini.connected()) { askGemini(command); return }
        if (!translate(command)) {
            addMessage(false, store.handle(command))
            animate(nextMove())
        }
    }

    private fun exactPrompt() {
        if (!store.needsExactPermission) { resetActions(); return }
        resetActions(listOf("⏰ Kesin alarm iznini aç" to { openExactAlarmSettings() }))
    }

    // ---- Chat games ---------------------------------------------------------------------------------

    private fun showGamesFromChat() {
        addMessage(false, "Hangi oyunu oynayalım? 🎮")
        resetActions(listOf(
            "🔤 Kelime tahmini" to { startGuess() },
            "❓ Bilmece" to { startRiddle() },
            "📘 Kelime testi" to { startQuiz() },
            "⚡ Obi'yi yakala" to { startCatchGame() },
        ))
    }

    private fun startGuess() {
        val answer = ObiLife.guessWords.random()
        game = GuessGame(answer)
        animate(WordSiegeMascotAction.THINK)
        addMessage(false, "5 harfli bir kelime düşündüm. İlk harfi ‘${answer.first().uppercaseChar()}’. 6 hakkın var!\n🟩 doğru yer • 🟨 kelimede var • ⬜ yok\n(Vazgeçmek için ‘pes’)")
        resetActions(listOf("🏳 Pes" to { submit("pes") }))
    }

    private fun startRiddle() {
        val riddle = ObiLife.riddles.random()
        game = RiddleGame(riddle)
        animate(WordSiegeMascotAction.THINK)
        addMessage(false, "❓ ${riddle.question}")
        resetActions(listOf("💡 İpucu" to { submit("ipucu") }, "🏳 Pes" to { submit("pes") }))
    }

    private fun startQuiz() {
        val quizGame = QuizGame(System.currentTimeMillis())
        game = quizGame
        askQuiz(quizGame)
    }

    private fun askQuiz(quizGame: QuizGame) {
        val quiz = ObiLife.quiz(LocalDate.now(), quizGame.index, quizGame.seed + quizGame.index)
        addMessage(false, "📘 Soru ${quizGame.index + 1}/3: “${quiz.word.en}” ne demek?")
        resetActions(quiz.options.map { option -> option to { submit(option) } })
    }

    /** Returns true when the message was a move in the running game. */
    private fun playGame(current: ChatGame, raw: String): Boolean {
        val t = ObiLife.lower(raw)
        if (t == "pes" || t == "çık" || t == "cik" || t == "bitir") {
            val reveal = when (current) {
                is GuessGame -> "Kelime ‘${current.answer}’ idi. Bir dahakine! 🙂"
                is RiddleGame -> "Cevap: ${current.riddle.answers.first()}. Güzel bilmeceydi, değil mi?"
                is QuizGame -> "Testi bitirdik. Puanın: ${current.score}/3"
            }
            game = null; resetActions()
            addMessage(false, reveal); animate(WordSiegeMascotAction.SHRUG)
            return true
        }
        when (current) {
            is GuessGame -> {
                val guess = t.filter { it.isLetter() }
                if (guess.length != 5) { addMessage(false, "5 harfli bir kelime yaz 🙂"); return true }
                current.tries++
                val marks = ObiLife.guessMarks(current.answer, guess)
                if (guess == current.answer) {
                    game = null; resetActions()
                    addMessage(false, "$marks\nBildin! ${current.tries}. denemede 🎉"); animate(WordSiegeMascotAction.DANCE); reward(3)
                } else if (current.tries >= 6) {
                    game = null; resetActions()
                    addMessage(false, "$marks\nHakların bitti. Kelime ‘${current.answer}’ idi."); animate(WordSiegeMascotAction.SHRUG)
                } else {
                    addMessage(false, "$marks  ${guess.uppercase(java.util.Locale("tr", "TR"))}\nKalan hak: ${6 - current.tries}"); animate(WordSiegeMascotAction.NOD)
                }
            }
            is RiddleGame -> {
                if (t == "ipucu") { addMessage(false, "💡 ${current.riddle.hint}"); return true }
                if (ObiLife.riddleCorrect(current.riddle, t)) {
                    game = null; resetActions()
                    addMessage(false, "Doğru! 🎉 Cevap: ${current.riddle.answers.first()}"); animate(WordSiegeMascotAction.CHEER); reward(3)
                } else if (++current.misses >= 3) {
                    game = null; resetActions()
                    addMessage(false, "Olmadı 🙂 Cevap: ${current.riddle.answers.first()}"); animate(WordSiegeMascotAction.GIGGLE)
                } else {
                    addMessage(false, "Hmm, değil. Tekrar dene ya da ‘ipucu’ yaz."); animate(WordSiegeMascotAction.SHRUG)
                }
            }
            is QuizGame -> {
                val quiz = ObiLife.quiz(LocalDate.now(), current.index, current.seed + current.index)
                val right = t == ObiLife.lower(quiz.word.tr)
                if (right) current.score++
                addMessage(false, if (right) "Doğru ✓ ${quiz.word.en} = ${quiz.word.tr}\n“${quiz.word.example}”" else "Olmadı. ${quiz.word.en} = ${quiz.word.tr}\n“${quiz.word.example}”")
                animate(if (right) WordSiegeMascotAction.CLAP else WordSiegeMascotAction.SHRUG)
                current.index++
                if (current.index >= 3) {
                    game = null; resetActions()
                    addMessage(false, "Test bitti! Puanın: ${current.score}/3 ${if (current.score == 3) "🏆" else "✨"}")
                    if (current.score > 0) reward(3)
                } else handler.postDelayed({ if (game === current) askQuiz(current) }, 700L)
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
            addMessage(false, "Çevireceğin metni de yaz: ‘Çevir: Hello, how are you?’", save = false)
            return true
        }
        val placeholder = addMessage(false, "Dili algılıyorum…", save = false)
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
                    .addOnSuccessListener { translated -> finish(placeholder, translated); translator.close() }
                    .addOnFailureListener { finish(placeholder, "Çeviri başarısız oldu. Tekrar dene."); translator.close() }
            }
            .addOnFailureListener { finish(placeholder, "Dil modeli indirilemedi. İnternet bağlantını kontrol et."); translator.close() }
    }

    private fun hidePanel() {
        panel?.let { runCatching { windows.removeView(it) } }
        panel = null
        chatMascot = null
        menuBox = null; chatBox = null; chatLog = null; chatScroll = null; actionRow = null; input = null
        if (catchUntil == 0L) mascot?.visibility = View.VISIBLE
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
        runCatching { unregisterReceiver(events) }
        runCatching { unregisterReceiver(phone) }
        handler.removeCallbacksAndMessages(null)
        hideBubble()
        hidePanel()
        timerLabel?.let { runCatching { windows.removeView(it) } }
        timerLabel = null
        mascot?.let { runCatching { windows.removeView(it) } }
        mascot = null
        mascotParams = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
