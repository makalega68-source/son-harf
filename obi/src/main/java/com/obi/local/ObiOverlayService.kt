package com.obi.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/** User-started, user-stoppable overlay. No accessibility or screen-reading permission is used. */
class ObiOverlayService : Service() {
    companion object {
        const val ACTION_START = "com.obi.local.START_OVERLAY"
        const val ACTION_STOP = "com.obi.local.STOP_OVERLAY"
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
    private val store by lazy { ObiStore(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotification()
        if (mascot == null) {
            try {
                addMascot()
            } catch (_: SecurityException) {
                stopSelf()
            } catch (_: WindowManager.BadTokenException) {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Obi ekranda", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, ObiOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle("Obi ekranında")
            .setContentText("Dokunarak yazış • Sürükleyerek taşı")
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
            x = prefs.getInt("x", (bounds.widthPixels - size).coerceAtLeast(0))
                .coerceIn(0, (bounds.widthPixels - size).coerceAtLeast(0))
            y = prefs.getInt("y", bounds.heightPixels / 3)
                .coerceIn(0, (bounds.heightPixels - size).coerceAtLeast(0))
        }
        val view = WordSiegeMascotView(this).apply {
            contentDescription = "Obi. Dokunarak yazış; basılı tutup sürükleyerek taşı."
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
                if (panel == null) showPanel() else hidePanel()
            }
        }
        windows.addView(view, params)
        mascot = view
        mascotParams = params
        prefs.edit().putBoolean("running", true).apply()
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
            text = "Merhaba! Ne yapmamı istersin?"
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
                    message.text = store.handle(command)
                    input.text.clear()
                    mascot?.updateGame(null, false, 0, 0, 0, null, emptyList(), true, WordSiegeMascotEmotion.HAPPY)
                }
            }
        }
        card.addView(title)
        card.addView(message)
        card.addView(input)
        card.addView(send)
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
        hidePanel()
        mascot?.let { runCatching { windows.removeView(it) } }
        mascot = null
        mascotParams = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
