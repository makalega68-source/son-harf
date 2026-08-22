package com.sonharf.game

import android.content.Context
import android.content.SharedPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.util.Locale

/**
 * Dictionary runtime with two-level cache. Meanings that are successfully resolved are
 * kept in RAM for the session and persisted on-device so a later lookup does not depend
 * on network availability. The match summary also preloads every used word.
 */
internal object WordMeaningRuntime {
    private val http = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }
    private val memory = LinkedHashMap<String, String>()
    private var prefs: SharedPreferences? = null

    private val verifiedOffline = mapOf(
        "tr:telefon" to "Sesin uzak mesafelere elektriksel veya elektronik yollarla iletilmesini sağlayan haberleşme aracı.",
        "tr:navigasyon" to "Bir yerden başka bir yere ulaşmak için konum ve rota belirleme işi; yol bulma.",
        "tr:masa" to "Üzerinde çalışma, yemek yeme veya eşya koyma amacıyla kullanılan, ayaklı düz yüzeyli mobilya.",
        "tr:araba" to "İnsan veya yük taşımaya yarayan tekerlekli taşıt.",
        "tr:kalem" to "Yazı yazmak veya çizim yapmak için kullanılan araç.",
        "tr:armut" to "Gülgillerden, tatlı ve sulu meyvesi bulunan ağaç ve bu ağacın meyvesi.",
        "en:apple" to "A round fruit with firm flesh and a skin that is commonly red, green, or yellow.",
        "en:table" to "A piece of furniture with a flat top supported by legs.",
        "en:water" to "A clear liquid essential for life, chemically composed of hydrogen and oxygen.",
        "en:rabbit" to "A small mammal with long ears and powerful hind legs."
    )

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("word_meaning_cache_v2", Context.MODE_PRIVATE)
        verifiedOffline.forEach { (k, v) -> memory.putIfAbsent(k, v) }
    }

    private fun normalize(word: String, language: String): String {
        val locale = if (language == "tr") Locale("tr", "TR") else Locale.ENGLISH
        return word.trim().lowercase(locale)
    }

    private fun store(key: String, value: String) {
        if (value.isBlank()) return
        synchronized(memory) {
            memory[key] = value
            while (memory.size > 600) memory.remove(memory.keys.first())
        }
        prefs?.edit()?.putString(key, value)?.apply()
    }

    suspend fun meaning(word: String, language: String): String {
        val normalized = normalize(word, language)
        val key = "$language:$normalized"
        synchronized(memory) { memory[key] }?.let { return it }
        prefs?.getString(key, null)?.takeIf { it.isNotBlank() }?.let {
            synchronized(memory) { memory[key] = it }
            return it
        }
        verifiedOffline[key]?.let { store(key, it); return it }

        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name()).replace("+", "%20")
        val host = if (language == "en") "en.wiktionary.org" else "tr.wiktionary.org"

        val dictionaryApi = if (language == "en") runCatching {
            val body = http.get("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject
            root?.get("meanings")?.jsonArray.orEmpty().asSequence().mapNotNull { meaning ->
                meaning.jsonObject["definitions"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("definition")?.jsonPrimitive?.content?.trim()
            }.firstOrNull { !it.isNullOrBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val restSummary = if (dictionaryApi.isBlank()) runCatching {
            val body = http.get("https://$host/api/rest_v1/page/summary/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            root["extract"]?.jsonPrimitive?.content?.trim().orEmpty()
        }.getOrDefault("") else ""

        val queryExtract = if (dictionaryApi.isBlank() && restSummary.isBlank()) runCatching {
            val url = "https://$host/w/api.php?action=query&format=json&prop=extracts&explaintext=1&redirects=1&titles=$encoded"
            val root = json.parseToJsonElement(http.get(url).bodyAsText()).jsonObject
            val pages = root["query"]?.jsonObject?.get("pages")?.jsonObject
            pages?.values?.asSequence()?.mapNotNull { it.jsonObject["extract"]?.jsonPrimitive?.content?.trim() }
                ?.firstOrNull { it.isNotBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val raw = dictionaryApi.ifBlank { restSummary }.ifBlank { queryExtract }
        val concise = raw
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^${Regex.escape(normalized)}\\s*", RegexOption.IGNORE_CASE), "")
            .trim(' ', '-', ':', ';')
            .let { if (it.length > 520) it.take(517).trimEnd() + "…" else it }

        if (concise.isNotBlank()) {
            store(key, concise)
            return concise
        }

        return if (language == "en")
            "Bu kelime oyun sözlüğünde geçerli. Kısa İngilizce tanımı sözlük kaynağından alınamadı."
        else
            "Bu kelime oyun sözlüğünde geçerli. Kısa Türkçe tanımı sözlük kaynağından alınamadı."
    }
}
