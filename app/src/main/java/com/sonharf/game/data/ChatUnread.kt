package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest

suspend fun OnlineGameBackend.getMyUnreadChatCount(): Int =
    SupabaseProvider.client.postgrest
        .rpc("get_my_unread_chat_count_v1")
        .decodeSingle()

suspend fun OnlineGameBackend.markMyChatRead() {
    SupabaseProvider.client.postgrest.rpc("mark_my_chat_read_v1")
}
