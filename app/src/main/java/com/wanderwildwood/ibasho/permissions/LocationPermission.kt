package com.wanderwildwood.ibasho.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.warnings.openAppSettings


class LocationPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_location_name

    fun isForegroundGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }

    fun isBackgroundGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PERMISSION_GRANTED
        } else true
    }

    override fun isGranted(context: Context): Boolean {
        return isForegroundGranted(context) && isBackgroundGranted(context)
    }

    val REQUEST_CODE = CODE

    override fun request(activity: Activity) {
        // We cannot request ACCESS_FINE_LOCATION and ACCESS_BACKGROUND_LOCATION at the same time
        // The must be granted one after the other.
        // Thus this method will usually be called twice (since calling it once will not get you both permissions).
        if (!isForegroundGranted(activity.applicationContext)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE
            )
            return
        }
        if (!isBackgroundGranted(activity.applicationContext)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            backgroundAskedAt = SystemClock.elapsedRealtime()
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_CODE
            )
        }
    }

    companion object {
        private const val CODE = 8050
        private const val SILENT_MS = 500L
        private var backgroundAskedAt = 0L

        /**
         * Once "all the time" has been turned down twice, Android answers the request at once
         * without showing anything, and the Grant button looks dead. A refusal that comes back
         * faster than anyone could press is that case: open the app's own settings page, where
         * Permissions > Location still offers "Allow all the time".
         */
        fun onResult(
            activity: Activity, requestCode: Int, permissions: Array<out String>, results: IntArray,
        ) {
            if (requestCode != CODE) return
            val i = permissions.indexOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if (i < 0 || results.getOrNull(i) == PERMISSION_GRANTED) return
            val took = SystemClock.elapsedRealtime() - backgroundAskedAt
            backgroundAskedAt = 0L
            if (took in 0..SILENT_MS) openAppSettings(activity)
        }
    }
}
