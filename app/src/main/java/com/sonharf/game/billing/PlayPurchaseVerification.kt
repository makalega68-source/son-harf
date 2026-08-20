package com.sonharf.game.billing

import com.sonharf.game.BuildConfig
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PlayPurchaseVerification {
    suspend fun verify(productId: String, purchaseToken: String) {
        require(productId.isNotBlank())
        require(purchaseToken.isNotBlank())

        val accessToken = SupabaseProvider.client.auth.currentAccessTokenOrNull()
            ?: error("No authenticated session")

        withContext(Dispatchers.IO) {
            val connection = (URL("${BuildConfig.SUPABASE_URL}/functions/v1/verify-play-purchase").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
                setRequestProperty("Content-Type", "application/json")
            }

            try {
                val body = JSONObject()
                    .put("productId", productId)
                    .put("purchaseToken", purchaseToken)
                    .toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val text = runCatching {
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }.getOrDefault("")

                if (code !in 200..299) {
                    val error = runCatching { JSONObject(text).optString("error") }.getOrNull().orEmpty()
                    throw IllegalStateException(error.ifBlank { "Play purchase verification failed ($code)" })
                }

                val verified = runCatching { JSONObject(text).optBoolean("verified", false) }.getOrDefault(false)
                check(verified) { "Purchase was not verified" }
            } finally {
                connection.disconnect()
            }
        }
    }
}
