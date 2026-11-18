package com.wanderwildwood.ibasho.utils

import java.net.URI

/**
 * A phone number as it may go in the app's log, which can be exported and attached to a bug
 * report: its last three digits only, enough to tell two senders apart.
 */
fun maskNumberForLog(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length <= 3) "***" else "***" + digits.takeLast(3)
}

/**
 * An address as it may go in the log: its host only. A push endpoint's path is what lets
 * anyone holding it wake the phone, so it stays out.
 */
fun hostForLog(url: String): String =
    runCatching { URI(url.trim()).host }.getOrNull() ?: "(address not shown)"
