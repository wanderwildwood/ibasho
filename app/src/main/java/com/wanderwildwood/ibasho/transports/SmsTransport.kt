package com.wanderwildwood.ibasho.transports

import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.commands.AccessResponse
import com.wanderwildwood.ibasho.commands.ParserResult
import com.wanderwildwood.ibasho.commands.PasswordPause
import com.wanderwildwood.ibasho.commands.WrongPasswords
import com.wanderwildwood.ibasho.commands.hasPermission
import com.wanderwildwood.ibasho.data.AccessRepository
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.database.TEMP_USAGE_VALIDITY_MILLIS
import com.wanderwildwood.ibasho.database.TempPhoneNumber
import com.wanderwildwood.ibasho.permissions.SmsPermission
import com.wanderwildwood.ibasho.services.TempContactExpiredService
import com.wanderwildwood.ibasho.ui.access.AccessControlActivity
import com.wanderwildwood.ibasho.utils.log
import com.wanderwildwood.ibasho.utils.normalizeNumberForStorage
import com.wanderwildwood.ibasho.utils.maskNumberForLog


class SmsTransport(
    private val context: Context,
    private val phoneNumber: String,
    private val subscriptionId: Int,
) : Transport<String>(phoneNumber) {

    companion object {
        private val TAG = SmsTransport::class.simpleName
    }

    private val settings = SettingsRepository.getInstance(context)
    private val accessRepo = AccessRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_sms

    @get:StringRes
    override val title = R.string.transport_sms_title

    private val keyword = settings.get(Settings.SET_FMD_COMMAND) as String
    override val description = context.getString(R.string.transport_sms_description, keyword)

    override val descriptionAuth =
        context.getString(R.string.transport_sms_description_auth, keyword)

    override val descriptionNote = context.getString(R.string.transport_sms_description_note)

    override val requiredPermissions = listOf(SmsPermission())

    override val actions = listOf(TransportAction(R.string.Settings_Access_Control) { activity ->
        activity.startActivity(Intent(context, AccessControlActivity::class.java))
    })

    override fun getDestinationString() =
        normalizeNumberForStorage(context, phoneNumber) ?: phoneNumber

    override suspend fun isAllowed(parsed: ParserResult.Success): AccessResponse {
        var isKnownButDenied = false

        // Case 1: phone number in Allowed Contacts
        val storedNumber = accessRepo.getPhoneNumber(phoneNumber)
        if (storedNumber != null) {
            val hasPermission = storedNumber.permission.hasPermission(parsed.command.permission)
            if (hasPermission) {
                context.log()
                    .i(TAG, "${maskNumberForLog(storedNumber.number)} used FMD via allowlist")
                return AccessResponse.ALLOWED
            } else {
                // Even if the number is in the list of phone numbers and is explicitly denied this command, continue anyway.
                // Check below if there is a password that allows the command.
                // As long as one element allows access, that is sufficient.
                context.log().i(
                    TAG,
                    "${maskNumberForLog(storedNumber.number)} denied access to ${parsed.command.keyword}. Continuing to check for password."
                )
                isKnownButDenied = true
            }
        }

        // Case 2: phone number in temporary allowlist (i.e., it sent the correct password earlier)
        // A grant that has run out counts as none, whether or not the cleanup job has removed it
        // yet: it is removed now (with its "expired" text), and the password in this message is
        // checked below. A grant that lacks this command's permission does not stop a password
        // in the message from being checked either; a right one takes its place.
        var replacedTempNumber: TempPhoneNumber? = null
        val storedTempNumber = accessRepo.getTempPhoneNumber(phoneNumber)
        if (storedTempNumber != null) {
            val hasPermission =
                storedTempNumber.smsPassword.permission.hasPermission(parsed.command.permission)
            val isExpired = storedTempNumber.tempPhoneNumber.isExpired()

            if (hasPermission && !isExpired) {
                context.log().i(TAG, "${maskNumberForLog(phoneNumber)} used FMD via temporary allowlist")
                return AccessResponse.ALLOWED
            } else if (isExpired) {
                context.log().i(TAG, "${maskNumberForLog(phoneNumber)}: temporary access has run out")
                accessRepo.removeAndNotifyExpiredTempPhoneNumbers()
            } else {
                val passwordLabel = storedTempNumber.smsPassword.toDisplayLabel(context)
                context.log().i(
                    TAG,
                    "${maskNumberForLog(phoneNumber)} denied access to ${parsed.command.keyword} via password=$passwordLabel"
                )
                if (parsed.passwordHash == null) return AccessResponse.DENIED_EXISTS
                isKnownButDenied = true
                replacedTempNumber = storedTempNumber.tempPhoneNumber
            }
        }

        // Case 3: the message contains a correct password
        if (parsed.passwordHash != null) {
            val wrongPasswords = WrongPasswords(context, WrongPasswords.SMS, getDestinationString())
            if (wrongPasswords.pause() != PasswordPause.NONE) {
                // Too many wrong ones lately, from this number or from all of them together:
                // not even checked. From a number locked out, it counts as one more.
                context.log().w(TAG, "SMS password not checked: too many wrong passwords lately")
                countWrongPassword(wrongPasswords)
                return if (isKnownButDenied) AccessResponse.DENIED_EXISTS else AccessResponse.DENIED_UNKNOWN
            }
            val smsPass = accessRepo.getSmsPassword(parsed.passwordHash)
            if (smsPass == null) {
                context.log().i(TAG, "Received unknown SMS password")
                countWrongPassword(wrongPasswords)
            } else {
                wrongPasswords.right()
                context.log().i(TAG, "${maskNumberForLog(phoneNumber)} used FMD via SMS password '${smsPass.label}'")

                // Whatever the command is: add this phone number to the temporary list
                // (the number is there at most once, so a grant it had makes way)
                replacedTempNumber?.let { accessRepo.deleteTempPhoneNumber(it) }
                accessRepo.insertTempPhoneNumber(phoneNumber, subscriptionId, smsPass)

                send(context, context.getString(R.string.access_sms_password_granted_info))

                TempContactExpiredService.scheduleJob(context, TEMP_USAGE_VALIDITY_MILLIS + 1000)

                // Return based on whether this command is allowed
                val hasPermission = smsPass.permission.hasPermission(parsed.command.permission)
                return if (hasPermission) AccessResponse.ALLOWED else AccessResponse.DENIED_EXISTS
            }
        }

        return if (isKnownButDenied) AccessResponse.DENIED_EXISTS else AccessResponse.DENIED_UNKNOWN
    }

    private fun countWrongPassword(wrongPasswords: WrongPasswords) {
        notifyPauseStarted(context, wrongPasswords.wrong(), context.getString(R.string.transport_sms_title))
    }

    override fun send(context: Context, msg: String) {
        super.send(context, msg)

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val defaultSmsManager = context.getSystemService(SmsManager::class.java)
            if (subscriptionId == -1) {
                defaultSmsManager
            } else {
                defaultSmsManager.createForSubscriptionId(subscriptionId)
            }
        } else {
            if (subscriptionId == -1) {
                SmsManager.getDefault()
            } else {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
        }

        if (msg.length <= 160) {
            smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
        } else {
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        }
    }
}