package com.wanderwildwood.ibasho.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wanderwildwood.ibasho.FmdApplication

/**
 * The connection turned off, by hand, until it is turned on again.
 *
 * Asked for on the forum: there was no way to close the connection short of removing the
 * server account. Off means the push connection is closed and the server jobs -- the location
 * upload, the connectivity check, the low-battery report -- stop; nothing about the account or
 * the push registration is thrown away, so turning it on again picks up where it was. SMS
 * commands keep working, since they need no connection.
 */
object ConnectionPause {

    private const val PREFS = "connection_pause"
    private const val KEY = "paused"

    fun isPaused(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setPaused(context: Context, paused: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, paused).apply()
        (context.applicationContext as FmdApplication).restartServices()
    }

    /** "Turn off" on the connection's own notification. */
    class TurnOffReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = setPaused(context, true)
    }
}
