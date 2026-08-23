package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Focused lobby: profile -> play -> games -> next target -> daily mission -> tournament -> rematch. */
@Composable
internal fun HomeLobby(
    onQuickPlay: () -> Unit,
    onSonHarf: () -> Unit,
    onBilBakalim: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = PortalBlue.copy(alpha = .12f)) {
                Icon(Icons.Rounded.Person, null, tint = PortalBlue, modifier = Modifier.padding(11.dp).size(27.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("OYUN ARENASI", color = PortalText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Gümüş Lig • 1000 Rating • 🔥 Seri", color = PortalMuted, fontSize = 10.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = PortalGold.copy(alpha = .15f)) {
                Text("🪙 Son Coin", Modifier.padding(9.dp), color = PortalText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(onClick = onQuickPlay, modifier = Modifier.fillMaxWidth().height(68.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = PortalBlue)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("OYNA", fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Rakip bul ve başla", fontSize = 10.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            LobbyGameButton("SON HARF", "Kelime Düellosu", Icons.Rounded.Link, PortalBlue, Modifier.weight(1f), onSonHarf)
            LobbyGameButton("BİL BAKALIM", "Bilgi Düellosu", Icons.Rounded.AutoAwesome, PortalGold, Modifier.weight(1f), onBilBakalim)
        }

        LobbyInfoCard("🎯 YAKIN HEDEF", "Altın Lig'e 200 rating", "Bir sonraki hedef her zaman görünür.")
        LobbyInfoCard("🎁 GÜNLÜK GÖREV", "Bugün 3 maç kazan", "Ödül: sandık • güç avantajı vermez")
        LobbyInfoCard("🏆 AKTİF TURNUVA", "Günlük Arena", "Sıralamada yüksel, prestij ödülü kazan")

        OutlinedButton(onClick = onQuickPlay, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(15.dp)) {
            Icon(Icons.Rounded.Replay, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("RÖVANŞ / SON RAKİP", fontWeight = FontWeight.Bold)
        }

        NavigationBar(containerColor = Color.White) {
            listOf("Ana Sayfa" to Icons.Rounded.Home, "Lig" to Icons.Rounded.EmojiEvents, "Arkadaşlar" to Icons.Rounded.Group, "Style" to Icons.Rounded.Palette, "Profil" to Icons.Rounded.Person).forEachIndexed { i, item ->
                NavigationBarItem(selected = i == 0, onClick = {}, icon = { Icon(item.second, item.first) }, label = { Text(item.first, fontSize = 8.sp) })
            }
        }
    }
}

@Composable
private fun LobbyGameButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, accent.copy(alpha = .5f))) {
        Column(Modifier.fillMaxWidth().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(27.dp))
            Text(title, color = PortalText, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text(subtitle, color = PortalMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LobbyInfoCard(label: String, title: String, detail: String) {
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFD6EAF4))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = PortalMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(title, color = PortalText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(detail, color = PortalMuted, fontSize = 9.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = PortalMuted)
        }
    }
}
