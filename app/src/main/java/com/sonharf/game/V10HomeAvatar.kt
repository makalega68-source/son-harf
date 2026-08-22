package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun V10HomeAvatar(url: String?, name: String, size: Int) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFD9EEE8)),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank() && !failed) {
            AsyncImage(
                model = url,
                contentDescription = "$name profil fotoğrafı",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                onError = { failed = true },
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color(0xFF126A6A),
                fontWeight = FontWeight.Black,
                fontSize = (size / 2.2).sp,
            )
        }
    }
}
