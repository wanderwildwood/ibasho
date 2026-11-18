package com.wanderwildwood.ibasho.commands

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpEchoTest {
    private val keywords = listOf("locate", "ring", "lock", "help", "gps", "stats", "delete", "ringermode", "bluetooth")

    @Test
    fun theHelpTextComingBackIsCaught() {
        val echo = "fmd commands\nlocate [gps|cell] - where it is\nring - rings\nlock - locks\nstats - battery"
        assertTrue(looksLikeHelpEcho(echo, "commands", keywords))
    }

    @Test
    fun aSentenceAfterLockStillRuns() {
        // "bring" is not "ring", and "Please" is not a keyword.
        assertFalse(looksLikeHelpEcho("fmd lock Please help, bring it to the desk", "lock", keywords))
    }

    @Test
    fun aPasswordWithKeywordsInItIsNotCounted() {
        assertFalse(looksLikeHelpEcho("fmd gpslockring locate", "gpslockring", keywords))
    }

    @Test
    fun aPasswordWithKeywordsAndPunctuationIsNotCounted() {
        assertFalse(looksLikeHelpEcho("fmd ring.lock locate", "ring.lock", keywords))
        assertFalse(looksLikeHelpEcho("fmd Gps#Lock ring", "Gps#Lock", keywords))
    }

    @Test
    fun aQuotedPasswordWithKeywordsIsNotCounted() {
        assertFalse(looksLikeHelpEcho("fmd \"gps lock\" locate", "gps lock", keywords))
    }
}
