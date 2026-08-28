package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.MascotRoomItemDto
import com.sonharf.game.data.MascotRoomStateDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.careForMascotV2
import com.sonharf.game.data.getMascotRoomCatalog
import com.sonharf.game.data.getMascotRoomState
import com.sonharf.game.data.setMascotRoomItem
import kotlinx.coroutines.launch

@Composable
internal fun MascotRoomScreen(
    backend: OnlineGameBackend?,
    onBack: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { MascotSelectionRuntime.load(context) }

    val mascotId = MascotSelectionRuntime.selectedId
    val character = LetharaLore.characterForMascot(mascotId)
    var state by remember(mascotId) { mutableStateOf<MascotRoomStateDto?>(null) }
    var roomItems by remember { mutableStateOf<List<MascotRoomItemDto>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend ?: return
        state = runCatching { b.getMascotRoomState(mascotId) }.getOrNull()
        roomItems = runCatching { b.getMascotRoomCatalog() }.getOrDefault(emptyList())
    }

    LaunchedEffect(mascotId) { reload() }

    val selectedItem = state?.selectedRoomItem ?: "star_window"
    val accent = roomAccent(selectedItem, character.color)
    val friendshipLevel = state?.friendshipLevel ?: 1
    val friendshipXp = state?.friendshipXp ?: 0
    val levelProgress = (friendshipXp % 40) / 40f

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF050B1D), accent.copy(alpha = .28f), LetharaPalette.Night)
            )
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = LetharaPalette.Text)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("MÜHÜR ODASI", "SEAL ROOM"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        Text(sh("MASKOT EVİ • LETHARA", "MASCOT HOME • LETHARA"), color = LetharaPalette.Muted, fontSize = 9.sp, letterSpacing = 1.3.sp)
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Rounded.AutoStories, sh("Hikâye", "Story"), tint = LetharaPalette.Cyan)
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xCC0B1530),
                    border = BorderStroke(1.dp, accent.copy(alpha = .65f)),
                ) {
                    Box(Modifier.fillMaxWidth().height(330.dp)) {
                        RoomDecorationBackdrop(selectedItem, accent)
                        MascotLive3DStage(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
                            mascotId = mascotId,
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xD90B1530),
                            border = BorderStroke(1.dp, accent.copy(alpha = .45f)),
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(character.name.uppercase(), color = LetharaPalette.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text(if (SonHarfUiState.isEnglish) character.titleEn else character.titleTr, color = character.color, fontSize = 9.sp)
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(11.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xE60B1530),
                        ) {
                            Text(
                                roomLine(selectedItem),
                                Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                color = LetharaPalette.Text,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LetharaPalette.Panel,
                    border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha = .36f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFFF8BCB), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(sh("DOSTLUK SEVİYESİ", "FRIENDSHIP LEVEL") + " " + friendshipLevel, color = LetharaPalette.Text, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Text((friendshipXp % 40).toString() + "/40", color = LetharaPalette.Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(
                            progress = { levelProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = Color(0xFFFF8BCB),
                            trackColor = Color.White.copy(alpha = .09f),
                        )
                        Text(
                            sh(
                                "Dostluk seviyesi oda dekorlarını ve hikâye katmanlarını açar; rekabet gücü vermez.",
                                "Friendship unlocks room decorations and story layers; it never grants competitive power.",
                            ),
                            color = LetharaPalette.Muted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }

            item {
                Text(sh("BUGÜNKÜ BAĞ", "TODAY'S BOND"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    BondAction(sh("SEV", "LOVE"), "❤", state?.lovedToday == true, Modifier.weight(1f), !busy) {
                        runCareAction(scope, backend, mascotId, "love", { busy = it }, { notice = it }, { state = it })
                    }
                    BondAction(sh("OYNA", "PLAY"), "✦", state?.playedToday == true, Modifier.weight(1f), !busy) {
                        runCareAction(scope, backend, mascotId, "play", { busy = it }, { notice = it }, { state = it })
                    }
                    BondAction(sh("BAKIM", "GROOM"), "✨", state?.groomedToday == true, Modifier.weight(1f), !busy) {
                        runCareAction(scope, backend, mascotId, "groom", { busy = it }, { notice = it }, { state = it })
                    }
                }
                if (state?.dailyBondCompleted == true) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        sh("✓ Günlük bağ tamamlandı; bonus yalnızca ilk tamamlamada verilir.", "✓ Daily bond complete; the bonus is awarded only on the first completion."),
                        color = LetharaPalette.Green,
                        fontSize = 9.sp,
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("ODA MÜHÜRLERİ", "ROOM SEALS"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(sh("Seçili", "Selected") + ": " + roomShortName(selectedItem), color = LetharaPalette.Muted, fontSize = 9.sp)
                }
            }

            items(roomItems, key = { it.id }) { roomItem ->
                val unlocked = friendshipLevel >= roomItem.unlockFriendshipLevel
                val selected = selectedItem == roomItem.id
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = unlocked && !busy) {
                        val b = backend ?: return@clickable
                        scope.launch {
                            busy = true
                            notice = null
                            runCatching { b.setMascotRoomItem(mascotId, roomItem.id) }
                                .onSuccess {
                                    state = it
                                    MascotRuntime.react(MascotMotion.GREETING)
                                    notice = sh("Oda mührü değiştirildi.", "Room seal changed.")
                                }
                                .onFailure {
                                    notice = sh("Bu oda mührü henüz açılamadı.", "This room seal cannot be opened yet.")
                                }
                            busy = false
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) accent.copy(alpha = .18f) else LetharaPalette.Panel,
                    border = BorderStroke(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) accent else Color.White.copy(alpha = .10f),
                    ),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = accent.copy(alpha = .13f)) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Text(roomItem.icon, fontSize = 24.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (SonHarfUiState.isEnglish) roomItem.nameEn else roomItem.nameTr,
                                color = if (unlocked) LetharaPalette.Text else LetharaPalette.Muted,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                            )
                            Text(
                                if (SonHarfUiState.isEnglish) roomItem.descriptionEn else roomItem.descriptionTr,
                                color = LetharaPalette.Muted,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            selected -> Text(sh("AKTİF", "ACTIVE"), color = LetharaPalette.Green, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            unlocked -> Text(sh("SEÇ", "SELECT"), color = LetharaPalette.Cyan, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            else -> Text(sh("SV.", "LV.") + " " + roomItem.unlockFriendshipLevel, color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, LetharaPalette.Gold),
                    ) {
                        Icon(Icons.Rounded.AutoStories, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(sh("HİKÂYE", "STORY"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                    Button(
                        onClick = onOpenCompanion,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LetharaPalette.Violet),
                    ) {
                        Icon(Icons.Rounded.Pets, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(sh("YOLDAŞIM", "COMPANION"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }

            notice?.let {
                item {
                    Text(it, modifier = Modifier.fillMaxWidth(), color = LetharaPalette.Cyan, textAlign = TextAlign.Center, fontSize = 10.sp)
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

private fun runCareAction(
    scope: kotlinx.coroutines.CoroutineScope,
    backend: OnlineGameBackend?,
    mascotId: String,
    action: String,
    onBusy: (Boolean) -> Unit,
    onNotice: (String?) -> Unit,
    onState: (MascotRoomStateDto) -> Unit,
) {
    val b = backend ?: return
    scope.launch {
        onBusy(true)
        onNotice(null)
        runCatching { b.careForMascotV2(mascotId, action) }
            .onSuccess { careResult ->
                MascotRuntime.react(
                    when (action) {
                        "play" -> MascotMotion.RUN
                        "love" -> MascotMotion.LOOK_AT_PLAYER
                        else -> MascotMotion.GREETING
                    }
                )
                val room = runCatching { b.getMascotRoomState(mascotId) }.getOrNull()
                if (room != null) onState(room)
                onNotice(
                    if (careResult.dailyBonusAwarded) {
                        sh("+${careResult.friendshipGained} Dostluk XP • Günlük bağ bonusu!", "+${careResult.friendshipGained} Friendship XP • Daily bond bonus!")
                    } else {
                        sh("+${careResult.friendshipGained} Dostluk XP", "+${careResult.friendshipGained} Friendship XP")
                    }
                )
            }
            .onFailure {
                onNotice(sh("Yoldaş etkileşimi tamamlanamadı.", "Companion interaction could not be completed."))
            }
        onBusy(false)
    }
}

@Composable
private fun BondAction(
    label: String,
    icon: String,
    done: Boolean,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        enabled = enabled,
        border = BorderStroke(1.dp, if (done) LetharaPalette.Green else LetharaPalette.Cyan.copy(alpha = .55f)),
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 5.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (done) "✓" else icon, fontSize = 16.sp)
            Text(label, fontWeight = FontWeight.Black, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RoomDecorationBackdrop(selectedItem: String, accent: Color) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(accent.copy(alpha = .22f), Color.Transparent, Color(0xAA020716)))
        )
    ) {
        Text("✦   ·   ✧", color = Color.White.copy(alpha = .36f), fontSize = 18.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp))
        when (selectedItem) {
            "seal_rug" -> Text("◇  ◇  ◇", color = accent.copy(alpha = .72f), fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
            "moon_lantern" -> Text("☾", color = LetharaPalette.Gold.copy(alpha = .92f), fontSize = 52.sp, modifier = Modifier.align(Alignment.TopEnd).padding(24.dp))
            "memory_book" -> Text("📖", fontSize = 42.sp, modifier = Modifier.align(Alignment.BottomStart).padding(24.dp))
            "memory_crystal" -> Text("🔮", fontSize = 48.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp))
            "celestial_gate" -> {
                Text("✧", color = accent, fontSize = 64.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
                Text("✧", color = accent, fontSize = 64.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp))
            }
            else -> Text("✦", color = accent.copy(alpha = .72f), fontSize = 48.sp, modifier = Modifier.align(Alignment.TopEnd).padding(22.dp))
        }
    }
}

private fun roomAccent(id: String, fallback: Color): Color = when (id) {
    "seal_rug" -> Color(0xFFFFC66B)
    "moon_lantern" -> Color(0xFFB18CFF)
    "memory_book" -> Color(0xFF64D6FF)
    "memory_crystal" -> Color(0xFFBE7CFF)
    "celestial_gate" -> Color(0xFFFFD36A)
    "star_window" -> Color(0xFF7EDBFF)
    else -> fallback
}

private fun roomShortName(id: String): String = when (id) {
    "seal_rug" -> sh("Mühür Halısı", "Seal Rug")
    "moon_lantern" -> sh("Ay Feneri", "Moon Lantern")
    "memory_book" -> sh("Hafıza Kitabı", "Memory Book")
    "memory_crystal" -> sh("Hafıza Kristali", "Memory Crystal")
    "celestial_gate" -> sh("Göksel Geçit", "Celestial Gate")
    else -> sh("Yıldız Penceresi", "Star Window")
}

private fun roomLine(id: String): String = when (id) {
    "seal_rug" -> sh("Altı Mühür'ün işaretleri odanın zemininde yeniden birleşiyor.", "The marks of the Six Seals reunite beneath the room.")
    "moon_lantern" -> sh("Mor Ay'ın ışığı eski anıları rahatsız etmeden aydınlatıyor.", "Violet Moon light illuminates old memories without disturbing them.")
    "memory_book" -> sh("Açık sayfalarda yalnızca geri kazanılmış anılar yazıyor.", "Only recovered memories appear on the open pages.")
    "memory_crystal" -> sh("Kristal, dostluk arttıkça daha güçlü bir yankı veriyor.", "The crystal echoes more strongly as friendship grows.")
    "celestial_gate" -> sh("Göksel Geçit güç değil, Hatırlatıcı ile kurulan bağın prestij işaretidir.", "The Celestial Gate is prestige from companionship, not power.")
    else -> sh("Yıldız Penceresi Lethara'nın gecesini sessizce odaya taşıyor.", "The Star Window quietly brings Lethara's night into the room.")
}
