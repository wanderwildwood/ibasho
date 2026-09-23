package com.wanderwildwood.ibasho.commands

/**
 * Bitmask for encoding fine-grained, per-command permissions into a Long.
 * This allows us to store a single Long in the database to persist the permissions.
 */
enum class FmdPermission(val bit: Long) {

    // In practice, "help" should always be allowed.
    // But the implementation is simpler if we have a bitmask value for it.
    // Changing this should not be offered by the UI.
    HELP(1L shl 0),

    BLUETOOTH(1L shl 1),
    // 1L shl 2 was CAMERA and 1L shl 3 was DELETE; both commands were removed.
    // Keep the bits unused so stored masks never grant something new.
    FLASH(1L shl 4),
    GPS(1L shl 5),
    LOCATE(1L shl 6),
    LOCK(1L shl 7),
    NO_DISTURB(1L shl 8),
    RING(1L shl 9),
    RINGER_MODE(1L shl 10),
    STATS(1L shl 11),
    BLUETOOTH_SCAN(1L shl 12),
    ;

    companion object {
        val NONE = 0L

        val ALL: Long = entries.fold(0L) { acc, p -> acc or p.bit }

        /**
         * A sensible set of locked-down, but still useful default permissions.
         */
        val DEFAULT: Long = NONE
            .addPermission(HELP) // should always be allowed
            .addPermission(FLASH)
            .addPermission(LOCATE)
            .addPermission(RING)
    }
}

fun Long.hasPermission(p: FmdPermission): Boolean {
    return this and p.bit != 0L
}

fun Long.setPermission(p: FmdPermission, enabled: Boolean): Long {
    return if (enabled) addPermission(p) else removePermission(p)
}

fun Long.addPermission(p: FmdPermission): Long {
    return this or p.bit
}

fun Long.removePermission(p: FmdPermission): Long {
    return this and p.bit.inv()
}

fun Long.togglePermission(p: FmdPermission): Long {
    return this xor p.bit
}
