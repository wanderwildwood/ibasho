package de.nulide.findmydevice.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.utils.log
import de.nulide.findmydevice.utils.normalizePhoneNumber
import de.nulide.findmydevice.workers.CommandExecutionWorker


class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = SmsReceiver::class.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            // wrong receiver
            return
        }
        val bundle = intent.extras ?: return
        val format = bundle.getString("format") ?: return
        val pdus = bundle.get("pdus") as Array<ByteArray>? ?: return
        val subscriptionId = intent.getIntExtra("subscription", -1)

        var phoneNumber: String? = null
        var fullMessageBuilder = StringBuilder()

        // Long, multipart SMS: combine into one message
        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu, format)
            if (phoneNumber == null) {
                phoneNumber = sms.originatingAddress
            }
            fullMessageBuilder.append(sms.messageBody)
        }

        if (phoneNumber.isNullOrBlank()) {
            context.log().i(TAG, "Cannot handle SMS: phoneNumber is empty!")
            return
        }
        phoneNumber = normalizePhoneNumber(context, phoneNumber) ?: phoneNumber

        val msg = fullMessageBuilder.toString()

        if (msg.isBlank()) {
            context.log().i(TAG, "Cannot handle SMS: msg is empty!")
            return
        }

        // Early sanity check + abort
        val settings = SettingsRepository.getInstance(context)
        val fmdTriggerWord = (settings.get(Settings.SET_FMD_COMMAND) as String).lowercase()
        if (!msg.lowercase().startsWith(fmdTriggerWord)) {
            return
        }

        val inputData = workDataOf(
            CommandExecutionWorker.KEY_COMMAND to msg,
            CommandExecutionWorker.KEY_TRANSPORT_TYPE to CommandExecutionWorker.TRANS_SMS,
            CommandExecutionWorker.KEY_DESTINATION to phoneNumber,
            CommandExecutionWorker.KEY_SMS_SUBSCRIPTION_ID to subscriptionId,
        )
        val workRequest = OneTimeWorkRequestBuilder<CommandExecutionWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
