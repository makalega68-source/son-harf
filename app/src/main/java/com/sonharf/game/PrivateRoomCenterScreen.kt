package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
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
            detail = sh(
                "Özel oda için sunucu bağlantısı gerekir.",
                "Private rooms require a server connection.",
            ),
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
            if (
                latest.status in setOf("playing", "quiz", "final", "sudden_death") &&
                latest.guestId != null
            ) {
                onRoomReady(latest.language)
                break
            }
            if (latest.status != "waiting") break
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        GameTopBar(
            title = sh("PRO Özel Oda", "PRO Private Room"),
            subtitle = sh(
                "Son Harf için davet kodlu özel düello",
                "Invite-code private duel for Last Letter",
            ),
            onBack = ::leave,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameSurface(
                elevated = true,
                borderColor = GameColors.RewardAmber.copy(alpha = .36f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.RewardAmber.copy(alpha = .12f),
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = GameColors.RewardAmber,
                            modifier = Modifier.padding(9.dp).size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("ODA OLUŞTUR", "CREATE ROOM"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            sh(
                                "PRO sahibi oda oluşturur. Rakibin aşağıdaki kodla katılabilir.",
                                "A PRO member creates the room. Your rival can join with its code.",
                            ),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

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
                                        "vip_required" in error.message.orEmpty() -> sh(
                                            "Özel oda PRO üyeliği gerektirir.",
                                            "Private rooms require PRO.",
                                        )
                                        "player_already_in_game" in error.message.orEmpty() -> sh(
                                            "Önce aktif maçını tamamla.",
                                            "Finish your active match first.",
                                        )
                                        else -> sh(
                                            "Özel oda oluşturulamadı.",
                                            "Private room could not be created.",
                                        )
                                    }
                                }
                            busy = false
                        }
                    },
                    enabled = !busy && room == null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = GameShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.PrimaryBlue,
                        contentColor = GameColors.TextPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.MeetingRoom, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        sh("ÖZEL ODA OLUŞTUR", "CREATE PRIVATE ROOM"),
                        fontWeight = FontWeight.Black,
                    )
                }

                room?.let { active ->
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = GameShapes.Medium,
                        color = GameColors.ElevatedBackground,
                        border = BorderStroke(
                            1.dp,
                            GameColors.RewardAmber.copy(alpha = .42f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                sh("ODA KODU", "ROOM CODE"),
                                color = GameColors.TextTertiary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                active.code,
                                color = GameColors.RewardAmber,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = GameColors.PrimaryBlue,
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    sh("Rakip bekleniyor…", "Waiting for rival…"),
                                    color = GameColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                        shape = GameShapes.Medium,
                        border = BorderStroke(1.dp, GameColors.Border),
                    ) {
                        Text(
                            sh("ODAYI İPTAL ET", "CANCEL ROOM"),
                            color = GameColors.TextSecondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            GameSurface(
                borderColor = GameColors.TacticalTurquoise.copy(alpha = .34f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.TacticalTurquoise.copy(alpha = .11f),
                    ) {
                        Icon(
                            Icons.Rounded.Key,
                            contentDescription = null,
                            tint = GameColors.TacticalTurquoise,
                            modifier = Modifier.padding(9.dp).size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("KODLA KATIL", "JOIN WITH CODE"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            sh(
                                "Oda sahibinin verdiği 6 karakterli kodu gir.",
                                "Enter the 6-character code from the room host.",
                            ),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.filter(Char::isLetterOrDigit).take(6).uppercase()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(sh("Oda kodu", "Room code")) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Key,
                            contentDescription = null,
                            tint = GameColors.TacticalTurquoise,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GameColors.TextPrimary,
                        unfocusedTextColor = GameColors.TextPrimary,
                        focusedBorderColor = GameColors.TacticalTurquoise,
                        unfocusedBorderColor = GameColors.Border,
                        focusedLabelColor = GameColors.TacticalTurquoise,
                        unfocusedLabelColor = GameColors.TextTertiary,
                        cursorColor = GameColors.TacticalTurquoise,
                    ),
                )

                Spacer(Modifier.height(10.dp))

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
                                        "room_not_available" in error.message.orEmpty() -> sh(
                                            "Bu oda bulunamadı veya artık açık değil.",
                                            "This room was not found or is no longer open.",
                                        )
                                        "player_already_in_game" in error.message.orEmpty() -> sh(
                                            "Önce aktif maçını tamamla.",
                                            "Finish your active match first.",
                                        )
                                        else -> sh(
                                            "Odaya katılınamadı.",
                                            "Could not join the room.",
                                        )
                                    }
                                }
                            busy = false
                        }
                    },
                    enabled = !busy && code.length == 6,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = GameShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.TacticalTurquoise,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        sh("ODAYA KATIL", "JOIN ROOM"),
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            notice?.let {
                Surface(
                    shape = GameShapes.Medium,
                    color = GameColors.PrimaryBlue.copy(alpha = .10f),
                    border = BorderStroke(
                        1.dp,
                        GameColors.PrimaryBlue.copy(alpha = .22f),
                    ),
                ) {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    detail: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GameSurface(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GameColors.PrimaryBlue.copy(alpha = .30f),
            elevated = true,
        ) {
            Text(
                title,
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                detail,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = GameShapes.Medium,
                border = BorderStroke(1.dp, GameColors.PrimaryBlue),
            ) {
                Text(
                    sh("GERİ", "BACK"),
                    color = GameColors.PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
