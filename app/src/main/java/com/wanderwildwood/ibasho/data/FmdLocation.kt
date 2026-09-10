package com.wanderwildwood.ibasho.data

import android.content.Context
import android.location.Location
import com.wanderwildwood.ibasho.utils.Utils
import com.wanderwildwood.ibasho.utils.Utils.Companion.getOpenStreetMapLink
import org.json.JSONException
import org.json.JSONObject
import java.util.Date


data class FmdLocation(
    val lat: Double,
    val lon: Double,

    /** Horizontal radius in meter */
    val accuracy: Float? = null,
    /** Height above sea level in meter */
    val altitude: Double? = null,
    /** Horizontal direction of travel between 0.0 and 360.0 */
    val bearing: Float? = null,
    /** Speed in m/s */
    val speed: Float? = null,

    val provider: String,
    val batteryLevel: Int,

    // Or this? -> Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
    val timeMillis: Long = System.currentTimeMillis(),
) {

    companion object {
        fun fromAndroidLocation(context: Context, loc: Location): FmdLocation {
            return FmdLocation(
                lat = loc.latitude,
                lon = loc.longitude,
                accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
                altitude = if (loc.hasAltitude()) loc.altitude else null,
                bearing = if (loc.hasBearing()) loc.bearing else null,
                speed = if (loc.hasSpeed()) loc.speed else null,
                provider = loc.provider ?: "GPS",
                batteryLevel = Utils.getBatteryLevel(context),
                timeMillis = loc.time,
            )
        }
    }

    override fun toString(): String {
        val string = StringBuilder()
            .append("$provider:\n")
            .append("Lat: $lat\n")
            .append("Lon: $lon\n")

        if (accuracy != null) {
            string.append("Accuracy: %.1f m\n".format(accuracy))
        }
        if (altitude != null) {
            string.append("Altitude: %.1f m\n".format(altitude))
        }
        if (bearing != null) {
            string.append("Bearing: %.0f\n".format(bearing))
        }
        if (speed != null) {
            string.append("Speed: %.1f m/s = %.1f km/h\n".format(speed, speed * 3.6))
        }

        string.append("Time: ${Date(timeMillis)}\n")
            .append("Battery: $batteryLevel %\n")
            .append(getOpenStreetMapLink(lat, lon))
        return string.toString()
    }

    fun encodeToJson(): String {
        val obj = JSONObject()
        try {
            obj.put("provider", this.provider)

            obj.put("lat", this.lat)
            obj.put("lon", this.lon)

            obj.put("accuracy", this.accuracy)
            obj.put("altitude", this.altitude)
            obj.put("heading", this.bearing)
            obj.put("speed", this.speed)

            obj.put("bat", this.batteryLevel)
            obj.put("date", this.timeMillis)
            obj.put("time", Date(this.timeMillis).toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return obj.toString()
    }
}
