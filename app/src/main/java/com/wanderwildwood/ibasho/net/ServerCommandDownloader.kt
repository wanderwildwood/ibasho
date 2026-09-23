package com.wanderwildwood.ibasho.net

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.net.model.MSG_ACCOUNT_LOCKED
import com.wanderwildwood.ibasho.net.model.MSG_OTHER
import com.wanderwildwood.ibasho.net.model.ServerMessage
import com.wanderwildwood.ibasho.utils.Notifications
import com.wanderwildwood.ibasho.utils.log
import com.wanderwildwood.ibasho.utils.toIsoDateTimeString
import com.wanderwildwood.ibasho.workers.CommandExecutionWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServerCommandDownloader(
    private val context: Context
) {

    companion object {
        val TAG = ServerCommandDownloader::class.simpleName
    }

    private val settingsRepo = SettingsRepository.getInstance(context)

    private var tryCount = 0

    /**
     * Downloads the latest command from the server.
     * Then enqueues a WorkManager work request to execute it.
     */
    fun download() {
        if (!settingsRepo.serverAccountExists()) {
            context.log().i(TAG, "No server account, so there is no command to download")
            return
        }

        context.log().i(TAG, "Downloading command from the server")
        val fmdServerRepo = FmdServerRepository(context).getApiService()
        fmdServerRepo.getCommand(::onResponse, ::onError)

        if ((settingsRepo.get(Settings.SET_FMD_CRYPT_PROTO) as Number).toInt() == FMD_SERVER_PROTO_V2) {
            (fmdServerRepo as FmdServerApiV2Repository).getMessages(
                listener = ::onMessageResponse,
                errorListener = { error ->
                    val msg =
                        "Error downloading server messages: statusCode=${error.statusCode ?: 0} msg=${error.message}"
                    context.log().e(TAG, msg)
                },
            )
        }
    }

    private fun onError(error: ServerError) {
        tryCount += 1
        val shouldRetry = tryCount < 3

        val statusCode = error.statusCode ?: 0
        val msg =
            "Error downloading command: statusCode=$statusCode msg=${error.message} attempt=$tryCount retrying=$shouldRetry"
        context.log().e(TAG, msg)

        if (statusCode == 423) {
            // Account is locked for 10 minutes, no need to retry
            return
        }

        if (shouldRetry) {
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                delay(1000L * tryCount)
                download()
            }
        }
    }

    private fun onResponse(remoteCommand: String) {
        if (remoteCommand.isBlank()) {
            return
        }

        if (remoteCommand.startsWith("423")) {
            showLoginBruteForceWarning(0) // APIv1 doesn't tell us the timestamp
            return
        }

        context.log().i(TAG, "Received remote command '$remoteCommand'")

        // The CommandParser needs the keyword prepended
        val commandKeyword = settingsRepo.get(Settings.SET_FMD_COMMAND).toString()
        val fullCommand = "$commandKeyword $remoteCommand"

        // Schedule work request to execute the command
        val inputData = Data.Builder()
            .putString(CommandExecutionWorker.KEY_COMMAND, fullCommand)
            .putString(
                CommandExecutionWorker.KEY_TRANSPORT_TYPE,
                CommandExecutionWorker.TRANS_FMD_SERVER
            )
            .putString(CommandExecutionWorker.KEY_DESTINATION, "FMD Server")
            .build()
        val workRequest: WorkRequest =
            OneTimeWorkRequest.Builder(CommandExecutionWorker::class.java)
                .setInputData(inputData)
                .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun onMessageResponse(messages: List<ServerMessage>) {
        val fmdServerRepo = FmdServerRepository(context).getApiService()

        for (m in messages) {
            when (m.code) {
                MSG_OTHER -> {
                    context.log().i(TAG, "Received server message, other, text=${m.text}")
                    Notifications.notify(
                        context,
                        context.getString(R.string.server_message_other_title),
                        context.getString(R.string.server_message_other_text, m.text),
                        Notifications.CHANNEL_SERVER
                    )
                }

                MSG_ACCOUNT_LOCKED -> showLoginBruteForceWarning(m.unixMillis)
                else -> context.log().w(TAG, "Unknown message code=${m.code} text=${m.text}")
            }

            (fmdServerRepo as FmdServerApiV2Repository).deleteSingleMessage(m.uuid)
        }
    }

    private fun showLoginBruteForceWarning(time: Long) {
        val account = settingsRepo.get(Settings.SET_FMDSERVER_ID) as String
        val msg: String = context.getString(
            R.string.server_login_attempts_text_time,
            account,
            time.toIsoDateTimeString()
        )
        context.log().w(TAG, msg)

        Notifications.notify(
            context,
            context.getString(R.string.server_login_attempts_title),
            msg,
            Notifications.CHANNEL_SERVER
        )
    }
}
