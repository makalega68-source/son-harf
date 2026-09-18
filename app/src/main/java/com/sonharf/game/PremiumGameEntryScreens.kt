package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object SiegeEntryUi {
    val Ink = Color(0xFF20352B)
    val Muted = Color(0xFF64746C)
    val Forest = Color(0xFF335F4A)
    val ForestDeep = Color(0xFF234638)
    val Sage = Color(0xFFBFD4C6)
    val Sand = Color(0xFFF2EBDD)
    val Gold = Color(0xFFB28A45)
    val Surface = Color(0xFFFFFDF8)
}

private object LastLetterEntryUi {
    val Ink = Color(0xFF213A36)
    val Muted = Color(0xFF667A75)
    val Teal = Color(0xFF28796F)
    val Deep = Color(0xFF1D5751)
    val Aqua = Color(0xFFBFE9E1)
    val Mint = Color(0xFFE9F6F2)
    val Coral = Color(0xFFD76C67)
    val Surface = Color(0xFFFFFEFA)
}

private object LetterPathEntryUi {
    val Ink = Color(0xFF29364A)
    val Muted = Color(0xFF6F7888)
    val Blue = Color(0xFF526B9B)
    val Deep = Color(0xFF3C4F78)
    val Lavender = Color(0xFFDCDDF4)
    val Mint = Color(0xFFDDEEE8)
    val Surface = Color(0xFFFFFEFB)
}

