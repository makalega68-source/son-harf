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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val SonHarfBg = Color(0xFF080B14)
internal val SonHarfSurface = Color(0xFF121827)
internal val SonHarfSurface2 = Color(0xFF1A2236)
internal val SonHarfPurple = Color(0xFF7C5CFC)
internal val SonHarfCyan = Color(0xFF35D5FF)
internal val SonHarfGold = Color(0xFFFFC857)
internal val SonHarfText = Color(0xFFF5F7FF)
internal val SonHarfMuted = Color(0xFF9AA6BE)

enum class AppScreen { HOME, GAME, SHOP, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = SonHarfPurple,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGold,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText
                )
            ) {
                SonHarfApp()
            }
        }
    }
}

@Composable
private fun SonHarfApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }

    Scaffold(
        containerColor = SonHarfBg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D1220), tonalElevation = 0.dp) {
                NavItem("⌂", "Ana Sayfa", screen == AppScreen.HOME) { screen = AppScreen.HOME }
                NavItem("⚔", "Oyna", screen == AppScreen.GAME) { screen = AppScreen.GAME }
                NavItem("◆", "Mağaza", screen == AppScreen.SHOP) { screen = AppScreen.SHOP }
                NavItem("●", "Profil", screen == AppScreen.PROFILE) { screen = AppScreen.PROFILE }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B1020), SonHarfBg, Color(0xFF090A11))
                    )
                )
        ) {
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
private fun RowScope.NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Surface(
                color = if (selected) SonHarfPurple.copy(alpha = .18f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    icon,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    fontSize = 19.sp,
                    color = if (selected) SonHarfCyan else SonHarfMuted
                )
            }
        },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedTextColor = SonHarfText,
            unselectedTextColor = SonHarfMuted,
            indicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun HomeScreen(onPlay: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SON HARF", fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Text("Kelime düellosu", color = SonHarfMuted, fontSize = 14.sp)
                }
                Surface(color = SonHarfSurface2, shape = RoundedCornerShape(18.dp)) {
                    Text("◆  0", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = SonHarfCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6A4CF4), Color(0xFF3F65F8), Color(0xFF1FB6D9))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = Color.White.copy(alpha = .15f), shape = RoundedCornerShape(999.dp)) {
                            Text("⚡ ONLINE DÜELLO", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Son harfi yakala,\nrakibini bitir.", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black)
                        Text("45 saniyelik turlar • canlı oda • anlık sohbet", color = Color.White.copy(alpha = .82f))
                        Button(
                            onClick = onPlay,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A2140))
                        ) {
                            Text("OYUNA GİR", fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("Hızlı Bakış", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("⏱", "45 sn", "Tur süresi", Modifier.weight(1f))
                StatCard("⚔", "1v1", "Düello", Modifier.weight(1f))
                StatCard("✦", "Canlı", "Sohbet", Modifier.weight(1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NASIL OYNANIR?", color = SonHarfCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Bir oda oluştur, kodu arkadaşına gönder ve son harfle başlayan yeni kelimeyi süre dolmadan yaz.", color = SonHarfText, lineHeight = 22.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniPill("1  Oda")
                        MiniPill("2  Kelime")
                        MiniPill("3  Kazan")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: String, value: String, title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(title, color = SonHarfMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MiniPill(text: String) {
    Surface(color = SonHarfSurface2, shape = RoundedCornerShape(999.dp)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = SonHarfMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ShopScreen() {
    val products = listOf(
        Triple("◆ 100", "Başlangıç Elmas Paketi", "Yakında"),
        Triple("◆ 500", "Güçlü Elmas Paketi", "Yakında"),
        Triple("◈ Tema", "Premium Tema Paketi", "Yakında"),
        Triple("☺ Emoji", "Özel Emoji Paketi", "Yakında")
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("MAĞAZA", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Hesabını kişiselleştir", color = SonHarfMuted)
        }

        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFFFB547), Color(0xFFFF7A59))))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("VIP", fontSize = 30.sp, fontWeight = FontWeight.Black)
                            Text("♛", fontSize = 28.sp)
                        }
                        Text("Reklamsız deneyim • VIP rozeti • özel temalar • gelişmiş istatistik", color = Color(0xFF2C1B13))
                        Surface(color = Color.Black.copy(alpha = .18f), shape = RoundedCornerShape(14.dp)) {
                            Text("Test tamamlanınca aktif olacak", Modifier.fillMaxWidth().padding(12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Text("Paketler", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

        items(products) { product ->
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(product.first, color = SonHarfCyan, fontWeight = FontWeight.Black)
                        Text(product.second, fontWeight = FontWeight.SemiBold)
                        Text("Google Play ödeme aşamasında etkinleşecek", color = SonHarfMuted, fontSize = 11.sp)
                    }
                    Surface(color = SonHarfSurface2, shape = RoundedCornerShape(999.dp)) {
                        Text(product.third, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = SonHarfMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("PROFİL", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Oyuncu kimliğin ve istatistiklerin", color = SonHarfMuted)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(26.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = SonHarfPurple.copy(alpha = .18f), shape = RoundedCornerShape(999.dp)) {
                        Text("SK", Modifier.padding(20.dp), fontSize = 26.sp, fontWeight = FontWeight.Black, color = SonHarfCyan)
                    }
                    Text("TEST OYUNCUSU", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Online test sürümü", color = SonHarfMuted)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ProfileMetric("0", "Galibiyet", Modifier.weight(1f))
                ProfileMetric("0", "Mağlubiyet", Modifier.weight(1f))
                ProfileMetric("0", "Elmas", Modifier.weight(1f))
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("İstatistikler", fontWeight = FontWeight.Bold)
                    Text("Canlı maç verileri bağlandıktan sonra galibiyet, seri ve kelime performansı burada görünecek.", color = SonHarfMuted)
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(label, color = SonHarfMuted, fontSize = 10.sp)
        }
    }
}
