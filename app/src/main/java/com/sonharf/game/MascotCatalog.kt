package com.sonharf.game

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonharf.game.mascotdata2.MascotEmbeddedModel

internal data class MascotCatalogItem(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val standard: Boolean,
    val licensedForCommercialGame: Boolean,
    val assetPath: String? = null,
)

internal object MascotCatalog {
    const val DEFAULT_ID = "mascot_white"
    const val CHIBI_WIZARD_ID = "mascot_chibi_wizard"

    val all = listOf(
        MascotCatalogItem(
            id = DEFAULT_ID,
            nameTr = "Beyaz Dost",
            nameEn = "White Buddy",
            standard = true,
            licensedForCommercialGame = true,
        ),
        MascotCatalogItem(
            id = CHIBI_WIZARD_ID,
            nameTr = "Chibi Büyücü",
            nameEn = "Chibi Wizard",
            standard = false,
            licensedForCommercialGame = true,
            assetPath = "models/mascots/chibi_wizard.glb",
        ),
    )

    fun item(id: String?): MascotCatalogItem =
        all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT_ID }

    fun isAssetReady(context: Context, id: String): Boolean = when (id) {
        DEFAULT_ID -> runCatching { MascotEmbeddedModel.ensureFile(context).isFile }.getOrDefault(false)
        CHIBI_WIZARD_ID -> runCatching {
            context.assets.open(requireNotNull(item(id).assetPath)).use { }
            true
        }.getOrDefault(false)
        else -> false
    }

    fun modelLocation(context: Context, id: String): String? = when (id) {
        DEFAULT_ID -> runCatching { Uri.fromFile(MascotEmbeddedModel.ensureFile(context)).toString() }.getOrNull()
        CHIBI_WIZARD_ID -> item(id).assetPath?.takeIf { isAssetReady(context, id) }
        else -> null
    }

    fun clip(id: String, motion: MascotMotion): String = when (id) {
        CHIBI_WIZARD_ID -> when (motion) {
            MascotMotion.WALK, MascotMotion.TURN_LEFT, MascotMotion.TURN_RIGHT -> "Walk"
            MascotMotion.RUN -> "Run"
            MascotMotion.VICTORY -> "Special Attack"
            MascotMotion.DEFEAT -> "Hurt"
            MascotMotion.CRITICAL -> "Attack"
            MascotMotion.IDLE,
            MascotMotion.LOOK_AT_PLAYER,
            MascotMotion.GREETING,
            MascotMotion.THINKING,
            MascotMotion.SIT -> "Idle"
        }
        else -> when (motion) {
            MascotMotion.WALK, MascotMotion.RUN, MascotMotion.TURN_LEFT, MascotMotion.TURN_RIGHT -> "Walk"
            MascotMotion.VICTORY -> "Victory"
            else -> "Idle"
        }
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
        selectedId = MascotCatalog.item(stored).id
        loaded = true
    }

    fun select(context: Context, id: String) {
        val resolved = MascotCatalog.item(id).id
        selectedId = resolved
        loaded = true
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, resolved)
            .apply()
    }
}
