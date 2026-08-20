package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReferenceSonHarfAppEnhanced() {
    Box(Modifier.fillMaxSize()) {
        ReferenceSonHarfApp()
        OnlineGameScreenComboOverlayOnly()
        ReferenceGameKeyboardLayer()
    }
}

@Composable
private fun ReferenceGameKeyboardLayer() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showLastWord by remember { mutableStateOf(false) }
    val me = backend.currentUserId()

    suspend fun activeRoom(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == uid || it.guestId == uid) && it.status in listOf("playing", "final", "sudden_death") }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        while (true) {
            room = runCatching { activeRoom() }.getOrNull()
            room?.let { r -> words = runCatching { backend.getWords(r.id) }.getOrDefault(words) }
            if (room == null) { words = emptyList(); input = "" }
            delay(500)
        }
    }

    val last = words.lastOrNull()
    LaunchedEffect(last?.id) {
        if (last != null) {
            showLastWord = true
            delay(720)
            showLastWord = false
        }
    }
    LaunchedEffect(room?.currentPlayerId, room?.validWordCount, room?.roundNo) { input = ""; notice = null }

    val active = room ?: return
    val myTurn = active.currentPlayerId == me
    val normalized = last?.normalizedWord
    val required = normalized?.lastOrNull()?.uppercaseChar()?.toString() ?: "•"
    val focusText = if (showLastWord && normalized != null) normalized.uppercase() else required
    val focusLabel = if (showLastWord && normalized != null) "SON KELİME" else "SON HARF"

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center).offset(y = (-78).dp).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(focusLabel, color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                focusText,
                color = SonHarfText,
                fontSize = if (focusText.length <= 1) 76.sp else if (focusText.length <= 6) 44.sp else 32.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

        ReferenceKeyboard(
            language = active.language,
            value = input,
            enabled = myTurn && !busy,
            notice = notice,
            onKey = { if (myTurn && !busy && input.length < 40) { input += it; SonHarfSoundFx.tap() } },
            onBackspace = { if (myTurn && !busy && input.isNotEmpty()) { input = input.dropLast(1); SonHarfSoundFx.tap() } },
            onSend = {
                if (!myTurn || busy || input.isBlank()) return@ReferenceKeyboard
                val submitted = input.trim()
                input = ""
                busy = true
                scope.launch {
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { result ->
                            room = result
                            notice = when (result.lastEvent) {
                                "word_already_used" -> "Bu kelime daha önce kullanıldı."
                                "wrong_start_letter" -> "Kelime $required ile başlamalı."
                                "not_in_dictionary" -> "Bu kelime sözlükte bulunamadı."
                                "invalid_word" -> "Bu kelime geçerli değil."
                                "turn_expired" -> "Süren doldu. −1 puan."
                                else -> "${submitted.uppercase()} kabul edildi."
                            }
                            if (result.lastEvent in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired")) SonHarfSoundFx.warning() else SonHarfSoundFx.wordAccepted()
                        }
                        .onFailure { notice = "Bağlantı sorunu. Yeniden deneniyor."; SonHarfSoundFx.warning() }
                    busy = false
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReferenceKeyboard(
    language: String,
    value: String,
    enabled: Boolean,
    notice: String?,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = if (language == "tr") listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
        listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
        listOf("Z","X","C","V","B","N","M","Ö","Ç")
    ) else listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P"),
        listOf("A","S","D","F","G","H","J","K","L"),
        listOf("Z","X","C","V","B","N","M")
    )

    Surface(modifier.fillMaxWidth(), color = Color(0xFF050B15), border = BorderStroke(1.dp, Color.White.copy(alpha = .08f))) {
        Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(15.dp), color = SonHarfSurface, border = BorderStroke(1.dp, if (enabled) SonHarfCyan.copy(alpha = .45f) else Color.White.copy(alpha = .08f))) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 13.dp), contentAlignment = Alignment.CenterStart) {
                        Text(if (value.isBlank()) if (enabled) "Kelimenizi yazın…" else "Rakibin sırası…" else value.uppercase(), color = if (value.isBlank()) SonHarfMuted else SonHarfText, fontSize = 17.sp, fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Bold)
                    }
                }
                Button(onClick = onSend, enabled = enabled && value.isNotBlank(), modifier = Modifier.size(46.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)) { Text("➤") }
            }
            if (!notice.isNullOrBlank()) Text(notice, Modifier.fillMaxWidth(), color = SonHarfMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
            rows.forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (rowIndex == 2) Spacer(Modifier.weight(.25f))
                    row.forEach { key ->
                        Button(onClick = { onKey(key) }, enabled = enabled, modifier = Modifier.weight(1f).height(34.dp), shape = RoundedCornerShape(7.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152137))) { Text(key, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (rowIndex == 2) {
                        Button(onClick = onBackspace, enabled = enabled && value.isNotEmpty(), modifier = Modifier.weight(1.3f).height(34.dp), shape = RoundedCornerShape(7.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25182A))) { Text("⌫", fontSize = 17.sp) }
                        Spacer(Modifier.weight(.25f))
                    }
                }
            }
        }
    }
}
