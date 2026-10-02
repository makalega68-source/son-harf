package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto

/** An entry into existing friends/invitations; never invents presence or sends an invitation. */
@Composable
internal fun HomeSocialArena(backend: OnlineGameBackend, incomingCount: Int, onSocial: () -> Unit) {
    var friends by remember { mutableStateOf<List<ProfileDto>?>(null) }
    LaunchedEffect(backend) {
        friends = gameRequestResult { backend.getFriends().map { it.second } }.getOrNull()
    }
    val available = friends.orEmpty().filter { it.presenceStatus == "online" }
    Surface(shape = RoundedCornerShape(22.dp), color = Hf.Surface,
        border = BorderStroke(1.dp, Hf.Gold.copy(alpha = .6f)), shadowElevation = 3.dp) {
        Column(Modifier.background(Brush.horizontalGradient(listOf(lerp(Hf.Surface, Hf.Green, .10f), lerp(Hf.Surface, Hf.Gold, .12f))))
            .padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Group, null, tint = Hf.Green, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("BİRLİKTE OYNA", "PLAY TOGETHER"), color = Hf.Text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(when {
                        incomingCount > 0 -> sh("$incomingCount yeni istek veya maç daveti", "$incomingCount new requests or invitations")
                        friends == null -> sh("Arkadaşlarını bul, rekabeti paylaş", "Find friends, share the rivalry")
                        available.isNotEmpty() -> sh("${available.size} arkadaşın oynamaya hazır", "${available.size} friends ready to play")
                        else -> sh("Arkadaş ekle, maçta yeniden buluş", "Add friends, meet again in a match")
                    }, color = Hf.TextMuted, fontSize = 11.sp)
                }
                if (incomingCount > 0) Surface(color = Hf.Green, shape = CircleShape) {
                    Text(if (incomingCount > 9) "9+" else "$incomingCount", Modifier.padding(7.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            if (available.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                available.take(3).forEach { friend ->
                    Box(Modifier.padding(end = 5.dp)) {
                        ProfilePhotoAvatarWithGender(friend.avatarPath, friend.gender, friend.displayName, 32.dp,
                            accent = Hf.Green, visible = friend.avatarVisibility != "hidden", frameId = rememberPlayerFrame(friend.id))
                        Box(Modifier.align(Alignment.BottomEnd).size(8.dp).background(Hf.Green, CircleShape))
                    }
                }
                Text(available.take(2).joinToString(" · ") { it.displayName }, color = Hf.Text, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            Button(onClick = onSocial, modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Hf.Green)) {
                Icon(if (incomingCount > 0 || available.isNotEmpty()) Icons.Rounded.Group else Icons.Rounded.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (incomingCount > 0) sh("DAVETLERİ GÖR", "VIEW INVITATIONS") else sh("ARKADAŞLARLA OYNA", "PLAY WITH FRIENDS"), fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}
