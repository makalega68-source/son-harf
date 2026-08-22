package com.sonharf.game

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

internal object WordMeaningRuntime {
    private val http = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, String>()

    suspend fun meaning(word: String, language: String): String {
        val normalized = word.trim().lowercase()
        val key = "$language:$normalized"
        cache[key]?.let { return it }
        val host = if (language == "en") "en.wiktionary.org" else "tr.wiktionary.org"
        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name()).replace("+", "%20")
        val url = "https://$host/api/rest_v1/page/summary/$encoded"
        val result = runCatching {
            val root = json.parseToJsonElement(http.get(url).bodyAsText()).jsonObject
            root["extract"]?.jsonPrimitive?.content?.trim().orEmpty()
        }.getOrDefault("")
        val value = result.ifBlank {
            if (language == "en") "No concise dictionary definition could be retrieved for this word." else "Bu kelime için kısa sözlük açıklaması alınamadı."
        }
        cache[key] = value
        return value
    }
}
