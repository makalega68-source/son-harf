package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun SocialActivityScreen(backend: OnlineGameBackend, onBack: () -> Unit,
    onFriends: () -> Unit, onOpenGame: (WordSiegeGameDto) -> Unit, onOpenTarget: (String, String?) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MainScreenHeader(sh("Bildirimler", "Notifications"), "", onBack = onBack) }
        item { SocialInboxHistory(backend, onOpenTarget) }
    }
}

@Composable
private fun InviteActions(busy: Boolean, onDecline: () -> Unit, onAccept: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDecline, enabled = !busy) { Text(sh("REDDET", "DECLINE"), color = Hf.Red) }
        Button(onClick = onAccept, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Hf.Green, contentColor = androidx.compose.ui.graphics.Color.White)) { Text(sh("KABUL ET", "ACCEPT")) }
    }
}

@Composable
internal fun ActivityTile(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = LobbyPalette.Paper, border = BorderStroke(1.dp, LobbyPalette.Line)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = LobbyPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            content()
        }
    }
}
