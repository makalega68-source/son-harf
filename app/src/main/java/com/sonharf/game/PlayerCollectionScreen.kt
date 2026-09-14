package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend

/** The profile is the only place where owned cosmetics and themes are equipped. */
@Composable
internal fun PlayerCollectionScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(SonHarfTheme.Background)) {
        androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = SonHarfTheme.TextPrimary) }
            Column(Modifier.padding(start = 6.dp)) {
                Text(sh("KOLEKSİYONUM", "MY COLLECTION"), color = SonHarfTheme.TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(sh("Alınan ürünleri burada kullan ve değiştir.", "Equip and change owned items here."), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            item {
                ProfileOwnedThemesSection(backend)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
