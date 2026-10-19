package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import java.util.Locale

internal suspend fun OnlineGameBackend.lastLetterSeriesFor(room: GameRoomDto, me: String): LastLetterSeries? {
    if (room.isBot) return null
    val known = getLastLetterRooms().associateBy { it.id }.toMutableMap()
    known[room.id] = room
    var cursor = room
    val seen = mutableSetOf<String>()
    repeat(300) {
        if (!seen.add(cursor.id)) return null
        val parent = cursor.rematchOf ?: return lastLetterSeries(room, known.values.toList(), me)
        cursor = known[parent] ?: getRoom(parent).also { known[it.id] = it }
    }
    return null
}

@Composable
internal fun LastLetterResultDetails(room: GameRoomDto, words: List<GameWordDto>, me: String?, series: LastLetterSeries?, dark: Boolean = true) {
    var discovery by remember(room.id) { mutableStateOf<Pair<String, String>?>(null) }
    var discoverySource by remember(room.id) { mutableStateOf<String?>(null) }
    val longest = remember(room.id, words.size, me) { lastLetterLongest(words, me) }
    val locale = if (room.language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
    LaunchedEffect(room.id, words.size) {
        val played = words.map { it.normalizedWord.ifBlank { it.word }.lowercase(locale) }.distinct().sortedByDescending { it.length }
        val local = played.firstNotNullOfOrNull { word -> LastLetterDiscoveries.meaning(word, room.language)?.let { word to it } }
        if (local != null) {
            discovery = local
            discoverySource = sh("Kelime Tahtı · Kısa açıklama", "Word Throne · Short definition")
        } else if (room.language == "tr" && played.isNotEmpty()) {
            val found = DictionaryDefinitionService.lookup(played.first(), room.language)
            if (found != null) {
                discovery = found.headword to found.meanings.first()
                discoverySource = found.sourceLabel
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        series?.let {
            ResultNote(sh("ÜÇ MAÇLIK SERİ", "THREE-MATCH SERIES"),
                sh("Sen ${it.myWins} · Rakip ${it.rivalWins}  |  ${it.played}/3 maç tamamlandı",
                    "You ${it.myWins} · Rival ${it.rivalWins}  |  ${it.played}/3 matches complete"),
                if (it.complete) sh("Seri tamamlandı. Rövanşla yeni bir seri başlatabilirsin.", "Series complete. Rematch to start a new series.")
                else sh("Sonraki maç, ikiniz de rövanşı kabul ettiğinizde başlar.", "The next match starts when you both accept a rematch."), dark)
        }
        longest?.let {
            ResultNote(sh("OBİ’NİN MAÇ NOTU", "OBI'S MATCH NOTE"),
                sh("En uzun kelimen ${it.uppercase(locale)}: ${it.length} harf.", "Your longest word was ${it.uppercase(locale)}: ${it.length} letters."),
                sh("Bu maçta ${words.count { w -> w.playerId == me && !w.isBot }} geçerli kelime kurdun.",
                    "You played ${words.count { w -> w.playerId == me && !w.isBot }} valid words in this match."), dark)
        }
        discovery?.let { (word, meaning) ->
            ResultNote(sh("BU MAÇTAN BİR KELİME", "A WORD FROM THIS MATCH"), word.uppercase(locale) + " · " + meaning, discoverySource, dark)
        }
    }
}

@Composable
private fun ResultNote(title: String, text: String, detail: String?, dark: Boolean) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (dark) Color.White.copy(alpha = .07f) else LobbyPalette.Paper,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = .14f) else LobbyPalette.Line)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = if (dark) Color(0xFFE5CA90) else LobbyPalette.Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text, color = if (dark) Color.White else LobbyPalette.Ink, fontSize = 13.sp, lineHeight = 18.sp)
            if (detail != null) Text(detail, color = if (dark) Color.White.copy(alpha = .7f) else LobbyPalette.Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
