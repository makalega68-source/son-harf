package com.sonharf.game

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Where an invite can be sent. Apps are opened directly; a web link or the share sheet is the fallback. */
internal enum class InviteChannel(
    val label: String,
    val badge: String,
    val color: Long,
    val packages: List<String>,
) {
    WHATSAPP("WhatsApp", "💬", 0xFF25D366, listOf("com.whatsapp", "com.whatsapp.w4b")),
    TELEGRAM("Telegram", "✈️", 0xFF229ED9, listOf("org.telegram.messenger")),
    INSTAGRAM("Instagram", "📸", 0xFFE1306C, listOf("com.instagram.android")),
    MESSENGER("Messenger", "⚡", 0xFF0084FF, listOf("com.facebook.orca")),
    SMS("SMS", "✉️", 0xFF34A853, emptyList()),
    EMAIL("E-posta", "📧", 0xFFEA4335, emptyList()),
    COPY("Kopyala", "🔗", 0xFF6B7A90, emptyList()),
    MORE("Diğer", "➕", 0xFF8B6CF0, emptyList()),
}

internal object SonHarfInvite {
    const val STORE_LINK = "https://play.google.com/store/apps/details?id=com.sonharf.game"

    fun message(playerName: String?): String {
        val name = playerName?.trim()?.takeIf { it.isNotBlank() }
        return if (SonHarfUiState.language == "en") {
            (if (name != null) "$name invites you to Word Siege! 🎮\n" else "Join me on Word Siege! 🎮\n") +
                "Word duels, territory battles and daily puzzles. Can you beat me? ⚔️\n$STORE_LINK"
        } else {
            (if (name != null) "$name seni Kelime Kuşatması'na davet ediyor! 🎮\n" else "Kelime Kuşatması'nda benimle oyna! 🎮\n") +
                "Kelime düelloları, alan savaşları ve günlük bulmacalar. Beni yenebilir misin? ⚔️\n$STORE_LINK"
        }
    }

    private fun sendTo(context: Context, text: String, packageName: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun openUri(context: Context, uri: Uri): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun chooser(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, sh("Arkadaşını davet et", "Invite a friend")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun share(context: Context, channel: InviteChannel, playerName: String?) {
        val text = message(playerName)
        val encoded = Uri.encode(text)
        val handled = when (channel) {
            InviteChannel.WHATSAPP ->
                channel.packages.any { sendTo(context, text, it) } ||
                    openUri(context, Uri.parse("https://wa.me/?text=$encoded"))
            InviteChannel.TELEGRAM ->
                channel.packages.any { sendTo(context, text, it) } ||
                    openUri(context, Uri.parse("https://t.me/share/url?url=${Uri.encode(STORE_LINK)}&text=$encoded"))
            InviteChannel.INSTAGRAM, InviteChannel.MESSENGER ->
                channel.packages.any { sendTo(context, text, it) }
            InviteChannel.SMS -> try {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
            InviteChannel.EMAIL -> try {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_SUBJECT, sh("Kelime Tahtı daveti", "Word Board invite"))
                        .putExtra(Intent.EXTRA_TEXT, text)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
            InviteChannel.COPY -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Kelime Tahtı", text))
                Toast.makeText(context, sh("Davet bağlantısı kopyalandı", "Invite link copied"), Toast.LENGTH_SHORT).show()
                true
            }
            InviteChannel.MORE -> false
        }
        // The app is not installed (or MORE was chosen): let the player pick from the share sheet.
        if (!handled) chooser(context, text)
    }
}

/** Home-screen invite card: opens the invite sheet with WhatsApp and other channels. */
@Composable
internal fun InviteFriendsCard(playerName: String?, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Surface(
        onClick = { open = true },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF14B8B0), Color(0xFF3E7BFA), Color(0xFF8B6CF0))))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).background(Color.White.copy(alpha = .22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.GroupAdd, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("ARKADAŞINI DAVET ET", "INVITE A FRIEND"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(
                    sh("WhatsApp, Telegram, SMS ve daha fazlasıyla paylaş", "Share via WhatsApp, Telegram, SMS and more"),
                    color = Color.White.copy(alpha = .88f),
                    fontSize = 12.sp,
                )
            }
            Surface(shape = RoundedCornerShape(99.dp), color = Color.White) {
                Text(sh("DAVET", "INVITE"), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color(0xFF3E4FB8), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
    if (open) InviteFriendsSheet(playerName = playerName, onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteFriendsSheet(playerName: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(sh("Arkadaşını davet et", "Invite a friend"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0B1B33))
            Text(
                sh("Bir kanal seç; davet mesajı hazır gelir.", "Pick a channel; the invite message is ready to send."),
                fontSize = 12.sp,
                color = Color(0xFF5B6478),
            )
            Spacer(Modifier.height(16.dp))
            InviteChannel.entries.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { channel ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                onClick = {
                                    SonHarfInvite.share(context, channel, playerName)
                                    onDismiss()
                                },
                                shape = CircleShape,
                                color = Color(channel.color),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(58.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(channel.badge, fontSize = 24.sp) }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                when (channel) {
                                    InviteChannel.EMAIL -> sh("E-posta", "Email")
                                    InviteChannel.COPY -> sh("Kopyala", "Copy")
                                    InviteChannel.MORE -> sh("Diğer", "More")
                                    else -> channel.label
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B1B33),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
