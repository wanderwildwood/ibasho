package com.wanderwildwood.ibasho.push

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.utils.log

/**
 * The built-in UnifiedPush distributor: Whereabouts is its own push app.
 *
 * The UnifiedPush connector counts any app with a receiver for the distributor's REGISTER
 * action as a distributor, and that includes this app's own unexported
 * [EmbeddedDistributorReceiver]. So the connector registers with this one exactly as it
 * would with Sunup, and gets its endpoint and its messages back through the same
 * broadcasts: [com.wanderwildwood.ibasho.services.UnifiedPushService] cannot tell the
 * difference, and nothing downstream of it changed.
 *
 * Behind it is one WebSocket to Mozilla's push server, held by [PushConnectionService],
 * the way Sunup (https://codeberg.org/Sunup/android) holds it.
 */
object EmbeddedPush {
    const val TAG = "EmbeddedPush"

    // The UnifiedPush Android spec's actions and extras, as the connector uses them.
    // https://unifiedpush.org/developers/spec/android/
    const val ACTION_REGISTER = "org.unifiedpush.android.distributor.REGISTER"
    const val ACTION_UNREGISTER = "org.unifiedpush.android.distributor.UNREGISTER"
    private const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
    private const val ACTION_REGISTRATION_FAILED = "org.unifiedpush.android.connector.REGISTRATION_FAILED"
    private const val ACTION_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"
    const val EXTRA_TOKEN = "token"
    const val EXTRA_APPLICATION = "application"
    const val EXTRA_VAPID = "vapid"
    private const val EXTRA_ENDPOINT = "endpoint"
    private const val EXTRA_BYTES_MESSAGE = "bytesMessage"
    private const val EXTRA_REASON = "reason"

    /** Where the WebSocket goes. Sunup's default, and Mozilla's public push server. */
    const val PUSH_SERVER_URL = "https://push.services.mozilla.com"

    /**
     * The connection is held only while there is a server account to be woken for, the
     * built-in distributor is the chosen one, and something is registered with it.
     */
    fun shouldRun(context: Context): Boolean {
        val store = EmbeddedPushStore(context)
        val hasAccount = SettingsRepository.getInstance(context).serverAccountExists() ||
                store.debugWithoutAccount
        return hasAccount && PushChoice.get(context) == PushChoice.BUILT_IN && store.hasChannels()
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Starts or stops the connection to match [shouldRun]. Safe to call from anywhere. */
    fun sync(context: Context) {
        val appContext = context.applicationContext
        if (shouldRun(appContext)) {
            PushRestartWorker.schedulePeriodic(appContext)
            PushConnectionService.start(appContext)
        } else {
            // "Register again" unregisters and registers in one breath: stopping at once would
            // restart the service, and redraw its notification, for nothing. Look again shortly.
            handler.postDelayed({
                if (!shouldRun(appContext)) {
                    PushRestartWorker.cancel(appContext)
                    PushConnectionService.stop(appContext)
                }
            }, 1_000L)
        }
    }

    fun onRegister(context: Context, token: String, vapid: String?) {
        val store = EmbeddedPushStore(context)
        val channel = store.channelFor(token, vapid)
        context.log().i(TAG, "Registration request, channel ${channel.channelId}")
        if (channel.endpoint != null) {
            // Known already: answer at once, as the connector re-registers on every start.
            deliverEndpoint(context, channel.token, channel.endpoint)
        }
        PushConnectionService.instance?.registerPending()
        sync(context)
    }

    fun onUnregister(context: Context, token: String) {
        val channel = EmbeddedPushStore(context).remove(token)
        if (channel != null) {
            context.log().i(TAG, "Unregistered channel ${channel.channelId}")
            PushConnectionService.instance?.unregisterChannel(channel.channelId)
        }
        sync(context)
    }

    fun deliverEndpoint(context: Context, token: String, endpoint: String) {
        context.log().i(TAG, "New endpoint for the connector: $endpoint")
        context.sendBroadcast(toConnector(context, ACTION_NEW_ENDPOINT, token).apply {
            putExtra(EXTRA_ENDPOINT, endpoint)
        })
    }

    fun deliverRegistrationFailed(context: Context, token: String) {
        context.sendBroadcast(toConnector(context, ACTION_REGISTRATION_FAILED, token).apply {
            putExtra(EXTRA_REASON, "INTERNAL_ERROR")
        })
    }

    fun deliverMessage(context: Context, token: String, message: ByteArray) {
        context.log().i(TAG, "Push message received, ${message.size} bytes, handing to the connector")
        context.sendBroadcast(toConnector(context, ACTION_MESSAGE, token).apply {
            putExtra(EXTRA_BYTES_MESSAGE, message)
        })
    }

    private fun toConnector(context: Context, action: String, token: String) =
        Intent(action).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_TOKEN, token)
        }
}
