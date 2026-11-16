package com.wanderwildwood.ibasho.commands

import com.wanderwildwood.ibasho.commands.WrongPasswordRules.FORGET_MILLIS
import com.wanderwildwood.ibasho.commands.WrongPasswordRules.LOCK_MILLIS
import com.wanderwildwood.ibasho.commands.WrongPasswordRules.MAX_WRONG
import com.wanderwildwood.ibasho.commands.WrongPasswordRules.afterWrong
import com.wanderwildwood.ibasho.commands.WrongPasswordRules.isLocked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class WrongPasswordRulesTest {

    private val t0 = 1_800_000_000_000L

    private fun wrongTimes(n: Int, start: WrongPasswordState = WrongPasswordState(), at: Long = t0): WrongPasswordState {
        var s = start
        repeat(n) { s = afterWrong(s, at) }
        return s
    }

    @Test
    fun fewWrongDoNotLock() {
        assertFalse(isLocked(wrongTimes(MAX_WRONG - 1), t0))
    }

    @Test
    fun maxWrongLocksForAnHour() {
        val s = wrongTimes(MAX_WRONG)
        assertTrue(isLocked(s, t0))
        assertTrue(isLocked(s, t0 + LOCK_MILLIS - 1))
        assertFalse(isLocked(s, t0 + LOCK_MILLIS))
    }

    @Test
    fun oneWrongAfterTheLockLocksAgain() {
        val s = afterWrong(wrongTimes(MAX_WRONG), t0 + LOCK_MILLIS)
        assertTrue(isLocked(s, t0 + LOCK_MILLIS + 1))
    }

    @Test
    fun aQuietDayForgets() {
        val s = afterWrong(wrongTimes(MAX_WRONG), t0 + FORGET_MILLIS + 1)
        assertEquals(1, s.wrong)
        assertFalse(isLocked(s, t0 + FORGET_MILLIS + 2))
    }

    @Test
    fun aClockGoingBackDoesNotLockForever() {
        val s = wrongTimes(MAX_WRONG)
        assertFalse(isLocked(s, t0 - 1))
    }
}
