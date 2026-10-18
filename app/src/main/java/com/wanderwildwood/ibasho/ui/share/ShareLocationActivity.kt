package com.wanderwildwood.ibasho.ui.share

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.permissions.LocationPermission
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import kotlin.math.roundToInt

/**
 * Hands one location to another app, once, when the person says so.
 *
 * Another app starts this for a result with [ACTION]. The screen names that app and asks;
 * on Share it takes one fresh fix and returns it in [EXTRA_LATITUDE], [EXTRA_LONGITUDE],
 * [EXTRA_ACCURACY] (metres, when known) and [EXTRA_TIME] (when the fix was taken, epoch ms).
 * If no fresh fix comes, a recent last known one is offered instead, saying how old it is.
 * Cancel, Back, a fix that never comes, or a start that is not for a result all return
 * RESULT_CANCELED, the last two with [EXTRA_REASON].
 *
 * Nothing is stored and nothing leaves the phone from here: the coordinates go to the app
 * that asked, and what it does with them is up to that app and the person using it. No
 * permission is asked for that Whereabouts does not already hold for its own work.
 */
class ShareLocationActivity : FmdActivity(), LocationListener {

    companion object {
        const val ACTION = "com.wanderwildwood.ibasho.action.SHARE_LOCATION_ONCE"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ACCURACY = "accuracy"
        const val EXTRA_TIME = "time"
        const val EXTRA_REASON = "reason"

        const val REASON_NO_PERMISSION = "no_permission"
        const val REASON_NO_FIX = "no_fix"

        /** How long to wait for a fresh fix before falling back. */
        private const val FIX_TIMEOUT_MS = 60_000L

        /** A fix this good ends the wait at once. */
        private const val GOOD_ENOUGH_M = 30f

        /** After this long, the best fix so far is good enough. */
        private const val SETTLE_MS = 20_000L

        /** A last known fix older than this is not offered. */
        private const val RECENT_MS = 10 * 60_000L
    }

    private sealed interface Step {
        data object Ask : Step
        data object Finding : Step
        data class Stale(val location: Location) : Step
        data object NoPermission : Step
    }

    private var step by mutableStateOf<Step>(Step.Ask)
    private var caller = ""

    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private val handler = Handler(Looper.getMainLooper())
    private var best: Location? = null
    private var startedAt = 0L
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only for a result: with no calling package there is nobody to hand the place to, and
        // nobody to name on the screen.
        val calling = callingPackage
        if (calling == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        caller = appLabel(calling)
        setResult(Activity.RESULT_CANCELED)

        if (!LocationPermission().isForegroundGranted(this)) step = Step.NoPermission

        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    ShareScreen()
                }
            }
        }
    }

    private fun appLabel(pkg: String): String = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ShareScreen() {
        Column(Modifier.fillMaxSize()) {
            TopAppBarMMD(
                title = {
                    TextMMD(
                        stringResource(R.string.share_location_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (val s = step) {
                    Step.Ask -> {
                        TextMMD(
                            stringResource(R.string.share_location_question, caller),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextMMD(stringResource(R.string.share_location_explain, caller))
                        ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = ::startFinding) {
                            TextMMD(stringResource(R.string.share_location_share))
                        }
                        OutlinedButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = ::cancel) {
                            TextMMD(stringResource(R.string.cancel))
                        }
                    }

                    Step.Finding -> {
                        // Words, not a spinner: on this panel a spinner is a smear.
                        TextMMD(
                            stringResource(R.string.share_location_finding),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = ::cancel) {
                            TextMMD(stringResource(R.string.cancel))
                        }
                    }

                    is Step.Stale -> {
                        val minutes = ((System.currentTimeMillis() - s.location.time) / 60_000L)
                            .coerceAtLeast(1L).toInt()
                        TextMMD(
                            stringResource(R.string.share_location_no_fresh_fix),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextMMD(
                            resources.getQuantityString(
                                R.plurals.share_location_last_known_age, minutes, minutes
                            )
                        )
                        ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = { deliver(s.location) }) {
                            TextMMD(stringResource(R.string.share_location_share_last))
                        }
                        OutlinedButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = ::cancel) {
                            TextMMD(stringResource(R.string.cancel))
                        }
                    }

                    Step.NoPermission -> {
                        TextMMD(
                            stringResource(R.string.share_location_no_permission),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButtonMMD(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { cancel(REASON_NO_PERMISSION) },
                        ) {
                            TextMMD(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // checked in onCreate
    private fun startFinding() {
        if (!LocationPermission().isForegroundGranted(this)) {
            step = Step.NoPermission
            return
        }
        step = Step.Finding
        best = null
        startedAt = SystemClock.elapsedRealtime()
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            fallBack()
            return
        }
        // A real fix, as Locate asks for one: the legacy overload settles on low power, which
        // on a phone with no network location is close to asking for nothing.
        for (provider in providers) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.requestLocationUpdates(
                    provider,
                    LocationRequest.Builder(1_000L)
                        .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                        .build(),
                    mainExecutor,
                    this,
                )
            } else {
                locationManager.requestLocationUpdates(provider, 1_000L, 0f, this, Looper.getMainLooper())
            }
        }
        listening = true
        handler.postDelayed(::onTimeout, FIX_TIMEOUT_MS)
    }

    override fun onLocationChanged(location: Location) {
        if (step != Step.Finding) return
        val current = best
        if (current == null || accuracyOf(location) < accuracyOf(current)) best = location
        val chosen = best ?: return
        val settled = SystemClock.elapsedRealtime() - startedAt >= SETTLE_MS
        if (accuracyOf(chosen) <= GOOD_ENOUGH_M || settled) deliver(chosen)
    }

    private fun accuracyOf(location: Location): Float =
        if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE

    private fun onTimeout() {
        if (step != Step.Finding) return
        val chosen = best
        if (chosen != null) deliver(chosen) else fallBack()
    }

    /** No fresh fix: offer the last known one, only if it is recent, and say how old. */
    @SuppressLint("MissingPermission")
    private fun fallBack() {
        stopListening()
        val now = System.currentTimeMillis()
        val fromAndroid = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
        val fromCache = SettingsRepository.getInstance(this).getLastKnownLocation()?.let {
            Location("cache").apply {
                latitude = it.lat
                longitude = it.lon
                it.accuracy?.let { a -> accuracy = a }
                time = it.timeMillis
            }
        }
        val newest = (fromAndroid + listOfNotNull(fromCache)).maxByOrNull { it.time }
        if (newest != null && now - newest.time in 0..RECENT_MS) {
            step = Step.Stale(newest)
        } else {
            cancel(REASON_NO_FIX)
        }
    }

    private fun deliver(location: Location) {
        stopListening()
        val data = Intent()
            .putExtra(EXTRA_LATITUDE, location.latitude)
            .putExtra(EXTRA_LONGITUDE, location.longitude)
            .putExtra(EXTRA_TIME, location.time)
        if (location.hasAccuracy()) data.putExtra(EXTRA_ACCURACY, location.accuracy.roundToInt().toFloat())
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun cancel() = cancel(null)

    private fun cancel(reason: String?) {
        stopListening()
        setResult(
            Activity.RESULT_CANCELED,
            reason?.let { Intent().putExtra(EXTRA_REASON, it) },
        )
        finish()
    }

    private fun stopListening() {
        handler.removeCallbacksAndMessages(null)
        if (listening) {
            locationManager.removeUpdates(this)
            listening = false
        }
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
}
