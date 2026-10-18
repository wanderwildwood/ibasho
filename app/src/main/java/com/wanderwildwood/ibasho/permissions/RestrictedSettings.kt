package com.wanderwildwood.ibasho.permissions

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.common.EInkAlertDialogBuilder

/**
 * Android's "restricted settings": from Android 13 an app installed from a file may not be given
 * notification access (and a few other things) until the person allows it on the app's info
 * page, under ⋮ → Allow restricted settings. Nothing on screen says so. This does, and only
 * while the restriction is in force for this app.
 */
object RestrictedSettings {

    private const val OP = "android:access_restricted_settings"

    fun isRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return runCatching {
            val ops = context.getSystemService(AppOpsManager::class.java)
            // Errored is what Android sets for an app installed from a file; allowing restricted
            // settings sets it to allowed. Anything else is not this restriction.
            val mode = ops.unsafeCheckOpNoThrow(OP, context.applicationInfo.uid, context.packageName)
            mode == AppOpsManager.MODE_ERRORED || mode == AppOpsManager.MODE_IGNORED
        }.getOrDefault(false)
    }

    fun showHint(activity: Activity) {
        EInkAlertDialogBuilder(activity)
            .setMessage(R.string.restricted_settings_hint)
            .setPositiveButton(R.string.restricted_settings_app_info) { _, _ ->
                runCatching {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${activity.packageName}"),
                        )
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
