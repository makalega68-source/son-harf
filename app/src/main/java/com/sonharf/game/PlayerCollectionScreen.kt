package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend

/** One shelf of the collection; [kind] is the store item kind it shows. */
private data class CollectionShelf(val kind: String, val icon: ImageVector, val label: String)

private fun collectionShelves() = listOf(
    CollectionShelf("game_theme", Icons.Rounded.Palette, sh("Temalar", "Themes")),
    CollectionShelf("board_skin", Icons.Rounded.GridOn, sh("Tahtalar", "Boards")),
    CollectionShelf("profile_frame", Icons.Rounded.AccountCircle, sh("Çerçeveler", "Frames")),
    CollectionShelf("mascot_hat", Icons.Rounded.Pets, sh("Maskot", "Mascot")),
    CollectionShelf("keyboard_theme", Icons.Rounded.Keyboard, sh("Klavyeler", "Keyboards")),
    CollectionShelf("name_style", Icons.Rounded.TextFields, sh("İsim Stili", "Name Style")),
)

/** The profile is the only place where owned cosmetics and themes are equipped, one shelf at a time. */
@Composable
internal fun PlayerCollectionScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    val shelves = remember { collectionShelves() }
    var shelf by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(LobbyPalette.Ground)) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            MainScreenHeader(sh("Koleksiyonum", "My collection"),
                sh("Sahip olduklarını seç, tarzını yansıt", "Equip your favorites, make it yours"), onBack = onBack)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            shelves.forEachIndexed { index, s ->
                val active = index == shelf
                Surface(onClick = { shelf = index }, shape = RoundedCornerShape(50),
                    color = if (active) LobbyBrand.Chip else LobbyPalette.Paper,
                    border = BorderStroke(1.dp, if (active) LobbyBrand.Chip else LobbyPalette.Line)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(s.icon, null, tint = if (active) LobbyBrand.Gold else LobbyPalette.Accent, modifier = Modifier.size(18.dp))
                        Text(s.label, color = if (active) androidx.compose.ui.graphics.Color.White else LobbyPalette.Ink,
                            fontSize = 14.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            item {
                ProfileOwnedThemesSection(backend, category = shelves[shelf].kind)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
