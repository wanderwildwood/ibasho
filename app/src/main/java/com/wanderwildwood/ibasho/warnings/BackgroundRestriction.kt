package com.wanderwildwood.ibasho.warnings

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Android can restrict an app's background use, and when it does, this one stops
 * working completely and says nothing: every scheduled job still fires on time,
 * then fails to bind to its service and is dropped. Locations simply stop
 * arriving, which for an app whose whole job is to say where a phone is, is the
 * worst way to fail -- you find out when you need it.
 *
 * Being exempt from battery optimisation is a different setting and does not
 * cover this, which is the trap: the app can look correctly configured and still
 * be unable to run.
 */
fun isBackgroundRestricted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
    val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.isBackgroundRestricted
}

/** Opens this app's own settings page, where the restriction can be lifted. */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
