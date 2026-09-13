package com.sonharf.game.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.ktor.http.Headers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryRpcDecodingTest {
    private val client = createSupabaseClient("https://dictionary-test.invalid", "test-public-key") {
        install(Postgrest)
    }

    @After
    fun closeClient() = runBlocking { client.close() }

    @Test
    fun snapshotAcceptsTheJsonObjectReturnedByTheRpc() {
        val result = PostgrestResult(
            """{"language":"tr","words":["kalem","çiğ","ışık"]}""",
            Headers.Empty, client.postgrest,
        ).decodeDictionaryRpc<DictionarySnapshotDto>()

        assertEquals("tr", result.language)
        assertEquals(listOf("kalem", "çiğ", "ışık"), result.words)
    }

    @Test
    fun validationAcceptsTheJsonObjectReturnedByTheRpc() {
        val result = PostgrestResult(
            """{"valid":true,"reason":"ok","char_length":5,"last_letter":"m","first_letter":"k","normalized_word":"kalem"}""",
            Headers.Empty, client.postgrest,
        ).decodeDictionaryRpc<GameWordValidationDto>()

        assertTrue(result.valid)
        assertEquals("kalem", result.normalizedWord)
        assertEquals(5, result.charLength)
    }
}
