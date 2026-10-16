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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ObiApp() }
    }
}

private data class ChatLine(val text: String, val fromObi: Boolean)

@Composable
private fun ObiApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ObiStore(context) }
    val chat = remember {
        mutableStateListOf(
            ChatLine("Merhaba Ümit. Ben Obi. Notlarını, yapılacaklarını ve hatırlatıcılarını bu telefonda tutarım.", true),
        )
    }
    var input by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF111827)) {
            Column(Modifier.fillMaxSize().padding(top = 34.dp)) {
                ObiHeader(onNotificationRequest = {
                    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                })
                QuickActions(
                    onAction = { command ->
                        val result = store.handle(command)
                        chat += ChatLine(command, false)
                        chat += ChatLine(result, true)
                    },
                )
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(chat) { line -> ChatBubble(line) }
                    item { NotesCard(store) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Obi'ye yaz…") },
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val command = input.trim()
                            if (command.isNotBlank()) {
                                chat += ChatLine(command, false)
                                chat += ChatLine(store.handle(command), true)
                                input = ""
                            }
                        },
                        modifier = Modifier.size(52.dp),
                    ) { Icon(Icons.Rounded.Send, "Gönder") }
                }
            }
        }
    }
}

@Composable
private fun ObiHeader(onNotificationRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(CircleShape)
                .background(Color(0xFF23B69A)).border(3.dp, Color(0xFFB7FFEF), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("OBI", color = Color(0xFF062D28), fontWeight = FontWeight.Black, fontSize = 14.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Obi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("Yerel kişisel asistan • Ses yok • Bulut yok", color = Color(0xFFB8C3D5), fontSize = 12.sp)
        }
        FilledIconButton(onClick = onNotificationRequest) {
            Icon(Icons.Rounded.NotificationsActive, "Bildirim izni")
        }
    }
}

@Composable
private fun QuickActions(onAction: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickButton("Not al") { onAction("Not al ") }
        QuickButton("Notlarım") { onAction("Notlarımı göster") }
        QuickButton("20 dk") { onAction("20 dakika zamanlayıcı başlat") }
    }
}

@Composable
private fun QuickButton(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = Color(0xFF1F2937)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = Color(0xFFDAF8F2), fontSize = 12.sp)
    }
}

@Composable
private fun ChatBubble(line: ChatLine) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromObi) Arrangement.Start else Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (line.fromObi) Color(0xFF1F2937) else Color(0xFF147D6A),
        ) { Text(line.text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Color.White, fontSize = 14.sp) }
    }
}

@Composable
private fun NotesCard(store: ObiStore) {
    val notes = store.notes()
    if (notes.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF172233)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AddTask, null, tint = Color(0xFF82EEDB))
                Spacer(Modifier.width(8.dp))
                Text("Obi notları", color = Color.White, fontWeight = FontWeight.Bold)
            }
            notes.takeLast(5).reversed().forEach { note ->
                Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("• $note", modifier = Modifier.weight(1f), color = Color(0xFFD7E1EE), fontSize = 13.sp)
                    Icon(Icons.Rounded.DeleteOutline, "Sil", tint = Color(0xFFB8C3D5), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private class ObiStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)

    fun handle(raw: String): String {
        val text = raw.trim()
        val normalized = text.lowercase(Locale("tr", "TR"))
        if (normalized.contains("notlarımı göster") || normalized.contains("notlarimi göster")) {
            return notes().takeLast(5).reversed().mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n").ifBlank { "Henüz notun yok. “Not al …” diyebilirsin." }
        }
        noteText(normalized, text)?.let { note ->
            save(note)
            return "Tamam, notlarına ekledim: “$note”"
        }
        timerMinutes(normalized)?.let { minutes ->
            schedule(System.currentTimeMillis() + minutes * 60_000L, "Obi zamanlayıcısı")
            return "Tamam. $minutes dakika sonra sana haber vereceğim."
        }
        if (normalized.contains("hatırlat") || normalized.contains("hatirlat")) {
            val time = reminderTime(normalized)
            if (time == null) return "Hatırlatıcı için saat de yaz: örneğin “yarın saat 09.00'da annemi ara diye hatırlat”."
            val note = text.replace(Regex("(?i)(yarın|yarin)?\\s*saat\\s*\\d{1,2}(?:[:.]\\d{2})?.*?(diye )?hatırlat(ır mısın|ir misin)?"), "").trim().ifBlank { "Hatırlatıcı" }
            schedule(time, note)
            return "Tamam, ${formatClock(time)} için “$note” hatırlatıcısını kurdum."
        }
        return "Şimdilik yerel moddayım. “Not al …”, “Notlarımı göster”, “20 dakika zamanlayıcı başlat” veya “Yarın saat 09.00'da … diye hatırlat” yazabilirsin."
    }

    fun notes(): List<String> = runCatching {
        val values = JSONArray(prefs.getString("notes", "[]") ?: "[]")
        List(values.length()) { values.getString(it) }
    }.getOrDefault(emptyList())

    private fun save(note: String) {
        val values = JSONArray(prefs.getString("notes", "[]") ?: "[]")
        values.put(note)
        val trimmed = JSONArray()
        val start = (values.length() - 50).coerceAtLeast(0)
        for (index in start until values.length()) trimmed.put(values.getString(index))
        prefs.edit().putString("notes", trimmed.toString()).apply()
    }

    private fun noteText(normalized: String, original: String): String? {
        val prefix = listOf("not al", "bunu not et", "şunu not et", "not et").firstOrNull { normalized.startsWith(it) } ?: return null
        return original.drop(prefix.length).trim(' ', ':', '-', '“', '”').takeIf { it.isNotBlank() }
    }

    private fun timerMinutes(text: String): Int? {
        if (!text.contains("zamanlayıcı") && !text.contains("zamanlayici") && !text.contains("timer")) return null
        val match = Regex("(\\d{1,3})\\s*(dakika|dk|saat)").find(text) ?: return null
        val number = match.groupValues[1].toIntOrNull() ?: return null
        return (if (match.groupValues[2] == "saat") number * 60 else number).takeIf { it in 1..1440 }
    }

    private fun reminderTime(text: String): Long? {
        val match = Regex("saat\\s*(\\d{1,2})(?:[:.]?(\\d{2}))?").find(text) ?: return null
        val hour = match.groupValues[1].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = match.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: 0
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (text.contains("yarın") || text.contains("yarin") || timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    private fun formatClock(time: Long): String = Calendar.getInstance().run {
        timeInMillis = time
        String.format(Locale("tr", "TR"), "%02d:%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))
    }

    private fun schedule(time: Long, text: String) {
        val request = (System.nanoTime() and 0x7fff_ffffL).toInt()
        val intent = Intent(context, ObiReminderReceiver::class.java).putExtra("text", text).setAction("com.obi.local.REMINDER.$request")
        val pending = PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }
}

class ObiReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = "obi_reminders"
        manager.createNotificationChannel(NotificationChannel(channel, "Obi hatırlatıcıları", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 4, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = intent.getStringExtra("text") ?: "Hatırlatıcı zamanı geldi."
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Obi hatırlatıyor")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() and 0x7fff_ffffL).toInt(), notification)
    }
}
