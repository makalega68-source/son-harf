package com.obi.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
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
import com.google.mlkit.common.model.DownloadConditions
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
    private var phraseIndex = 0
    private var lastUsageCheckAt = 0L
    private val celebration = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            animate(WordSiegeMascotAction.CHEER)
            showBubble(intent.getStringExtra("text") ?: "Yaşasın! ✨")
        }
    }
    private val phrases = listOf(
        "İyi ki buradasın ✨", "Bugün birlikte güzel şeyler yapabiliriz!", "Gülüşün ekrana yakışıyor 💜",
        "Biraz soluklan; ben buradayım.", "Küçük bir adım bile ilerlemedir!", "Seninle vakit geçirmek güzel 🌟",
        "Kendine nazik davran, olur mu?", "Hadi, güzel bir şey düşünelim!"
    )
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
                checkScreenTime()
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

    private fun checkScreenTime() {
        val now = System.currentTimeMillis()
        if (now - lastUsageCheckAt < 10 * 60_000L) return
        lastUsageCheckAt = now
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val ops = getSystemService(AppOpsManager::class.java)
        if (ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) != AppOpsManager.MODE_ALLOWED) return
        val day = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        val stats = getSystemService(UsageStatsManager::class.java).queryUsageStats(UsageStatsManager.INTERVAL_DAILY, day, now)
        val total = stats.sumOf { it.totalTimeInForeground }
        if (total >= 3 * 60 * 60_000L && prefs.getLong("warned_day", 0L) != day) {
            prefs.edit().putLong("warned_day", day).apply()
            animate(WordSiegeMascotAction.NUZZLE)
            showBubble("Biraz ekrandan uzaklaşıp dinlenelim mi? 💜")
        }
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
                    val action = listOf(WordSiegeMascotAction.SPACE_FLIGHT, WordSiegeMascotAction.CHEER,
                        WordSiegeMascotAction.NUZZLE, WordSiegeMascotAction.LOVE, WordSiegeMascotAction.BOUNCE,
                        WordSiegeMascotAction.WINK, WordSiegeMascotAction.KISS).random()
                    animate(action)
                    showBubble(phrases[phraseIndex++ % phrases.size])
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
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(27, 37, 59))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.rgb(110, 215, 192))
            }
            elevation = dp(10).toFloat()
        }
        val title = TextView(this).apply {
            text = "Obi  ·  ×"
            setTextColor(Color.WHITE)
            textSize = 18f
            setOnClickListener { hidePanel() }
        }
        val message = TextView(this).apply {
            text = "Merhaba! Çift dokunarak yazışabiliriz. Örnek: ‘Yarın saat 09.00 alarm kur’ veya ‘Doğum günüm 12 Mayıs’."
            setTextColor(Color.rgb(218, 248, 242))
            textSize = 14f
            setPadding(0, dp(8), 0, dp(8))
        }
        val input = EditText(this).apply {
            hint = "Obi'ye yaz…"
            setSingleLine(false)
            maxLines = 2
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            textSize = 15f
        }
        val send = Button(this).apply {
            text = "Gönder"
            setOnClickListener {
                val command = input.text.toString().trim()
                if (command.isNotEmpty()) {
                    if (!translate(command, message)) message.text = store.handle(command)
                    input.text.clear()
                    animate(WordSiegeMascotAction.CHEER)
                }
            }
        }
        card.addView(title)
        card.addView(message)
        card.addView(input)
        card.addView(send)
        card.addView(Button(this).apply {
            text = "AFAD son depremler"
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://deprem.afad.gov.tr/last-earthquakes.html")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        card.addView(Button(this).apply {
            text = "Ekran süresi uyarısını aç"
            setOnClickListener {
                hidePanel()
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        card.addView(Button(this).apply {
            text = "Hatırlatıcı bildirim izni"
            setOnClickListener {
                hidePanel()
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        val params = WindowManager.LayoutParams(
            (width - dp(32)).coerceAtMost(dp(380)), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(70)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            windows.addView(card, params)
            panel = card
            input.requestFocus()
        } catch (_: SecurityException) {
            // The user may have revoked the overlay permission while Obi was open.
        }
    }

    private fun translate(command: String, result: TextView): Boolean {
        val english = Regex("(?i)^ingilizceye çevir[: ]+(.+)$").find(command)
        val turkish = Regex("(?i)^türkçeye çevir[: ]+(.+)$").find(command)
        val match = english ?: turkish ?: return false
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(if (english != null) TranslateLanguage.TURKISH else TranslateLanguage.ENGLISH)
            .setTargetLanguage(if (english != null) TranslateLanguage.ENGLISH else TranslateLanguage.TURKISH)
            .build()
        val translator = Translation.getClient(options)
        result.text = "Çeviri modeli hazırlanıyor. İlk kullanımda Wi-Fi ve yaklaşık 30 MB indirme gerekebilir…"
        translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener {
                translator.translate(match.groupValues[1])
                    .addOnSuccessListener { result.text = it; translator.close() }
                    .addOnFailureListener { result.text = "Çeviri yapılamadı. Yeniden dene."; translator.close() }
            }
            .addOnFailureListener { result.text = "Model indirilemedi. Wi-Fi bağlantısını kontrol et."; translator.close() }
        return true
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
