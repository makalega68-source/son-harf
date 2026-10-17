package com.obi.local

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle

/** Invisible screen that asks once for the microphone, then hands back to the floating Obi. */
class ObiMicPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { done(true); return }
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        done(grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
    }

    private fun done(granted: Boolean) {
        // Started while this screen is visible, so the service may add the microphone to its type.
        runCatching {
            startForegroundService(Intent(this, ObiOverlayService::class.java)
                .setAction(ObiOverlayService.ACTION_MIC_READY).putExtra("granted", granted))
        }
        finish()
    }
}
