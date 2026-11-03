package com.wanderwildwood.ibasho.family

import com.wanderwildwood.ibasho.family.FamilyLogic.Age
import com.wanderwildwood.ibasho.family.FamilyLogic.Seen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyLogicTest {

    private val ada = FamilyPerson("ada-whitlock", "KEY-A", false)
    private val tomas = FamilyPerson("tomas-reyes", "KEY-T", false)
    private val newcomer = FamilyPerson("newcomer", "", false)
    private val me = FamilyPerson("me", "KEY-ME", false)
    private val everyone = listOf(tomas, me, ada, newcomer)

    @Test
    fun nobodyIsSharedWithUntilTicked() {
        val p = FamilyLogic.plan("me", everyone, emptySet(), emptyMap())
        assertTrue(p.sealTo.isEmpty())
        assertTrue(p.audience.isEmpty())
    }

    @Test
    fun onlyTickedPeopleWithAKeyGetACopyAndFirstKeysArePinned() {
        val p = FamilyLogic.plan("me", everyone, setOf("ada-whitlock", "newcomer", "me", "gone"), emptyMap())
        assertEquals(listOf("ada-whitlock"), p.sealTo.map { it.name })
        // The newcomer stays in the audience: they will get copies once they set up.
        assertEquals(listOf("ada-whitlock", "newcomer"), p.audience)
        assertEquals(mapOf("ada-whitlock" to "KEY-A"), p.pins)
    }

    @Test
    fun aChangedKeyIsNotSealedToButKeptInTheAudience() {
        val pins = mapOf("ada-whitlock" to "OLD-KEY", "tomas-reyes" to "KEY-T")
        val p = FamilyLogic.plan("me", everyone, setOf("ada-whitlock", "tomas-reyes"), pins)
        assertEquals(listOf("tomas-reyes"), p.sealTo.map { it.name })
        assertEquals(listOf("ada-whitlock"), p.keyChanged)
        assertEquals(listOf("ada-whitlock", "tomas-reyes"), p.audience)
        assertEquals("OLD-KEY", p.pins["ada-whitlock"])
    }

    @Test
    fun untickingDropsThePersonFromTheAudience() {
        val p = FamilyLogic.plan("me", everyone, setOf("tomas-reyes"), mapOf("ada-whitlock" to "KEY-A"))
        assertEquals(listOf("tomas-reyes"), p.audience)
    }

    @Test
    fun whatTheViewerIsTold() {
        val pins = mapOf("ada-whitlock" to "KEY-A")
        assertEquals(Seen.Paused, FamilyLogic.before(ada.copy(paused = true), true, pins))
        assertEquals(Seen.NotSetUp, FamilyLogic.before(newcomer, false, pins))
        assertEquals(Seen.KeyChanged, FamilyLogic.before(ada.copy(publicKey64 = "OTHER"), true, pins))
        assertEquals(Seen.NotSharing, FamilyLogic.before(tomas, false, pins))
        assertNull(FamilyLogic.before(ada, true, pins))
    }

    @Test
    fun keysArePinnedOnSight() {
        val pins = FamilyLogic.pinOnSight(everyone, mapOf("ada-whitlock" to "OLD"))
        assertEquals("OLD", pins["ada-whitlock"])
        assertEquals("KEY-T", pins["tomas-reyes"])
        assertTrue("newcomer" !in pins)
    }

    @Test
    fun ages() {
        val now = 10_000_000_000L
        assertEquals(Age.JustNow, FamilyLogic.age(now, now - 59_000))
        assertEquals(Age.Minutes(12), FamilyLogic.age(now, now - 12 * 60_000 - 5))
        assertEquals(Age.Hours(3), FamilyLogic.age(now, now - 3 * 3_600_000 - 5))
        assertEquals(Age.Date(now - 86_400_000), FamilyLogic.age(now, now - 86_400_000))
        assertEquals(Age.JustNow, FamilyLogic.age(now, now + 5_000)) // a clock ahead
    }
}
