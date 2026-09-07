package de.nulide.findmydevice.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.IntentCompat

class BluetoothScan(
    private val context: Context,
    private val onScanResults: (List<BluetoothDevice>) -> Unit,
) {

    companion object {
        private val TAG = BluetoothScan::class.simpleName
    }

    private val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

    private val foundDevices = mutableListOf<BluetoothDevice>()

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = IntentCompat.getParcelableExtra(
                        intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
                    )
                    if (device != null && foundDevices.none { it.address == device.address }) {
                        foundDevices.add(device)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    context.log().i(
                        TAG,
                        "Bluetooth discovery finished. Found ${foundDevices.size} device(s)."
                    )
                    onResult()
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // BLUETOOTH_SCAN / BLUETOOTH_ADMIN, checked via Permission classes before this is called
    fun startScan() {
        if (bluetoothAdapter == null) {
            onScanResults(emptyList())
            return
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        context.registerReceiver(discoveryReceiver, intentFilter)

        val success = bluetoothAdapter.startDiscovery()
        context.log().i(TAG, "Started Bluetooth discovery")

        if (!success) {
            context.log().e(TAG, "Starting Bluetooth discovery failed.")
            onResult()
        }
    }

    private fun onResult() {
        context.unregisterReceiver(this.discoveryReceiver)
        onScanResults(foundDevices)
    }
}
