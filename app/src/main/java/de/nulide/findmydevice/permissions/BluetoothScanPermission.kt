package de.nulide.findmydevice.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.nulide.findmydevice.R


@RequiresApi(Build.VERSION_CODES.S)
class BluetoothScanPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_bluetooth_scan_name

    override fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PERMISSION_GRANTED
    }

    val REQUEST_CODE = 8096

    override fun request(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_CODE
        )
    }
}
