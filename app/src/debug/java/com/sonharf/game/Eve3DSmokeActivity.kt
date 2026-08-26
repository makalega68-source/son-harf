package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Debug-only visual smoke surface for CI/emulator verification of the production Eve3DStage.
 * The smoke screen intentionally contains no private renderer/model copy: CI exercises the same
 * production component used by the real app.
 */
class Eve3DSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EveMascotRuntime.calm()
        setContent {
            MaterialTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF073B32), Color(0xFF176B52), Color(0xFF0A4A3C)),
                            ),
                        ),
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            "PRODUCTION EVE 3D SMOKE",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            EveAssetPolicy.MODEL_ASSET,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = Color.White.copy(alpha = .72f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(620.dp)
                                .padding(top = 14.dp),
                            color = Color.White.copy(alpha = .08f),
                        ) {
                            Eve3DStage(Modifier.fillMaxSize())
                        }
                    }
                    Text(
                        "Production Eve3DStage · Real GLB · No 2D fallback",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp),
                        color = Color.White.copy(alpha = .65f),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
