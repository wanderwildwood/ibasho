package com.wanderwildwood.ibasho.commands

/**
 * Which providers a locate runs, from the words after "locate".
 *
 * A word picks its provider; "all" adds fused and cell. With no provider word, fused runs,
 * and cell (which asks BeaconDB, and OpenCelliD when a key is set) runs only when the
 * owner has turned on "Locate by cell towers and Wi-Fi" ([cellByDefault]).
 */
data class LocateChoice(val fused: Boolean, val gps: Boolean, val cell: Boolean) {
    companion object {
        fun from(args: List<String>, cellByDefault: Boolean): LocateChoice {
            val all = args.contains("all")
            val cell = args.contains("cell")
            val fused = args.contains("fused")
            val gps = args.contains("gps")
            val plain = !cell && !fused && !gps && !all
            return LocateChoice(
                fused = fused || all || plain,
                gps = gps,
                cell = cell || all || (plain && cellByDefault),
            )
        }
    }
}
