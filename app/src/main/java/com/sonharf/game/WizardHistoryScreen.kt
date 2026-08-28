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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.EquippedCosmeticsDto
import com.sonharf.game.data.MascotProgressDto
import com.sonharf.game.data.MascotRoomStateDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.equipShopItem
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getMascotProgress
import com.sonharf.game.data.getMascotRoomState
import kotlinx.coroutines.launch

@Composable
internal fun WizardHistoryScreen(
    backend: OnlineGameBackend?,
    onBack: () -> Unit,
    onOpenMascot: () -> Unit,
    onOpenShop: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { MascotSelectionRuntime.load(context) }
    var progress by remember { mutableStateOf<MascotProgressDto?>(null) }
    var roomState by remember { mutableStateOf<MascotRoomStateDto?>(null) }
    var ownedMascots by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var rosterLoaded by remember { mutableStateOf(false) }
    var rosterNotice by remember { mutableStateOf<String?>(null) }
    var rosterBusy by remember { mutableStateOf<String?>(null) }
    var selectedCharacter by remember { mutableStateOf<WizardLoreCharacter?>(null) }
    var selectedChapter by remember { mutableStateOf<WizardLoreChapter?>(null) }

    suspend fun reloadRoster() {
        val b = backend
        if (b == null) {
            rosterLoaded = true
            ownedMascots = emptySet()
            equipped = null
            return
        }
        runCatching {
            ownedMascots = b.getInventory()
            equipped = b.getEquippedCosmetics()
            equipped?.mascotId?.let { MascotSelectionRuntime.select(context, it) }
        }.onSuccess {
            rosterLoaded = true
            rosterNotice = null
        }.onFailure {
            rosterLoaded = false
            rosterNotice = sh("Mühür sahipliği şu an doğrulanamadı.", "Seal ownership could not be verified right now.")
        }
    }

    LaunchedEffect(MascotSelectionRuntime.selectedId) {
        if (backend != null) {
            progress = runCatching { backend.getMascotProgress(MascotSelectionRuntime.selectedId) }.getOrNull()
            roomState = runCatching { backend.getMascotRoomState(MascotSelectionRuntime.selectedId) }.getOrNull()
            reloadRoster()
        } else {
            progress = null
            roomState = null
            rosterLoaded = true
        }
    }

    val level = roomState?.friendshipLevel ?: progress?.level ?: 1
    val fragments = progress?.memoryFragments ?: 0

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(LetharaPalette.Night, LetharaPalette.Night2, Color(0xFF15113A)))
        )
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = LetharaPalette.Text)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("BÜYÜCÜLERİN GEÇMİŞİ", "PAST OF THE MAGES"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(sh("LETHARA ARŞİVİ", "LETHARA ARCHIVE"), color = LetharaPalette.Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    IconButton(onClick = onOpenMascot) {
                        Icon(Icons.Rounded.Stars, sh("Yoldaşım", "Companion"), tint = LetharaPalette.Cyan)
                    }
                }
            }
            item {
                LorePanel {
                    Text(if (SonHarfUiState.isEnglish) LetharaLore.introEn else LetharaLore.introTr, color = LetharaPalette.Text, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✦ " + sh("Hafıza Parçaları", "Memory Fragments"), color = LetharaPalette.Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text(fragments.toString() + " / 120", color = LetharaPalette.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (fragments / 120f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = LetharaPalette.Gold,
                        trackColor = Color.White.copy(alpha = .10f),
                    )
                }
            }
            item {
                Text(sh("ALTI MÜHÜR", "THE SIX SEALS"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(sh("Bir isme dokun; anlamını, mizacını ve unutulmuş yankısını aç.", "Tap a name to reveal its meaning, temperament and forgotten echo."), color = LetharaPalette.Muted, fontSize = 10.sp)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LetharaLore.characters.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { character ->
                                val rosterState = SealRosterPolicy.state(character, ownedMascots, equipped?.mascotId)
                                CharacterCard(
                                    character = character,
                                    rosterState = rosterState,
                                    rosterLoaded = rosterLoaded,
                                    modifier = Modifier.weight(1f),
                                ) { selectedCharacter = character }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Text(sh("HAFIZA BÖLÜMLERİ", "MEMORY CHAPTERS"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp) }
            items(LetharaLore.chapters, key = { it.id }) { chapter ->
                val unlocked = level >= chapter.unlockLevel
                LorePanel(
                    modifier = Modifier.clickable(enabled = unlocked) { selectedChapter = chapter },
                    border = if (unlocked) LetharaPalette.Cyan.copy(alpha=.45f) else Color.White.copy(alpha=.12f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (unlocked) LetharaPalette.Cyan.copy(alpha=.18f) else Color.White.copy(alpha=.06f)) {
                            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                if (unlocked) Text(chapter.order.toString(), color = LetharaPalette.Cyan, fontWeight = FontWeight.Black)
                                else Icon(Icons.Rounded.Lock, null, tint = LetharaPalette.Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (SonHarfUiState.isEnglish) chapter.titleEn else chapter.titleTr, color = if (unlocked) LetharaPalette.Text else LetharaPalette.Muted, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(
                                if (unlocked) {
                                    if (SonHarfUiState.isEnglish) chapter.summaryEn else chapter.summaryTr
                                } else {
                                    sh("Dostluk seviyesi " + chapter.unlockLevel + " olduğunda açılır.", "Unlocks at friendship level " + chapter.unlockLevel + ".")
                                },
                                color = LetharaPalette.Muted, fontSize = 10.sp, lineHeight = 14.sp,
                            )
                        }
                    }
                }
            }
            rosterNotice?.let { message ->
                item {
                    Text(message, modifier = Modifier.fillMaxWidth(), color = LetharaPalette.Cyan, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    selectedCharacter?.let { character ->
        val rosterState = SealRosterPolicy.state(character, ownedMascots, equipped?.mascotId)
        val mascotId = character.mascotId
        val assetReady = remember(mascotId) { mascotId != null && MascotCatalog.isAssetReady(context, mascotId) }
        AlertDialog(
            onDismissRequest = { if (rosterBusy == null) selectedCharacter = null },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TextButton(onClick = { selectedCharacter = null }, enabled = rosterBusy == null) {
                        Text(sh("Kapat", "Close"))
                    }
                    when {
                        rosterState.active -> Button(onClick = {
                            selectedCharacter = null
                            onOpenMascot()
                        }) {
                            Text(sh("YOLDAŞIMA GİT", "OPEN COMPANION"), fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        !rosterLoaded && mascotId != null && mascotId != MascotCatalog.DEFAULT_ID -> OutlinedButton(
                            onClick = {},
                            enabled = false,
                        ) {
                            Text(sh("DOĞRULANIYOR", "VERIFYING"), fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        rosterState.availability == SealRosterAvailability.STORE -> Button(onClick = {
                            selectedCharacter = null
                            onOpenShop()
                        }) {
                            Text(sh("MAĞAZADA AÇ", "OPEN IN SHOP"), fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        SealRosterPolicy.canEquip(rosterState) && mascotId != null && assetReady -> Button(
                            enabled = rosterBusy == null && rosterLoaded,
                            onClick = {
                                val b = backend
                                if (b == null) {
                                    rosterNotice = sh("Mühür seçimi için sunucu bağlantısı gerekli.", "A server connection is required to select a Seal.")
                                    return@Button
                                }
                                scope.launch {
                                    rosterBusy = mascotId
                                    runCatching { b.equipShopItem(mascotId) }
                                        .onSuccess {
                                            MascotSelectionRuntime.select(context, mascotId)
                                            equipped = b.getEquippedCosmetics()
                                            progress = runCatching { b.getMascotProgress(mascotId) }.getOrNull()
                                            roomState = runCatching { b.getMascotRoomState(mascotId) }.getOrNull()
                                            rosterNotice = sh(character.name + " aktif Mührün oldu.", character.name + " is now your active Seal.")
                                            selectedCharacter = null
                                        }
                                        .onFailure {
                                            rosterNotice = sh("Mühür etkinleştirilemedi.", "The Seal could not be equipped.")
                                        }
                                    rosterBusy = null
                                }
                            },
                        ) {
                            Text(if (rosterBusy == mascotId) "…" else sh("MÜHRÜ SEÇ", "SELECT SEAL"), fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                    }
                }
            },
            title = {
                Text(character.name + " — " + if (SonHarfUiState.isEnglish) character.titleEn else character.titleTr, color = LetharaPalette.Gold, fontWeight = FontWeight.Black)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (assetReady && mascotId != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(210.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = character.color.copy(alpha = .08f),
                            border = BorderStroke(1.dp, character.color.copy(alpha = .32f)),
                        ) {
                            MascotLive3DStage(
                                modifier = Modifier.fillMaxSize(),
                                mascotId = mascotId,
                                motion = MascotMotion.IDLE,
                            )
                        }
                    } else {
                        WizardEmblem(character, 86.dp)
                    }
                    SealStatusBadge(character, rosterState, rosterLoaded)
                    Text(if (SonHarfUiState.isEnglish) character.nameMeaningEn else character.nameMeaningTr, color = LetharaPalette.Text)
                    Text(if (SonHarfUiState.isEnglish) character.archetypeEn else character.archetypeTr, color = character.color, fontWeight = FontWeight.Bold)
                    Text(if (SonHarfUiState.isEnglish) character.temperamentEn else character.temperamentTr, color = LetharaPalette.Muted)
                    Text("“" + LetharaLore.randomWhisper(character, SonHarfUiState.language, fragments + character.name.length) + "”", color = LetharaPalette.Text, fontStyle = FontStyle.Italic)
                    if (rosterState.availability == SealRosterAvailability.AWAITING_3D) {
                        Text(
                            sh("Bu Mührün kimliği ve hikâyesi hazır; ayrı lisanslı 3D formu runtime testini geçmeden oynanabilir yapılmaz.", "This Seal's identity and story are ready; it will not become playable until its distinct licensed 3D form passes runtime validation."),
                            color = LetharaPalette.Muted,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            },
            containerColor = LetharaPalette.PanelStrong,
        )
    }

    selectedChapter?.let { chapter ->
        AlertDialog(
            onDismissRequest = { selectedChapter = null },
            confirmButton = { TextButton(onClick = { selectedChapter = null }) { Text(sh("Arşive dön", "Back to archive")) } },
            title = { Text(if (SonHarfUiState.isEnglish) chapter.titleEn else chapter.titleTr, color = LetharaPalette.Gold, fontWeight = FontWeight.Black) },
            text = { Text(if (SonHarfUiState.isEnglish) chapter.bodyEn else chapter.bodyTr, color = LetharaPalette.Text, lineHeight = 20.sp) },
            containerColor = LetharaPalette.PanelStrong,
        )
    }
}

@Composable
private fun LorePanel(
    modifier: Modifier = Modifier,
    border: Color = LetharaPalette.Gold.copy(alpha = .35f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = LetharaPalette.Panel,
        border = BorderStroke(1.dp, border),
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun CharacterCard(
    character: WizardLoreCharacter,
    rosterState: SealRosterState,
    rosterLoaded: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mascotId = character.mascotId
    val assetReady = remember(mascotId) { mascotId != null && MascotCatalog.isAssetReady(context, mascotId) }
    Surface(
        modifier = modifier.height(160.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = LetharaPalette.Panel,
        border = BorderStroke(if (rosterState.active) 1.8.dp else 1.dp, if (rosterState.active) LetharaPalette.Gold else character.color.copy(alpha=.55f)),
    ) {
        Column(Modifier.fillMaxSize().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            WizardEmblem(character, 58.dp)
            if (assetReady) {
                Text(sh("3D FORM HAZIR", "3D FORM READY"), color = character.color, fontSize = 6.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(5.dp))
            Text(character.name.uppercase(), color = LetharaPalette.Text, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(if (SonHarfUiState.isEnglish) character.titleEn else character.titleTr, color = character.color, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            SealStatusBadge(character, rosterState, rosterLoaded)
        }
    }
}

@Composable
private fun SealStatusBadge(
    character: WizardLoreCharacter,
    rosterState: SealRosterState,
    rosterLoaded: Boolean,
) {
    val text = when {
        rosterState.active -> sh("AKTİF MÜHÜR", "ACTIVE SEAL")
        rosterState.availability == SealRosterAvailability.AWAITING_3D ->
            sh("3D FORM BEKLENİYOR", "AWAITING 3D FORM")
        !rosterLoaded && character.mascotId != MascotCatalog.DEFAULT_ID ->
            sh("DOĞRULANIYOR", "VERIFYING")
        rosterState.availability == SealRosterAvailability.FREE ->
            sh("ÜCRETSİZ", "FREE")
        rosterState.availability == SealRosterAvailability.OWNED ->
            sh("SAHİPSİN", "OWNED")
        else -> "◈ " + (rosterState.plannedPrice ?: SealRosterPolicy.plannedPrice(character.key)) + " SC"
    }
    val color = when {
        rosterState.active -> LetharaPalette.Gold
        rosterState.availability == SealRosterAvailability.AWAITING_3D -> LetharaPalette.Muted
        rosterState.availability == SealRosterAvailability.OWNED || rosterState.availability == SealRosterAvailability.FREE -> LetharaPalette.Green
        else -> character.color
    }
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = color.copy(alpha = .10f),
        border = BorderStroke(1.dp, color.copy(alpha = .32f)),
    ) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = color, fontSize = 7.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WizardEmblem(character: WizardLoreCharacter, size: Dp) {
    Surface(shape = CircleShape, color = character.color.copy(alpha=.15f), border = BorderStroke(1.5.dp, character.color.copy(alpha=.7f))) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AutoStories, null, tint = character.color, modifier = Modifier.size(size * .48f))
        }
    }
}
