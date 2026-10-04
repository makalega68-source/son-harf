package com.sonharf.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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

    suspend fun get(userId: String): com.sonharf.game.data.ProfileDto {
        val now = System.currentTimeMillis()
        cache[userId]?.takeIf { now - it.savedAt < 60_000L }?.let { return it.profile }
        val profile = requests.get(userId) { com.sonharf.game.data.OnlineGameBackend().getProfile(userId) }
        cache[userId] = Entry(profile, System.currentTimeMillis())
        return profile
    }
}
