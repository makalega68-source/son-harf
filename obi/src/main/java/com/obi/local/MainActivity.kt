package com.obi.local

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    private var overlayRunning by mutableStateOf(false)
    private var pendingOverlayStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObiApp(overlayRunning = overlayRunning, onOverlayToggle = { toggleOverlay() })
        }
    }

    override fun onResume() {
        super.onResume()
        if (pendingOverlayStart && Settings.canDrawOverlays(this)) {
            pendingOverlayStart = false
            startOverlay()
            return
        }
        overlayRunning = ObiOverlayService.isRunning(this)
    }

    private fun toggleOverlay() {
        if (overlayRunning) {
            stopService(Intent(this, ObiOverlayService::class.java))
            overlayRunning = false
        } else if (Settings.canDrawOverlays(this)) {
            startOverlay()
        } else {
            pendingOverlayStart = true
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            try { startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun startOverlay() {
        startForegroundService(Intent(this, ObiOverlayService::class.java).setAction(ObiOverlayService.ACTION_START))
        overlayRunning = true
    }
}

@Composable
private fun ObiApp(overlayRunning: Boolean, onOverlayToggle: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ObiStore(context) }
    var input by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("Ben buradayım Ümit. Bana dokun; birlikte başlayalım.") }
    var actionKey by remember { mutableStateOf(1L) }
    var action by remember { mutableStateOf<WordSiegeMascotAction?>(WordSiegeMascotAction.WAVE) }
    var emotion by remember { mutableStateOf(WordSiegeMascotEmotion.HAPPY) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun respond(command: String) {
        if (command.isBlank()) return
        reply = store.handle(command); input = ""
        emotion = WordSiegeMascotEmotion.SPEAKING
        action = WordSiegeMascotAction.NOD; actionKey++
    }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF0B1020)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Obi", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("Kişisel yol arkadaşın  •  Ses yok", color = Color(0xFFAFB9D4), fontSize = 13.sp)
                    }
                    FilledIconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Icon(Icons.Rounded.NotificationsActive, "Hatırlatıcı bildirimleri") }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOverlayToggle, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(if (overlayRunning) "Obi'yi ekrandan kaldır" else "Obi'yi telefonda gezdir", fontSize = 16.sp)
                }
                Text(
                    if (overlayRunning) "Obi diğer uygulamaların üzerinde. Sürükleyerek taşı; dokununca yazı alanı açılır."
                    else "Bir kez ekran üstü izni ver. Obi, uygulamadan çıksan da ekranda kalır; bildirimden kapatabilirsin.",
                    color = Color(0xFFAFB9D4), fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("Dokun bana", color = Color(0xFF8FECDC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WordSiegeMascot(
                        moveId = null, lastMoveMine = false, pendingCells = emptyList<Int>(), playerTurn = true,
                        requestedEmotion = emotion, action = action, actionKey = actionKey,
                        speaking = emotion == WordSiegeMascotEmotion.SPEAKING, modifier = Modifier.size(310.dp),
                        onTap = {
                            reply = listOf("Buradayım! Ne yapalım?", "Seni dinliyorum.", "Bir not mu bırakalım?", "Bugün için bir şey planlayalım mı?").random()
                            emotion = WordSiegeMascotEmotion.HAPPY; action = WordSiegeMascotAction.WAVE; actionKey++
                        },
                    )
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(reply, Modifier.padding(18.dp), color = Color.White, fontSize = 16.sp, lineHeight = 23.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ObiAction("Not al") { input = "Not al " }
                    ObiAction("Notlarım") { respond("Notlarımı göster") }
                    ObiAction("20 dk") { respond("20 dakika zamanlayıcı başlat") }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Obi'ye yaz…") }, maxLines = 2, shape = RoundedCornerShape(22.dp))
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = { respond(input) }, modifier = Modifier.size(54.dp)) { Icon(Icons.Rounded.Send, "Gönder") }
                }
            }
        }
    }
}

