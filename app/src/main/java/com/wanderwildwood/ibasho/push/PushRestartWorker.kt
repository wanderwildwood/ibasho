package com.wanderwildwood.ibasho.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Reconnects the built-in distributor after a failure, and every 15 minutes checks that it
 * is still up: starts the service if it has died, reconnects if it is down, and pings the
 * server if it is up, so a connection that died silently is found out.
 *
 * After Sunup's RestartWorker (https://codeberg.org/Sunup/android, Apache-2.0, commit
 * fe768d0 "1.3.3"). WorkManager runs it inside Doze's maintenance windows, and only with
 * a network.
 */
class PushRestartWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (!EmbeddedPush.shouldRun(applicationContext)) return Result.success()
        val service = PushConnectionService.instance
        when {
            service == null -> PushConnectionService.start(applicationContext)
            service.isOpen -> service.ping()
            else -> service.connect()
        }
        return Result.success()
    }

    companion object {
        private const val ONCE = "embedded-push-reconnect"
        private const val PERIODIC = "embedded-push-check"

        private val needsNetwork = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun runOnce(context: Context, delayMillis: Long) {
            val work = OneTimeWorkRequestBuilder<PushRestartWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(needsNetwork)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, work)
        }

        fun schedulePeriodic(context: Context) {
            val work = PeriodicWorkRequestBuilder<PushRestartWorker>(15, TimeUnit.MINUTES)
                .setConstraints(needsNetwork)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, work)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(ONCE)
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}
