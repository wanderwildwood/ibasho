package com.wanderwildwood.ibasho.push

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * The connection's Quick Settings tile, as Tailscale has one: lit while the connection is on,
 * a tap turns it off or on. See [ConnectionPause].
 */
class ConnectionTileService : TileService() {

    override fun onStartListening() = draw()

    override fun onClick() {
        ConnectionPause.setPaused(this, !ConnectionPause.isPaused(this))
        draw()
    }

    private fun draw() {
        val tile = qsTile ?: return
        tile.state = if (ConnectionPause.isPaused(this)) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