@Composable
private fun ObiAction(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color(0xFF17233A), modifier = Modifier.height(46.dp)) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text(label, color = Color(0xFFD8FFF7), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}

internal class ObiStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)
    fun handle(raw: String): String {
        val text = raw.trim(); val normalized = text.lowercase(Locale("tr", "TR"))
        if (normalized.contains("doğum günüm") || normalized.contains("dogum gunum")) {
            val date = Regex("(\\d{1,2})\\s*(ocak|şubat|mart|nisan|mayıs|haziran|temmuz|ağustos|eylül|ekim|kasım|aralık)").find(normalized)
                ?: return "Doğum gününü ‘Doğum günüm 12 Mayıs’ diye yazabilirsin."
            val months = listOf("ocak", "şubat", "mart", "nisan", "mayıs", "haziran", "temmuz", "ağustos", "eylül", "ekim", "kasım", "aralık")
            val day = date.groupValues[1].toInt()
            val month = months.indexOf(date.groupValues[2]) + 1
            if (day !in 1..31) return "Günü 1 ile 31 arasında yazabilir misin?"
            prefs.edit().putInt("birth_day", day).putInt("birth_month", month).apply()
            scheduleDaily()
            return "Harika! $day ${date.groupValues[2].replaceFirstChar { it.uppercase() }} gününü kaydettim 🎂"
        }
        if (normalized.contains("günaydın mesajı") || normalized.contains("sabah motive et")) {
            prefs.edit().putBoolean("morning_enabled", true).apply()
            scheduleDaily()
            return "Tamam! Sabah 08.00 civarında sana güzel bir mesaj göndereceğim. Bildirim iznini açmayı unutma."
        }
        if (normalized.contains("sabah mesajını kapat")) {
            prefs.edit().putBoolean("morning_enabled", false).apply()
            return "Sabah mesajlarını kapattım."
        }
        if (normalized.contains("alarm kur")) {
            val time = reminderTime(normalized) ?: return "Örnek: ‘Yarın saat 09.00 alarm kur’."
            schedule(time, "Obi alarmı: Uyanma zamanı!", "alarm")
            return "Alarmı ${formatClock(time)} için kurdum. Android bildirimiyle haber vereceğim."
        }
        if (normalized.contains("deprem")) return "Deprem bilgisi için AFAD Son Depremler sayfasını aç. Obi henüz doğrulanmış acil uyarı göndermiyor; resmî uyarıların yerine geçmez."
        if (normalized.contains("çevir") || normalized.contains("cevir")) return "Çeviri için dil ve metni yazabilirsin; yerel çeviri modeli henüz eklenmedi."
        if (normalized.contains("notlarımı göster") || normalized.contains("notlarimi göster")) return notes().takeLast(5).reversed().mapIndexed { i, item -> "${i + 1}. $item" }.joinToString("\n").ifBlank { "Henüz notun yok. ‘Not al …’ yazabilirsin." }
        noteText(normalized, text)?.let { note -> save(note); return "Tamam, bunu hatırlayacağım: ‘$note’" }
        timerMinutes(normalized)?.let { minutes -> schedule(System.currentTimeMillis() + minutes * 60_000L, "Obi zamanlayıcısı"); return "Tamam. $minutes dakika sonra sana haber vereceğim." }
        if (normalized.contains("hatırlat") || normalized.contains("hatirlat")) {
            val time = reminderTime(normalized) ?: return "Saat de yaz: örneğin ‘yarın saat 09.00’da annemi ara diye hatırlat’."
            val note = text.replace(Regex("(?i)(yarın|yarin)?\\s*saat\\s*\\d{1,2}(?:[:.]\\d{2})?.*?(diye )?hatırlat(ır mısın|ir misin)?"), "").trim().ifBlank { "Hatırlatıcı" }
            schedule(time, note); return "Tamam, ${formatClock(time)} için ‘$note’ hatırlatıcısını kurdum."
        }
        return "‘Not al …’, ‘20 dakika zamanlayıcı başlat’, ‘Yarın saat 09.00 alarm kur’, ‘Doğum günüm 12 Mayıs’ veya ‘Sabah motive et’ yazabilirsin."
    }
    fun notes(): List<String> = runCatching { val v = JSONArray(prefs.getString("notes", "[]") ?: "[]"); List(v.length()) { v.getString(it) } }.getOrDefault(emptyList())
    private fun save(note: String) { val v = JSONArray(prefs.getString("notes", "[]") ?: "[]"); v.put(note); prefs.edit().putString("notes", v.toString()).apply() }
    private fun noteText(n: String, o: String): String? { val p = listOf("not al", "bunu not et", "şunu not et", "not et").firstOrNull { n.startsWith(it) } ?: return null; return o.drop(p.length).trim(' ', ':', '-', '“', '”').takeIf { it.isNotBlank() } }
    private fun timerMinutes(t: String): Int? { if (!t.contains("zamanlayıcı") && !t.contains("zamanlayici") && !t.contains("timer")) return null; val m = Regex("(\\d{1,3})\\s*(dakika|dk|saat)").find(t) ?: return null; val n = m.groupValues[1].toIntOrNull() ?: return null; return (if (m.groupValues[2] == "saat") n * 60 else n).takeIf { it in 1..1440 } }
    private fun reminderTime(t: String): Long? { val m = Regex("saat\\s*(\\d{1,2})(?:[:.]?(\\d{2}))?").find(t) ?: return null; val h = m.groupValues[1].toIntOrNull()?.takeIf { it in 0..23 } ?: return null; val min = m.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: 0; return Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (t.contains("yarın") || t.contains("yarin") || timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis }
    private fun formatClock(time: Long): String = Calendar.getInstance().run { timeInMillis = time; String.format(Locale("tr", "TR"), "%02d:%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE)) }
    private fun schedule(time: Long, text: String, kind: String = "reminder") { val id = (System.nanoTime() and 0x7fff_ffffL).toInt(); val intent = Intent(context, ObiReminderReceiver::class.java).putExtra("text", text).putExtra("kind", kind).setAction("com.obi.local.REMINDER.$id"); val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending) }
    fun scheduleDaily() {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val pending = PendingIntent.getBroadcast(context, 808, Intent(context, ObiReminderReceiver::class.java).setAction("com.obi.local.DAILY"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    }
}

class ObiReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.obi.local.DAILY") {
            val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)
            val today = Calendar.getInstance()
            val birthday = prefs.getInt("birth_day", 0) == today.get(Calendar.DAY_OF_MONTH) && prefs.getInt("birth_month", 0) == today.get(Calendar.MONTH) + 1
            val message = when {
                birthday -> "İyi ki doğdun! Bugün senin günün 🎂✨"
                prefs.getBoolean("morning_enabled", false) -> "Günaydın! Bugün küçük bir adım da büyük bir başlangıç 💜"
                else -> null
            }
            if (message != null) notify(context, message, "Obi günaydın diyor")
            if (message != null) context.sendBroadcast(Intent("com.obi.local.CELEBRATE").setPackage(context.packageName).putExtra("text", message))
            ObiStore(context).scheduleDaily()
            return
        }
        notify(context, intent.getStringExtra("text") ?: "Hatırlatıcı zamanı geldi.", if (intent.getStringExtra("kind") == "alarm") "Obi alarmı" else "Obi hatırlatıyor")
        context.sendBroadcast(Intent("com.obi.local.CELEBRATE").setPackage(context.packageName).putExtra("text", intent.getStringExtra("text") ?: "Hatırlatıcı zamanı geldi."))
    }

    private fun notify(context: Context, text: String, title: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return; val channel = "obi_reminders"
        manager.createNotificationChannel(NotificationChannel(channel, "Obi hatırlatıcıları", NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(context, 4, Intent(context, ObiEntryActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify((System.currentTimeMillis() and 0x7fff_ffffL).toInt(), Notification.Builder(context, channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build())
    }
}

class ObiBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ObiStore(context).scheduleDaily()
    }
}
