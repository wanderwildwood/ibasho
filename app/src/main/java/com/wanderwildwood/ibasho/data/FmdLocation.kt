package com.wanderwildwood.ibasho.data

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.utils.Utils
import com.wanderwildwood.ibasho.utils.Utils.Companion.getOpenStreetMapLink
import kotlinx.serialization.Serializable
import org.json.JSONException
import org.json.JSONObject
import java.util.Date


@Serializable
data class FmdLocation(
    val lat: Double,
    val lon: Double,

    /** Horizontal radius in meter */
    val accuracy: Float? = null,

    /** Height above sea level in meter */
    val altitude: Double? = null,

    /** Horizontal direction of travel between 0.0 and 360.0 */
    @SerializedName("heading")
    val bearing: Float? = null,

    /** Speed in m/s */
    val speed: Float? = null,

    val provider: String,

    @SerializedName("bat")
    val batteryLevel: Int,

    // Or this? -> Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
    @SerializedName("date")
    val timeMillis: Long = System.currentTimeMillis(),
) {

    companion object {
        private val gson = Gson()

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

        fun decodeFromJson(jsonStr: String): FmdLocation? {
            return try {
                gson.fromJson(jsonStr, FmdLocation::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /** The location reply sent back to the asker, with its labels from strings.xml. */
    fun toReply(context: Context): String {
        val string = StringBuilder()
            .append("$provider:\n")
            .append(context.getString(R.string.location_reply_lat, lat.toString())).append('\n')
            .append(context.getString(R.string.location_reply_lon, lon.toString())).append('\n')

        if (accuracy != null) {
            string.append(context.getString(R.string.location_reply_accuracy, accuracy)).append('\n')
        }
        if (altitude != null) {
            string.append(context.getString(R.string.location_reply_altitude, altitude)).append('\n')
        }
        if (bearing != null) {
            string.append(context.getString(R.string.location_reply_bearing, bearing)).append('\n')
        }
        if (speed != null) {
            string.append(context.getString(R.string.location_reply_speed, speed, speed * 3.6))
                .append('\n')
        }

        string.append(context.getString(R.string.location_reply_time, Date(timeMillis).toString()))
            .append('\n')
            .append(context.getString(R.string.location_reply_battery, batteryLevel)).append('\n')
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
