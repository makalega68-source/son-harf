package com.sonharf.game

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class RememberedCredential(
    val email: String,
    val password: String,
)

/** Keeps remembered login credentials encrypted with an app-private Android Keystore key. */
internal object RememberedCredentialVault {
    private const val STORE_FILE = "son_harf_secure_credentials"
    private const val PAYLOAD_KEY = "login_payload_v1"
    private const val KEY_ALIAS = "son_harf_login_credentials_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun load(context: Context): RememberedCredential? {
        return runCatching {
            val encoded = context.getSharedPreferences(STORE_FILE, Context.MODE_PRIVATE)
                .getString(PAYLOAD_KEY, null)
                ?: return@runCatching null
            val parts = encoded.split(':', limit = 2)
            if (parts.size != 2) error("invalid_credential_payload")

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
            )
            val clearText = String(
                cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                StandardCharsets.UTF_8,
            )
            val separator = clearText.indexOf('\n')
            if (separator <= 0 || separator == clearText.lastIndex) error("invalid_credential_content")
            RememberedCredential(
                email = clearText.substring(0, separator),
                password = clearText.substring(separator + 1),
            )
        }.getOrElse {
            clear(context)
            null
        }
    }

    fun save(context: Context, email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            clear(context)
            return
        }
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(
                "$normalizedEmail\n$password".toByteArray(StandardCharsets.UTF_8)
            )
            val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            context.getSharedPreferences(STORE_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(PAYLOAD_KEY, payload)
                .apply()
        }.onFailure {
            clear(context)
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(STORE_FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(PAYLOAD_KEY)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
