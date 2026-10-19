package com.obi.local

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService

/**
 * Opens Obi's voice command from outside the app: the phone's assistant gesture (when Obi is
 * chosen as the digital assistant app), a headset button, or the "Obi konuş" quick setting.
 * Shows nothing itself: Obi starts listening on top of whatever is on screen.
 */
class ObiAssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            startForegroundService(Intent(this, ObiOverlayService::class.java).setAction(ObiOverlayService.ACTION_LISTEN))
        }
        finish()
        overridePendingTransition(0, 0)
    }
}

/** Quick settings tile: one tap from the notification shade and Obi listens. */
class ObiTalkTile : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, ObiAssistActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 31, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
