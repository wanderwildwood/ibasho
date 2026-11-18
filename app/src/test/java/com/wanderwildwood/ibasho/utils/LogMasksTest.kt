package com.wanderwildwood.ibasho.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogMasksTest {

    @Test
    fun numberKeepsOnlyItsLastThreeDigits() {
        assertEquals("***142", maskNumberForLog("+1 555 010 0142"))
        assertFalse(maskNumberForLog("+15550100142").contains("555"))
    }

    @Test
    fun shortNumberIsHiddenWhole() {
        assertEquals("***", maskNumberForLog("911"))
        assertEquals("***", maskNumberForLog(""))
    }

    @Test
    fun addressKeepsOnlyItsHost() {
        assertEquals("push.example.org", hostForLog("https://push.example.org/wpush/v2/abcdef123456"))
    }

    @Test
    fun brokenAddressIsNotShown() {
        assertEquals("(address not shown)", hostForLog("not a url at all"))
    }
}
