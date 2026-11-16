package com.wanderwildwood.ibasho.commands

import org.junit.Assert.assertEquals
import org.junit.Test


class RedactTest {

    @Test
    fun noPasswordIsLeftAlone() {
        assertEquals("fmd locate gps", redactCommand("fmd locate gps", "fmd"))
    }

    @Test
    fun accessPasswordIsHidden() {
        assertEquals("fmd *** locate gps", redactCommand("fmd hunter2hunter locate gps", "fmd"))
    }

    @Test
    fun triggerWordIgnoresCase() {
        assertEquals("FMD *** ring", redactCommand("FMD hunter2hunter ring", "fmd"))
    }

    @Test
    fun wipePasswordIsHidden() {
        assertEquals("fmd delete *** dryrun", redactCommand("fmd delete correcthorse dryrun", "fmd"))
    }

    @Test
    fun bothPasswordsAreHidden() {
        assertEquals("fmd *** delete ***", redactCommand("fmd hunter2hunter delete correcthorse", "fmd"))
    }

    @Test
    fun serverCommandWithoutTriggerWord() {
        assertEquals("delete ***", redactCommand("delete correcthorse", null))
        assertEquals("locate", redactCommand("locate", null))
    }

    @Test
    fun lockMessageIsKept() {
        assertEquals("fmd lock Call Ada Whitlock", redactCommand("fmd lock Call Ada Whitlock", "fmd"))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", redactCommand("", "fmd"))
    }
}
