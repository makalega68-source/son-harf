package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                AppScreen.GAME -> OnlineGameScreen()
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
                    Text("İki Kişilik Düello", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("45 saniyelik turlar • oda kodu • canlı sohbet")
                    Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) { Text("Online Teste Gir") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Mod", "Son Harf", Modifier.weight(1f))
                StatCard("Tur", "45 saniye", Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Test Hedefi", fontWeight = FontWeight.Bold)
                    Text("İki telefonda oda oluştur, kodla katıl, kelime gönder ve sohbet et.")
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
private fun ShopScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mağaza", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Reklamsız • VIP rozeti • özel temalar • gelişmiş istatistik")
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Testten sonra etkinleşecek") }
                }
            }
        }
        items(listOf("100 Elmas", "500 Elmas", "Premium Tema Paketi", "Özel Emoji Paketi")) { item ->
            ListItem(headlineContent = { Text(item) }, supportingContent = { Text("Google Play aşamasında etkinleşecek") }, trailingContent = { Text("Yakında") })
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
                Text("TEST OYUNCUSU", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Online test sürümü")
                Text("İstatistikler testlerden sonra bağlanacak.")
            }
        }
    }
}
