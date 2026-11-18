package com.wanderwildwood.ibasho.commands

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest


/**
 * How many wrong passwords in a row from one sender, and when the last one came.
 */
data class WrongPasswordState(val wrong: Int = 0, val lastWrongMillis: Long = 0L)

/**
 * Wrong passwords from all senders on a channel within one hour, counted from the first.
 */
data class ChannelWrongState(val wrong: Int = 0, val windowStartMillis: Long = 0L)

/**
 * The rules for wrong passwords, kept apart from storage so they can be tested.
 *
 * Each sender has its own count. After [MAX_WRONG] wrong passwords in a row from one sender,
 * that sender's passwords are not even checked for [LOCK_MILLIS]. A password it sends while
 * locked counts as wrong and starts its hour again. One right password clears the sender's
 * count; so does a day with no wrong one. A stranger guessing only locks out himself.
 *
 * Over all senders together, at most [MAX_WRONG_PER_HOUR] wrong passwords are checked in an
 * hour, counted from the first wrong one. Once there are that many, the whole channel is
 * paused until the hour is over, so many senders cannot guess faster than that. A password
 * that comes in during the pause is not counted, so the pause never runs longer than the hour.
 */
object WrongPasswordRules {
    const val MAX_WRONG = 5
    const val LOCK_MILLIS = 60 * 60 * 1000L
    const val FORGET_MILLIS = 24 * 60 * 60 * 1000L

    const val MAX_WRONG_PER_HOUR = 30
    const val WINDOW_MILLIS = 60 * 60 * 1000L

    fun isLocked(s: WrongPasswordState, now: Long): Boolean =
        s.wrong >= MAX_WRONG && now - s.lastWrongMillis in 0 until LOCK_MILLIS

    fun afterWrong(s: WrongPasswordState, now: Long): WrongPasswordState {
        val stale = now - s.lastWrongMillis !in 0..FORGET_MILLIS
        val base = if (stale) 0 else s.wrong
        return WrongPasswordState(base + 1, now)
    }

    /** Whether this sender's count can be dropped: nothing about it would still matter. */
    fun isForgotten(s: WrongPasswordState, now: Long): Boolean =
        now - s.lastWrongMillis !in 0..FORGET_MILLIS

    private fun inWindow(s: ChannelWrongState, now: Long): Boolean =
        now - s.windowStartMillis in 0 until WINDOW_MILLIS

    fun isPaused(s: ChannelWrongState, now: Long): Boolean =
        s.wrong >= MAX_WRONG_PER_HOUR && inWindow(s, now)

    fun afterWrong(s: ChannelWrongState, now: Long): ChannelWrongState =
        if (inWindow(s, now)) ChannelWrongState(s.wrong + 1, s.windowStartMillis)
        else ChannelWrongState(1, now)
}

/** Why a password was not checked. */
enum class PasswordPause { NONE, SENDER, CHANNEL }

/**
 * Wrong passwords on one channel, for one sender and for all senders together, kept in the
 * app's own preferences. Counts and times only; a sender is kept as a short hash, not as its
 * number or app name.
 */
class WrongPasswords(context: Context, private val channel: String, sender: String) {

    companion object {
        const val SMS = "sms"
        const val NOTIFICATION = "notification"

        private const val FILE = "wrong_passwords"
        private val lock = Any()

        /** A short, stable stand-in for a sender, so no number is written down. */
        fun senderKey(sender: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(sender.toByteArray(Charsets.UTF_8))
            return digest.take(12).joinToString("") { "%02x".format(it) }
        }
    }

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val senderPrefix = "${channel}_sender_"
    private val senderWrong = "$senderPrefix${senderKey(sender)}_wrong"
    private val senderLast = "$senderPrefix${senderKey(sender)}_last"
    private val channelWrong = "${channel}_all_wrong"
    private val channelStart = "${channel}_all_start"

    private fun readSender() = WrongPasswordState(
        prefs.getInt(senderWrong, 0),
        prefs.getLong(senderLast, 0L),
    )

    private fun readChannel() = ChannelWrongState(
        prefs.getInt(channelWrong, 0),
        prefs.getLong(channelStart, 0L),
    )

    /** Whether a password from this sender is checked now, and if not, why. */
    fun pause(now: Long = System.currentTimeMillis()): PasswordPause = synchronized(lock) {
        when {
            WrongPasswordRules.isPaused(readChannel(), now) -> PasswordPause.CHANNEL
            WrongPasswordRules.isLocked(readSender(), now) -> PasswordPause.SENDER
            else -> PasswordPause.NONE
        }
    }

    /**
     * Counts one wrong password from this sender. While the channel is paused nothing is
     * counted. Returns the pause this one started, if any.
     */
    fun wrong(now: Long = System.currentTimeMillis()): PasswordPause {
        synchronized(lock) {
            return countWrong(now)
        }
    }

    private fun countWrong(now: Long): PasswordPause {
        val channelBefore = readChannel()
        if (WrongPasswordRules.isPaused(channelBefore, now)) return PasswordPause.NONE

        val senderBefore = readSender()
        val senderAfter = WrongPasswordRules.afterWrong(senderBefore, now)
        val edit = prefs.edit()
            .putInt(senderWrong, senderAfter.wrong)
            .putLong(senderLast, senderAfter.lastWrongMillis)

        // A sender already locked out is not checked, so its tries are not guesses.
        val wasChecked = !WrongPasswordRules.isLocked(senderBefore, now)
        var channelAfter = channelBefore
        if (wasChecked) {
            channelAfter = WrongPasswordRules.afterWrong(channelBefore, now)
            edit.putInt(channelWrong, channelAfter.wrong).putLong(channelStart, channelAfter.windowStartMillis)
        }
        forgetOld(edit, now)
        edit.apply()

        return when {
            WrongPasswordRules.isPaused(channelAfter, now) -> PasswordPause.CHANNEL
            wasChecked && WrongPasswordRules.isLocked(senderAfter, now) -> PasswordPause.SENDER
            else -> PasswordPause.NONE
        }
    }

    /** A right password from this sender clears its count. */
    fun right() = synchronized(lock) {
        prefs.edit().remove(senderWrong).remove(senderLast).apply()
    }

    /** Drops other senders' counts that no longer matter, and the old per-channel count. */
    private fun forgetOld(edit: SharedPreferences.Editor, now: Long) {
        edit.remove("${channel}_wrong").remove("${channel}_last")
        for ((key, value) in prefs.all) {
            if (!key.startsWith(senderPrefix) || !key.endsWith("_last") || key == senderLast) continue
            val last = value as? Long ?: continue
            if (WrongPasswordRules.isForgotten(WrongPasswordState(0, last), now)) {
                edit.remove(key).remove(key.removeSuffix("_last") + "_wrong")
            }
        }
    }
}
