package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private suspend fun logBilBakalimEvent(name: String, value: String? = null) {
    if (!SupabaseProvider.configured) return
    runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "log_app_event_v1",
            buildJsonObject {
                put("p_event_name", name)
                if (value != null) put("p_event_value", value)
            },
        )
    }
}

@Composable
fun TrackedBilBakalimStandaloneScreen(onBack: () -> Unit) {
    val sessionKey = remember { System.currentTimeMillis().toString() }
    LaunchedEffect(sessionKey) {
        logBilBakalimEvent("bil_bakalim_open", "excitement_v2")
    }
    BilBakalimExcitementScreen(onBack = onBack)
}
