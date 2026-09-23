package com.wanderwildwood.ibasho

import android.app.Application
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wanderwildwood.ibasho.data.AllowlistRepository
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.data.UncaughtExceptionHandler.Companion.initUncaughtExceptionHandler
import com.wanderwildwood.ibasho.push.EmbeddedPush
import com.wanderwildwood.ibasho.push.EmbeddedPushStore
import com.wanderwildwood.ibasho.push.PushChoice
import com.wanderwildwood.ibasho.services.FmdBatteryLowService
import com.wanderwildwood.ibasho.services.ServerConnectivityCheckService
import com.wanderwildwood.ibasho.services.ServerLocationUploadService
import com.wanderwildwood.ibasho.services.unregisterWithUnifiedPush
import com.wanderwildwood.ibasho.utils.NetworkUtils.isNetworkAvailable
import com.wanderwildwood.ibasho.utils.Notifications
import com.wanderwildwood.ibasho.utils.log
import com.wanderwildwood.ibasho.warnings.notifyWarnUnifiedPushRequired
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.INSTANCE_DEFAULT
import org.unifiedpush.android.connector.UnifiedPush
import java.io.File


class FmdApplication : Application() {

    companion object {
        private val TAG = FmdApplication::class.java.simpleName

        private val TEMP_ALLOWLIST_FILENAME = "temporary_allowlist.json"
    }

    // Workaround to "pass" this from the NotificationListenerService to the CommandExecutionWorker.
    // The problem is that we cannot pass objects between them directly.
    // But we also cannot retrieve the notification in the worker by ID,
    // because notificationManager.activeNotifications only returns the notifications posted by our own app.
    //
    // Mark this as @Volatile to ensure that other threads can see changes (such as potential worker threads).
    @Volatile
    var latestStatusBarNotification: StatusBarNotification? = null

    override fun onCreate() {
        super.onCreate()

        this.log().i(TAG, "Starting FmdApplication")

        Notifications.init(this)
        initUncaughtExceptionHandler(this)

        doUpdateMigrations(this)

        restartServices()
    }

    private fun doUpdateMigrations(context: Context) {
        val settings = SettingsRepository.getInstance(context)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            settings.migrateSettings()
            AllowlistRepository.getInstance(context).migrateAllowlist()
        }

        // Cleanup old file
        val file = File(TEMP_ALLOWLIST_FILENAME)
        if (file.exists()) {
            file.delete()
        }

    }

    fun restartServices() {
        val settings = SettingsRepository.getInstance(this)
        if (settings.serverAccountExists()) {
            // Scheduling a job that is already running should be fine (?),
            // because they have the same, fixed JOB_ID.
            if (settings.get(Settings.SET_FMD_LOW_BAT_SEND) as Boolean) {
                FmdBatteryLowService.scheduleJobNow(this)
            }
            ServerLocationUploadService.scheduleRecurring(this)
            ServerConnectivityCheckService.scheduleJob(this)

            restartPush()
        } else {
            FmdBatteryLowService.cancelJob(this)
            ServerLocationUploadService.cancelJob(this)
            ServerConnectivityCheckService.cancelJob(this)

            if (EmbeddedPushStore(this).debugWithoutAccount) {
                // A debug build testing push without an account (see DebugPushReceiver)
                restartPush()
            } else {
                unregisterWithUnifiedPush(this)
                EmbeddedPush.sync(this)
            }
        }
    }

    private fun restartPush() {
        PushChoice.migrate(this)
        if (PushChoice.isSetUp(this)) {
            if (isNetworkAvailable(this)) {
                // Re-register with the saved distributor, to keep the registration fresh.
                // Doing this on each Application start is important, because e.g. UP library upgrades
                // can reset internal state. A re-registration resolves this automatically.
                this.log().i(TAG, "Renewing push registration")
                UnifiedPush.register(this, INSTANCE_DEFAULT, null, null)
            } else {
                this.log().i(TAG, "Skipping push renewal (no network)")
            }
        } else if (!PushChoice.registerIfPossible(this)) {
            // Another app is chosen, and there is none, or more than one to choose from.
            notifyWarnUnifiedPushRequired(this)
        }
        EmbeddedPush.sync(this)
    }
}
