package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.PrivateRoomBackend
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PRO private-room lobby for Son Harf. The server remains authoritative: creating a room is
 * PRO-gated by the RPC, joining requires a valid waiting-room code, and the regular game screen
 * adopts the resulting live room after both players are present.
 */
@Composable
internal fun PrivateRoomCenterScreen(
    onBack: () -> Unit,
    onRoomReady: (String) -> Unit,
) {
    if (!SupabaseProvider.configured) {
        CenteredMessage(
            title = sh("Sunucu bağlantısı yok", "Server unavailable"),
            detail = sh("Özel oda için sunucu bağlantısı gerekir.", "Private rooms require a server connection."),
            onBack = onBack,
        )
        return
    }

    val service = remember { PrivateRoomBackend() }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun leave() {
        val waiting = room?.takeIf { it.status == "waiting" && it.guestId == null }
        if (waiting == null) {
            onBack()
        } else {
            scope.launch {
                runCatching { service.cancel(waiting.id) }
                room = null
                onBack()
            }
        }
    }

    BackHandler(onBack = ::leave)

    LaunchedEffect(room?.id) {
        val waiting = room ?: return@LaunchedEffect
        if (waiting.status != "waiting") return@LaunchedEffect
        while (true) {
            delay(1_250)
            val latest = runCatching { backend.getRoom(waiting.id) }.getOrNull() ?: break
            room = latest
            if (latest.status in setOf("playing", "quiz", "final", "sudden_death") && latest.guestId != null) {
                onRoomReady(latest.language)
                break
            }
            if (latest.status != "waiting") break
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::leave) { Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back")) }
            Column(Modifier.weight(1f)) {
                Text(sh("PRO ÖZEL ODA", "PRO PRIVATE ROOM"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = SonHarfText)
                Text(sh("Son Harf için davet kodlu özel düello", "Invite-code private duel for Last Letter"), fontSize = 10.sp, color = SonHarfMuted)
            }
            Icon(Icons.Rounded.Lock, null, tint = SonHarfGold)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SonHarfSurface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(sh("ODA OLUŞTUR", "CREATE ROOM"), fontWeight = FontWeight.Black, color = SonHarfText)
                Text(
                    sh("PRO sahibi oda oluşturur. Rakibin aşağıdaki kodla katılabilir.", "A PRO member creates the room. Your rival can join with its code."),
                    fontSize = 10.sp,
                    color = SonHarfMuted,
                )
                Button(
                    onClick = {
                        if (busy || room != null) return@Button
                        scope.launch {
                            busy = true
                            notice = null
                            runCatching { service.create(SonHarfUiState.language) }
                                .onSuccess { room = it }
                                .onFailure { error ->
                                    notice = when {
                                        "vip_required" in error.message.orEmpty() -> sh("Özel oda PRO üyeliği gerektirir.", "Private rooms require PRO.")
                                        "player_already_in_game" in error.message.orEmpty() -> sh("Önce aktif maçını tamamla.", "Finish your active match first.")
                                        else -> sh("Özel oda oluşturulamadı.", "Private room could not be created.")
                                    }
                                }
                            busy = false
                        }
                    },
                    enabled = !busy && room == null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
                ) {
                    Icon(Icons.Rounded.MeetingRoom, null)
                    Spacer(Modifier.width(8.dp))
                    Text(sh("ÖZEL ODA OLUŞTUR", "CREATE PRIVATE ROOM"), fontWeight = FontWeight.Black)
                }

                room?.let { active ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SonHarfSurface2,
                        border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .45f)),
                    ) {
                        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(sh("ODA KODU", "ROOM CODE"), color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(active.code, color = SonHarfGold, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(7.dp))
                                Text(sh("Rakip bekleniyor…", "Waiting for rival…"), color = SonHarfMuted, fontSize = 10.sp)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (busy) return@OutlinedButton
                            scope.launch {
                                busy = true
                                runCatching { service.cancel(active.id) }
                                room = null
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(sh("ODAYI İPTAL ET", "CANCEL ROOM"), fontWeight = FontWeight.Bold) }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SonHarfSurface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(sh("KODLA KATIL", "JOIN WITH CODE"), fontWeight = FontWeight.Black, color = SonHarfText)
                Text(
                    sh("Oda sahibinin verdiği 6 karakterli kodu gir.", "Enter the 6-character code from the room host."),
                    fontSize = 10.sp,
                    color = SonHarfMuted,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isLetterOrDigit).take(6).uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(sh("Oda kodu", "Room code")) },
                    leadingIcon = { Icon(Icons.Rounded.Key, null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
                Button(
                    onClick = {
                        if (busy || code.length != 6) return@Button
                        scope.launch {
                            busy = true
                            notice = null
                            runCatching { service.join(code) }
                                .onSuccess { joined ->
                                    room = joined
                                    onRoomReady(joined.language)
                                }
                                .onFailure { error ->
                                    notice = when {
                                        "room_not_available" in error.message.orEmpty() -> sh("Bu oda bulunamadı veya artık açık değil.", "This room was not found or is no longer open.")
                                        "player_already_in_game" in error.message.orEmpty() -> sh("Önce aktif maçını tamamla.", "Finish your active match first.")
                                        else -> sh("Odaya katılınamadı.", "Could not join the room.")
                                    }
                                }
                            busy = false
                        }
                    },
                    enabled = !busy && code.length == 6,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(sh("ODAYA KATIL", "JOIN ROOM"), fontWeight = FontWeight.Black) }
            }
        }

        notice?.let {
            Surface(shape = RoundedCornerShape(14.dp), color = SonHarfSurface2) {
                Text(it, modifier = Modifier.fillMaxWidth().padding(12.dp), color = SonHarfText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CenteredMessage(title: String, detail: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = SonHarfText)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = SonHarfMuted, fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = onBack) { Text(sh("GERİ", "BACK")) }
    }
}
