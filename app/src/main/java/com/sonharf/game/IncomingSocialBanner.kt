package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Watches for incoming friend requests and Kelime Tahtı invitations while the player is on
 * any main page, so a request no longer waits unseen inside the Friends page.
 * Reports the count for the bottom-bar badge and shows a short banner for each new request.
 */
@Composable
internal fun IncomingSocialWatcher(
    backend: OnlineGameBackend,
    enabled: Boolean,
    onCount: (Int) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onAcceptedSiege: (WordSiegeGameDto) -> Unit = {},
) {
    var seen by remember { mutableStateOf<Set<String>?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    val openAccepted by androidx.compose.runtime.rememberUpdatedState(onAcceptedSiege)

    // The sender also enters the room when their friend accepts. Watching only incoming
    // invitations left the sender in the lobby while the server marked both as in-game.
    LaunchedEffect(enabled, WordSiegeLaunchConfig.awaitedInviteRevision) {
        if (!enabled || !SupabaseProvider.configured) return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            val outgoing = runCatching { backend.getOutgoingWordSiegeInvites() }.getOrNull()
            val series = runCatching { backend.getOutgoingWordSiegeSeriesInvites() }.getOrNull()
            val states = outgoing.orEmpty().map { Triple(it.id, it.status, it.gameId) } +
                series.orEmpty().map { Triple(it.id, it.status, it.gameId) }
            states.filter { it.second == "pending" }.forEach { WordSiegeLaunchConfig.awaitInvite(it.first) }
            val accepted = states.firstOrNull {
                it.first in WordSiegeLaunchConfig.awaitedInviteIds && it.second == "accepted" && it.third != null
            }
            if (accepted != null) {
                val game = runCatching { backend.getWordSiegeGame(requireNotNull(accepted.third)) }.getOrNull()
                if (game != null) {
                    WordSiegeLaunchConfig.awaitedInviteIds.remove(accepted.first)
                    if (game.status == "playing") openAccepted(game)
                }
            }
            states.filter { it.second in listOf("declined", "expired", "cancelled") }.forEach {
                WordSiegeLaunchConfig.awaitedInviteIds.remove(it.first)
            }
            delay(if (WordSiegeLaunchConfig.awaitedInviteIds.isEmpty()) 10_000L else 2_000L)
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled || !SupabaseProvider.configured) return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            if (backend.currentUserId() != null) {
                val requests = runCatching { backend.getIncomingFriendRequests() }.getOrNull()
                val invites = runCatching { backend.getIncomingWordSiegeInvites() }.getOrNull()
                if (requests != null || invites != null) {
                    val requestKeys = requests.orEmpty().associate { (friendship, profile) ->
                        "friend:${friendship.requestedBy}:${friendship.createdAt}" to
                            sh("${profile.displayName} sana arkadaşlık isteği gönderdi.", "${profile.displayName} sent you a friend request.")
                    }
                    val inviteKeys = invites.orEmpty().associate { invite ->
                        "siege:${invite.id}" to sh("Yeni bir Kelime Tahtı davetin var.", "You have a new Kelime Tahtı invitation.")
                    }
                    val all = requestKeys + inviteKeys
                    onCount(all.size)
                    val previous = seen
                    if (previous != null) {
                        all.entries.firstOrNull { it.key !in previous }?.let { fresh ->
                            banner = fresh.value
                            SonHarfSoundFx.softNotify()
                        }
                    }
                    seen = (previous.orEmpty() + all.keys)
                }
            }
            delay(15_000)
        }
    }

    LaunchedEffect(banner) {
        if (banner != null) {
            delay(8_000)
            banner = null
        }
    }

    val text = banner
    if (enabled && text != null) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MainUi.Surface,
            border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .45f)),
            shadowElevation = 6.dp,
        ) {
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GroupAdd, null, tint = MainUi.Blue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text,
                        color = MainUi.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = {
                    banner = null
                    onOpen()
                }) {
                    Text(sh("GÖR", "VIEW"), color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                IconButton(onClick = { banner = null }) {
                    Icon(Icons.Rounded.Close, sh("Kapat", "Close"), tint = MainUi.Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
