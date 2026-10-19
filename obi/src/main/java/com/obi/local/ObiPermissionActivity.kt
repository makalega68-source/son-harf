package com.obi.local

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle

/**
 * Invisible screen that asks for the permissions a command needs (contacts for "annemi ara",
 * calendar for "bugün ne var"…), then gives the same command back to Obi to finish.
 */
class ObiPermissionActivity : Activity() {
    companion object {
        const val EXTRA_PERMISSIONS = "permissions"
        const val EXTRA_COMMAND = "command"

        fun ask(context: Context, permissions: Array<String>, command: String) {
            runCatching {
                context.startActivity(Intent(context, ObiPermissionActivity::class.java)
                    .putExtra(EXTRA_PERMISSIONS, permissions).putExtra(EXTRA_COMMAND, command)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wanted = intent.getStringArrayExtra(EXTRA_PERMISSIONS).orEmpty()
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isEmpty()) { done(true); return }
        requestPermissions(wanted.toTypedArray(), 8)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // The first permission is the one the command cannot do without; the rest are optional.
        done(grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
    }

    private fun done(granted: Boolean) {
        runCatching {
            startForegroundService(Intent(this, ObiOverlayService::class.java)
                .setAction(ObiOverlayService.ACTION_PERMISSION_READY)
                .putExtra("granted", granted)
                .putExtra(EXTRA_COMMAND, intent.getStringExtra(EXTRA_COMMAND)))
        }
        finish()
    }
}
