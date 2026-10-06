package com.sonharf.game

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.work.*
import com.sonharf.game.data.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Authenticated inbox reminders. Android may defer the 15-minute background check. */
internal object MatchNotifications {
    private const val WORK = "social_inbox_notifications"
    private const val CHANNEL = "match_activity"
    private const val PREFS = "match_activity"
    fun enabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", true)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply()
        if (enabled) schedule(context) else WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
    fun schedule(context: Context) {
        if (!enabled(context) || !SupabaseProvider.configured) return
        val user = OnlineGameBackend().currentUserId() ?: return
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val data = workDataOf("user_id" to user)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MatchNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints).setInputData(data).build())
        WorkManager.getInstance(context).enqueueUniqueWork("${WORK}_now", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<MatchNotificationWorker>().setConstraints(constraints).setInputData(data).build())
    }
    fun signedOut(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
        WorkManager.getInstance(context).cancelUniqueWork("${WORK}_now")
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.activeNotifications?.filter { it.notification.channelId == CHANNEL }?.forEach { manager.cancel(it.id) }
        SonHarfInvite.playerCode = null
    }
    internal fun notificationLink(event: SocialActivityDto): String = when {
        event.targetKind in setOf("siege", "series", "son_harf") && event.targetId != null -> "kelimetahti://${event.targetKind}/${event.targetId}"
        else -> "kelimetahti://activity/inbox"
    }
    internal fun eligible(event: SocialActivityDto, now: Instant): Boolean = event.readAt == null &&
        event.kind in setOf("your_turn", "challenge", "rematch", "friend_request", "friend_accepted", "match_finished") &&
        runCatching { com.sonharf.game.data.requireServerInstant(event.createdAt).let { !it.isAfter(now) && it.isAfter(now.minusSeconds(86_400)) } }.getOrDefault(false)

    suspend fun deliver(context: Context, expectedUser: String): Boolean {
        if (!enabled(context) || !SupabaseProvider.configured) return true
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return true
        withTimeoutOrNull(10_000) { SupabaseProvider.client.auth.sessionStatus.first { it !is SessionStatus.Initializing } }
        val backend = OnlineGameBackend()
        // Never sign in, create a guest or use another account from background work.
        if (backend.currentUserId() != expectedUser || !hasVerifiedMembershipSession()) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return true
        val english = SonHarfPreferences.language(context) == "en"
        manager.createNotificationChannel(NotificationChannel(CHANNEL, if (english) "Matches and invitations" else "Maçlar ve davetler", NotificationManager.IMPORTANCE_DEFAULT))
        if (!manager.areNotificationsEnabled()) return true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return true
        val seenKey = "seen_$expectedUser"
        val seen = prefs.getStringSet(seenKey, emptySet()).orEmpty().toMutableSet()
        val inbox = backend.getSocialInbox()
        val events = inbox.filter { event ->
            eligible(event, Instant.now()) && event.id !in seen && when(event.kind) {
                "challenge", "rematch" -> SonHarfPreferences.gameInviteNotificationsEnabled(context)
                "friend_request", "friend_accepted" -> SonHarfPreferences.friendRequestNotificationsEnabled(context)
                else -> SonHarfPreferences.systemNotificationsEnabled(context)
            }
        }.take(8)
        for (event in events.reversed()) {
            if (backend.currentUserId() != expectedUser) return true
            if (event.kind == "your_turn") {
                val stillYourTurn = when (event.targetKind) {
                    "siege", "series" -> event.targetId?.let { backend.getWordSiegeGame(it) }?.let { it.status == "playing" && it.currentPlayerId == expectedUser } == true
                    "son_harf" -> event.targetId?.let { backend.getRoom(it) }?.let { it.status in setOf("playing", "quiz", "final", "sudden_death") && it.currentPlayerId == expectedUser } == true
                    else -> false
                }
                if (!stillYourTurn) { seen += event.id; continue }
            }
            val text = when (event.kind) {
                "your_turn" -> if (english) "Your turn" else "Sıra sende"
                "challenge" -> if (english) "New challenge" else "Yeni meydan okuma"
                "rematch" -> if (english) "Rematch requested" else "Rövanş isteği"
                "friend_request" -> if (english) "Friend request" else "Arkadaşlık isteği"
                "friend_accepted" -> if (english) "Friend request accepted" else "Arkadaşlık isteğin kabul edildi"
                else -> if (english) "Match finished" else "Maç bitti"
            }
            val open = PendingIntent.getActivity(context, event.id.hashCode(),
                Intent(context, MainActivity::class.java).setData(Uri.parse(notificationLink(event)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(event.id.hashCode(), Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_stat_obi)
                .setContentTitle("Kelime Tahtı").setContentText(text).setContentIntent(open).setAutoCancel(true)
                .setGroup("kelimetahti_activity").build())
            seen += event.id
            prefs.edit().putStringSet(seenKey, seen).apply()
        }
        prefs.edit().putStringSet(seenKey, seen.intersect(inbox.map { it.id }.toSet())).apply()
        return true
    }
}

class MatchNotificationWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        inputData.getString("user_id")?.let { MatchNotifications.deliver(applicationContext, it) }
        Result.success()
    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (_: Exception) { Result.retry() }
}
