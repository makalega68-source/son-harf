package com.sonharf.game

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SonHarfBrandLogo(modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val remoteBytes = RemoteExperience.brandLogoBytes
    val remoteLogo = remember(remoteBytes) {
        runCatching {
            if (remoteBytes == null || remoteBytes.isEmpty()) null
            else BitmapFactory.decodeByteArray(remoteBytes, 0, remoteBytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    if (remoteLogo != null) {
        Image(
            bitmap = remoteLogo,
            contentDescription = "Son Harf Maskot",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF05255E), Color(0xFF0B4CB8))),
                    RoundedCornerShape(18.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.Text("♛", color = Color(0xFFFFC928), fontSize = (size.value * .18f).sp, fontWeight = FontWeight.Black)
                androidx.compose.material3.Text("SON", color = Color.White, fontSize = (size.value * .20f).sp, fontWeight = FontWeight.Black)
                androidx.compose.material3.Text("HARF", color = Color(0xFFFFB000), fontSize = (size.value * .18f).sp, fontWeight = FontWeight.Black)
                androidx.compose.material3.Text("MASKOT", color = Color.White, fontSize = (size.value * .08f).sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
