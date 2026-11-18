package com.wanderwildwood.ibasho.data

import com.wanderwildwood.ibasho.data.LastKnownLocationRule.SAME_MOMENT_MILLIS
import com.wanderwildwood.ibasho.data.LastKnownLocationRule.shouldReplace
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class LastKnownLocationRuleTest {

    private val t = 1_800_000_000_000L
    private val now = t + 60_000L

    @Test
    fun anythingReplacesAnEmptyCache() {
        assertTrue(shouldReplace(-1L, Float.NaN, t, null, now))
    }

    @Test
    fun towerWithoutAccuracyDoesNotReplaceGpsFromTheSameRun() {
        assertFalse(shouldReplace(t, 8f, t + 20_000L, null, now))
        assertFalse(shouldReplace(t, 8f, t + 20_000L, Float.NaN, now))
    }

    @Test
    fun coarserFixFromTheSameMomentIsKeptOut() {
        assertFalse(shouldReplace(t, 8f, t + 20_000L, 1500f, now))
    }

    @Test
    fun gpsReplacesAnEarlierTowerFix() {
        assertTrue(shouldReplace(t, Float.NaN, t + 20_000L, 8f, now))
        assertTrue(shouldReplace(t, 1500f, t + 20_000L, 8f, now))
    }

    @Test
    fun aClearlyNewerFixAlwaysWins() {
        assertTrue(shouldReplace(t, 8f, t + SAME_MOMENT_MILLIS, null, now + SAME_MOMENT_MILLIS))
    }

    @Test
    fun aClearlyOlderFixNeverWins() {
        assertFalse(shouldReplace(t, 1500f, t - SAME_MOMENT_MILLIS, 8f, now))
    }

    @Test
    fun aCacheDatedInTheFutureDoesNotBlock() {
        assertTrue(shouldReplace(now + 10 * SAME_MOMENT_MILLIS, 8f, t, null, now))
    }
}
