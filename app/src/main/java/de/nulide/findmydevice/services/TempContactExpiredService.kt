package de.nulide.findmydevice.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.AccessRepository
import de.nulide.findmydevice.transports.SmsTransport
import de.nulide.findmydevice.utils.log
import kotlinx.coroutines.launch

class TempContactExpiredService : FmdJobService() {

    override val TAG = TempContactExpiredService::class.java.simpleName

    companion object {
        private val FIVE_MINS_MILLIS = 5 * 60 * 1000L

        @JvmStatic
        fun scheduleJob(context: Context, initialDelay: Int) {
            val serviceComponent = ComponentName(context, TempContactExpiredService::class.java)

            // We need a unique jobId so that if multiple different phone numbers access
            // FMD concurrently, each of them gets their own ExpiredService.
            val jobId = System.currentTimeMillis().toInt()

            val builder = JobInfo.Builder(jobId, serviceComponent)
                .setMinimumLatency(initialDelay.toLong())
                .setOverrideDeadline(initialDelay + FIVE_MINS_MILLIS)

            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler.schedule(builder.build())
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        super.onStartJob(params)

        val context = this
        val repo = AccessRepository.getInstance(context)

        coroutineScope.launch {
            val expired = repo.deleteExpiredTempPhoneNumbers()

            for (item in expired) {
                val transport = SmsTransport(context, item.number, item.subscriptionId)
                transport.send(context, getString(R.string.temporary_allowlist_expired))
                context.log().i(TAG, "Phone number expired ${item.number}")
            }
            jobFinished()
        }

        // Continue running in coroutine
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        super.onStopJob(params)
        return false
    }
}
