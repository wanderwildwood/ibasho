package de.nulide.findmydevice.locationproviders

import android.Manifest
import android.content.Context
import android.net.wifi.ScanResult
import androidx.annotation.RequiresPermission
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.FmdLocation
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.BeaconDbRepository
import de.nulide.findmydevice.net.OpenCelliDRepository
import de.nulide.findmydevice.net.OpenCelliDSpec
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.CellParameters
import de.nulide.findmydevice.utils.Utils
import de.nulide.findmydevice.utils.WifiScan
import de.nulide.findmydevice.utils.log
import de.nulide.findmydevice.utils.prettyPrint
import de.nulide.findmydevice.utils.requestCellInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred


/**
 * Only call this provider via the LocateCommand!
 * (because it handles things like LocationAutoOnOff centrally)
 */
class CellLocationProvider<T>(
    private val context: Context,
    private val transport: Transport<T>,
) : LocationProvider() {

    companion object {
        private val TAG = CellLocationProvider::class.simpleName
    }

    private var deferred = CompletableDeferred<Unit>()

    @Volatile
    private var ocidFinished = false

    @Volatile
    private var beaconDbFinished = false

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override suspend fun getAndSendLocation(): Deferred<Unit> {
        deferred = CompletableDeferred<Unit>()
        ocidFinished = false
        beaconDbFinished = false

        requestCellInfo(context, this::onCellInfoUpdate)
        return deferred
    }

    private fun onCellInfoUpdate(cellParas: List<CellParameters>) {
        if (cellParas.isEmpty()) {
            context.log().i(TAG, "Cell paras are null. Are you connected to the cellular network?")
            transport.send(context, context.getString(R.string.OpenCellId_test_no_connection))

            // Cannot try OpenCelliD
            ocidFinished = true

            // Still try WiFi networks
            WifiScan(context, { scanResults ->
                queryBeaconDb(emptyList(), scanResults)
            }).startWifiScan()
            return
        }

        // Since internally both repositories use Volley with callbacks, the requests don't block on each other.
        queryOpenCelliD(cellParas.first())

        WifiScan(context, { scanResults ->
            queryBeaconDb(cellParas, scanResults)
        }).startWifiScan()
    }

    private fun queryOpenCelliD(paras: CellParameters) {
        val settings = SettingsRepository.getInstance(context)
        val apiAccessToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String
        if (apiAccessToken.isEmpty()) {
            val msg = "Cannot query OpenCelliD: Missing API Token"
            context.log().i(TAG, msg)
            transport.send(context, msg)
            ocidFinished = true
            onFinished()
            return
        }

        context.log().d(TAG, "Querying OpenCelliD")
        val ocidRepo = OpenCelliDRepository.getInstance(OpenCelliDSpec(context))
        ocidRepo.getCellLocation(
            paras, apiAccessToken,
            onSuccess = {
                context.log().d(TAG, "Location found by OpenCelliD")

                val loc = FmdLocation(
                    lat = it.lat,
                    lon = it.lon,
                    provider = "OpenCelliD",
                    batteryLevel = Utils.getBatteryLevel(context),
                    timeMillis = paras.timeMillis,
                )

                settings.storeLastKnownLocation(loc)
                transport.sendNewLocation(context, loc)
                ocidFinished = true
                onFinished()
            },
            onError = {
                context.log().i(TAG, "Failed to get location from OpenCelliD")
                val msg = context.getString(
                    R.string.cmd_locate_response_opencellid_failed,
                    it.url,
                    paras.prettyPrint()
                )
                transport.send(context, msg)
                ocidFinished = true
                onFinished()
            },
        )
    }

    private fun queryBeaconDb(
        cellParas: List<CellParameters>,
        wifiParas: List<ScanResult>,
    ) {
        context.log().d(TAG, "Querying BeaconDB")

        // Use the most recent of the timestamps
        val timeMillis = cellParas.maxOfOrNull { it.timeMillis } ?: System.currentTimeMillis()

        val beaconDbRepo = BeaconDbRepository.getInstance(context)
        beaconDbRepo.getCellLocation(
            cellParas,
            wifiParas,
            onSuccess = { beaconDb ->
                context.log().d(TAG, "Location found by BeaconDB")

                val loc = FmdLocation(
                    lat = beaconDb.lat,
                    lon = beaconDb.lon,
                    accuracy = beaconDb.accuracy?.toFloat(),
                    provider = "BeaconDB",
                    batteryLevel = Utils.getBatteryLevel(context),
                    timeMillis = timeMillis,
                )

                val settings = SettingsRepository.getInstance(context)
                settings.storeLastKnownLocation(loc)
                transport.sendNewLocation(context, loc)
                beaconDbFinished = true
                onFinished()
            },
            onError = {
                context.log().i(TAG, "Failed to get location from BeaconDB")
                val msg = context.getString(
                    R.string.cmd_locate_response_beacondb_failed,
                    cellParas.prettyPrint()
                )
                transport.send(context, msg)
                beaconDbFinished = true
                onFinished()
            },
        )
    }

    private fun onFinished() {
        if (ocidFinished && beaconDbFinished) {
            deferred.complete(Unit)
        }
    }
}
