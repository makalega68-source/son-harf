package com.sonharf.game.data

import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** One watched row set: changes to [table] where [column] equals [value]. */
internal class LiveWatch(val table: String, val column: String, val value: String)

/**
 * Wakes a game screen the moment its rows change on the server (Supabase Realtime), so screens
 * no longer ask the server every second. Each screen still re-reads through the same RPCs; a
 * signal only says "something changed". [connected] reports whether the channel is live, so the
 * caller keeps a short poll while it is not and only a slow safety poll while it is.
 */
internal object LiveSignals {
    fun of(watches: List<LiveWatch>, connected: AtomicBoolean): Flow<Unit> = callbackFlow {
        val client = SupabaseProvider.client
        val channel = client.channel("live-" + UUID.randomUUID().toString().take(12))
        val changes = watches.map { watch ->
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = watch.table
                filter(watch.column, FilterOperator.EQ, watch.value)
            }
        }
        changes.forEach { flow -> launch { runCatching { flow.collect { trySend(Unit) } } } }
        launch {
            runCatching { channel.status.collect { connected.set(it == RealtimeChannel.Status.SUBSCRIBED) } }
        }
        runCatching { channel.subscribe() }
        awaitClose {
            connected.set(false)
            // The collecting scope is gone here; leave the channel from a short-lived one.
            CoroutineScope(Dispatchers.IO).launch { runCatching { client.realtime.removeChannel(channel) } }
        }
    }
}

/** A conflated wake-up signal fed by [LiveSignals]; [await] returns on a change or after [timeoutMs]. */
internal class LiveWake(private val scope: CoroutineScope, watches: List<LiveWatch>) {
    private val wake = Channel<Unit>(Channel.CONFLATED)
    val live = AtomicBoolean(false)

    init {
        if (SupabaseProvider.configured) {
            scope.launch { runCatching { LiveSignals.of(watches, live).collect { wake.trySend(Unit) } } }
        }
    }

    /** Waits for the next change; while Realtime is down it falls back to [pollMs], else [safetyMs]. */
    suspend fun await(pollMs: Long, safetyMs: Long) {
        withTimeoutOrNull(if (live.get()) safetyMs else pollMs) { wake.receive() }
    }
}
