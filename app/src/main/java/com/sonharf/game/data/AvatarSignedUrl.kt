package com.sonharf.game.data

import com.sonharf.game.BuildConfig
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Resolves private profile-photos storage paths to short-lived signed URLs. */
object AvatarSignedUrl {
    private val http = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun resolve(pathOrUrl: String?): String? {
        val value = pathOrUrl?.trim().orEmpty()
        if (value.isBlank()) return null
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val session = SupabaseProvider.client.auth.currentSessionOrNull() ?: return null
        val encodedPath = value.split('/').joinToString("/") { it.encodeURLPathPart() }
        val response = http.post("${BuildConfig.SUPABASE_URL}/storage/v1/object/sign/profile-photos/$encodedPath") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("apikey", BuildConfig.SUPABASE_KEY)
            contentType(ContentType.Application.Json)
            setBody("{\"expiresIn\":3600}")
        }
        if (!response.status.isSuccess()) return null
        val root = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonObject }.getOrNull() ?: return null
        val signed = root["signedURL"]?.jsonPrimitive?.content
            ?: root["signedUrl"]?.jsonPrimitive?.content
            ?: return null
        return if (signed.startsWith("http")) signed else "${BuildConfig.SUPABASE_URL}$signed"
    }
}
