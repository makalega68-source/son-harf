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
    const val CHIBI_WIZARD_ID = "mascot_chibi_wizard"
    const val DEFAULT_ID = CHIBI_WIZARD_ID

    val all = listOf(
        MascotCatalogItem(
            id = CHIBI_WIZARD_ID,
            nameTr = "Chibi",
            nameEn = "Chibi",
            standard = true,
            licensedForCommercialGame = true,
        ),
    )

    fun item(id: String?): MascotCatalogItem =
        all.firstOrNull { it.id == id } ?: all.first()

    fun isAssetReady(context: Context, id: String): Boolean =
        id == CHIBI_WIZARD_ID &&
            runCatching { ChibiEmbeddedModel.ensureFile(context).isFile }.getOrDefault(false)

    fun modelLocation(context: Context, id: String): String? =
        if (id == CHIBI_WIZARD_ID) {
            runCatching { Uri.fromFile(ChibiEmbeddedModel.ensureFile(context)).toString() }.getOrNull()
        } else {
            null
        }

    fun clip(id: String, motion: MascotMotion): String {
        if (id != CHIBI_WIZARD_ID) return "Idle"
        return when (motion) {
            MascotMotion.WALK -> "Walk"
            MascotMotion.TURN_LEFT -> "Turn_Left"
            MascotMotion.TURN_RIGHT -> "Turn_Right"
            MascotMotion.RUN -> "Run"
            MascotMotion.VICTORY -> "Special_Attack"
            MascotMotion.DEFEAT -> "Hurt"
            MascotMotion.CRITICAL -> "Attack"
            MascotMotion.GREETING -> "Turn_Right"
            MascotMotion.THINKING,
            MascotMotion.LOOK_AT_PLAYER -> "Turn_Left"
            MascotMotion.IDLE,
            MascotMotion.SIT -> "Idle"
        }
    }
}

internal object MascotSelectionRuntime {
    private const val PREFS = "son_harf_mascot_selection"
    private const val KEY = "selected_mascot_id"

    var selectedId by mutableStateOf(MascotCatalog.CHIBI_WIZARD_ID)
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        selectedId = MascotCatalog.CHIBI_WIZARD_ID
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, MascotCatalog.CHIBI_WIZARD_ID)
            .apply()
        loaded = true
    }

    fun select(context: Context, id: String) {
        selectedId = MascotCatalog.CHIBI_WIZARD_ID
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, MascotCatalog.CHIBI_WIZARD_ID)
            .apply()
        loaded = true
    }
}
