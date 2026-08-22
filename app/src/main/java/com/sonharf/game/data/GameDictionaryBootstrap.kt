package com.sonharf.game.data

import com.sonharf.game.BuildConfig
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * One-shot, authenticated bootstrap for the server-side Turkish/English game dictionaries.
 * The Edge Function is idempotent; subsequent calls are cheap once both dictionaries are ready.
 */
object GameDictionaryBootstrap {
    private val http = HttpClient(OkHttp)
    @Volatile private var completedInProcess = false

    suspend fun syncIfNeeded() {
        if (completedInProcess || !SupabaseProvider.configured) return
        val session = SupabaseProvider.client.auth.currentSessionOrNull() ?: return
        val response = http.post("${BuildConfig.SUPABASE_URL}/functions/v1/dictionary-sync") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("apikey", BuildConfig.SUPABASE_KEY)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        check(response.status.isSuccess()) {
            "dictionary_sync_${response.status.value}_${response.bodyAsText().take(120)}"
        }
        completedInProcess = true
    }
}
