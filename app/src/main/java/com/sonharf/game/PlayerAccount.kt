package com.sonharf.game

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.signInAnonymously
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * How a player gets into the game. A new player plays at once on a guest account (no email, no
 * password): their name, coins and games are kept on the server under that account. They can keep
 * it for good by linking Google, which upgrades the same account, so nothing is lost. Google can
 * also sign a returning player straight in. Email sign-in stays as an option.
 */
internal object PlayerAccount {
    /** Any signed-in account, guest or member, may play. */
    fun hasPlayableSession(): Boolean =
        runCatching { SupabaseProvider.client.auth.currentUserOrNull() != null }.getOrDefault(false)

    /** A guest account: no email or Google identity yet, so it lives only on this device. */
    fun isGuest(): Boolean = runCatching {
        val user = SupabaseProvider.client.auth.currentUserOrNull() ?: return@runCatching false
        user.email.isNullOrBlank()
    }.getOrDefault(false)

    /** Google sign-in is offered once the app has its Google web client ID. */
    val googleAvailable: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** The profile RPC takes the gender without Turkish letters. */
    fun serverGender(gender: String): String = when (gender.trim().lowercase()) {
        "kadın", "kadin" -> "kadin"
        "diğer", "diger" -> "diger"
        else -> "erkek"
    }

    /** Starts playing on a new guest account with the chosen name, kept on this device. */
    suspend fun startGuest(context: Context, name: String, gender: String) {
        val auth = SupabaseProvider.client.auth
        if (auth.currentUserOrNull() == null) auth.signInAnonymously()
        try {
            SupabaseProvider.client.postgrest.rpc(
                "complete_profile_identity_v2",
                buildJsonObject {
                    put("p_display_name", name.trim())
                    put("p_gender", serverGender(gender))
                },
            )
        } catch (error: Throwable) {
            // A taken or invalid name: drop the half-made guest so the player can simply try again.
            runCatching { auth.signOut() }
            throw error
        }
        // Remembered so the app opens straight into the game next time (no stored password).
        SonHarfPreferences.setRememberLogin(context, true)
    }

    /** The Google account's ID token from the system account picker. Needs an activity context. */
    private suspend fun googleIdToken(activityContext: Context): String {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "google_no_credential"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    /** Signs in (or up) with Google: a returning player gets their account back on any phone. */
    suspend fun signInWithGoogle(activityContext: Context) {
        val token = googleIdToken(activityContext)
        SupabaseProvider.client.auth.signInWith(IDToken) {
            idToken = token
            provider = Google
        }
        val email = SupabaseProvider.client.auth.currentUserOrNull()?.email.orEmpty()
        SonHarfPreferences.setRememberLogin(activityContext, true, email)
    }

    /** Keeps the guest account for good: Google joins this same account, so all progress stays. */
    suspend fun linkGoogle(activityContext: Context) {
        val token = googleIdToken(activityContext)
        SupabaseProvider.client.auth.linkIdentityWithIdToken(provider = Google, idToken = token)
        runCatching { SupabaseProvider.client.auth.refreshCurrentSession() }
        val email = SupabaseProvider.client.auth.currentUserOrNull()?.email.orEmpty()
        SonHarfPreferences.setRememberLogin(activityContext, true, email)
    }

    /** A readable message for the account errors a player can meet. */
    fun friendlyError(raw: String): String = when {
        "display_name_taken" in raw -> sh("Bu oyuncu adı alınmış. Başka bir ad dene.", "That player name is taken. Try another one.")
        "invalid_display_name" in raw -> sh("Oyuncu adı 2-24 karakter olmalı.", "Player name must be 2-24 characters.")
        "anonymous" in raw.lowercase() && "disabled" in raw.lowercase() ->
            sh("Misafir girişi şu an kapalı. E-posta ile devam et.", "Guest play is turned off right now. Continue with email.")
        "identity_already_exists" in raw || "already linked" in raw.lowercase() ->
            sh("Bu Google hesabı başka bir oyuncu hesabına bağlı. O hesapla girmek için önce çıkış yap.",
                "This Google account belongs to another player account. Sign out first to use it.")
        "manual_linking_disabled" in raw || "Manual linking is disabled" in raw ->
            sh("Google bağlama şu an kapalı. Daha sonra tekrar dene.", "Google linking is turned off right now. Try again later.")
        "GetCredentialCancellationException" in raw || "canceled" in raw.lowercase() || "cancelled" in raw.lowercase() -> ""
        "NoCredentialException" in raw || "google_no_credential" in raw ->
            sh("Telefonda Google hesabı bulunamadı.", "No Google account was found on this phone.")
        "Unable to resolve host" in raw || "timeout" in raw.lowercase() ->
            sh("İnternet bağlantısını kontrol edip tekrar dene.", "Check your internet connection and try again.")
        else -> sh("İşlem tamamlanamadı. Tekrar dene.", "That didn't work. Try again.")
    }
}
