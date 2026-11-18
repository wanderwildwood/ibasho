package com.wanderwildwood.ibasho.commands

import org.junit.Assert.assertEquals
import org.junit.Test


class LocateChoiceTest {

    @Test
    fun aPlainLocateLeavesCellOutWhileTheSettingIsOff() {
        assertEquals(LocateChoice(fused = true, gps = false, cell = false), LocateChoice.from(emptyList(), false))
    }

    @Test
    fun aPlainLocateAsksCellWhenTheSettingIsOn() {
        assertEquals(LocateChoice(fused = true, gps = false, cell = true), LocateChoice.from(emptyList(), true))
    }

    @Test
    fun theAccuracyWordIsNotAProviderWord() {
        assertEquals(LocateChoice(fused = true, gps = false, cell = false), LocateChoice.from(listOf("acc=20"), false))
    }

    @Test
    fun cellAndAllStayExplicitWhateverTheSetting() {
        assertEquals(LocateChoice(fused = false, gps = false, cell = true), LocateChoice.from(listOf("cell"), false))
        assertEquals(LocateChoice(fused = true, gps = false, cell = true), LocateChoice.from(listOf("all"), false))
    }

    @Test
    fun namedProvidersRunWithoutCell() {
        assertEquals(LocateChoice(fused = false, gps = true, cell = false), LocateChoice.from(listOf("gps"), true))
        assertEquals(LocateChoice(fused = true, gps = true, cell = false), LocateChoice.from(listOf("fused", "gps"), true))
    }
}
