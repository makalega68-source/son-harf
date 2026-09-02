package com.sonharf.game

import android.content.Context

/**
 * Tracks the mandatory first-install language decision independently from the
 * selected language itself. Existing installs keep their current language and
 * only new installs are required to complete the gate once.
 */
internal object FirstRunLanguagePreferences {
    private const val FILE = "son_harf_first_run"
    private const val LANGUAGE_COMPLETE = "language_complete"

    fun isComplete(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(LANGUAGE_COMPLETE, false)

    fun complete(context: Context, language: String) {
        SonHarfPreferences.setLanguage(context, language)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(LANGUAGE_COMPLETE, true)
            .apply()
    }
}
