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
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        activity.startActivity(intent)
        // Android 13 and later grey this switch out for an app installed from a file until
        // restricted settings are allowed for it, and do not say how. So when the person comes
        // back and access is still not granted, and that restriction can be the reason, say it.
        // On Android 12 (MuditaOS) there is no such restriction, and nothing changes.
        if (!RestrictedSettings.isRestricted(activity)) return
        activity.application.registerActivityLifecycleCallbacks(object : SimpleLifecycle() {
            private var left = false
            override fun onActivityPaused(a: Activity) { if (a === activity) left = true }
            override fun onActivityResumed(a: Activity) {
                if (a !== activity || !left) return
                activity.application.unregisterActivityLifecycleCallbacks(this)
                if (!isGranted(activity)) RestrictedSettings.showHint(activity)
            }
            override fun onActivityDestroyed(a: Activity) {
                if (a === activity) activity.application.unregisterActivityLifecycleCallbacks(this)
            }
        })
    }

    private open class SimpleLifecycle : android.app.Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(a: Activity, b: android.os.Bundle?) = Unit
        override fun onActivityStarted(a: Activity) = Unit
        override fun onActivityResumed(a: Activity) = Unit
        override fun onActivityPaused(a: Activity) = Unit
        override fun onActivityStopped(a: Activity) = Unit
        override fun onActivitySaveInstanceState(a: Activity, b: android.os.Bundle) = Unit
        override fun onActivityDestroyed(a: Activity) = Unit
    }
}
