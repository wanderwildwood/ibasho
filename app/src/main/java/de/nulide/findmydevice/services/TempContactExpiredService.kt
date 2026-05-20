package de.nulide.findmydevice.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import de.nulide.findmydevice.data.AccessRepository
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
            repo.removeAndNotifyExpiredTempPhoneNumbers()
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
