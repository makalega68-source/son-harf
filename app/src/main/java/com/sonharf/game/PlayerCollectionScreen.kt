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
    Column(Modifier.fillMaxSize().background(LobbyPalette.Ground)) {
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            MainScreenHeader(sh("Koleksiyonum", "My collection"),
                sh("Sahip olduklarını seç, tarzını yansıt", "Equip your favorites, make it yours"), onBack = onBack)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            item {
                ProfileOwnedThemesSection(backend)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
