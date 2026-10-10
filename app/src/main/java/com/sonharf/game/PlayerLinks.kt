package com.sonharf.game

import android.content.Context
import java.net.URI
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class PlayerTarget(val kind:String,val id:String?)
internal object PlayerLinks {
    var pending by mutableStateOf<PlayerTarget?>(null)
    fun parse(raw:String?): PlayerTarget? {
        val uri=raw?.let { runCatching { URI(it) }.getOrNull() } ?: return null
        if(uri.scheme!="kelimetahti") return null
        if (uri.rawQuery != null || uri.rawFragment != null || uri.userInfo != null || uri.port != -1) return null
        val id=uri.path?.removePrefix("/")?.takeIf { it.isNotBlank() && "/" !in it } ?: return null
        // URI.host rejects underscores although Android custom schemes accept them.
        val kind = uri.rawAuthority ?: return null
        return when(kind) {
            "invite" -> id.uppercase().takeIf { it.matches(Regex("[A-F0-9]{12}")) }?.let { PlayerTarget("invite",it) }
            "siege","series","son_harf" -> id.takeIf { it.matches(Regex("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")) }?.let { PlayerTarget(kind,it) }
            "activity" -> PlayerTarget("activity",null)
            else -> null
        }
    }
    fun accept(context:Context,raw:String?) {
        val target=parse(raw) ?: return
        context.getSharedPreferences("player_links",Context.MODE_PRIVATE).edit().putString("pending",raw).apply()
        pending=target
    }
    fun take(context:Context):PlayerTarget? {
        val prefs=context.getSharedPreferences("player_links",Context.MODE_PRIVATE)
        val target=pending ?: parse(prefs.getString("pending",null))
        pending=null; prefs.edit().remove("pending").apply();return target
    }
}
internal object SonHarfLaunchConfig { var pendingRoomId:String?=null }