@Composable
internal fun PremiumSiegeEntryScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameEntryTopBar(
                title = sh("KELİME KUŞATMASI", "WORD SIEGE"),
                subtitle = sh("Taktik alan savaşı", "Tactical territory battle"),
                accent = SiegeEntryUi.Forest,
                onBack = onBack,
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(SiegeEntryUi.ForestDeep, SiegeEntryUi.Forest)))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        sh("HARİTAYI KELİMELERLE ELE GEÇİR", "CAPTURE THE MAP WITH WORDS"),
                        color = Color.White,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        sh(
                            "Kelime puanı kalıcıdır. Bölge puanı harita hâkimiyetini değiştirir. Rakibinin alanını kuşat ve kontrolü ele geçir.",
                            "Word score is permanent. Territory score shifts map control. Siege rival territory and take control.",
                        ),
                        color = Color.White.copy(alpha = .78f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                    SiegeEntryMiniMap()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameEntryMetric(Icons.Rounded.GridView, sh("ALAN", "TERRITORY"), "2 P / ${sh("KÜP", "CELL")}", Color.White, Modifier.weight(1f))
                        GameEntryMetric(Icons.Rounded.Groups, "1v1", sh("CANLI + BOT", "LIVE + BOT"), Color.White, Modifier.weight(1f))
                        GameEntryMetric(Icons.Rounded.EmojiEvents, sh("LİG", "LEAGUE"), sh("RATING", "RATING"), Color.White, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            GameLanguagePanel(
                language = language,
                accent = SiegeEntryUi.Forest,
                surface = SiegeEntryUi.Surface,
                selectedSoft = SiegeEntryUi.Sage.copy(alpha = .55f),
                onLanguageChange = onLanguageChange,
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SiegeEntryUi.Sand.copy(alpha = .72f),
                border = BorderStroke(1.dp, SiegeEntryUi.Gold.copy(alpha = .28f)),
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = SiegeEntryUi.Gold, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(sh("TAKTİK İPUCU", "TACTICAL TIP"), color = SiegeEntryUi.Ink, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(sh("Kelimeyi yalnızca puan için değil, rakibin bölgesini kırmak için de konumlandır.", "Place words not only for points, but to break rival territory."), color = SiegeEntryUi.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
        item {
            GameEntryPrimaryButton(
                label = if (language == "tr") "TÜRKÇE KUŞATMAYA GİR" else "ENTER ENGLISH SIEGE",
                icon = Icons.Rounded.Shield,
                accent = SiegeEntryUi.Forest,
                onClick = onPlay,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
internal fun PremiumLastLetterEntryScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameEntryTopBar(
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı 1v1 kelime düellosu", "Fast 1v1 word duel"),
                accent = LastLetterEntryUi.Teal,
                onBack = onBack,
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = LastLetterEntryUi.Surface,
                border = BorderStroke(1.dp, LastLetterEntryUi.Aqua),
                shadowElevation = 7.dp,
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = LastLetterEntryUi.Mint) {
                            Icon(Icons.Rounded.Bolt, null, tint = LastLetterEntryUi.Teal, modifier = Modifier.padding(12.dp).size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("ZİNCİRİ KIRMA", "KEEP THE CHAIN ALIVE"), color = LastLetterEntryUi.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(sh("Son harften devam et. Hızlı düşün. Rakibini üç roundda geç.", "Continue from the last letter. Think fast. Win across three rounds."), color = LastLetterEntryUi.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                    LastLetterChainPreview(language)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LastLetterRuleCard("3", sh("ROUND", "ROUNDS"), Modifier.weight(1f))
                        LastLetterRuleCard("10 + 10", sh("KELİME", "WORDS"), Modifier.weight(1f))
                        LastLetterRuleCard("15→13→11", sh("SANİYE", "SECONDS"), Modifier.weight(1f))
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = LastLetterEntryUi.Mint) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Verified, null, tint = LastLetterEntryUi.Teal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(sh("Sunucu doğrulamalı sözlük • seviye uyumlu bot fallback", "Server-verified dictionary • level-matched bot fallback"), color = LastLetterEntryUi.Deep, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            GameLanguagePanel(
                language = language,
                accent = LastLetterEntryUi.Teal,
                surface = LastLetterEntryUi.Surface,
                selectedSoft = LastLetterEntryUi.Aqua.copy(alpha = .55f),
                onLanguageChange = onLanguageChange,
            )
        }
        item {
            GameEntryPrimaryButton(
                label = if (language == "tr") "TÜRKÇE DÜELLOYU BAŞLAT" else "START ENGLISH DUEL",
                icon = Icons.Rounded.Bolt,
                accent = LastLetterEntryUi.Teal,
                onClick = onPlay,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
internal fun PremiumLetterPathEntryScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GameEntryTopBar(
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Kısa oturum • kelime rotası", "Quick session • word route"),
                accent = LetterPathEntryUi.Blue,
                onBack = onBack,
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = LetterPathEntryUi.Surface,
                border = BorderStroke(1.dp, LetterPathEntryUi.Lavender),
                shadowElevation = 6.dp,
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(sh("BİR HARFTEN DİĞERİNE ROTA KUR", "BUILD A ROUTE FROM LETTER TO LETTER"), color = LetterPathEntryUi.Ink, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kısa oturumlarda kelime rotanı tamamla, yeni hedefleri aç ve ilerlemeyi sürdür.", "Complete word routes in short sessions, unlock new targets, and keep progressing."), color = LetterPathEntryUi.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    LetterPathPreview(language)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameEntryMetric(Icons.Rounded.Route, sh("ROTA", "ROUTE"), sh("HEDEF", "TARGET"), LetterPathEntryUi.Blue, Modifier.weight(1f))
                        GameEntryMetric(Icons.Rounded.Timer, sh("HIZLI", "QUICK"), sh("OTURUM", "SESSION"), LetterPathEntryUi.Blue, Modifier.weight(1f))
                        GameEntryMetric(Icons.Rounded.AutoAwesome, sh("İLERLE", "PROGRESS"), sh("AÇILIM", "UNLOCKS"), LetterPathEntryUi.Blue, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            GameLanguagePanel(
                language = language,
                accent = LetterPathEntryUi.Blue,
                surface = LetterPathEntryUi.Surface,
                selectedSoft = LetterPathEntryUi.Lavender.copy(alpha = .72f),
                onLanguageChange = onLanguageChange,
            )
        }
        item {
            GameEntryPrimaryButton(
                label = if (language == "tr") "TÜRKÇE ROTAYI BAŞLAT" else "START ENGLISH PATH",
                icon = Icons.Rounded.Route,
                accent = LetterPathEntryUi.Blue,
                onClick = onPlay,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun GameEntryTopBar(title: String, subtitle: String, accent: Color, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(44.dp).clickable(onClick = onBack),
            shape = RoundedCornerShape(15.dp),
            color = Color.White.copy(alpha = .88f),
            border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = accent, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = .3.sp)
            Text(subtitle, color = Color(0xFF6B7772), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GameLanguagePanel(
    language: String,
    accent: Color,
    surface: Color,
    selectedSoft: Color,
    onLanguageChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .17f)),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Language, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text(sh("OYUN DİLİ", "GAME LANGUAGE"), color = Color(0xFF2D3F39), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text(if (language == "tr") "TÜRKÇE" else "ENGLISH", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                LanguageOption("tr", "TR", "TÜRKÇE", language, accent, selectedSoft, Modifier.weight(1f), onLanguageChange)
                LanguageOption("en", "EN", "ENGLISH", language, accent, selectedSoft, Modifier.weight(1f), onLanguageChange)
            }
        }
    }
}

@Composable
private fun LanguageOption(
    code: String,
    short: String,
    label: String,
    language: String,
    accent: Color,
    selectedSoft: Color,
    modifier: Modifier,
    onLanguageChange: (String) -> Unit,
) {
    val selected = language == code
    Surface(
        modifier = modifier.height(54.dp).clickable { onLanguageChange(code) },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) selectedSoft else Color.Transparent,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else accent.copy(alpha = .32f)),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
            }
            Column {
                Text(short, color = if (selected) accent else Color(0xFF3D4543), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(label, color = if (selected) accent else Color(0xFF59625F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GameEntryPrimaryButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(62.dp),
        shape = RoundedCornerShape(19.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = .35.sp)
    }
}

@Composable
private fun GameEntryMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    accent: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = Color.White.copy(alpha = if (accent == Color.White) .12f else .72f),
        border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
            Text(detail, color = accent.copy(alpha = .78f), fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun SiegeEntryMiniMap() {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = .09f), border = BorderStroke(1.dp, Color.White.copy(alpha = .16f))) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(4) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(7) { col ->
                        val owner = (row * 7 + col) % 5
                        val fill = when (owner) {
                            0, 1 -> SiegeEntryUi.Sage
                            2 -> Color(0xFFAEC9DF)
                            else -> Color.White.copy(alpha = .72f)
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(fill.copy(alpha = if (owner >= 3) .42f else .88f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (row == 1 && col == 3) Icon(Icons.Rounded.Star, null, tint = SiegeEntryUi.Gold, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LastLetterChainPreview(language: String) {
    val letters = if (language == "en") listOf("W", "D", "M", "E") else listOf("K", "E", "E", "A")
    val words = if (language == "en") listOf("WORD", "DREAM", "MOVE", "ECHO") else listOf("KALE", "ELMA", "ARPA", "AKIL")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            words.forEachIndexed { index, word ->
                Surface(shape = RoundedCornerShape(12.dp), color = if (index == words.lastIndex) LastLetterEntryUi.Aqua else LastLetterEntryUi.Mint) {
                    Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(word, color = LastLetterEntryUi.Deep, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text(letters[index], color = LastLetterEntryUi.Coral, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (index < words.lastIndex) Icon(Icons.Rounded.ArrowForward, null, tint = LastLetterEntryUi.Teal.copy(alpha = .55f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun LastLetterRuleCard(value: String, label: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(15.dp), color = LastLetterEntryUi.Mint, border = BorderStroke(1.dp, LastLetterEntryUi.Aqua)) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = LastLetterEntryUi.Deep, fontSize = if (value.length > 7) 11.sp else 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = LastLetterEntryUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun LetterPathPreview(language: String) {
    val letters = if (language == "en") listOf("W", "O", "R", "D") else listOf("H", "A", "R", "F")
    Surface(shape = RoundedCornerShape(20.dp), color = LetterPathEntryUi.Lavender.copy(alpha = .42f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            letters.forEachIndexed { index, letter ->
                Surface(shape = CircleShape, color = if (index == letters.lastIndex) LetterPathEntryUi.Blue else Color.White, border = BorderStroke(2.dp, LetterPathEntryUi.Blue.copy(alpha = .42f))) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Text(letter, color = if (index == letters.lastIndex) Color.White else LetterPathEntryUi.Deep, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (index < letters.lastIndex) {
                    Box(Modifier.width(22.dp).height(3.dp).background(LetterPathEntryUi.Blue.copy(alpha = .34f)))
                }
            }
        }
    }
}
