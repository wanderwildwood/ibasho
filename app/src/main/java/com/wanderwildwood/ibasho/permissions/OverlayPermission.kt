package com.wanderwildwood.ibasho.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import com.wanderwildwood.ibasho.R


class OverlayPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_overlay_name

    override fun isGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    override fun request(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + activity.packageName)
        )
        activity.startActivity(intent)
    }
}
