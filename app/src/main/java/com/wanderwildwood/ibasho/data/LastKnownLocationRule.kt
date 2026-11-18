package com.wanderwildwood.ibasho.data

/**
 * Whether a new fix may take the place of the cached last known location.
 *
 * A locate runs the GPS and cell providers side by side, and whichever finishes last would
 * otherwise win, so a coarse tower position could replace the GPS fix taken in the same run.
 * Fixes close together in time are compared by accuracy; further apart, the newer one wins.
 */
object LastKnownLocationRule {

    /** Fixes this close together count as the same moment, and the more accurate one is kept. */
    const val SAME_MOMENT_MILLIS = 2 * 60 * 1000L

    /**
     * @param storedTimeMillis time of the cached fix, or a value <= 0 when nothing is cached
     * @param storedAccuracy accuracy of the cached fix in meters, null or NaN when unknown
     * @param nowMillis the wall clock now; a cached fix dated in the future (a clock set back)
     *   never blocks new ones
     */
    fun shouldReplace(
        storedTimeMillis: Long,
        storedAccuracy: Float?,
        newTimeMillis: Long,
        newAccuracy: Float?,
        nowMillis: Long,
    ): Boolean {
        if (storedTimeMillis <= 0L) return true
        if (storedTimeMillis > nowMillis + SAME_MOMENT_MILLIS) return true
        val storedAcc = storedAccuracy?.takeUnless { it.isNaN() }
        val newAcc = newAccuracy?.takeUnless { it.isNaN() }

        if (newTimeMillis >= storedTimeMillis + SAME_MOMENT_MILLIS) return true
        if (newTimeMillis <= storedTimeMillis - SAME_MOMENT_MILLIS) return false

        // About the same moment: a fix with no accuracy never displaces one that has it,
        // and a less accurate one never displaces a more accurate one.
        if (storedAcc == null) return newAcc != null || newTimeMillis >= storedTimeMillis
        if (newAcc == null) return false
        return newAcc <= storedAcc
    }
}
