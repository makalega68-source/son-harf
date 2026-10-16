package com.obi.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The launcher is only a one-time permission handoff. Obi itself lives on the phone screen. */
class ObiEntryActivity : ComponentActivity() {
    private var openedPermissionScreen = false
    private var waitingForMascot = false
    private var status by mutableStateOf("Obi'yi ana ekranda göstermek için aşağıdaki adımları izle.")
    private var showPrivacy by mutableStateOf(false)
    private var askingNotifications = false
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        askingNotifications = false
        if (Settings.canDrawOverlays(this)) startMascot()
    }

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ObiOverlayService.ACTION_READY) {
                waitingForMascot = false
                showPhoneHome()
            } else if (intent.action == ObiOverlayService.ACTION_ERROR) {
                waitingForMascot = false
                status = "Obi ekrana yerleşemedi. İzni kontrol edip tekrar dene."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter().apply {
            addAction(ObiOverlayService.ACTION_READY)
            addAction(ObiOverlayService.ACTION_ERROR)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(resultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(resultReceiver, filter)
        }
        // Reminders, alarms and the morning message are notifications; ask once on Android 13+.
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askingNotifications = runCatching { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }.isSuccess
        }
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0B1020)) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Obi", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        Text(status, modifier = Modifier.padding(top = 16.dp), color = Color(0xFFDAF8F2), fontSize = 17.sp)
                        Text(
                            "1. Aşağıdaki düğmeye bas.\n2. Açılan ‘Diğer uygulamaların üzerinde göster’ listesinde gerekirse ‘Daha fazla’ya bas.\n3. Listeden Obi’yi bul ve üzerine dokun. Ana ekran uygulamasını seçme.\n4. ‘Diğer uygulamaların üzerinde göster’ anahtarını aç.\n5. Geri okuna basarak Obi’ye dön. Obi ana ekranda görünecek.",
                            modifier = Modifier.padding(top = 18.dp), color = Color.White, fontSize = 16.sp, lineHeight = 25.sp,
                        )
                        Button(onClick = {
                            if (Settings.canDrawOverlays(this@ObiEntryActivity)) startMascot() else openPermissionSettings()
                        }, modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
                            Text(if (Settings.canDrawOverlays(this@ObiEntryActivity)) "Obi'yi göster" else "İzin ayarlarını aç")
                        }
                        Text(
                            "İzin verdikten sonra Ayarlar'dan geri dön. Obi görünmezse uygulama simgesine tekrar dokun.",
                            modifier = Modifier.padding(top = 14.dp), color = Color(0xFFAFB9D4), fontSize = 13.sp,
                        )
                        Text(
                            "Neden bu izin? Obi, diğer uygulamaların üzerinde küçük bir arkadaş olarak durabilmek için ‘üstte gösterme’ iznine ihtiyaç duyar. Ekranını okumaz, dokunduğun başka hiçbir şeyi görmez.",
                            modifier = Modifier.padding(top = 18.dp), color = Color(0xFFDAF8F2), fontSize = 13.sp, lineHeight = 19.sp,
                        )
                        Text(
                            if (showPrivacy) "Gizlilik  ▲" else "Gizlilik  ▼",
                            modifier = Modifier.padding(top = 16.dp).clickable { showPrivacy = !showPrivacy },
                            color = Color(0xFF8FECDC), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        )
                        if (showPrivacy) Text(
                            ObiPrivacy.TEXT,
                            modifier = Modifier.padding(top = 8.dp), color = Color(0xFFAFB9D4), fontSize = 12.sp, lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (askingNotifications) return
        if (Settings.canDrawOverlays(this)) {
            startMascot()
        } else if (openedPermissionScreen) {
            status = "İzin hâlâ kapalı. Listede Obi'yi seçip anahtarı aç, sonra geri dön."
        }
    }

    private fun openPermissionSettings() {
        openedPermissionScreen = true
        status = "Listede ‘Daha fazla’ → Obi → ‘Diğer uygulamaların üzerinde göster’ anahtarını aç; sonra geri dön."
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        try {
            startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun startMascot() {
        if (waitingForMascot) return
        waitingForMascot = true
        status = "Obi ekrana geliyor…"
        try {
            startForegroundService(Intent(this, ObiOverlayService::class.java).setAction(ObiOverlayService.ACTION_START))
        } catch (_: RuntimeException) {
            waitingForMascot = false
            status = "Obi başlatılamadı. Tekrar dene."
        }
    }

    private fun showPhoneHome() {
        try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: RuntimeException) {
            moveTaskToBack(true)
        }
        finish()
    }

    override fun onDestroy() {
        unregisterReceiver(resultReceiver)
        super.onDestroy()
    }
}
