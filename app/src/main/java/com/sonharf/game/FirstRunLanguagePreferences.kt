package com.sonharf.game

import android.content.Context

/**
 * Tracks the mandatory first-install language decision independently from the
 * selected language itself. Existing installs keep their current language and
 * are not unexpectedly forced through the new onboarding after an update.
 */
internal object FirstRunLanguagePreferences {
    private const val FILE = "son_harf_first_run"
    private const val LANGUAGE_COMPLETE = "language_complete"
    private const val ONBOARDING_REQUIRED = "onboarding_required"

    fun isComplete(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(LANGUAGE_COMPLETE, false)

    fun needsOnboarding(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(ONBOARDING_REQUIRED, false)

    fun complete(context: Context, language: String) {
        SonHarfPreferences.setLanguage(context, language)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(LANGUAGE_COMPLETE, true)
            .putBoolean(ONBOARDING_REQUIRED, true)
            .apply()
    }

    fun completeOnboarding(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ONBOARDING_REQUIRED, false)
            .apply()
    }
}
