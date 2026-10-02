package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
internal fun PlayerInviteCard(backend:OnlineGameBackend) {
    var own by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    LaunchedEffect(Unit) { gameRequestResult { backend.getInviteCode() }.onSuccess { own=it;SonHarfInvite.playerCode=it }.onFailure { notice=sh("Davet kodu alınamadı", "Invite code unavailable") } }
    ActivityTile(sh("ARKADAŞ KODU", "FRIEND CODE")) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(own ?: "—",color=Hf.Text,fontWeight=FontWeight.Bold)
            TextButton(enabled=own!=null,onClick={SonHarfInvite.share(context,InviteChannel.COPY,null)}) { Text(sh("KOPYALA", "COPY")) }
        }
        OutlinedTextField(value=entered,onValueChange={entered=it.take(100)},singleLine=true,modifier=Modifier.fillMaxWidth(),label={Text(sh("Arkadaşının kodu", "Friend's code"))})
        Button(enabled=!busy && entered.isNotBlank(),onClick={scope.launch {
            busy=true
            val code=PlayerLinks.parse(entered.trim())?.takeIf { it.kind=="invite" }?.id ?: entered.trim().uppercase()
            gameRequestResult { backend.useInviteCode(code) }.onSuccess { notice=sh("Arkadaşlık isteği gönderildi", "Friend request sent");entered="" }
                .onFailure { notice=when { it.message.orEmpty().contains("cannot_friend_self")->sh("Bu senin kodun", "This is your code")
                    it.message.orEmpty().contains("invalid_invite_code")->sh("Davet kodu bulunamadı", "Invite code not found")
                    else->sh("İstek gönderilemedi", "Could not send request") } }
            busy=false
        }}) { Text(if(busy) "…" else sh("ARKADAŞ EKLE", "ADD FRIEND")) }
        notice?.let { Text(it,color=Hf.TextMuted) }
    }
}
