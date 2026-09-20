package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend

/** The profile is the only place where owned cosmetics and themes are equipped. */
@Composable
internal fun PlayerCollectionScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MainUi.Background)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            MainScreenHeader(
                title = sh("Koleksiyonum", "My Collection"),
                subtitle = sh("Sahip olduğun kozmetik ve temaları buradan yönet.", "Manage the cosmetics and themes you own."),
                onBack = onBack,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                ProfileOwnedThemesSection(backend)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
