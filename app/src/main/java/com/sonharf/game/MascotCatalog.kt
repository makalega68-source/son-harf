package com.sonharf.game

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonharf.game.mascotdata3.ChibiEmbeddedModel

internal data class MascotCatalogItem(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val standard: Boolean,
    val licensedForCommercialGame: Boolean,
)

internal object MascotCatalog {
    const val DEFAULT_ID = "mascot_white"
    const val CHIBI_WIZARD_ID = "mascot_chibi_wizard"
    const val WHITE_ASSET = "embedded:chibi-wizard-v1"

    val all = listOf(
        MascotCatalogItem(
            id = DEFAULT_ID,
            nameTr = "Lyra — Beyaz Mühür",
            nameEn = "Lyra — White Seal",
            standard = true,
            licensedForCommercialGame = true,
        ),
        MascotCatalogItem(
            id = CHIBI_WIZARD_ID,
            nameTr = "Neris — Gölge Bilgesi",
            nameEn = "Neris — Shadow Sage",
            standard = false,
            licensedForCommercialGame = true,
        ),
    )

    fun item(id: String?): MascotCatalogItem =
        all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT_ID }

    fun isAssetReady(context: Context, id: String): Boolean = when (id) {
        DEFAULT_ID,
        CHIBI_WIZARD_ID -> runCatching { ChibiEmbeddedModel.ensureFile(context).isFile }.getOrDefault(false)
        else -> false
    }

    fun modelLocation(context: Context, id: String): String? = when (id) {
        DEFAULT_ID,
        CHIBI_WIZARD_ID -> runCatching { Uri.fromFile(ChibiEmbeddedModel.ensureFile(context)).toString() }.getOrNull()
        else -> null
    }

    fun clip(id: String, motion: MascotMotion): String = when (id) {
        DEFAULT_ID,
        CHIBI_WIZARD_ID -> when (motion) {
            MascotMotion.WALK -> "Walk"
            MascotMotion.TURN_LEFT -> "Turn_Left"
            MascotMotion.TURN_RIGHT -> "Turn_Right"
            MascotMotion.RUN -> "Run"
            MascotMotion.VICTORY -> "Special_Attack"
            MascotMotion.DEFEAT -> "Hurt"
            MascotMotion.CRITICAL -> "Attack"
            MascotMotion.IDLE,
            MascotMotion.LOOK_AT_PLAYER,
            MascotMotion.GREETING,
            MascotMotion.THINKING,
            MascotMotion.SIT -> "Idle"
        }
        else -> "Idle"
    }
}

internal object MascotSelectionRuntime {
    private const val PREFS = "son_harf_mascot_selection"
    private const val KEY = "selected_mascot_id"

    var selectedId by mutableStateOf(MascotCatalog.DEFAULT_ID)
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val stored = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, MascotCatalog.DEFAULT_ID)
        val requested = MascotCatalog.item(stored).id
        selectedId = if (MascotCatalog.isAssetReady(context, requested)) requested else MascotCatalog.DEFAULT_ID
        loaded = true
    }

    fun select(context: Context, id: String) {
        val requested = MascotCatalog.item(id).id
        val resolved = if (MascotCatalog.isAssetReady(context, requested)) requested else MascotCatalog.DEFAULT_ID
        selectedId = resolved
        loaded = true
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, resolved)
            .apply()
    }
}
