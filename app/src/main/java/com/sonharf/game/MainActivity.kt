package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.domain.GameState
import com.sonharf.game.domain.WordChainEngine
import com.sonharf.game.ui.theme.SonHarfTheme

enum class AppScreen { HOME, GAME, SHOP, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SonHarfTheme { SonHarfApp() } }
    }
}

@Composable
private fun SonHarfApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = screen == AppScreen.HOME, onClick = { screen = AppScreen.HOME }, icon = { Text("⌂") }, label = { Text("Ana Sayfa") })
                NavigationBarItem(selected = screen == AppScreen.GAME, onClick = { screen = AppScreen.GAME }, icon = { Text("⚔") }, label = { Text("Oyna") })
                NavigationBarItem(selected = screen == AppScreen.SHOP, onClick = { screen = AppScreen.SHOP }, icon = { Text("◆") }, label = { Text("Mağaza") })
                NavigationBarItem(selected = screen == AppScreen.PROFILE, onClick = { screen = AppScreen.PROFILE }, icon = { Text("●") }, label = { Text("Profil") })
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                AppScreen.HOME -> HomeScreen(onPlay = { screen = AppScreen.GAME })
                AppScreen.GAME -> GameScreen()
                AppScreen.SHOP -> ShopScreen()
                AppScreen.PROFILE -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen(onPlay: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("SON HARF", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text("Rakibini kelimelerle köşeye sıkıştır.", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hızlı Düello", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("45 saniyelik turlar • 2 oyuncu • canlı sohbet")
                    Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) { Text("Rakip Bul") }
                    OutlinedButton(onClick = onPlay, modifier = Modifier.fillMaxWidth()) { Text("Özel Oda Oluştur") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Lig", "Bronz III", Modifier.weight(1f))
                StatCard("Seri", "3 galibiyet", Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Günün Görevi", fontWeight = FontWeight.Bold)
                    Text("3 maç tamamla")
                    LinearProgressIndicator(progress = { 1f / 3f }, modifier = Modifier.fillMaxWidth())
                    Text("Ödül: 25 elmas", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GameScreen() {
    val engine = remember { WordChainEngine() }
    var state by remember { mutableStateOf(GameState()) }
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("ÜMİT", fontWeight = FontWeight.Bold); Text("1240 XP", style = MaterialTheme.typography.labelMedium) }
            Surface(shape = RoundedCornerShape(999.dp), tonalElevation = 4.dp) { Text("00:45", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold) }
            Column(horizontalAlignment = Alignment.End) { Text("RAKİP", fontWeight = FontWeight.Bold); Text("1185 XP", style = MaterialTheme.typography.labelMedium) }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sıra: Oyuncu ${state.currentPlayer}", fontWeight = FontWeight.Bold)
                Text(state.message)
                val required = state.chain.lastOrNull()?.word?.lastOrNull()?.uppercaseChar()
                if (required != null) Text("Yeni kelime $required ile başlamalı", color = MaterialTheme.colorScheme.primary)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.chain.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("İlk kelimeyi yazarak zinciri başlat.")
                    }
                }
            }
            items(state.chain) { entry ->
                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.word.uppercase(), fontWeight = FontWeight.Bold)
                        Text("Oyuncu ${entry.player}")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AssistChip(onClick = {}, label = { Text("Tuzak Harf") })
            AssistChip(onClick = {}, label = { Text("Pas") })
            AssistChip(onClick = {}, label = { Text("Sohbet") })
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Kelime yaz") },
            enabled = state.winner == null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val next = engine.submit(state, input)
                    if (next.chain.size > state.chain.size) input = ""
                    state = next
                },
                enabled = state.winner == null && input.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("Gönder") }
            OutlinedButton(onClick = { state = engine.forfeit(state) }, enabled = state.winner == null, modifier = Modifier.weight(1f)) { Text("Pes Et") }
        }
        if (state.winner != null) Button(onClick = { state = GameState(); input = "" }, modifier = Modifier.fillMaxWidth()) { Text("Rövanş") }
    }
}

@Composable
private fun ShopScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mağaza", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Reklamsız • VIP rozeti • özel temalar • gelişmiş istatistik")
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("VIP'i Gör") }
                }
            }
        }
        items(listOf("100 Elmas", "500 Elmas", "Premium Tema Paketi", "Özel Emoji Paketi")) { item ->
            ListItem(headlineContent = { Text(item) }, supportingContent = { Text("Google Play üzerinden güvenli satın alma") }, trailingContent = { Text("Yakında") })
            HorizontalDivider()
        }
    }
}

@Composable
private fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ÜMİT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Bronz III • 1240 XP")
                Text("12 galibiyet • 8 mağlubiyet • %60 kazanma")
            }
        }
        Text("Başarımlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("🔥 3 maçlık seri   •   ⚡ Hızlı cevap   •   🧠 50 farklı kelime")
    }
}
