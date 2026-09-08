package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.sonharf.game.mascot.*

private val BG = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
private val CARD = Color(0xFF1E293B)
private val LINE = Color(0xFF334155)
private val GOLD = Color(0xFFF59E0B)

// ============================== LOBİ ==============================
@Composable
fun LobbyScreen(vm: GameViewModel, onPro: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(BG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBanner(isPro = vm.profile.isPro)          // banner SADECE burada
        Spacer(Modifier.height(8.dp))

        StatsRow(vm)

        Spacer(Modifier.weight(1f))

        EquippedMascotFigure(
            equippedKey = vm.profile.mascot,
            mood = MascotBrain.mood,
            size = 180.dp,
            modifier = Modifier.clickable { MascotBrain.onPoke() }
        )
        MascotBrain.bubble?.let {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                Text(it, color = Color(0xFF1E293B), fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("SON HARF", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(
            if (vm.lang == "tr") "Kelime düellosu" else "Word duel",
            color = Color(0xFF94A3B8), fontSize = 13.sp
        )

        Spacer(Modifier.weight(1f))

        BigButton(if (vm.lang == "tr") "OYNA" else "PLAY", GOLD) { vm.startMatch() }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallButton(if (vm.lang == "tr") "Mağaza" else "Store") { vm.screen = Screen.STORE }
            SmallButton(if (vm.lang == "tr") "Dil: ${vm.lang.uppercase()}" else "Lang: ${vm.lang.uppercase()}") {
                vm.setLanguage(if (vm.lang == "tr") "en" else "tr")
            }
            SmallButton("PRO") { onPro() }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (vm.lang == "tr") "Krediler" else "Credits",
            color = Color(0xFF64748B), fontSize = 11.sp,
            modifier = Modifier.clickable { vm.screen = Screen.CREDITS }.padding(8.dp)
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun StatsRow(vm: GameViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Chip("LV ${vm.profile.level}")
        Chip("${vm.profile.coins} 🪙")
        Chip("XP ${vm.profile.xp}")
        if (vm.profile.isPro) Chip("PRO")
    }
}

@Composable
private fun Chip(t: String) {
    Surface(shape = RoundedCornerShape(100.dp), color = CARD) {
        Text(t, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
    }
}

@Composable
private fun BigButton(t: String, c: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = c,
        modifier = Modifier.fillMaxWidth(0.7f).height(56.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(t, color = Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SmallButton(t: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp), color = CARD,
        border = BorderStroke(1.dp, LINE),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(t, color = Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

// ============================== OYUN ==============================
@Composable
fun GameScreen(vm: GameViewModel) {
    // DİKKAT: Burada Box/overlay YOK. Her bileşen Column içinde kendi satırında.
    // Maskot şeridi de bir satır olduğu için hiçbir şeyin üstünü kapatamaz.
    Column(
        modifier = Modifier.fillMaxSize().background(BG).padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        // 1. satır — skor
        ScoreStrip(vm.myScore, vm.rivalScore)

        // 2. satır — MASKOT ŞERİDİ (sabit 96dp, kendi alanı)
        MascotLane(secondsLeft = vm.secondsLeft, mascotKey = vm.profile.mascot)

        // 3. satır — sayaç + zorunlu harf
        Surface(shape = RoundedCornerShape(100.dp),
            color = if (vm.secondsLeft <= 5) Color(0xFFEF4444) else Color(0xFF3B82F6)) {
            Text("⏱ ${vm.secondsLeft}", color = Color.White, fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(if (vm.lang == "tr") "ZORUNLU HARF" else "REQUIRED LETTER",
            color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Surface(modifier = Modifier.size(70.dp), shape = RoundedCornerShape(16.dp),
            color = CARD, border = BorderStroke(2.dp, GOLD)) {
            Box(contentAlignment = Alignment.Center) {
                Text(vm.requiredLetter.uppercaseChar().toString(),
                    color = Color(0xFFF8FAFC), fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4. satır — yazılan kelime
        Surface(modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp), color = CARD,
            border = BorderStroke(1.dp, LINE)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    vm.input.ifEmpty { if (vm.lang == "tr") "Kelime yaz..." else "Type word..." },
                    color = if (vm.input.isEmpty()) Color(0xFF64748B) else Color(0xFFF8FAFC),
                    fontSize = 22.sp, fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // 5. satır — klavye
        WordKeyboard(vm)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ScoreStrip(my: Int, rival: Int) {
    val total = (my + rival).coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$my", color = Color(0xFF4ADE80), fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("$rival", color = Color(0xFFF87171), fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            Box(Modifier.weight(my.toFloat() / total).fillMaxHeight().background(Color(0xFF4ADE80)))
            Box(Modifier.weight(rival.toFloat() / total).fillMaxHeight().background(Color(0xFFF87171)))
        }
    }
}

@Composable
private fun WordKeyboard(vm: GameViewModel) {
    val rows = if (vm.lang == "tr")
        listOf("ERTYUIOPĞÜ", "ASDFGHJKLŞİ", "ZCVBNMÖÇ")
    else
        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(vertical = 3.dp)) {
                row.forEach { c ->
                    Surface(
                        shape = RoundedCornerShape(6.dp), color = CARD,
                        modifier = Modifier.weight(1f).height(42.dp).clickable { vm.onKey(c) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(c.toString(), color = Color(0xFFE2E8F0),
                                fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = CARD,
                modifier = Modifier.weight(1f).height(46.dp).clickable { vm.onBackspace() }) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⌫", color = Color(0xFFE2E8F0), fontSize = 18.sp)
                }
            }
            Surface(shape = RoundedCornerShape(10.dp), color = GOLD,
                modifier = Modifier.weight(2f).height(46.dp).clickable { vm.onSubmit() }) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (vm.lang == "tr") "GÖNDER" else "SUBMIT",
                        color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ============================== SONUÇ ==============================
@Composable
fun ResultScreen(vm: GameViewModel, onWatchAd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(BG)) {

        // Kazandıysa maskot ekranda serbestçe uçar
        if (vm.won) VictoryFlight(mascotKey = vm.profile.mascot, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (!vm.won) {
                EquippedMascotFigure(vm.profile.mascot, MascotBrain.mood, 150.dp)
                Spacer(Modifier.height(10.dp))
            }
            Text(
                if (vm.won) (if (vm.lang == "tr") "KAZANDIN!" else "YOU WON!")
                else (if (vm.lang == "tr") "KAYBETTİN" else "YOU LOST"),
                color = if (vm.won) GOLD else Color(0xFFF87171),
                fontSize = 32.sp, fontWeight = FontWeight.Black
            )
            Text("${vm.myScore} - ${vm.rivalScore}", color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("+${vm.lastGain.first} XP   +${vm.lastGain.second} 🪙",
                color = Color(0xFF94A3B8), fontSize = 14.sp)

            Spacer(Modifier.height(18.dp))

            if (!vm.doubleClaimed && !vm.profile.isPro) {
                BigButton(if (vm.lang == "tr") "ÖDÜLÜ 2X YAP (Reklam)" else "DOUBLE REWARD (Ad)",
                    Color(0xFF38BDF8)) { onWatchAd() }
                Spacer(Modifier.height(8.dp))
            }
            BigButton(if (vm.lang == "tr") "TEKRAR OYNA" else "PLAY AGAIN", GOLD) { vm.startMatch() }
            Spacer(Modifier.height(8.dp))
            SmallButton(if (vm.lang == "tr") "Lobiye dön" else "Back to lobby") { vm.backToLobby() }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================== MAĞAZA ==============================
@Composable
fun StoreScreen(vm: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        TopBanner(isPro = vm.profile.isPro)
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(if (vm.lang == "tr") "MAĞAZA" else "STORE",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Chip("${vm.profile.coins} 🪙")
        }
        Text(
            if (vm.lang == "tr") "Her şey oyun parasıyla alınır. Gerçek para yok."
            else "Everything is bought with in-game coins.",
            color = Color(0xFF64748B), fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            items(vm.catalog) { item ->
                val isOwned = vm.owned.contains(item.id)
                val locked = vm.profile.level < item.reqLevel
                Surface(
                    shape = RoundedCornerShape(14.dp), color = CARD,
                    border = BorderStroke(1.dp, LINE),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (vm.lang == "tr") item.nameTr else item.nameEn,
                                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(if (vm.lang == "tr") item.descTr else item.descEn,
                                color = Color(0xFF94A3B8), fontSize = 11.sp)
                            if (locked) Text(
                                (if (vm.lang == "tr") "Seviye " else "Level ") + item.reqLevel,
                                color = Color(0xFFF87171), fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOwned) Color(0xFF334155) else if (locked) Color(0xFF334155) else GOLD,
                            modifier = Modifier.clickable(enabled = !locked) {
                                if (isOwned) vm.equip(item) else vm.buy(item)
                            }
                        ) {
                            Text(
                                when {
                                    isOwned -> if (vm.lang == "tr") "Kuşan" else "Equip"
                                    locked -> "🔒"
                                    else -> "${item.price} 🪙"
                                },
                                color = if (isOwned || locked) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                                fontSize = 13.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }
        SmallButton(if (vm.lang == "tr") "Geri" else "Back") { vm.backToLobby() }
        Spacer(Modifier.height(14.dp))
    }
}

// ============================== KREDİLER (CC BY ZORUNLU) ==============================
@Composable
fun CreditsScreen(vm: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(BG).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text(if (vm.lang == "tr") "KREDİLER" else "CREDITS",
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text(
            "Mage Cat — ana maskot\n" +
            "Yüz ifadeleri: proje lisanslı varlık\n\n" +
            "Ek maskotlar (Rive Marketplace, CC BY 4.0):\n" +
            "• Gut Expression Mascot\n" +
            "• Interactive Sprout Mascot\n" +
            "• Orb Mascot\n\n" +
            "KURULUMDA YAPILACAK: yukarıdaki üç satırın yanına\n" +
            "Rive sayfasındaki yaratıcı kullanıcı adlarını yaz.\n" +
            "CC BY lisansı bunu zorunlu kılar.",
            color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        SmallButton(if (vm.lang == "tr") "Geri" else "Back") { vm.backToLobby() }
        Spacer(Modifier.height(20.dp))
    }
}
