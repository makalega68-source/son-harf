package com.sonharf.game

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MascotRuntimeSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedId = intent.getStringExtra("mascot_id") ?: MascotCatalog.CHIBI_WIZARD_ID
        val resolvedId = MascotCatalog.item(requestedId).id

        setContent {
            MaterialTheme {
                var motion by remember { mutableStateOf(MascotMotion.IDLE) }

                LaunchedEffect(resolvedId) {
                    val ready = MascotCatalog.isAssetReady(this@MascotRuntimeSmokeActivity, resolvedId)
                    Log.i("MascotSmoke", "MASCOT_ASSET_READY id=$resolvedId ready=$ready")
                    check(ready) { "Mascot asset is not ready: $resolvedId" }

                    if (resolvedId == MascotCatalog.CHIBI_WIZARD_ID) {
                        delay(3_000)
                        motion = MascotMotion.RUN
                        Log.i("MascotSmoke", "MASCOT_MOTION id=$resolvedId motion=RUN")
                        delay(3_000)
                        motion = MascotMotion.VICTORY
                        Log.i("MascotSmoke", "MASCOT_MOTION id=$resolvedId motion=VICTORY")
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF061A2C)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(width = 330.dp, height = 520.dp)) {
                            MascotLive3DStage(
                                modifier = Modifier.fillMaxSize(),
                                mascotId = resolvedId,
                                motion = motion,
                            )
                        }
                        Text(
                            text = resolvedId,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
