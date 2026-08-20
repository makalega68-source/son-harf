package com.sonharf.game.data

import com.sonharf.game.BuildConfig
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object AccountDeletion {
    suspend fun deleteCurrentAccount() {
        val client = SupabaseProvider.client
        val token = client.auth.currentAccessTokenOrNull()
            ?: error("No authenticated session")

        withContext(Dispatchers.IO) {
            val connection = (URL("${BuildConfig.SUPABASE_URL}/functions/v1/delete-account").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
                setRequestProperty("Content-Type", "application/json")
            }

            try {
                connection.outputStream.use { it.write("{}".toByteArray()) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val message = runCatching { connection.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull()
                    error("Account deletion failed ($code): ${message.orEmpty().take(240)}")
                }
            } finally {
                connection.disconnect()
            }
        }

        client.auth.clearSession()
    }
}
