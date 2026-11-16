package com.wanderwildwood.ibasho.commands

import android.content.Context


/**
 * How many wrong passwords in a row, and when the last one came. Nothing secret is kept.
 */
data class WrongPasswordState(val wrong: Int = 0, val lastWrongMillis: Long = 0L)

/**
 * The rules for wrong passwords, kept apart from storage so they can be tested.
 *
 * After [MAX_WRONG] wrong passwords in a row, passwords on that channel are not even checked
 * for [LOCK_MILLIS]. A password that comes in while locked counts as wrong and starts the hour
 * again, so guessing runs at a few tries an hour at most. One right password clears the count;
 * so does a day with no wrong one.
 */
object WrongPasswordRules {
    const val MAX_WRONG = 5
    const val LOCK_MILLIS = 60 * 60 * 1000L
    const val FORGET_MILLIS = 24 * 60 * 60 * 1000L

    fun isLocked(s: WrongPasswordState, now: Long): Boolean =
        s.wrong >= MAX_WRONG && now - s.lastWrongMillis in 0 until LOCK_MILLIS

    fun afterWrong(s: WrongPasswordState, now: Long): WrongPasswordState {
        val stale = now - s.lastWrongMillis !in 0..FORGET_MILLIS
        val base = if (stale) 0 else s.wrong
        return WrongPasswordState(base + 1, now)
    }
}

/**
 * Wrong passwords per channel, kept in the app's own preferences (counts and times only).
 */
class WrongPasswords(context: Context, private val channel: String) {

    companion object {
        const val SMS = "sms"
        const val NOTIFICATION = "notification"

        private const val FILE = "wrong_passwords"
        private val lock = Any()
    }

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun read() = WrongPasswordState(
        prefs.getInt("${channel}_wrong", 0),
        prefs.getLong("${channel}_last", 0L),
    )

    private fun write(s: WrongPasswordState) {
        prefs.edit()
            .putInt("${channel}_wrong", s.wrong)
            .putLong("${channel}_last", s.lastWrongMillis)
            .apply()
    }

    fun isLocked(now: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        WrongPasswordRules.isLocked(read(), now)
    }

    /** Counts one wrong password. Returns the count, and whether this one started a lock. */
    fun wrong(now: Long = System.currentTimeMillis()): Pair<Int, Boolean> = synchronized(lock) {
        val before = read()
        val after = WrongPasswordRules.afterWrong(before, now)
        write(after)
        val started = !WrongPasswordRules.isLocked(before, now) && WrongPasswordRules.isLocked(after, now)
        Pair(after.wrong, started)
    }

    fun right() = synchronized(lock) { write(WrongPasswordState()) }
}
