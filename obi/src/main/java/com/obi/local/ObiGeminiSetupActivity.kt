package com.obi.local

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

/** Regular Android screen for credential entry; the floating mascot stays hidden until this closes. */
class ObiGeminiSetupActivity : Activity() {
    private val gemini by lazy { ObiGemini(this) }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).roundToInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(17, 24, 39)
        window.navigationBarColor = Color.rgb(17, 24, 39)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(32))
            setBackgroundColor(Color.rgb(17, 24, 39))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(body) }
        setContentView(scroll)

        val close = TextView(this).apply {
            text = "←  Obi'ye dön"; textSize = 16f; setTextColor(Color.rgb(154, 223, 211))
            setPadding(0, dp(12), 0, dp(20)); setOnClickListener { finish() }
        }
        body.addView(close)
        val title = TextView(this).apply {
            text = "✦  Gemini bağlantısı"; textSize = 26f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        body.addView(title)
        val guide = TextView(this).apply {
            text = "1. ‘API anahtarı al’ düğmesine bas. Google AI Studio'da hesabınla giriş yap.\n\n2. ‘Create API key’ seçeneğiyle anahtar oluştur ve kopyala. Anahtarı kimseyle paylaşma.\n\n3. Obi'ye geri dön, ‘Panodan yapıştır’ ve ‘Bağlantıyı kaydet’ düğmelerine bas.\n\n4. Ekrandaki Obi'ye çift dokun, ‘Sohbet’i aç ve örneğin ‘Bunu İngilizceye çevir: Merhaba’ yaz.\n\nBağlandıktan sonra kurulum menüden kalkar. Anahtar bu telefonda şifreli saklanır; Gemini yanıtları için yazdıkların Google'a gönderilir."
            textSize = 15f; setTextColor(Color.rgb(205, 219, 233))
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        body.addView(guide, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        val status = TextView(this).apply {
            text = if (gemini.connected()) "Anahtar kayıtlı. Obi'ye dönüp yazışabilirsin." else "Bağlantı için API anahtarı bekleniyor."
            textSize = 14f; setTextColor(Color.rgb(152, 222, 209))
        }
        body.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })
        val keyField = EditText(this).apply {
            hint = "API anahtarı"; textSize = 16f
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(177, 190, 211))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true); setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.rgb(33, 47, 69)); cornerRadius = dp(15).toFloat()
                setStroke(dp(1), Color.rgb(90, 183, 169))
            }
        }
        body.addView(keyField, LinearLayout.LayoutParams(-1, dp(55)).apply { topMargin = dp(16) })

        fun action(label: String, accent: Boolean, clicked: () -> Unit): TextView = TextView(this).apply {
            text = label; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setOnClickListener { clicked() }
            background = GradientDrawable().apply {
                setColor(if (accent) Color.rgb(101, 74, 168) else Color.rgb(42, 61, 80))
                cornerRadius = dp(17).toFloat()
            }
        }
        body.addView(action("API anahtarı al ↗", false) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys")))
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(22) })
        body.addView(action("Panodan yapıştır", false) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val value = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
            if (value.isEmpty()) status.text = "Panoda metin yok. AI Studio'da anahtarı kopyalayıp geri dön."
            else { keyField.setText(value); keyField.setSelection(value.length); status.text = "Anahtar alındı. Kaydet'e bas." }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        body.addView(action("Bağlantıyı kaydet", true) {
            if (gemini.saveKey(keyField.text.toString())) {
                keyField.text.clear()
                status.text = "Anahtar kaydedildi. Obi'ye dönüp bir mesaj yazarak bağlantıyı dene."
                finish()
            } else status.text = "Anahtar kaydedilemedi. AI Studio'daki API anahtarını kontrol et."
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
        if (gemini.connected()) body.addView(action("Kayıtlı anahtarı kaldır", false) {
            gemini.disconnect(); status.text = "Anahtar kaldırıldı. Obi yerel modda kalacak."
        }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(18) })
        // Don't open the keyboard until the user taps the field or the clipboard action.
        body.isFocusableInTouchMode = true
        body.requestFocus()
    }

    override fun onDestroy() {
        if (isFinishing) sendBroadcast(Intent(ObiOverlayService.ACTION_SHOW).setPackage(packageName))
        super.onDestroy()
    }
}
