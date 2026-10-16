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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    private var status by mutableStateOf("Obi'yi ekranda göstermek için bir kez izin vermen gerekiyor.")

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
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0B1020)) {
                    Column(
                        Modifier.fillMaxSize().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Obi", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        Text(status, modifier = Modifier.padding(top = 16.dp), color = Color(0xFFDAF8F2), fontSize = 17.sp)
                        Button(onClick = {
                            if (Settings.canDrawOverlays(this@ObiEntryActivity)) startMascot() else openPermissionSettings()
                        }, modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
                            Text(if (Settings.canDrawOverlays(this@ObiEntryActivity)) "Obi'yi göster" else "Ekran üstü iznini aç")
                        }
                        Text(
                            "Ayarlar'da Obi'yi seçip ‘Diğer uygulamaların üzerinde göster’ iznini aç. Geri dönünce Obi ana ekranda belirecek.",
                            modifier = Modifier.padding(top = 14.dp), color = Color(0xFFAFB9D4), fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startMascot()
        } else if (!openedPermissionScreen) {
            // Let the launcher draw first; then Android shows the required system permission page.
            window.decorView.post { if (!isFinishing && !openedPermissionScreen) openPermissionSettings() }
        } else {
            status = "İzin kapalı. Obi'nin ekranda görünmesi için Ayarlar'da Obi'ye izin ver."
        }
    }

    private fun openPermissionSettings() {
        openedPermissionScreen = true
        status = "Ayarlar'da Obi'yi seçip ekran üstü iznini aç."
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
