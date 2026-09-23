package com.wanderwildwood.ibasho.utils

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale


/** Format with E.164 for storage, since this is unique: +4912345678 */
fun normalizeNumberForStorage(context: Context, number: String): String? {
    val tm: TelephonyManager = context.getSystemService(TelephonyManager::class.java)
    val iso = tm.networkCountryIso
        .ifBlank { tm.simCountryIso }
        .ifBlank { Locale.getDefault().country }

    return PhoneNumberUtils.formatNumberToE164(number, iso.uppercase())
}


/** Format with pretty spacing for display: +49 123 45 678 */
fun normalizeNumberForDisplay(context: Context, number: String): String? {
    val tm: TelephonyManager = context.getSystemService(TelephonyManager::class.java)
    val iso = tm.networkCountryIso
        .ifBlank { tm.simCountryIso }
        .ifBlank { Locale.getDefault().country }

    return PhoneNumberUtils.formatNumber(number, iso.uppercase())
}
