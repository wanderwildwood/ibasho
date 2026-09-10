package com.wanderwildwood.ibasho.services

import android.content.Context
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.net.FmdServerRepository
import com.wanderwildwood.ibasho.net.ServerCommandDownloader
import com.wanderwildwood.ibasho.net.ServerError
import com.wanderwildwood.ibasho.ui.settings.FMDServerActivity
import com.wanderwildwood.ibasho.utils.Notifications
import com.wanderwildwood.ibasho.utils.log
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage


class UnifiedPushService : PushService() {
    companion object {
        val TAG: String = UnifiedPushService::class.java.getSimpleName()
    }

    override fun onMessage(message: PushMessage, instance: String) {
        log().i(TAG, "Received push message")
        ServerCommandDownloader(this).download()
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        log().i(TAG, "Received new push endpoint")

        val settings = SettingsRepository.getInstance(this)
        settings.set(Settings.SET_FMDSERVER_PUSH_URL, endpoint.url)

        val repo = FmdServerRepository(this).getApiService()
        repo.registerPushEndpoint(endpoint.url, { _: ServerError ->
            val context = this
            Notifications.notify(
                context,
                title = context.getString(R.string.push_registration_failed_title),
                text = context.getString(R.string.push_registration_upload_failed_text),
                channelID = Notifications.CHANNEL_SERVER,
                cls = FMDServerActivity::class.java,
            )
        })
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        // do nothing
        log().e(TAG, "Push registration failed, reason=$reason")
    }

    override fun onUnregistered(instance: String) {
        clearPushState(this)
    }
}

fun isUnifiedPushAvailable(context: Context): Boolean {
    return UnifiedPush.getDistributors(context).isNotEmpty()
}

fun isRegisteredWithUnifiedPush(context: Context): Boolean {
    return UnifiedPush.getAckDistributor(context).isNullOrEmpty().not()
}

// fun registerWithUnifiedPush: see FMDServerActivity

fun unregisterWithUnifiedPush(context: Context) {
    if (isRegisteredWithUnifiedPush(context)) {
        UnifiedPush.unregister(context)
    }
    // ensure that the state is cleared
    clearPushState(context)
}

private fun clearPushState(context: Context) {
    val settings: SettingsRepository = SettingsRepository.getInstance(context)
    settings.set(Settings.SET_FMDSERVER_PUSH_URL, "")

    // Either we have triggered the push deregistration (after server account deletion),
    // or someone else (e.g., the distributor itself) has triggered the deregistration.
    // In the latter case, inform the server.
    if (settings.serverAccountExists()) {
        val repo = FmdServerRepository(context).getApiService()
        repo.registerPushEndpoint("", { _ -> /* noop */ })
    }
}
