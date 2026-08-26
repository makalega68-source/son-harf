package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * Debug-only HOME compositor smoke.
 * Current-head acceptance is intentionally triggered through this debug-only source.
 *
 * Hosts the exact production fixed companion in the sketch-inspired HOME position: Eve on the
 * left, Son Harf on the right and Bil Bakalım below. An opaque card deliberately extends behind
 * part of Eve's viewport so CI catches regressions where compact rendering falls behind Compose.
 * EveHomeFixedCompanion starts the real GLB living-behavior loop itself; separated screenshots
 * therefore verify visible skeletal motion as well as TextureSurface compositor stability.
 */
class EveHomeFloatingSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3FAFF)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-18).dp, y = 126.dp)
                            .size(width = 278.dp, height = 224.dp)
                            .background(Color(0xFF0E7490), RoundedCornerShape(30.dp)),
                    ) {
                        Text(
                            text = "SON HARF",
                            modifier = Modifier.align(Alignment.Center).padding(start = 24.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 372.dp)
                            .size(width = 330.dp, height = 190.dp)
                            .background(Color(0xFF4D7C0F), RoundedCornerShape(30.dp)),
                    ) {
                        Text(
                            text = "BİL BAKALIM",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 23.sp,
                        )
                    }

                    EveHomeFixedCompanion(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 18.dp, y = 116.dp)
                            .size(width = 120.dp, height = 244.dp)
                            .zIndex(20f),
                    )

                    Text(
                        text = "🐾  EVE EVİ",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-36).dp),
                        color = Color(0xFF163B58),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
