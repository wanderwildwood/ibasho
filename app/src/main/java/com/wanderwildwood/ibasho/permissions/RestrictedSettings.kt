package com.wanderwildwood.ibasho.permissions

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
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

    /**
     * True where the restriction can be in force for this app: Android 13 or later, installed
     * from a file (the package installer records that), and not lifted. On Android 13 and 14
     * lifting it sets the app-op to allowed; from Android 15 Android keeps that elsewhere, so
     * there this is answered only after a request has already come back refused.
     */
    fun isRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val fromFile = runCatching {
            val source = context.packageManager.getInstallSourceInfo(context.packageName).packageSource
            source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE ||
                source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
        }.getOrDefault(false)
        if (!fromFile) return false
        val lifted = runCatching {
            val ops = context.getSystemService(AppOpsManager::class.java)
            ops.unsafeCheckOpNoThrow(OP, context.applicationInfo.uid, context.packageName) ==
                AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
        return !lifted
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
