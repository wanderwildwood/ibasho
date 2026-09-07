package de.nulide.findmydevice.commands

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.permissions.BluetoothConnectPermission
import de.nulide.findmydevice.permissions.BluetoothScanPermission
import de.nulide.findmydevice.permissions.LocationPermission
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.BluetoothScan
import kotlinx.coroutines.CompletableDeferred


class BluetoothScanCommand(context: Context) : Command(context) {

    override val keyword = "bluetoothscan"
    override val usage = "bluetoothscan"

    override val permission = FmdPermission.BLUETOOTH_SCAN

    @get:DrawableRes
    override val icon = R.drawable.ic_bluetooth

    @get:StringRes
    override val shortDescription = R.string.cmd_bluetoothscan_description_short

    override val longDescription = R.string.cmd_bluetoothscan_description_long

    override val requiredPermissions: List<Permission> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(BluetoothConnectPermission(), BluetoothScanPermission())
        } else {
            listOf(LocationPermission())
        }

    @SuppressLint("MissingPermission")
    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val bluetoothManager: BluetoothManager =
            context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            transport.send(context, context.getString(R.string.cmd_bluetooth_response_no_bluetooth))
            return
        }

        // Users should manually turn Bluetooth on via the separate command
        // TODO: Consider making the LocationAutoOnOffHandler generic.
        if (!bluetoothAdapter.isEnabled) {
            transport.send(context, context.getString(R.string.cmd_bluetooth_response_is_off))
            return
        }

        // Devices that are currently connected (e.g. via BLE/GATT), even if the discovery scan
        // below does not (re-)discover them while a connection is already established.
        val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)

        val deferred = CompletableDeferred<Unit>()

        BluetoothScan(context, { nearbyDevices ->
            val devicesString = (connectedDevices + nearbyDevices)
                .distinctBy { d -> d.address }
                .sortedBy { d -> d.name == null } // list devices that have a name at the top
                .joinToString("\n\n") { d ->
                    context.getString(
                        R.string.cmd_bluetoothscan_response_item,
                        d.name ?: "",
                        d.address
                    )
                }

            val reply = context.getString(R.string.cmd_bluetoothscan_response, devicesString)

            transport.send(context, reply)
            deferred.complete(Unit)
        }).startScan()

        deferred.await()
    }
}
