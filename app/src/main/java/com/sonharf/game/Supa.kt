package com.sonharf.game

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Profile(
    val id: String = "",
    val username: String = "Oyuncu",
    val language: String = "tr",
    val isPro: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 250,
    val hints: Int = 5,
    val swaps: Int = 3,
    val mults: Int = 2,
    val mascot: String = "magecat",
    val eyes: String = "blue",
    val played: Int = 0,
    val won: Int = 0,
    val bestStreak: Int = 0
)

data class StoreItem(
    val id: String, val category: String, val nameTr: String, val nameEn: String,
    val descTr: String, val descEn: String, val price: Int, val reqLevel: Int,
    val key: String
)

object Supa {
    private val JSON = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()

    private lateinit var prefs: SharedPreferences
    private var token: String? = null
    var online: Boolean = false
        private set

    fun boot(ctx: Context) {
        prefs = ctx.getSharedPreferences("sonharf", Context.MODE_PRIVATE)
        token = prefs.getString("access_token", null)
    }

    private fun req(path: String, body: String?, auth: Boolean = true): Request.Builder {
        val b = Request.Builder()
            .url(Config.SUPABASE_URL + path)
            .addHeader("apikey", Config.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
        if (auth) token?.let { b.addHeader("Authorization", "Bearer $it") }
        else b.addHeader("Authorization", "Bearer " + Config.SUPABASE_ANON_KEY)
        if (body != null) b.post(body.toRequestBody(JSON)) else b.get()
        return b
    }

    private fun call(r: Request): String? = runCatching {
        http.newCall(r).execute().use { res -> if (res.isSuccessful) res.body?.string() else null }
    }.getOrNull()

    suspend fun signInAnonymously(): Boolean = withContext(Dispatchers.IO) {
        if (!token.isNullOrBlank()) { online = true; return@withContext true }
        val body = call(req("/auth/v1/signup", "{}", auth = false).build()) ?: return@withContext false
        val j = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext false
        token = j.optString("access_token").ifBlank { null }
        if (token == null) return@withContext false
        prefs.edit().putString("access_token", token).apply()
        online = true
        true
    }

    private fun rpc(fn: String, args: JSONObject): String? =
        call(req("/rest/v1/rpc/$fn", args.toString()).build())

    private fun parseProfile(raw: String?): Profile? {
        val s = raw?.trim() ?: return null
        val o = runCatching {
            if (s.startsWith("[")) JSONArray(s).optJSONObject(0) else JSONObject(s)
        }.getOrNull() ?: return null
        return Profile(
            id = o.optString("id"),
            username = o.optString("username", "Oyuncu"),
            language = o.optString("preferred_language", "tr"),
            isPro = o.optBoolean("is_pro", false),
            xp = o.optInt("xp"), level = o.optInt("level", 1), coins = o.optInt("coins"),
            hints = o.optInt("hint_tokens"), swaps = o.optInt("swap_tokens"),
            mults = o.optInt("multiplier_tokens"),
            mascot = o.optString("equipped_mascot", "magecat"),
            eyes = o.optString("equipped_eyes", "blue"),
            played = o.optInt("matches_played"), won = o.optInt("matches_won"),
            bestStreak = o.optInt("best_streak")
        )
    }

    suspend fun ensureProfile(): Profile? = withContext(Dispatchers.IO) {
        parseProfile(rpc("ensure_profile", JSONObject()))
    }

    suspend fun submitMatch(my: Int, rival: Int, words: Int, streak: Int, lang: String, dbl: Boolean): Profile? =
        withContext(Dispatchers.IO) {
            parseProfile(rpc("submit_match", JSONObject()
                .put("p_my_score", my).put("p_rival_score", rival)
                .put("p_words", words).put("p_streak", streak)
                .put("p_language", lang).put("p_double", dbl)))
        }

    suspend fun buy(itemId: String): Profile? = withContext(Dispatchers.IO) {
        parseProfile(rpc("buy_item", JSONObject().put("p_item_id", itemId)))
    }

    suspend fun equip(itemId: String): Profile? = withContext(Dispatchers.IO) {
        parseProfile(rpc("equip_item", JSONObject().put("p_item_id", itemId)))
    }

    suspend fun claimRewarded(kind: String): Profile? = withContext(Dispatchers.IO) {
        parseProfile(rpc("claim_rewarded", JSONObject().put("p_kind", kind)))
    }

    suspend fun activatePro(days: Int): Profile? = withContext(Dispatchers.IO) {
        parseProfile(rpc("activate_pro", JSONObject().put("p_days", days)))
    }

    suspend fun catalog(): List<StoreItem> = withContext(Dispatchers.IO) {
        val raw = call(req("/rest/v1/store_catalog?select=*&is_active=eq.true&order=sort_order", null).build())
            ?: return@withContext emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            StoreItem(
                id = o.optString("id"), category = o.optString("category"),
                nameTr = o.optString("name_tr"), nameEn = o.optString("name_en"),
                descTr = o.optString("description_tr"), descEn = o.optString("description_en"),
                price = o.optInt("coin_price"), reqLevel = o.optInt("required_level", 1),
                key = o.optJSONObject("metadata")?.optString("key") ?: ""
            )
        }
    }

    suspend fun ownedIds(): Set<String> = withContext(Dispatchers.IO) {
        val raw = call(req("/rest/v1/user_inventory?select=item_id", null).build()) ?: return@withContext emptySet()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return@withContext emptySet()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("item_id") }.toSet()
    }

    suspend fun words(lang: String): List<String> = withContext(Dispatchers.IO) {
        val raw = call(req("/rest/v1/dictionary_words?select=word&language=eq.$lang&limit=60000", null).build())
            ?: return@withContext emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("word") }
    }
}
