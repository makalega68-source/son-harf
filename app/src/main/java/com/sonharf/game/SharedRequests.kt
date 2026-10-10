package com.sonharf.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import io.github.jan.supabase.auth.auth
import java.util.concurrent.ConcurrentHashMap

/**
 * Collapses identical concurrent requests: when several avatars for the same player ask at once,
 * one network call runs and every caller awaits its result. A failure reaches every waiter.
 */
internal class SharedRequests<T> {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<T>>()

    suspend fun get(key: String, load: suspend () -> T): T {
        val request = inFlight.computeIfAbsent(key) { scope.async { load() } }
        return try {
            request.await()
        } finally {
            inFlight.remove(key, request)
        }
    }
}

/** Short-lived identity cache for avatars that look players up by id. */
internal object PlayerIdentityCache {
    private data class Entry(val profile: com.sonharf.game.data.ProfileDto, val savedAt: Long)
    private val cache = ConcurrentHashMap<String, Entry>()
    private val requests = SharedRequests<com.sonharf.game.data.ProfileDto>()

    /** The last profile seen for this player, however old: enough to draw a first frame. */
    fun peek(userId: String): com.sonharf.game.data.ProfileDto? = cache[userId]?.profile

    suspend fun get(userId: String): com.sonharf.game.data.ProfileDto {
        val now = System.currentTimeMillis()
        cache[userId]?.takeIf { now - it.savedAt < 60_000L }?.let { return it.profile }
        val profile = requests.get(userId) { com.sonharf.game.data.OnlineGameBackend().getProfile(userId) }
        cache[userId] = Entry(profile, System.currentTimeMillis())
        return profile
    }
}

/**
 * The signed-in player's own profile, kept in memory and on disk. Every screen that shows "you"
 * starts from it, so the photo, name and frame are there on the first frame instead of popping in
 * after a fetch (the fetch still runs and refreshes it).
 */
internal object OwnProfile {
    private const val PREFS = "own_profile"
    private const val KEY = "profile_json"
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    @Volatile private var prefs: android.content.SharedPreferences? = null
    @Volatile private var memory: com.sonharf.game.data.ProfileDto? = null

    fun init(context: android.content.Context) {
        val store = context.applicationContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        prefs = store
        if (memory == null) memory = runCatching {
            store.getString(KEY, null)?.let { json.decodeFromString(com.sonharf.game.data.ProfileDto.serializer(), it) }
        }.getOrNull()
    }

    private fun currentId(): String? = runCatching {
        if (!com.sonharf.game.data.SupabaseProvider.configured) null
        else com.sonharf.game.data.SupabaseProvider.client.auth.currentUserOrNull()?.id
    }.getOrNull()

    fun remember(profile: com.sonharf.game.data.ProfileDto) {
        if (profile.id != currentId() || profile == memory) return
        memory = profile
        runCatching { prefs?.edit()?.putString(KEY, json.encodeToString(com.sonharf.game.data.ProfileDto.serializer(), profile))?.apply() }
    }

    /** Your last known profile, or null when signed out or another account is signed in. */
    fun snapshot(): com.sonharf.game.data.ProfileDto? {
        val id = currentId() ?: return null
        return memory?.takeIf { it.id == id }
    }
}
