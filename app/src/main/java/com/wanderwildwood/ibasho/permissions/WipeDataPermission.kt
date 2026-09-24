package com.wanderwildwood.ibasho.permissions

import android.app.Activity
import android.app.admin.DeviceAdminInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.annotation.StringRes
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.receiver.DeviceAdminReceiver

/**
 * Device admin that includes permission to wipe, which is not the same as device admin.
 *
 * Android fixes an admin's permissions at the moment it is switched on. A phone that switched
 * it on when this app declared only force-lock keeps exactly that after updating, although the
 * app now declares wipe-data too: it is still an admin, and asking it to wipe throws. So this
 * checks for the wipe policy itself, and asks again the same way the first time did, which
 * Android answers with the approval screen for the new permission alone.
 */
class WipeDataPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_device_admin_wipe_name

    override fun isGranted(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceAdminReceiver::class.java)
        return dpm.isAdminActive(admin) &&
            dpm.hasGrantedPolicy(admin, DeviceAdminInfo.USES_POLICY_WIPE_DATA)
    }

    override fun request(activity: Activity) = DeviceAdminPermission().request(activity)
}
