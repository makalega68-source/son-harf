package com.sonharf.game

import android.content.Context

/**
 * Some things are kept on the phone for an instant start: the equipped frame and styles, and the
 * owned mascots. They belong to one account, so when another account signs in on the same phone
 * (or the player signs out) they are forgotten, and the new account never shows the last one's
 * frame, PRO look or mascot.
 */
internal object AccountCaches {
    private const val PREFS = "account_cache_owner"
    private const val KEY_OWNER = "owner_id"

    /** Call once the signed-in account is known; clears what another account left on this phone. */
    fun ensureFor(context: Context, userId: String?) {
        if (userId.isNullOrBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owner = prefs.getString(KEY_OWNER, null)
        if (owner == userId) return
        if (owner != null) clearLocal(context)
        prefs.edit().putString(KEY_OWNER, userId).apply()
    }

    /** Signing out: nothing of this account stays visible to the next one. */
    fun signedOut(context: Context) {
        clearLocal(context)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun clearLocal(context: Context) {
        SonHarfCosmetics.clearLocal(context)
        WordSiegeMascotOwnership.clearLocal(context)
    }
}
