package com.sonharf.game

import com.sonharf.game.data.EquippedCosmeticsDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

/** Keeps the premium shell's startup cosmetics sync without changing the production economy store. */
suspend fun OnlineGameBackend.getEquippedCosmetics(): EquippedCosmeticsDto? {
    val me = currentUserId() ?: return null
    return SupabaseProvider.client.from("user_equipped_cosmetics")
        .select { filter { eq("user_id", me) } }
        .decodeList<EquippedCosmeticsDto>()
        .firstOrNull()
}
