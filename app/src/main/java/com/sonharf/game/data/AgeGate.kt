package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * G4.6 istemci-tarafı — doğum yılı güncelle.
 * Migration: supabase/migrations/20260917080000_g46_birth_year_rpc_v1.sql
 *
 * [birthYear] null verirse sunucu kaydı temizler ("yaş belirtilmedi").
 * Aksi halde 1900..bu yıl aralığında olmalı; sunucu 'invalid_birth_year'
 * hatası atar.
 */
suspend fun OnlineGameBackend.updateMyBirthYear(birthYear: Int?) {
    SupabaseProvider.client.postgrest.rpc(
        "update_my_birth_year",
        buildJsonObject {
            if (birthYear == null) put("p_birth_year", JsonNull)
            else put("p_birth_year", birthYear)
        },
    )
}
