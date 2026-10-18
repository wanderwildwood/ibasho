package com.wanderwildwood.ibasho.permissions

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.services.NotificationListenService


class NotificationAccessPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_notification_access_name

    override fun isGranted(context: Context): Boolean {
        val cn = ComponentName(context, NotificationListenService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return nm.isNotificationListenerAccessGranted(cn)
        } else {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners" // Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
            )
            return flat != null && flat.contains(cn.flattenToString())
        }
    }

    override fun request(activity: Activity) {
        // Android 13 and later grey this switch out for an app installed from a file until
        // restricted settings are allowed for it, and say nothing about how. Where that is in
        // force, say it instead of opening a list whose switch cannot be moved. On Android 12
        // (MuditaOS) there is no such restriction, and nothing changes.
        if (RestrictedSettings.isRestricted(activity)) {
            RestrictedSettings.showHint(activity)
            return
        }
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        activity.startActivity(intent)
    }
}
