package com.obi.local

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Carries out [PhoneCommand]s on the phone. Each call returns Obi's reply, or null when the
 * command could not be matched (e.g. no app with that name) so the chat can try something else.
 * [beforeLaunch] closes Obi's chat window so the opened app is not hidden behind it.
 */
internal class ObiPhone(private val context: Context, private val beforeLaunch: () -> Unit) {
    private val cameras = context.getSystemService(CameraManager::class.java)
    private var torchOn = false
    private val torchWatch = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if (cameraId == torchId) torchOn = enabled }
    }
    private val torchId: String? by lazy {
        runCatching {
            cameras?.cameraIdList?.firstOrNull { id ->
                val c = cameras.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }

    init { runCatching { cameras?.registerTorchCallback(torchWatch, null) } }

    fun release() { runCatching { cameras?.unregisterTorchCallback(torchWatch) } }

    fun run(command: PhoneCommand): String? = when (command) {
        is PhoneCommand.OpenApp -> openApp(command.name)
        is PhoneCommand.AppSearch -> appSearch(command.app, command.query)
        is PhoneCommand.WebSearch -> webSearch(command.query)
        is PhoneCommand.PlayMusic -> playMusic(command.query)
        is PhoneCommand.Camera -> camera(command.video, command.selfie)
        is PhoneCommand.Torch -> torch(command.on)
        is PhoneCommand.Brightness -> brightness(command)
        is PhoneCommand.Volume -> volume(command)
        is PhoneCommand.SettingsPage -> settings(command.page, command.wantsToggle)
        is PhoneCommand.Global -> global(command.action)
        PhoneCommand.Home -> if (launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))) "Ana ekrana döndüm 🏠" else null
    }

    private fun launch(intent: Intent): Boolean {
        beforeLaunch()
        return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    }

    // ---- Apps --------------------------------------------------------------------------------------

    private fun installedApps(): List<Pair<String, String>> = runCatching {
        val pm = context.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
    }.getOrDefault(emptyList())

    private fun openApp(name: String): String? = when (val target = ObiPhoneParse.resolve(name)) {
        is AppTarget.Category -> openCategory(target.category)?.let { "${target.label} açıldı ✓" }
        is AppTarget.Named -> {
            val apps = installedApps()
            val pkg = ObiPhoneParse.bestMatch(target.term, apps)
            val intent = pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
            if (intent != null && launch(intent)) "${apps.firstOrNull { it.second == pkg }?.first ?: target.term} açıldı ✓" else null
        }
    }

    private fun openCategory(category: String): Boolean? {
        val intent = when (category) {
            "dialer" -> Intent(Intent.ACTION_DIAL)
            "clock" -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            else -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, when (category) {
                "gallery" -> Intent.CATEGORY_APP_GALLERY
                "music" -> Intent.CATEGORY_APP_MUSIC
                "calendar" -> Intent.CATEGORY_APP_CALENDAR
                "calculator" -> Intent.CATEGORY_APP_CALCULATOR
                "contacts" -> Intent.CATEGORY_APP_CONTACTS
                "messaging" -> Intent.CATEGORY_APP_MESSAGING
                "maps" -> Intent.CATEGORY_APP_MAPS
                "browser" -> Intent.CATEGORY_APP_BROWSER
                else -> Intent.CATEGORY_APP_EMAIL
            })
        }
        return launch(intent).takeIf { it }
    }

    private fun appSearch(app: String, query: String): String {
        val encoded = Uri.encode(query)
        val done = when (app) {
            "spotify" -> launch(Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).setPackage("com.spotify.music").putExtra(SearchManager.QUERY, query)) ||
                launch(Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$encoded")))
            "maps" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")))
            else -> launch(Intent(Intent.ACTION_SEARCH).setPackage("com.google.android.youtube").putExtra(SearchManager.QUERY, query)) ||
                launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")))
        }
        val where = when (app) { "spotify" -> "Spotify'da"; "maps" -> "Haritalar'da"; else -> "YouTube'da" }
        return if (done) "$where ‘$query’ için açtım ✓" else "$where arayamadım; uygulama yüklü mü?"
    }

    private fun webSearch(query: String): String {
        val done = launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)) ||
            launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
        return if (done) "‘$query’ için aradım 🔎" else "Arama açılamadı."
    }

    private fun playMusic(query: String): String {
        val done = launch(Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).putExtra(SearchManager.QUERY, query)
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")) ||
            launch(Intent(Intent.ACTION_SEARCH).setPackage("com.google.android.youtube").putExtra(SearchManager.QUERY, query))
        return if (done) "‘$query’ çalıyorum ♪" else "Müzik uygulaması bulamadım."
    }

    // ---- Camera, torch, screen, sound ----------------------------------------------------------------

    private fun camera(video: Boolean, selfie: Boolean): String {
        val intent = Intent(if (video) MediaStore.INTENT_ACTION_VIDEO_CAMERA else MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        if (selfie) intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        return if (launch(intent)) (if (video) "Kamera video modunda açıldı 🎥" else if (selfie) "Ön kamera açıldı, gülümse! 📸" else "Kamera açıldı 📸")
        else "Kamera açılamadı."
    }

    private fun torch(on: Boolean?): String {
        val id = torchId ?: return "Bu telefonda fener bulamadım 🔦"
        val next = on ?: !torchOn
        return runCatching { cameras?.setTorchMode(id, next); torchOn = next; if (next) "Feneri açtım 🔦" else "Feneri kapattım." }
            .getOrDefault("Fener şu an kullanılamıyor (kamera açık olabilir).")
    }

    private fun brightness(command: PhoneCommand.Brightness): String {
        if (!Settings.System.canWrite(context)) {
            launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
            return "Ekran ışığını ayarlayabilmem için ‘Sistem ayarlarını değiştirme’ iznini açman gerekiyor. Açtıktan sonra tekrar söyle ☀️"
        }
        val resolver = context.contentResolver
        if (command.auto) {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
            return "Otomatik parlaklığı açtım ☀️"
        }
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        val current = runCatching { Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) }.getOrDefault(128)
        val next = when {
            command.percent != null -> (command.percent * 255 / 100).coerceIn(2, 255)
            command.step < 0 -> (current / 2).coerceAtLeast(2)
            else -> (current * 2).coerceIn(24, 255)
        }
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, next)
        val percent = (next * 100 / 255).coerceAtLeast(1)
        return if (next < current) "Ekran ışığını kıstım (%$percent) 🌙" else "Ekran ışığını açtım (%$percent) ☀️"
    }

    private fun volume(command: PhoneCommand.Volume): String {
        val audio = context.getSystemService(AudioManager::class.java) ?: return "Ses ayarına ulaşamadım."
        command.ringer?.let { mode ->
            return runCatching {
                audio.ringerMode = if (mode == "vibrate") AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
                if (mode == "vibrate") "Telefonu titreşime aldım 📳" else "Zil sesini açtım 🔔"
            }.getOrElse {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (nm?.isNotificationPolicyAccessGranted == false) launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                "Sessiz modu değiştirmem için ‘Rahatsız Etmeyin erişimi’ izni gerekiyor; açılan sayfadan Obi'ye izin verebilirsin."
            }
        }
        val stream = AudioManager.STREAM_MUSIC
        val max = audio.getStreamMaxVolume(stream)
        when (command.step) {
            2 -> audio.setStreamVolume(stream, max, AudioManager.FLAG_SHOW_UI)
            0 -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            else -> {
                if (command.step > 0) audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                val step = (max / 5).coerceAtLeast(1) * command.step
                audio.setStreamVolume(stream, (audio.getStreamVolume(stream) + step).coerceIn(0, max), AudioManager.FLAG_SHOW_UI)
            }
        }
        val now = audio.getStreamVolume(stream) * 100 / max.coerceAtLeast(1)
        return when (command.step) {
            0 -> "Sesi kapattım 🔇"
            2 -> "Ses sonuna kadar açık 🔊"
            else -> "Ses: %$now ${if (command.step > 0) "🔊" else "🔉"}"
        }
    }

    // ---- Settings pages --------------------------------------------------------------------------------

    private fun settings(page: String, wantsToggle: Boolean): String {
        val panel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val (action, name) = when (page) {
            "wifi" -> (if (panel) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS) to "Wi-Fi"
            "internet" -> (if (panel) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS) to "İnternet"
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth"
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS to "Konum"
            "airplane" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS to "Uçak modu"
            "dnd" -> "android.settings.ZEN_MODE_SETTINGS" to "Rahatsız Etmeyin"
            "sound" -> (if (panel) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS) to "Ses"
            "display" -> Settings.ACTION_DISPLAY_SETTINGS to "Ekran"
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS to "Pil"
            "nfc" -> Settings.ACTION_NFC_SETTINGS to "NFC"
            "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS to "Depolama"
            else -> Settings.ACTION_SETTINGS to "Ayarlar"
        }
        if (!launch(Intent(action)) && !launch(Intent(Settings.ACTION_SETTINGS))) return "Ayarlar açılamadı."
        return when {
            page == "main" -> "Ayarları açtım ⚙️"
            wantsToggle -> "$name ayarını açtım; tek dokunuşla değiştirebilirsin. (Android, uygulamaların bunu kendi başına açıp kapatmasına izin vermiyor.)"
            else -> "$name ayarlarını açtım ⚙️"
        }
    }

    // ---- Lock screen, screenshot… (Obi's accessibility helper) ------------------------------------------

    private fun global(action: String): String {
        val helper = ObiAccessibilityService.instance
        if (helper == null) {
            launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return "Bunu yapabilmem için Erişilebilirlik ayarlarında ‘Obi telefon yardımcısı’nı açman gerekiyor (bir kez). " +
                "Açtıktan sonra ‘ekranı kilitle’ dediğinde kilitlerim 🔒"
        }
        val code = when (action) {
            "lock" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN else null
            "screenshot" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT else null
            "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            else -> AccessibilityService.GLOBAL_ACTION_RECENTS
        } ?: return "Bu Android sürümünde bunu yapamıyorum."
        beforeLaunch()
        // A short pause so Obi's chat window is gone before the screen is locked or captured.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ helper.performGlobalAction(code) }, 350L)
        return when (action) {
            "lock" -> "Ekranı kilitliyorum 🔒"
            "screenshot" -> "Ekran görüntüsü alıyorum 📸"
            "notifications" -> "Bildirimleri açtım."
            else -> "Son uygulamalar."
        }
    }
}

/**
 * Obi's phone helper: only performs the system actions the user asks for by voice or chat
 * (lock screen, screenshot, notifications). It does not read the screen or any app's content.
 */
class ObiAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: ObiAccessibilityService? = null
    }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }
}
