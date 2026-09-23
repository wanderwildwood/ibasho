package com.wanderwildwood.ibasho.warnings

import android.content.Context
import android.os.Build
import com.wanderwildwood.ibasho.data.BackgroundLocationType
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.utils.Utils

/**
 * Whether the regular uploads are still happening, and if not, which of two quite different
 * things is wrong. A phone that stops the app from running and a phone that runs it indoors
 * with no fix look the same from the server: nothing arrives. They need different answers.
 */
enum class UploadStall {
    /** Uploading, or not meant to be. */
    NONE,

    /** The upload job has not started for a long while: the phone is not letting it run. */
    NOT_RUNNING,

    /** The job runs, but no location has reached the server in that time: no fix. */
    NO_FIX,
}

// Three missed uploads, and never less than three hours, so one bad hour is not an alarm.
private const val MISSED_UPLOADS = 3
private const val MIN_STALL_MILLIS = 3 * 60 * 60 * 1000L

fun uploadStall(context: Context): UploadStall {
    val settings = SettingsRepository.getInstance(context)
    if (!settings.serverAccountExists()) return UploadStall.NONE

    val locType = BackgroundLocationType(
        (settings.get(Settings.SET_FMDSERVER_LOCATION_TYPE) as Number).toInt()
    )
    if (locType.isEmpty()) return UploadStall.NONE

    val intervalMillis =
        (settings.get(Settings.SET_FMDSERVER_UPDATE_TIME) as Number).toLong() * 60 * 1000L
    val threshold = maxOf(MISSED_UPLOADS * intervalMillis, MIN_STALL_MILLIS)
    val now = System.currentTimeMillis()

    val lastJob = (settings.get(Settings.SET_FMDSERVER_LAST_UPLOAD_JOB_MILLIS) as Number).toLong()
    // Zero means the job has never been scheduled on this version; nothing to judge yet.
    if (lastJob == 0L) return UploadStall.NONE
    if (now - lastJob > threshold) return UploadStall.NOT_RUNNING

    val lastUpload =
        (settings.get(Settings.SET_FMDSERVER_LAST_LOCATION_UPLOAD_TIME) as Number).toLong()
    // -1 means nothing has been uploaded yet on this install; a fresh one is not "stalled".
    if (lastUpload > 0L && now - lastUpload > threshold) return UploadStall.NO_FIX

    return UploadStall.NONE
}

/**
 * A Mudita Kompakt restricts installed apps in the background and offers no setting on the
 * phone to lift it: its App info has no Battery page. The way out is a one-time step from a
 * computer, which the README describes.
 */
fun isMuditaKompakt(): Boolean = Build.MANUFACTURER.equals("Mudita", ignoreCase = true)

const val KOMPAKT_HELP_URL = "https://github.com/wanderwildwood/ibasho#on-a-mudita-kompakt"

fun openKompaktHelp(context: Context) = Utils.openUrl(context, KOMPAKT_HELP_URL)
