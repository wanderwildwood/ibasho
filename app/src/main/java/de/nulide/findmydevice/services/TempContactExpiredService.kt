package de.nulide.findmydevice.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.TemporaryAllowlistRepository
import de.nulide.findmydevice.transports.SmsTransport
import de.nulide.findmydevice.utils.log

class TempContactExpiredService : JobService() {

    private val TAG = TempContactExpiredService::class.java.simpleName

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
        val repo = TemporaryAllowlistRepository.getInstance(this)
        val expired = repo.removeExpired()

        for (temporaryPhoneNumber in expired) {
            val transport =
                SmsTransport(this, temporaryPhoneNumber.first, temporaryPhoneNumber.second)
            transport.send(this, getString(R.string.temporary_allowlist_expired))
            this.log().i(TAG, "Phone number expired: " + temporaryPhoneNumber.first)
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // This job is not periodic. Ask the system to reschedule us if we are stopped, so that the cleanup can run later.
        return true
    }
}
