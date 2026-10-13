package com.sonharf.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.EveStatus
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.ThroneBackend
import io.github.jan.supabase.auth.auth

/**
 * Who has Eve. She is never sold: she is earned from the weekly throne podium (shards counted on
 * the server) and admins have her automatically. The last answer is kept on the device for the
 * signed-in account, so she appears at once on the next launch.
 */
internal object EveOwnership {
    private const val PREFS = "eve_companion"
    private const val KEY_OWNER = "owner_id"
    private const val KEY_HOME = "show_on_home"

    var status by mutableStateOf<EveStatus?>(null)
        private set
    private var statusUserId: String? = null
    private var cachedOwner by mutableStateOf<String?>(null)
    var homeEnabled by mutableStateOf(true)
        private set
    private var restored = false
    private var loadedAt = 0L

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun restore(context: Context) {
        if (restored) return
        restored = true
        val p = prefs(context)
        cachedOwner = p.getString(KEY_OWNER, null)
        homeEnabled = p.getBoolean(KEY_HOME, true)
    }

    /** Whether [userId] has Eve: the server's answer, the remembered one, or the admin list. */
    fun owns(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return (statusUserId == userId && status?.owned == true) || cachedOwner == userId || isAdmin(userId)
    }

    /** The current account's Eve entitlement, used by the existing three-hint match system. */
    fun ownsCurrentUser(): Boolean {
        val userId = runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.id }.getOrNull()
        return owns(userId)
    }

    /** Eve walks the home screen instead of the floating mascot (never both at once). */
    fun showOnHome(userId: String?): Boolean = owns(userId) && homeEnabled

    fun setHomeEnabled(context: Context, enabled: Boolean) {
        homeEnabled = enabled
        prefs(context).edit().putBoolean(KEY_HOME, enabled).apply()
    }

    suspend fun refresh(context: Context, userId: String?, force: Boolean = false) {
        restore(context)
        if (statusUserId != userId) {
            statusUserId = userId
            status = null
            loadedAt = 0L
        }
        if (!SupabaseProvider.configured || userId.isNullOrBlank()) return
        val now = System.currentTimeMillis()
        if (!force && now - loadedAt < 60_000L) return
        gameRequestResult { ThroneBackend.eveStatus() }.onSuccess { s ->
            if (statusUserId != userId) return@onSuccess
            status = s
            loadedAt = now
            cachedOwner = if (s.owned) userId else null
            prefs(context).edit().putString(KEY_OWNER, cachedOwner).apply()
        }
    }
}

/** Eve's memory on this device, for [EveMind]. */
internal class PrefsEveMemory(context: Context) : EveMemory {
    private val p = context.applicationContext.getSharedPreferences("eve_mind", Context.MODE_PRIVATE)
    override fun getInt(key: String, default: Int) = p.getInt(key, default)
    override fun putInt(key: String, value: Int) { p.edit().putInt(key, value).apply() }
    override fun getLong(key: String, default: Long) = p.getLong(key, default)
    override fun putLong(key: String, value: Long) { p.edit().putLong(key, value).apply() }
    override fun getString(key: String, default: String) = p.getString(key, default) ?: default
    override fun putString(key: String, value: String) { p.edit().putString(key, value).apply() }
}

private val EveLeaf = Color(0xFF8FD18B)
private val EveCream = Color(0xFFF2FFE9)

/**
 * The throne page's introduction to Eve: who she is, how her 12 shards are earned on the weekly
 * podium, this player's shards so far, and (for owners) the home screen switch.
 */
@Composable
internal fun EveThroneCard(userId: String?) {
    val context = LocalContext.current
    LaunchedEffect(userId) { EveOwnership.refresh(context, userId, force = true) }
    val status = EveOwnership.status
    val owned = EveOwnership.owns(userId)
    val shards = if (owned) 12 else (status?.shards ?: 0).coerceIn(0, 12)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF173D2E),
        border = BorderStroke(1.5.dp, EveLeaf.copy(alpha = .7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(92.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF214E3B))) {
                    Image(
                        painterResource(R.drawable.eve_still), sh("Eve", "Eve"),
                        contentScale = ContentScale.Fit, modifier = Modifier.size(92.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sh("TAHT ÖDÜLÜ · MAĞAZADA YOK", "THRONE REWARD · EXCLUSIVE"), color = EveLeaf, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(sh("Eve · Orman Dostu", "Eve · Forest Friend"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        sh("Ana ekranda ve oyun alanında sana eşlik eder; hamlelerine göre konuşur, galibiyetlerini hatırlar. Her maçta 3 ücretsiz ipucu sunar.",
                            "Eve joins you at home and in games, responds to your moves, and remembers your wins. She gives you 3 free hints each match."),
                        color = EveCream.copy(alpha = .85f), fontSize = 12.sp, lineHeight = 16.sp,
                    )
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF24513D)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("3", color = EveLeaf, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Column {
                        Text(
                            sh("ÜCRETSİZ İPUCU / MAÇ", "FREE HINTS / MATCH"),
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black,
                        )
                        Text(
                            sh("Kuşatma antrenmanı ve Son Harf", "Word Siege practice and Last Letter"),
                            color = EveCream.copy(alpha = .8f), fontSize = 10.sp,
                        )
                    }
                }
            }
            // Twelve shard slots, filled as the player earns them.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(12) { i ->
                    Box(
                        Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp))
                            .background(if (i < shards) EveLeaf else Color.White.copy(alpha = .14f)),
                    )
                }
            }
            Text(
                if (owned) sh("Eve açıldı · 12/12 parça tamam", "Eve unlocked · 12/12 shards")
                else sh("Eve ilerlemen · $shards/12 parça", "Eve progress · $shards/12 shards"),
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black,
            )
            if (!owned) {
                Text(
                    sh("Haftalık Taht sıralamasında 1. olan 3, 2. olan 2, 3. olan 1 parça kazanır. 12 parçayı tamamladığında Eve açılır.",
                        "Each week's Throne podium awards Eve shards: 1st gets 3, 2nd gets 2, and 3rd gets 1. Collect 12 shards to unlock Eve."),
                    color = EveCream.copy(alpha = .8f), fontSize = 11.sp, lineHeight = 15.sp,
                )
            }
            val rank = status?.currentRank ?: 0
            if (!owned && rank in 1..3) {
                Text(
                    sh("Şu an $rank. sıradasın. Haftayı burada bitirirsen +${4 - rank} parça kazanırsın.", "You're currently #$rank. Finish the week here to earn +${4 - rank} shards."),
                    color = EveLeaf, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
            if (owned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("Eve ana ekranda görünsün", "Show Eve on the home screen"), Modifier.weight(1f), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = EveOwnership.homeEnabled,
                        onCheckedChange = { EveOwnership.setHomeEnabled(context, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = EveLeaf),
                    )
                }
                Text(
                    sh("Bu seçimi kapatırsan ana ekranda mevcut maskotun görünür.",
                        "Turn this off to show your current mascot on the home screen."),
                    color = EveCream.copy(alpha = .7f), fontSize = 10.sp,
                )
            }
        }
    }
}
