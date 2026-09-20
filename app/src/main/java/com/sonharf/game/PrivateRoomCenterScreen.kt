package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MainScreenHeader(
            title = sh("PRO Özel Oda", "PRO Private Room"),
            subtitle = sh("Son Harf için davet kodlu özel düello", "Invite-code private duel for Last Letter"),
            onBack = ::leave,
            actionIcon = Icons.Rounded.Lock,
            actionDescription = "PRO",
            onAction = {},
        )

        MainGameCard {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MainUiShape.Control, color = MainUi.BlueSoft) {
                        Icon(Icons.Rounded.MeetingRoom, null, tint = MainUi.Blue, modifier = Modifier.padding(9.dp).size(21.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(sh("Oda oluştur", "Create room"), fontWeight = FontWeight.Bold, color = MainUi.Text, fontSize = 15.sp)
                        Text(
                            sh("PRO sahibi oda oluşturur; rakibin kodla katılır.", "A PRO member creates the room; your rival joins by code."),
                            fontSize = 10.sp,
                            color = MainUi.Muted,
                        )
                    }
                }

                MainGameButton(
                    text = sh("ÖZEL ODA OLUŞTUR", "CREATE PRIVATE ROOM"),
                    onClick = {
                        if (busy || room != null) return@MainGameButton
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
                    icon = Icons.Rounded.MeetingRoom,
                    modifier = Modifier.fillMaxWidth(),
                )

                room?.let { active ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MainUiShape.Card,
                        color = MainUi.SurfaceSoft,
                        border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .34f)),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(sh("ODA KODU", "ROOM CODE"), color = MainUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(active.code, color = MainUi.Gold, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(15.dp), color = MainUi.Blue, strokeWidth = 2.dp)
                                Spacer(Modifier.width(7.dp))
                                Text(sh("Rakip bekleniyor…", "Waiting for rival…"), color = MainUi.Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    MainDestructiveButton(
                        text = sh("ODAYI İPTAL ET", "CANCEL ROOM"),
                        onClick = {
                            if (busy) return@MainDestructiveButton
                            scope.launch {
                                busy = true
                                runCatching { service.cancel(active.id) }
                                room = null
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        MainGameCard {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MainUiShape.Control, color = MainUi.SurfaceSoft) {
                        Icon(Icons.Rounded.Key, null, tint = MainUi.Blue, modifier = Modifier.padding(9.dp).size(21.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(sh("Kodla katıl", "Join with code"), fontWeight = FontWeight.Bold, color = MainUi.Text, fontSize = 15.sp)
                        Text(sh("Oda sahibinin verdiği 6 karakterli kodu gir.", "Enter the 6-character code from the room host."), fontSize = 10.sp, color = MainUi.Muted)
                    }
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isLetterOrDigit).take(6).uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(sh("Oda kodu", "Room code")) },
                    leadingIcon = { Icon(Icons.Rounded.Key, null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = MainUiShape.Control,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MainUi.Blue,
                        unfocusedBorderColor = MainUi.Border,
                        focusedContainerColor = MainUi.Surface,
                        unfocusedContainerColor = MainUi.Surface,
                    ),
                )

                MainGameButton(
                    text = sh("ODAYA KATIL", "JOIN ROOM"),
                    onClick = {
                        if (busy || code.length != 6) return@MainGameButton
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        notice?.let {
            Surface(shape = MainUiShape.Control, color = MainUi.BlueSoft, border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .16f))) {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    color = MainUi.Text,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
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
        Surface(shape = MainUiShape.Card, color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MainUi.Text, textAlign = TextAlign.Center)
                Text(detail, color = MainUi.Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                MainSecondaryButton(sh("GERİ", "BACK"), onBack, Modifier.fillMaxWidth())
            }
        }
    }
}
