package com.sonharf.game

import android.content.Context

/** Tracks the mandatory first-install language decision. */
internal object FirstRunLanguagePreferences {
    private const val FILE = "son_harf_first_run"
    private const val LANGUAGE_COMPLETE = "language_complete"
    private const val ONBOARDING_REQUIRED = "onboarding_required"

    fun isComplete(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(LANGUAGE_COMPLETE, false)

    /** Kept only for compatibility with installs that may still have the old preference key. */
    fun needsOnboarding(context: Context): Boolean = false

    fun complete(context: Context, language: String) {
        SonHarfPreferences.setLanguage(context, language)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(LANGUAGE_COMPLETE, true)
            .putBoolean(ONBOARDING_REQUIRED, false)
            .apply()
    }

    /** Clears the retired onboarding flag for compatibility with older installs. */
    fun completeOnboarding(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ONBOARDING_REQUIRED, false)
            .apply()
    }
}
