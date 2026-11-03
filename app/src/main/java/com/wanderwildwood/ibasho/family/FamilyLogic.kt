package com.wanderwildwood.ibasho.family

/**
 * The decisions behind family sharing, kept free of Android and the network so they can be tested.
 * See docs/PLAN-family.md.
 */

/** Someone else on the server, as the server lists them. */
data class FamilyPerson(
    val name: String,
    /** Base64 of their 65-byte public point; empty if they have not set up sharing yet. */
    val publicKey64: String,
    val paused: Boolean,
)

/** What one upload should do. */
data class SharePlan(
    /** People to seal this place for. */
    val sealTo: List<FamilyPerson>,
    /** Everyone this phone still shares with, sent so the server drops copies for anyone else. */
    val audience: List<String>,
    /** Chosen people whose key is not the one pinned: nothing new is sealed to them. */
    val keyChanged: List<String>,
    /** Pins after this upload: first-seen keys are added. */
    val pins: Map<String, String>,
)

object FamilyLogic {

    /**
     * Who gets a copy. Only people the owner ticked ([chosen]), who are on the server and have a
     * key. A key seen for the first time is pinned; a key that differs from its pin is not sealed
     * to (a server could hand over its own key in someone's place), and the person stays in the
     * audience so the copy sealed to their old key, which only they can open, is left alone.
     */
    fun plan(
        me: String,
        people: List<FamilyPerson>,
        chosen: Set<String>,
        pins: Map<String, String>,
    ): SharePlan {
        val newPins = pins.toMutableMap()
        val sealTo = mutableListOf<FamilyPerson>()
        val keyChanged = mutableListOf<String>()
        val audience = mutableListOf<String>()
        for (p in people.sortedBy { it.name }) {
            if (p.name == me || p.name !in chosen) continue
            audience += p.name
            if (p.publicKey64.isEmpty()) continue
            val pinned = pins[p.name]
            when {
                pinned == null -> {
                    newPins[p.name] = p.publicKey64
                    sealTo += p
                }
                pinned == p.publicKey64 -> sealTo += p
                else -> keyChanged += p.name
            }
        }
        return SharePlan(sealTo, audience, keyChanged, newPins)
    }

    /** What the Family screen says about one person. */
    sealed interface Seen {
        /** They share with this phone; [unixMillis] is when the place was taken. */
        data class Place(val lat: Double, val lon: Double, val accuracy: Float?, val battery: Int?, val unixMillis: Long) : Seen
        data object NotSharing : Seen
        data object Paused : Seen
        data object NotSetUp : Seen
        data object KeyChanged : Seen
        data object Unreadable : Seen
    }

    /**
     * The state of one person before their copy is opened: [Seen.Place] is decided by the caller
     * after opening. Returns null when the copy should be opened with [publicKey64].
     */
    fun before(person: FamilyPerson, hasCopy: Boolean, pins: Map<String, String>): Seen? = when {
        person.paused -> Seen.Paused
        person.publicKey64.isEmpty() -> Seen.NotSetUp
        pins[person.name] != null && pins[person.name] != person.publicKey64 -> Seen.KeyChanged
        !hasCopy -> Seen.NotSharing
        else -> null
    }

    /** Pins a key the first time it is seen on the viewing side too. */
    fun pinOnSight(people: List<FamilyPerson>, pins: Map<String, String>): Map<String, String> {
        val out = pins.toMutableMap()
        for (p in people) if (p.publicKey64.isNotEmpty() && p.name !in out) out[p.name] = p.publicKey64
        return out
    }

    /** "just now", "12 min", "3 h", or null for older (the caller shows a date). */
    fun age(nowMillis: Long, thenMillis: Long): Age {
        val d = (nowMillis - thenMillis).coerceAtLeast(0)
        return when {
            d < 60_000 -> Age.JustNow
            d < 3_600_000 -> Age.Minutes((d / 60_000).toInt())
            d < 86_400_000 -> Age.Hours((d / 3_600_000).toInt())
            else -> Age.Date(thenMillis)
        }
    }

    sealed interface Age {
        data object JustNow : Age
        data class Minutes(val n: Int) : Age
        data class Hours(val n: Int) : Age
        data class Date(val millis: Long) : Age
    }
}
