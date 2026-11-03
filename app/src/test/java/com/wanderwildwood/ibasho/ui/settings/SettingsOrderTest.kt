package com.wanderwildwood.ibasho.ui.settings

import com.wanderwildwood.ibasho.ui.helper.SettingsEntry
import com.wanderwildwood.ibasho.ui.helper.SettingsEntry.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsOrderTest {
    private fun groups(debug: Boolean): List<List<Action>> {
        val out = mutableListOf<MutableList<Action>>()
        for (r in SettingsEntry.rows(debug)) {
            if (r.startsGroup || out.isEmpty()) out.add(mutableListOf())
            out.last().add(r.action)
        }
        return out
    }

    @Test fun everyRowOnceInItsGroup() {
        assertEquals(
            listOf(
                listOf(Action.SERVER, Action.FAMILY),
                listOf(Action.COMMANDS, Action.ACCESS_CONTROL),
                listOf(Action.LOCATION_SOURCES),
                listOf(Action.EXPORT, Action.IMPORT),
                listOf(Action.LANGUAGE, Action.SETUP_GUIDE),
                listOf(Action.LOGS, Action.TROUBLESHOOTING),
            ),
            groups(debug = true),
        )
        assertEquals(Action.entries.size, SettingsEntry.ORDER.map { it.action }.toSet().size)
        assertEquals(Action.entries.size, SettingsEntry.ORDER.size)
    }

    @Test fun troubleshootingOnlyInDebugBuilds() {
        assertFalse(SettingsEntry.rows(false).any { it.action == Action.TROUBLESHOOTING })
        assertEquals(listOf(Action.LOGS), groups(debug = false).last())
    }
}
