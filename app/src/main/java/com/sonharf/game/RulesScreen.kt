package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One rules topic: a short title and a short answer (no walls of text). */
internal data class RuleTopic(val id: String, val icon: ImageVector, val accent: Color, val title: String, val body: String)

/**
 * The rules, in the order a new player needs them. The scoring text matches the server:
 * total = word points + territory points, 2 points per held cell, word points are never lost.
 */
internal fun ruleTopics(): List<RuleTopic> = listOf(
    RuleTopic("what", Icons.Rounded.GridView, GameColors.TacticalTurquoise,
        gameText("Kelime Kuşatması nedir?", "What is Kelime Kuşatması?"),
        gameText(
            "İki oyunculu bir kelime ve alan savaşı. Tahtaya kelime koyarak puan alır, kelimenin değdiği küpleri ele geçirerek haritayı büyütürsün.",
            "A two-player word and territory battle. Place words to score and capture the cells they touch to grow your map.",
        )),
    RuleTopic("word", Icons.Rounded.Abc, GameColors.PrimaryBlue,
        gameText("Kelime nasıl oluşturulur?", "How do I make a word?"),
        gameText(
            "Rafındaki harfleri tek satır veya tek sütun boyunca yerleştir. İlk kelime merkezden geçer; sonrakiler tahtadaki harflere bağlanmalıdır. Oluşan tüm kelimeler sözlükte olmalıdır.",
            "Place tiles from your rack in one row or one column. The first word crosses the centre; later words must connect to tiles on the board. Every word formed must be in the dictionary.",
        )),
    RuleTopic("capture", Icons.Rounded.Flag, GameColors.PlayGreen,
        gameText("Bölge nasıl ele geçirilir?", "How do I capture territory?"),
        gameText(
            "Kelimenin kapladığı küpler senin olur. Rakibin küplerinin üzerinden geçen bir kelime o küpleri ondan alır.",
            "The cells your word covers become yours. A word that runs over rival cells takes them from the rival.",
        )),
    RuleTopic("siege", Icons.Rounded.Shield, GameColors.RewardAmber,
        gameText("Kuşatma nedir?", "What is a siege?"),
        gameText(
            "Rakibin bölgesine kelimeyle girip küplerini almak kuşatmadır. Büyük bir kuşatma skoru tek hamlede çevirebilir.",
            "Entering rival territory with a word and taking its cells is a siege. A big siege can swing the score in one move.",
        )),
    RuleTopic("word_points", Icons.Rounded.Star, GameColors.PrimaryBlue,
        gameText("Kelime puanı nasıl hesaplanır?", "How are word points counted?"),
        gameText(
            "Her kelime, harf değerleri ve özel karelerle puan verir. Kelime puanı kalıcıdır: rakip bölgeni alsa bile kazandığın kelime puanı geri alınmaz.",
            "Each word scores from its letter values and bonus squares. Word points are permanent: even if the rival takes your cells, your word points stay.",
        )),
    RuleTopic("area_points", Icons.Rounded.CropSquare, GameColors.PlayGreen,
        gameText("Bölge puanı nasıl hesaplanır?", "How are territory points counted?"),
        gameText(
            "Elinde tuttuğun her küp 2 puandır. Küp kaybedersen yalnızca o küplerin bölge puanı düşer.",
            "Every cell you hold is worth 2 points. If you lose cells, only their territory points go.",
        )),
    RuleTopic("map", Icons.Rounded.Map, GameColors.TacticalTurquoise,
        gameText("Harita kontrolü nedir?", "What is map control?"),
        gameText(
            "Tahtadaki küplerin yüzde kaçının senin olduğudur. Daha az kelime puanıyla bile, daha geniş alanla toplamda öne geçebilirsin.",
            "The share of the board's cells you hold. With fewer word points you can still lead on total by holding more ground.",
        )),
    RuleTopic("win", Icons.Rounded.EmojiEvents, GameColors.PrestigeGold,
        gameText("Maç nasıl kazanılır?", "How do I win?"),
        gameText(
            "Maç bittiğinde toplam skoru (kelime puanı + bölge puanı) yüksek olan kazanır. Süresinde hamle yapmamak ve pes etmek maçı kaybettirebilir.",
            "When the match ends, the higher total (word points + territory points) wins. Missing turns or resigning can lose the match.",
        )),
    RuleTopic("league", Icons.Rounded.Leaderboard, GameColors.RewardAmber,
        gameText("Lig ve rating", "League and rating"),
        gameText(
            "Dereceli maçlar rating'ini değiştirir; rating'in ligini belirler: Bronz, Gümüş, Altın, Platin, Elmas, Efsane. Bot maçları rating'i etkilemez.",
            "Ranked matches change your rating, and your rating sets your league: Bronze, Silver, Gold, Platinum, Diamond, Legend. Bot games are unrated.",
        )),
    RuleTopic("last_letter", Icons.Rounded.Bolt, GameColors.PrimaryBlue,
        gameText("Son Harf", "Last Letter"),
        gameText(
            "Sıra sende olduğunda, önceki kelimenin son harfiyle başlayan geçerli bir kelime yaz. Her tur 15 saniyedir; hızlı ve doğru olan kazanır.",
            "On your turn, type a valid word starting with the last letter of the previous word. Each turn is 15 seconds; the quick and accurate win.",
        )),
    RuleTopic("letter_path", Icons.Rounded.Route, GameColors.TacticalTurquoise,
        gameText("Harf Yolu", "Letter Path"),
        gameText(
            "Başlangıç kelimesinden hedef kelimeye, her adımda tek harf değiştirerek geçerli kelimelerle ulaş.",
            "Get from the start word to the target word by changing one letter per step, using valid words.",
        )),
)

@Composable
internal fun RulesScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf(setOf("what")) }
    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = gameText("Nasıl Oynanır", "How to Play"),
            subtitle = gameText("Kelime Kuşatması 30 saniyede", "Kelime Kuşatması in 30 seconds"),
            onBack = onBack,
        )
        RulesContent(open = open, onToggle = { id -> open = if (id in open) open - id else open + id })
    }
}

@Composable
internal fun RulesContent(open: Set<String>, onToggle: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "score-formula") {
            Surface(
                shape = GameShapes.Large,
                color = GameColors.PrimarySurface,
                border = BorderStroke(1.dp, GameColors.TacticalTurquoise.copy(alpha = .45f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(gameText("TOPLAM SKOR", "TOTAL SCORE"), color = GameColors.TacticalTurquoise, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        gameText("Kelime puanı + Bölge puanı", "Word points + Territory points"),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        gameText("Her küp 2 puan • kelime puanı kalıcı", "2 points per cell • word points are permanent"),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        items(ruleTopics(), key = { it.id }) { topic ->
            RuleCard(topic, expanded = topic.id in open) { onToggle(topic.id) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun RuleCard(topic: RuleTopic, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = GameShapes.Medium,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, if (expanded) topic.accent.copy(alpha = .45f) else GameColors.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(topic.icon, null, tint = topic.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(topic.title, Modifier.weight(1f), color = GameColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) gameText("Kapat", "Collapse") else gameText("Aç", "Expand"),
                    tint = GameColors.TextSecondary,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(
                    topic.body,
                    Modifier.padding(top = 8.dp, start = 34.dp),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
